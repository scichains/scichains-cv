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

package net.algart.executors.modules.cv.matrices.misc.slopes;

import net.algart.arrays.JArrays;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;

public class SlopeEmphasizerSimpleTest {
    private static String bytesToString(byte[] values) {
        int[] ints = IntStream.range(0, values.length).map(k -> values[k] & 0xFF).toArray();
        return JArrays.toString(ints, Locale.US, "%5d", ", ", 1000);
    }

    private static String shortsToString(short[] values) {
        int[] ints = IntStream.range(0, values.length).map(k -> values[k] & 0xFFFF).toArray();
        return JArrays.toString(values, Locale.US, "%7d", ", ", 1000);
    }

    private static String longsToString(long[] values) {
        return JArrays.toString(values, Locale.US, "%9d", ", ", 1000);
    }

    private static String floatsToString(float[] values) {
        return JArrays.toString(values, Locale.US, "%5.1f", ", ", 1000);
    }

    public static void main(String[] args) {
        final SlopeEmphasizer emphasizer = SlopeEmphasizer.getInstance()
                .setSlopeWidth(5)
                .setMinimalChange(5)
                .setProcessAscending(true)
                .setProcessDescending(true)
                .setAllowLongSlopes(true)
                .setExactHalfSum(true);
//        RandomForDebugging rnd = new RandomForDebugging().setRandSeed(1312);
        Random rnd = new Random(231);
        byte[] bytes;
        short[] shorts;
        long[] longs;
        float[] floats;
        for (int test = 1000000; test >= 0; test--) {
            bytes = new byte[(rnd.nextInt() & 0xFFFF) % 100];
            shorts = new short[bytes.length];
            longs = new long[bytes.length];
            floats = new float[bytes.length];
            emphasizer.setAllowLongSlopes((rnd.nextInt() % 2 == 0));
            emphasizer.setSlopeWidth(1 + (rnd.nextInt() & 0xF));
            emphasizer.setMinimalChange(rnd.nextInt() & 0xFF);
            for (int k = 0; k < bytes.length; k++) {
                bytes[k] = (byte) rnd.nextInt();
                floats[k] = bytes[k] & 0xFF;
                shorts[k] = (short) rnd.nextInt();
                longs[k] = (long) rnd.nextInt() | ((long) rnd.nextInt()) << 32;
            }
            byte[] bytesClone = bytes.clone();
            short[] shortsClone = shorts.clone();
            long[] longsClone = longs.clone();
            float[] floatsClone = floats.clone();
            emphasizer.emphasize(bytes, 0, bytes.length);
            emphasizer.emphasize(shorts, 0, shorts.length);
            emphasizer.emphasize(longs, 0, longs.length);
            emphasizer.emphasize(floats, 0, floats.length);
            int errorPosition = -1;
            for (int k = 0; k < bytes.length; k++) {
                if (floats[k] != (bytes[k] & 0xFF)) {
                    errorPosition = k;
                    break;
                }
            }
            if (test <= 20 || errorPosition >= 0) {
                System.out.println(shortsToString(shortsClone));
                System.out.println(shortsToString(shorts));
                System.out.println();
                System.out.println(longsToString(longsClone));
                System.out.println(longsToString(longs));
                System.out.println();
                System.out.println(bytesToString(bytesClone));
                System.out.println(bytesToString(bytes));
                System.out.println();
                System.out.println(floatsToString(floatsClone));
                System.out.println(floatsToString(floats));
                System.out.println();
            }
            if (errorPosition >= 0) {
                emphasizer.emphasize(bytesClone, 0, bytes.length);
                emphasizer.emphasize(floatsClone, 0, floats.length);
                throw new AssertionError("Difference in emphasizing! " + errorPosition);
            }
            if (emphasizer.getSlopeWidth() == 1
                    && emphasizer.getMinimalChange() != 0
                && !Arrays.equals(bytes, bytesClone)) {
                throw new AssertionError("Slope width = 1 cannot change an array!");
            }
        }
        emphasizer.setSlopeWidth(5)
                .setMinimalChange(5)
                .setProcessAscending(true)
                .setProcessDescending(true)
                .setAllowLongSlopes(true)
                .setExactHalfSum(false);
        bytes = new byte[190];
        for (int k = 0; k < bytes.length; k++) {
            bytes[k] = (byte) rnd.nextInt();
        }
        System.out.println(bytesToString(bytes));
        emphasizer.emphasize(bytes, 0, bytes.length);
        System.out.println(bytesToString(bytes));
        System.out.println();

        bytes = new byte[] {2, 2, 3, 6, 11, 10, 20, 30, 14, 13, 12, 0, 1, 2, 2};
        floats = new float[bytes.length];
        for (int k = 0; k < bytes.length; k++) {
            floats[k] = bytes[k] & 0xFF;
        }

        System.out.println(bytesToString(bytes));
        emphasizer.emphasize(bytes);
        System.out.println(bytesToString(bytes));

        emphasizer.emphasize(floats);
        System.out.println(floatsToString(floats));
        System.out.println();
    }
}
