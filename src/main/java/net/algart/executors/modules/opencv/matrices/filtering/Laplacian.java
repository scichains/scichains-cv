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

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.executors.modules.opencv.util.enums.ODepthOrUnchanged;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class Laplacian extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    private ODepthOrUnchanged resultDepth = ODepthOrUnchanged.UNCHANGED;
    private int kernelSize = 1;
    private double scale = 1.0;
    private double delta = 0.0;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public ODepthOrUnchanged getResultDepth() {
        return resultDepth;
    }

    public void setResultDepth(ODepthOrUnchanged resultDepth) {
        this.resultDepth = nonNull(resultDepth);
    }

    public int getKernelSize() {
        return kernelSize;
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = positive(kernelSize);
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
    }

    @Override
    public void process(Mat result, Mat source) {
        opencv_imgproc.Laplacian(source, result, resultDepth.code(), kernelSize, scale, delta, borderType.code());
    }

    @Override
    public void process(UMat result, UMat source) {
        opencv_imgproc.Laplacian(source, result, resultDepth.code(), kernelSize, scale, delta, borderType.code());
    }
}
