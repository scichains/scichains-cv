/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.misc;

import net.algart.arrays.Arrays;
import net.algart.arrays.*;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToNumbers;
import net.algart.executors.modules.core.matrices.geometry.ContinuationMode;
import net.algart.executors.modules.cv.matrices.misc.extremums.ExtremumsFinder;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.executors.modules.cv.matrices.morphology.StrictMorphology;
import net.algart.executors.modules.cv.matrices.objects.MeasureLabelledObjects;
import net.algart.executors.modules.opencv.matrices.filtering.GaussianBlur;
import net.algart.math.IRange;
import net.algart.math.functions.Func;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.*;

public final class LocalExtremums extends MultiMatrixToNumbers {
    private static final int MULTITHREADING_Y_BLOCK_LENGTH = 8; // zero value disables multithreading
    private static final int MIN_MULTITHREADING_Y_BLOCK_LENGTH = 4;
    // - Note that every thread allocates arrays with length, equal to aperture size

    public static final String INPUT_MASK = "mask";
    public static final String INPUT_IGNORE = "ignore";
    public static final String OUTPUT_EXTREMUMS = "extremums";
    public static final String OUTPUT_EXTREMUMS_MASK = "extremums_mask";
    public static final String OUTPUT_EXTREMUMS_ON_SOURCE = "extremums_on_source";

    public enum ResultValues {
        MAXIMUMS,
        MINIMUMS
    }

    public enum ResultAtPlateau {
        ALL_PIXELS(false),
        CENTROID(true),
        CENTROID_OF_CIRCLE(true);

        final boolean postProcessingRequired;

        ResultAtPlateau(boolean postProcessingRequired) {
            this.postProcessingRequired = postProcessingRequired;
        }
    }

    private ResultValues resultValues = ResultValues.MAXIMUMS;
    private int gaussianBlurKernelSize = 5;
    private int apertureSize = 5;
    private int depthApertureSize = 0;
    private boolean depthApertureRing = false;
    private ExtremumsFinder.DeepTestSettings.Mode depthAnalysisMode = ExtremumsFinder.DeepTestSettings.Mode.PERCENTILE;
    private double depthPercentileLevel = 1.0;
    private double minimalDepth = 0.0;
    private ResultAtPlateau resultAtPlateau = ResultAtPlateau.CENTROID;
    private int resultCircleSize = 1;
    private String drawingExtremumsColor = "#FFFFFF";
    private boolean autoContrastSourceUnderExtremums = false;
    private boolean visibleExtremumsOnSource = false;

    private MultiMatrix2D resultExtremumsMask = null;

    public LocalExtremums() {
        addInputMat(INPUT_MASK);
        addInputMat(INPUT_IGNORE);
        setDefaultOutputNumbers(OUTPUT_EXTREMUMS);
        addOutputMat(OUTPUT_EXTREMUMS_MASK);
    }

    public ResultValues getResultValues() {
        return resultValues;
    }

    public LocalExtremums setResultValues(ResultValues resultValues) {
        this.resultValues = nonNull(resultValues);
        return this;
    }

    public int getGaussianBlurKernelSize() {
        return gaussianBlurKernelSize;
    }

    public LocalExtremums setGaussianBlurKernelSize(int gaussianBlurKernelSize) {
        this.gaussianBlurKernelSize = nonNegative(gaussianBlurKernelSize);
        return this;
    }

    public int getApertureSize() {
        return apertureSize;
    }

    public LocalExtremums setApertureSize(int apertureSize) {
        this.apertureSize = positive(apertureSize);
        return this;
    }

    public int getDepthApertureSize() {
        return depthApertureSize;
    }

    public LocalExtremums setDepthApertureSize(int depthApertureSize) {
        this.depthApertureSize = nonNegative(depthApertureSize);
        return this;
    }

    public LocalExtremums setDepthApertureSize(String depthApertureSize) {
        this.depthApertureSize = nonNegative(intOrDefault(depthApertureSize, 0));
        return this;
    }

    public boolean isDepthApertureRing() {
        return depthApertureRing;
    }

    public LocalExtremums setDepthApertureRing(boolean depthApertureRing) {
        this.depthApertureRing = depthApertureRing;
        return this;
    }

    public ExtremumsFinder.DeepTestSettings.Mode getDepthAnalysisMode() {
        return depthAnalysisMode;
    }

    public LocalExtremums setDepthAnalysisMode(ExtremumsFinder.DeepTestSettings.Mode depthAnalysisMode) {
        this.depthAnalysisMode = nonNull(depthAnalysisMode);
        return this;
    }

    public double getDepthPercentileLevel() {
        return depthPercentileLevel;
    }

    public LocalExtremums setDepthPercentileLevel(double depthPercentileLevel) {
        this.depthPercentileLevel = inRange(depthPercentileLevel, 0.0, 1.0);
        return this;
    }

    public double getMinimalDepth() {
        return minimalDepth;
    }

    public LocalExtremums setMinimalDepth(double minimalDepth) {
        this.minimalDepth = nonNegative(minimalDepth);
        return this;
    }

