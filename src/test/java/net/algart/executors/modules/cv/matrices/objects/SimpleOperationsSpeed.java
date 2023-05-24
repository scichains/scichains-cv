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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.*;
import net.algart.math.Range;
import net.algart.math.functions.SelectConstantFunc;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.IntStream;

public final class SimpleOperationsSpeed {
    private static final int n = 2048 * 2048;

    private static void time(String name, long t1, long t2) {
        System.out.printf("%-36s %.6f ms, %.6f ns/element, %.3f Giga-element/second %n",
                name + ":", (t2 - t1) * 1e-6, (t2 - t1) / (double) n, (double) n / (t2 - t1));
    }

    private static void setBitNonParallel(long[] dest, long index, boolean value) {
        if (value)
            dest[(int) (index >>> 6)] |= 1L << (index & 63);
        else
            dest[(int) (index >>> 6)] &= ~(1L << (index & 63));
    }

    private static class IntFiller extends Arrays.ParallelExecutor {
        final int[] array;

        IntFiller(int[] array) {
            super(null, null,
                    SimpleMemoryModel.asUpdatableIntArray(array), 65536, 0, 0);
            this.array = array;
        }

        @Override
        protected void processSubArr(long position, int count, int threadIndex) {
            java.util.Arrays.fill(array, (int) position, (int) position + count, 0);
        }
    }

