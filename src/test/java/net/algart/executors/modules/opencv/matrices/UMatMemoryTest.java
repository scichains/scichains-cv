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

package net.algart.executors.modules.opencv.matrices;

import net.algart.executors.api.Executor;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.UMat;

import java.util.ArrayList;
import java.util.List;

public final class UMatMemoryTest {
    private static final Size SIZE = new Size(2000, 2000);

    public static void main(String[] args) throws InterruptedException {
        opencv_core.setUseOpenCL(true);
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image number_of_copies%n", UMatMemoryTest.class.getName());
            return;
        }
        final String sourceFile = args[0];
        final int n = Integer.parseInt(args[1]);
        final Mat source = opencv_imgcodecs.imread(sourceFile);
        List<Object> list = new ArrayList<>();
        Thread.sleep(1000);
        for (int k = 1; k <= n; k++) {
            UMat uMat = source.getUMat(opencv_core.ACCESS_RW);
            UMat uResized = new UMat();
            opencv_imgproc.resize(uMat, uResized, SIZE);
//            opencv_imgcodecs.imwrite(sourceFile + ".resized." + k + ".bmp", uResized);
            // list.add(OTools.toMatAndClose(uResized.clone())); // also use GPU memory, but temporary
            // list.add(OTools.toMatAndClose(uResized));
            list.add(uResized);
            uMat.close();
            System.out.printf("%d/%d: %s", k, n, Executor.Timing.memoryInfo());
            Thread.sleep(50);
        }
    }
}
