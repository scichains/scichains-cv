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

package net.algart.executors.modules.opencv.matrices.conversions;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.ODepth;
import net.algart.executors.modules.opencv.util.enums.ODepthOrUnchanged;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class ConvertPrecision extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    public enum ConvertMode {
        RAW,
        AUTO,
        CUSTOM
    }

    private ConvertMode convertMode = ConvertMode.AUTO;
    private ODepthOrUnchanged resultDepth = ODepthOrUnchanged.UNCHANGED;
    private double customAlpha = 1.0;
    private double customBeta = 0.0;

    public ConvertMode getConvertMode() {
        return convertMode;
    }

    public ConvertPrecision setConvertMode(ConvertMode convertMode) {
        this.convertMode = nonNull(convertMode);
        return this;
    }

    public ODepthOrUnchanged getResultDepth() {
        return resultDepth;
    }

    public ConvertPrecision setResultDepth(ODepthOrUnchanged resultDepth) {
        this.resultDepth = nonNull(resultDepth);
        return this;
    }

    public double getCustomAlpha() {
        return customAlpha;
    }

    public ConvertPrecision setCustomAlpha(double customAlpha) {
        this.customAlpha = customAlpha;
        return this;
    }

    public double getCustomBeta() {
        return customBeta;
    }

    public ConvertPrecision setCustomBeta(double customBeta) {
        this.customBeta = customBeta;
        return this;
    }

    @Override
    public void process(Mat result, Mat source) {
        final int resultDepthCode = resultDepth.code(source.depth());
        source.convertTo(result, resultDepthCode, alpha(source.depth()), beta(resultDepthCode));
    }

    @Override
    public void process(UMat result, UMat source) {
        final int resultDepthCode = resultDepth.code(source.depth());
        source.convertTo(result, resultDepthCode, alpha(source.depth()), beta(resultDepthCode));
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }

    private double alpha(int currentDepth) {
        switch (convertMode) {
            case AUTO:
                return resultDepth == ODepthOrUnchanged.UNCHANGED ?
                        1.0 :
                        resultDepth.maxValue() / ODepth.of(currentDepth).maxValue();
            case RAW:
                return 1.0;
            case CUSTOM:
                return customAlpha;
            default:
                throw new AssertionError("Unsupported " + convertMode);
        }
    }

    private double beta(int resultDepth) {
        switch (convertMode) {
            case AUTO:
            case RAW:
                return 0.0;
            case CUSTOM:
                return customBeta * OTools.maxPossibleValue(resultDepth);
            default:
                throw new AssertionError("Unsupported " + convertMode);
        }
    }
}
