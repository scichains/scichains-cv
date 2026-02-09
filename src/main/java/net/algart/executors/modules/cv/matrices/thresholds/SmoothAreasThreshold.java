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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.arrays.Matrix;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.matrices.drawing.DrawRectangle;
import net.algart.executors.modules.cv.matrices.morphology.*;
import net.algart.executors.modules.cv.matrices.objects.RetainOrRemoveMode;
import net.algart.executors.modules.cv.matrices.objects.binary.components.FindConnectedWithMask;
import net.algart.executors.modules.opencv.matrices.filtering.GaussianBlur;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix2D;

public final class SmoothAreasThreshold extends MultiMatrix2DFilter {
    public static final String INPUT_MASK = "mask";

    public enum ResultType {
        SMOOTH_AREAS,
        EDGES_BETWEEN_AREAS
    }

    private int gaussianBlurKernelSize = 5;
    private int gradientDiameter = 5;
    private double gradientLevel = 1.0;
    // - 1.0 means strict morphology and works faster
    private int medianDiameter = 0;
    private double threshold = 0.1;
    private boolean rawValue = false;
    private ResultType resultType = ResultType.SMOOTH_AREAS;
    private boolean borderAsDefaultMask = false;
    private boolean includeMaskInRetained = false;

    public SmoothAreasThreshold() {
        addInputMat(INPUT_MASK);
    }

    public int getGaussianBlurKernelSize() {
        return gaussianBlurKernelSize;
    }

    public SmoothAreasThreshold setGaussianBlurKernelSize(int gaussianBlurKernelSize) {
        this.gaussianBlurKernelSize = nonNegative(gaussianBlurKernelSize);
        return this;
    }

    public int getGradientDiameter() {
        return gradientDiameter;
    }

    public SmoothAreasThreshold setGradientDiameter(int gradientDiameter) {
        this.gradientDiameter = nonNegative(gradientDiameter);
        return this;
    }

    public double getGradientLevel() {
        return gradientLevel;
    }

    public SmoothAreasThreshold setGradientLevel(double gradientLevel) {
        this.gradientLevel = inRange(gradientLevel, 0.0, 1.0);
        return this;
    }

    public int getMedianDiameter() {
        return medianDiameter;
    }

    public SmoothAreasThreshold setMedianDiameter(int medianDiameter) {
        this.medianDiameter = nonNegative(medianDiameter);
        return this;
    }

    public double getThreshold() {
        return threshold;
    }

    public SmoothAreasThreshold setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public boolean isRawValue() {
        return rawValue;
    }

    public SmoothAreasThreshold setRawValue(boolean rawValue) {
        this.rawValue = rawValue;
        return this;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public SmoothAreasThreshold setResultType(ResultType resultType) {
        this.resultType = nonNull(resultType);
        return this;
    }

    public boolean isBorderAsDefaultMask() {
        return borderAsDefaultMask;
    }

    public SmoothAreasThreshold setBorderAsDefaultMask(boolean borderAsDefaultMask) {
        this.borderAsDefaultMask = borderAsDefaultMask;
        return this;
    }

    public boolean isIncludeMaskInRetained() {
        return includeMaskInRetained;
    }

    public SmoothAreasThreshold setIncludeMaskInRetained(boolean includeMaskInRetained) {
        this.includeMaskInRetained = includeMaskInRetained;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final MultiMatrix2D mask = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        return process(source, mask);
    }

    public MultiMatrix2D process(MultiMatrix2D source, MultiMatrix2D mask) {
        source = source.toMonoIfNot();
        if (gaussianBlurKernelSize > 0) {
            source = GaussianBlur.blur(source, gaussianBlurKernelSize, true);
        }
        if (mask == null && borderAsDefaultMask) {
            DrawRectangle drawRectangle = new DrawRectangle();
            drawRectangle.setPercents(true).setLeft(0).setTop(0).setWidth(100.0).setHeight(100.0);
            drawRectangle.setThickness(1);
            drawRectangle.setClearSource(true);
            mask = drawRectangle.process(source.asPrecision(boolean.class));
        }
        final MorphologyFilter morphologyGradient = gradientLevel == 1.0 ?
                new StrictMorphology().setOperation(MorphologyOperation.BEUCHER_GRADIENT) :
                new RankMorphology().setOperation(MorphologyOperation.BEUCHER_GRADIENT).setLevel(gradientLevel);
        morphologyGradient.setPattern(MorphologyFilter.Shape.SPHERE, gradientDiameter);
        morphologyGradient.setContinuationMode(Matrix.ContinuationMode.MIRROR_CYCLIC);
        final MultiMatrix2D gradient = morphologyGradient.process(source).asMultiMatrix2D();

        SimpleThreshold simpleThreshold = new SimpleThreshold();
        simpleThreshold.setMin(0.0);
        simpleThreshold.setMax(threshold);
        simpleThreshold.setRawValues(rawValue);
        MultiMatrix2D result = simpleThreshold.process(gradient);

        if (medianDiameter > 0) {
            final MorphologyFilter morphologyMedian = new Percentile().setPercentile(0.5);
            morphologyMedian.setPattern(MorphologyFilter.Shape.SPHERE, medianDiameter);
            morphologyMedian.setContinuationMode(Matrix.ContinuationMode.MIRROR_CYCLIC);
            result = morphologyMedian.process(result).asMultiMatrix2D();
        }

        if (mask != null) {
            final FindConnectedWithMask findConnectedWithMask = new FindConnectedWithMask();
            findConnectedWithMask.setMode(RetainOrRemoveMode.RETAIN);
            findConnectedWithMask.setIncludeMaskInRetained(includeMaskInRetained);
            result = findConnectedWithMask.process(result, mask);
        }

        switch (resultType) {
            case EDGES_BETWEEN_AREAS:
                return result.asFunc(LinearFunc.getInstance(result.maxPossibleValue(), -1.0)).clone();
            case SMOOTH_AREAS:
                return result;
            default:
                throw new AssertionError("Unknown " + resultType);
        }
    }
}
