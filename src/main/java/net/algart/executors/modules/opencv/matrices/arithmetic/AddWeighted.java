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

package net.algart.executors.modules.opencv.matrices.arithmetic;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.opencv.common.VoidResultTwoUMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.ODepthOrUnchanged;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class AddWeighted extends VoidResultTwoUMatFilter implements ReadOnlyExecutionInput {
    private ODepthOrUnchanged resultDepth = ODepthOrUnchanged.UNCHANGED;
    private double alpha = 0.5;
    private double beta = 0.5;
    private double gamma = 0.0;

    public AddWeighted() {
        super("x", "y");
    }

    public ODepthOrUnchanged getResultDepth() {
        return resultDepth;
    }

    public AddWeighted setResultDepth(ODepthOrUnchanged resultDepth) {
        this.resultDepth = nonNull(resultDepth);
        return this;
    }

    public double getAlpha() {
        return alpha;
    }

    public AddWeighted setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    public double getBeta() {
        return beta;
    }

    public AddWeighted setBeta(double beta) {
        this.beta = beta;
        return this;
    }

    public double getGamma() {
        return gamma;
    }

    public AddWeighted setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    @Override
    public void process(Mat result, Mat source, Mat secondMat) {
        final double scaledGamma = this.gamma * OTools.maxPossibleValue(resultDepth.code(source.depth()));
        opencv_core.addWeighted(source, alpha, secondMat, beta, scaledGamma, result, resultDepth.code());

    }

    @Override
    public void process(UMat result, UMat source, UMat secondMat) {
        final double scaledGamma = this.gamma * OTools.maxPossibleValue(resultDepth.code(source.depth()));
        opencv_core.addWeighted(source, alpha, secondMat, beta, scaledGamma, result, resultDepth.code());
    }
}
