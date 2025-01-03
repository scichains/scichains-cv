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

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OThreshType;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_ximgproc;

public final class Threshold extends UMatFilter {
    public enum Algorithm {
        SIMPLE(0, false, false),
        OTSU(opencv_imgproc.CV_THRESH_OTSU, false, false),
        TRIANGLE(opencv_imgproc.CV_THRESH_TRIANGLE, false, false),
        NIBLACK(opencv_ximgproc.BINARIZATION_NIBLACK, true, false),
        SAUVOLA(opencv_ximgproc.BINARIZATION_SAUVOLA, true, false),
        WOLF(opencv_ximgproc.BINARIZATION_WOLF, true, false),
        NICK(opencv_ximgproc.BINARIZATION_NICK, true, false),
        ADAPTIVE_MEAN(opencv_imgproc.CV_ADAPTIVE_THRESH_MEAN_C, false, true),
        ADAPTIVE_GAUSSIAN_C(opencv_imgproc.CV_ADAPTIVE_THRESH_GAUSSIAN_C, false, true);

        private final int subMethod;
        private final boolean niblack;
        private final boolean adaptive;

        Algorithm(int subMethod, boolean niblack, boolean adaptive) {
            this.subMethod = subMethod;
            this.niblack = niblack;
            this.adaptive = adaptive;
        }
    }

    private Algorithm algorithm = Algorithm.SIMPLE;
    private OThreshType thresholdType = OThreshType.PACKED_BITS;
    private double threshold = 0.5;
    private double maxValue = 1.0;
    private boolean rawValues = false;
    private int gaussianBlurKernelSize = 0;
    private int kernelSize = 5;
    private double k = 0.0;
    private double r = 128.0;
    private boolean convertToByte = false;

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public Threshold setAlgorithm(Algorithm algorithm) {
        this.algorithm = nonNull(algorithm);
        return this;
    }

    public OThreshType getThresholdType() {
        return thresholdType;
    }

    public Threshold setThresholdType(OThreshType thresholdType) {
        this.thresholdType = nonNull(thresholdType);
        return this;
    }

    public double getThreshold() {
        return threshold;
    }

    public Threshold setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public Threshold setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public Threshold setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public int getGaussianBlurKernelSize() {
        return gaussianBlurKernelSize;
    }

    public Threshold setGaussianBlurKernelSize(int gaussianBlurKernelSize) {
        this.gaussianBlurKernelSize = nonNegative(gaussianBlurKernelSize);
        return this;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    public Threshold setKernelSize(int kernelSize) {
        this.kernelSize = nonNegative(kernelSize);
        return this;
    }

    public double getK() {
        return k;
    }

    public Threshold setK(double k) {
        this.k = k;
        return this;
    }

    public double getR() {
        return r;
    }

    public Threshold setR(double r) {
        this.r = r;
        return this;
    }

    public boolean isConvertToByte() {
        return convertToByte;
    }

    public Threshold setConvertToByte(boolean convertToByte) {
        this.convertToByte = convertToByte;
        return this;
    }

    @Override
    public Mat process(Mat source) {
        final double scale = rawValues ? 1.0 : OTools.maxPossibleValue(source);
        if (convertToByte && source.depth() != opencv_core.CV_8U) {
            source.convertTo(source, opencv_core.CV_8U, 255.0 / OTools.maxPossibleValue(source), 0);
        }
        if (algorithm != Algorithm.SIMPLE && source.channels() > 1) {
            // other cases do not work with RGB image
            opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_BGR2GRAY);
        }
        if (gaussianBlurKernelSize > 0) {
            final int correctedSize = gaussianBlurKernelSize | 0x1;
            try (Size size = new Size(correctedSize, correctedSize)) {
                opencv_imgproc.GaussianBlur(source, source, size, 0.0, 0.0, opencv_core.BORDER_DEFAULT);
            }
        }
        final Mat result = new Mat();
        if (algorithm.niblack) {
            opencv_ximgproc.niBlackThreshold(
                    source,
                    result,
                    maxValue * scale,
                    thresholdType.code(),
                    kernelSize | 0x1,
                    k,
                    algorithm.subMethod,
                    r);

        } else if (algorithm.adaptive) {
            opencv_imgproc.adaptiveThreshold(
                    source,
                    result,
                    maxValue * scale,
                    algorithm.subMethod,
                    thresholdType.code(),
                    kernelSize | 0x1,
                    k);
        } else {
            opencv_imgproc.threshold(
                    source,
                    result,
                    threshold * scale,
                    maxValue * scale,
                    thresholdType.code() | algorithm.subMethod);
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        final double scale = rawValues ? 1.0 : OTools.maxPossibleValue(source);
        if (convertToByte && source.depth() != opencv_core.CV_8U) {
            source.convertTo(source, opencv_core.CV_8U, 255.0 / OTools.maxPossibleValue(source), 0);
        }
        if (algorithm != Algorithm.SIMPLE && source.channels() > 1) {
            // other cases do not work with RGB image
            opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_BGR2GRAY);
        }
        if (gaussianBlurKernelSize > 0) {
            final int correctedSize = gaussianBlurKernelSize | 0x1;
            try (Size size = new Size(correctedSize, correctedSize)) {
                opencv_imgproc.GaussianBlur(source, source, size, 0.0, 0.0, opencv_core.BORDER_DEFAULT);
            }
        }
        final UMat result = new UMat();
        if (algorithm.niblack) {
            opencv_ximgproc.niBlackThreshold(
                    source,
                    result,
                    maxValue * scale,
                    thresholdType.code(),
                    kernelSize | 0x1,
                    k,
                    algorithm.subMethod,
                    r);

        } else if (algorithm.adaptive) {
            opencv_imgproc.adaptiveThreshold(
                    source,
                    result,
                    maxValue * scale,
                    algorithm.subMethod,
                    thresholdType.code(),
                    kernelSize | 0x1,
                    k);
        } else {
            opencv_imgproc.threshold(
                    source,
                    result,
                    threshold * scale,
                    maxValue * scale,
                    thresholdType.code() | algorithm.subMethod);
        }
        return result;
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }

    @Override
    protected boolean packOutputBits() {
        return thresholdType.packedBits();
    }
}
