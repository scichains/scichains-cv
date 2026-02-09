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

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;
import net.algart.multimatrix.MultiMatrix;

import java.util.Locale;

public final class StrictMorphology extends MorphologyFilter {
    public static final String INPUT_MASK = "mask";

    private MorphologyOperation operation = MorphologyOperation.DILATION;
    private boolean optimizeSpeedForLargePatterns = true;
    private boolean invertSource = false;

    private Pattern pattern;

    public StrictMorphology() {
        addInputMat(INPUT_MASK);
    }

    public final MorphologyOperation getOperation() {
        return operation;
    }

    public StrictMorphology setOperation(MorphologyOperation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public boolean isOptimizeSpeedForLargePatterns() {
        return optimizeSpeedForLargePatterns;
    }

    public StrictMorphology setOptimizeSpeedForLargePatterns(boolean optimizeSpeedForLargePatterns) {
        this.optimizeSpeedForLargePatterns = optimizeSpeedForLargePatterns;
        return this;
    }

    public boolean isInvertSource() {
        return invertSource;
    }

    public StrictMorphology setInvertSource(boolean invertSource) {
        this.invertSource = invertSource;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        final MultiMatrix mask = getInputMat(INPUT_MASK, true).toMultiMatrix();
        long t1 = debugTime();
        MultiMatrix result = super.process(source);
        long t2 = debugTime();
        if (mask != null) {
            result = result.min(mask);
        }
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US, "Strict %s with %s%s for %s: "
                        + "%.3f ms morphology operation + %.3f ms masking ",
                operation, pattern,
                continuationMode == null ? "" : ", " + continuationMode,
                sourceMultiMatrix(),
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6));
        return result;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        if (invertSource) {
            m = Matrices.asFuncMatrix(
                            LinearFunc.getInstance(m.maxPossibleValue(), -1.0), m.type(PArray.class), m)
                    .clone();
        }
        this.pattern = getPattern(m, optimizeSpeedForLargePatterns);
        return operation.perform(createMorphology(m), m, pattern, this);
    }
}
