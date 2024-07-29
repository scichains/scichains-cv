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

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.enums.OInterpolation;
import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_core.*;

public final class BilateralTextureFilter extends VoidResultUMatFilter {
    private int filterRadius = 5;
    private int numberOfIterations = 3;
    private double sigmaAlpha = -1;
    private double sigmaAvg = -1;

    public BilateralTextureFilter() {
        setStretchingInterpolation(OInterpolation.INTER_LINEAR);
    }

    public int getFilterRadius() {
        return filterRadius;
    }

    public void setFilterRadius(int filterRadius) {
        this.filterRadius = positive(filterRadius, "diameter of neighborhood");
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = positive(numberOfIterations);
    }

    public double getSigmaAlpha() {
        return sigmaAlpha;
    }

    public void setSigmaAlpha(double sigmaAlpha) {
        this.sigmaAlpha = sigmaAlpha;
    }

    public double getSigmaAvg() {
        return sigmaAvg;
    }

    public void setSigmaAvg(double sigmaAvg) {
        this.sigmaAvg = sigmaAvg;
    }

    @Override
    public void process(Mat result, Mat source) {
        logDebug(() -> "Bilateral texture filter: fr = " + filterRadius
                + ", sigmaAlpha = " + sigmaAlpha + ", sigmaAvg = " + sigmaAvg
                + " (source: " + source + ")");
        opencv_ximgproc.bilateralTextureFilter(
                source,
                result,
                filterRadius,
                numberOfIterations,
                sigmaAlpha,
                sigmaAvg);
    }

    @Override
    public void process(UMat result, UMat source) {
        logDebug(() -> "Bilateral texture filter (GPU): fr = " + filterRadius
                + ", sigmaAlpha = " + sigmaAlpha + ", sigmaAvg = " + sigmaAvg
                + " (source: " + source + ")");
        opencv_ximgproc.bilateralTextureFilter(
                source,
                result,
                filterRadius,
                numberOfIterations,
                sigmaAlpha,
                sigmaAvg);
    }
}
