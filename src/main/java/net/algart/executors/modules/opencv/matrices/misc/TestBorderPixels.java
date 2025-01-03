/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.modules.opencv.common.MatToNumbers;
import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;

import java.nio.ByteBuffer;
import java.util.Objects;

public class TestBorderPixels extends MatToNumbers implements ReadOnlyExecutionInput {
    public static final String OUTPUT_DIM_X = "dim_x";
    public static final String OUTPUT_DIM_Y = "dim_y";
    public static final String OUTPUT_MAX_BORDER = "max_border";
    public static final String OUTPUT_MAX_TOP = "max_top";
    public static final String OUTPUT_MAX_BOTTOM = "max_bottom";
    public static final String OUTPUT_MAX_LEFT = "max_left";
    public static final String OUTPUT_MAX_RIGHT = "max_right";

    public static class BorderStatistics {
        private final byte[] elementsAtBounds;
        private final int dimX;
        private final int dimY;
        private final int maxTop;
        private final int maxRight;
        private final int maxBottom;
        private final int maxLeft;

        public BorderStatistics(final byte[] elementsAtBounds, final int dimX, final int dimY) {
            Objects.requireNonNull(elementsAtBounds, "Null elementsAtBounds array");
            if (dimX < 0) {
                throw new IllegalArgumentException("Negative dimX = " + dimX);
            }
            if (dimY < 0) {
                throw new IllegalArgumentException("Negative dimY = " + dimY);
            }
            if (elementsAtBounds.length != 2 * ((long) dimX + (long) dimY)) {
                throw new IllegalArgumentException("elementsAtBounds length " + elementsAtBounds.length
                        + " and matrix sizes " + dimX + "x" + dimY
                        + " mismatch: length must be = 2 * (dimX + dimY))");
            }
            this.elementsAtBounds = elementsAtBounds;
            this.dimX = dimX;
            this.dimY = dimY;
            this.maxTop = maxInRange(elementsAtBounds, 0, dimX);
            this.maxRight = maxInRange(elementsAtBounds, dimX, dimY);
            this.maxBottom = maxInRange(elementsAtBounds, dimX + dimY, dimX);
            this.maxLeft = maxInRange(elementsAtBounds, 2 * dimX + dimY, dimY);
        }

        public boolean isEmpty() {
            return elementsAtBounds.length == 0;
        }

        public byte[] elementsAtBounds() {
            return elementsAtBounds;
        }

        public int dimX() {
            return dimX;
        }

        public int dimY() {
            return dimY;
        }

        public int maxTop() {
            return maxTop;
        }

        public int maxRight() {
            return maxRight;
        }

        public int maxBottom() {
            return maxBottom;
        }

        public int maxLeft() {
            return maxLeft;
        }

        public int max() {
            return Math.max(Math.max(maxTop, maxBottom), Math.max(maxLeft, maxRight));
        }

        private static int maxInRange(byte[] array, int offset, int count) {
            int max = Integer.MIN_VALUE;
            for (int to = offset + count; offset < to; offset++) {
                final int value = array[offset] & 0xFF;
                if (value > max) {
                    max = value;
                }
            }
            return max;
        }
    }

    private int outsideIndent = 0;

    public TestBorderPixels() {
        addOutputScalar(OUTPUT_DIM_X);
        addOutputScalar(OUTPUT_DIM_Y);
        addOutputScalar(OUTPUT_MAX_BORDER);
        addOutputScalar(OUTPUT_MAX_TOP);
        addOutputScalar(OUTPUT_MAX_BOTTOM);
        addOutputScalar(OUTPUT_MAX_LEFT);
        addOutputScalar(OUTPUT_MAX_RIGHT);
    }

    public int getOutsideIndent() {
        return outsideIndent;
    }

    public TestBorderPixels setOutsideIndent(int outsideIndent) {
        this.outsideIndent = nonNegative(outsideIndent);
        return this;
    }

