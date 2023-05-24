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

package net.algart.executors.modules.cv.matrices.objects.labels;

import java.util.Objects;

abstract class CardinalitiesAndSumsCalculator extends CardinalitiesCalculator {
    final int numberOfChannels;
    final double[][][] threadSums;
    private final double[][][] requestedSums;
    double[] sums;
    // Note: the sums (unlike threadSums/requestedSums) are ordered as RGBRGB...
    // - sums[numberOfChannels*k+c] corresponds to label #k for channel #c

    CardinalitiesAndSumsCalculator(int[] labels, int numberOfChannels) {
        super(labels);
        this.numberOfChannels = numberOfChannels;
        this.requestedSums = requestClearedDoubleArrays(numberOfTasks(), numberOfChannels);
        this.threadSums = new double[this.requestedSums.length][][];
        for (int k = 0; k < this.requestedSums.length; k++) {
            this.threadSums[k] = requestedSums[k].clone();
            // - Java arrays threadSums[k][c] will be probably reallocated,
            // and we need to store original references to correctly release them
        }
    }

    static CardinalitiesAndSumsCalculator getInstance(int[] labels, Object[] channels) {
        Objects.requireNonNull(labels, "Null labels");
        Objects.requireNonNull(channels, "Null channels");
        if (channels.length == 0) {
            throw new IllegalArgumentException("Empty channels array");
        }
        final Object channel0 = channels[0];
        if (!isArraySupported(channel0)) {
            throw new IllegalArgumentException("Illegal array type: " + channel0);
        }
        for (int k = 1; k < channels.length; k++) {
            if (channels[k].getClass() != channel0.getClass()) {
                throw new IllegalArgumentException("Different type of channels: " + channels[k].getClass()
                        + " != " + channel0.getClass());
            }
        }
        switch (channels.length) {
            /*Repeat() case 1  ==> case 2,,case 3,,case 4,,case 5;;
                       1(Channels) ==> 2$1,,3$1,,4$1,,5$1
             */
            case 1: {
                if (channel0 instanceof byte[]) {
                    return new CardinalitiesAndSumsCalculator1Channels.ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new CardinalitiesAndSumsCalculator1Channels.ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new CardinalitiesAndSumsCalculator1Channels.ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new CardinalitiesAndSumsCalculator1Channels.ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new CardinalitiesAndSumsCalculator1Channels.ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
            case 2: {
                if (channel0 instanceof byte[]) {
                    return new CardinalitiesAndSumsCalculator2Channels.ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new CardinalitiesAndSumsCalculator2Channels.ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new CardinalitiesAndSumsCalculator2Channels.ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new CardinalitiesAndSumsCalculator2Channels.ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new CardinalitiesAndSumsCalculator2Channels.ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }

            case 3: {
                if (channel0 instanceof byte[]) {
                    return new CardinalitiesAndSumsCalculator3Channels.ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new CardinalitiesAndSumsCalculator3Channels.ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new CardinalitiesAndSumsCalculator3Channels.ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new CardinalitiesAndSumsCalculator3Channels.ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new CardinalitiesAndSumsCalculator3Channels.ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }

            case 4: {
                if (channel0 instanceof byte[]) {
                    return new CardinalitiesAndSumsCalculator4Channels.ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new CardinalitiesAndSumsCalculator4Channels.ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new CardinalitiesAndSumsCalculator4Channels.ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new CardinalitiesAndSumsCalculator4Channels.ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new CardinalitiesAndSumsCalculator4Channels.ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }

            case 5: {
                if (channel0 instanceof byte[]) {
                    return new CardinalitiesAndSumsCalculator5Channels.ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new CardinalitiesAndSumsCalculator5Channels.ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new CardinalitiesAndSumsCalculator5Channels.ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new CardinalitiesAndSumsCalculator5Channels.ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new CardinalitiesAndSumsCalculator5Channels.ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            /*Repeat.AutoGeneratedEnd*/
            default: {
                if (channel0 instanceof byte[]) {
                    return new ForBytes(labels, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new ForShorts(labels, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new ForInts(labels, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new ForFloats(labels, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new ForDoubles(labels, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
        }
    }

    @Override
    public void close() {
        super.close();
        releaseAndClearDoubleArrays(requestedSums, maxLabel + 1);
    }

    @Override
    protected void finish() {
        super.finish();
        if ((long) maxLabel * (long) numberOfChannels > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large required array for " + numberOfChannels
                    + " channels: more that 2^31-1 elements");
        }
        this.sums = new double[numberOfChannels * maxLabel];
        // Zero-filled by Java
        for (double[][] sums : this.threadSums) {
            for (int c = 0; c < numberOfChannels; c++) {
                if (sums[c].length != sums[0].length) {
                    throw new AssertionError("Different sums length for different channels!");
                }
                final int length = Math.min(maxLabel, sums[c].length - 1);
                for (int k = 1, disp = c; k <= length; k++, disp += numberOfChannels) {
                    this.sums[disp] += sums[c][k];
                }
                // Note: in the resulting "this.sums" we use first numberOfChannels elements (zero label):
                // actual information for label is in this.sums[(label-1)*numberOfChannels]
            }
        }
    }

    /*Repeat() Bytes  ==> Shorts,,Ints,,Floats,,Doubles;;
               byte   ==> short,,int,,float,,double;;
               (data\w*\[c\]\[k\]) \& 0xFF ==> $1 & 0xFFFF,,$1,,$1,,$1
     */
    private static class ForBytes extends CardinalitiesAndSumsCalculator {
        private final byte[][] data;

        public ForBytes(int[] labels, byte[][] data) {
            super(labels, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            int[] cardinalities = this.threadCardinalities[threadIndex];
            double[][] sums = this.threadSums[threadIndex];
            for (int k = p, kMax = k + count; k < kMax; k++) {
                int label = labels[k];
                if (label > 0) {
                    if (label >= cardinalities.length) {
                        cardinalities = ensureCapacityForLabel(cardinalities, label);
                        ensureSeveralArraysCapacityForLabel(sums, label);
                    }
                    cardinalities[label]++;
                    for (int c = 0; c < numberOfChannels; c++) {
                        sums[c][label] += data[c][k] & 0xFF;
                        // Note: for better performance, skip cardinalities/sums[0]=0
                    }
                }
            }
            this.threadCardinalities[threadIndex] = cardinalities;
        }
    }
    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private static class ForShorts extends CardinalitiesAndSumsCalculator {
        private final short[][] data;

        public ForShorts(int[] labels, short[][] data) {
            super(labels, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            int[] cardinalities = this.threadCardinalities[threadIndex];
            double[][] sums = this.threadSums[threadIndex];
            for (int k = p, kMax = k + count; k < kMax; k++) {
                int label = labels[k];
                if (label > 0) {
                    if (label >= cardinalities.length) {
                        cardinalities = ensureCapacityForLabel(cardinalities, label);
                        ensureSeveralArraysCapacityForLabel(sums, label);
                    }
                    cardinalities[label]++;
                    for (int c = 0; c < numberOfChannels; c++) {
                        sums[c][label] += data[c][k] & 0xFFFF;
                        // Note: for better performance, skip cardinalities/sums[0]=0
                    }
                }
            }
            this.threadCardinalities[threadIndex] = cardinalities;
        }
    }

    private static class ForInts extends CardinalitiesAndSumsCalculator {
        private final int[][] data;

        public ForInts(int[] labels, int[][] data) {
            super(labels, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            int[] cardinalities = this.threadCardinalities[threadIndex];
            double[][] sums = this.threadSums[threadIndex];
            for (int k = p, kMax = k + count; k < kMax; k++) {
                int label = labels[k];
                if (label > 0) {
                    if (label >= cardinalities.length) {
                        cardinalities = ensureCapacityForLabel(cardinalities, label);
                        ensureSeveralArraysCapacityForLabel(sums, label);
                    }
                    cardinalities[label]++;
                    for (int c = 0; c < numberOfChannels; c++) {
                        sums[c][label] += data[c][k];
                        // Note: for better performance, skip cardinalities/sums[0]=0
                    }
                }
            }
            this.threadCardinalities[threadIndex] = cardinalities;
        }
    }

    private static class ForFloats extends CardinalitiesAndSumsCalculator {
        private final float[][] data;

        public ForFloats(int[] labels, float[][] data) {
            super(labels, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            int[] cardinalities = this.threadCardinalities[threadIndex];
            double[][] sums = this.threadSums[threadIndex];
            for (int k = p, kMax = k + count; k < kMax; k++) {
                int label = labels[k];
                if (label > 0) {
                    if (label >= cardinalities.length) {
                        cardinalities = ensureCapacityForLabel(cardinalities, label);
                        ensureSeveralArraysCapacityForLabel(sums, label);
                    }
                    cardinalities[label]++;
                    for (int c = 0; c < numberOfChannels; c++) {
                        sums[c][label] += data[c][k];
                        // Note: for better performance, skip cardinalities/sums[0]=0
                    }
                }
            }
            this.threadCardinalities[threadIndex] = cardinalities;
        }
    }

    private static class ForDoubles extends CardinalitiesAndSumsCalculator {
        private final double[][] data;

        public ForDoubles(int[] labels, double[][] data) {
            super(labels, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            int[] cardinalities = this.threadCardinalities[threadIndex];
            double[][] sums = this.threadSums[threadIndex];
            for (int k = p, kMax = k + count; k < kMax; k++) {
                int label = labels[k];
                if (label > 0) {
                    if (label >= cardinalities.length) {
                        cardinalities = ensureCapacityForLabel(cardinalities, label);
                        ensureSeveralArraysCapacityForLabel(sums, label);
                    }
                    cardinalities[label]++;
                    for (int c = 0; c < numberOfChannels; c++) {
                        sums[c][label] += data[c][k];
                        // Note: for better performance, skip cardinalities/sums[0]=0
                    }
                }
            }
            this.threadCardinalities[threadIndex] = cardinalities;
        }
    }
    /*Repeat.AutoGeneratedEnd*/
}