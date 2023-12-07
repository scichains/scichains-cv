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

package net.algart.executors.modules.opencv.util;

import net.algart.arrays.SizeMismatchException;
import net.algart.arrays.TooLargeArrayException;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.executors.api.SystemEnvironment;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.ChannelOperation;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_imgproc.Vec4fVector;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class OTools {
    private OTools() {
    }

    public static final String USE_GPU_PROPERTY_NAME = "net.algart.executors.modules.opencv.useGPU";

    private static final boolean GPU_OPTIMIZATION_ENABLED =
            SystemEnvironment.getBooleanProperty(USE_GPU_PROPERTY_NAME, true) && opencv_core.haveOpenCL();
    // - Note: we check haveOpenCL(), not useOpenCL(): the last method can return varying results
    // depending on the device status. See https://github.com/bytedeco/javacpp-presets/issues/659

    private static final Scalar zeroScalar = new Scalar(0.0, 0.0, 0.0, 0.0);

    public static boolean isGPUOptimizationEnabled() {
        return GPU_OPTIMIZATION_ENABLED;
    }

    public static String openCVVersion() {
        return opencv_core.getVersionMajor() + "." + opencv_core.getVersionMinor();
    }

    public static SMat.Depth depth(int openCVDepth) {
        return SMat.Depth.valueOf(openCVDepth);
    }

    public static SMat.Depth depth(Mat mat) {
        return SMat.Depth.valueOf(mat.depth());
    }

    public static SMat.Depth depth(UMat mat) {
        return SMat.Depth.valueOf(mat.depth());
    }

    public static Class<?> elementType(Mat mat) {
        return depth(mat).elementType();
    }

    public static Class<?> elementType(UMat mat) {
        return depth(mat).elementType();
    }

    public static double maxPossibleValue(int depth) {
        switch (depth) {
            case opencv_core.CV_8U:
                return 255;
            case opencv_core.CV_8S:
                return 127;
            case opencv_core.CV_16U:
                return 65535;
            case opencv_core.CV_16S:
                return 32767;
            case opencv_core.CV_32S:
                return Integer.MAX_VALUE;
            default:
                return 1.0;
        }
    }

    public static boolean isFloatingPoint(int depth) {
        return depth == opencv_core.CV_16F || depth == opencv_core.CV_32F || depth == opencv_core.CV_64F;
    }

    public static double maxPossibleValue(Mat mat) {
        Objects.requireNonNull(mat, "Null mat");
        return maxPossibleValue(mat.depth());
    }

    public static double maxPossibleValue(UMat mat) {
        Objects.requireNonNull(mat, "Null mat");
        return maxPossibleValue(mat.depth());
    }

    public static long sizeOfInBytes(Mat mat) {
        return mat.total() * mat.elemSize();
    }

    public static long sizeOfInBytes(UMat mat) {
        return mat.total() * mat.elemSize();
    }

    public static boolean isPackedBits(SMat m) {
        return m.getDepth() == SMat.Depth.BIT && m.getNumberOfChannels() == 1;
    }

    /**
     * Creates scalar with 3 components <tt>value</tt> and 4th zero. Don't use it in BGRA/RGBA context.
     *
     * @param value value of each component.
     * @return scalar.
     */
    public static Scalar scalarBGR(double value) {
        return new Scalar(value, value, value, 0.0);
    }

    public static Scalar scalarBGR(Color color, double maxValue) {
        return new Scalar(
                color.getBlue() / 255.0 * maxValue,
                color.getGreen() / 255.0 * maxValue,
                color.getRed() / 255.0 * maxValue,
                color.getAlpha() / 255.0 * maxValue);
    }

    public static Scalar scalarBGRA(String color, double maxValue) {
        final double[] rgba = ChannelOperation.decodeRGBA(color, maxValue);
        return new Scalar(rgba[2], rgba[1], rgba[0], rgba[3]);
    }

    public static Scalar scalarBGRA(double value, double alpha) {
        return new Scalar(value, value, value, alpha);
    }

    public static Point toPoint(IPoint point) {
        Objects.requireNonNull(point, "Null point");
        if (point.coordCount() != 2) {
            throw new IllegalArgumentException("point is not 2-dimensional: " + point);
        }
        final long x = point.x();
        final long y = point.y();
        if (x != (int) x || y != (int) y) {
            throw new IllegalArgumentException("point is too large and cannot be represented by 32 bits: "
                    + point);
        }
        return new Point((int) x, (int) y);
    }

    public static IPoint toIPoint(Point point) {
        return IPoint.valueOf(point.x(), point.y());
    }

    public static Rect toRect(IRectangularArea rectangularArea) {
        Objects.requireNonNull(rectangularArea, "Null rectangularArea");
        if (rectangularArea.coordCount() != 2) {
            throw new IllegalArgumentException("rectangularArea is not 2-dimensional: " + rectangularArea);
        }
        final long minX = rectangularArea.minX();
        final long minY = rectangularArea.minY();
        final long maxX = rectangularArea.maxX();
        final long maxY = rectangularArea.maxY();
        final long sizeX = rectangularArea.sizeX();
        final long sizeY = rectangularArea.sizeY();
        if (minX != (int) minX
                || minY != (int) minY
                || maxX != (int) maxX
                || maxY != (int) maxY
                || sizeX != (int) sizeX
                || sizeY != (int) sizeY) {
            throw new IllegalArgumentException("rectangularArea is too large and cannot be represented by 32 bits: "
                    + rectangularArea);
        }
        return new Rect((int) minX, (int) minY, (int) sizeX, (int) sizeY);
    }

    public static IRectangularArea toIRectangularArea(Rect rect) {
        final int minX = rect.x();
        final int minY = rect.y();
        final int maxX = minX + rect.width() - 1;
        final int maxY = minY + rect.height() - 1;
        return minX <= maxX && minY <= maxY ?
                IRectangularArea.valueOf(minX, minY, maxX, maxY) :
                null;
    }

    public static int[] toIntArray(Vec4iVector vector) {
        final long n = vector.size();
        if (n > Integer.MAX_VALUE / 4) {
            throw new TooLargeArrayException("Too large Vec4iVector");
        }
        int[] result = new int[4 * (int) n];
        for (int k = 0; k < n; k++) {
            try (Scalar4i scalar = vector.get(k)) {
                scalar.get(result, 4 * k, 4);
            }
        }
        return result;
    }

    public static float[] toFloatArray(Vec4fVector vector) {
        final long n = vector.size();
        if (n > Integer.MAX_VALUE / 4) {
            throw new TooLargeArrayException("Too large Vec4iVector");
        }
        float[] result = new float[4 * (int) n];
        for (int k = 0; k < n; k++) {
            try (Scalar4f scalar = vector.get(k)) {
                scalar.get(result, 4 * k, 4);
            }
        }
        return result;
    }

    public static Mat clone(Mat m) {
        final Mat clone = new Mat(m.rows(), m.cols(), m.type());
        m.copyTo(clone);
        return clone;
    }

    public static UMat clone(UMat u) {
        final UMat clone = new UMat(u.rows(), u.cols(), u.type());
        u.copyTo(clone);
        return clone;
    }

    public static Mat toMat(UMat u) {
        final Mat clone = new Mat(u.rows(), u.cols(), u.type());
        u.copyTo(clone);
//        System.out.printf("UUU Cloning %s to %s%n", toString(u), toString(clone));
        // Note: u.getMat would be a serious bug here: it can easily lead to JVM crash with message
        // "UMat deallocation error: some derived Mat is still alive".
        // See https://github.com/bytedeco/javacpp-presets/issues/644
        return clone;
    }

    public static UMat toUMat(Mat m) {
        final UMat clone = new UMat(m.rows(), m.cols(), m.type());
        m.copyTo(clone);
//        System.out.printf("OOO Cloning %s to %s%n", toString(m), toString(clone));
        // Note: m.getUMat is incorrect solution, because its result stay to be "connected" with source m:
        // changes in result of getUMat reflect to source Mat.
        return clone;
    }

    public static Mat newCompatibleMat(Mat mat) {
        return newCompatibleMat(mat, mat.type());
    }

    public static Mat newCompatibleMat(Mat mat, int newType) {
        try (final Size size = mat.size()) {
            return new Mat(size, newType);
        }
    }

    public static UMat newCompatibleMat(UMat mat) {
        return newCompatibleUMat(mat, mat.type());
    }

    public static UMat newCompatibleUMat(UMat mat, int newType) {
        try (final Size size = mat.size()) {
            return new UMat(size, newType);
        }
    }

    public static Mat newCompatibleZeros(Mat mat) {
        try (final Size size = mat.size()) {
            return new Mat(size, mat.type(), zeroScalar);
        }
    }

    public static UMat newCompatibleZeros(UMat mat) {
        try (final Size size = mat.size()) {
            return new UMat(size, mat.type(), zeroScalar);
        }
    }

    public static Mat constantMat8U(int dimX, int dimY, Color color) {
        try (Scalar scalar = scalarBGR(color, 255.0)) {
            return new Mat(dimY, dimX, opencv_core.CV_8UC3, scalar);
        }
    }

    public static Mat constantMonoMat8U(int dimX, int dimY, int filler) {
        try (Scalar scalar = new Scalar((double) filler)) {
            return new Mat(dimY, dimX, opencv_core.CV_8UC1, scalar);
        }
    }

    public static Mat to8UIfNot(Mat mat) {
        if (mat.depth() == opencv_core.CV_8U) {
            return mat;
        }
        Mat result = new Mat();
        mat.convertTo(result, opencv_core.CV_8U, 255.0 / maxPossibleValue(mat), 0);
        return result;
    }

    public static UMat to8UIfNot(UMat mat) {
        if (mat.depth() == opencv_core.CV_8U) {
            return mat;
        }
        UMat result = new UMat();
        mat.convertTo(result, opencv_core.CV_8U, 255.0 / maxPossibleValue(mat), 0);
        return result;
    }

    public static Mat to32FIfNot(Mat mat) {
        if (mat.depth() == opencv_core.CV_32F) {
            return mat;
        }
        Mat result = new Mat();
        mat.convertTo(result, opencv_core.CV_32F, 1.0 / maxPossibleValue(mat), 0);
        return result;
    }

    public static UMat to32FIfNot(UMat mat) {
        if (mat.depth() == opencv_core.CV_32F) {
            return mat;
        }
        UMat result = new UMat();
        mat.convertTo(result, opencv_core.CV_32F, 1.0 / maxPossibleValue(mat), 0);
        return result;
    }


    public static void make32FIfNot(Mat mat) {
        if (mat.depth() == opencv_core.CV_32F) {
            return;
        }
        mat.convertTo(mat, opencv_core.CV_32F, 1.0 / maxPossibleValue(mat), 0);
    }

    public static void make32FIfNot(UMat mat) {
        if (mat.depth() == opencv_core.CV_32F) {
            return;
        }
        mat.convertTo(mat, opencv_core.CV_32F, 1.0 / maxPossibleValue(mat), 0);
    }

    public static void makeMonoIfNot(Mat mat) {
        if (mat.channels() == 1) {
            return;
        }
        opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.CV_BGR2GRAY);
    }

    public static void makeMonoIfNot(UMat mat) {
        if (mat.channels() == 1) {
            return;
        }
        opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.CV_BGR2GRAY);
    }

    public static Mat toMonoIfNot(Mat mat) {
        if (mat.channels() <= 1) {
            return mat;
        }
        Mat result = new Mat();
        opencv_imgproc.cvtColor(mat, result, opencv_imgproc.CV_BGR2GRAY);
        return result;
    }

    public static UMat toMonoIfNot(UMat mat) {
        if (mat.channels() <= 1) {
            return mat;
        }
        UMat result = new UMat();
        opencv_imgproc.cvtColor(mat, result, opencv_imgproc.CV_BGR2GRAY);
        return result;
    }

    public static Mat toMono32FIfNot(Mat mat) {
        if (mat.channels() <= 1 && mat.depth() == opencv_core.CV_32F) {
            return mat;
        }
        Mat result = to32FIfNot(mat);
        if (result == mat) {
            return toMonoIfNot(mat);
        } else {
            if (mat.channels() > 1) {
                opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_BGR2GRAY);
                // - memory will be reallocated internally in the result matrix
            }
            return result;
        }
    }

    public static UMat toMono32FIfNot(UMat mat) {
        if (mat.channels() <= 1 && mat.depth() == opencv_core.CV_32F) {
            return mat;
        }
        UMat result = to32FIfNot(mat);
        if (result == mat) {
            return toMonoIfNot(mat);
        } else {
            if (mat.channels() > 1) {
                opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_BGR2GRAY);
                // - memory will be reallocated internally in the result matrix
            }
            return result;
        }
    }

    public static Mat toMono8UIfNot(Mat mat) {
        if (mat.channels() <= 1 && mat.depth() == opencv_core.CV_8U) {
            return mat;
        }
        Mat result = to8UIfNot(mat);
        if (result == mat) {
            return toMonoIfNot(mat);
        } else {
            if (mat.channels() > 1) {
                opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_BGR2GRAY);
                // - memory will be reallocated internally in the result matrix
            }
            return result;
        }
    }

    public static UMat toMono8UIfNot(UMat mat) {
        if (mat.channels() <= 1 && mat.depth() == opencv_core.CV_8U) {
            return mat;
        }
        UMat result = to8UIfNot(mat);
        if (result == mat) {
            return toMonoIfNot(mat);
        } else {
            if (mat.channels() > 1) {
                opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_BGR2GRAY);
                // - memory will be reallocated internally in the result matrix
            }
            return result;
        }
    }

    public static void closeFirstIfDiffersFromSecond(Mat mat, Mat referenceToCompare) {
        if (mat != referenceToCompare) {
            mat.close();
        }
    }

    public static void closeFirstIfDiffersFromSecond(UMat mat, UMat referenceToCompare) {
        if (mat != referenceToCompare) {
            mat.close();
        }
    }

    public static TermCriteria termCriteria(
            int terminationMaxCount,
            double terminationEpsilon,
            boolean nullAllowed) {
        if (terminationMaxCount != 0 || terminationEpsilon != 0.0) {
            int type = (terminationMaxCount != 0 ? TermCriteria.COUNT : 0)
                    + (terminationEpsilon != 0.0 ? TermCriteria.EPS : 0);
            return new TermCriteria(type, terminationMaxCount, terminationEpsilon);
        } else if (nullAllowed) {
            return null;
        } else {
            throw new IllegalArgumentException("Termination max count or epsilon must be non-zero");
        }
    }

    public static void flipRBChannels(Mat mat) {
        final int n = mat.channels();
        if (n == 3 || n == 4) {
            opencv_imgproc.cvtColor(mat, mat, n == 3 ? opencv_imgproc.COLOR_BGR2RGB : opencv_imgproc.COLOR_BGRA2RGBA);
        }
    }

    public static void flipRBChannels(UMat mat) {
        final int n = mat.channels();
        if (n == 3 || n == 4) {
            opencv_imgproc.cvtColor(mat, mat, n == 3 ? opencv_imgproc.COLOR_BGR2RGB : opencv_imgproc.COLOR_BGRA2RGBA);
        }
    }

    public static Mat drawBitMaskOnMat(Mat mat, Mat mask, Color color) {
        Mat result = new Mat();
        mat.convertTo(result, opencv_core.CV_8U, 255.0 / maxPossibleValue(mat), 0);
        if (result.channels() == 1) {
            opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_GRAY2BGR);
        } else if (result.channels() == 4) {
            opencv_imgproc.cvtColor(result, result, opencv_imgproc.CV_BGRA2BGR);
        }
        try (Mat constant = constantMat8U(mat.cols(), mat.rows(), color)) {
            final Mat mask8U = to8UIfNot(mask);
            constant.copyTo(result, mask8U);
            if (mask8U != mask) {
                mask8U.close();
            }
        }
        return result;
    }

    public static void drawBitMaskOnMatByWhite(Mat mat, Mat mask) {
        if (mat.depth() != mask.depth()) {
            mask.convertTo(mask,
                    mat.depth(),
                    maxPossibleValue(mat) / maxPossibleValue(mask),
                    0.0);
        }
        if (mask.channels() > 1) {
            opencv_imgproc.cvtColor(mask, mask, opencv_imgproc.CV_BGR2GRAY);
        }
        if (mat.channels() > 1) {
            opencv_imgproc.cvtColor(mask, mask, opencv_imgproc.CV_GRAY2BGR, mat.channels());
        }
        opencv_core.bitwise_or(mat, mask, mask);
    }

    public static void morphology(Mat mat, int op, int shape, int patternSize) {
        if (patternSize > 0) {
            try (Size size = new Size(patternSize, patternSize)) {
                try (Mat element = opencv_imgproc.getStructuringElement(shape, size)) {
                    opencv_imgproc.morphologyEx(mat, mat, op, element);
                }
            }
        }
    }

    public static void morphology(UMat mat, int op, int shape, int patternSize) {
        if (patternSize > 0) {
            try (Size size = new Size(patternSize, patternSize)) {
                try (Mat element = opencv_imgproc.getStructuringElement(shape, size)) {
                    try (UMat elementUMat = toUMat(element)) {
                        opencv_imgproc.morphologyEx(mat, mat, op, elementUMat);
                    }
                }
            }
        }
    }

    public static byte[] toByteArray(Mat m) {
        final ByteBuffer byteBuffer = asByteBuffer(m).duplicate();
        final byte[] result = new byte[byteBuffer.capacity()];
        byteBuffer.get(result);
        return result;
    }

    public static ByteBuffer toByteBuffer(Mat m) {
        return SMat.cloneByteBuffer(asByteBuffer(m));
    }

    public static ByteBuffer toByteBuffer(UMat u) {
        final long size = sizeOfInBytes(u);
        if (size > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot convert UMat to ByteBuffer: it's length " + size
                    + " > Integer.MAX_VALUE (" + u + ")");
        }
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) size);
        byteBuffer.order(ByteOrder.nativeOrder());
        final Mat m = asMat(u.cols(), u.rows(), u.type(), byteBuffer);
        u.copyTo(m);
        return byteBuffer;
    }

    public static Mat asMat(int width, int height, int type, ByteBuffer byteBuffer) {
        return new Mat(height, width, type, new BytePointer(byteBuffer.duplicate()));
    }

    public static Mat toMat(int width, int height, int type, ByteBuffer byteBuffer) {
        final Mat result = new Mat(height, width, type);
        byteBuffer = byteBuffer.duplicate();
        byteBuffer.position(0);
        asByteBuffer(result).put(byteBuffer);
        return result;
    }

    public static Mat toMat(int width, int height, int type, byte[] byteArray) {
        final Mat result = new Mat(height, width, type);
        asByteBuffer(result).put(byteArray);
        return result;
    }

    public static UMat toUMat(int width, int height, int type, ByteBuffer byteBuffer) {
        return toUMat(asMat(width, height, type, byteBuffer));
    }

    public static UMat toUMat(int width, int height, int type, byte[] byteArray) {
        return toUMat(width, height, type, ByteBuffer.wrap(byteArray));
    }

    public static void checkDimensionOfNonNullMatEquality(List<? extends Mat> matrices) {
        Objects.requireNonNull(matrices);
        Mat first = null;
        int firstIndex = -1;
        int index = -1;
        for (Mat m : matrices) {
            index++;
            if (m == null) {
                continue;
            }
            if (first == null) {
                first = m;
                firstIndex = index;
            } else if (m.cols() != first.cols() || m.rows() != first.rows()) {
                throw new SizeMismatchException("The OpenCV matrix #" + index + " and #" + firstIndex
                        + " dimensions mismatch: #" + index + " is " + m + ", #" + firstIndex + " is " + first);
            }
        }
    }

    public static void checkDimensionOfNonNullUMatEquality(List<? extends UMat> matrices) {
        Objects.requireNonNull(matrices);
        UMat first = null;
        int firstIndex = -1;
        int index = -1;
        for (UMat u : matrices) {
            index++;
            if (u == null) {
                continue;
            }
            if (first == null) {
                first = u;
                firstIndex = index;
            } else if (u.cols() != first.cols() || u.rows() != first.rows()) {
                throw new SizeMismatchException("The OpenCV matrix #" + index + " and #" + firstIndex
                        + " dimensions mismatch: #" + index + " is " + toString(u)
                        + ", #" + firstIndex + " is " + toString(first));
            }
        }
    }

    public static void checkDimensionOfNonNullMatEquality(Mat... m) {
        checkDimensionOfNonNullMatEquality(java.util.Arrays.asList(m));
    }

    public static void checkDimensionOfNonNullMatEquality(UMat... m) {
        checkDimensionOfNonNullUMatEquality(java.util.Arrays.asList(m));
    }

    // More informative than standard Mat.toString
    public static String toString(Mat mat) {
        return mat == null ?
                null :
                String.format(Locale.US, "%s (address 0x%x, %.2f MB)",
                        mat, mat.address(), sizeOfInBytes(mat) / 1048576.0);
    }

    // More informative than standard UMat.toString
    public static String toString(UMat mat) {
        return mat == null ?
                null :
                String.format(Locale.US, "UMat[%dbit %dx%dx%d]: %s (%.2f MB)",
                        8 * mat.elemSize1(), mat.channels(), mat.cols(), mat.rows(),
                        mat, sizeOfInBytes(mat) / 1048576.0);
    }

    public static String toString(TermCriteria termCriteria) {
        return termCriteria == null ?
                null :
                "termCriteria: type=" + termCriteria.type() + ", maxCount="
                        + termCriteria.maxCount() + ", epsilon=" + termCriteria.epsilon();
    }

    // Warning: it is a very dangerous function!
    // We should never return ByteBuffer, created by asByteBuffer, without cloning:
    // it is very dangerous. Maybe, we will store such buffer for long time, for example,
    // as a part of MultiMatrix (buffered memory model) or something else; when OpenCV
    // will free memory from this Mat, we will still use our reference and JVM will crash.
    private static ByteBuffer asByteBuffer(Mat m) {
        Objects.requireNonNull(m, "Null Mat");
        final long arraySize = m.arraySize();
        if (arraySize > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot convert Mat to ByteBuffer: it's length " + arraySize
                    + " > Integer.MAX_VALUE (" + m + ")");
        }
        return m.data().position(0).capacity(arraySize).asByteBuffer();
        // - note: m.asByteBuffer has absolutely another sense: it is a memory for storing Mat structure
    }
}
