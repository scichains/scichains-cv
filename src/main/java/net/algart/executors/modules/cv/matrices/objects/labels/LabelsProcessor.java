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

package net.algart.executors.modules.cv.matrices.objects.labels;

import net.algart.arrays.Array;
import net.algart.arrays.Arrays;
import net.algart.arrays.JArrayPool;
import net.algart.arrays.TooLargeArrayException;

public abstract class LabelsProcessor extends Arrays.ParallelExecutor implements AutoCloseable {
    private static final int NUMBER_OR_PARTICLES_IN_BLOCK_FOR_PARALLEL_PROCESSING = 64;

    private static final int MAX_ALLOWED_LABEL = Integer.MAX_VALUE / 2 - 1;
    // - Theoretically we could process even Integer.MAX_VALUE different labels,
    // but it complicates calculations and will probably lead to out of memory.
    // Mostly probable that it is just an error (random labels).
    // This limit allows to guarantee that (label+1)*2 is still integer.

    private static final int BUFFER_SIZE = 65536;
    private static final JArrayPool BYTE_BUFFERS = JArrayPool.getInstance(byte.class, BUFFER_SIZE);
    private static final JArrayPool SHORT_BUFFERS = JArrayPool.getInstance(short.class, BUFFER_SIZE);
    private static final JArrayPool INT_BUFFERS = JArrayPool.getInstance(int.class, BUFFER_SIZE);
    private static final JArrayPool FLOAT_BUFFERS = JArrayPool.getInstance(float.class, BUFFER_SIZE);
    private static final JArrayPool DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, BUFFER_SIZE);

    private static final JArrayPool CLEARED_INT_BUFFERS = JArrayPool.getInstance(int.class, BUFFER_SIZE);
    private static final JArrayPool CLEARED_DOUBLE_BUFFERS = JArrayPool.getInstance(double.class, BUFFER_SIZE);

    protected LabelsProcessor(Array src) {
        this(src, 32768);
    }

    protected LabelsProcessor(Array src, int blockSize) {
        super(null, null, src, blockSize, 0, 0);
    }

    protected final void processSubArr(long position, int count, int threadIndex) {
        int p = (int) position;
        assert p == position : "Java arrays cannot be >=2^31: " + position;
        processSubArr(p, count, threadIndex);
    }

    protected abstract void processSubArr(int p, int count, int threadIndex);

    protected static int numberOrParticlesInBlockForParallelProcessing(int numberOfParticles) {
        int maxRecommended = numberOfParticles / (2 * Arrays.SystemSettings.cpuCount());
        return Math.max(4, Math.min(maxRecommended, NUMBER_OR_PARTICLES_IN_BLOCK_FOR_PARALLEL_PROCESSING));
        // - if there are little number of particles, we should still try to pass >=2 particles to every CPU
    }

    protected static boolean isArraySupported(Object array) {
        return array instanceof byte[]
                || array instanceof short[]
                || array instanceof int[]
                || array instanceof float[]
                || array instanceof double[];
    }

    protected static byte[][] castToByte(Object[] channels) {
        byte[][] result = new byte[channels.length][];
        for (int k = 0; k < channels.length; k++) {
            result[k] = (byte[]) channels[k];
        }
        return result;
    }

    protected static short[][] castToShort(Object[] channels) {
        short[][] result = new short[channels.length][];
        for (int k = 0; k < channels.length; k++) {
            result[k] = (short[]) channels[k];
        }
        return result;
    }

    protected static int[][] castToInt(Object[] channels) {
        int[][] result = new int[channels.length][];
        for (int k = 0; k < channels.length; k++) {
            result[k] = (int[]) channels[k];
        }
        return result;
    }

    protected static float[][] castToFloat(Object[] channels) {
        float[][] result = new float[channels.length][];
        for (int k = 0; k < channels.length; k++) {
            result[k] = (float[]) channels[k];
        }
        return result;
    }

    protected static double[][] castToDouble(Object[] channels) {
        double[][] result = new double[channels.length][];
        for (int k = 0; k < channels.length; k++) {
            result[k] = (double[]) channels[k];
        }
        return result;
    }

    /*Repeat() \(byte\)\s*0 ==> (short) 0,,0,,0.0f,,0.0;;
               Byte ==> Short,,Int,,Float,,Double;;
               byte ==> short,,int,,float,,double;;
               BYTE ==> SHORT,,INT,,FLOAT,,DOUBLE */

    protected static byte[][] requestByteArrays(int numberOfArrays) {
        byte[][] result = new byte[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (byte[]) BYTE_BUFFERS.requestArray();
        }
        return result;
    }

    protected static byte[][][] requestByteArrays(int numberOfArrays, int numberOfChannels) {
        byte[][][] result = new byte[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestByteArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseByteArrays(byte[][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            BYTE_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseByteArrays(byte[][][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseByteArrays(arrays[k]);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    protected static short[][] requestShortArrays(int numberOfArrays) {
        short[][] result = new short[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (short[]) SHORT_BUFFERS.requestArray();
        }
        return result;
    }

    protected static short[][][] requestShortArrays(int numberOfArrays, int numberOfChannels) {
        short[][][] result = new short[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestShortArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseShortArrays(short[][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            SHORT_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseShortArrays(short[][][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseShortArrays(arrays[k]);
        }
    }

    protected static int[][] requestIntArrays(int numberOfArrays) {
        int[][] result = new int[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (int[]) INT_BUFFERS.requestArray();
        }
        return result;
    }

    protected static int[][][] requestIntArrays(int numberOfArrays, int numberOfChannels) {
        int[][][] result = new int[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestIntArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseIntArrays(int[][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            INT_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseIntArrays(int[][][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseIntArrays(arrays[k]);
        }
    }

    protected static float[][] requestFloatArrays(int numberOfArrays) {
        float[][] result = new float[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (float[]) FLOAT_BUFFERS.requestArray();
        }
        return result;
    }

    protected static float[][][] requestFloatArrays(int numberOfArrays, int numberOfChannels) {
        float[][][] result = new float[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestFloatArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseFloatArrays(float[][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            FLOAT_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseFloatArrays(float[][][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseFloatArrays(arrays[k]);
        }
    }

    protected static double[][] requestDoubleArrays(int numberOfArrays) {
        double[][] result = new double[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (double[]) DOUBLE_BUFFERS.requestArray();
        }
        return result;
    }

    protected static double[][][] requestDoubleArrays(int numberOfArrays, int numberOfChannels) {
        double[][][] result = new double[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestDoubleArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseDoubleArrays(double[][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            DOUBLE_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseDoubleArrays(double[][][] arrays) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseDoubleArrays(arrays[k]);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    protected static int[][] requestClearedIntArrays(int numberOfArrays) {
        int[][] result = new int[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (int[]) CLEARED_INT_BUFFERS.requestArray();
            // - zero-filled by Java while first request
        }
        return result;
    }

    protected static int[][][] requestClearedIntArrays(int numberOfArrays, int numberOfChannels) {
        int[][][] result = new int[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestClearedIntArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseAndClearIntArrays(int[][] arrays, int numberOfElementsToZero) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            if (numberOfElementsToZero > 0) {
                final int length = Math.min(numberOfElementsToZero, arrays[k].length);
                java.util.Arrays.fill(arrays[k], 0, length, 0);
                // - restore initial zero-filled state for future usage
            }
            CLEARED_INT_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseAndClearIntArrays(int[][][] arrays, int numberOfElementsToZero) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseAndClearIntArrays(arrays[k], numberOfElementsToZero);
        }
    }

    protected static double[][] requestClearedDoubleArrays(int numberOfArrays) {
        double[][] result = new double[numberOfArrays][];
        for (int k = 0; k < result.length; k++) {
            result[k] = (double[]) CLEARED_DOUBLE_BUFFERS.requestArray();
            // - zero-filled by Java while first request
        }
        return result;
    }

    protected static double[][][] requestClearedDoubleArrays(int numberOfArrays, int numberOfChannels) {
        double[][][] result = new double[numberOfArrays][][];
        for (int k = 0; k < result.length; k++) {
            result[k] = requestClearedDoubleArrays(numberOfChannels);
        }
        return result;
    }

    protected static void releaseAndClearDoubleArrays(double[][] arrays, int numberOfElementsToZero) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            if (numberOfElementsToZero > 0) {
                final int length = Math.min(numberOfElementsToZero, arrays[k].length);
                java.util.Arrays.fill(arrays[k], 0, length, 0.0);
                // - restore initial zero-filled state for future usage
            }
            CLEARED_DOUBLE_BUFFERS.releaseArray(arrays[k]);
        }
    }

    protected static void releaseAndClearDoubleArrays(double[][][] arrays, int numberOfElementsToZero) {
        for (int k = arrays.length - 1; k >= 0; k--) {
            releaseAndClearDoubleArrays(arrays[k], numberOfElementsToZero);
        }
    }

    /*Repeat() Bytes  ==> Shorts,,Ints,,Floats,,Doubles;;
               byte   ==> short,,int,,float,,double */

    protected static void ensureCapacityForPixels(byte[][] arrays, int totalNumberOfPixels, int newElementIndex) {
        if (newElementIndex < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForPixels(newElementIndex, totalNumberOfPixels, arrays.length);
        assert newElementIndex < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    protected static void ensureCapacityForPixels(short[][] arrays, int totalNumberOfPixels, int newElementIndex) {
        if (newElementIndex < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForPixels(newElementIndex, totalNumberOfPixels, arrays.length);
        assert newElementIndex < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
        }
    }

    protected static void ensureCapacityForPixels(int[][] arrays, int totalNumberOfPixels, int newElementIndex) {
        if (newElementIndex < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForPixels(newElementIndex, totalNumberOfPixels, arrays.length);
        assert newElementIndex < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
        }
    }

    protected static void ensureCapacityForPixels(float[][] arrays, int totalNumberOfPixels, int newElementIndex) {
        if (newElementIndex < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForPixels(newElementIndex, totalNumberOfPixels, arrays.length);
        assert newElementIndex < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
        }
    }

    protected static void ensureCapacityForPixels(double[][] arrays, int totalNumberOfPixels, int newElementIndex) {
        if (newElementIndex < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForPixels(newElementIndex, totalNumberOfPixels, arrays.length);
        assert newElementIndex < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    protected static int[] ensureCapacityForLabel(int[] array, int label) {
        if (label < array.length) {
            return array;
        } else {
            int newLength = increaseCapacityForLabel(label, array.length);
            assert label < newLength;
            return java.util.Arrays.copyOf(array, newLength);
            // - Java automatically fills new elements by zero
        }
    }

    protected static double[] ensureCapacityForLabel(double[] array, int label) {
        if (label < array.length) {
            return array;
        } else {
            int newLength = increaseCapacityForLabel(label, array.length);
            assert label < newLength;
            return java.util.Arrays.copyOf(array, newLength);
            // - Java automatically fills new elements by zero
        }
    }

    // Note: all arrays must be identical length
    protected static void ensureSeveralArraysCapacityForLabel(double[][] arrays, int label) {
        if (label < arrays[0].length) {
            return;
        }
        int newLength = increaseCapacityForLabel(label, arrays[0].length);
        assert label < newLength;
        for (int k = 0; k < arrays.length; k++) {
            arrays[k] = java.util.Arrays.copyOf(arrays[k], newLength);
            // - Java automatically fills new elements by zero
        }
    }

    private static int increaseCapacityForPixels(int newElementIndex, int totalNumberOfPixels, int currentLength) {
        if (newElementIndex >= totalNumberOfPixels) {
            throw new AssertionError("Invalid usage: required number of elements "
                    + (newElementIndex + 1) + " > total number of pixels " + totalNumberOfPixels);
        }
        long newLength = currentLength;
        while (newElementIndex >= newLength) {
            newLength = Math.min(2 * newLength, totalNumberOfPixels);
        }
        return (int) newLength;
    }

    private static int increaseCapacityForLabel(int label, int currentLength) {
        if (label > MAX_ALLOWED_LABEL) {
            throw new TooLargeArrayException("Too large label " + label + " > " + MAX_ALLOWED_LABEL
                    + ": it is probably a random int value, not a label");
        }
        long newLength = currentLength;
        while (label >= newLength) {
            newLength = Math.min(2 * newLength, MAX_ALLOWED_LABEL);
        }
        return (int) newLength;
    }
}
