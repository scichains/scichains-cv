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
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LabelsAnalyser {
    private int[] labels;
    private boolean labelsMustBeImmutable;
    private Object[] channels;
    private Object[] channelsForPercentiles;
    private double[][] percentileLevelByChannels = new double[0][0];
    private int maxNumberOfPercentileLevels = 0;
    private boolean needTruncatedMeans = false;
    private boolean useCommonLevelChannelForPercentiles = false;
    private boolean[] separateChannelPercentilesSet = new boolean[0];
    private int lowTruncatedMeanIndex = 0;
    private int highTruncatedMeanIndex = 0;
    private Class<?> elementType;
    private double scale;

    private int maxLabel = Integer.MIN_VALUE;
    private int[] cardinalities;
    private double[] sums;
    private double[] sumsOfSquares;
    private int[] lists;
    private int[] listHeads;
    private float[][][] percentilesByChannels;
    private float[][] groupedPercentilesByLevels;
    private float[] truncatedMeans;
    private int[] firstNonZeroIndexes;
    private int[] firstNonZeroIntValues;
    private float[] firstNonZeroFloatValues;

    private IntJArrayHolder labelsHolder = new IntJArrayHolder();
    private IntJArrayHolder listsHolder = new IntJArrayHolder();

    public LabelsAnalyser setLabels(MultiMatrix2D labelsMatrix) {
        return setLabels(labelsMatrix, null);
    }

    public LabelsAnalyser setLabels(MultiMatrix2D labelsMatrix, MultiMatrix2D maskMatrix) {
        Objects.requireNonNull(labelsMatrix, "Null labelsMatrix");
        labelsMatrix.checkDimensionEquality(maskMatrix, "labels", "mask");
        final Matrix<? extends PArray> labelsChannel = labelsMatrix.channel(0);
        PArray labelsArray;
        int[] labels = null;
        if ((labelsArray = labelsChannel.array()).elementType() == int.class
                && labelsArray instanceof DirectAccessible da) {
            if (da.hasJavaArray() && da.javaArrayOffset() == 0) {
                // - non-zero offset is very improbable for matrices
                labels = (int[]) da.javaArray();
            }
        }
        boolean labelsMustBeImmutable = labels != null;
        if (labels == null) {
            labels = Matrices.toIntJavaArray(labelsHolder.quickNew(labelsChannel), labelsChannel);
        }
        if (maskMatrix != null) {
            BitArray maskArray = maskMatrix.nonZeroPixelsMatrix(false).array();
            if (labelsMustBeImmutable) {
                labels = labelsHolder.quickClone(labels);
                labelsMustBeImmutable = false;
            }
            Arrays.unpackZeroBits(IntArray.as(labels), maskArray, 0);
        }
        this.labels = labels;
        this.labelsMustBeImmutable = labelsMustBeImmutable;
        return this;
    }

    public LabelsAnalyser setImage(MultiMatrix2D image, boolean rawValues) {
        Objects.requireNonNull(image, "Null image");
        this.channels = retrieveChannelsOrFloats(image);
        this.channelsForPercentiles = this.channels;
        this.useCommonLevelChannelForPercentiles = false;
        this.elementType = channels[0].getClass().getComponentType();
        this.scale = rawValues ? 1.0 : 1.0 / image.maxPossibleValue();
        return this;
    }

    public LabelsAnalyser setImageAndLevelMatrix(
            MultiMatrix2D image,
            Matrix<? extends PArray> levels,
            boolean rawValues) {
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(levels, "Null levels");
        image.checkDimensionEquality(MultiMatrix.valueOf2DMono(levels),
                "image", "levels");
        List<Matrix<? extends PArray>> channelList = new ArrayList<>(image.allChannels());
        channelList.add(levels);
        channelList.add(levels);
        // - we add the same reference twice: it will lead to creating two copies of levels,
        // almost without spending extra time
        final boolean forceFloat = levels.elementType() != image.elementType();
        this.channelsForPercentiles = retrieveChannelsOrFloats(channelList, forceFloat);
        this.channels = java.util.Arrays.copyOfRange(
                channelsForPercentiles, 0, channelsForPercentiles.length - 2);
        this.useCommonLevelChannelForPercentiles = true;
        this.elementType = this.channels[0].getClass().getComponentType();
        this.scale = rawValues ? 1.0 : 1.0 / image.maxPossibleValue();
        return this;
    }

    public LabelsAnalyser setSeparateChannelPercentilesSet(boolean[] separateChannelPercentilesSet) {
        Objects.requireNonNull(separateChannelPercentilesSet, "Null separateChannelPercentilesSet");
        this.separateChannelPercentilesSet = separateChannelPercentilesSet.clone();
        return this;
    }

    public void findCardinalities() {
        checkInitialized();
        try (CardinalitiesCalculator processor = new CardinalitiesCalculator(labels)) {
            processor.process();
            this.maxLabel = processor.maxLabel;
            this.cardinalities = processor.cardinalities;
        }
    }

    public void findMeansAndCardinalities() {
        checkImageInitialized();
        try (CardinalitiesAndSumsCalculator processor = CardinalitiesAndSumsCalculator.getInstance(labels, channels)) {
            processor.process();
            this.maxLabel = processor.maxLabel;
            this.cardinalities = processor.cardinalities;
            this.sums = processor.sums;
            scaleValues(this.sums, scale);
        }
    }

    public void findMeansAndStandardDeviationsAndCardinalities() {
        checkImageInitialized();
        try (CardinalitiesAndSumsOfSquaresCalculator processor =
                     CardinalitiesAndSumsOfSquaresCalculator.getInstance(labels, channels)) {
            processor.process();
            this.maxLabel = processor.maxLabel;
            this.cardinalities = processor.cardinalities;
            this.sums = processor.sums;
            this.sumsOfSquares = processor.sumsOfSquares;
            scaleValues(this.sums, scale);
            scaleValues(this.sumsOfSquares, scale * scale);
        }
    }

    public void prepareLists() {
        checkInitialized();
        this.lists = listsHolder.quickNew(labels.length);
        try (LabelsListsBuilder processor = LabelsListsBuilder.getInstance(labels, lists)) {
            processor.process();
            this.maxLabel = processor.maxLabel();
            this.listHeads = processor.listHeads();
        }
    }

    public LabelsAnalyser setPercentileLevelByChannels(double[][] percentileLevelByChannels) {
        Objects.requireNonNull(percentileLevelByChannels, "Null percentileLevelsForChannels");
        this.percentileLevelByChannels = percentileLevelByChannels.clone();
        int maxNumber = 0;
        for (int k = 0; k < percentileLevelByChannels.length; k++) {
            this.percentileLevelByChannels[k] = this.percentileLevelByChannels[k].clone();
            maxNumber = Math.max(maxNumber, this.percentileLevelByChannels[k].length);
        }
        this.maxNumberOfPercentileLevels = maxNumber;
        return this;
    }

    public LabelsAnalyser setNeedTruncatedMeans(boolean needTruncatedMeans) {
        this.needTruncatedMeans = needTruncatedMeans;
        return this;
    }

    public LabelsAnalyser setLowTruncatedMeanIndex(int lowTruncatedMeanIndex) {
        this.lowTruncatedMeanIndex = lowTruncatedMeanIndex;
        return this;
    }

    public LabelsAnalyser setHighTruncatedMeanIndex(int highTruncatedMeanIndex) {
        this.highTruncatedMeanIndex = highTruncatedMeanIndex;
        return this;
    }

    public void findPercentilesAndCardinalities() {
        checkImageInitialized();
        checkListsInitialized();
        final int lowTruncatedMeanIndex = needTruncatedMeans ? this.lowTruncatedMeanIndex : -1;
        final int highTruncatedMeanIndex = needTruncatedMeans ? this.highTruncatedMeanIndex : -1;
        try (PercentilesFinder percentilesFinder = useCommonLevelChannelForPercentiles ?
                PercentilesFinderByLastChannel.getInstance(
                        channelsForPercentiles.length,
                        maxLabel,
                        percentileLevelByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex,
                        separateChannelPercentilesSet) :
                new PercentilesFinderForSeparateChannels(
                        channelsForPercentiles.length,
                        channelsForPercentiles.length,
                        maxLabel,
                        percentileLevelByChannels,
                        lowTruncatedMeanIndex,
                        highTruncatedMeanIndex)) {
            try (LabelledObjectsProcessor processor = LabelledObjectsProcessor.getInstance(
                    lists, listHeads, percentilesFinder, channelsForPercentiles)) {
                percentilesFinder.preprocess(elementType, processor.numberOfTasks());
                processor.process();
                this.cardinalities = processor.cardinalities;
                this.percentilesByChannels = percentilesFinder.percentilesByChannels();
                this.groupedPercentilesByLevels = new float[maxNumberOfPercentileLevels][];
                float[][] percentileForSingleLevel = new float[percentilesByChannels.length][];
                for (int p = 0; p < maxNumberOfPercentileLevels; p++) {
                    for (int c = 0; c < percentilesByChannels.length; c++) {
                        if (p < percentilesByChannels[c].length) {
                            percentileForSingleLevel[c] = percentilesByChannels[c][p];
                        } else {
                            percentileForSingleLevel[c] = new float[maxLabel];
                            JArrays.fillFloatArray(percentileForSingleLevel[c], Float.NaN);
                        }
                    }
                    this.groupedPercentilesByLevels[p] = combineMultiChannel(percentileForSingleLevel, scale);
                }
                float[][] truncatedMeansByChannels = percentilesFinder.truncatedMeansByChannels();
                this.truncatedMeans = truncatedMeansByChannels != null ?
                        combineMultiChannel(truncatedMeansByChannels, scale) :
                        null;
            }
        }
    }

    public void findFirstNonZeroPixels() {
        checkImageInitialized();
        try (FirstNonZeroCalculator processor = FirstNonZeroCalculator.getInstance(labels, channels)) {
            processor.process();
            this.maxLabel = processor.maxLabel;
            this.cardinalities = processor.cardinalities;
            this.firstNonZeroIndexes = processor.firstNonZeroIndexes;
            this.firstNonZeroIntValues = processor.firstNonZeroIntValues;
            this.firstNonZeroFloatValues = processor.firstNonZeroFloatValues;
        }
    }

    /**
     * Returns maximal label index. It contains a correct value after <i>any</i> of <tt>findXxx</tt> methods.
     * The number of elements in arrays with parameters of objects, returned by this class,
     * like {@link #cardinalities()} or {@link #listHeads()}, is equal to this value;
     * information about object with given <tt>label</tt> is stored in element with index <tt>label-1</tt>
     * (<tt>label=0</tt> is impossible and skipped).
     *
     * @return maximal label index.
     */
    public int maxLabel() {
        return maxLabel;
    }

    /**
     * Returns labels array.
     *
     * <p>Warning: if {@link #unsafeLabelsMustBeImmutable()} returns <tt>true</tt>, you <b>must not</b>
     * modify the content of the returned array!
     *
     * @return labels array.
     */
    public int[] unsafeLabels() {
        return labels;
    }

    public boolean unsafeLabelsMustBeImmutable() {
        return labelsMustBeImmutable;
    }

    public int[] labelsWithCloningIfNecessary() {
        return labelsMustBeImmutable ? labels.clone() : labels;
    }

    public boolean isReadyCardinalities() {
        return cardinalities != null;
    }

    public int[] cardinalities() {
        return cardinalities;
    }

    public boolean isReadySums() {
        return sums != null;
    }

    public double[] sums() {
        return sums;
    }

    public float[] means() {
        return averageValues(this.sums, this.cardinalities, this.channels.length);
    }

    public boolean isReadySumsOfSquares() {
        return sumsOfSquares != null;
    }

    public double[] sumOfSquares() {
        return sumsOfSquares;
    }

    public float[] meanSquares() {
        return averageValues(this.sumsOfSquares, this.cardinalities, this.channels.length);
    }

    public float[] standardDeviations() {
        return standardDeviations(this.sums, this.sumsOfSquares, this.cardinalities, this.channels.length);
    }

    public boolean isReadyLists() {
        return lists != null;
    }

    public int[] lists() {
        return lists;
    }

    public int[] listHeads() {
        return listHeads;
    }

    public boolean isReadyPercentiles() {
        return percentilesByChannels != null;
    }

    public float[][][] percentilesByChannels() {
        return percentilesByChannels;
    }

    public float[][] percentiles(int channelIndex) {
        return percentilesByChannels[channelIndex];
    }

    public float[] groupedPercentilesByLevel(int levelIndex) {
        return levelIndex < groupedPercentilesByLevels.length ? groupedPercentilesByLevels[levelIndex] : null;
    }

    // Note: requires some time
    public float[] percentilesRange(int percentileIndex1, int percentileIndex2) {
        final float[] percentiles1 = groupedPercentilesByLevel(percentileIndex1);
        final float[] percentiles2 = groupedPercentilesByLevel(percentileIndex2);
        if (percentiles1 == null || percentiles2 == null) {
            return null;
        }
        assert percentiles1.length == percentiles2.length;
        float[] result = new float[percentiles1.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = percentiles2[k] - percentiles1[k];
        }
        return result;
    }

    public boolean isReadyTruncatedMeans() {
        return truncatedMeans != null;
    }

    public float[] truncatedMeans() {
        return truncatedMeans;
    }

    public boolean isReadyFirstNonZeroInformation() {
        return firstNonZeroIndexes != null;
    }

    public int[] firstNonZeroIndexes() {
        return firstNonZeroIndexes;
    }

    public boolean isFirstNonZeroValuesInteger() {
        return firstNonZeroIntValues != null;
    }

    public int[] firstNonZeroIntValues() {
        return firstNonZeroIntValues;
    }

    public float[] firstNonZeroFloatValues() {
        return firstNonZeroFloatValues;
    }

    public Object firstNonZeroValues() {
        return firstNonZeroIntValues != null ? firstNonZeroIntValues : firstNonZeroFloatValues;
    }

    public float[] firstNonZeroFloatValues(boolean autoConvertFromIntValues) {
        if (autoConvertFromIntValues && firstNonZeroFloatValues == null) {
            float[] result = new float[firstNonZeroIntValues.length];
            for (int k = 0; k < result.length; k++) {
                result[k] = (float) (firstNonZeroIntValues[k] * scale);
            }
            return result;
        } else {
            return firstNonZeroFloatValues;
        }
    }

    private void checkInitialized() {
        if (labels == null) {
            throw new IllegalStateException("Object is not initialized: labels are not set");
        }
    }

    private void checkImageInitialized() {
        checkInitialized();
        if (channels == null) {
            throw new IllegalStateException("Object is not completely initialized: image for processing is not set");
        }
    }

    private void checkListsInitialized() {
        checkInitialized();
        if (!isReadyLists()) {
            throw new IllegalStateException("Lists of pixels are not built: prepareLists() was not called yet");
        }
    }

    private static Object[] retrieveChannelsOrFloats(MultiMatrix2D matrix) {
        return retrieveChannelsOrFloats(matrix.allChannels(), false);
    }

    private static Object[] retrieveChannelsOrFloats(List<Matrix<? extends PArray>> channels, boolean forceFloat) {
        boolean direct = !forceFloat;
        if (!forceFloat) {
            for (Matrix<? extends PArray> channel : channels) {
                final PArray array = channel.array();
                direct &= array instanceof DirectAccessible
                        && ((DirectAccessible) array).hasJavaArray()
                        && ((DirectAccessible) array).javaArrayOffset() == 0
                        && CardinalitiesAndSumsCalculator.isArraySupported(((DirectAccessible) array).javaArray());
                // - non-zero offset is very improbable for matrices
            }
        }
        final Object[] result = new Object[channels.size()];
        if (direct) {
            for (int k = 0; k < result.length; k++) {
                result[k] = ((DirectAccessible) channels.get(k).array()).javaArray();
            }
        } else {
            for (int k = 0; k < result.length; k++) {
                Matrix<? extends PArray> m = channels.get(k);
                if (!forceFloat && (m.elementType() == boolean.class || m.elementType() == byte.class)) {
                    result[k] = Matrices.toByteJavaArray(m);
                } else {
                    result[k] = Matrices.toFloatJavaArray(m);
                }
            }
        }
        return result;
    }

    private static float[] combineMultiChannel(float[][] channelValues, double scale) {
        final int numberOfChannels = channelValues.length;
        final int length = channelValues[0].length;
        if ((long) length * (long) numberOfChannels > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Too large required array for " + numberOfChannels
                    + " channels: more that 2^31-1 elements");
        }
        final float[] result = new float[numberOfChannels * length];
        for (int k = 0, disp = 0; k < length; k++, disp += numberOfChannels) {
            for (int c = 0; c < numberOfChannels; c++) {
                result[disp + c] = (float) (channelValues[c][k] * scale);
            }
        }
        return result;
    }

    private static void scaleValues(double[] values, double scale) {
        for (int k = 0; k < values.length; k++) {
            values[k] *= scale;
        }
    }

    private static float[] averageValues(double[] values, int[] cardinalities, int numberOfChannels) {
        assert numberOfChannels * cardinalities.length == values.length;
        final float[] result = new float[values.length];
        for (int k = 0, disp = 0; k < cardinalities.length; k++, disp += numberOfChannels) {
            for (int c = 0; c < numberOfChannels; c++) {
                result[disp + c] = (float) (values[disp + c] / cardinalities[k]);
            }
        }
        return result;
    }

    private static float[] standardDeviations(
            double[] sums,
            double[] sumsOfSquares,
            int[] cardinalities,
            int numberOfChannels) {
        assert numberOfChannels * cardinalities.length == sums.length;
        final float[] result = new float[sums.length];
        for (int k = 0, disp = 0; k < cardinalities.length; k++, disp += numberOfChannels) {
            for (int c = 0; c < numberOfChannels; c++) {
                final double mean = sums[disp + c] / cardinalities[k];
                final double meanSquare = sumsOfSquares[disp + c] / cardinalities[k];
                final double variance = Math.max(meanSquare - mean * mean, 0.0);
                result[disp + c] = (float) Math.sqrt(variance);
            }
        }
        return result;
    }
}
