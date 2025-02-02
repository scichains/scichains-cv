/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.arrays.IntArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.math.functions.AbstractFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

public final class SeveralThresholds extends MultiMatrix2DFilter {
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_LABELS = "labels";

    private double[] thresholds = {};
    private int[] values = {1};
    private boolean rawValues = false;

    public SeveralThresholds() {
        addInputMat(INPUT_MASK);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public SeveralThresholds setThresholds(double[] thresholds) {
        this.thresholds = nonNull(thresholds).clone();
        return this;
    }

    public SeveralThresholds setThresholds(String thresholds) {
        this.thresholds = new SScalar(nonNull(thresholds)).toDoubles();
        return this;
    }

    public int[] getValues() {
        return values.clone();
    }

    public SeveralThresholds setValues(int[] values) {
        nonNull(values);
        if (values.length == 0) {
            throw new IllegalArgumentException("At least 1 value must be specifed");
        }
        this.values = values.clone();
        return this;
    }

    public SeveralThresholds setValues(String values) {
        return setValues(new SScalar(nonNull(values)).toInts());
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public SeveralThresholds setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        return process(
                source,
                getInputMat(INPUT_MASK, true).toMultiMatrix2D());
    }

    public MultiMatrix2D process(MultiMatrix2D source, MultiMatrix2D mask) {
        final Matrix<? extends PArray> intensity = source.intensityChannel();
        final double scale = rawValues ? 1.0 : intensity.array().maxPossibleValue(1.0);
        final double[] scaledThresholds = new double[thresholds.length];
        for (int k = 0; k < scaledThresholds.length; k++) {
            scaledThresholds[k] = scale * thresholds[k];
        }
        final int[] appendedValues = new int[thresholds.length + 1];
        for (int k = 0; k < appendedValues.length; k++) {
            appendedValues[k] = k < this.values.length ? this.values[k] : k;
        }
        MultiMatrix2D result = MultiMatrix.of2DMono(Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        int result = appendedValues[0];
                        for (int k = 0; k < scaledThresholds.length; k++) {
                            if (x0 >= scaledThresholds[k]) {
                                result = appendedValues[k + 1];
                            }
                        }
                        return result;
                    }
                },
                IntArray.class, intensity));
        if (mask != null) {
            result = result.min(mask.nonZeroAnyChannel());
        }
        return result;
    }
}
