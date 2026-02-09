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

package net.algart.executors.modules.cv.matrices.derivatives;

import net.algart.arrays.*;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.matrices.conversions.ChangePrecision;
import net.algart.executors.modules.core.matrices.geometry.ContinuationMode;
import net.algart.executors.modules.opencv.matrices.filtering.GaussianBlur;
import net.algart.matrices.linearfiltering.BasicConvolution;
import net.algart.matrices.linearfiltering.ContinuedConvolution;
import net.algart.matrices.linearfiltering.Convolution;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public abstract class MultichannelDerivativesFilter extends MultiMatrix2DFilter {
    private boolean gaussianBlurOfSource = true;
    private int gaussianBlurKernelSizeX = 5;
    private int gaussianBlurKernelSizeY = 0;
    private ContinuationMode continuationMode = ContinuationMode.MIRROR_CYCLIC;
    private CombiningMatricesMetric combiningChannelsMetric = CombiningMatricesMetric.NORMALIZED_EUCLIDEAN;
    private double[] channelsWeights = {};
    private double additionalMultiplier = 1.0;
    private boolean onlyFirst3Channels = true;
    private boolean floatResult = true;

    protected MultichannelDerivativesFilter() {
    }

    public boolean isGaussianBlurOfSource() {
        return gaussianBlurOfSource;
    }

    public MultichannelDerivativesFilter setGaussianBlurOfSource(boolean gaussianBlurOfSource) {
        this.gaussianBlurOfSource = gaussianBlurOfSource;
        return this;
    }

    public int getGaussianBlurKernelSizeX() {
        return gaussianBlurKernelSizeX;
    }

    public MultichannelDerivativesFilter setGaussianBlurKernelSizeX(int gaussianBlurKernelSizeX) {
        this.gaussianBlurKernelSizeX = nonNegative(gaussianBlurKernelSizeX);
        return this;
    }

    public int getGaussianBlurKernelSizeY() {
        return gaussianBlurKernelSizeY;
    }

    public MultichannelDerivativesFilter setGaussianBlurKernelSizeY(int gaussianBlurKernelSizeY) {
        this.gaussianBlurKernelSizeY = nonNegative(gaussianBlurKernelSizeY);
        return this;
    }

    public ContinuationMode getContinuationMode() {
        return continuationMode;
    }

    public MultichannelDerivativesFilter setContinuationMode(ContinuationMode continuationMode) {
        this.continuationMode = nonNull(continuationMode);
        return this;
    }

    public CombiningMatricesMetric getCombiningChannelsMetric() {
        return combiningChannelsMetric;
    }

    public MultichannelDerivativesFilter setCombiningChannelsMetric(CombiningMatricesMetric combiningChannelsMetric) {
        this.combiningChannelsMetric = nonNull(combiningChannelsMetric);
        return this;
    }

    public double[] getChannelsWeights() {
        return channelsWeights;
    }

    public MultichannelDerivativesFilter setChannelsWeights(double[] channelsWeights) {
        this.channelsWeights = nonNull(channelsWeights).clone();
        return this;
    }

    public MultichannelDerivativesFilter setChannelsWeights(String channelsWeights) {
        this.channelsWeights = new SScalar(nonNull(channelsWeights)).toDoubles();
        return this;
    }

    public double getAdditionalMultiplier() {
        return additionalMultiplier;
    }

    public MultichannelDerivativesFilter setAdditionalMultiplier(double additionalMultiplier) {
        this.additionalMultiplier = additionalMultiplier;
        return this;
    }

    public boolean isOnlyFirst3Channels() {
        return onlyFirst3Channels;
    }

    public MultichannelDerivativesFilter setOnlyFirst3Channels(boolean onlyFirst3Channels) {
        this.onlyFirst3Channels = onlyFirst3Channels;
        return this;
    }

    public boolean isFloatResult() {
        return floatResult;
    }

    public MultichannelDerivativesFilter setFloatResult(boolean floatResult) {
        this.floatResult = floatResult;
        return this;
    }

    @Override
    public void process() {
        if (gaussianBlurOfSource) {
            final SMat inputMat = getInputMat();
            if (floatResult && !inputMat.getDepth().isFloatingPoint()) {
                try (ChangePrecision changePrecision = new ChangePrecision()) {
                    changePrecision.setRawCast(false);
                    changePrecision.setElementType(float.class);
                    inputMat.setTo(changePrecision.process(inputMat));
                }
            }
            GaussianBlur.blurIמPlace(inputMat, gaussianBlurKernelSizeX, gaussianBlurKernelSizeY, false);
        }
        super.process();
    }

    public abstract MultiMatrix2D process(MultiMatrix2D source);

    public Convolution createConvolution() {
        Convolution convolution = BasicConvolution.getInstance(null, false);
        if (continuationMode.continuationModeOrNull() != null) {
            convolution = ContinuedConvolution.getInstance(convolution, continuationMode.continuationModeOrNull());
        }
        return convolution;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    protected final MultiMatrix2D preprocess(MultiMatrix2D source) {
        if (floatResult && !source.isFloatingPoint()) {
            source = source.asPrecision(float.class).clone();
        }
        if (combiningChannelsMetric.isSingleChannel()) {
            source = source.isColor() ? source.asMono().clone() : source.asOtherNumberOfChannels(1);
        } else if (onlyFirst3Channels) {
            source = source.asOtherNumberOfChannels(Math.min(source.numberOfChannels(), 3));
        }
        return source;
    }

    protected final Matrix<? extends PArray> combineResult(
            Class<? extends PArray> requiredType,
            List<Matrix<? extends PArray>> processedChannels) {
        return Matrices.clone(combiningChannelsMetric.combine(
                requiredType,
                processedChannels,
                channelsWeights,
                additionalMultiplier));
    }

    protected static Class<? extends PFloatingArray> floatingType(Class<?> elementType) {
        return elementType == double.class ? DoubleArray.class : FloatArray.class;
    }
}
