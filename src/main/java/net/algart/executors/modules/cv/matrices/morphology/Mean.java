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

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.RankMorphology;

public final class Mean extends RankMorphologyFilter {
    @Override
    protected Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m) {
        final Pattern pattern = getPattern(m);
        if (currentChannel() == 0) {
            logDebug(() -> "Mean"
                    + rankMorphologyLogMessage()
                    + " with " + pattern
                    + (continuationMode == null ? "" : ", " + continuationMode)
                    + " for " + sourceMultiMatrix());
        }
        final RankMorphology morphology = createRankMorphology(m.elementType(), 1.0);
        return morphology.mean(m, pattern);
    }
}