    @Override
    public SNumbers analyse(Mat source) {
        BorderStatistics statistics = findBorderStatistics(source, outsideIndent);
        getScalar(OUTPUT_DIM_X).setTo(statistics.dimX());
        getScalar(OUTPUT_DIM_Y).setTo(statistics.dimY());
        final SNumbers result = SNumbers.valueOfArray(statistics.elementsAtBounds());
        if (statistics.isEmpty()) {
            return result;
        }
        getScalar(OUTPUT_MAX_BORDER).setTo(statistics.max());
        getScalar(OUTPUT_MAX_TOP).setTo(statistics.maxTop());
        getScalar(OUTPUT_MAX_BOTTOM).setTo(statistics.maxBottom());
        getScalar(OUTPUT_MAX_LEFT).setTo(statistics.maxLeft());
        getScalar(OUTPUT_MAX_RIGHT).setTo(statistics.maxRight());
        return result;
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }

    public static BorderStatistics findBorderStatistics(Mat source, int outsideIndent) {
        final byte[] bounds = extractBorder(source, outsideIndent);
        final int analysedDimX = analysedDimX(source, outsideIndent);
        final int analysedDimY = analysedDimY(source, outsideIndent);
        return new BorderStatistics(bounds, analysedDimX, analysedDimY);
    }

    public static byte[] extractBorder(Mat source, int outsideIndent) {
        Objects.requireNonNull(source, "Null source");
        if (outsideIndent < 0) {
            throw new IllegalArgumentException("Negative outsideIndent = " + outsideIndent);
        }
        if (source.channels() != 1 || source.depth() != opencv_core.CV_8U) {
            throw new IllegalArgumentException("Unsupported matrix type: " + source
                    + "; only monochrome byte matrices can be processed");
        }
        final int dimX = source.cols();
        final int dimY = source.rows();
        if (dimX <= 2 * (long) outsideIndent || dimY <= 2 * (long) outsideIndent) {
            return new byte[0];
        }
        final int reducedDimX = analysedDimX(source, outsideIndent);
        final int reducedDimY = analysedDimY(source, outsideIndent);
        final long resultSize = 2 * ((long) reducedDimX + (long) reducedDimY);
        // Note: we prefer to duplicate corners pixels to avoid complex logic with reducedDimX/Y=1
        if (resultSize > Integer.MAX_VALUE) {
            // - very improbable
            throw new TooLargeArrayException("Too large result");
        }
        final ByteBuffer byteBuffer = asByteBuffer(source);
        final byte[] result = new byte[(int) resultSize];
        // For example, 10x10, outsideIndent=0
        int disp = 0;
        int p = outsideIndent * dimX + outsideIndent;
        for (int to = disp + reducedDimX; disp < to; disp++, p++) {
            // x=0..9, y=0
            result[disp] = byteBuffer.get(p);
        }
        p--;
        assert p == outsideIndent * dimX + dimX - outsideIndent - 1;
        for (int to = disp + reducedDimY; disp < to; disp++, p += dimX) {
            // x=9, y=0..9; (9,0) is duplicated
            result[disp] = byteBuffer.get(p);
        }
        p -= dimX;
        assert p == (dimY - outsideIndent - 1) * dimX + dimX - outsideIndent - 1;
        for (int to = disp + reducedDimX; disp < to; disp++, p--) {
            // x=9..0, y=9; (9,9) is duplicated
            result[disp] = byteBuffer.get(p);
        }
        p++;
        assert p == (dimY - outsideIndent - 1) * dimX + outsideIndent;
        for (int to = disp + reducedDimY; disp < to; disp++, p -= dimX) {
            // x=0, y=9..0; (9,0) and (0,0) are duplicated
            result[disp] = byteBuffer.get(p);
        }
        p += dimX;
        assert p == outsideIndent * dimX + outsideIndent;
        return result;
    }

    public static int analysedDimX(Mat source, int outsideIndent) {
        return source.cols() - 2 * outsideIndent;
    }

    public static int analysedDimY(Mat source, int outsideIndent) {
        return source.rows() - 2 * outsideIndent;
    }

    static ByteBuffer asByteBuffer(Mat m) {
        final long arraySize = m.arraySize();
        return m.data().position(0).capacity(arraySize).asByteBuffer();
    }
}
