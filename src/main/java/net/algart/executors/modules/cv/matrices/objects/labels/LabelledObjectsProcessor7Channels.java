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

abstract class LabelledObjectsProcessor7Channels extends LabelledObjectsProcessor {
    LabelledObjectsProcessor7Channels(int[] lists, int[] listHeads, SingleObjectProcessor processor) {
        super(lists, listHeads, processor, 7);
    }

    /*Repeat() Byte ==> Short,,Int,,Float,,Double;;
               byte ==> short,,int,,float,,double */

    static class ForBytes extends LabelledObjectsProcessor7Channels {
        private final byte[] data0;
        private final byte[] data1;
        private final byte[] data2;
        private final byte[] data3;
        private final byte[] data4;
        private final byte[] data5;
        private final byte[] data6;
        private final byte[][][] threadObjectData;
        private final byte[][][] requestedObjectData;

        public ForBytes(int[] lists, int[] listHeads, SingleObjectProcessor processor, byte[][] data) {
            super(lists, listHeads, processor);
            this.data0 = data[0];
            this.data1 = data[1];
            this.data2 = data[2];
            this.data3 = data[3];
            this.data4 = data[4];
            this.data5 = data[5];
            this.data6 = data[6];
            this.requestedObjectData = requestByteArrays(numberOfTasks(), numberOfChannels);
            this.threadObjectData = new byte[this.requestedObjectData.length][][];
            for (int k = 0; k < this.requestedObjectData.length; k++) {
                this.threadObjectData[k] = requestedObjectData[k].clone();
                // - Java arrays threadObjectData[k][c] will be probably reallocated,
                // and we need to store original references to correctly release them
            }
        }

        @Override
        public void close() {
            releaseByteArrays(requestedObjectData);
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            final byte[][] objectData = this.threadObjectData[threadIndex];
            byte[] objectData0 = objectData[0];
            byte[] objectData1 = objectData[1];
            byte[] objectData2 = objectData[2];
            byte[] objectData3 = objectData[3];
            byte[] objectData4 = objectData[4];
            byte[] objectData5 = objectData[5];
            byte[] objectData6 = objectData[6];
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= objectData0.length) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        objectData0 = objectData[0];
                        objectData1 = objectData[1];
                        objectData2 = objectData[2];
                        objectData3 = objectData[3];
                        objectData4 = objectData[4];
                        objectData5 = objectData[5];
                        objectData6 = objectData[6];
                    }
                    objectData0[pixelCount] = data0[index];
                    objectData1[pixelCount] = data1[index];
                    objectData2[pixelCount] = data2[index];
                    objectData3[pixelCount] = data3[index];
                    objectData4[pixelCount] = data4[index];
                    objectData5[pixelCount] = data5[index];
                    objectData6[pixelCount] = data6[index];
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

    static class ForShorts extends LabelledObjectsProcessor7Channels {
        private final short[] data0;
        private final short[] data1;
        private final short[] data2;
        private final short[] data3;
        private final short[] data4;
        private final short[] data5;
        private final short[] data6;
        private final short[][][] threadObjectData;
        private final short[][][] requestedObjectData;

        public ForShorts(int[] lists, int[] listHeads, SingleObjectProcessor processor, short[][] data) {
            super(lists, listHeads, processor);
            this.data0 = data[0];
            this.data1 = data[1];
            this.data2 = data[2];
            this.data3 = data[3];
            this.data4 = data[4];
            this.data5 = data[5];
            this.data6 = data[6];
            this.requestedObjectData = requestShortArrays(numberOfTasks(), numberOfChannels);
            this.threadObjectData = new short[this.requestedObjectData.length][][];
            for (int k = 0; k < this.requestedObjectData.length; k++) {
                this.threadObjectData[k] = requestedObjectData[k].clone();
                // - Java arrays threadObjectData[k][c] will be probably reallocated,
                // and we need to store original references to correctly release them
            }
        }

