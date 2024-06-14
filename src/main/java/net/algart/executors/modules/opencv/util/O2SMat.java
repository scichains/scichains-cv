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

package net.algart.executors.modules.opencv.util;

import net.algart.arrays.*;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.data.ConvertibleByteBufferMatrix;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_ml;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class O2SMat {
    public static int ML_LAYOUT = opencv_ml.ROW_SAMPLE;

    private static final boolean OPTIMIZE_COPYING = true;

    static {
        final Class<?> dummy = OTools.class;
        // initialize OTools class
    }

    private O2SMat() {
    }

    public static Mat toMat(MultiMatrix2D multiMatrix) {
        if (multiMatrix.elementType() == boolean.class) {
            multiMatrix = multiMatrix.asPrecision(byte.class);
        }
        return toMat(SMat.valueOf(multiMatrix));
    }

    public static UMat toUMat(MultiMatrix2D multiMatrix) {
        if (multiMatrix.elementType() == boolean.class) {
            multiMatrix = multiMatrix.asPrecision(byte.class);
        }
        return toUMat(SMat.valueOf(multiMatrix));
    }

    public static MultiMatrix2D toMultiMatrix(Mat mat) {
        return setTo(new SMat(), mat, true).toMultiMatrix2D(true);
    }

    public static MultiMatrix2D toMultiMatrix(UMat mat) {
        return setTo(new SMat(), mat, true).toMultiMatrix2D(true);
    }

    public static MultiMatrix2D toBinaryMatrix(Mat mat) {
        return toMultiMatrix(mat).asMono().asPrecision(boolean.class).clone();
        // note: clone() necessary for usage in "try" section in the method
        // toBinaryMatrix(UMat mat)
    }

    public static MultiMatrix2D toBinaryMatrix(UMat mat) {
        try (Mat m = OTools.toMat(mat)) {
            return toBinaryMatrix(m);
        }
    }

    public static Mat toMat(SMat m) {
        // Usually it is better to throw an exception than to convert to byte with unpredictable behaviour and slowing
        return toMat(m, false);
    }

    public static Mat toMat(SMat m, boolean autoConvertPackedBits) {
        if (!m.isInitialized()) {
            return null;
        }
        final SMat.Convertible pointer = m.getPointer();
        if (pointer instanceof ConvertibleMat) {
//            System.out.println("OOO use cached " + pointer);
            return ((ConvertibleMat) pointer).mat();
        }
        m = prepareForOpenCV(m, autoConvertPackedBits);
//        System.out.println("Converting " + m + " to Mat");
        final int type = opencv_core.CV_MAKE_TYPE(m.getDepth().code(), m.getNumberOfChannels());

//        // This code is not efficient: if does not cache byteArray inside the pointer;
//        // in any case, it is useless for UMat
//        if (m.getDepth().elementType() == byte.class && pointer instanceof ConvertibleMultiMatrix) {
//            long t1 = System.nanoTime();
//            final byte[] byteArray = pointer.toByteArray(m);
//            long t2 = System.nanoTime();
//            final Mat result = OTools.toMat((int) m.getDimX(), (int) m.getDimY(), type, byteArray);
//            long t3 = System.nanoTime();
//            System.out.printf("/// %.3f + %.3f ms%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
//            return result;
//        }
//        long t1 = System.nanoTime();
        final ByteBuffer byteBuffer = m.getByteBuffer();
//        long t2 = System.nanoTime();
        final Mat result = OTools.toMat((int) m.getDimX(), (int) m.getDimY(), type, byteBuffer);
//        long t3 = System.nanoTime();
//        System.out.printf("???? %.3f + %.3f ms%n", (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
        return result;
        // - we MUST make extra copy: toMat instead of asMat, and return Mat, not connected with this byte buffer:
        // we don't know what and how created it, for example, it can be a storage of some AlgART matrix,
        // that must stay immutable
    }

    public static UMat toUMat(SMat m) {
        // Usually it is better to throw an exception than to convert to byte with unpredictable behaviour and slowing
        return toUMat(m, false);
    }

    public static UMat toUMat(SMat m, boolean autoConvertPackedBits) {
        if (!m.isInitialized()) {
            return null;
        }
        final SMat.Convertible pointer = m.getPointer();
        if (pointer instanceof ConvertibleUMat) {
//            System.out.println("OOO use cached " + pointer);
            return ((ConvertibleUMat) pointer).mat();
        }
        m = prepareForOpenCV(m, autoConvertPackedBits);
//        System.out.println("Converting " + m + " to Mat");
        final int type = opencv_core.CV_MAKE_TYPE(m.getDepth().code(), m.getNumberOfChannels());
        return OTools.toUMat((int) m.getDimX(), (int) m.getDimY(), type, m.getByteBuffer());
    }

    public static Mat toMat(Matrix<? extends PArray> packedChannels) {
        return toMat(packedChannels, false);
    }

    public static Mat toMat(Matrix<? extends PArray> packedChannels, boolean autoConvertPackedBits) {
        return toMat(SMat.valueOfPackedMatrix(packedChannels), autoConvertPackedBits);
    }

    public static UMat toUMat(Matrix<? extends PArray> packedChannels) {
        return toUMat(packedChannels, false);
    }

    public static UMat toUMat(Matrix<? extends PArray> packedChannels, boolean autoConvertPackedBits) {
        return toUMat(SMat.valueOfPackedMatrix(packedChannels), autoConvertPackedBits);
    }

    public static SMat toSMat(Mat mat) {
        return setTo(new SMat(), mat);
    }

    public static SMat toSMat(UMat mat) {
        return setTo(new SMat(), mat, false);
    }

    public static PArray toRawArray(Mat mat) {
        Objects.requireNonNull(mat, "Null mat");
        if (mat.rows() == 0 || mat.cols() == 0) {
            return Arrays.nPCopies(0, OTools.elementType(mat), 0.0);
        }
        return toSMat(mat).toPackedMatrix(true).array();
    }

    public static PArray toRawArray(UMat mat) {
        Objects.requireNonNull(mat, "Null mat");
        if (mat.rows() == 0 || mat.cols() == 0) {
            return Arrays.nPCopies(0, OTools.elementType(mat), 0.0);
        }
        return toSMat(mat).toPackedMatrix(true).array();
    }

    public static SNumbers toRawNumbers(Mat mat, int blockLength) {
        final PArray array = toRawArray(mat);
        if (array.isEmpty()) {
            return SNumbers.zeros(array.elementType(), 0, Math.max(blockLength, 1));
            // - we prefer to return empty 1-column array instead of throwing exception when blockLength=0
        }
        final Object javaArray = Arrays.toJavaArray(array);
        // - In current version, all element types, supported by Mat, are also supported by numbers array.
        // In any case, if it is not so, the next method will throw an exception.
        return SNumbers.valueOfArray(javaArray, blockLength);
    }

    public static SNumbers toRawNumbers(UMat mat, int blockLength) {
        final PArray array = toRawArray(mat);
        if (array.isEmpty()) {
            return SNumbers.zeros(array.elementType(), 0, Math.max(blockLength, 1));
            // - we prefer to return empty 1-column array instead of throwing exception when blockLength=0
        }
        final Object javaArray = Arrays.toJavaArray(array);
        // - In current version, all element types, supported by Mat, are also supported by numbers array.
        // In any case, if it is not so, the next method will throw an exception.
        return SNumbers.valueOfArray(javaArray, blockLength);
    }

    public static SMat setToOrRemove(SMat result, Mat mat) {
        if (mat == null || mat.cols() == 0 || mat.rows() == 0) {
            result.remove();
            return result;
        }
        return setTo(result, mat);
    }

    public static SMat setToOrRemove(SMat result, UMat mat) {
        if (mat == null || mat.cols() == 0 || mat.rows() == 0) {
            result.remove();
            return result;
        }
        return setTo(result, mat);
    }

    public static SMat setTo(SMat result, Mat mat) {
        return setTo(result, mat, false);
    }

    public static SMat setTo(SMat result, UMat mat) {
        return setTo(result, mat, false);
    }

    public static Mat matrixToMlSamplesOrResponsesMat(SMat values, boolean intResult) {
        return toMat(matrixToMlSamplesOrResponsesSMat(values, intResult));
    }

    public static UMat matrixToMlSamplesOrResponsesUMat(SMat values, boolean intResult) {
        return toUMat(matrixToMlSamplesOrResponsesSMat(values, intResult));
    }

    public static Mat numbersToMulticolumnMat(SNumbers values) {
        Objects.requireNonNull(values, "Null values");
        if (!values.isInitialized()) {
            throw new IllegalArgumentException("Not initialized values");
        }
        return toMat(Matrices.matrix(
                PArray.as(values.getArray()),
                1, // - 1 channel
                values.getBlockLength(),
                values.n()));
    }

    public static UMat numbersToMulticolumnUMat(SNumbers values) {
        Objects.requireNonNull(values, "Null values");
        if (!values.isInitialized()) {
            throw new IllegalArgumentException("Not initialized values");
        }
        return toUMat(Matrices.matrix(
                PArray.as(values.getArray()),
                1, // - 1 channel
                values.getBlockLength(),
                values.n()));
    }

    public static Mat numbersToMulticolumn32BitMat(SNumbers values, boolean intResult) {
        return toMat(numbersToMulticolumn32BitSMat(values, intResult));
    }

    public static UMat numbersToMulticolumn32BitUMat(SNumbers values, boolean intResult) {
        return toUMat(numbersToMulticolumn32BitSMat(values, intResult));
    }

    public static SMat mlResultsToMatrix(Mat packedByRows, long dimX, long dimY) {
        Objects.requireNonNull(packedByRows, "Null packed mat");
        if (dimX * dimY != packedByRows.rows()) {
            throw new IllegalArgumentException("Number of rows of ML mat " + packedByRows.rows()
                    + " is not equal to required number of pixels " + dimX + "*" + dimY);
        }
        if (packedByRows.channels() != 1) {
            throw new IllegalArgumentException("Number of channels of ML mat " + packedByRows.channels()
                    + " must be 1");
        }
        final int numberOfChannels = packedByRows.cols();
        return SMat.valueOf(
                dimX,
                dimY,
                SMat.Depth.valueOf(packedByRows.depth()),
                numberOfChannels,
                OTools.toByteBuffer(packedByRows));
    }

    public static SMat mlResultsToMatrix(UMat packedByRows, long dimX, long dimY) {
        Objects.requireNonNull(packedByRows, "Null packed mat");
        if (dimX * dimY != packedByRows.rows()) {
            throw new IllegalArgumentException("Number of rows of ML mat " + packedByRows.rows()
                    + " is not equal to required number of pixels " + dimX + "*" + dimY);
        }
        if (packedByRows.channels() != 1) {
            throw new IllegalArgumentException("Number of channels of ML mat " + packedByRows.channels()
                    + " must be 1");
        }
        final int numberOfChannels = packedByRows.cols();
        return SMat.valueOf(
                dimX,
                dimY,
                SMat.Depth.valueOf(packedByRows.depth()),
                numberOfChannels,
                OTools.toByteBuffer(packedByRows));
    }

    public static SNumbers multicolumnMatToNumbers(Mat packedByRows) {
        Objects.requireNonNull(packedByRows, "Null packed mat");
        if (packedByRows.channels() != 1) {
            throw new IllegalArgumentException("ML mat must contain only 1 channel, but it contains "
                    + packedByRows.channels() + " channels: " + packedByRows);
        }
        return toRawNumbers(packedByRows, packedByRows.cols());
    }

    public static SNumbers multicolumnMatToNumbers(UMat packedByRows) {
        try (Mat m = OTools.toMat(packedByRows)) {
            return multicolumnMatToNumbers(m);
        }
    }

    private static SMat matrixToMlSamplesOrResponsesSMat(SMat values, boolean intResult) {
        Objects.requireNonNull(values, "Null values");
        if (!values.isInitialized()) {
            throw new IllegalArgumentException("Not initialized values");
        }
        if (!values.isChannelsOrderCompatibleWithMultiMatrix()) {
            final MultiMatrix2D multiMatrix = values.toMultiMatrix2D(true);
            values = SMat.valueOf(multiMatrix, SMat.ChannelOrder.ORDER_IN_PACKED_BYTE_BUFFER);
            // numbersToMulticolumnMat suppose that the SNumbers was get from pixel-processing functions,
            // working with standard RGB order; so, ByteBuffer in packedByRows must use the same order
        }
        final Matrix<? extends PArray> packed3d = Matrices.asPrecision(
                values.toPackedMatrix(true),
                intResult ? int.class : float.class);
        assert packed3d.dimCount() == 3;
        final Matrix<? extends PArray> packedByRows = Matrices.matrix(
                packed3d.array(),
                1, // - 1 channel
                packed3d.dim(0),
                packed3d.dim(1) * packed3d.dim(2));
        return SMat.valueOfPackedMatrix(packedByRows);
    }

    private static SMat numbersToMulticolumn32BitSMat(SNumbers values, boolean intResult) {
        Objects.requireNonNull(values, "Null values");
        if (!values.isInitialized()) {
            throw new IllegalArgumentException("Not initialized values");
        }
        final UpdatablePArray array = intResult ?
                IntArray.as(values.toIntArray()) :
                FloatArray.as(values.toFloatArray());
        return SMat.valueOfPackedMatrix(Matrices.matrix(
                array,
                1, // - 1 channel in the ML mat
                values.getBlockLength(),
                values.n()));
    }

    private static SMat setTo(SMat result, Mat mat, boolean serializeData) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(mat, "Null mat");
//        System.out.println("\n!!!!!!!!!!!!!!!!!! " + OPTIMIZE_COPYING);
        final long dimX = mat.cols();
        final long dimY = mat.rows();
        final SMat.Depth depth = SMat.Depth.valueOf(mat.depth());
        final int channels = mat.channels();
        final SMat.Convertible pointer = OPTIMIZE_COPYING && !serializeData ?
                new ConvertibleMat(mat) :
                new ConvertibleByteBufferMatrix(OTools.toByteBuffer(mat));
        return result.setAll(new long[] {dimX, dimY}, depth, channels, pointer);
    }

    private static SMat setTo(SMat result, UMat mat, boolean serializeData) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(mat, "Null mat");
