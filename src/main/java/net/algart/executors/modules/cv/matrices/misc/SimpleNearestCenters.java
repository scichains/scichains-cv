/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.algart.executors.modules.cv.matrices.misc;

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.arrays.*;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

public final class SimpleNearestCenters extends MultiMatrix2DFilter {
    public static final String INPUT_SAMPLE_IMAGE = "sample_image";
    public static final String INPUT_POSITIONS = "positions";
    public static final String OUTPUT_LABELS = "labels";

    private static final boolean OPTIMIZED_VERSION = true;

    private long dimX = 100;
    private long dimY = 100;

    public SimpleNearestCenters() {
        setDefaultInputMat(INPUT_SAMPLE_IMAGE);
        addInputNumbers(INPUT_POSITIONS);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public long getDimX() {
        return dimX;
    }

    public SimpleNearestCenters setDimX(long dimX) {
        this.dimX = positive(dimX);
        return this;
    }

    public long getDimY() {
        return dimY;
    }

    public SimpleNearestCenters setDimY(long dimY) {
        this.dimY = positive(dimY);
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        return process(source, getInputNumbers(INPUT_POSITIONS));
    }

    public MultiMatrix2D process(MultiMatrix2D source, SNumbers positions) {
        final long dimX = source == null ? this.dimX : source.dimX();
        final long dimY = source == null ? this.dimY : source.dimY();
        return process(dimX, dimY, positions);
    }

    public MultiMatrix2D process(long dimX, long dimY, SNumbers positions) {
        final float[] x = positions.column(0).toFloatArray();
        final float[] y = positions.column(1).toFloatArray();
        final Matrix<UpdatablePArray> result = Arrays.SMM.newMatrix(UpdatablePArray.class, int.class, dimX, dimY);
        final FindNearestPoint findingFunc = new FindNearestPoint(x, y);
        Matrices.copy(null, result, Matrices.asCoordFuncMatrix(findingFunc, IntArray.class, dimX, dimY));
        // - multithreaded filling
        return MultiMatrix.valueOf2DMono(result);
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

    private static class FindNearestPoint extends AbstractFunc {
        private final float[] x;
        private final float[] y;
        private final int n;

        FindNearestPoint(float[] x, float[] y) {
            assert x != null;
            assert y != null;
            assert x.length == y.length;
            this.x = x.clone();
            this.y = y.clone();
            this.n = x.length;
            // Sorting by y:
            ArraySorter.getQuickSorter().sort(0, n,
                    (firstIndex, secondIndex) -> {
                        final int i = (int) firstIndex;
                        final int j = (int) secondIndex;
                        return this.y[i] < this.y[j] || (this.y[i] == this.y[j] && this.x[i] < this.x[j]);
                        // - check also x for maximally stable results
                    },
                    (firstIndex, secondIndex) -> {
                        final int i = (int) firstIndex;
                        final int j = (int) secondIndex;
                        float temp = this.x[i];
                        this.x[i] = this.x[j];
                        this.x[j] = temp;
                        temp = this.y[i];
                        this.y[i] = this.y[j];
                        this.y[j] = temp;
                    });
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1]);
        }

        @Override
        public double get(double x, double y) {
            double minDistanceSquare = Double.POSITIVE_INFINITY;
            int index = -1;
            if (OPTIMIZED_VERSION) {
                int insertionPoint = java.util.Arrays.binarySearch(this.y, (float) y);
                if (insertionPoint < 0) {
                    insertionPoint = -insertionPoint - 1;
                }
                assert insertionPoint >= 0 && insertionPoint <= n;
                assert insertionPoint == 0 || this.y[insertionPoint - 1] <= (float) y;
                assert insertionPoint == n || this.y[insertionPoint] >= (float) y;
                for (int k = insertionPoint; k < n; k++) {
                    final double diffY = this.y[k] - y;
                    assert diffY >= 0.0;
                    final double diffYSquare = diffY * diffY;
                    if (diffYSquare >= minDistanceSquare) {
                        break;
                        // - no sense to further search in this direction
                    }
                    final double diffX = this.x[k] - x;
                    final double distanceSquare = diffX * diffX + diffYSquare;
                    if (distanceSquare < minDistanceSquare) {
                        // - if means that for equidistant points we select minimal index
                        minDistanceSquare = distanceSquare;
                        index = k;
                    }
                }
                for (int k = insertionPoint - 1; k >= 0; k--) {
                    final double diffY = y - this.y[k];
                    assert diffY >= 0.0;
                    final double diffYSquare = diffY * diffY;
                    if (diffYSquare > minDistanceSquare) {
                        break;
                        // - no sense to further search in this direction
                    }
                    final double diffX = this.x[k] - x;
                    final double distanceSquare = diffX * diffX + diffYSquare;
                    if (distanceSquare <= minDistanceSquare) {
                        // - if means that for equidistant points we select minimal index (k is decreasing)
                        minDistanceSquare = distanceSquare;
                        index = k;
                    }
                }
            } else {
                for (int k = 0; k < n; k++) {
                    final double diffX = this.x[k] - x;
                    final double diffY = this.y[k] - y;
                    final double distanceSquare = diffX * diffX + diffY * diffY;
                    if (distanceSquare < minDistanceSquare) {
                        // - if means that for equidistant points we select minimal index
                        minDistanceSquare = distanceSquare;
                        index = k;
                    }
                }
            }
            return index + 1;
        }
    }
}
