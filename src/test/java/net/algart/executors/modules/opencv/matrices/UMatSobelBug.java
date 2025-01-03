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

package net.algart.executors.modules.opencv.matrices;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

// See https://github.com/bytedeco/javacpp-presets/issues/645
public final class UMatSobelBug {
    public static void main(String[] args) {
        opencv_core.setUseOpenCL(true);
        if (args.length < 3) {
            System.out.printf("Usage: %s source_image target_mat target_umat%n", UMatSobelBug.class.getName());
            return;
        }
        final String sourceFile = args[0];
        final String targetMat = args[1];
        final String targetUMat = args[2];
        final Mat source = opencv_imgcodecs.imread(sourceFile);
        opencv_imgproc.GaussianBlur(source, source, new Size(55, 55), 0.0);

        Mat mat = new Mat();
        opencv_imgproc.Sobel(source, mat, opencv_core.CV_8U,
                1, 1, 3, 10.0, 128, opencv_core.BORDER_DEFAULT);
        opencv_imgcodecs.imwrite(targetMat, mat);

        UMat uMat = new UMat();
        opencv_imgproc.Sobel(source.getUMat(opencv_core.ACCESS_RW), uMat, opencv_core.CV_8U,
                1, 1, 3, 10.0, 128, opencv_core.BORDER_DEFAULT);
        opencv_imgcodecs.imwrite(targetUMat, uMat);
    }
}