//        System.out.println("\nUUUUUUUUUUUUUUUUUUU " + OPTIMIZE_COPYING);
        final long dimX = mat.cols();
        final long dimY = mat.rows();
        final SMat.Depth depth = SMat.Depth.valueOf(mat.depth());
        final int channels = mat.channels();
        final SMat.Convertible pointer;
        if (OPTIMIZE_COPYING && !serializeData) {
            pointer = new ConvertibleUMat(mat);
        } else {
            pointer = new ConvertibleByteBufferMatrix(OTools.toByteBuffer(mat));
        }
        return result.setAll(new long[] {dimX, dimY}, depth, channels, pointer);
    }

    private static SMat prepareForOpenCV(SMat m, boolean autoConvertPackedBits) {
        if (!m.getDepth().isOpenCVCompatible()) {
            if (autoConvertPackedBits && OTools.isPackedBits(m)) {
                m = SMat.valueOf(m.toMultiMatrix().toPrecisionIfNot(byte.class));
            } else {
                throw new IllegalArgumentException("Matrix element depth "
                        + (m.getNumberOfChannels() == 1 ? "" : "for " + m.getNumberOfChannels() + " channels ")
                        + "is not supported: " + m);
            }
        }
        if (m.getDimX() > Integer.MAX_VALUE || m.getDimY() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large matrix sizes (>=2^31): " + m);
        }
        return m;
    }

}
