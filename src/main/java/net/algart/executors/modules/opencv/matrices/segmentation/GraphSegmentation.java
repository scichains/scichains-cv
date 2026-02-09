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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class GraphSegmentation extends AbstractSuperpixel {
    private double sigma = 0.5;
    private double k = 1.5;
    private int minSize = 100;

    public double getSigma() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = nonNegative(minSize);
    }

    @Override
    public Mat process(Mat source) {
        Mat result = new Mat();
        try (final org.bytedeco.opencv.opencv_ximgproc.GraphSegmentation graphSegmentation =
                     opencv_ximgproc.createGraphSegmentation(
                             sigma,
                             (float) (k * OTools.maxPossibleValue(source)),
                             minSize)) {
            logDebug(() -> "GraphSegmentation: sigma " + sigma + ", K " + k + ", minSize " + minSize
                    + " (source: " + source + ")");
            graphSegmentation.processImage(source, result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(BoundariesExtractor.DEFAULT, source, result);
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        UMat result = new UMat();
        try (final org.bytedeco.opencv.opencv_ximgproc.GraphSegmentation graphSegmentation =
                     opencv_ximgproc.createGraphSegmentation(
                             sigma,
                             (float) (k * OTools.maxPossibleValue(source)),
                             minSize)) {
            logDebug(() -> "GraphSegmentation: sigma " + sigma + ", K " + k + ", minSize " + minSize
                    + " (source: " + source + ")");
            graphSegmentation.processImage(source, result);
            makeLabelsPositiveIfRequired(result);
            setToOutputBoundaries(BoundariesExtractor.DEFAULT, source, result);
        }
        return result;
    }
}