        @Override
        public void close() {
            releaseShortArrays(requestedObjectData);
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            final short[][] objectData = this.threadObjectData[threadIndex];
            short[] objectData0 = objectData[0];
            short[] objectData1 = objectData[1];
            short[] objectData2 = objectData[2];
            short[] objectData3 = objectData[3];
            short[] objectData4 = objectData[4];
            short[] objectData5 = objectData[5];
            short[] objectData6 = objectData[6];
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= objectData0.length) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        objectData0 = objectData[0];
                        objectData1 = objectData[1];
                        objectData2 = objectData[2];
                        objectData3 = objectData[3];
                        objectData4 = objectData[4];
                        objectData5 = objectData[5];
                        objectData6 = objectData[6];
                    }
                    objectData0[pixelCount] = data0[index];
                    objectData1[pixelCount] = data1[index];
                    objectData2[pixelCount] = data2[index];
                    objectData3[pixelCount] = data3[index];
                    objectData4[pixelCount] = data4[index];
                    objectData5[pixelCount] = data5[index];
                    objectData6[pixelCount] = data6[index];
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

    static class ForInts extends LabelledObjectsProcessor7Channels {
        private final int[] data0;
        private final int[] data1;
        private final int[] data2;
        private final int[] data3;
        private final int[] data4;
        private final int[] data5;
        private final int[] data6;
        private final int[][][] threadObjectData;
        private final int[][][] requestedObjectData;

        public ForInts(int[] lists, int[] listHeads, SingleObjectProcessor processor, int[][] data) {
            super(lists, listHeads, processor);
            this.data0 = data[0];
            this.data1 = data[1];
            this.data2 = data[2];
            this.data3 = data[3];
            this.data4 = data[4];
            this.data5 = data[5];
            this.data6 = data[6];
            this.requestedObjectData = requestIntArrays(numberOfTasks(), numberOfChannels);
            this.threadObjectData = new int[this.requestedObjectData.length][][];
            for (int k = 0; k < this.requestedObjectData.length; k++) {
                this.threadObjectData[k] = requestedObjectData[k].clone();
                // - Java arrays threadObjectData[k][c] will be probably reallocated,
                // and we need to store original references to correctly release them
            }
        }

        @Override
        public void close() {
            releaseIntArrays(requestedObjectData);
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            final int[][] objectData = this.threadObjectData[threadIndex];
            int[] objectData0 = objectData[0];
            int[] objectData1 = objectData[1];
            int[] objectData2 = objectData[2];
            int[] objectData3 = objectData[3];
            int[] objectData4 = objectData[4];
            int[] objectData5 = objectData[5];
            int[] objectData6 = objectData[6];
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= objectData0.length) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        objectData0 = objectData[0];
                        objectData1 = objectData[1];
                        objectData2 = objectData[2];
                        objectData3 = objectData[3];
                        objectData4 = objectData[4];
                        objectData5 = objectData[5];
                        objectData6 = objectData[6];
                    }
                    objectData0[pixelCount] = data0[index];
                    objectData1[pixelCount] = data1[index];
                    objectData2[pixelCount] = data2[index];
                    objectData3[pixelCount] = data3[index];
                    objectData4[pixelCount] = data4[index];
                    objectData5[pixelCount] = data5[index];
                    objectData6[pixelCount] = data6[index];
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

    static class ForFloats extends LabelledObjectsProcessor7Channels {
        private final float[] data0;
        private final float[] data1;
        private final float[] data2;
        private final float[] data3;
        private final float[] data4;
        private final float[] data5;
        private final float[] data6;
        private final float[][][] threadObjectData;
        private final float[][][] requestedObjectData;

        public ForFloats(int[] lists, int[] listHeads, SingleObjectProcessor processor, float[][] data) {
            super(lists, listHeads, processor);
            this.data0 = data[0];
            this.data1 = data[1];
            this.data2 = data[2];
            this.data3 = data[3];
            this.data4 = data[4];
            this.data5 = data[5];
            this.data6 = data[6];
            this.requestedObjectData = requestFloatArrays(numberOfTasks(), numberOfChannels);
            this.threadObjectData = new float[this.requestedObjectData.length][][];
            for (int k = 0; k < this.requestedObjectData.length; k++) {
                this.threadObjectData[k] = requestedObjectData[k].clone();
                // - Java arrays threadObjectData[k][c] will be probably reallocated,
                // and we need to store original references to correctly release them
            }
        }

