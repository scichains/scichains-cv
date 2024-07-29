/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.*;
import net.algart.matrices.morphology.BasicRankMorphology;
import net.algart.matrices.morphology.ContinuedRankMorphology;
import net.algart.matrices.morphology.RankPrecision;

public abstract class RankMorphologyFilter extends MorphologyFilter {
    private boolean interpolatedHistogram = false;
    private double optimizingScale = 1.0;
    // - note: pattern is not scaled automatically, you need to reduce it in compressionForFasterProcessing  times

    private long[] sourceMatrixDimensions = null;

    public boolean isInterpolatedHistogram() {
        return interpolatedHistogram;
    }

    public void setInterpolatedHistogram(boolean interpolatedHistogram) {
        this.interpolatedHistogram = interpolatedHistogram;
    }

    public double getOptimizingScale() {
        return optimizingScale;
    }

    public void setOptimizingScale(double optimizingScale) {
        if (optimizingScale < 1.0) {
            throw new IllegalArgumentException("Scale for faster processing " + optimizingScale + " must be >= 1.0");
        }
        this.optimizingScale = optimizingScale;
    }

    public net.algart.matrices.morphology.RankMorphology createRankMorphology(Class<?> elementType, double level) {
        net.algart.matrices.morphology.RankMorphology morphology =
                BasicRankMorphology.getInstance(null, level, rankPrecision(elementType));
        if (continuationMode != null) {
            morphology = ContinuedRankMorphology.getInstance(morphology, continuationMode);
        }
        return morphology;
    }

    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        return uncompress(processCompressedChannel(compress(m)));
    }

    protected Matrix<? extends PArray> compress(Matrix<? extends PArray> m) {
        this.sourceMatrixDimensions = m.dimensions();
        if (optimizingScale == 1.0) {
            return m;
        }
        long[] newDimensions = new long[sourceMatrixDimensions.length];
        for (int k = 0; k < newDimensions.length; k++) {
            newDimensions[k] = (long) (sourceMatrixDimensions[k] / optimizingScale);
        }
        Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(
                UpdatablePArray.class,
                m.elementType(),
                newDimensions);
        Matrices.resize(null, Matrices.ResizingMethod.AVERAGING, result, m);
        return result;
    }

    protected Matrix<? extends PArray> uncompress(Matrix<? extends PArray> m) {
        if (this.sourceMatrixDimensions == null) {
            throw new IllegalStateException("compress() method must be called at least once before uncompress");
        }
        if (optimizingScale == 1.0) {
            return m;
        }
        Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(
                UpdatablePArray.class,
                m.elementType(),
                sourceMatrixDimensions);
        Matrices.resize(null, Matrices.ResizingMethod.POLYLINEAR_INTERPOLATION, result, m);
        return result;
    }

    protected abstract Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m);

    protected final String rankMorphologyLogMessage() {
        return (interpolatedHistogram ? ", interpolated histogram" : "")
                + (optimizingScale == 1.0 ? "" :
                ", compressed in " + optimizingScale + " times");
    }

    RankPrecision rankPrecision(Class<?> elementType) {
        final long bitsPerElement = Arrays.bitsPerElement(elementType);
        RankPrecision result = bitsPerElement <= 8 ? RankPrecision.BITS_8
                : bitsPerElement <= 16 ? RankPrecision.BITS_16_PER_8
                : RankPrecision.BITS_22_PER_7;
        result = result.otherInterpolation(interpolatedHistogram);
        return result;
    }
}
