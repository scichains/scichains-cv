/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_ximgproc;

public final class SuperpixelLSC extends AbstractSuperpixel {
    private int regionSize = 10;
    private double ratio = 0.075;
    private int enforceLabelConnectivityMinElementSize = 0;
    private int numberOfIterations = 10;

    public int getRegionSize() {
        return regionSize;
    }

    public void setRegionSize(int regionSize) {
        this.regionSize = nonNegative(regionSize);
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = nonNegative(ratio);
    }

    public int getEnforceLabelConnectivityMinElementSize() {
        return enforceLabelConnectivityMinElementSize;
    }

    public void setEnforceLabelConnectivityMinElementSize(int enforceLabelConnectivityMinElementSize) {
        this.enforceLabelConnectivityMinElementSize = nonNegative(enforceLabelConnectivityMinElementSize);
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
        try (final org.bytedeco.opencv.opencv_ximgproc.SuperpixelLSC superpixel = opencv_ximgproc.createSuperpixelLSC(
                source,
                regionSize,
                (float) ratio)) {
            superpixel.iterate(numberOfIterations);
            logDebug(() -> "Superpixel LSC found " + superpixel.getNumberOfSuperpixels() + " superpixels"
                    + "; regionSize " + regionSize + ", ratio " + ratio
                    + " (source: " + source + ")");
            if (enforceLabelConnectivityMinElementSize > 0) {
                superpixel.enforceLabelConnectivity(enforceLabelConnectivityMinElementSize);
            }
            superpixel.getLabels(result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(
                    (boundaries, labels, needThick) ->
                            superpixel.getLabelContourMask(boundaries, drawingBoundariesStyle.thickBoundaries),
                    source, result);
            // - don't know why, but thickBoundaries flag has non-trivial, almost reverse effect in SuperpixelLSC
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        UMat result = new UMat();
        try (final org.bytedeco.opencv.opencv_ximgproc.SuperpixelLSC superpixel = opencv_ximgproc.createSuperpixelLSC(
                source,
                regionSize,
                (float) ratio)) {
            superpixel.iterate(numberOfIterations);
            logDebug(() -> "Superpixel LSC found " + superpixel.getNumberOfSuperpixels() + " superpixels"
                    + "; regionSize " + regionSize + ", ratio " + ratio
                    + " (source: " + source + ")");
            if (enforceLabelConnectivityMinElementSize > 0) {
                superpixel.enforceLabelConnectivity(enforceLabelConnectivityMinElementSize);
            }
            superpixel.getLabels(result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(
                    (boundaries, labels, needThick) ->
                            superpixel.getLabelContourMask(boundaries, drawingBoundariesStyle.thickBoundaries),
                    source, result);
            // - don't know why, but thickBoundaries flag has non-trivial, almost reverse effect in SuperpixelLSC
        }
        return result;
    }
}
