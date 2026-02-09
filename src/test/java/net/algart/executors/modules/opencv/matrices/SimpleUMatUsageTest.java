/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;


public final class SimpleUMatUsageTest {
    private static final Size KSIZE = new Size(55, 55);

    public static Mat toMatAndClose(UMat u) {
        final Mat m = u.getMat(opencv_core.ACCESS_RW);
        try {
            return m.clone();
        } finally {
            m.close();
            u.close();
            // - this way is necessary to avoid error
            // OpenCV Error: Assertion failed (u->refcount == 0 &&
            //   "UMat deallocation error: some derived Mat is still alive")
            // while garbage collection (leading to crash of JVM)
        }
    }

    interface Tester {
        Mat test(Mat source);
    }

    final static Tester SIMPLE_CLONING = source -> {
        final UMat uMat = OTools.toUMat(source);
        printMat("source", source);
        opencv_imgproc.GaussianBlur(uMat, uMat, KSIZE, 0.0);
        printMat("uMat", uMat);
        final Mat result = OTools.toMat(uMat);
        printMat("result", result);
        // following code - for testing only:
        final UMat uMatClone = OTools.toUMat(result);
        printMat("uMat clone", uMatClone);
        uMatClone.close();
        return result;
    };

    final static Tester SIMPLE_CLOSING_DAMAGING_SOURCE = source -> {
        final UMat uMat = source.getUMat(opencv_core.ACCESS_RW);
        opencv_imgproc.GaussianBlur(uMat, uMat, KSIZE, 0.0);
        return toMatAndClose(uMat);
    };

    final static Tester THROUGH_BYTE_BUFFER = source -> {
        final UMat uMat = OTools.toUMat(source);
        printMat("source", source);
        opencv_imgproc.GaussianBlur(uMat, uMat, KSIZE, 0.0);
        printMat("uMat", uMat);
        final ByteBuffer byteBuffer = OTools.toByteBuffer(uMat);
        final Mat result = OTools.asMat(source.cols(), source.rows(), source.type(), byteBuffer);
        printMat("result", result);
        // following code - for testing only:
        final UMat uMatClone = OTools.toUMat(result);
        printMat("uMat clone", uMatClone);
        uMatClone.close();
        return result;
    };

    final static Tester LOW_LEVEL_CRASHING = source -> {
        final UMat uMat = source.getUMat(opencv_core.ACCESS_RW);
        opencv_imgproc.GaussianBlur(uMat, uMat, KSIZE, 0.0);
        final Mat result = uMat.getMat(opencv_core.ACCESS_RW);
//        result.close();
//        uMat.close();
        return result;
    };

    final static Tester LOW_LEVEL_IN_PLACE_ON_CLONE = source -> {
        final UMat uMat = source.getUMat(opencv_core.ACCESS_RW);
        printMat("source", source);
        final UMat uMatClone = uMat.clone();
        printMat("uMat", uMat);
        opencv_imgproc.GaussianBlur(uMatClone, uMatClone, KSIZE, 0.0);
        printMat("uMatClone", uMatClone);
        Mat mat = uMatClone.getMat(opencv_core.ACCESS_RW);
        printMat("source", source);
        printMat("mat", mat);
        Mat result = mat.clone();
        printMat("result", result);
        mat.close();
        uMatClone.close();
        printMat("uMat", uMat);
        printMat("uMatClone", uMatClone);
        return result;
    };

    final static Tester LOW_LEVEL_IN_OTHER = source -> {
        final UMat uMat = source.getUMat(opencv_core.ACCESS_RW);
        printMat("source", source);
        printMat("uMat", uMat);
        UMat uResult = new UMat();
        opencv_imgproc.GaussianBlur(uMat, uResult, KSIZE, 0.0);
        printMat("uMat", uResult);
        Mat mat = uResult.getMat(opencv_core.ACCESS_RW);
        printMat("source", source);
        printMat("mat", mat);
        Mat result = mat.clone();
        printMat("result", result);
        mat.release();
        uResult.release();
        printMat("uMat", uMat);
        return result;
    };

    private static void printMat(String name, Mat mat) {
        System.out.printf("%-9s %s, address 0x%x (%d*%d bytes)%n",
                name + ":", mat, mat.address(), mat.limit(), mat.sizeof());
    }

    private static void printMat(String name, UMat mat) {
        System.out.printf("%-9s %s, address 0x%x, buffer %s%n",
                name + ":",
                OTools.toString(mat),
                mat.address(), mat.asByteBuffer());
    }

