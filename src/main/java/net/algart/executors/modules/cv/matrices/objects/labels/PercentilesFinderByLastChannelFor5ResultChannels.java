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

package net.algart.executors.modules.cv.matrices.objects.labels;

import net.algart.arrays.ArraySelector;
import net.algart.arrays.ByteArraySelector;

class PercentilesFinderByLastChannelFor5ResultChannels extends PercentilesFinderByLastChannel {
    private final boolean separateChannels0;
    private final boolean separateChannels1;
    private final boolean separateChannels2;
    private final boolean separateChannels3;
    private final boolean separateChannels4;
    PercentilesFinderByLastChannelFor5ResultChannels(
            int maxLabel,
            double[][] levels,
            int lowTruncatedMeanIndex,
            int highTruncatedMeanIndex,
            boolean[] separateChannelsSet) {
        super(
                7,
                maxLabel,
                levels,
                lowTruncatedMeanIndex,
                highTruncatedMeanIndex,
                separateChannelsSet);
        this.separateChannels0 = this.separateChannelsSet[0];
        this.separateChannels1 = this.separateChannelsSet[1];
        this.separateChannels2 = this.separateChannelsSet[2];
        this.separateChannels3 = this.separateChannelsSet[3];
        this.separateChannels4 = this.separateChannelsSet[4];
    }

