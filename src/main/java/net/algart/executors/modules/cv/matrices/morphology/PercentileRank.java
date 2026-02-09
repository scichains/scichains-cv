/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;

public final class PercentileRank extends RankMorphologyFilter {
    public enum ResultInterpretation {
        ABSOLUTE_INDEX,
        NORMALIZED_0_1;
    }

    private ResultInterpretation resultInterpretation = ResultInterpretation.NORMALIZED_0_1;

    public ResultInterpretation getResultInterpretation() {
        return resultInterpretation;
    }

    public void setResultInterpretation(ResultInterpretation resultInterpretation) {
        this.resultInterpretation = nonNull(resultInterpretation);
    }

    @Override
    protected Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m) {
        final Pattern pattern = getPattern(m);
        if (currentChannel() == 0) {
            logDebug(() -> "Percentile rank ("
                    + (resultInterpretation == ResultInterpretation.ABSOLUTE_INDEX ?
                    "31-bit index)" : "normalized float)")
                    + rankMorphologyLogMessage()
                    + " with " + pattern
                    + (continuationMode == null ? "" : ", " + continuationMode)
                    + " for " + sourceMultiMatrix());
        }
        final Matrix<? extends IntArray> rank = createRankMorphology(m.elementType(), 1.0)
                .rank(IntArray.class, m, m, pattern);
        return switch (resultInterpretation) {
            case ABSOLUTE_INDEX -> rank;
            case NORMALIZED_0_1 -> Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0, 1.0 / (double) pattern.pointCount()),
                    FloatArray.class,
                    rank);
            default -> throw new AssertionError("Unknown resultInterpretation");
        };
    }
}
