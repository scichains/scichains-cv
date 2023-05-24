/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.util.opencv.enums;

import org.bytedeco.opencv.global.opencv_imgproc;

public enum ODistanceType {
    DIST_L1(opencv_imgproc.CV_DIST_L1),
    DIST_L2(opencv_imgproc.CV_DIST_L2),
    DIST_C(opencv_imgproc.CV_DIST_C),
    DIST_L12(opencv_imgproc.CV_DIST_L12),
    DIST_FAIR(opencv_imgproc.CV_DIST_FAIR),
    DIST_WELSCH(opencv_imgproc.CV_DIST_WELSCH),
    DIST_HUBER(opencv_imgproc.CV_DIST_HUBER);

    private final int code;

    public int code() {
        return code;
    }

    ODistanceType(int code) {
        this.code = code;
    }
}