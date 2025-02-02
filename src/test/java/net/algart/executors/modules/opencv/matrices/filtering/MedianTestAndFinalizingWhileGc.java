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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.ShortArray;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.io.MatrixIO;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class MedianTestAndFinalizingWhileGc {
    private static void test(Path sourceFile, Path targetFile) throws IOException {
        final List<Matrix<? extends PArray>> image = new ArrayList<>(MatrixIO.readImage(sourceFile));
        for (int i = 0; i < image.size(); i++) {
            Matrix<? extends PArray> m = image.get(i);
            m = Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, 65535.0 / 255.0), ShortArray.class, m);
            image.set(i, m);
        }
        final Median median = new Median();
        median.setKernelSize(5);
        median.getInputMatContainer().setTo(MultiMatrix.of2DRGBA(image));
        median.execute();
        final Mat resMat = O2SMat.toMat(median.getMat());
        opencv_imgcodecs.imwrite(targetFile.toString(), resMat);
        // System.out.println(Jsons.toPrettyString(Jsons.toJson(median.visibleResultsMetaInformation())));

        /* // very strange bahaviour
        Mat source = new Mat(3, new int[] {100, 200, 300}, opencv_core.CV_8U);
        final Mat target = PortConversions.newCompatibleMat(source);
        System.out.println(source + "," + source.size() + "; " + target + "," + target.size());
        final Size size = source.size();
        for (int k = 0; k < source.dims(); k++) {
            System.out.println("size[" + k + "]=" + size.get(k) + "!!");
        }
        opencv_imgproc.medianBlur(source, target, 3);
        */

        // median.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image target_image%n", Median.class.getName());
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final Path targetFile = Paths.get(args[1]);
        test(sourceFile, targetFile);
        System.gc();
        Thread.sleep(100);
        System.gc();
        System.gc();
        // - should show warning without explicit median.close()
    }
}
