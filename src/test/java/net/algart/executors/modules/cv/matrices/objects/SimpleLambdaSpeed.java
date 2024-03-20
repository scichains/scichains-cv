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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.math.IRangeFinder.IntArrayAppender;
import net.algart.arrays.MutableIntArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

public final class SimpleLambdaSpeed {
    private static final int n = 1048 * 1048;
    private static int[] progression;

    private interface IntSequenceConsumer {
        // - returns false, if actually this value was not accepted (count should not be increased)
        boolean accept(int value, int currentCount);

        static IntSequenceConsumer getArrayFiller(int[] array, int offset) {
            return (value, currentCount) -> {
                array[offset + currentCount] = value;
                return true;
            };
        }
    }

    private static void time(String name, long t1, long t2) {
        System.out.printf("%-48s %.3f ms, %.5f ns/element%n", name + ":", (t2 - t1) * 1e-6, (t2 - t1) / (double) n);
    }

    private static long sumInts(int[] array, int from, int to) {
        long sum = 0;
        for (int k = from; k < to; k++) {
            sum += array[k];
        }
        return sum;
    }

    private static long sumInts(int[] array, int from, int to, IntUnaryOperator op) {
        long sum = 0;
        for (int k = from; k < to; k++) {
            sum += op.applyAsInt(array[k]);
        }
        return sum;
    }

    private static long sumIntsTransforming(int[] array, int from, int to, IntUnaryOperator op) {
        return sumInts(array, from, to, operand -> 2 * op.applyAsInt(operand));
    }

    private static void fillIntsByIntArrayAppender(int[] array, int from, int to) {
        IntArrayAppender appender = new IntArrayAppender(array, from);
        for (int k = from; k < to; k++) {
            appender.accept(k);
        }
    }

    private static void fillIntsByIntArrayTranslatingAppender(int[] array, int from, int to) {
        IntArrayTranslatingAppender appender = new IntArrayTranslatingAppender(array, i -> progression[i], from);
        for (int k = from; k < to; k++) {
            appender.accept(k);
        }
    }

    private static void fillIntsByIntSequenceConsumer(int[] array, int from, int to) {
        IntSequenceConsumer intSequenceConsumer = IntSequenceConsumer.getArrayFiller(array, from);
        for (int k = from, counter = 0; k < to; k++) {
            if (intSequenceConsumer.accept(k, counter)) {
                counter++;
            }
        }
    }

    private static void fillIntArrayByIntConsumer(MutableIntArray array, int from, int to) {
        IntConsumer intConsumer = k -> array.pushInt(k);
        for (int k = from; k < to; k++) {
            intConsumer.accept(k);
        }
    }

    private static List<IntUnaryOperator> list = new ArrayList<>();

    private static long sumIntsTransformingClass(int[] array, int from, int to, IntUnaryOperator op) {
        class Transformed implements IntUnaryOperator {
            long result;

            @Override
            public int applyAsInt(int operand) {
                result += operand;
                return operand;
            }
        }
        Transformed transformed = new Transformed();
//        list.add(transformed);
        sumInts(array, from, to, transformed);
        return transformed.result;
    }

    private static long sumIntsPassing(int[] array, int from, int to, IntUnaryOperator op) {
        return 2 * sumInts(array, from, to, op);
    }

