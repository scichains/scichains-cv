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

package net.algart.executors.modules.cv.matrices.derivatives;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.IPoint;
import net.algart.math.patterns.Patterns;
import net.algart.math.patterns.WeightedPattern;
import net.algart.math.patterns.WeightedPatterns;
import net.algart.matrices.linearfiltering.Convolution;

public enum DerivativeOperation {
    // First derivatives:
    DX_PAIR(0, 0, 1, 0, new double[]{1, -1}),
    DY_PAIR(0, 0, 0, 1, new double[]{1, -1}),
    DX(-1, 0, 1, 0, new double[]{0.5, 0, -0.5}),
    DY(0, -1, 0, 1, new double[]{0.5, 0, -0.5}),
    SOBEL_X(-1, -1, 1, 1, new double[]{
            +1, 0, -1,
            +2, 0, -2,
            +1, 0, -1
    }),
    SOBEL_Y(-1, -1, 1, 1, new double[]{
            +1, +2, +1,
            0, 0, 0,
            -1, -2, -1
    }),
    SCALED_SOBEL_X(-1, -1, 1, 1, new double[]{
            +1 / 8.0, 0, -1 / 8.0,
            +2 / 8.0, 0, -2 / 8.0,
            +1 / 8.0, 0, -1 / 8.0
    }),
    SCALED_SOBEL_Y(-1, -1, 1, 1, new double[]{
            +1 / 8.0, +2 / 8.0, +1 / 8.0,
            0, 0, 0,
            -1 / 8.0, -2 / 8.0, -1 / 8.0
    }),
    SCHARR_X(-1, -1, 1, 1, new double[]{
            +3, 0, -3,
            +10, 0, -10,
            +3, 0, -3
    }),
    SCHARR_Y(-1, -1, 1, 1, new double[]{
            +3, +10, +3,
            0, 0, 0,
            -3, -10, -3
    }),
    SCALED_SCHARR_X(-1, -1, 1, 1, new double[]{
            +3 / 32.0, 0, -3 / 32.0,
            +10 / 32.0, 0, -10 / 32.0,
            +3 / 32.0, 0, -3 / 32.0
    }),
    SCALED_SCHARR_Y(-1, -1, 1, 1, new double[]{
            +3 / 32.0, +10 / 32.0, +3 / 32.0,
            0, 0, 0,
            -3 / 32.0, -10 / 32.0, -3 / 32.0
    }),
    ROBERTS_CROSS_QUADRANT_1(0, 0, 1, 1, new double[]{
            +1, 0,
            0, -1
    }),
    ROBERTS_CROSS_QUADRANT_2(0, 0, 1, 1, new double[]{
            0, +1,
            -1, 0
    }),
    SCALED_ROBERTS_CROSS_QUADRANT_1(0, 0, 1, 1, new double[]{
            +1.0 / StrictMath.sqrt(2.0), 0,
            0, -1.0 / StrictMath.sqrt(2.0)
    }),
    SCALED_ROBERTS_CROSS_QUADRANT_2(0, 0, 1, 1, new double[]{
            0, +1.0 / StrictMath.sqrt(2.0),
            -1.0 / StrictMath.sqrt(2.0), 0
    }),
    // Second derivatives:
    D2_DX2(-1, 0, 1, 0, new double[]{
            1, -2, 1
            // So, if m[x,y] = x^2, we will have d2/dx2 = (x+1)+(x-1)-2x=2: correct value of 2nd derivative
    }),
    D2_DY2(0, -1, 0, 1, new double[]{
            1, -2, 1
    }),
    D2_DXDY(-1, -1, 1, 1, new double[]{
            0.25, 0.0, -0.25,
            0.0, 0.0, 0.0,
            -0.25, 0.0, 0.25
            // a1 b1 c1          (c1-b1 + b1-a1)/2 = (c1-a1)/2 = v1
            // a2 b2 c2   d/dx = (c2-b2 + b2-a2)/2 = (c2-a2)/2 = v2   d/dy = (v3-v2 + v2-v1)/2 = (v3-v1)/2
            // a3 b3 c3          (c2-b2 + b2-a2)/2 = (c3-a3)/2 = v3
            // If m[x,y] = (x+y)^2, we will have d2/dxdy = (x+y+2)^2/4 + (x+y-2)^2/4 - 2*(x+y)^2/4 = 2: also correct
    }),
    LAPLACIAN(-1, -1, 1, 1, new double[]{
            0, 1, 0,
            1, -4, 1,
            0, 1, 0
    });

    private final WeightedPattern weightedPattern;

    DerivativeOperation(int minX, int minY, int maxX, int maxY, double[] weights) {
        this.weightedPattern = WeightedPatterns.newPattern(
                Patterns.newRectangularIntegerPattern(IPoint.valueOf(minX, minY), IPoint.valueOf(maxX, maxY)),
                weights);
    }

    public <T extends PArray> Matrix<? extends T> process(
            Class<? extends T> requiredType,
            Convolution convolution,
            Matrix<? extends PArray> matrix) {
        return convolution.convolution(requiredType, matrix, weightedPattern);
    }
}
