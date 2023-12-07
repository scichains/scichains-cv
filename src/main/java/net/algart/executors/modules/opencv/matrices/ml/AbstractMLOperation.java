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

package net.algart.executors.modules.opencv.matrices.ml;

import net.algart.executors.modules.opencv.common.OpenCVExecutor;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.arrays.*;
import net.algart.arrays.Arrays;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.IntStream;

public abstract class AbstractMLOperation extends OpenCVExecutor {
    public static final String INPUT_SAMPLES = "samples";
    public static final String INPUT_TRAINING_RESPONSES = "training_responses";

    public static final int MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY = 512;

    private final MLSamplesType samplesType;

    private String statModelFile = "";

    protected AbstractMLOperation(MLSamplesType samplesType) {
        this.samplesType = Objects.requireNonNull(samplesType, "Null samplesType");
    }

    public MLSamplesType samplesType() {
        return samplesType;
    }

    public String getStatModelFile() {
        return statModelFile;
    }

    public void setStatModelFile(String statModelFile) {
        this.statModelFile = nonNull(statModelFile);
    }

    public Path statModelFile() {
        final String modelFileName = nonEmpty(statModelFile, "statistical model file name");
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(modelFileName, this);
    }

    public static Mat categoricalToMultiBinaryResponses(Mat categoricalResponses) {
        Objects.requireNonNull(categoricalResponses, "Null categoricalResponses");
        if (categoricalResponses.channels() != 1) {
            throw new IllegalArgumentException("ML categoricalResponses must contain only 1 channel, but it contains "
                    + categoricalResponses.channels() + " channels: " + categoricalResponses);
        }
        if (categoricalResponses.cols() != 1) {
            throw new IllegalArgumentException("ML categoricalResponses must contain only 1 column, but it contains "
                    + categoricalResponses.cols() + " columns: " + categoricalResponses);
        }
//        long t1 = System.nanoTime();
        try (Mat tempMat = categoricalResponses.depth() != opencv_core.CV_32S ?
                new Mat() : null) {
            Mat intResponses = categoricalResponses;
            if (tempMat != null) {
                intResponses = tempMat;
                categoricalResponses.convertTo(intResponses, opencv_core.CV_32S);
            }
            final PArray array = O2SMat.toRawArray(intResponses);
//            long t2 = System.nanoTime();
            final Mat result = (Mat) categoricalToMultiBinaryResponses(array, false);
//            long t3 = System.nanoTime();
//            System.out.printf(java.util.Locale.US, "Conversion: %.3f ms + %.3f ms%n",
//                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
            return result;
        }
    }

    public static UMat categoricalToMultiBinaryResponses(UMat categoricalResponses) {
        Objects.requireNonNull(categoricalResponses, "Null categoricalResponses");
        if (categoricalResponses.channels() != 1) {
            throw new IllegalArgumentException("ML categoricalResponses must contain only 1 channel, but it contains "
                    + categoricalResponses.channels() + " channels: " + categoricalResponses);
        }
        if (categoricalResponses.cols() != 1) {
            throw new IllegalArgumentException("ML categoricalResponses must contain only 1 column, but it contains "
                    + categoricalResponses.channels() + " columns: " + categoricalResponses);
        }
        try (UMat tempMat = categoricalResponses.depth() != opencv_core.CV_32S ?
                new UMat() : null) {
            UMat intResponses = categoricalResponses;
            if (tempMat != null) {
                intResponses = tempMat;
                categoricalResponses.convertTo(intResponses, opencv_core.CV_32S);
            }
            final PArray array = O2SMat.toRawArray(intResponses);
            return (UMat) categoricalToMultiBinaryResponses(array, true);
        }
    }

    // Note: this function is used to return final result from prediction function,
    // so, we have no reasons to convert it to UMat - if necessary, you can do this later.
    public static Mat selectIndexesOfMaximalMultiResponses(UMat multiResponses) {
        Objects.requireNonNull(multiResponses, "Null multiResponses");
        if (multiResponses.channels() != 1) {
            throw new IllegalArgumentException("ML multiResponses must contain only 1 channel, but it contains "
                    + multiResponses.channels() + " channels: " + multiResponses);
        }
        if (multiResponses.depth() != opencv_core.CV_32F) {
            throw new IllegalArgumentException("ML multiResponses must be CV_32F (float) matrix, but it is "
                    + OTools.toString(multiResponses));
        }
        if (multiResponses.cols() >= MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY) {
            throw new IllegalArgumentException("Too large length of every response: "
                    + multiResponses.cols() + "; it muat be in 1.."
                    + MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY + " range");
        }
        if (multiResponses.cols() == 0) {
            throw new IllegalArgumentException("Zero length of every response (empty matrix");
        }
        try (Mat multiResponsesMat = OTools.toMat(multiResponses)) {
            return selectIndexesOfMaximalMultiResponses(multiResponsesMat);
        }
    }

