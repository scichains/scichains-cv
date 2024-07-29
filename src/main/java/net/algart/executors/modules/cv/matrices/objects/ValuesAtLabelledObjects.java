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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.Arrays;
import net.algart.arrays.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.cv.matrices.objects.labels.LabelsAnalyser;
import net.algart.executors.modules.cv.matrices.objects.markers.PaintLabelledObjects;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ValuesAtLabelledObjects extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_MASK = "mask";
    public static final String INPUT_LEVEL = "level";
    public static final String OUTPUT_PAINT_LABELLED = "paint_labelled";

    public enum ObjectParameter {
        MEAN("mean"),
        MEAN_SQUARE("mean_square"),
        STANDARD_DEVIATION("standard_deviation"),
        LOW_PERCENTILE("low_percentile"),
        HIGH_PERCENTILE("high_percentile"),
        PERCENTILE_A("percentile_A"),
        PERCENTILE_B("percentile_B"),
        PERCENTILE_C("percentile_C"),
        PERCENTILES_RANGE("percentiles_range"),
        TRUNCATED_MEAN("truncated_mean"),
        CARDINALITY("cardinality"),
        FIRST_NON_ZERO("first_non_zero");

        public static final EnumSet<ObjectParameter> ALL_PERCENTILES = EnumSet.of(
                LOW_PERCENTILE,
                HIGH_PERCENTILE,
                PERCENTILE_A,
                PERCENTILE_B,
                PERCENTILE_C);

        final String outputPort;

        ObjectParameter(String outputPort) {
            this.outputPort = outputPort;
        }
    }

    private static final boolean OPTIMIZE = true;
    // - You can clear this flag to false for debugging needs, to compare results with simple, but slow code.
    // Note that percentiles A,B,C, more than 1 level per channel and different levels for different channels
    // are not available in non-optimized version.

    private static final Map<String, ObjectParameter> OUTPUT_STATISTICS = new LinkedHashMap<>();

    static {
        for (ObjectParameter parameter : ObjectParameter.values()) {
            OUTPUT_STATISTICS.put(parameter.outputPort, parameter);
        }
    }

    private boolean rawValues = false;
    private double[] lowPercentile = {0.2};
    private double[] highPercentile = {};
    private double[] percentileA = {};
    private double[] percentileB = {};
    private double[] percentileC = {};
    private boolean channelPercentiles = false;
    private int[] separateChannelPercentilesList = {};
    private int levelChannel = 0;
    private ObjectParameter paintedParameter = ObjectParameter.MEAN;
    private boolean paintLabelledOnSource = false;
    private boolean visiblePaintLabelled = false;

    private final LabelsAnalyser analyser = new LabelsAnalyser();
    // - reusing memory for labels

    public ValuesAtLabelledObjects() {
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_LABELS);
        addInputMat(INPUT_MASK);
        addInputMat(INPUT_LEVEL);
        setDefaultOutputNumbers(ObjectParameter.MEAN.outputPort);
        for (ObjectParameter parameter : ObjectParameter.values()) {
            addOutputNumbers(parameter.outputPort);
        }
        addOutputMat(OUTPUT_PAINT_LABELLED);
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public ValuesAtLabelledObjects setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public double[] getLowPercentile() {
        return lowPercentile.clone();
    }

    public ValuesAtLabelledObjects setLowPercentile(double... lowPercentile) {
        this.lowPercentile = checkPercentileLevels(lowPercentile).clone();
        return this;
    }

    public ValuesAtLabelledObjects setLowPercentile(String lowPercentile) {
        return setLowPercentile(new SScalar(nonNull(lowPercentile)).toDoubles());
    }

    public double[] getHighPercentile() {
        return highPercentile.clone();
    }

    public ValuesAtLabelledObjects setHighPercentile(double... highPercentile) {
        this.highPercentile = checkPercentileLevels(highPercentile).clone();
        return this;
    }

    public ValuesAtLabelledObjects setHighPercentile(String highPercentile) {
        return setHighPercentile(new SScalar(nonNull(highPercentile)).toDoubles());
    }

    public double[] getPercentileA() {
        return percentileA.clone();
    }

    public ValuesAtLabelledObjects setPercentileA(double[] percentileA) {
        this.percentileA = checkPercentileLevels(percentileA).clone();
        return this;
    }

    public ValuesAtLabelledObjects setPercentileA(String percentileA) {
        return setPercentileA(new SScalar(nonNull(percentileA)).toDoubles());
    }

    public double[] getPercentileB() {
        return percentileB.clone();
    }

    public ValuesAtLabelledObjects setPercentileB(double[] percentileB) {
        this.percentileB = checkPercentileLevels(percentileB).clone();
        return this;
    }

    public ValuesAtLabelledObjects setPercentileB(String percentileB) {
        return setPercentileB(new SScalar(nonNull(percentileB)).toDoubles());
    }

    public double[] getPercentileC() {
        return percentileC.clone();
    }

    public ValuesAtLabelledObjects setPercentileC(double[] percentileC) {
        this.percentileC = checkPercentileLevels(percentileC).clone();
        return this;
    }

    public ValuesAtLabelledObjects setPercentileC(String percentileC) {
        return setPercentileC(new SScalar(nonNull(percentileC)).toDoubles());
    }

    public boolean isChannelPercentiles() {
        return channelPercentiles;
    }

    public ValuesAtLabelledObjects setChannelPercentiles(boolean channelPercentiles) {
        this.channelPercentiles = channelPercentiles;
        return this;
    }

    public int[] getSeparateChannelPercentilesList() {
        return separateChannelPercentilesList.clone();
    }

    public ValuesAtLabelledObjects setSeparateChannelPercentilesList(int[] separateChannelPercentilesList) {
        this.separateChannelPercentilesList = nonNull(separateChannelPercentilesList).clone();
        return this;
    }

    public ValuesAtLabelledObjects setSeparateChannelPercentilesList(String separateChannelPercentilesList) {
        return setSeparateChannelPercentilesList(new SScalar(nonNull(separateChannelPercentilesList)).toInts());
    }

    public int getLevelChannel() {
        return levelChannel;
    }

    public ValuesAtLabelledObjects setLevelChannel(int levelChannel) {
        this.levelChannel = nonNegative(levelChannel);
        return this;
    }

    public ObjectParameter getPaintedParameter() {
        return paintedParameter;
    }

    public ValuesAtLabelledObjects setPaintedParameter(ObjectParameter paintedParameter) {
        this.paintedParameter = nonNull(paintedParameter);
        return this;
    }

    public boolean isPaintLabelledOnSource() {
        return paintLabelledOnSource;
    }

    public ValuesAtLabelledObjects setPaintLabelledOnSource(boolean paintLabelledOnSource) {
        this.paintLabelledOnSource = paintLabelledOnSource;
        return this;
    }

    public boolean isVisiblePaintLabelled() {
        return visiblePaintLabelled;
    }

    public ValuesAtLabelledObjects setVisiblePaintLabelled(boolean visiblePaintLabelled) {
        this.visiblePaintLabelled = visiblePaintLabelled;
        return this;
    }

    public ValuesAtLabelledObjects requestPaintLabelled() {
        requestOutput(OUTPUT_PAINT_LABELLED);
        return this;
    }

    public SMat resultPaintLabelled() {
        return getMat(OUTPUT_PAINT_LABELLED);
    }

    @Override
    public void process() {
        final MultiMatrix2D sourceMatrix = getInputMat().toMultiMatrix2D();
        final MultiMatrix2D labelsMatrix = getInputMat(INPUT_LABELS).toMultiMatrix2D();
        final MultiMatrix2D maskMatrix = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        final Map<ObjectParameter, SNumbers> resultStatistics = convertMap(
                allOutputContainers(SNumbers.class, true));
        setStartProcessingTimeStamp();
        analyse(resultStatistics, sourceMatrix, labelsMatrix, maskMatrix);
        for (Map.Entry<ObjectParameter, SNumbers> result : resultStatistics.entrySet()) {
            // maybe, analyse method has added some results
            getNumbers(result.getKey().outputPort).setTo(result.getValue());
        }
        setEndProcessingTimeStamp();
    }

    public void analyse(
            final Map<ObjectParameter, SNumbers> results,
            final MultiMatrix2D sourceMatrix,
            final MultiMatrix2D labelsMatrix,
            final MultiMatrix2D maskMatrix) {
        analyse(
                results,
                sourceMatrix,
                labelsMatrix,
                maskMatrix,
                getInputMat(INPUT_LEVEL, true).toMultiMatrix2D());
    }

    // 0 value in labels means background and ignored; values 1, 2, ..., N mean processed objects (areas):
    // the length of returned array is N, not N+1 (because 0 values are not included into processing)
    public void analyse(
            final Map<ObjectParameter, SNumbers> results,
            final MultiMatrix2D sourceMatrix,
            final MultiMatrix2D labelsMatrix,
            final MultiMatrix2D maskMatrix,
            MultiMatrix2D levelMatrix) {
        Objects.requireNonNull(sourceMatrix, "Null source");
        Objects.requireNonNull(labelsMatrix, "Null labels");
        long t1 = debugTime();
        sourceMatrix.checkDimensionEquality(labelsMatrix, "source", "labels");
        sourceMatrix.checkDimensionEquality(maskMatrix, "source", "mask");
        sourceMatrix.checkDimensionEquality(levelMatrix, "source", "level");
        final boolean paintLabelledRequested = isOutputNecessary(OUTPUT_PAINT_LABELLED);
        if (paintLabelledRequested) {
            results.putIfAbsent(paintedParameter, new SNumbers());
            // - request calculating it
        }
        Map<ObjectParameter, Integer> percentileIndexes = new HashMap<>();
        final double[][] percentileLevelsByChannels = percentileLevels(
                results, sourceMatrix.numberOfChannels(), percentileIndexes);
        final boolean needPercentiles = percentileLevelsByChannels[0].length > 0;
        final boolean channelPercentiles = this.channelPercentiles
                || (levelMatrix == null && sourceMatrix.isMono());
        // - use common (more simple) algorithm when levelMatrix == null and sourceMatrix.isMono()

        if (!OPTIMIZE) {
            analyseStable(results, sourceMatrix, labelsMatrix, maskMatrix, levelMatrix);
            return;
        }

        final int numberOfChannels = sourceMatrix.numberOfChannels();
        analyser.setLabels(labelsMatrix, maskMatrix);
        if (needPercentiles && !channelPercentiles) {
            if (levelMatrix == null) {
                levelMatrix = sourceMatrix;
            }
            Matrix<? extends PArray> levels = levelMatrix.channel(
                    Math.min(levelChannel, levelMatrix.numberOfChannels() - 1));
            analyser.setImageAndLevelMatrix(sourceMatrix, levels, rawValues);
            analyser.setSeparateChannelPercentilesSet(separateChannelPercentilesSet(sourceMatrix));
        } else {
            analyser.setImage(sourceMatrix, rawValues);
        }
        long t2 = debugTime();
        long t3 = t2;
        if (results.containsKey(ObjectParameter.STANDARD_DEVIATION)
                || results.containsKey(ObjectParameter.MEAN_SQUARE)) {
            analyser.findMeansAndStandardDeviationsAndCardinalities();
        } else if (results.containsKey(ObjectParameter.MEAN)) {
            analyser.findMeansAndCardinalities();
        }
        if (needPercentiles) {
            analyser.setPercentileLevelByChannels(percentileLevelsByChannels);
            analyser.setNeedTruncatedMeans(results.containsKey(ObjectParameter.TRUNCATED_MEAN));
            analyser.setLowTruncatedMeanIndex(0);
            analyser.setHighTruncatedMeanIndex(Math.min(1, percentileLevelsByChannels[0].length - 1));
            // - low and high percentile levels are always in elements 0 and 1
            analyser.prepareLists();
            t3 = debugTime();
            analyser.findPercentilesAndCardinalities();
        }
        if (results.containsKey(ObjectParameter.FIRST_NON_ZERO)) {
            analyser.findFirstNonZeroPixels();
        }
        if (!analyser.isReadyCardinalities() && results.containsKey(ObjectParameter.CARDINALITY)) {
            analyser.findCardinalities();
        }

        long t4 = debugTime();
        if (results.containsKey(ObjectParameter.CARDINALITY)) {
            assert analyser.isReadyCardinalities();
            results.get(ObjectParameter.CARDINALITY).setTo(analyser.cardinalities(), 1);
        }
        if (results.containsKey(ObjectParameter.MEAN)) {
            assert analyser.isReadySums();
            results.get(ObjectParameter.MEAN).setTo(analyser.means(), numberOfChannels);
        }
        if (results.containsKey(ObjectParameter.MEAN_SQUARE)) {
            assert analyser.isReadySumsOfSquares();
            results.get(ObjectParameter.MEAN_SQUARE).setTo(analyser.meanSquares(), numberOfChannels);
        }
        if (results.containsKey(ObjectParameter.STANDARD_DEVIATION)) {
            assert analyser.isReadySumsOfSquares();
            results.get(ObjectParameter.STANDARD_DEVIATION).setTo(analyser.standardDeviations(), numberOfChannels);
        }
        if (needPercentiles) {
            assert analyser.isReadyPercentiles();
            for (ObjectParameter percentileParameter : ObjectParameter.ALL_PERCENTILES) {
                Integer levelIndex = percentileIndexes.get(percentileParameter);
                if (levelIndex != null) {
                    setResultTo(results, percentileParameter,
                            analyser.groupedPercentilesByLevel(levelIndex), numberOfChannels);
                }
            }
            if (results.containsKey(ObjectParameter.PERCENTILES_RANGE)) {
                // - calculating range may require some additional time: skip it if not requested
                setResultTo(results, ObjectParameter.PERCENTILES_RANGE,
                        analyser.percentilesRange(0, 1), numberOfChannels);
            }
            setResultTo(results, ObjectParameter.TRUNCATED_MEAN, analyser.truncatedMeans(), numberOfChannels);
        }
        if (results.containsKey(ObjectParameter.FIRST_NON_ZERO)) {
            assert analyser.isReadyFirstNonZeroInformation();
            if (rawValues) {
                results.get(ObjectParameter.FIRST_NON_ZERO).setToArray(
                        analyser.firstNonZeroValues(), numberOfChannels);
            } else {
                results.get(ObjectParameter.FIRST_NON_ZERO).setTo(
                        analyser.firstNonZeroFloatValues(true), numberOfChannels);
            }
        }
        long t5 = debugTime();
        if (LOGGABLE_DEBUG) {
            logDebug(String.format(Locale.US, "Values at %d labelled objects at %s calculated in %.3f ms: "
                            + "%.3f reading/masking matrices, "
                            + "%.3f processing labels%s, "
                            + "%.3f making results %s",
                    analyser.maxLabel(), sourceMatrix, (t5 - t1) * 1e-6,
                    (t2 - t1) * 1e-6,
                    (t4 - t2) * 1e-6,
                    needPercentiles ?
                            String.format(Locale.US, " (%.3f for lists + %.3f for %d percentiles: [%s])",
                                    (t3 - t2) * 1e-6,
                                    (t4 - t3) * 1e-6,
                                    percentileIndexes.size(),
                                    percentileLevelsToString(percentileLevelsByChannels)) :
                            "",
                    (t5 - t4) * 1e-6, results.keySet()));
        }
        if (paintLabelledRequested) {
            getMat(OUTPUT_PAINT_LABELLED).setTo(paintLabelledObjects(
                    results.get(paintedParameter),
                    sourceMatrix,
                    labelsMatrix,
                    null,
                    // - for visible result, it is better to ignore mask while painting
                    // (it is very easy to add it after drawing, if necessary)
                    paintedParameter == ObjectParameter.CARDINALITY,
                    paintLabelledOnSource));
        }
    }


    private void analyseStable(
            final Map<ObjectParameter, SNumbers> results,
            final MultiMatrix2D sourceMatrix,
            final MultiMatrix2D labelsMatrix,
            final MultiMatrix2D maskMatrix,
            MultiMatrix2D levelMatrix) {
        Objects.requireNonNull(sourceMatrix, "Null source");
        Objects.requireNonNull(labelsMatrix, "Null labels");
        sourceMatrix.checkDimensionEquality(labelsMatrix, "source", "labels");
        sourceMatrix.checkDimensionEquality(maskMatrix, "source", "mask");
        sourceMatrix.checkDimensionEquality(levelMatrix, "source", "level");
        final boolean paintLabelledRequested = isOutputNecessary(OUTPUT_PAINT_LABELLED);
        if (paintLabelledRequested) {
            results.putIfAbsent(paintedParameter, new SNumbers());
            // - request calculating it
        }
        if (this.lowPercentile.length == 0) {
            throw new IllegalArgumentException("At least one low percentile must be specified");
        }
        final double lowPercentile = this.lowPercentile[0];
        final double highPercentileCorrected = this.highPercentile.length == 0 ?
                1.0 - lowPercentile :
                this.highPercentile[0];

        long t1 = debugTime();
        final MultiMatrix2D maskedLabels = maskedLabels(labelsMatrix, maskMatrix);
        long t2 = debugTime();
        final int numberOfChannels = sourceMatrix.numberOfChannels();
        final double scale = rawValues ? 1.0 : 1.0 / sourceMatrix.maxPossibleValue();
        final int[] labels = maskedLabels.channelToIntArray(0);
        long t3 = debugTime();
        final int[] cardinalities = findLabelCardinalities(labels);
        if (results.containsKey(ObjectParameter.CARDINALITY)) {
            results.get(ObjectParameter.CARDINALITY).setTo(cardinalities, 1);
        }
        SNumbers.checkDimensions(labels.length, numberOfChannels);
        long t4 = debugTime();
        if (results.containsKey(ObjectParameter.MEAN)) {
            final float[] result = new float[numberOfChannels * cardinalities.length];
            for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
                float[][] values = splitByLabels(
                        cardinalities, labels, sourceMatrix.channelToFloatArray(channelIndex));
                for (int k = 0; k < values.length; k++) {
                    result[k * numberOfChannels + channelIndex] = (float) (average(values[k]) * scale);
                }
            }
            results.get(ObjectParameter.MEAN).setTo(result, numberOfChannels);
        }
        if (results.containsKey(ObjectParameter.STANDARD_DEVIATION)) {
            final float[] result = new float[numberOfChannels * cardinalities.length];
            for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
                float[][] values = splitByLabels(
                        cardinalities, labels, sourceMatrix.channelToFloatArray(channelIndex));
                for (int k = 0; k < values.length; k++) {
                    double average = average(values[k]);
                    double standardDeviation = standardDeviation(values[k], average);
                    result[k * numberOfChannels + channelIndex] = (float) (standardDeviation * scale);
                }
            }
            results.get(ObjectParameter.STANDARD_DEVIATION).setTo(result, numberOfChannels);
        }
        if (results.containsKey(ObjectParameter.LOW_PERCENTILE)
                || results.containsKey(ObjectParameter.HIGH_PERCENTILE)
                || results.containsKey(ObjectParameter.PERCENTILES_RANGE)
                || results.containsKey(ObjectParameter.TRUNCATED_MEAN)) {
            if (!channelPercentiles) {
                if (levelMatrix == null) {
                    levelMatrix = sourceMatrix.asMono();
                    // But in some applications we may prefer to get something else at the point of the given percentile
                } else {
                    levelMatrix = levelMatrix.asMono();
                }
            }
            final float[] levels = channelPercentiles ? null : levelMatrix.channelToFloatArray(0);
            long t5 = debugTime();
            final PercentilePairs levelsPercentiles = channelPercentiles ? null : new PercentilePairs(
                    labels, cardinalities, levels, lowPercentile, highPercentileCorrected);
            long t6 = debugTime();
            final Stream<Matrix<? extends PArray>> channelStream = Arrays.SystemSettings.cpuCount() == 1 ?
                    sourceMatrix.allChannels().stream() :
                    sourceMatrix.allChannels().parallelStream();
            final List<ChannelStatistics> allChannelsResults = channelStream.map(m -> {
                final float[] array = Matrices.toFloatJavaArray(m);
                PercentilePairs percentiles = channelPercentiles ?
                        new PercentilePairs(labels, cardinalities, array, lowPercentile, highPercentileCorrected) :
                        levelsPercentiles;
                float[][] values = channelPercentiles ?
                        percentiles.dataByLabels :
                        splitByLabels(cardinalities, labels, array);
                assert values.length == cardinalities.length;
                final ChannelStatistics stat = new ChannelStatistics(cardinalities.length, results.keySet());
                for (int k = 0; k < values.length; k++) {
                    // Unless channelPercentiles, we find percentile in levels, then search
                    // for all level values equal to this percentile (at least 1 instance),
                    // look up for the corresponding source values and average them
                    final double low = channelPercentiles ?
                            percentiles.lowPercentiles[k] :
                            averageEqual(percentiles.dataByLabels[k], percentiles.lowPercentiles[k], values[k]);
                    final double high = channelPercentiles ?
                            percentiles.highPercentiles[k] :
                            averageEqual(percentiles.dataByLabels[k], percentiles.highPercentiles[k], values[k]);
                    stat.lowPercentiles[k] = (float) (low * scale);
                    stat.highPercentiles[k] = (float) (high * scale);
                    if (stat.percentilesRange != null) {
                        // - to be the safe side (always true in current ChannelStatistics)
                        stat.percentilesRange[k] = (float) ((high - low) * scale);
                    }
                    if (stat.truncatedMean != null) {
                        stat.truncatedMean[k] = (float) (averageInRange(
                                percentiles.dataByLabels[k],
                                percentiles.lowPercentiles[k],
                                percentiles.highPercentiles[k],
                                values[k]) * scale);
                    }
                }
                return stat;
            }).collect(Collectors.toList());
            long t7 = debugTime();
            for (ObjectParameter parameter : ChannelStatistics.calculatedParameters(results.keySet())) {
                SNumbers result = results.get(parameter);
                if (result == null) {
                    result = new SNumbers();
                    results.put(parameter, result);
                }
                final float[] array = new float[numberOfChannels * cardinalities.length];
                for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
                    final float[] channelArray = allChannelsResults.get(channelIndex).result(parameter);
                    assert channelArray != null;
                    for (int k = 0; k < cardinalities.length; k++) {
                        array[k * numberOfChannels + channelIndex] = channelArray[k];
                    }
                }
                result.setTo(array, numberOfChannels);
            }
            long t8 = debugTime();
            logDebug(() -> String.format(Locale.US, "Values at %d labelled objects calculated in %.3f ms: "
                            + "%.3f masking, %.3f reading labels, "
                            + "%.3f counting labels, %.3f making level, "
                            + "%.3f splitting + sorting pixels, "
                            + "%.3f calculating statistics, %.3f making result",
                    cardinalities.length, (t8 - t1) * 1e-6,
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
                    (t4 - t3) * 1e-6, (t5 - t4) * 1e-6,
                    (t6 - t5) * 1e-6,
                    (t7 - t6) * 1e-6, (t8 - t7) * 1e-6));
        } else {
            long t5 = debugTime();
            logDebug(() -> String.format(Locale.US, "Values at %d labelled objects calculated in %.3f ms: "
                            + "%.3f masking, %.3f reading labels, "
                            + "%.3f counting labels, %.3f calculating statistics",
                    cardinalities.length, (t5 - t1) * 1e-6,
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
                    (t4 - t3) * 1e-6, (t5 - t4) * 1e-6));
        }
        if (results.containsKey(ObjectParameter.FIRST_NON_ZERO)) {
            final BitArray nonZero = sourceMatrix.nonZeroAnyChannelMatrix().array().updatableClone(Arrays.SMM);
            if (rawValues && !sourceMatrix.isFloatingPoint()) {
                final int[] result = new int[numberOfChannels * cardinalities.length];
                for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
                    int[] firstValues = firstNonZeroValueForLabels(
                            nonZero, cardinalities.length, labels, sourceMatrix.channelToIntArray(channelIndex));
                    for (int k = 0; k < firstValues.length; k++) {
                        result[k * numberOfChannels + channelIndex] = firstValues[k];
                    }
                }
                results.get(ObjectParameter.FIRST_NON_ZERO).setTo(result, numberOfChannels);
            } else {
                final float[] result = new float[numberOfChannels * cardinalities.length];
                for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
                    float[] firstValues = firstNonZeroValueForLabels(
                            nonZero, cardinalities.length, labels, sourceMatrix.channelToFloatArray(channelIndex));
                    for (int k = 0; k < firstValues.length; k++) {
                        result[k * numberOfChannels + channelIndex] = (float) (firstValues[k] * scale);
                    }
                }
                results.get(ObjectParameter.FIRST_NON_ZERO).setTo(result, numberOfChannels);
            }
        }
        if (paintLabelledRequested) {
            getMat(OUTPUT_PAINT_LABELLED).setTo(paintLabelledObjects(
                    results.get(paintedParameter),
                    sourceMatrix,
                    labelsMatrix,
                    null,
                    // - for visible result, it is better to ignore mask while painting
                    // (it is very easy to add it after drawing, if necessary)
                    paintedParameter == ObjectParameter.CARDINALITY,
                    paintLabelledOnSource));
        }
    }

    @Override
    public String visibleOutputPortName() {
        return visiblePaintLabelled ? OUTPUT_PAINT_LABELLED : paintedParameter.outputPort;
    }

    public static MultiMatrix2D paintLabelledObjects(
            SNumbers statistics,
            final MultiMatrix2D sourceMatrix,
            final MultiMatrix2D labelsMatrix,
            final MultiMatrix2D maskMatrix,
            boolean intElementType,
            boolean paintLabelledOnSource) {
        return new PaintLabelledObjects()
                .setRawValues(intElementType)
                .setElementType(intElementType ? int.class : sourceMatrix.elementType())
                .process(
                        maskedLabels(labelsMatrix, maskMatrix),
                        paintLabelledOnSource ? sourceMatrix : null,
                        statistics);
    }

    private double[][] percentileLevels(
            final Map<ObjectParameter, SNumbers> requested,
            final int numberOfChannels,
            final Map<ObjectParameter, Integer> levelIndexes) {
        final boolean needRange = requested.containsKey(ObjectParameter.PERCENTILES_RANGE);
        final boolean needTruncatedMean = requested.containsKey(ObjectParameter.TRUNCATED_MEAN);
        final boolean needLowPercentile = requested.containsKey(ObjectParameter.LOW_PERCENTILE);
        final boolean needHighPercentile = requested.containsKey(ObjectParameter.HIGH_PERCENTILE);
        final double[][] result = new double[numberOfChannels][];
        for (int c = 0; c < numberOfChannels; c++) {
            final double[] levels = new double[16];
            // - to be on the safe side (really 5 is enough)
            int count = 0;
            if (needLowPercentile || needRange || needTruncatedMean) {
                levelIndexes.put(ObjectParameter.LOW_PERCENTILE, count);
                levels[count++] = percentileLevel(c, lowPercentile, "low percentile");
            }
            if (needHighPercentile || needRange || needTruncatedMean) {
                levelIndexes.put(ObjectParameter.HIGH_PERCENTILE, count);
                levels[count++] = percentileLevel(c, highPercentile,
                        () -> 1.0 - percentileLevel(0, lowPercentile, "low percentile"));
            }
            if (requested.containsKey(ObjectParameter.PERCENTILE_A)) {
                levelIndexes.put(ObjectParameter.PERCENTILE_A, count);
                levels[count++] = percentileLevel(c, percentileA, "percentile A");
            }
            if (requested.containsKey(ObjectParameter.PERCENTILE_B)) {
                levelIndexes.put(ObjectParameter.PERCENTILE_B, count);
                levels[count++] = percentileLevel(c, percentileB, "percentile B");
            }
            if (requested.containsKey(ObjectParameter.PERCENTILE_C)) {
                levelIndexes.put(ObjectParameter.PERCENTILE_C, count);
                levels[count++] = percentileLevel(c, percentileC, "percentile C");
            }
            result[c] = JArrays.copyOfRange(levels, 0, count);
        }
        return result;
    }

    private boolean[] separateChannelPercentilesSet(MultiMatrix2D sourceMatrix) {
        boolean[] result = new boolean[sourceMatrix.numberOfChannels()];
        // - zero-filled by Java
        for (int c : separateChannelPercentilesList) {
            if (c >= 0 && c < result.length) {
                result[c] = true;
            }
        }
        return result;
    }

    private static String percentileLevelsToString(double[][] percentileLevels) {
        StringBuilder sb = new StringBuilder();
        int count = percentileLevels.length;
        while (count > 1 && java.util.Arrays.equals(percentileLevels[count - 2], percentileLevels[count - 1])) {
            count--;
        }
        for (int i = 0; i < count; i++) {
            double[] levels = percentileLevels[i];
            if (i > 0) {
                sb.append("; ");
            }
            for (int j = 0; j < levels.length; j++) {
                if (j > 0) {
                    sb.append(", ");
                }
                sb.append(levels[j]);
            }
        }
        if (count < percentileLevels.length) {
            sb.append(";...");
        }
        return sb.toString();
    }

    private static double[] checkPercentileLevels(double[] levels) {
        nonNull(levels);
        for (double p : levels) {
            inRange(p, 0.0, 1.0);
        }
        return levels;
    }

    private static double percentileLevel(int channelIndex, double[] levels, Supplier<Double> defaultValue) {
        return channelIndex < levels.length ? levels[channelIndex]
                : levels.length > 0 ? levels[levels.length - 1]
                : defaultValue.get();
    }

    private static double percentileLevel(int channelIndex, double[] levels, String percentileName) {
        if (levels.length > 0) {
            return percentileLevel(channelIndex, levels, () -> Double.NaN);
        } else {
            throw new IllegalArgumentException("At least one " + percentileName + " must be specified");
        }
    }

    private static void setResultTo(
            Map<ObjectParameter, SNumbers> results,
            ObjectParameter parameter,
            float[] array,
            int numberOfChannels) {
        if (array == null) {
            return;
        }
        SNumbers result = results.get(parameter);
        if (result == null) {
            result = new SNumbers();
            results.put(parameter, result);
        }
        result.setTo(array, numberOfChannels);
    }

    private static MultiMatrix2D maskedLabels(final MultiMatrix2D labelsMatrix, final MultiMatrix2D maskMatrix) {
        return maskMatrix != null ? labelsMatrix.min(maskMatrix.nonZeroPixels(false)) : labelsMatrix;
    }

    private static int[] findLabelCardinalities(int[] labels) {
        int resultLength = 0;
        for (int label : labels) {
            resultLength = Math.max(resultLength, label);
        }
        int[] cardinalities = new int[resultLength];
        for (int label : labels) {
            if (label > 0) {
                cardinalities[label - 1]++;
            }
        }
        return cardinalities;
    }

    private static float[] firstNonZeroValueForLabels(
            BitArray nonZero,
            int numberOfLabels,
            int[] labels,
            float[] values) {
        float[] result = new float[numberOfLabels];
        for (int i = labels.length - 1; i >= 0; i--) {
            int label = labels[i];
            if (label > 0 && nonZero.getBit(i)) {
                result[label - 1] = values[i];
            }
        }
        return result;
    }

    private static int[] firstNonZeroValueForLabels(
            BitArray nonZero,
            int numberOfLabels,
            int[] labels,
            int[] values) {
        int[] result = new int[numberOfLabels];
        for (int i = labels.length - 1; i >= 0; i--) {
            int label = labels[i];
            if (label > 0 && nonZero.getBit(i)) {
                result[label - 1] = values[i];
            }
        }
        return result;
    }

    private static float[][] splitByLabels(
            int[] cardinalities,
            int[] labels,
            float[] values) {
        float[][] result = new float[cardinalities.length][];
        for (int k = 0; k < result.length; k++) {
            result[k] = new float[cardinalities[k]];
        }
        int[] labelIndexes = new int[cardinalities.length];
        for (int i = 0; i < labels.length; i++) {
            int label = labels[i];
            if (label > 0) {
                result[label - 1][labelIndexes[label - 1]++] = values[i];
            }
        }
        return result;
    }

    private static float[] sortValues(float[] values) {
        values = values.clone();
        java.util.Arrays.sort(values);
        return values;
    }

    private static double findPercentile(float[] sortedValues, double percentile) {
        if (sortedValues.length == 0) {
            return Float.NaN;
        }
        int index = (int) Math.round(percentile * (sortedValues.length - 1));
        if (index < 0) {
            index = 0;
        }
        if (index > sortedValues.length - 1) {
            index = sortedValues.length - 1;
        }
        return sortedValues[index];
    }

    private static double averageEqual(float[] levels, double selectedLevel, float[] valuesToAverage) {
        assert levels.length == valuesToAverage.length;
        double sum = 0.0;
        int count = 0;
        for (int k = 0; k < levels.length; k++) {
            if (levels[k] == selectedLevel) {
                sum += valuesToAverage[k];
                count++;
            }
        }
        return sum / (double) count;
    }

    private static double averageInRange(float[] levels, double lowLevel, double highLevel, float[] valuesToAverage) {
        assert levels.length == valuesToAverage.length;
        double sum = 0.0;
        int count = 0;
        for (int k = 0; k < levels.length; k++) {
            if (levels[k] >= lowLevel && levels[k] <= highLevel) {
                sum += valuesToAverage[k];
                count++;
            }
        }
        return sum / (double) count;
    }

    private static double average(float[] valuesToAverage) {
        double sum = 0.0;
        for (int k = 0; k < valuesToAverage.length; k++) {
            sum += valuesToAverage[k];
        }
        return sum / (double) valuesToAverage.length;
    }

    private static double standardDeviation(float[] valuesToAverage, double average) {
        double sum = 0.0;
        for (int k = 0; k < valuesToAverage.length; k++) {
            double diff = valuesToAverage[k] - average;
            sum += diff * diff;
        }
        return Math.sqrt(sum / (double) valuesToAverage.length);
    }

    private static Map<ObjectParameter, SNumbers> convertMap(Map<String, SNumbers> statistics) {
        Map<ObjectParameter, SNumbers> result = new LinkedHashMap<>();
        statistics.forEach((String s, SNumbers numbers) -> result.put(OUTPUT_STATISTICS.get(s), numbers));
        return result;
    }

    private static class PercentilePairs {
        final double[] lowPercentiles;
        final double[] highPercentiles;
        final float[][] dataByLabels;

        PercentilePairs(
                int[] labels,
                int[] cardinalities,
                float[] data,
                double lowPercentile,
                double highPercentile) {
            this.dataByLabels = splitByLabels(cardinalities, labels, data);
            final List<float[]> levelsByLabelsList = java.util.Arrays.asList(dataByLabels);
            final Stream<float[]> sortingStream = Arrays.SystemSettings.cpuCount() == 1 ?
                    levelsByLabelsList.stream() :
                    levelsByLabelsList.parallelStream();
            final List<float[]> sortedLevelsByLabelsList = sortingStream.map(
                    ValuesAtLabelledObjects::sortValues).toList();
            // - relatively slow sorting
            this.lowPercentiles = sortedLevelsByLabelsList.stream().map(
                            sorted -> findPercentile(sorted, lowPercentile))
                    .mapToDouble(Double::doubleValue).toArray();
            this.highPercentiles = sortedLevelsByLabelsList.stream().map(
                            sorted -> findPercentile(sorted, highPercentile))
                    .mapToDouble(Double::doubleValue).toArray();
            // - quick extracting
        }
    }

    private static class ChannelStatistics {
        static final ObjectParameter[] PARAMETERS = {
                ObjectParameter.LOW_PERCENTILE,
                ObjectParameter.HIGH_PERCENTILE,
                ObjectParameter.PERCENTILES_RANGE,
                ObjectParameter.TRUNCATED_MEAN};

        final float[] lowPercentiles;
        final float[] highPercentiles;
        final float[] percentilesRange;
        final float[] truncatedMean;

        ChannelStatistics(int length, Set<ObjectParameter> requested) {
            this.lowPercentiles = new float[length];
            this.highPercentiles = new float[length];
            this.percentilesRange = new float[length];
            // - previous 3 statistics are calculated always, if any of these 4 statistics is requested
            this.truncatedMean = requested.contains(ObjectParameter.TRUNCATED_MEAN) ? new float[length] : null;
        }

        static Set<ObjectParameter> calculatedParameters(Set<ObjectParameter> requested) {
            final Set<ObjectParameter> result = new HashSet<>();
            result.add(ObjectParameter.LOW_PERCENTILE);
            result.add(ObjectParameter.HIGH_PERCENTILE);
            result.add(ObjectParameter.PERCENTILES_RANGE);
            if (requested.contains(ObjectParameter.TRUNCATED_MEAN)) {
                result.add(ObjectParameter.TRUNCATED_MEAN);
            }
            return result;
        }

        float[] result(ObjectParameter parameter) {
            switch (parameter) {
                case LOW_PERCENTILE:
                    return lowPercentiles;
                case HIGH_PERCENTILE:
                    return highPercentiles;
                case PERCENTILES_RANGE:
                    return percentilesRange;
                case TRUNCATED_MEAN:
                    return truncatedMean;
                default:
                    return null;
            }
        }
    }

}