    public static void main(String[] args) {
        double someInfo = 0;
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
            for (int k = 0; k < n; k++) {
                ints[k] = k;
            }
            t2 = System.nanoTime();
            time("loop int[k] = k", t1, t2);
            someInfo += System.identityHashCode(ints);
            progression = ints.clone();

            t1 = System.nanoTime();
            Arrays.setAll(ints, j -> j);
            t2 = System.nanoTime();
            time("j -> j", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            Arrays.parallelSetAll(ints, j -> j);
            t2 = System.nanoTime();
            time("parallel j -> j", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            long sum = 0;
            for (int k = 0; k < n; k++) {
                sum += k + 1;
            }
            t2 = System.nanoTime();
            time("loop sum += k+1", t1, t2);
            someInfo += sum;

            t1 = System.nanoTime();
            sum = 0;
            for (int k = 0; k < n; k++) {
                sum += ((IntUnaryOperator) j -> j + 1).applyAsInt(k);
            }
            t2 = System.nanoTime();
            time("loop sum += (j -> j+1)(k)", t1, t2);
            someInfo += sum;

            t1 = System.nanoTime();
            sum = sumInts(ints, 0, n);
            t2 = System.nanoTime();
            time("sumInts", t1, t2);
            someInfo += sum;

            t1 = System.nanoTime();
            sum = sumInts(ints, 0, n, j -> j);
            t2 = System.nanoTime();
            time("sumInts(j -> j)", t1, t2);
            someInfo += sum;

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += sumInts(ints, k, k + 1);
            }
            t2 = System.nanoTime();
            time("loop sumInts", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += sumInts(ints, k, k + 1, j -> j);
            }
            t2 = System.nanoTime();
            time("loop sumInts(j -> j)", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += sumIntsPassing(ints, k, k + 1, j -> j);
            }
            t2 = System.nanoTime();
            time("loop sumIntsPassing", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += sumIntsTransforming(ints, k, k + 1, j -> j);
            }
            t2 = System.nanoTime();
            time("loop sumIntsTransforming", t1, t2);
            someInfo += System.identityHashCode(ints);

            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                sum += sumIntsTransformingClass(ints, k, k + 1, j -> j);
            }
            t2 = System.nanoTime();
            time("loop sumIntsTransformingClass", t1, t2);
            someInfo += System.identityHashCode(ints);

            Arrays.fill(ints, 0);
            t1 = System.nanoTime();
            fillIntsByIntArrayAppender(ints, 0, n);
            t2 = System.nanoTime();
            time("loop IntConsumer (filling)", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(ints, progression)) throw new AssertionError();

            Arrays.fill(ints, 0);
            t1 = System.nanoTime();
            fillIntsByIntSequenceConsumer(ints, 0, n);
            t2 = System.nanoTime();
            time("loop IntSequenceConsumer (filling)", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(ints, progression)) throw new AssertionError();

            MutableIntArray intArray = net.algart.arrays.Arrays.SMM.newEmptyIntArray();
            t1 = System.nanoTime();
            fillIntArrayByIntConsumer(intArray, 0, n);
            t2 = System.nanoTime();
            time("loop IntConsumer for MutableArray", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(net.algart.arrays.Arrays.toJavaArray(intArray), progression))
                throw new AssertionError();

            Arrays.fill(ints, 0);
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                fillIntsByIntArrayAppender(ints, k, k + 1);
            }
            t2 = System.nanoTime();
            time("loop fillIntsByIntArrayAppender", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(ints, progression)) throw new AssertionError();

            Arrays.fill(ints, 0);
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                fillIntsByIntArrayTranslatingAppender(ints, k, k + 1);
            }
            t2 = System.nanoTime();
            time("loop fillIntsByIntArrayTranslatingAppender", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(ints, progression)) throw new AssertionError();

            Arrays.fill(ints, 0);
            t1 = System.nanoTime();
            for (int k = 0; k < n; k++) {
                fillIntsByIntSequenceConsumer(ints, k, k + 1);
            }
            t2 = System.nanoTime();
            time("loop fillIntsByIntSequenceConsumer", t1, t2);
            someInfo += System.identityHashCode(ints);
            if (!Arrays.equals(ints, progression)) throw new AssertionError();

            intArray.length(0);
            for (int k = 0; k < n; k++) {
                fillIntArrayByIntConsumer(intArray, k, k + 1);
            }
            t2 = System.nanoTime();
            time("loop fillIntArrayByIntConsumer", t1, t2);
            someInfo += System.identityHashCode(intArray);
            if (!Arrays.equals(net.algart.arrays.Arrays.toJavaArray(intArray), progression))
                throw new AssertionError();
        }
        System.out.printf("%nSome info: %s%n", someInfo);
        // - to avoid extra optimization
    }
}