    public static Mat selectIndexesOfMaximalMultiResponses(Mat multiResponses) {
        Objects.requireNonNull(multiResponses, "Null multiResponses");
        if (multiResponses.channels() != 1) {
            throw new IllegalArgumentException("ML multiResponses must contain only 1 channel, but it contains "
                    + multiResponses.channels() + " channels: " + multiResponses);
        }
        if (multiResponses.depth() != opencv_core.CV_32F) {
            throw new IllegalArgumentException("ML multiResponses must be CV_32F (float) matrix, but it is "
                    + OTools.toString(multiResponses));
        }
        if (multiResponses.cols() >= MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY) {
            throw new IllegalArgumentException("Too large length of every response: "
                    + multiResponses.cols() + "; it muat be in 1.."
                    + MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY + " range");
        }
        if (multiResponses.cols() == 0) {
            throw new IllegalArgumentException("Zero length of every response (empty matrix");
        }
//        long t1 = System.nanoTime();
        final int n = multiResponses.rows();
        final int sampleLength = multiResponses.cols();
        final Mat result = new Mat(n, 1, opencv_core.CV_32S);
        final FloatBuffer sourceMultiResponses = asByteBuffer(multiResponses).asFloatBuffer();
        final IntBuffer resultResponses = asByteBuffer(result).asIntBuffer();
        assert sourceMultiResponses.capacity() == n * sampleLength;
        assert resultResponses.capacity() == n;
//        long t2 = System.nanoTime();
        IntStream.range(0, (n + 255) >>> 8).parallel().forEach(block -> {
            // Note: splitting to blocks helps to provide normal speed
            for (int i = block << 8, disp = i * sampleLength, to = (int) Math.min((long) i + 256, n); i < to; i++) {
                float maxValue = sourceMultiResponses.get(disp++);
                int maxIndex = 0;
                for (int j = 1; j < sampleLength; j++) {
                    float value = sourceMultiResponses.get(disp++);
                    if (value > maxValue) {
                        maxValue = value;
                        maxIndex = j;
                    }
                }
                resultResponses.put(i, maxIndex);
            }
        });
//        long t3 = System.nanoTime();
//        System.out.printf(java.util.Locale.US, "Selection: %.3f ms + %.3f ms%n",
//                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6);
        return result;
    }

    private static Object categoricalToMultiBinaryResponses(PArray array, boolean uMat) {
        assert array instanceof IntArray;
        final int maxValue = (int) Arrays.rangeOf(array).max();
        if (maxValue >= MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY) {
            throw new IllegalArgumentException("Too large value of categorical response: "
                    + maxValue + "; it muat be in 0.."
                    + (MAX_NUMBER_OF_CATEGORICAL_RESPONSES_FOR_CONVERSION_TO_BINARY - 1) + " range");
        }
        final int numberOfColumns = Math.max(maxValue + 1, 2);
        // - If all classes are 0, we still create 2-column response:
        // in other case, AbstractMLPredict will ignore the flag selectIndexesOfMaximalResponses
        // and will produce incorrect result for this model.
        final long capacity = (long) numberOfColumns * Arrays.sizeOf(float.class, array.length());
        if (capacity > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large number of required responcese: "
                    + numberOfColumns + " * " + array.length() + " >= 2^31 / sizeof(float)");
        }
        final int[] categories = Arrays.toJavaArray((IntArray) array);
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) capacity);
        // - zero-initialized by Java
        byteBuffer.order(ByteOrder.nativeOrder());
        final FloatBuffer resultResponses = byteBuffer.asFloatBuffer();
        for (int i = 0, disp = 0; i < categories.length; i++, disp += numberOfColumns) {
            // Note: parallel stream is anti-optimization here
            final int category = Math.max(0, categories[i]);
            assert category < numberOfColumns;
            resultResponses.put(disp + category, 1.0f);
        }
        return uMat ?
                OTools.toUMat(numberOfColumns, categories.length, opencv_core.CV_32F, byteBuffer) :
                OTools.toMat(numberOfColumns, categories.length, opencv_core.CV_32F, byteBuffer);
    }

    // Warning: it is a very dangerous function! See the same function in OTools.
    private static ByteBuffer asByteBuffer(Mat m) {
        final long arraySize = m.arraySize();
        return m.data().position(0).capacity(arraySize).asByteBuffer();
    }
}
