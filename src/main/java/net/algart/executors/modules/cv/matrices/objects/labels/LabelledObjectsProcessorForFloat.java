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

import net.algart.arrays.IntArray;

import java.util.Objects;

public abstract class LabelledObjectsProcessorForFloat extends LabelsProcessor {
    final int numberOfChannels;
    final int[] lists;
    final int[] listHeads;
    final SingleObjectProcessor processor;
    final float[][][] threadObjectData;
    private final float[][][] requestedObjectData;
    final int[] cardinalities;

    LabelledObjectsProcessorForFloat(
            int[] lists,
            int[] listHeads,
            SingleObjectProcessor processor,
            int numberOfChannels) {
        super(
                IntArray.as(Objects.requireNonNull(listHeads, "Null listHeads")),
                numberOrParticlesInBlockForParallelProcessing(listHeads.length));
        this.numberOfChannels = numberOfChannels;
        this.lists = Objects.requireNonNull(lists, "Null lists");
        this.listHeads = listHeads;
        this.processor = Objects.requireNonNull(processor, "Null processor");
        this.requestedObjectData = requestFloatArrays(numberOfTasks(), numberOfChannels);
        this.threadObjectData = new float[this.requestedObjectData.length][][];
        for (int k = 0; k < this.requestedObjectData.length; k++) {
            this.threadObjectData[k] = requestedObjectData[k].clone();
            // - Java arrays threadObjectData[k][c] will be probably reallocated,
            // and we need to store original references to correctly release them
        }
        this.cardinalities = new int[Math.max(0, listHeads.length - 1)];
    }

