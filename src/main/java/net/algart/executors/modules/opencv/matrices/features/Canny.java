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

package net.algart.executors.modules.opencv.matrices.features;

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class Canny extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    private double thresholdLower = 0.1;
    private double thresholdUpper = 0.3;
    private int kernelSizeSobel = 3;
    private boolean moreAccurateGradient = false;

    public double getThresholdLower() {
        return thresholdLower;
    }

    public void setThresholdLower(double thresholdLower) {
        this.thresholdLower = nonNegative(thresholdLower);
    }

    public double getThresholdUpper() {
        return thresholdUpper;
    }

    public void setThresholdUpper(double thresholdUpper) {
        this.thresholdUpper = nonNegative(thresholdUpper);
    }

    public int getKernelSizeSobel() {
        return kernelSizeSobel;
    }

    public void setKernelSizeSobel(int kernelSizeSobel) {
        this.kernelSizeSobel = positive(kernelSizeSobel);
    }

    public boolean isMoreAccurateGradient() {
        return moreAccurateGradient;
    }

    public void setMoreAccurateGradient(boolean moreAccurateGradient) {
        this.moreAccurateGradient = moreAccurateGradient;
    }

    @Override
    public void process(Mat result, Mat source) {
        Mat mat = source;
        try {
            mat = OTools.to8UIfNot(mat);
            final double scale = OTools.maxPossibleValue(mat);
            opencv_imgproc.Canny(
                    mat,
                    result,
                    thresholdLower * scale,
                    thresholdUpper * scale,
                    kernelSizeSobel,
                    moreAccurateGradient);
        } finally {
            if (mat != source) {
                mat.close();
            }
        }
    }

    @Override
    public void process(UMat result, UMat source) {
        UMat mat = source;
        try {
            mat = OTools.to8UIfNot(mat);
            final double scale = OTools.maxPossibleValue(mat);
            opencv_imgproc.Canny(
                    mat,
                    result,
                    thresholdLower * scale,
                    thresholdUpper * scale,
                    kernelSizeSobel,
                    moreAccurateGradient);
        } finally {
            if (mat != source) {
                mat.close();
            }
        }
    }
}
