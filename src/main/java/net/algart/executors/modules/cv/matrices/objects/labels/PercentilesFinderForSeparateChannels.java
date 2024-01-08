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

import java.util.Objects;

class PercentilesFinderForSeparateChannels implements PercentilesFinder {
    private static final boolean ACCURATE_TRUNCATED_MEAN = true;
    // - true value improves accuracy, but slows down processing (very little)
    static final int MIN_LENGTH_FOR_USING_BYTE_SELECTOR = 128;
    // - replace with Integer.MAX_VALUE to disable optimization

    final float[][][] percentilesByChannels;
    final float[][] truncatedMeansByChannels;

    private final int numberOfChannels;
    final int numberOfResultChannels;
    final double[][] levels;
    final double[][] sortedLevels;
    final int[][] unsortedLevelsIndexes;
    final boolean needTruncatedMeans;
    final int lowTruncatedMeanIndex;
    final int highTruncatedMeanIndex;
    byte[][] threadBytePercentiles;
    ByteArraySelector[] threadByteArraySelectors;

    PercentilesFinderForSeparateChannels(
            int numberOfChannels,
            int numberOfResultChannels,
            int maxLabel,
            double[][] levelsByChannels,
            int lowTruncatedMeanIndex,
            int highTruncatedMeanIndex) {
        Objects.requireNonNull(levelsByChannels, "Null levels");
        if (numberOfResultChannels <= 0) {
            throw new IllegalArgumentException("Zero or negative number of actual channels");
        }
        if (numberOfChannels < numberOfResultChannels) {
            throw new IllegalArgumentException("Number of channels (" + numberOfChannels
                    + ") must be >= number of actual channels (" + numberOfResultChannels + ")") ;
        }
        if (levelsByChannels.length < numberOfResultChannels) {
            throw new IllegalArgumentException("Percentile levels are specified only for " + levelsByChannels.length
                    + " channels from " + numberOfResultChannels);
        }
        this.needTruncatedMeans = lowTruncatedMeanIndex >= 0 && highTruncatedMeanIndex >= 0;
        this.lowTruncatedMeanIndex = lowTruncatedMeanIndex;
        this.highTruncatedMeanIndex = highTruncatedMeanIndex;
        this.truncatedMeansByChannels = needTruncatedMeans ? new float[numberOfResultChannels][maxLabel] : null;
        this.numberOfChannels = numberOfChannels;
        this.numberOfResultChannels = numberOfResultChannels;
        this.percentilesByChannels = new float[numberOfResultChannels][][];
        this.levels = new double[numberOfResultChannels][];
        this.sortedLevels = new double[numberOfResultChannels][];
        this.unsortedLevelsIndexes = new int[numberOfResultChannels][];
        for (int c = 0; c < numberOfResultChannels; c++) {
            this.levels[c] = levelsByChannels[c].clone();
            final double[] sorted = this.levels[c].clone();
            final int[] indexes = new int[sorted.length];
            JArrays.fillIntProgression(indexes, 0, 1);
            ArraySorter.getQuickSorter().sort(0, sorted.length,
                    new JArrays.DoubleArrayComparator(sorted),
                    new JArrays.DoubleAndIndexArrayExchanger(sorted, indexes));
            for (int k = 0; k < indexes.length; k++) {
                assert this.levels[c][indexes[k]] == sorted[k];
            }
            this.sortedLevels[c] = sorted;
            this.unsortedLevelsIndexes[c] = indexes;
            this.percentilesByChannels[c] = new float[sorted.length][maxLabel];
        }
    }

    @Override
    public float[][][] percentilesByChannels() {
        return percentilesByChannels;
    }

    @Override
    public float[][] truncatedMeansByChannels() {
        return truncatedMeansByChannels;
    }

    @Override
    public void preprocess(Class<?> elementType, int numberOfTasks) {
        if (elementType == byte.class) {
            int maxNumberOfLevels = 0;
            for (int c = 0; c < numberOfResultChannels; c++) {
                maxNumberOfLevels = Math.max(maxNumberOfLevels, this.levels[c].length);
            }
            this.threadBytePercentiles = new byte[numberOfTasks][maxNumberOfLevels];
            this.threadByteArraySelectors = new ByteArraySelector[numberOfTasks];
            for (int k = 0; k < numberOfTasks; k++) {
                this.threadByteArraySelectors[k] = new ByteArraySelector();
            }
        }
    }

