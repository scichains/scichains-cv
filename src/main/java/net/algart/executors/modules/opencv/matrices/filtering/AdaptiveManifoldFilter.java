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

package net.algart.executors.modules.opencv.matrices.filtering;

import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_core.*;

public final class AdaptiveManifoldFilter extends AbstractFilterWithGuideImage {
    private double sigmaS = 16;
    private double sigmaR = 0.2;
    private int treeHeight = -1;
    private int numPcaIterations = 1;
    private boolean adjustOutliers = false;
    private boolean useRNG = true;

    public double getSigmaS() {
        return sigmaS;
    }

    public void setSigmaS(double sigmaS) {
        this.sigmaS = nonNegative(sigmaS);
    }

    public double getSigmaR() {
        return sigmaR;
    }

    public void setSigmaR(double sigmaR) {
        this.sigmaR = nonNegative(sigmaR);
    }

    public int getTreeHeight() {
        return treeHeight;
    }

    public void setTreeHeight(int treeHeight) {
        this.treeHeight = treeHeight;
    }

    public int getNumPcaIterations() {
        return numPcaIterations;
    }

    public void setNumPcaIterations(int numPcaIterations) {
        this.numPcaIterations = nonNegative(numPcaIterations);
    }

    public boolean isAdjustOutliers() {
        return adjustOutliers;
    }

    public void setAdjustOutliers(boolean adjustOutliers) {
        this.adjustOutliers = adjustOutliers;
    }

    public boolean isUseRNG() {
        return useRNG;
    }

    public void setUseRNG(boolean useRNG) {
        this.useRNG = useRNG;
    }

    @Override
    public void process(Mat result, Mat source, Mat guide) {
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.AdaptiveManifoldFilter filter = opencv_ximgproc.createAMFilter(
                sigmaS,
                sigmaR,
                adjustOutliers)) {
            addServiceTime(System.nanoTime() - tStart);
            filter.setTreeHeight(treeHeight);
            filter.setPCAIterations(numPcaIterations);
            filter.setUseRNG(useRNG);
            filter.filter(source, result, guide);
        }
    }

    @Override
    public void process(UMat result, UMat source, UMat guide) {
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.AdaptiveManifoldFilter filter = opencv_ximgproc.createAMFilter(
                sigmaS,
                sigmaR,
                adjustOutliers)) {
            addServiceTime(System.nanoTime() - tStart);
            filter.setTreeHeight(treeHeight);
            filter.setPCAIterations(numPcaIterations);
            filter.setUseRNG(useRNG);
            filter.filter(source, result, guide);
        }
    }
}