    public static void main(String[] args) {
        UpdatableBitArray mask = Arrays.SMM.newBitArray(n);
        UpdatableBitArray bitArray = Arrays.SMM.newBitArray(n);
        UpdatableByteArray byteArray = Arrays.SMM.newByteArray(n);
        UpdatableIntArray intArray = Arrays.SMM.newIntArray(n);
        long[] bits = new long[(int) PackedBitArrays.packedLength(n)];
        Random rnd = new Random(157);
        for (int k = 0; k < n; k++) {
            if (rnd.nextInt(3) == 0) {
                mask.setBit(k);
            }
        }
        double someInfo = 0;
        long[] histogram = new long[256];
        for (int test = 1; test <= 32; test++) {
            System.gc();
            System.out.printf("%nSpeed test #%d (%d numbers)%n", test, n);

            long t1, t2;

            t1 = System.nanoTime();
            int[] ints = new int[n];
            t2 = System.nanoTime();
            time("new int[]", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            long[] longs = new long[n];
            t2 = System.nanoTime();
            time("new long[]", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            byte[] bytes = new byte[n];
            t2 = System.nanoTime();
            time("new byte[]", t1, t2);
            someInfo += System.identityHashCode(bytes);

            t1 = System.nanoTime();
            double[] doubles = new double[n];
            t2 = System.nanoTime();
            time("new double[]", t1, t2);
            someInfo += System.identityHashCode(doubles);

            t1 = System.nanoTime();
            byte[] byteClone = bytes.clone();
            t2 = System.nanoTime();
            time("byte[].clone()", t1, t2);
            someInfo += System.identityHashCode(byteClone);

            t1 = System.nanoTime();
            int[] intClone = ints.clone();
            t2 = System.nanoTime();
            time("int[].clone()", t1, t2);
            someInfo += System.identityHashCode(intClone);

            t1 = System.nanoTime();
            double[] doubleClone = doubles.clone();
            t2 = System.nanoTime();
            time("double[].clone()", t1, t2);
            someInfo += System.identityHashCode(doubleClone);

            t1 = System.nanoTime();
            System.arraycopy(bytes, 0, byteClone, 0, bytes.length);
            t2 = System.nanoTime();
            time("arraycopy(byte[],...)", t1, t2);
            someInfo += System.identityHashCode(intClone);

            t1 = System.nanoTime();
            System.arraycopy(ints, 0, intClone, 0, ints.length);
            t2 = System.nanoTime();
            time("arraycopy(int[],...)", t1, t2);
            someInfo += System.identityHashCode(intClone);

            t1 = System.nanoTime();
            java.util.Arrays.sort(intClone);
            t2 = System.nanoTime();
            time("sort(int[])", t1, t2);
            someInfo += System.identityHashCode(intClone);

            System.arraycopy(ints, 0, intClone, 0, ints.length);
            t1 = System.nanoTime();
            java.util.Arrays.parallelSort(intClone);
            t2 = System.nanoTime();
            time("parallelSort(int[])", t1, t2);
            someInfo += System.identityHashCode(intClone);

            t1 = System.nanoTime();
            ByteBuffer byteBuffer = ByteBuffer.allocate(n);
            t2 = System.nanoTime();
            time("ByteBuffer.allocate[]", t1, t2);
            someInfo += System.identityHashCode(byteBuffer);

            t1 = System.nanoTime();
            ByteBuffer byteBufferDirect = ByteBuffer.allocateDirect(n);
            t2 = System.nanoTime();
            time("ByteBuffer.allocateDirect[]", t1, t2);
            someInfo += System.identityHashCode(byteBufferDirect);

            t1 = System.nanoTime();
            JArrays.fillByteArray(bytes, (byte) 1);
            t2 = System.nanoTime();
            time("JArrays.fillByteArray", t1, t2);
            someInfo += System.identityHashCode(bytes);

            t1 = System.nanoTime();
            java.util.Arrays.fill(bytes, (byte) 1);
            t2 = System.nanoTime();
            time("Arrays.fill(byte[])", t1, t2);
            someInfo += System.identityHashCode(bytes);

            t1 = System.nanoTime();
            JArrays.fillIntArray(ints, 1);
            t2 = System.nanoTime();
            time("JArrays.fillIntArray", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            java.util.Arrays.fill(ints, 1);
            t2 = System.nanoTime();
            time("Arrays.fill(int[])", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            IntStream.range(0, (n + 1023) >>> 10).parallel().forEach(b -> {
                int from = b << 10;
                int to = Math.min(n, from + 1024);
                java.util.Arrays.fill(ints, from, to, 1);
            });
            t2 = System.nanoTime();
            time("parallel block Arrays.fill(int[])", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            java.util.Arrays.parallelSetAll(ints, operand -> 1);
            t2 = System.nanoTime();
            time("Arrays.parallelSetAll(int[])", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            new IntFiller(ints).process();
            t2 = System.nanoTime();
            time("IntFiller(int[])", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            JArrays.fillDoubleArray(doubles, 0);
            t2 = System.nanoTime();
            time("JArrays.fillDoubleArray", t1, t2);
            someInfo += System.identityHashCode(doubles);

            t1 = System.nanoTime();
            java.util.Arrays.fill(doubles, 0);
            t2 = System.nanoTime();
            time("Arrays.fill(double[])", t1, t2);
            someInfo += System.identityHashCode(doubles);

            t1 = System.nanoTime();
            java.util.Arrays.parallelSetAll(doubles, operand -> 0);
            t2 = System.nanoTime();
            time("Arrays.parallelSetAll(doubles[])", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            UpdatablePArray array = SimpleMemoryModel.asUpdatableByteArray(bytes);
            Arrays.applyFunc(null, false,
                    SelectConstantFunc.getInstance(0, 255), array, (BitArray) mask);
            t2 = System.nanoTime();
            time("unpackBits via applyFunc (->byte)", t1, t2);

            t1 = System.nanoTime();
            Arrays.unpackBits(SimpleMemoryModel.asUpdatableByteArray(bytes), mask, 0, 255);
            t2 = System.nanoTime();
            time("unpackBits() (->byte)", t1, t2);

            t1 = System.nanoTime();
            Arrays.unpackZeroBits(SimpleMemoryModel.asUpdatableIntArray(ints), mask, 0);
            t2 = System.nanoTime();
            time("unpackZeroBits() (->int)", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                bitArray.setBit(k, true);
            }
            t2 = System.nanoTime();
            time("setBit", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                PackedBitArrays.setBit(bits, k, true);
            }
            t2 = System.nanoTime();
            time("PackedBitArrays.setBit", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                setBitNonParallel(bits, k, true);
            }
            t2 = System.nanoTime();
            time("setBitNonParallel", t1, t2);

            t1 = System.nanoTime();
            boolean bit = false;
            for (int k = 0; k < n; k++) {
                bit |= bitArray.getBit(k);
            }
            t2 = System.nanoTime();
            someInfo += (bit ? 1 : 2);
            time("getBit", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                byteArray.setByte(k, (byte) k);
            }
            t2 = System.nanoTime();
            time("setByte(k,k)", t1, t2);

            t1 = System.nanoTime();
            int byteSum = 0;
            for (int k = 0; k < n; k++) {
                byteSum += byteArray.getByte(k);
            }
            t2 = System.nanoTime();
            someInfo += byteSum;
            time("sum += getByte(k)", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                intArray.setInt(k, k);
            }
            t2 = System.nanoTime();
            time("setInt(k,k)", t1, t2);

            t1 = System.nanoTime();
            int intSum = 0;
            for (int k = 0; k < n; k++) {
                intSum += intArray.getInt(k);
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += getInt(k)", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                ints[k] = k;
            }
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(ints[n / 2]);
            time("simple ints[k]=k", t1, t2);

            t1 = System.nanoTime();
            intSum = 0;
            for (int k = 0; k < n; k++) {
                intSum += ints[k];
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += ints[k]", t1, t2);

            t1 = System.nanoTime();
            intSum = 0;
            for (int k = 0; k < n; k++) {
                intSum += k;
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += k (int)", t1, t2);

            t1 = System.nanoTime();
            intSum = 0;
            for (int k = 0; k < n; k++) {
                intSum += k / 9;
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += k / 9 (int)", t1, t2);

            final double MULTIPLIER = 1.0 / 9.0;
            t1 = System.nanoTime();
            intSum = 0;
            for (int k = 0; k < n; k++) {
                intSum += (int) (k * MULTIPLIER);
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += k * (1.0/9.0) (int)", t1, t2);

            t1 = System.nanoTime();
            intSum = 0;
            for (int k = 1; k <= n; k++) {
                intSum += n / k;
            }
            t2 = System.nanoTime();
            someInfo += intSum;
            time("sum += n / k (int)", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                ints[k] = 1;
            }
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(ints[n / 2]);
            time("simple ints[k]=1", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                longs[k] = k;
            }
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(longs[n / 2]);
            time("simple longs[k]=k", t1, t2);

            t1 = System.nanoTime();
            long longSum = 0;
            for (int k = 0; k < n; k++) {
                longSum += longs[k];
            }
            t2 = System.nanoTime();
            someInfo += longSum;
            time("sum += longs[k]", t1, t2);

            t1 = System.nanoTime();
            longSum = 0;
            for (long k = 0; k < n; k++) {
                longSum += k;
            }
            t2 = System.nanoTime();
            someInfo += longSum;
            time("sum += k (long)", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                longs[k] = 1;
            }
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(longs[n / 2]);
            time("simple longs[k]=1", t1, t2);

            t1 = System.nanoTime();
            IntStream.range(0, n).parallel().forEach(k -> {
                ints[k] = k;
            });
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(ints[10]);
            time("parallel ints[k]=k", t1, t2);

            t1 = System.nanoTime();
            IntStream.range(0, (n + 1023) >>> 10).parallel().forEach(b -> {
                for (int k = b << 10, to = Math.min(n, k + 1024); k < to; k++) {
                    ints[k] = k;
                }
            });
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(ints[10]);
            time("parallel block ints[k]=k", t1, t2);

            int[] cardinalities = new int[n];
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                cardinalities[ints[k]]++;
            }
            t2 = System.nanoTime();
            someInfo += System.identityHashCode(cardinalities);
            time("cardinalities[...]++", t1, t2);

            t1 = System.nanoTime();
            final Range range = Arrays.rangeOf(SimpleMemoryModel.asUpdatableIntArray(ints));
            t2 = System.nanoTime();
            time("rangeOf()", t1, t2);
            someInfo += range.size();

            t1 = System.nanoTime();
            Arrays.histogramOf(SimpleMemoryModel.asUpdatableIntArray(ints), histogram, range.min(), range.max());
            t2 = System.nanoTime();
            time("histogramOf()", t1, t2);
            someInfo += Arrays.sumOf(SimpleMemoryModel.asUpdatableLongArray(histogram));

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                doubles[k] = k + 0.5;
            }
            t2 = System.nanoTime();
            time("simple doubles[k]=k+0.5", t1, t2);

            double sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += doubles[k];
            }
            t2 = System.nanoTime();
            time("summing of doubles", t1, t2);
            someInfo += sum;

            double[][] rgb = new double[][]{doubles, doubles, doubles};
            double[] sums = new double[rgb.length];
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < 3; j++) {
                    sums[j] += rgb[j][k];
                }
            }
            t2 = System.nanoTime();
            time("summing with short loop", t1, t2);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sums[0] += rgb[0][k];
                sums[1] += rgb[1][k];
                sums[2] += rgb[2][k];
            }
            t2 = System.nanoTime();
            time("summing with 3 operations", t1, t2);

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += (long) doubles[k];
            }
            t2 = System.nanoTime();
            time("summing of (long)v", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += Math.round(doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of Math.round", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += Math.rint(doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of Math.rint", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += (int) Math.rint(doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of (int)Math.rint", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += Math.sqrt(doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of Math.sqrt", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += Math.pow(2.5, doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of Math.pow", t1, t2);
            someInfo += sum;

            sum = 0.0;
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += Math.exp(doubles[k]);
            }
            t2 = System.nanoTime();
            time("summing of Math.exp", t1, t2);
            someInfo += sum;

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                doubleClone[k] = Math.exp(doubles[k]);
            }
            t2 = System.nanoTime();
            time("doubles[k]=Math.exp", t1, t2);

            t1 = System.nanoTime();
            IntStream.range(0, (n + 1023) >>> 10).parallel().forEach(b -> {
                for (int k = b << 10, to = Math.min(n, k + 1024); k < to; k++) {
                    doubleClone[k] = Math.exp(doubles[k]);
                }
            });
            t2 = System.nanoTime();
            time("parallel block doubles[k]=Math.exp", t1, t2);
        }
        System.out.printf("%nSome info: %s%n", someInfo);
        // - to avoid extra optimization
    }
}
