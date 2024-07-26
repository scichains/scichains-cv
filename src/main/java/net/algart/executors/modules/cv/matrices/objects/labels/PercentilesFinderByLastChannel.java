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

import net.algart.arrays.*;

import java.util.Arrays;
import java.util.Objects;

// Requires to additional channels M-1 and M-2: M-1 for storing levels, M-2 for storing sorted levels
class PercentilesFinderByLastChannel extends PercentilesFinderForSeparateChannels {
    private static final int NUMBER_OF_SERVICE_CHANNELS = 2;

    final double[] commonLevels;
    final double[] commonSortedLevels;
    final int[] commonUnsortedLevelsIndexes;
    final boolean[] separateChannelsSet;

    PercentilesFinderByLastChannel(
            int numberOfChannels,
            int maxLabel,
            double[][] levelsByChannels,
            int lowTruncatedMeanIndex,
            int highTruncatedMeanIndex,
            boolean[] separateChannelsSet) {
        super(
                numberOfChannels,
                numberOfChannels - NUMBER_OF_SERVICE_CHANNELS,
                maxLabel,
                levelsByChannels,
                lowTruncatedMeanIndex, highTruncatedMeanIndex);
        Objects.requireNonNull(separateChannelsSet, "Null separateChannelsSet");
        this.commonLevels = this.levels[0];
        this.commonSortedLevels = this.sortedLevels[0];
        this.commonUnsortedLevelsIndexes = this.unsortedLevelsIndexes[0];
        if (lowTruncatedMeanIndex >= commonLevels.length || highTruncatedMeanIndex >= commonLevels.length) {
            throw new IllegalArgumentException("Low or high level index for truncated mean >= "
                    + commonLevels.length + " (number of levels)");
        }
        this.separateChannelsSet = Arrays.copyOf(separateChannelsSet, this.numberOfResultChannels);
    }

