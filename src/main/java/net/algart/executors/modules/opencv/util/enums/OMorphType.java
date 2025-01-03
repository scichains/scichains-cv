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

package net.algart.executors.modules.opencv.util.enums;

import org.bytedeco.opencv.global.opencv_imgproc;

public enum OMorphType {
    ERODE(opencv_imgproc.MORPH_ERODE),
    DILATE(opencv_imgproc.MORPH_DILATE),
    OPEN(opencv_imgproc.MORPH_OPEN),
    CLOSE(opencv_imgproc.MORPH_CLOSE),
    GRADIENT(opencv_imgproc.MORPH_GRADIENT),
    TOPHAT(opencv_imgproc.MORPH_TOPHAT),
    BLACKHAT(opencv_imgproc.MORPH_BLACKHAT),
    HITMISS(opencv_imgproc.MORPH_HITMISS);

    private final int code;

    public int code() {
        return code;
    }

    OMorphType(int code) {
        this.code = code;
    }
}