    public ResultAtPlateau getResultAtPlateau() {
        return resultAtPlateau;
    }

    public LocalExtremums setResultAtPlateau(ResultAtPlateau resultAtPlateau) {
        this.resultAtPlateau = nonNull(resultAtPlateau);
        return this;
    }

    public int getResultCircleSize() {
        return resultCircleSize;
    }

    public LocalExtremums setResultCircleSize(int resultCircleSize) {
        this.resultCircleSize = positive(resultCircleSize);
        return this;
    }

    public String getDrawingExtremumsColor() {
        return drawingExtremumsColor;
    }

    public LocalExtremums setDrawingExtremumsColor(String drawingExtremumsColor) {
        this.drawingExtremumsColor = nonNull(drawingExtremumsColor);
        return this;
    }

    public boolean isAutoContrastSourceUnderExtremums() {
        return autoContrastSourceUnderExtremums;
    }

    public LocalExtremums setAutoContrastSourceUnderExtremums(boolean autoContrastSourceUnderExtremums) {
        this.autoContrastSourceUnderExtremums = autoContrastSourceUnderExtremums;
        return this;
    }

    public boolean isVisibleExtremumsOnSource() {
        return visibleExtremumsOnSource;
    }

    public LocalExtremums setVisibleExtremumsOnSource(boolean visibleExtremumsOnSource) {
        this.visibleExtremumsOnSource = visibleExtremumsOnSource;
        return this;
    }

    public MultiMatrix2D resultExtremumsMask() {
        return resultExtremumsMask;
    }

    @Override
    public SNumbers analyse(MultiMatrix source) {
        Objects.requireNonNull(source, "Null source");
        return analyse(
                source.asMultiMatrix2D(),
                getInputMat(INPUT_MASK, true).toMultiMatrix2D(),
                getInputMat(INPUT_IGNORE, true).toMultiMatrix2D());
    }

