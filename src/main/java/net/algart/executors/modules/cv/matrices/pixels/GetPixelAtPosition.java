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

package net.algart.executors.modules.cv.matrices.pixels;

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToNumbers;
import net.algart.multimatrix.MultiMatrix;

public final class GetPixelAtPosition extends MultiMatrixToNumbers {
    public static final String INPUT_POSITIONS = "positions";

    private long x;
    private long y;
    private boolean rawPixelValues = false;

    public GetPixelAtPosition() {
        addInputNumbers(INPUT_POSITIONS);
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = nonNegative(x);
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = nonNegative(y);
    }

    public boolean isRawPixelValues() {
        return rawPixelValues;
    }

    public void setRawPixelValues(boolean rawPixelValues) {
        this.rawPixelValues = rawPixelValues;
    }

    @Override
    public SNumbers analyse(MultiMatrix source) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        return analyse(source, positions);
    }

    public SNumbers analyse(MultiMatrix source, SNumbers positions) {
        long[] xy = new long[]{x, y};
        if (positions != null && positions.isInitialized()) {
            if (positions.getBlockLength() < 2) {
                throw new IllegalArgumentException("Positions must contain at least 2 elements per block");
            } else if (positions.getBlockLength() > 2) {
                positions = positions.columnRange(0, 2);
            }
            xy = positions.toLongArray();
        }
        final int n = xy.length / 2;
        final int blockLength = source.numberOfChannels();
        SNumbers.checkDimensions(n, blockLength);
        MultiMatrix.PixelValue pixel = null;
        if (rawPixelValues) {
            if (source.elementType() == boolean.class) {
                final byte[] result = new byte[n * blockLength];
                for (int i = 0, disp = 0; i < n; i++, disp += blockLength) {
                    pixel = source.getPixel(source.indexInArray(xy[2 * i], xy[2 * i + 1]), pixel);
                    for (int j = 0; j < blockLength; j++) {
                        result[disp + j] = (byte) pixel.getChannel(j);
                    }
                }
                return SNumbers.ofArray(result, blockLength);
            } else {
                if (source.elementType() == char.class) {
                    final float[] result = new float[n * blockLength];
                    float[] channels = null;
                    for (int i = 0, disp = 0; i < n; i++, disp += blockLength) {
                        pixel = source.getPixel(source.indexInArray(xy[2 * i], xy[2 * i + 1]), pixel);
                        channels = pixel.getFloatChannels(channels);
                        System.arraycopy(channels, 0, result, disp, blockLength);
                    }
                    return SNumbers.ofArray(result, blockLength);
                } else {
                    final Object result = java.lang.reflect.Array.newInstance(
                            source.elementType(), n * blockLength);
                    Object channels = null;
                    for (int i = 0, disp = 0; i < n; i++, disp += blockLength) {
                        pixel = source.getPixel(source.indexInArray(xy[2 * i], xy[2 * i + 1]), pixel);
                        channels = pixel.getChannels(channels);
                        System.arraycopy(channels, 0, result, disp, blockLength);
                    }
                    return SNumbers.ofArray(result, blockLength);
                }
            }
        } else {
            final float[] result = new float[n * blockLength];
            final double mult = 1.0 / source.maxPossibleValue();
            float[] channels = null;
            for (int i = 0, disp = 0; i < n; i++, disp += blockLength) {
                pixel = source.getPixel(source.indexInArray(xy[2 * i], xy[2 * i + 1]), pixel);
                channels = pixel.getFloatChannels(channels);
                for (int j = 0; j < channels.length; j++) {
                    channels[j] *= mult;
                }
                System.arraycopy(channels, 0, result, disp, blockLength);
            }
            return SNumbers.ofArray(result, blockLength);
        }
    }
}
