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
import org.bytedeco.opencv.opencv_core.*;

public final class MultitrehadingManyUMatProblem {

    public static UMat clone(UMat u) {
        try (Mat cloneInRAM = new Mat(u.rows(), u.cols(), u.type())) {
            u.copyTo(cloneInRAM);
            UMat result = new UMat(u.rows(), u.cols(), u.type());
            u.copyTo(result);
            return result;
        }
    }

    public static UMat cloneBad(UMat u) {
        UMat result = new UMat(u.rows(), u.cols(), u.type());
        u.copyTo(result);
        return result;
    }

    public static void main(String[] args) {
        System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
        System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
        System.out.println();
        final int n = Integer.parseInt(args[0]);
        for (int k = 1; k <= n; k++) {
            final int index = k;
            new Thread(() -> {
                System.out.println("Starting " + index + "/" + n + "...");
                final UMat mat = new UMat(2000, 2000, opencv_core.CV_8U,
                        new Scalar(0, 0, 0, 0));
                System.out.println("Cloning " + index + "/" + n + "...");
                final UMat clone = cloneBad(mat);
                // - mat.clone() and cloneBad(mat) are freezing in OpenCV 3.4.0-1.4
                System.out.println("Finishing " + index + "/" + n + "...");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                }
            }).start();
        }
    }
}
