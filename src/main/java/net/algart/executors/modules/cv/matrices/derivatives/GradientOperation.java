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

package net.algart.executors.modules.cv.matrices.derivatives;

import static net.algart.executors.modules.cv.matrices.derivatives.DerivativeOperation.*;

public enum GradientOperation {
    SIMPLE_PAIR(DX_PAIR, DY_PAIR),
    SIMPLE(DX, DY),
    ROBERTS(DX_PAIR, DY_PAIR, SCALED_ROBERTS_CROSS_QUADRANT_1, SCALED_ROBERTS_CROSS_QUADRANT_2),
    SOBEL(SOBEL_X, SOBEL_Y),
    SCALED_SOBEL(SCALED_SOBEL_X, SCALED_SOBEL_Y),
    SCHARR(SCHARR_X, SCHARR_Y),
    SCALED_SCHARR(SCALED_SCHARR_X, SCALED_SCHARR_Y);

    private final DerivativeOperation[] derivatives;

    GradientOperation(DerivativeOperation... derivatives) {
        assert derivatives.length >= 2;
        this.derivatives = derivatives;
    }

    public DerivativeOperation[] derivatives() {
        return derivatives.clone();
    }

    public DerivativeOperation dxOperation() {
        return derivatives[0];
    }

    public DerivativeOperation dyOperation() {
        return derivatives[1];
    }
}
