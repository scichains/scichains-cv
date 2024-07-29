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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.math.functions.RectangularFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

public final class BestBoundaryThreshold extends MultiMatrix2DFilter {
    private double alpha = 0.0;
    private double gamma = 1.0;
    private boolean invert = false;

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = positive(gamma);
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final Matrix<UpdatableByteArray> intensity = Arrays.SMM.newByteMatrix(source.dimensions());
        Matrices.copy(null, intensity, source.toPrecisionIfNot(byte.class).intensityChannel());
        long[] intensityIntegral = new long[256];
        long[] boundaryHistogram = new long[256];
        final int thresholdMax = findThreshold(intensity, alpha, gamma, intensityIntegral, boundaryHistogram);
        logDebug(() -> "Best-boundary threshold, alpha = " + alpha + ", gamma = " + gamma
                + "; optimal threshold found at " + thresholdMax + " (" + thresholdMax / 255.0 + ")"
                + " for " + source);
        final double inValue = invert ? 1.0 : 0.0;
        final double outValue = invert ? 0.0 : 1.0;
        final RectangularFunc f = RectangularFunc.getInstance(0, thresholdMax, inValue, outValue);
        final Matrix<BitArray> result = Matrices.asFuncMatrix(f, BitArray.class, intensity);
        return MultiMatrix.valueOf2DMono(result);
    }

    public int findThreshold(
            Matrix<? extends ByteArray> m, double alpha, double gamma) {
        return findThreshold(m, alpha, gamma, new long[256], new long[256]);
    }


    private int findThreshold(
            Matrix<? extends ByteArray> m,
            double alpha,
            double gamma,
            long[] intensityIntegral,
            long[] boundaryHistogram) {
        final ByteArray bytes = m.array();
        long[] intensityHistogram = new long[256];
        Arrays.histogramOf(bytes, intensityHistogram, 0.0, 256.0);
        long totalArea = Histogram.sumOf(intensityHistogram);
        boundaryHistogram(bytes, m.dimX(), m.dimY(), boundaryHistogram);
        double addition = alpha * ((double) m.dimX() + (double) m.dimY());
        int result = 0;
        double maxQuality = Double.NEGATIVE_INFINITY;
        long bestAreaOfBlackOrWhiteObjects = -1;
        long numberOfZeroPixels = 0;
        for (int threshMax = 0; threshMax < 256; threshMax++) {
            numberOfZeroPixels += intensityHistogram[threshMax];
            intensityIntegral[threshMax] = numberOfZeroPixels;
            long areaOfBlackOrWhiteObjects = Math.min(numberOfZeroPixels, totalArea - numberOfZeroPixels);
            if (areaOfBlackOrWhiteObjects == 0) {
                // - this threshold produces totally black or totally white picture
                continue;
            }
            long lengthOfBoundary = boundaryHistogram[threshMax];
            double quality = (double) areaOfBlackOrWhiteObjects / Math.pow(addition + lengthOfBoundary, gamma);
            if (quality > maxQuality) {
                bestAreaOfBlackOrWhiteObjects = areaOfBlackOrWhiteObjects;
                maxQuality = quality;
                result = threshMax;
            }
        }
        if (LOGGABLE_DEBUG) {
            logDebug("Best-boundary threshold found at " + result
                    + ": area of objects " + bestAreaOfBlackOrWhiteObjects
                    + ", boundary length " + boundaryHistogram[result]
                    + ", quality " + maxQuality);
        }
        return result;
    }

    private static void boundaryHistogram(ByteArray bytes, long dimX, long dimY, long[] boundaryHistogram) {
        if (dimX > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Matrix width = " + dimX + " is larger than 2^31-1");
        }
        byte[] line1 = new byte[(int) dimX];
        byte[] line2 = new byte[(int) dimX];
        long[] xMinHistogram = new long[256];
        long[] xMaxHistogram = new long[256];
        long[] yMinHistogram = new long[256];
        long[] yMaxHistogram = new long[256];
        // - filled by zero
        bytes.getData(0, line2);
        for (long y = 1, disp = dimX; y < dimY; y++, disp += dimX) {
            byte[] temp = line2;
            line2 = line1;
            line1 = temp;
            bytes.getData(disp, line2);
            for (int x = 1; x < line2.length; x++) {
                int a = line2[x] & 0xFF;
                int b = line2[x - 1] & 0xFF;
                int c = line1[x] & 0xFF;
                if (a < b) {
                    xMinHistogram[a]++;
                    xMaxHistogram[b]++;
                } else {
                    xMinHistogram[b]++;
                    xMaxHistogram[a]++;
                }
                if (a < c) {
                    yMinHistogram[a]++;
                    yMaxHistogram[c]++;
                } else {
                    yMinHistogram[c]++;
                    yMaxHistogram[a]++;
                }
            }
        }
        long minIntegral = 0;
        long maxIntegral = 0;
        for (int k = 0; k < 256; k++) {
            minIntegral += xMinHistogram[k] + yMinHistogram[k];
            maxIntegral += xMaxHistogram[k] + yMaxHistogram[k];
            boundaryHistogram[k] = minIntegral - maxIntegral;
        }
    }
}
