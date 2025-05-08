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

package net.algart.executors.modules.cv.matrices.filtering;

import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.matrices.filters3x3.AverageBySquare3x3;
import net.algart.multimatrix.MultiMatrix;

import java.util.Locale;

public final class Average3x3 extends MultiMatrixFilter {
    private boolean rounding = true;

    public Average3x3() {
    }

    public boolean isRounding() {
        return rounding;
    }

    public Average3x3 setRounding(boolean rounding) {
        this.rounding = rounding;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        long t1 = debugTime();
        final AverageBySquare3x3 average = AverageBySquare3x3.newInstance(
                source.elementType(), source.dimensions(), rounding);
        long t2 = debugTime();
        final MultiMatrix result = source.apply(average::filter);
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US, "3x3 average of %s calculated in %.3f ms: "
                        + "%.3f initializing, "
                        + "%.3f average",
                source,
                (t3 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        return result;
    }

}
