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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public interface BoundariesExtractor {
    BoundariesExtractor DEFAULT = (boundaries, labels, needThick) -> {
        if (labels.depth() == opencv_core.CV_8U || labels.depth() == opencv_core.CV_32F) {
            labels.copyTo(boundaries);
        } else {
            labels.convertTo(boundaries, opencv_core.CV_32F);
            // - morphologyEx does not support 32S in some compiled DLLs
        }
        if (boundaries.channels() > 1) {
            opencv_imgproc.cvtColor(boundaries, boundaries, opencv_imgproc.CV_BGR2GRAY);
        }
        final int shape = needThick ? opencv_imgproc.CV_SHAPE_RECT : opencv_imgproc.CV_SHAPE_CROSS;
        OTools.morphology(boundaries, opencv_imgproc.MORPH_GRADIENT, shape, 3);
        opencv_imgproc.threshold(boundaries, boundaries,
            0, 255, opencv_imgproc.CV_THRESH_BINARY);
        boundaries.convertTo(boundaries, opencv_core.CV_8U);
    };

    void findBoundaries(Mat boundaries, Mat labels, boolean needThick);
}
