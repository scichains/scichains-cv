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


import net.algart.arrays.*;
import net.algart.io.MatrixIO;
import net.algart.math.IPoint;
import net.algart.math.patterns.Patterns;
import net.algart.math.patterns.UniformGridPattern;
import net.algart.matrices.morphology.*;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;


/**
 * Created by Daniel on 03/07/2017.
 */
public final class SimpleMorphologyTest {
    private final Color testColor = Color.YELLOW;
    private final int dimX = 1001, dimY = 1001;
    private final int ptnRadius = 5;

    private final Path folder;
    private final ArrayContext context = ArrayContext.DEFAULT_SINGLE_THREAD;

    private SimpleMorphologyTest(Path folder) {
        this.folder = folder;
    }

    private Mat createMat() {
        final Mat mat = new Mat(dimY, dimX, opencv_core.CV_8UC3,
                new Scalar(testColor.getBlue(), testColor.getRed(), testColor.getRed(), 0));
        System.out.println("OpenCV Mat: " + mat);
        opencv_imgcodecs.imwrite(folder.resolve("simple.png").toString(), mat);
        return mat;
    }

    private void createTest(ByteBuffer result) {
        result.position(0);
        for (int i = 0, n = result.capacity() / 3; i < n; i++) {
            if (i < n / 2 ? i % 20 < 5 : i % 50 < 15) {
                int r = (byte) i * testColor.getRed() / 255;
                int g = (byte) i * testColor.getGreen() / 255;
                int b = (byte) i * testColor.getBlue() / 255;
                result.put((byte) b);
                result.put((byte) g);
                result.put((byte) r);
            } else {
                result.position(result.position() + 3);
            }
        }
    }

    private void testDilation(Mat grayMat) throws IOException {
        final Mat dilationMat = new Mat(dimY, dimX, opencv_core.CV_8UC1);
        final Matrix<? extends PArray> m = asMatrix(grayMat);
        final Morphology morphology = BasicMorphology.getInstance(context);
        System.out.println("OpenCV morphologyEx...");
        morphologyEx(grayMat, dilationMat, opencv_imgproc.MORPH_DILATE, 2 * ptnRadius + 1);
        morphologyEx(grayMat, dilationMat, opencv_imgproc.MORPH_DILATE, 2 * ptnRadius + 1);
        // warming up
        long t1 = System.nanoTime();
        morphologyEx(grayMat, dilationMat, opencv_imgproc.MORPH_DILATE, 2 * ptnRadius + 1);
        long t2 = System.nanoTime();
        System.out.printf("morphologyEx: %.3f ms%n", (t2 - t1) * 1e-6);
        final UniformGridPattern square = Patterns.newRectangularIntegerPattern(
                IPoint.valueOf(-ptnRadius, -ptnRadius), IPoint.valueOf(ptnRadius, ptnRadius));
        System.out.println("AlgART dilation...");
        morphology.dilation(m, square);
        morphology.dilation(m, square);
        // warming up
        t1 = System.nanoTime();
        final Matrix<? extends UpdatablePArray> dilationMatrix = morphology.dilation(m, square);
        t2 = System.nanoTime();
        System.out.printf("Morphology: %.3f ms%n", (t2 - t1) * 1e-6);
        opencv_imgcodecs.imwrite(folder.resolve("dilation" + ptnRadius + ".png").toString(), dilationMat);
        MatrixIO.writeImage(folder.resolve("algart_dilation" + ptnRadius + ".png"),
                Collections.singletonList(dilationMatrix));
    }

    private void testMedian(Mat grayMat) throws IOException {
        final Mat medianMat = new Mat(grayMat.rows(), grayMat.cols(), opencv_core.CV_8UC1);
        final Matrix<? extends PArray> m = asMatrix(grayMat);
        MatrixIO.writeImage(folder.resolve("algart_gray.png"), Collections.singletonList(m));
        final RankMorphology rankMorphology = BasicRankMorphology.getInstance(
                context, 0.5, RankPrecision.BITS_8);
        System.out.println("OpenCV median...");
        opencv_imgproc.medianBlur(grayMat, medianMat, 2 * ptnRadius + 1);
        opencv_imgproc.medianBlur(grayMat, medianMat, 2 * ptnRadius + 1);
        // warming up
        long t1 = System.nanoTime();
        opencv_imgproc.medianBlur(grayMat, medianMat, 2 * ptnRadius + 1);
        long t2 = System.nanoTime();
        System.out.printf("medianBlur: %.3f ms%n", (t2 - t1) * 1e-6);
        System.out.println("AlgART median...");
        final UniformGridPattern square = Patterns.newRectangularIntegerPattern(
                IPoint.valueOf(-ptnRadius, -ptnRadius), IPoint.valueOf(ptnRadius, ptnRadius));
        rankMorphology.dilation(m, square);
        rankMorphology.dilation(m, square);
        // warming up
        t1 = System.nanoTime();
        final Matrix<? extends UpdatablePArray> medianMatrix = rankMorphology.dilation(m, square);
        t2 = System.nanoTime();
        System.out.printf("RankMorphology: %.3f ms%n", (t2 - t1) * 1e-6);
        opencv_imgcodecs.imwrite(folder.resolve("median" + ptnRadius + ".png").toString(), medianMat);
        MatrixIO.writeImage(folder.resolve("algart_median" + ptnRadius + ".png"),
                Collections.singletonList(medianMatrix));
    }

    private static Matrix<? extends PArray> asMatrix(Mat m) {
        if (m.type() != opencv_core.CV_8UC1) {
            throw new IllegalArgumentException("Unsupported matrix type");
        }
        return Matrices.matrix(
                BufferMemoryModel.asUpdatableByteArray(m.data().capacity(m.arraySize()).asByteBuffer()),
                m.cols(), m.rows());
    }

    private static void morphologyEx(Mat src, Mat dst, int op, int size) {
        try (final Mat ptn = makeSquare(size)) {
            opencv_imgproc.morphologyEx(src, dst, op, ptn);
        }
    }

    private static Mat makeSquare(int size) {
        final byte[] buf = new byte[size * size];
        // - x=0..size-1, y=0..size-1
        JArrays.fill(buf, (byte) 1);
        final Mat ptn = new Mat(size, size, opencv_core.CV_8U);
        try (final BytePointer data = ptn.data()) {
            data.put(buf);
        }
        return ptn;
//        try (final Size size = new Size((int) size, (int) size)) {
//            return getStructuringElement(MORPH_ELLIPSE, size);
//        }
    }


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.printf("Usage: %s testFolder%n", SimpleMorphologyTest.class.getName());
            return;
        }
        final SimpleMorphologyTest test = new SimpleMorphologyTest(Paths.get(args[0]));
//        System.out.println("Welcome to OpenCV " + Core.VERSION);
        final Mat mat = test.createMat();
        test.createTest(mat.data().capacity(mat.arraySize()).asByteBuffer());
        opencv_imgcodecs.imwrite(test.folder.resolve("test.png").toString(), mat);
        final Mat grayMat = new Mat(mat.rows(), mat.cols(), opencv_core.CV_8UC1);
        opencv_imgproc.cvtColor(mat, grayMat, opencv_imgproc.COLOR_BGR2GRAY);

        test.testDilation(grayMat);
        test.testMedian(grayMat);
    }
}