    public SNumbers analyse(MultiMatrix2D source, MultiMatrix2D mask, MultiMatrix2D ignore) {
        source.checkDimensionEquality(mask, "source", "mask");
        source.checkDimensionEquality(ignore, "source", "ignore");
        long t1 = System.nanoTime();
        source = source.toMonoIfNot();
        if (gaussianBlurKernelSize > 0) {
            source = GaussianBlur.blur(source, gaussianBlurKernelSize, true);
        }
        long t2 = System.nanoTime();
        final boolean[] maskArray = mask == null ? null : mask.nonZeroRGBMatrix().array().toJavaArray();
        long t3 = System.nanoTime();
        final SortedRound2DAperture aperture = SortedRound2DAperture.getCircleWithSpeciallyOrderedPointsAtAxes(
                apertureSize, source.dimX());
        final int depthApertureSize = this.depthApertureSize == 0 ? apertureSize : this.depthApertureSize;
        final SortedRound2DAperture depthAperture = depthApertureSize == apertureSize && !depthApertureRing ?
                null :
                depthApertureRing ?
                        SortedRound2DAperture.getRing(depthApertureSize, source.dimX()) :
                        SortedRound2DAperture.getCircle(depthApertureSize, source.dimX());
        final Matrix<BitArray> ignoreMatrix = ignore == null ? null : ignore.nonZeroRGBMatrix();
        final float[] values = source.channel(0).toFloat();
        final Matrix<UpdatableBitArray> extremumsMaskMatrix = Arrays.SMM.newBitMatrix(source.dimensions());
        long t4 = System.nanoTime();
        MutableIntArray extremumsXY = Arrays.SMM.newEmptyIntArray();
        if (source.size() > 0) {
            final long dimY = source.dimY();
            if (MULTITHREADING_Y_BLOCK_LENGTH == 0 || Arrays.SystemSettings.cpuCount() == 1) {
                extremumsXY = processRange(
                        values, maskArray, aperture, depthAperture, ignoreMatrix, extremumsMaskMatrix,
                        IRange.valueOf(0, dimY - 1));
            } else {
                final List<IRange> yRanges = new ArrayList<>();
                final int yBlockLength = Math.max(MIN_MULTITHREADING_Y_BLOCK_LENGTH,
                        (int) Math.min(MULTITHREADING_Y_BLOCK_LENGTH,
                                dimY / Runtime.getRuntime().availableProcessors()));
                for (long y = 0; y < dimY; y += yBlockLength) {
                    yRanges.add(IRange.valueOf(y, Math.min(y + yBlockLength, dimY) - 1));
                }
                final List<? extends IntArray> results = yRanges.parallelStream().map(
                                range -> processRange(
                                        values, maskArray, aperture, depthAperture, ignoreMatrix, extremumsMaskMatrix
                                        , range))
                        .toList();
                for (IntArray a : results) {
                    extremumsXY.append(a);
                }
            }
        }
        long t5 = System.nanoTime();
        this.resultExtremumsMask = MultiMatrix.of2DMono(extremumsMaskMatrix);
        SNumbers result = postprocess(extremumsXY);
        dilateExtremumsMask();
        long t6 = System.nanoTime();
        if (LOGGABLE_DEBUG) {
            logDebug(String.format(Locale.US, "Local %s%s for %s, "
                            + "aperture size %d (%d points): %.3f ms = "
                            + "%.3f blur, %.3f preparing mask, %.3f preparing, %.3f search, "
                            + "%.3f postprocessing",
                    resultValues,
                    mask == null ? "" : " (masked)",
                    source,
                    apertureSize, aperture.count(),
                    (t6 - t1) * 1e-6,
                    (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6, (t5 - t4) * 1e-6,
                    (t6 - t5) * 1e-6));
        }
        setEndProcessingTimeStamp();
        getMat(OUTPUT_EXTREMUMS_MASK).setTo(resultExtremumsMask);
        if (isOutputNecessary(OUTPUT_EXTREMUMS_ON_SOURCE)) {
            try (Selector selector = new Selector()) {
                getMat(OUTPUT_EXTREMUMS_ON_SOURCE).setTo(selector
                        .setSelectorType(Selector.SelectorType.BINARY_MATRIX)
                        .setFiller(1, drawingExtremumsColor)
                        .setMinimalRequiredNumberOfChannels(3)
                        .process(
                                resultExtremumsMask,
                                autoContrastSourceUnderExtremums ? source.contrast() : source,
                                null));
            }
        }
        return result;
    }

    @Override
    public String visibleOutputPortName() {
        return visibleExtremumsOnSource ? OUTPUT_EXTREMUMS_ON_SOURCE : OUTPUT_EXTREMUMS_MASK;
    }

    private SNumbers postprocess(IntArray extremumsXY) {
        if (resultAtPlateau == ResultAtPlateau.ALL_PIXELS) {
            final PArray result = Arrays.asFuncArray(Func.IDENTITY, FloatArray.class, extremumsXY);
            return SNumbers.ofArray(result.toJavaArray(), 2);
        }
        if (resultAtPlateau == ResultAtPlateau.CENTROID_OF_CIRCLE) {
            dilateExtremumsMask();
        }
        final Map<MeasureLabelledObjects.ObjectParameter, SNumbers> resultStatistics = new HashMap<>();
        final SNumbers result = new SNumbers();
        resultStatistics.put(MeasureLabelledObjects.ObjectParameter.CENTROID, result);
        //noinspection resource
        new MeasureLabelledObjects()
                .setAutoSplitBitInputIntoConnectedComponents(true)
                .setBitInputConnectivityType(ConnectivityType.STRAIGHT_AND_DIAGONAL)
                .analyse(resultStatistics, resultExtremumsMask, null);
        final Matrix<UpdatableBitArray> mask = Arrays.SMM.newBitMatrix(resultExtremumsMask.dimensions());
        final long dimX = mask.dimX();
        final long dimY = mask.dimY();
        for (int k = 0, n = result.n(); k < n; k++) {
            final long x = Math.round(result.getValue(k, 0));
            final long y = Math.round(result.getValue(k, 1));
            if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                // - to be on the safe side and for a case of exact rounding to dimX/dimY
                mask.array().setBitNoSync(y * dimX + x);
            }
        }
        resultExtremumsMask = MultiMatrix.of2DMono(mask);
        return result;
    }

    private void dilateExtremumsMask() {
        //noinspection resource
        this.resultExtremumsMask = new StrictMorphology()
                .setOperation(MorphologyOperation.DILATION)
                .setContinuationMode(ContinuationMode.ZERO_CONSTANT)
                .setPattern(MorphologyFilter.Shape.SPHERE, resultCircleSize)
                .process(resultExtremumsMask).asMultiMatrix2D();
    }

    private MutableIntArray processRange(
            float[] values,
            boolean[] mask,
            SortedRound2DAperture aperture,
            SortedRound2DAperture depthAperture,
            Matrix<? extends BitArray> ignore,
            Matrix<UpdatableBitArray> extremumsMaskMatrix,
            IRange yRange) {
        final ExtremumsFinder.DeepTestSettings deepTestSettings = new ExtremumsFinder.DeepTestSettings()
                .setDepthAperture(depthAperture)
                .setMode(depthAnalysisMode)
                .setPercentileLevel(depthPercentileLevel)
                .setMinimalDepth(minimalDepth)
                .setIgnore(ignore);
        final ExtremumsFinder finder = resultValues == ResultValues.MAXIMUMS ?
                ExtremumsFinder.getMaximumsFinder(values, mask, aperture, deepTestSettings, extremumsMaskMatrix,
                        !resultAtPlateau.postProcessingRequired) :
                ExtremumsFinder.getMinimumsFinder(values, mask, aperture, deepTestSettings, extremumsMaskMatrix,
                        !resultAtPlateau.postProcessingRequired);
        for (int y = (int) yRange.min(), yMax = (int) yRange.max(); y <= yMax; y++) {
            finder.processLine(y);
        }
        return finder.getExtremumsXY();
    }

}