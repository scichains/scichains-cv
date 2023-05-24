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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.executors.modules.util.opencv.enums.OBorderType;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class FastGradientVariance extends UMatFilter implements ReadOnlyExecutionInput {

    private static final int RESULT_DEPTH = opencv_core.CV_32F;

    private OBorderType borderType = OBorderType.BORDER_DEFAULT;
    private double scale = 1.0;

    public FastGradientVariance() {
        setDefaultInputMat(DEFAULT_INPUT_PORT);
        setDefaultOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public FastGradientVariance setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
        return this;
    }

    public double getScale() {
        return scale;
    }

    public FastGradientVariance setScale(double scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public Mat process(Mat source) {
        final int borderType = this.borderType.code();
        final double scale = this.scale / OTools.maxPossibleValue(source);
        Mat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            try (Mat dxy = new Mat();
                 Mat gx = new Mat();
                 Mat gy = new Mat();
                 Mat vx = new Mat(); //temp dxx
                 Mat vy = new Mat() //temp dyy
            ) {
                opencv_imgproc.Sobel(mat, gx, RESULT_DEPTH,
                        1, 0, 1, 0.5 * scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, gy, RESULT_DEPTH,
                        0, 1, 1, 0.5 * scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, vx, RESULT_DEPTH,
                        2, 0, 1, scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, vy, RESULT_DEPTH,
                        0, 2, 1, scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, dxy, RESULT_DEPTH,
                        1, 1, 1, 0.25 * scale, 0.0, borderType);
                Mat result = new Mat();
                variance(vx, dxy, gx, gy, vx, result);
                variance(dxy, vy, gx, gy, vy, result);
                opencv_core.magnitude(gx, gy, dxy);
                opencv_core.magnitude(vx, vy, result);
                opencv_core.divide(result, dxy, result);
                return result;
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    @Override
    public UMat process(UMat source) {
        final int borderType = this.borderType.code();
        final double scale = this.scale / OTools.maxPossibleValue(source);
        UMat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            try (UMat dxy = new UMat();
                 UMat gx = new UMat();
                 UMat gy = new UMat();
                 UMat vx = new UMat(); //temp dxx
                 UMat vy = new UMat() //temp dyy
            ) {
                opencv_imgproc.Sobel(mat, gx, RESULT_DEPTH,
                        1, 0, 1, 0.5 * scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, gy, RESULT_DEPTH,
                        0, 1, 1, 0.5 * scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, vx, RESULT_DEPTH,
                        2, 0, 1, scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, vy, RESULT_DEPTH,
                        0, 2, 1, scale, 0.0, borderType);
                opencv_imgproc.Sobel(mat, dxy, RESULT_DEPTH,
                        1, 1, 1, 0.25 * scale, 0.0, borderType);
                UMat result = new UMat();
                variance(vx, dxy, gx, gy, vx, result);
                variance(dxy, vy, gx, gy, vy, result);
                opencv_core.magnitude(gx, gy, dxy);
                opencv_core.magnitude(vx, vy, result);
                opencv_core.divide(result, dxy, result);
                return result;
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    private void variance(Mat vx, Mat vy,
                          Mat gx, Mat gy,
                          Mat result, Mat temp) {
        opencv_core.multiply(vx, gy, temp, -1.0, -1);
        opencv_core.multiply(vy, gx, result);
        opencv_core.add(result, temp, result);
    }

    private void variance(UMat vx, UMat vy,
                          UMat gx, UMat gy,
                          UMat result, UMat temp) {
        opencv_core.multiply(vx, gy, temp, -1.0, -1);
        opencv_core.multiply(vy, gx, result);
        opencv_core.add(result, temp, result);
    }
}