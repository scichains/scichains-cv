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

package net.algart.executors.modules.opencv.matrices.features;

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class CornerHarris extends VoidResultUMatFilter {
    private int blockSize = 15;
    private int kernelSizeSobel = 5;
    private double k = 0.05;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = positive(blockSize);
    }

    public int getKernelSizeSobel() {
        return kernelSizeSobel;
    }

    public void setKernelSizeSobel(int kernelSizeSobel) {
        this.kernelSizeSobel = positive(kernelSizeSobel);
    }

    public double getK() {
        return k;
    }

    public void setK(double k) {
        this.k = k;
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
    }

    @Override
    public void process(Mat result, Mat source) {
        if (source.channels() > 1) {
            opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_BGR2GRAY);
        }
        opencv_imgproc.cornerHarris(source, result, blockSize, kernelSizeSobel | 1, k, borderType.code());
    }

    @Override
    public void process(UMat result, UMat source) {
        if (source.channels() > 1) {
            opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_BGR2GRAY);
        }
        opencv_imgproc.cornerHarris(source, result, blockSize, kernelSizeSobel | 1, k, borderType.code());
    }
}
