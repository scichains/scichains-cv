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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.filters3x3.PercentileBySquare3x3;
import net.algart.matrices.morphology.RankMorphology;

import java.util.Locale;

public final class Percentile extends RankMorphologyFilter {
    public enum ValueInterpretation {
        ABSOLUTE_INDEX,
        NORMALIZED_0_1;
    }

    private double percentile = 0.9;
    private ValueInterpretation valueInterpretation = ValueInterpretation.NORMALIZED_0_1;

    public double getPercentile() {
        return percentile;
    }

    public Percentile setPercentile(double percentile) {
        this.percentile = percentile;
        return this;
    }

    public ValueInterpretation getValueInterpretation() {
        return valueInterpretation;
    }

    public Percentile setValueInterpretation(ValueInterpretation valueInterpretation) {
        this.valueInterpretation = nonNull(valueInterpretation);
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m) {
        final Pattern pattern = getPattern(m);
        final double percentileIndex = valueInterpretation == ValueInterpretation.ABSOLUTE_INDEX ?
                percentile :
                percentile * (double) (pattern.pointCount() - 1);
        long t1 = debugTime(), t2;
        final Matrix<? extends UpdatablePArray> result;
        if (getShape() == Shape.CUBE
                && getPatternSize() == 3
                && continuationMode == Matrix.ContinuationMode.CYCLIC
                && percentileIndex >= 0
                && percentileIndex < pattern.pointCount()) {
            // - for illegal percentileIndex rankMorphology provides another behaviour (does not check it)
            final PercentileBySquare3x3 percentile3x3 = PercentileBySquare3x3.newInstance(
                    m.elementType(), m.dimensions(), (int) percentileIndex);
            t2 = debugTime();
            result = percentile3x3.filter(m);
        } else {
            final RankMorphology rankMorphology = createRankMorphology(m.elementType(), 1.0);
            t2 = debugTime();
            result = rankMorphology.percentile(m, percentileIndex, pattern);
        }
        long t3 = debugTime();
        if (currentChannel() == 0) {
            logDebug(() -> String.format(Locale.US,
                    "Percentile (index %.1f)%s with %s%s for %s: %.3f initializing + %.3f processing",
                    percentileIndex,
                    rankMorphologyLogMessage(),
                    pattern,
                    continuationMode == null ? "" : ", " + continuationMode,
                    sourceMultiMatrix(),
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        }
        return result;
    }
}
