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

package net.algart.executors.modules.cv.matrices.misc.extremums;

import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.executors.modules.cv.matrices.misc.SortedRound2DAperture;

class MaximumsFinderWithoutMask extends MaximumsFinder {
    MaximumsFinderWithoutMask(
            float[] values,
            SortedRound2DAperture aperture,
            DeepTestSettings deepTestSettings,
            Matrix<UpdatableBitArray> resultExtremums,
            boolean buildListOfExtremumsXY) {
        super(values, null, aperture, deepTestSettings, resultExtremums, buildListOfExtremumsXY);
    }

    @Override
    boolean horizontalCheck(int index0) {
        final float v0 = values[index0];
        for (int k = 1; k <= maxRadius; k++) {
            if (values[index0 - k] > v0 || values[index0 + k] > v0) {
                // So, line[index0+1] <= v0, line[index0+2] <= v0, ..., line[index0+k-1] <= v0.
                // They can be local maximum only if they ==v0!
                // So, we could increase index0 by k-1 (with checking of plateau).
                // But it does not lead to measurable optimization.
                return false;
            }
        }
        return true;
    }

    @Override
    void quickCheck(int index0) {
        final float v0 = values[index0];
        for (int k = 1; k <= maxRadius; k++) {
            if (values[index0 - k] > v0 || values[index0 + k] > v0) {
                return;
            }
        }
        // The analogous vertical loop does not help to optimize
        for (int k = 0; k < apertureCountWithoutLine; k++) {
            if (values[index0 - apertureOffsets[k]] > v0) {
                return;
            }
        }
        neighboursCount = apertureCount;
        processExtremum(index0, apertureOffsets);
    }

    @Override
    void detailedCheck(int index0) {
        final int x0 = index0 - valuesStart;
        final int y0 = this.y;
        final float v0 = values[index0];
        final int dimX = this.dimX;
        final int dimY = this.dimY;
        final int[] dx = aperture.dx();
        final int[] dy = aperture.dy();
        // - JVM works better with local variables, not fields of an object
        int count = 0;
        for (int k = 0; k < apertureCount; k++) {
            final int x = x0 - dx[k];
            final int y = y0 - dy[k];
            if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                final int offset = apertureOffsets[k];
                final int index = index0 - offset;
                if (values[index] > v0) {
                    return;
                }
                neighbourOffsets[count++] = offset;
            }
        }
        neighboursCount = count;
        processExtremum(index0, neighbourOffsets);
    }
}