    /*Repeat() \/\/\s*byte version start.*?\/\/\s*byte version end\s* ==> ,, ,, ,, ;;
               byte   ==> short,,int,,float,,double;;
               (objectPixels\[\w+\]) \& 0xFF ==> $1 & 0xFFFF,,$1,,$1,,$1
    */
    @Override
    public void processPixels(int objectLabel, byte[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        // Note that objectPixelsByChannels.length can be > numberOfResultChannels,
        // when this class is created as an optimized equivalent of other processor.
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }

        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        // byte version start
        if (numberOfPixels >= MIN_LENGTH_FOR_USING_BYTE_SELECTOR) {
            for (int c = 0; c < numberOfResultChannels; c++) {
                percentilesInChannelForBytes(
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
            return;
        }
        // byte version end
        for (int c = 0; c < numberOfResultChannels; c++) {
            percentilesInChannel(
                    resultDisp,
                    objectPixelsByChannels[c],
                    numberOfPixels,
                    this.percentilesByChannels[c],
                    needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                    this.levels[c],
                    this.sortedLevels[c]);
        }
    }

    void percentilesInChannel(
            final int resultDisp,
            final byte[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] levels,
            final double[] sortedLevels) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < levels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        ArraySelector.getQuickSelector().select(sortedLevels, objectPixels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        int indexLow = -1;
        int indexHigh = -1;
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], numberOfPixels);
            if (k == lowTruncatedMeanIndex) {
                indexLow = percentileIndex;
            } else if (k == highTruncatedMeanIndex) {
                indexHigh = percentileIndex;
            }
            percentiles[k][resultDisp] = (float) (objectPixels[percentileIndex] & 0xFF);
        }
        if (needTruncatedMeans) {
            double mean;
            if (indexLow == -1) {
                mean = Double.NaN;
            } else if (indexHigh == -1 || indexHigh == indexLow) {
                mean = objectPixels[indexLow] & 0xFF;
            } else {
                double low = objectPixels[indexLow] & 0xFF;
                double high = objectPixels[indexHigh] & 0xFF;

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    if (ACCURATE_TRUNCATED_MEAN) {
                        mean = 0.0;
                        int count = 0;
                        for (int k = 0; k < numberOfPixels; k++) {
                            final double v = objectPixels[k] & 0xFF;
                            if (v >= low && v <= high) {
                                mean += v;
                                count++;
                            }
                        }
                        mean /= (double) count;
                    } else {
                        if (indexLow > indexHigh) {
                            mean = Double.NaN;
                            // - in similar situation, count==0 above and we have 0.0/0.0
                        } else {
                            mean = 0.0;
                            for (int k = indexLow; k <= indexHigh; k++) {
                                mean += objectPixels[k] & 0xFF;
                            }
                            mean /= (double) (indexHigh - indexLow + 1);
                        }
                    }
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    @Override
    public void processPixels(int objectLabel, short[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        // Note that objectPixelsByChannels.length can be > numberOfResultChannels,
        // when this class is created as an optimized equivalent of other processor.
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }

        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        for (int c = 0; c < numberOfResultChannels; c++) {
            percentilesInChannel(
                    resultDisp,
                    objectPixelsByChannels[c],
                    numberOfPixels,
                    this.percentilesByChannels[c],
                    needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                    this.levels[c],
                    this.sortedLevels[c]);
        }
    }

    void percentilesInChannel(
            final int resultDisp,
            final short[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] levels,
            final double[] sortedLevels) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < levels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        ArraySelector.getQuickSelector().select(sortedLevels, objectPixels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        int indexLow = -1;
        int indexHigh = -1;
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], numberOfPixels);
            if (k == lowTruncatedMeanIndex) {
                indexLow = percentileIndex;
            } else if (k == highTruncatedMeanIndex) {
                indexHigh = percentileIndex;
            }
            percentiles[k][resultDisp] = (float) (objectPixels[percentileIndex] & 0xFFFF);
        }
        if (needTruncatedMeans) {
            double mean;
            if (indexLow == -1) {
                mean = Double.NaN;
            } else if (indexHigh == -1 || indexHigh == indexLow) {
                mean = objectPixels[indexLow] & 0xFFFF;
            } else {
                double low = objectPixels[indexLow] & 0xFFFF;
                double high = objectPixels[indexHigh] & 0xFFFF;

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    if (ACCURATE_TRUNCATED_MEAN) {
                        mean = 0.0;
                        int count = 0;
                        for (int k = 0; k < numberOfPixels; k++) {
                            final double v = objectPixels[k] & 0xFFFF;
                            if (v >= low && v <= high) {
                                mean += v;
                                count++;
                            }
                        }
                        mean /= (double) count;
                    } else {
                        if (indexLow > indexHigh) {
                            mean = Double.NaN;
                            // - in similar situation, count==0 above and we have 0.0/0.0
                        } else {
                            mean = 0.0;
                            for (int k = indexLow; k <= indexHigh; k++) {
                                mean += objectPixels[k] & 0xFFFF;
                            }
                            mean /= (double) (indexHigh - indexLow + 1);
                        }
                    }
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }


    @Override
    public void processPixels(int objectLabel, int[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        // Note that objectPixelsByChannels.length can be > numberOfResultChannels,
        // when this class is created as an optimized equivalent of other processor.
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }

        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        for (int c = 0; c < numberOfResultChannels; c++) {
            percentilesInChannel(
                    resultDisp,
                    objectPixelsByChannels[c],
                    numberOfPixels,
                    this.percentilesByChannels[c],
                    needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                    this.levels[c],
                    this.sortedLevels[c]);
        }
    }

    void percentilesInChannel(
            final int resultDisp,
            final int[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] levels,
            final double[] sortedLevels) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < levels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        ArraySelector.getQuickSelector().select(sortedLevels, objectPixels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        int indexLow = -1;
        int indexHigh = -1;
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], numberOfPixels);
            if (k == lowTruncatedMeanIndex) {
                indexLow = percentileIndex;
            } else if (k == highTruncatedMeanIndex) {
                indexHigh = percentileIndex;
            }
            percentiles[k][resultDisp] = (float) (objectPixels[percentileIndex]);
        }
        if (needTruncatedMeans) {
            double mean;
            if (indexLow == -1) {
                mean = Double.NaN;
            } else if (indexHigh == -1 || indexHigh == indexLow) {
                mean = objectPixels[indexLow];
            } else {
                double low = objectPixels[indexLow];
                double high = objectPixels[indexHigh];

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    if (ACCURATE_TRUNCATED_MEAN) {
                        mean = 0.0;
                        int count = 0;
                        for (int k = 0; k < numberOfPixels; k++) {
                            final double v = objectPixels[k];
                            if (v >= low && v <= high) {
                                mean += v;
                                count++;
                            }
                        }
                        mean /= (double) count;
                    } else {
                        if (indexLow > indexHigh) {
                            mean = Double.NaN;
                            // - in similar situation, count==0 above and we have 0.0/0.0
                        } else {
                            mean = 0.0;
                            for (int k = indexLow; k <= indexHigh; k++) {
                                mean += objectPixels[k];
                            }
                            mean /= (double) (indexHigh - indexLow + 1);
                        }
                    }
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }


    @Override
    public void processPixels(int objectLabel, float[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        // Note that objectPixelsByChannels.length can be > numberOfResultChannels,
        // when this class is created as an optimized equivalent of other processor.
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }

        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        for (int c = 0; c < numberOfResultChannels; c++) {
            percentilesInChannel(
                    resultDisp,
                    objectPixelsByChannels[c],
                    numberOfPixels,
                    this.percentilesByChannels[c],
                    needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                    this.levels[c],
                    this.sortedLevels[c]);
        }
    }

    void percentilesInChannel(
            final int resultDisp,
            final float[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] levels,
            final double[] sortedLevels) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < levels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        ArraySelector.getQuickSelector().select(sortedLevels, objectPixels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        int indexLow = -1;
        int indexHigh = -1;
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], numberOfPixels);
            if (k == lowTruncatedMeanIndex) {
                indexLow = percentileIndex;
            } else if (k == highTruncatedMeanIndex) {
                indexHigh = percentileIndex;
            }
            percentiles[k][resultDisp] = (float) (objectPixels[percentileIndex]);
        }
        if (needTruncatedMeans) {
            double mean;
            if (indexLow == -1) {
                mean = Double.NaN;
            } else if (indexHigh == -1 || indexHigh == indexLow) {
                mean = objectPixels[indexLow];
            } else {
                double low = objectPixels[indexLow];
                double high = objectPixels[indexHigh];

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    if (ACCURATE_TRUNCATED_MEAN) {
                        mean = 0.0;
                        int count = 0;
                        for (int k = 0; k < numberOfPixels; k++) {
                            final double v = objectPixels[k];
                            if (v >= low && v <= high) {
                                mean += v;
                                count++;
                            }
                        }
                        mean /= (double) count;
                    } else {
                        if (indexLow > indexHigh) {
                            mean = Double.NaN;
                            // - in similar situation, count==0 above and we have 0.0/0.0
                        } else {
                            mean = 0.0;
                            for (int k = indexLow; k <= indexHigh; k++) {
                                mean += objectPixels[k];
                            }
                            mean /= (double) (indexHigh - indexLow + 1);
                        }
                    }
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }


    @Override
    public void processPixels(int objectLabel, double[][] objectPixelsByChannels, int numberOfPixels, int threadIndex) {
        // Note that objectPixelsByChannels.length can be > numberOfResultChannels,
        // when this class is created as an optimized equivalent of other processor.
        if (objectLabel == 0) {
            // Don't process level=0
            return;
        }

        final int resultDisp = objectLabel - 1;
        // Note: in the resulting "this.percentiles" we use first element (zero label)
        for (int c = 0; c < numberOfResultChannels; c++) {
            percentilesInChannel(
                    resultDisp,
                    objectPixelsByChannels[c],
                    numberOfPixels,
                    this.percentilesByChannels[c],
                    needTruncatedMeans ? this.truncatedMeansByChannels[c] : null,
                    this.levels[c],
                    this.sortedLevels[c]);
        }
    }

    void percentilesInChannel(
            final int resultDisp,
            final double[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] levels,
            final double[] sortedLevels) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < levels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        ArraySelector.getQuickSelector().select(sortedLevels, objectPixels, numberOfPixels);
        // - note: though we pass here sorted levels, the required levels will be
        // at their correct places regardless the sorting.

        int indexLow = -1;
        int indexHigh = -1;
        for (int k = 0; k < levels.length; k++) {
            int percentileIndex = ArraySelector.percentileIndex(levels[k], numberOfPixels);
            if (k == lowTruncatedMeanIndex) {
                indexLow = percentileIndex;
            } else if (k == highTruncatedMeanIndex) {
                indexHigh = percentileIndex;
            }
            percentiles[k][resultDisp] = (float) (objectPixels[percentileIndex]);
        }
        if (needTruncatedMeans) {
            double mean;
            if (indexLow == -1) {
                mean = Double.NaN;
            } else if (indexHigh == -1 || indexHigh == indexLow) {
                mean = objectPixels[indexLow];
            } else {
                double low = objectPixels[indexLow];
                double high = objectPixels[indexHigh];

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    if (ACCURATE_TRUNCATED_MEAN) {
                        mean = 0.0;
                        int count = 0;
                        for (int k = 0; k < numberOfPixels; k++) {
                            final double v = objectPixels[k];
                            if (v >= low && v <= high) {
                                mean += v;
                                count++;
                            }
                        }
                        mean /= (double) count;
                    } else {
                        if (indexLow > indexHigh) {
                            mean = Double.NaN;
                            // - in similar situation, count==0 above and we have 0.0/0.0
                        } else {
                            mean = 0.0;
                            for (int k = indexLow; k <= indexHigh; k++) {
                                mean += objectPixels[k];
                            }
                            mean /= (double) (indexHigh - indexLow + 1);
                        }
                    }
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    void percentilesInChannelForBytes(
            final int resultDisp,
            final byte[] objectPixels,
            final int numberOfPixels,
            final float[][] percentiles,
            final float[] truncatedMeans,
            final double[] sortedLevels,
            final int[] unsortedLevelsIndexes,
            final byte[] bytePercentiles,
            ByteArraySelector selector) {
        if (levels.length == 0) {
            // Nothing to do: no percentiles requires for this channel
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }
        if (numberOfPixels == 0) {
            for (int k = 0; k < sortedLevels.length; k++) {
                percentiles[k][resultDisp] = Float.NaN;
            }
            if (needTruncatedMeans) {
                truncatedMeans[resultDisp] = Float.NaN;
            }
            return;
        }

        selector.select(bytePercentiles, sortedLevels, objectPixels, numberOfPixels);

        int kLow = -1;
        int kHigh = -1;
        for (int k = 0; k < sortedLevels.length; k++) {
            int unsortedIndex = unsortedLevelsIndexes[k];
            if (unsortedIndex == lowTruncatedMeanIndex) {
                kLow = k;
            } else if (unsortedIndex == highTruncatedMeanIndex) {
                kHigh = k;
            }
            percentiles[unsortedIndex][resultDisp] = (float) (bytePercentiles[k] & 0xFF);
        }
        if (needTruncatedMeans) {
            double mean;
            if (kLow == -1) {
                mean = Double.NaN;
            } else if (kHigh == -1 || kHigh == kLow) {
                mean = bytePercentiles[kLow] & 0xFF;
            } else {
                double low = bytePercentiles[kLow] & 0xFF;
                double high = bytePercentiles[kHigh] & 0xFF;

                if (low > high) {
                    mean = Double.NaN;
                } else if (low == high) {
                    mean = low;
                    // - important if !ACCURATE_TRUNCATED_MEAN: in this case, we can find
                    // any low/high indexes among equal values
                } else {
                    mean = 0.0;
                    int count = 0;
                    for (int k = 0; k < numberOfPixels; k++) {
                        final double v = objectPixels[k] & 0xFF;
                        if (v >= low && v <= high) {
                            mean += v;
                            count++;
                        }
                    }
                    mean /= (double) count;
                }
            }
            truncatedMeans[resultDisp] = (float) mean;
        }
    }
}