        @Override
        public void close() {
            releaseFloatArrays(requestedObjectData);
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            final float[][] objectData = this.threadObjectData[threadIndex];
            float[] objectData0 = objectData[0];
            float[] objectData1 = objectData[1];
            float[] objectData2 = objectData[2];
            float[] objectData3 = objectData[3];
            float[] objectData4 = objectData[4];
            float[] objectData5 = objectData[5];
            float[] objectData6 = objectData[6];
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= objectData0.length) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        objectData0 = objectData[0];
                        objectData1 = objectData[1];
                        objectData2 = objectData[2];
                        objectData3 = objectData[3];
                        objectData4 = objectData[4];
                        objectData5 = objectData[5];
                        objectData6 = objectData[6];
                    }
                    objectData0[pixelCount] = data0[index];
                    objectData1[pixelCount] = data1[index];
                    objectData2[pixelCount] = data2[index];
                    objectData3[pixelCount] = data3[index];
                    objectData4[pixelCount] = data4[index];
                    objectData5[pixelCount] = data5[index];
                    objectData6[pixelCount] = data6[index];
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

    static class ForDoubles extends LabelledObjectsProcessor7Channels {
        private final double[] data0;
        private final double[] data1;
        private final double[] data2;
        private final double[] data3;
        private final double[] data4;
        private final double[] data5;
        private final double[] data6;
        private final double[][][] threadObjectData;
        private final double[][][] requestedObjectData;

        public ForDoubles(int[] lists, int[] listHeads, SingleObjectProcessor processor, double[][] data) {
            super(lists, listHeads, processor);
            this.data0 = data[0];
            this.data1 = data[1];
            this.data2 = data[2];
            this.data3 = data[3];
            this.data4 = data[4];
            this.data5 = data[5];
            this.data6 = data[6];
            this.requestedObjectData = requestDoubleArrays(numberOfTasks(), numberOfChannels);
            this.threadObjectData = new double[this.requestedObjectData.length][][];
            for (int k = 0; k < this.requestedObjectData.length; k++) {
                this.threadObjectData[k] = requestedObjectData[k].clone();
                // - Java arrays threadObjectData[k][c] will be probably reallocated,
                // and we need to store original references to correctly release them
            }
        }

        @Override
        public void close() {
            releaseDoubleArrays(requestedObjectData);
        }

        @Override
        protected void processSubArr(int p, int count, int threadIndex) {
            final double[][] objectData = this.threadObjectData[threadIndex];
            double[] objectData0 = objectData[0];
            double[] objectData1 = objectData[1];
            double[] objectData2 = objectData[2];
            double[] objectData3 = objectData[3];
            double[] objectData4 = objectData[4];
            double[] objectData5 = objectData[5];
            double[] objectData6 = objectData[6];
            for (int label = p, labelMax = label + count; label < labelMax; label++) {
                int index = listHeads[label];
                int pixelCount = 0;
                while (index != -1) {
                    if (pixelCount >= objectData0.length) {
                        ensureCapacityForPixels(objectData, lists.length, pixelCount);
                        objectData0 = objectData[0];
                        objectData1 = objectData[1];
                        objectData2 = objectData[2];
                        objectData3 = objectData[3];
                        objectData4 = objectData[4];
                        objectData5 = objectData[5];
                        objectData6 = objectData[6];
                    }
                    objectData0[pixelCount] = data0[index];
                    objectData1[pixelCount] = data1[index];
                    objectData2[pixelCount] = data2[index];
                    objectData3[pixelCount] = data3[index];
                    objectData4[pixelCount] = data4[index];
                    objectData5[pixelCount] = data5[index];
                    objectData6[pixelCount] = data6[index];
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
