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

package net.algart.executors.modules.opencv.matrices.segmentation;

import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class SuperpixelSEEDS extends AbstractSuperpixel {
    private int numberOfSuperpixels = 100;
    private int numberOfLevels = 4;
    private int numberOfHistogramBins = 5;
    private int prior = 4;
    private boolean doubleStep = false;
    private int numberOfIterations = 10;

    public int getNumberOfSuperpixels() {
        return numberOfSuperpixels;
    }

    public void setNumberOfSuperpixels(int numberOfSuperpixels) {
        this.numberOfSuperpixels = positive(numberOfSuperpixels);
    }

    public int getNumberOfLevels() {
        return numberOfLevels;
    }

    public void setNumberOfLevels(int numberOfLevels) {
        this.numberOfLevels = positive(numberOfLevels);
    }

    public int getNumberOfHistogramBins() {
        return numberOfHistogramBins;
    }

    public void setNumberOfHistogramBins(int numberOfHistogramBins) {
        this.numberOfHistogramBins = positive(numberOfHistogramBins);
    }

    public int getPrior() {
        return prior;
    }

    public void setPrior(int prior) {
        this.prior = prior;
    }

    public boolean isDoubleStep() {
        return doubleStep;
    }

    public void setDoubleStep(boolean doubleStep) {
        this.doubleStep = doubleStep;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public void setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = positive(numberOfIterations);
    }

    @Override
    public Mat process(Mat source) {
        Mat result = new Mat();
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.SuperpixelSEEDS superpixel =
                     opencv_ximgproc.createSuperpixelSEEDS(
                             source.cols(),
                             source.rows(),
                             source.channels(),
                             numberOfSuperpixels,
                             numberOfLevels,
                             prior,
                             numberOfHistogramBins,
                             doubleStep)) {
            addServiceTime(System.nanoTime() - tStart);
            superpixel.iterate(source, numberOfIterations);
            logDebug(() -> "Superpixel SEEDS found " + superpixel.getNumberOfSuperpixels() + " superpixels"
                    + "; requested number of superpixels " + numberOfSuperpixels
                    + ", number of levels " + numberOfLevels
                    + ", number of histogram bins " + numberOfHistogramBins
                    + ", prior " + prior
                    + " (source: " + source + ")");
            superpixel.getLabels(result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(
                    (boundaries, labels, needThick) ->
                            superpixel.getLabelContourMask(boundaries, drawingBoundariesStyle.thickBoundaries),
                    source, result);
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        UMat result = new UMat();
        long tStart = System.nanoTime();
        try (final org.bytedeco.opencv.opencv_ximgproc.SuperpixelSEEDS superpixel =
                     opencv_ximgproc.createSuperpixelSEEDS(
                             source.cols(),
                             source.rows(),
                             source.channels(),
                             numberOfSuperpixels,
                             numberOfLevels,
                             prior,
                             numberOfHistogramBins,
                             doubleStep)) {
            addServiceTime(System.nanoTime() - tStart);
            superpixel.iterate(source, numberOfIterations);
            logDebug(() -> "Superpixel SEEDS found " + superpixel.getNumberOfSuperpixels() + " superpixels"
                    + "; requested number of superpixels " + numberOfSuperpixels
                    + ", number of levels " + numberOfLevels
                    + ", number of histogram bins " + numberOfHistogramBins
                    + ", prior " + prior
                    + " (source: " + source + ")");
            superpixel.getLabels(result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(
                    (boundaries, labels, needThick) ->
                            superpixel.getLabelContourMask(boundaries, drawingBoundariesStyle.thickBoundaries),
                    source, result);
        }
        return result;
    }
}