    /*Repeat() \/\/\s*byte version start.*?\/\/\s*byte version end\s* ==> ,, ,, ,, ;;
               byte ==> short,,int,,float,,double;;
               (\s*)\& 0xFF ==> $1& 0xFFFF,, ,,...
    */
    @Override
    public void processPixels(int objectLabel, byte[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int k = 0; k < commonLevels.length; k++) {
                percentilesByChannel0[k][resultDisp] = Float.NaN;
                percentilesByChannel1[k][resultDisp] = Float.NaN;
                percentilesByChannel2[k][resultDisp] = Float.NaN;
                percentilesByChannel3[k][resultDisp] = Float.NaN;
                percentilesByChannel4[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeansByChannel0[resultDisp] = Float.NaN;
                truncatedMeansByChannel1[resultDisp] = Float.NaN;
                truncatedMeansByChannel2[resultDisp] = Float.NaN;
                truncatedMeansByChannel3[resultDisp] = Float.NaN;
                truncatedMeansByChannel4[resultDisp] = Float.NaN;
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        // byte version start
        if (numberOfPixels >= MIN_LENGTH_FOR_USING_BYTE_SELECTOR) {
            percentilesForBytes(
                    resultDisp,
                    objectPixelsByChannels,
                    numberOfPixels,
                    this.threadBytePercentiles[threadIndex],
                    this.threadByteArraySelectors[threadIndex]);
            return;
        }
        // byte version end
        final byte[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final byte[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final byte[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final byte[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final byte[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        final byte[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final byte[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final byte selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j] & 0xFF;
                    sum1 += objectPixelsByChannel1[j] & 0xFF;
                    sum2 += objectPixelsByChannel2[j] & 0xFF;
                    sum3 += objectPixelsByChannel3[j] & 0xFF;
                    sum4 += objectPixelsByChannel4[j] & 0xFF;
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            if (!separateChannels0) {
                percentilesByChannel0[k][resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[k][resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[k][resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[k][resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[k][resultDisp] = (float) (sum4 * countInv);
            }
        }

        if (needTruncatedMeans) {
            final byte preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final byte preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow & 0xFF;
            final double high = preciseHigh & 0xFF;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final byte precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFF;
                final double channel0 = objectPixelsByChannel0[j] & 0xFF;
                final double channel1 = objectPixelsByChannel1[j] & 0xFF;
                final double channel2 = objectPixelsByChannel2[j] & 0xFF;
                final double channel3 = objectPixelsByChannel3[j] & 0xFF;
                final double channel4 = objectPixelsByChannel4[j] & 0xFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    @Override
    public void processPixels(int objectLabel, short[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int k = 0; k < commonLevels.length; k++) {
                percentilesByChannel0[k][resultDisp] = Float.NaN;
                percentilesByChannel1[k][resultDisp] = Float.NaN;
                percentilesByChannel2[k][resultDisp] = Float.NaN;
                percentilesByChannel3[k][resultDisp] = Float.NaN;
                percentilesByChannel4[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeansByChannel0[resultDisp] = Float.NaN;
                truncatedMeansByChannel1[resultDisp] = Float.NaN;
                truncatedMeansByChannel2[resultDisp] = Float.NaN;
                truncatedMeansByChannel3[resultDisp] = Float.NaN;
                truncatedMeansByChannel4[resultDisp] = Float.NaN;
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final short[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final short[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final short[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final short[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final short[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        final short[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final short[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final short selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j] & 0xFFFF;
                    sum1 += objectPixelsByChannel1[j] & 0xFFFF;
                    sum2 += objectPixelsByChannel2[j] & 0xFFFF;
                    sum3 += objectPixelsByChannel3[j] & 0xFFFF;
                    sum4 += objectPixelsByChannel4[j] & 0xFFFF;
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            if (!separateChannels0) {
                percentilesByChannel0[k][resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[k][resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[k][resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[k][resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[k][resultDisp] = (float) (sum4 * countInv);
            }
        }

        if (needTruncatedMeans) {
            final short preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final short preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow & 0xFFFF;
            final double high = preciseHigh & 0xFFFF;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final short precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFFFF;
                final double channel0 = objectPixelsByChannel0[j] & 0xFFFF;
                final double channel1 = objectPixelsByChannel1[j] & 0xFFFF;
                final double channel2 = objectPixelsByChannel2[j] & 0xFFFF;
                final double channel3 = objectPixelsByChannel3[j] & 0xFFFF;
                final double channel4 = objectPixelsByChannel4[j] & 0xFFFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }


    @Override
    public void processPixels(int objectLabel, int[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int k = 0; k < commonLevels.length; k++) {
                percentilesByChannel0[k][resultDisp] = Float.NaN;
                percentilesByChannel1[k][resultDisp] = Float.NaN;
                percentilesByChannel2[k][resultDisp] = Float.NaN;
                percentilesByChannel3[k][resultDisp] = Float.NaN;
                percentilesByChannel4[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeansByChannel0[resultDisp] = Float.NaN;
                truncatedMeansByChannel1[resultDisp] = Float.NaN;
                truncatedMeansByChannel2[resultDisp] = Float.NaN;
                truncatedMeansByChannel3[resultDisp] = Float.NaN;
                truncatedMeansByChannel4[resultDisp] = Float.NaN;
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final int[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final int[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final int[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final int[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final int[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        final int[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final int[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final int selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j];
                    sum1 += objectPixelsByChannel1[j];
                    sum2 += objectPixelsByChannel2[j];
                    sum3 += objectPixelsByChannel3[j];
                    sum4 += objectPixelsByChannel4[j];
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            if (!separateChannels0) {
                percentilesByChannel0[k][resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[k][resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[k][resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[k][resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[k][resultDisp] = (float) (sum4 * countInv);
            }
        }

        if (needTruncatedMeans) {
            final int preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final int preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final int precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                final double channel0 = objectPixelsByChannel0[j];
                final double channel1 = objectPixelsByChannel1[j];
                final double channel2 = objectPixelsByChannel2[j];
                final double channel3 = objectPixelsByChannel3[j];
                final double channel4 = objectPixelsByChannel4[j];
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }


    @Override
    public void processPixels(int objectLabel, float[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int k = 0; k < commonLevels.length; k++) {
                percentilesByChannel0[k][resultDisp] = Float.NaN;
                percentilesByChannel1[k][resultDisp] = Float.NaN;
                percentilesByChannel2[k][resultDisp] = Float.NaN;
                percentilesByChannel3[k][resultDisp] = Float.NaN;
                percentilesByChannel4[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeansByChannel0[resultDisp] = Float.NaN;
                truncatedMeansByChannel1[resultDisp] = Float.NaN;
                truncatedMeansByChannel2[resultDisp] = Float.NaN;
                truncatedMeansByChannel3[resultDisp] = Float.NaN;
                truncatedMeansByChannel4[resultDisp] = Float.NaN;
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final float[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final float[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final float[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final float[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final float[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        final float[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final float[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final float selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j];
                    sum1 += objectPixelsByChannel1[j];
                    sum2 += objectPixelsByChannel2[j];
                    sum3 += objectPixelsByChannel3[j];
                    sum4 += objectPixelsByChannel4[j];
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            if (!separateChannels0) {
                percentilesByChannel0[k][resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[k][resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[k][resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[k][resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[k][resultDisp] = (float) (sum4 * countInv);
            }
        }

        if (needTruncatedMeans) {
            final float preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final float preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final float precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                final double channel0 = objectPixelsByChannel0[j];
                final double channel1 = objectPixelsByChannel1[j];
                final double channel2 = objectPixelsByChannel2[j];
                final double channel3 = objectPixelsByChannel3[j];
                final double channel4 = objectPixelsByChannel4[j];
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }


    @Override
    public void processPixels(int objectLabel, double[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int k = 0; k < commonLevels.length; k++) {
                percentilesByChannel0[k][resultDisp] = Float.NaN;
                percentilesByChannel1[k][resultDisp] = Float.NaN;
                percentilesByChannel2[k][resultDisp] = Float.NaN;
                percentilesByChannel3[k][resultDisp] = Float.NaN;
                percentilesByChannel4[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeansByChannel0[resultDisp] = Float.NaN;
                truncatedMeansByChannel1[resultDisp] = Float.NaN;
                truncatedMeansByChannel2[resultDisp] = Float.NaN;
                truncatedMeansByChannel3[resultDisp] = Float.NaN;
                truncatedMeansByChannel4[resultDisp] = Float.NaN;
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final double[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final double[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final double[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final double[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final double[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        final double[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final double[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final double selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j];
                    sum1 += objectPixelsByChannel1[j];
                    sum2 += objectPixelsByChannel2[j];
                    sum3 += objectPixelsByChannel3[j];
                    sum4 += objectPixelsByChannel4[j];
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            if (!separateChannels0) {
                percentilesByChannel0[k][resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[k][resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[k][resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[k][resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[k][resultDisp] = (float) (sum4 * countInv);
            }
        }

        if (needTruncatedMeans) {
            final double preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final double preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final double precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                final double channel0 = objectPixelsByChannel0[j];
                final double channel1 = objectPixelsByChannel1[j];
                final double channel2 = objectPixelsByChannel2[j];
                final double channel3 = objectPixelsByChannel3[j];
                final double channel4 = objectPixelsByChannel4[j];
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private void percentilesForBytes(
            int resultDisp,
            byte[][] objectPixelsByChannels,
            int numberOfPixels,
            byte[] bytePercentiles,
            ByteArraySelector selector) {
        final byte[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        selector.select(bytePercentiles, commonSortedLevels, unchangedObjectPixelLevels, numberOfPixels);
        // - note: here we don't need second copy of pixel arrays for partial sorting

        final float[][] percentilesByChannel0 = percentilesByChannels[0];
        final float[][] percentilesByChannel1 = percentilesByChannels[1];
        final float[][] percentilesByChannel2 = percentilesByChannels[2];
        final float[][] percentilesByChannel3 = percentilesByChannels[3];
        final float[][] percentilesByChannel4 = percentilesByChannels[4];
        final float[] truncatedMeansByChannel0 = needTruncatedMeans ? truncatedMeansByChannels[0] : null;
        final float[] truncatedMeansByChannel1 = needTruncatedMeans ? truncatedMeansByChannels[1] : null;
        final float[] truncatedMeansByChannel2 = needTruncatedMeans ? truncatedMeansByChannels[2] : null;
        final float[] truncatedMeansByChannel3 = needTruncatedMeans ? truncatedMeansByChannels[3] : null;
        final float[] truncatedMeansByChannel4 = needTruncatedMeans ? truncatedMeansByChannels[4] : null;
        final byte[] objectPixelsByChannel0 = objectPixelsByChannels[0];
        final byte[] objectPixelsByChannel1 = objectPixelsByChannels[1];
        final byte[] objectPixelsByChannel2 = objectPixelsByChannels[2];
        final byte[] objectPixelsByChannel3 = objectPixelsByChannels[3];
        final byte[] objectPixelsByChannel4 = objectPixelsByChannels[4];

        int kLow = -1;
        int kHigh = -1;
        for (int k = 0; k < commonLevels.length; k++) {
            final int unsortedIndex = commonUnsortedLevelsIndexes[k];
            final boolean isLow = unsortedIndex == lowTruncatedMeanIndex;
            final boolean isHigh = unsortedIndex == highTruncatedMeanIndex;
            if (needTruncatedMeans && (isLow || isHigh)) {
                if (isLow) {
                    kLow = k;
                }
                if (isHigh) {
                    kHigh = k;
                }
                continue;
            }
            final byte selectedLevel = bytePercentiles[k];
            double sum0 = 0.0;
            double sum1 = 0.0;
            double sum2 = 0.0;
            double sum3 = 0.0;
            double sum4 = 0.0;
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    sum0 += objectPixelsByChannel0[j] & 0xFF;
                    sum1 += objectPixelsByChannel1[j] & 0xFF;
                    sum2 += objectPixelsByChannel2[j] & 0xFF;
                    sum3 += objectPixelsByChannel3[j] & 0xFF;
                    sum4 += objectPixelsByChannel4[j] & 0xFF;
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannels0) {
                    percentilesByChannel0[unsortedIndex][resultDisp] = (float) (sum0 * countInv);
                }
                if (!separateChannels1) {
                    percentilesByChannel1[unsortedIndex][resultDisp] = (float) (sum1 * countInv);
                }
                if (!separateChannels2) {
                    percentilesByChannel2[unsortedIndex][resultDisp] = (float) (sum2 * countInv);
                }
                if (!separateChannels3) {
                    percentilesByChannel3[unsortedIndex][resultDisp] = (float) (sum3 * countInv);
                }
                if (!separateChannels4) {
                    percentilesByChannel4[unsortedIndex][resultDisp] = (float) (sum4 * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            assert kLow >= 0;
            assert kHigh >= 0;
            final byte preciseLow = bytePercentiles[kLow];
            final byte preciseHigh = bytePercentiles[kHigh];
            final double low = preciseLow & 0xFF;
            final double high = preciseHigh & 0xFF;
            double sum0 = 0.0, sumLow0 = 0.0, sumHigh0 = 0.0;
            double sum1 = 0.0, sumLow1 = 0.0, sumHigh1 = 0.0;
            double sum2 = 0.0, sumLow2 = 0.0, sumHigh2 = 0.0;
            double sum3 = 0.0, sumLow3 = 0.0, sumHigh3 = 0.0;
            double sum4 = 0.0, sumLow4 = 0.0, sumHigh4 = 0.0;
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final byte precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFF;
                final double channel0 = objectPixelsByChannel0[j] & 0xFF;
                final double channel1 = objectPixelsByChannel1[j] & 0xFF;
                final double channel2 = objectPixelsByChannel2[j] & 0xFF;
                final double channel3 = objectPixelsByChannel3[j] & 0xFF;
                final double channel4 = objectPixelsByChannel4[j] & 0xFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    sum0 += channel0;
                    sum1 += channel1;
                    sum2 += channel2;
                    sum3 += channel3;
                    sum4 += channel4;
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    sumLow0 += channel0;
                    sumLow1 += channel1;
                    sumLow2 += channel2;
                    sumLow3 += channel3;
                    sumLow4 += channel4;
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    sumHigh0 += channel0;
                    sumHigh1 += channel1;
                    sumHigh2 += channel2;
                    sumHigh3 += channel3;
                    sumHigh4 += channel4;
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            if (!separateChannels0) {
                percentilesByChannel0[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow0 * countLowInv);
                percentilesByChannel0[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh0 * countHighInv);
                truncatedMeansByChannel0[resultDisp] = (float) (sum0 * countInv);
            }
            if (!separateChannels1) {
                percentilesByChannel1[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow1 * countLowInv);
                percentilesByChannel1[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh1 * countHighInv);
                truncatedMeansByChannel1[resultDisp] = (float) (sum1 * countInv);
            }
            if (!separateChannels2) {
                percentilesByChannel2[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow2 * countLowInv);
                percentilesByChannel2[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh2 * countHighInv);
                truncatedMeansByChannel2[resultDisp] = (float) (sum2 * countInv);
            }
            if (!separateChannels3) {
                percentilesByChannel3[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow3 * countLowInv);
                percentilesByChannel3[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh3 * countHighInv);
                truncatedMeansByChannel3[resultDisp] = (float) (sum3 * countInv);
            }
            if (!separateChannels4) {
                percentilesByChannel4[lowTruncatedMeanIndex][resultDisp] = (float) (sumLow4 * countLowInv);
                percentilesByChannel4[highTruncatedMeanIndex][resultDisp] = (float) (sumHigh4 * countHighInv);
                truncatedMeansByChannel4[resultDisp] = (float) (sum4 * countInv);
            }
        }
    }
}
