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

package net.algart.executors.modules.opencv.matrices;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class SimpleGaussianPlusCloneSpeed {
    private static final int KERNEL_SIZE = 111;

    // See https://github.com/opencv/opencv/issues/13648
    public static void main(String[] args) {
        System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
        System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
        final Size ksize = new Size(KERNEL_SIZE, KERNEL_SIZE);
        for (int testCount = 0; testCount < 16; testCount++) {
            System.out.printf("%nTest #%d%n", testCount);
            final UMat m = new UMat(1024, 1024,
                    opencv_core.CV_8UC3, new Scalar(0.0, 0.0, 0.0, 0.0));
            final UMat result = new UMat();
            long t1 = System.nanoTime();
            opencv_imgproc.GaussianBlur(m, result, ksize, 0.0);
            long t2 = System.nanoTime();
            final UMat clone = m.clone();// - slows down while increasing KERNEL_SIZE
            long t3 = System.nanoTime();
            System.out.printf("GaussianBlur: %.3f ms%nclone: %.3f ms%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
            clone.close();
            result.close();
            m.close();
        }
    }
}
