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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_ximgproc;

public final class FastGlobalSmootherFilter extends AbstractFilterWithGuideImage {
    private double lambda = 1.0;
    private double sigmaColor = 0.3;
    private double lambdaAttenuation = 0.25;
    private int numberOfIterations = 3;

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double getSigmaColor() {
        return sigmaColor;
    }

    public void setSigmaColor(double sigmaColor) {
        this.sigmaColor = nonNegative(sigmaColor);
    }

    public double getLambdaAttenuation() {
        return lambdaAttenuation;
    }

    public void setLambdaAttenuation(double lambdaAttenuation) {
        this.lambdaAttenuation = lambdaAttenuation;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = nonNegative(numberOfIterations);
    }

    @Override
    public void process(Mat result, Mat source, Mat guide) {
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.FastGlobalSmootherFilter filter =
                     opencv_ximgproc.createFastGlobalSmootherFilter(
                             guide,
                             lambda,
                             sigmaColor * OTools.maxPossibleValue(source),
                             lambdaAttenuation,
                             numberOfIterations)) {
            addServiceTime(System.nanoTime() - tStart);
            filter.filter(source, result);
        }
    }

    @Override
    public void process(UMat result, UMat source, UMat guide) {
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.FastGlobalSmootherFilter filter =
                     opencv_ximgproc.createFastGlobalSmootherFilter(
                             guide,
                             lambda,
                             sigmaColor * OTools.maxPossibleValue(source),
                             lambdaAttenuation,
                             numberOfIterations)) {
            addServiceTime(System.nanoTime() - tStart);
            filter.filter(source, result);
        }
    }
}
