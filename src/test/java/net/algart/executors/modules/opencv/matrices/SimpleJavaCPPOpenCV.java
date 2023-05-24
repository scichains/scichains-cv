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

import net.algart.executors.modules.util.opencv.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.UMat;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class SimpleJavaCPPOpenCV {
    private static final int KERNEL_SIZE = 119;
    private static final int N = 1;

    public static void main(String[] args) throws Exception {
        System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
        System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
        System.out.println("OpenCL enabled: " + OTools.isGPUOptimizationEnabled());
        if (args.length == 0) {
            System.out.printf("Usage: %s source_image%n", SimpleJavaCPPOpenCV.class.getName());
            return;
        }
        final String sourceFile = args[0];
        if (!Files.exists(Paths.get(sourceFile))) {
            throw new FileNotFoundException(sourceFile);
        }
        for (int testCount = 0; testCount < 10; testCount++) {
            final Mat mat = opencv_imgcodecs.imread(sourceFile);
            opencv_imgproc.resize(mat, mat, new Size(2000, 2000));
            System.out.println("Test #" + testCount);
            final Mat result = mat.clone();
            final Size ksize = new Size(KERNEL_SIZE, KERNEL_SIZE);
            opencv_imgproc.GaussianBlur(mat, result, ksize, 0.0); // - warming
            long t1 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                opencv_imgproc.GaussianBlur(mat, result, ksize, 0.0);
            }
            long t2 = System.nanoTime();
            System.out.printf("Mat %s (at 0x%x) blurred by %dx%d in %.3f ms%n",
                    mat, mat.address(), ksize.width(), ksize.height(), (t2 - t1) * 1e-6 / N);

//            final opencv_core.GpuMat gpuMat = new opencv_core.GpuMat(mat.rows(), mat.cols(), mat.type());
//            mat.copyTo(gpuMat);
//            opencv_cudaimgproc.bilateralFilter(gpuMat, gpuMat, 7, 32.0f, 5.0f);
            // - does not work yet
            UMat umat = mat.getUMat(opencv_core.ACCESS_RW);
            UMat uresult = umat.clone();
            // opencv_photo.nonLocalMeans(umat, uresult, 0, 15, 7, opencv_core.BORDER_DEFAULT, null); // - doesn't work
            opencv_imgproc.GaussianBlur(uresult, uresult, ksize, 0.0); // - warming
            t1 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                opencv_imgproc.GaussianBlur(umat, uresult, ksize, 0.0);
            }
            t2 = System.nanoTime();
            System.out.printf("UMat %s (at 0x%x) blurred by %dx%d in %.3f ms%n",
                    umat, umat.address(), ksize.width(), ksize.height(), (t2 - t1) * 1e-6 / N);
            opencv_imgcodecs.imwrite(sourceFile + ".javacpp.ublur.png", OTools.toMat(uresult));

            t1 = System.nanoTime();
            UMat umatClone = umat.clone();
            t2 = System.nanoTime();
            System.out.printf("UMat %s (at 0x%x) is cloned in %.3f ms%n",
                    umatClone, umatClone.address(), (t2 - t1) * 1e-6);

            opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.COLOR_BGR2GRAY);
            t1 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                opencv_imgproc.GaussianBlur(mat, result, ksize, 0.0);
            }
            t2 = System.nanoTime();
            System.out.printf("Mat %s (at 0x%x) blurred by %dx%d in %.3f ms%n",
                    mat, mat.address(), ksize.width(), ksize.height(), (t2 - t1) * 1e-6 / N);

            umat = mat.getUMat(opencv_core.ACCESS_RW);
            uresult = umat.clone();
            t1 = System.nanoTime();
            for (int k = 0; k < N; k++) {
                opencv_imgproc.GaussianBlur(umat, uresult, ksize, 0.0);
            }
            t2 = System.nanoTime();
            System.out.printf("UMat %s (at 0x%x) blurred by %dx%d in %.3f ms%n",
                    umat, umat.address(), ksize.width(), ksize.height(), (t2 - t1) * 1e-6 / N);
            opencv_imgcodecs.imwrite(sourceFile + ".javacpp.ublur.gray.png", uresult.getMat(opencv_core.ACCESS_RW));
            System.err.println("Start gc...");
            for (int k = 0; k < 5; k++) {
                System.gc();
                // - may leads to error while incorrect coding
            }
            System.err.println("End gc");
//            uresult.close();
//            umat.close();
//            result.close();
//            mat.close();
        }
    }
}