    public static PercentilesFinder getInstance(
            int numberOfChannels,
            int maxLabel,
            double[][] levelsByChannels,
            int lowTruncatedMeanIndex,
            int highTruncatedMeanIndex,
            boolean[] separateChannelsSet) {
        Objects.requireNonNull(separateChannelsSet, "Null separateChannelsSet");
        boolean allSeparated = true;
        int numberOfResultChannels = numberOfChannels - NUMBER_OF_SERVICE_CHANNELS;
        for (int k = 0; k < numberOfResultChannels; k++) {
            allSeparated &= k < separateChannelsSet.length && separateChannelsSet[k];
        }
        if (allSeparated) {
            return new PercentilesFinderForSeparateChannels(
                    numberOfResultChannels,
                    numberOfResultChannels,
                    maxLabel,
                    levelsByChannels,
                    lowTruncatedMeanIndex,
                    highTruncatedMeanIndex);
        }
        switch (numberOfResultChannels) {
            case 1: {
                return new PercentilesFinderByLastChannelFor1ResultChannels(
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
            case 2: {
                return new PercentilesFinderByLastChannelFor2ResultChannels(
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
            case 3: {
                return new PercentilesFinderByLastChannelFor3ResultChannels(
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
            case 4: {
                return new PercentilesFinderByLastChannelFor4ResultChannels(
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
            case 5: {
                return new PercentilesFinderByLastChannelFor5ResultChannels(
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
            default: {
                return new PercentilesFinderByLastChannel(
                        numberOfChannels,
                        maxLabel,
                        levelsByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelsSet);
            }
        }
    }

    @Override
    public void preprocess(Class<?> elementType, int numberOfTasks) {
        super.preprocess(elementType, numberOfTasks);
    }

    /*Repeat() \/\/\s*byte version start.*?\/\/\s*byte version end\s* ==> ,, ,, ,, ;;
               byte ==> short,,int,,float,,double;;
               (\s*)\& 0xFF ==> $1& 0xFFFF,, ,,... */

    @Override
    public void processPixels(int objectLabel, byte[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                for (int k = 0; k < commonLevels.length; k++) {
                    percentilesByChannels[c][k][resultDisp] = Float.NaN;
                }
                if (needTruncatedMeans) {
                    truncatedMeansByChannels[c][resultDisp] = Float.NaN;
                }
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

        final byte[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final byte[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final byte selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][k][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            final byte preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final byte preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow & 0xFF;
            final double high = preciseHigh & 0xFF;
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final byte precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }

    void processSeparatedPercentiles(
            int resultDisp,
            byte[][] objectPixelsByChannels,
            int numberOfPixels,
            int threadIndex) {
        // byte version start
        if (numberOfPixels >= MIN_LENGTH_FOR_USING_BYTE_SELECTOR) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (separateChannelsSet[c]) {
                    super.percentilesInChannelForBytes(
                            resultDisp,
                            objectPixelsByChannels[c],
                            numberOfPixels,
                            this.percentilesByChannels[c],
                            needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                            this.sortedLevels[c],
                            this.unsortedLevelsIndexes[c],
                            this.threadBytePercentiles[threadIndex],
                            this.threadByteArraySelectors[threadIndex]);
                }
            }
            return;
        }
        // byte version end
        for (int c = 0; c < numberOfResultChannels; c++) {
            if (separateChannelsSet[c]) {
                super.percentilesInChannel(
                        resultDisp,
                        objectPixelsByChannels[c],
                        numberOfPixels,
                        this.percentilesByChannels[c],
                        needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                        this.levels[c],
                        this.sortedLevels[c]);
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
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                for (int k = 0; k < commonLevels.length; k++) {
                    percentilesByChannels[c][k][resultDisp] = Float.NaN;
                }
                if (needTruncatedMeans) {
                    truncatedMeansByChannels[c][resultDisp] = Float.NaN;
                }
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final short[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final short[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final short selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFFFF;
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][k][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            final short preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final short preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow & 0xFFFF;
            final double high = preciseHigh & 0xFFFF;
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final short precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFFFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFFFF;
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j] & 0xFFFF;
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j] & 0xFFFF;
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }

    void processSeparatedPercentiles(
            int resultDisp,
            short[][] objectPixelsByChannels,
            int numberOfPixels,
            int threadIndex) {
        for (int c = 0; c < numberOfResultChannels; c++) {
            if (separateChannelsSet[c]) {
                super.percentilesInChannel(
                        resultDisp,
                        objectPixelsByChannels[c],
                        numberOfPixels,
                        this.percentilesByChannels[c],
                        needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                        this.levels[c],
                        this.sortedLevels[c]);
            }
        }
    }

    @Override
    public void processPixels(int objectLabel, int[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                for (int k = 0; k < commonLevels.length; k++) {
                    percentilesByChannels[c][k][resultDisp] = Float.NaN;
                }
                if (needTruncatedMeans) {
                    truncatedMeansByChannels[c][resultDisp] = Float.NaN;
                }
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final int[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final int[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final int selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][k][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            final int preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final int preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final int precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j];
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j];
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }

    void processSeparatedPercentiles(
            int resultDisp,
            int[][] objectPixelsByChannels,
            int numberOfPixels,
            int threadIndex) {
        for (int c = 0; c < numberOfResultChannels; c++) {
            if (separateChannelsSet[c]) {
                super.percentilesInChannel(
                        resultDisp,
                        objectPixelsByChannels[c],
                        numberOfPixels,
                        this.percentilesByChannels[c],
                        needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                        this.levels[c],
                        this.sortedLevels[c]);
            }
        }
    }

    @Override
    public void processPixels(int objectLabel, float[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                for (int k = 0; k < commonLevels.length; k++) {
                    percentilesByChannels[c][k][resultDisp] = Float.NaN;
                }
                if (needTruncatedMeans) {
                    truncatedMeansByChannels[c][resultDisp] = Float.NaN;
                }
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final float[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final float[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final float selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][k][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            final float preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final float preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final float precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j];
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j];
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }

    void processSeparatedPercentiles(
            int resultDisp,
            float[][] objectPixelsByChannels,
            int numberOfPixels,
            int threadIndex) {
        for (int c = 0; c < numberOfResultChannels; c++) {
            if (separateChannelsSet[c]) {
                super.percentilesInChannel(
                        resultDisp,
                        objectPixelsByChannels[c],
                        numberOfPixels,
                        this.percentilesByChannels[c],
                        needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                        this.levels[c],
                        this.sortedLevels[c]);
            }
        }
    }

    @Override
    public void processPixels(int objectLabel, double[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }
        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        if (numberOfPixels == 0) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                for (int k = 0; k < commonLevels.length; k++) {
                    percentilesByChannels[c][k][resultDisp] = Float.NaN;
                }
                if (needTruncatedMeans) {
                    truncatedMeansByChannels[c][resultDisp] = Float.NaN;
                }
            }
            return;
        }
        processSeparatedPercentiles(resultDisp, objectPixelsByChannels, numberOfPixels, threadIndex);
        final double[] unchangedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels];
        final double[] sortedObjectPixelLevels = objectPixelsByChannels[numberOfResultChannels + 1];
        // - will be partially sorted after "select" call
        ArraySelector.getQuickSelector().select(commonSortedLevels, sortedObjectPixelLevels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
        for (int k = 0; k < commonLevels.length; k++) {
            if (needTruncatedMeans && (k == lowTruncatedMeanIndex || k == highTruncatedMeanIndex)) {
                continue;
            }
            final double selectedLevel = sortedObjectPixelLevels[
                    ArraySelector.percentileIndex(commonLevels[k], numberOfPixels)];
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][k][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }

        if (needTruncatedMeans) {
            final double preciseLow = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[lowTruncatedMeanIndex], numberOfPixels)];
            final double preciseHigh = sortedObjectPixelLevels[ArraySelector.percentileIndex(
                    commonLevels[highTruncatedMeanIndex], numberOfPixels)];
            final double low = preciseLow;
            final double high = preciseHigh;
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final double precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j];
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j];
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j];
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }

    void processSeparatedPercentiles(
            int resultDisp,
            double[][] objectPixelsByChannels,
            int numberOfPixels,
            int threadIndex) {
        for (int c = 0; c < numberOfResultChannels; c++) {
            if (separateChannelsSet[c]) {
                super.percentilesInChannel(
                        resultDisp,
                        objectPixelsByChannels[c],
                        numberOfPixels,
                        this.percentilesByChannels[c],
                        needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                        this.levels[c],
                        this.sortedLevels[c]);
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

        double[] sum = new double[numberOfResultChannels];
        double[] sumLow = new double[numberOfResultChannels];
        double[] sumHigh = new double[numberOfResultChannels];
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
            Arrays.fill(sum, 0.0);
            int count = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                if (unchangedObjectPixelLevels[j] == selectedLevel) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    count++;
                }
            }
            final double countInv = 1.0 / count;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][unsortedIndex][resultDisp] = (float) (sum[c] * countInv);
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
            Arrays.fill(sum, 0.0);
            Arrays.fill(sumLow, 0.0);
            Arrays.fill(sumHigh, 0.0);
            int count = 0;
            int countLow = 0;
            int countHigh = 0;
            for (int j = 0; j < numberOfPixels; j++) {
                final byte precisePixelLevel = unchangedObjectPixelLevels[j];
                final double pixelLevel = precisePixelLevel & 0xFF;
                if (pixelLevel >= low && pixelLevel <= high) {
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sum[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    count++;
                }
                if (precisePixelLevel == preciseLow) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumLow[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    countLow++;
                }
                if (precisePixelLevel == preciseHigh) {
                    // in particular, when low > high
                    for (int c = 0; c < numberOfResultChannels; c++) {
                        sumHigh[c] += objectPixelsByChannels[c][j] & 0xFF;
                    }
                    countHigh++;
                }
            }
            final double countInv = 1.0 / count;
            final double countLowInv = 1.0 / countLow;
            final double countHighInv = 1.0 / countHigh;
            for (int c = 0; c < numberOfResultChannels; c++) {
                if (!separateChannelsSet[c]) {
                    percentilesByChannels[c][lowTruncatedMeanIndex][resultDisp] = (float) (sumLow[c] * countLowInv);
                    percentilesByChannels[c][highTruncatedMeanIndex][resultDisp] = (float) (sumHigh[c] * countHighInv);
                    truncatedMeansByChannels[c][resultDisp] = (float) (sum[c] * countInv);
                }
            }
        }
    }
}
