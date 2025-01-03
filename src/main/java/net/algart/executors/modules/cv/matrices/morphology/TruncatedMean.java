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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.DoubleArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.ChannelOperation;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.RankMorphology;

public final class TruncatedMean extends RankMorphologyFilter {
    public enum LimitInterpretation {
        NORMALIZED_0_1_PERCENTILE,
        NORMALIZED_0_1_VALUE;
    }

    private double lowLimit = 0.1;
    private double highLimit = 0.9;
    private LimitInterpretation limitInterpretation = LimitInterpretation.NORMALIZED_0_1_PERCENTILE;
    private double[] fillerColor = new double[3];

    public double getLowLimit() {
        return lowLimit;
    }

    public void setLowLimit(double lowLimit) {
        this.lowLimit = inRange(lowLimit, 0.0, 1.0);
    }

    public double getHighLimit() {
        return highLimit;
    }

    public void setHighLimit(double highLimit) {
        this.highLimit = inRange(highLimit, 0.0, 1.0);
    }

    public LimitInterpretation getLimitInterpretation() {
        return limitInterpretation;
    }

    public void setLimitInterpretation(LimitInterpretation limitInterpretation) {
        this.limitInterpretation = nonNull(limitInterpretation);
    }

    public double[] getFillerColor() {
        return fillerColor.clone();
    }

    public void setFillerColor(double[] fillerColor) {
        nonNull(fillerColor);
        this.fillerColor = new double[4];
        System.arraycopy(fillerColor, 0, this.fillerColor, 0, Math.min(fillerColor.length, 3));
    }

    public void setFillerColor(String fillerColor) {
        nonNull(fillerColor);
        setFillerColor(ChannelOperation.decodeRGBA(fillerColor));
    }

    @Override
    protected Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m) {
        final Pattern pattern = getPattern(m);
        if (currentChannel() == 0) {
            logDebug(() -> "Truncated mean"
                    + " in " + lowLimit + ".." + highLimit
                    + (limitInterpretation == LimitInterpretation.NORMALIZED_0_1_PERCENTILE ? "percentile" : "value")
                    + " range"
                    + rankMorphologyLogMessage()
                    + " with " + pattern
                    + (continuationMode == null ? "" : ", " + continuationMode)
                    + " for " + sourceMultiMatrix());
        }
        double scale = m.array().maxPossibleValue(1.0);
        double filler = scale * colorChannel(fillerColor);
        final RankMorphology morphology = createRankMorphology(m.elementType(), 1.0);
        switch (limitInterpretation) {
            case NORMALIZED_0_1_PERCENTILE: {
                double fromPercentile = lowLimit * (double) pattern.pointCount();
                double toPercentile = highLimit * (double) pattern.pointCount();
                return morphology.meanBetweenPercentiles(m, fromPercentile, toPercentile, pattern, filler);
            }
            case NORMALIZED_0_1_VALUE: {
                Matrix<? extends PArray> minV = Matrices.constantMatrix(lowLimit * scale,
                        DoubleArray.class, m.dimensions());
                Matrix<? extends PArray> maxV = Matrices.constantMatrix(highLimit * scale,
                        DoubleArray.class, m.dimensions());
                return morphology.meanBetweenValues(m, minV, maxV, pattern, filler);
            }
            default: {
                throw new AssertionError("Unknown limitInterpretation");
            }
        }
    }
}