    public static LabelledObjectsProcessorForFloat getInstance(
            int[] lists,
            int[] listHeads,
            SingleObjectProcessor processor,
            Object[] channels) {
        Objects.requireNonNull(lists, "Null lists");
        Objects.requireNonNull(listHeads, "Null listHeads");
        Objects.requireNonNull(processor, "Null processor");
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
            case 1: {
                if (channel0 instanceof byte[]) {
                    return new LabelledObjectsProcessorForFloat1Channels.ForBytes(
                            lists, listHeads, processor, (byte[]) channel0);
                } else if (channel0 instanceof short[]) {
                    return new LabelledObjectsProcessorForFloat1Channels.ForShorts(
                            lists, listHeads, processor, (short[]) channel0);
                } else if (channel0 instanceof int[]) {
                    return new LabelledObjectsProcessorForFloat1Channels.ForInts(
                            lists, listHeads, processor, (int[]) channel0);
                } else if (channel0 instanceof float[]) {
                    return new LabelledObjectsProcessorForFloat1Channels.ForFloats(
                            lists, listHeads, processor, (float[]) channel0);
                } else if (channel0 instanceof double[]) {
                    return new LabelledObjectsProcessorForFloat1Channels.ForDoubles(
                            lists, listHeads, processor, (double[]) channel0);
                } else {
                    throw new AssertionError();
                }
            }
            /*Repeat() case 2  ==> case 3,,case 4,,case 5;;
                       2(Channels) ==> 3$1,,4$1,,5$1
             */
            case 2: {
                if (channel0 instanceof byte[]) {
                    return new LabelledObjectsProcessorForFloat2Channels.ForBytes(
                            lists, listHeads, processor, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new LabelledObjectsProcessorForFloat2Channels.ForShorts(
                            lists, listHeads, processor, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new LabelledObjectsProcessorForFloat2Channels.ForInts(
                            lists, listHeads, processor, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new LabelledObjectsProcessorForFloat2Channels.ForFloats(
                            lists, listHeads, processor, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new LabelledObjectsProcessorForFloat2Channels.ForDoubles(
                            lists, listHeads, processor, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
            case 3: {
                if (channel0 instanceof byte[]) {
                    return new LabelledObjectsProcessorForFloat3Channels.ForBytes(
                            lists, listHeads, processor, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new LabelledObjectsProcessorForFloat3Channels.ForShorts(
                            lists, listHeads, processor, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new LabelledObjectsProcessorForFloat3Channels.ForInts(
                            lists, listHeads, processor, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new LabelledObjectsProcessorForFloat3Channels.ForFloats(
                            lists, listHeads, processor, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new LabelledObjectsProcessorForFloat3Channels.ForDoubles(
                            lists, listHeads, processor, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            case 4: {
                if (channel0 instanceof byte[]) {
                    return new LabelledObjectsProcessorForFloat4Channels.ForBytes(
                            lists, listHeads, processor, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new LabelledObjectsProcessorForFloat4Channels.ForShorts(
                            lists, listHeads, processor, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new LabelledObjectsProcessorForFloat4Channels.ForInts(
                            lists, listHeads, processor, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new LabelledObjectsProcessorForFloat4Channels.ForFloats(
                            lists, listHeads, processor, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new LabelledObjectsProcessorForFloat4Channels.ForDoubles(
                            lists, listHeads, processor, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            case 5: {
                if (channel0 instanceof byte[]) {
                    return new LabelledObjectsProcessorForFloat5Channels.ForBytes(
                            lists, listHeads, processor, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new LabelledObjectsProcessorForFloat5Channels.ForShorts(
                            lists, listHeads, processor, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new LabelledObjectsProcessorForFloat5Channels.ForInts(
                            lists, listHeads, processor, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new LabelledObjectsProcessorForFloat5Channels.ForFloats(
                            lists, listHeads, processor, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new LabelledObjectsProcessorForFloat5Channels.ForDoubles(
                            lists, listHeads, processor, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
            /*Repeat.AutoGeneratedEnd*/
            default: {
                if (channel0 instanceof byte[]) {
                    return new ForBytes(lists, listHeads, processor, castToByte(channels));
                } else if (channel0 instanceof short[]) {
                    return new ForShorts(lists, listHeads, processor, castToShort(channels));
                } else if (channel0 instanceof int[]) {
                    return new ForInts(lists, listHeads, processor, castToInt(channels));
                } else if (channel0 instanceof float[]) {
                    return new ForFloats(lists, listHeads, processor, castToFloat(channels));
                } else if (channel0 instanceof double[]) {
                    return new ForDoubles(lists, listHeads, processor, castToDouble(channels));
                } else {
                    throw new AssertionError();
                }
            }
        }
    }

    @Override
    public void close() {
        releaseFloatArrays(requestedObjectData);
    }

    public int[] cardinalities() {
        return cardinalities;
    }

    /*Repeat() Bytes  ==> Shorts,,Ints,,Floats,,Doubles;;
                   byte   ==> short,,int,,float,,double;;
                   (data\w*\[c\]\[index\]) \& 0xFF ==> $1 & 0xFFFF,,$1,,$1,,(float) $1 */

    private static class ForBytes extends LabelledObjectsProcessorForFloat {
        private final byte[][] data;

        ForBytes(int[] lists, int[] listHeads, SingleObjectProcessor processor, byte[][] data) {
            super(lists, listHeads, processor, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            float[][] objectData = this.threadObjectData[threadIndex];
            int capacity = objectData[0].length;
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= capacity) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        capacity = objectData[0].length;
                    }
                    for (int c = 0; c < numberOfChannels; c++) {
                        objectData[c][pixelCount] = data[c][index] & 0xFF;
                    }
                    pixelCount++;
                    index = lists[index];
                }
                if (label > 0) {
                    this.cardinalities[label - 1] = pixelCount;
                }
                this.processor.processPixels(label, objectData, pixelCount, threadIndex);
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */

    private static class ForShorts extends LabelledObjectsProcessorForFloat {
        private final short[][] data;

        ForShorts(int[] lists, int[] listHeads, SingleObjectProcessor processor, short[][] data) {
            super(lists, listHeads, processor, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            float[][] objectData = this.threadObjectData[threadIndex];
            int capacity = objectData[0].length;
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= capacity) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        capacity = objectData[0].length;
                    }
                    for (int c = 0; c < numberOfChannels; c++) {
                        objectData[c][pixelCount] = data[c][index] & 0xFFFF;
                    }
                    pixelCount++;
                    index = lists[index];
                }
                if (label > 0) {
                    this.cardinalities[label - 1] = pixelCount;
                }
                this.processor.processPixels(label, objectData, pixelCount, threadIndex);
            }
        }
    }

    private static class ForInts extends LabelledObjectsProcessorForFloat {
        private final int[][] data;

        ForInts(int[] lists, int[] listHeads, SingleObjectProcessor processor, int[][] data) {
            super(lists, listHeads, processor, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            float[][] objectData = this.threadObjectData[threadIndex];
            int capacity = objectData[0].length;
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= capacity) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        capacity = objectData[0].length;
                    }
                    for (int c = 0; c < numberOfChannels; c++) {
                        objectData[c][pixelCount] = data[c][index];
                    }
                    pixelCount++;
                    index = lists[index];
                }
                if (label > 0) {
                    this.cardinalities[label - 1] = pixelCount;
                }
                this.processor.processPixels(label, objectData, pixelCount, threadIndex);
            }
        }
    }

    private static class ForFloats extends LabelledObjectsProcessorForFloat {
        private final float[][] data;

        ForFloats(int[] lists, int[] listHeads, SingleObjectProcessor processor, float[][] data) {
            super(lists, listHeads, processor, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            float[][] objectData = this.threadObjectData[threadIndex];
            int capacity = objectData[0].length;
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= capacity) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        capacity = objectData[0].length;
                    }
                    for (int c = 0; c < numberOfChannels; c++) {
                        objectData[c][pixelCount] = data[c][index];
                    }
                    pixelCount++;
                    index = lists[index];
                }
                if (label > 0) {
                    this.cardinalities[label - 1] = pixelCount;
                }
                this.processor.processPixels(label, objectData, pixelCount, threadIndex);
            }
        }
    }

    private static class ForDoubles extends LabelledObjectsProcessorForFloat {
        private final double[][] data;

        ForDoubles(int[] lists, int[] listHeads, SingleObjectProcessor processor, double[][] data) {
            super(lists, listHeads, processor, data.length);
            this.data = data;
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            float[][] objectData = this.threadObjectData[threadIndex];
            int capacity = objectData[0].length;
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= capacity) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        capacity = objectData[0].length;
                    }
                    for (int c = 0; c < numberOfChannels; c++) {
                        objectData[c][pixelCount] = (float) data[c][index];
                    }
                    pixelCount++;
                    index = lists[index];
                }
                if (label > 0) {
                    this.cardinalities[label - 1] = pixelCount;
                }
                this.processor.processPixels(label, objectData, pixelCount, threadIndex);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/
}
