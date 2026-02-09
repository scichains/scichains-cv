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

package net.algart.executors.modules.opencv.matrices.misc;

import net.algart.arrays.Arrays;
import net.algart.arrays.MutableLongArray;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;

class CirclePointsBuilder {
    public static final int MAXIMAL_APERTURE_DIAMETER = 1000000;
    // - must be not too large to avoid too slow building the border or out of memory while making array of its points

    private final boolean removeDuplicates;
    // - for debugging needs

    private volatile double diameter = 0.0;
    private volatile int dimX = 0;
    private volatile int dimY = 0;

    private volatile int[] pointsXY = null;
    private volatile byte[] savedPixels = null;
    // - note: in current version, synchronization is not enough for multi-threading usage

    public CirclePointsBuilder(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
    }

    public CirclePointsBuilder() {
        this(true);
    }

    public double getDiameter() {
        return diameter;
    }

    public CirclePointsBuilder setDiameter(double diameter) {
        if (diameter < 0) {
            throw new IllegalArgumentException("Negative diameter = " + diameter);
        }
        if (diameter > MAXIMAL_APERTURE_DIAMETER) {
            throw new IllegalArgumentException("Too large aperture size " + diameter
                    + ": it must not be greater than " + MAXIMAL_APERTURE_DIAMETER);
        }
        final boolean changed = diameter != this.diameter;
        // - to be on the safe side: clearCache() will be called for last value even while multithreading
        this.diameter = diameter;
        if (changed) {
            clearCache();
        }
        return this;
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public CirclePointsBuilder setDimensions(int dimX, int dimY) {
        if (dimX < 0) {
            throw new IllegalArgumentException("Negative dimX = " + dimX);
        }
        if (dimY < 0) {
            throw new IllegalArgumentException("Negative dimY = " + dimY);
        }
        final boolean changed = dimX != this.dimX || dimY != this.dimY;
        // - to be on the safe side: clearCache() will be called for last value even while multithreading
        this.dimX = dimX;
        this.dimY = dimY;
        if (changed) {
            clearCache();
        }
        return this;
    }

    public void clear() {
        dimX = 0;
        diameter = 0.0;
        clearCache();
    }

    public int[] pointsXY() {
        int[] pointsXY = this.pointsXY;
        if (pointsXY == null) {
            long[] packedPoints = buildPackedPointsOf4ConnectedCircle();
            if (removeDuplicates) {
                packedPoints = sortAndRemoveDuplicates(packedPoints);
            }
            this.pointsXY = pointsXY = unpackPoints(packedPoints);
            this.savedPixels = new byte[pointsXY.length];
        }
        return pointsXY;
    }

    public void drawAndSavePrevious(Mat mat, long centerX, long centerY, byte value) {
        if (mat.type() != opencv_core.CV_8U) {
            throw new IllegalArgumentException("Illegal mat type (must be CV_8U): " + mat);
        }
        final int[] pointsXY = pointsXY();
        if (pointsXY.length == 0) {
            return;
        }
        final int dimX = mat.cols();
        final int dimY = mat.rows();
        final ByteBuffer maskBytes = TestBorderPixels.asByteBuffer(mat);
        for (int disp = 0, k = 0; disp < pointsXY.length; k++) {
            final long x = centerX + pointsXY[disp++];
            final long y = centerY + pointsXY[disp++];
            if (x >= 0 && x < dimX && y >= 0 && y < dimY) {
                final int offset = (int) y * dimX + (int) x;
                savedPixels[k] = maskBytes.get(offset);
                maskBytes.put(offset, value);
            }
        }
    }

    public void restorePrevious(Mat mask, long centerX, long centerY) {
        if (mask.type() != opencv_core.CV_8U) {
            throw new IllegalArgumentException("Illegal  mask type (must be CV_8U): " + mask);
        }
        final int[] pointsXY = pointsXY();
        if (pointsXY.length == 0) {
            return;
        }
        final int dimX = mask.cols();
        final int dimY = mask.rows();
        final ByteBuffer maskBytes = TestBorderPixels.asByteBuffer(mask);
        for (int disp = 0, k = 0; disp < pointsXY.length; k++) {
            final long x = centerX + pointsXY[disp++];
            final long y = centerY + pointsXY[disp++];
            if (x >= 0 && x < dimX && y >= 0 && y < dimY) {
                final int offset = (int) y * dimX + (int) x;
                maskBytes.put(offset, savedPixels[k]);
            }
        }
    }

    @Override
    public String toString() {
        return "CirclePointsBuilder" + (removeDuplicates ? "" : " with duplicates")
                + ": diameter=" + diameter + ", dimX=" + dimX + ", dimY=" + dimY
                + (pointsXY == null ? ", not built yet" : ", " + pointsXY.length / 2 + " points");
    }

    private void clearCache() {
        pointsXY = null;
        savedPixels = null;
    }

    // 4-connectivity is necessary when floodFill is called in CONNECTIVITY_8 mode
    private long[] buildPackedPointsOf4ConnectedCircle() {
        MutableLongArray packedPoints = Arrays.SMM.newEmptyLongArray();
        if (dimX > 0 && dimY > 0 && diameter > 0.0) {
            final double rSqr = 0.25 * diameter * diameter;
            int y = 0;
            int x = (int) Math.ceil(0.5 * diameter);
            int lastX = x;
            do {
                add8Points(packedPoints, lastX, y);
                lastX = x;
                y++;
                x = finxX(y, rSqr);
                while (lastX > x) {
                    // - usually lastX == x+1, but, for example, for diameter=3.0, r=1.5, we have
                    //     x(0)=1.5, ceil=2
                    //     x(1)=sqrt(1.25)=1.118033988749895, ceil=2
                    //     x(2)=sqrt(-1.75)=0, ceil=0
                    add8Points(packedPoints, lastX, y);
                    lastX--;
                }
            } while (y <= x);
        }
        return packedPoints.toJavaArray();
    }

    private static int finxX(double y, double rSqr) {
        final double xSqr = rSqr - y * y;
        return xSqr <= 0.0 ? 0 : (int) Math.ceil(Math.sqrt(xSqr));
    }

    private void add8Points(MutableLongArray packedPoints, int x, int y) {
        assert x >= 0;
        assert y >= 0;
        if (x < dimX && y < dimY) {
            // - greater shift will always lead outside the image
            addPoint(packedPoints, x, y);
            addPoint(packedPoints, y, x);
            addPoint(packedPoints, -x, y);
            addPoint(packedPoints, -y, x);
            addPoint(packedPoints, x, -y);
            addPoint(packedPoints, y, -x);
            addPoint(packedPoints, -x, -y);
            addPoint(packedPoints, -y, -x);
        }
    }

    private static void addPoint(MutableLongArray packedPoints, int x, int y) {
        final long p = (((long) y & 0xFFFFFFFFL) << 32) | ((long) x & 0xFFFFFFFFL);
        assert x == (int) p;
        assert y == (int) (p >>> 32);
        packedPoints.pushLong(p);
    }

    private static long[] sortAndRemoveDuplicates(long[] array) {
        if (array.length == 0) {
            return array;
        }
        java.util.Arrays.parallelSort(array);
        int n = 1;
        for (int k = 1; k < array.length; k++) {
            final long a = array[k];
            if (a != array[k - 1]) {
                array[n++] = a;
            }
        }
        return java.util.Arrays.copyOf(array, n);
    }

    private static int[] unpackPoints(long[] packedPoints) {
        final int[] pointsXY = new int[Math.multiplyExact(2, packedPoints.length)];
        int disp = 0;
        for (long p : packedPoints) {
            final int x = (int) p;
            final int y = (int) (p >>> 32);
            pointsXY[disp++] = x;
            pointsXY[disp++] = y;
        }
        return pointsXY;
    }

    public static void main(String[] args) {
        long[] array = {1, -2, 2, 50, 2, 50, 3, 1, 3};
        System.out.println("Array:              " + java.util.Arrays.toString(array));
        array = sortAndRemoveDuplicates(array);
        System.out.println("Without duplicates: " + java.util.Arrays.toString(array));

        array = new long[]{32};
        System.out.println("Array:              " + java.util.Arrays.toString(array));
        array = sortAndRemoveDuplicates(array);
        System.out.println("Without duplicates: " + java.util.Arrays.toString(array));

        final CirclePointsBuilder builder = new CirclePointsBuilder();
        final CirclePointsBuilder builderDup = new CirclePointsBuilder(false);
        System.out.printf("%s:%n    %s%n", builder, java.util.Arrays.toString(builder.pointsXY()));
        System.out.printf("%s:%n    %s%n", builderDup, java.util.Arrays.toString(builderDup.pointsXY()));

        builder.setDiameter(2).setDimensions(1000, 1000);
        System.out.printf("%s:%n    %s%n", builder, java.util.Arrays.toString(builder.pointsXY()));
        System.out.printf("%s:%n    %s%n", builderDup, java.util.Arrays.toString(builderDup.pointsXY()));
    }
}