    private static final Tester TESTER = THROUGH_BYTE_BUFFER;

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
//        opencv_core.setUseOpenCL(false);
        System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
        System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
        System.out.println();
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image target_image%n", SimpleUMatUsageTest.class.getName());
            return;
        }
        final String sourceFile = args[0];
        final String targetFile = args[1];
        if (!Files.exists(Paths.get(sourceFile))) {
            throw new FileNotFoundException(sourceFile);
        }
        final Mat source = opencv_imgcodecs.imread(sourceFile);
        System.out.printf("Reading source image %s from %s%n", OTools.toString(source), sourceFile);
        final Scalar zeroScalar = new Scalar(0.0, 0.0, 0.0, 0.0);

        for (int testCount = 0; testCount < 10; testCount++) {
            System.out.println("Performance test #" + testCount);
            final double m = OTools.sizeOfInBytes(source);
            long t1 = System.nanoTime();
            Mat newMat = new Mat(source.rows(), source.cols(), source.type());
            long t2 = System.nanoTime();
            UMat newUMat = new UMat(source.rows(), source.cols(), source.type());
            long t3 = System.nanoTime();
            newMat = new Mat(source.rows(), source.cols(), source.type(), zeroScalar);
            long t4 = System.nanoTime();
            newUMat = new UMat(source.rows(), source.cols(), source.type(), zeroScalar);
            long t5 = System.nanoTime();
            newMat = Mat.zeros(source.rows(), source.cols(), source.type()).asMat();
            long t6 = System.nanoTime();
            newUMat = UMat.zeros(source.rows(), source.cols(), source.type());
            long t7 = System.nanoTime();
            final Mat matClone = source.clone();
            long t8 = System.nanoTime();
            final UMat getUMat = matClone.getUMat(opencv_core.ACCESS_RW);
            long t9 = System.nanoTime();
            final UMat toUMat = OTools.toUMat(source);
            long t10 = System.nanoTime();
            final UMat uMatClone = getUMat.clone();
            long t11 = System.nanoTime();
            final UMat toUMatClone = toUMat.clone();
            long t12 = System.nanoTime();
            final Mat getMat = toUMat.getMat(opencv_core.ACCESS_RW);
            long t13 = System.nanoTime();
            final Mat toMat = OTools.toMat(toUMat);
            long t14 = System.nanoTime();
            final ByteBuffer byteBuffer = OTools.toByteBuffer(uMatClone);
            long t15 = System.nanoTime();
            final Mat toMatAndClose = toMatAndClose(uMatClone);
            long t16 = System.nanoTime();
            getMat.close(); // - necessary to avoid crash
            long t17 = System.nanoTime();
            toUMat.close();
            long t18 = System.nanoTime();
            opencv_core.finish();
            long t19 = System.nanoTime();
            System.out.printf("new Mat:              %.3f mcs, %.2f ns/byte%n", (t2 - t1) * 1e-3, (t2 - t1) / m);
            System.out.printf("new UMat:             %.3f mcs, %.2f ns/byte%n", (t3 - t2) * 1e-3, (t3 - t2) / m);
            System.out.printf("new zero Mat:         %.3f mcs, %.2f ns/byte%n", (t4 - t3) * 1e-3, (t4 - t3) / m);
            System.out.printf("new zero UMat:        %.3f mcs, %.2f ns/byte%n", (t5 - t4) * 1e-3, (t5 - t4) / m);
            System.out.printf("Mat.zeros():          %.3f mcs, %.2f ns/byte%n", (t6 - t5) * 1e-3, (t6 - t5) / m);
            System.out.printf("UMat.zeros():         %.3f mcs, %.2f ns/byte%n", (t7 - t6) * 1e-3, (t7 - t6) / m);
            System.out.printf("Mat.clone():          %.3f mcs, %.2f ns/byte%n", (t8 - t7) * 1e-3, (t8 - t7) / m);
            System.out.printf("Mat.getUMat():        %.3f mcs, %.2f ns/byte%n", (t9 - t8) * 1e-3, (t9 - t8) / m);
            System.out.printf("toUMat():             %.3f mcs, %.2f ns/byte%n", (t10 - t9) * 1e-3, (t10 - t9) / m);
            System.out.printf("(get)UMat.clone():    %.3f mcs, %.2f ns/byte%n", (t11 - t10) * 1e-3, (t11 - t10) / m);
            System.out.printf("(new)UMat.clone():    %.3f mcs, %.2f ns/byte%n", (t12 - t11) * 1e-3, (t12 - t11) / m);
            System.out.printf("UMat.getMat():        %.3f mcs, %.2f ns/byte%n", (t13 - t12) * 1e-3, (t13 - t12) / m);
            System.out.printf("toMat():              %.3f mcs, %.2f ns/byte%n", (t14 - t13) * 1e-3, (t14 - t13) / m);
            System.out.printf("toByteBuffer():       %.3f mcs, %.2f ns/byte%n", (t15 - t14) * 1e-3, (t15 - t14) / m);
            System.out.printf("toMatAndClose():      %.3f mcs, %.2f ns/byte%n", (t16 - t15) * 1e-3, (t16 - t15) / m);
            System.out.printf("Mat.close:()          %.3f mcs, %.2f ns/byte%n", (t17 - t16) * 1e-3, (t17 - t16) / m);
            System.out.printf("UMat.close():         %.3f mcs, %.2f ns/byte%n", (t18 - t17) * 1e-3, (t18 - t17) / m);
            System.out.printf("opencv_core.finish(): %.3f mcs, %.2f ns/byte%n", (t19 - t18) * 1e-3, (t19 - t18) / m);
            System.out.println();
        }

        for (int testCount = 0; testCount < 150; testCount++) {
            System.out.println("Test #" + testCount);

            final Mat result = TESTER.test(source);
            opencv_imgcodecs.imwrite(targetFile+".source.bmp", source);
            opencv_imgcodecs.imwrite(targetFile, result);
            System.err.println("Start gc...");
            for (int k = 0; k < 10; k++) {
                System.gc();
                Thread.sleep(5);
            }
            System.err.println("End gc");
        }
    }
}
