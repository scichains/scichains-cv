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

public enum OColorMap {
    COLORMAP_AUTUMN(opencv_imgproc.COLORMAP_AUTUMN),
    COLORMAP_BONE(opencv_imgproc.COLORMAP_BONE),
    COLORMAP_JET(opencv_imgproc.COLORMAP_JET),
    COLORMAP_WINTER(opencv_imgproc.COLORMAP_WINTER),
    COLORMAP_RAINBOW(opencv_imgproc.COLORMAP_RAINBOW),
    COLORMAP_OCEAN(opencv_imgproc.COLORMAP_OCEAN),
    COLORMAP_SUMMER(opencv_imgproc.COLORMAP_SUMMER),
    COLORMAP_SPRING(opencv_imgproc.COLORMAP_SPRING),
    COLORMAP_COOL(opencv_imgproc.COLORMAP_COOL),
    COLORMAP_HSV(opencv_imgproc.COLORMAP_HSV),
    COLORMAP_PINK(opencv_imgproc.COLORMAP_PINK),
    COLORMAP_HOT(opencv_imgproc.COLORMAP_HOT),
    COLORMAP_PARULA(opencv_imgproc.COLORMAP_PARULA);

    private final int code;

    public int code() {
        return code;
    }

    OColorMap(int code) {
        this.code = code;
    }
}
