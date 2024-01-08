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
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class CornerEigenValsAndVecs extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    public enum ResultValue {
        NONE(-1, null),
        LAMBDA_1(0, "lambda1"),
        LAMBDA_2(1, "lambda2"),
        X_1(2, "x1"),
        Y_1(3, "y1"),
        X_2(4, "x2"),
        Y_2(5, "y2");

        private final int valueIndex;
        private final String valueName;

        ResultValue(int valueIndex, String valueName) {
            this.valueIndex = valueIndex;
            this.valueName = valueName;
        }

        public String valueName() {
            return valueName;
        }
    }

    private int blockSize = 15;
    private int kernelSizeSobel = 5;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public CornerEigenValsAndVecs() {
        useVisibleResultParameter();
        for (ResultValue resultValue : ResultValue.values()) {
            final String portName = resultValue.valueName;
            if (portName != null) {
                addOutputMat(portName);
                // - necessary to provide ports for process() even if they are not specified in JSON configuration
            }
        }
    }

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

    public OBorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
    }

    @Override
    public void process(Mat result, Mat source) {
        Mat mat = source;
        try {
            if (source.channels() > 1) {
                mat = new Mat();
                opencv_imgproc.cvtColor(source, mat, opencv_imgproc.COLOR_BGR2GRAY);
            }
            logDebug(() -> "Eigenvalues and eigenvectors: block size " + blockSize
                    + ", kernel size for Sobel " + kernelSizeSobel + " (source: " + source + ")");
            opencv_imgproc.cornerEigenValsAndVecs(mat, result, blockSize, kernelSizeSobel, borderType.code());
            setEndProcessingTimeStamp();
            for (ResultValue resultValue : ResultValue.values()) {
                // No check for necessity of this output port: extracting channel is a quick operation,
                // so, all output data are always available.
                if (hasOutputPort(resultValue.valueName)) {
                    logDebug(() -> "Eigenvalues and eigenvectors: returning " + resultValue.valueName);
                    final Mat resultChannel = new Mat();
                    opencv_core.extractChannel(result, resultChannel, resultValue.valueIndex);
                    O2SMat.setTo(getMat(resultValue.valueName), resultChannel);
                }
            }
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
            mat = OTools.toMonoIfNot(mat);
            logDebug(() -> "Eigenvalues and eigenvectors (GPU): block size " + blockSize
                    + ", kernel size for Sobel " + kernelSizeSobel + " (source: " + source + ")");
            opencv_imgproc.cornerEigenValsAndVecs(mat, result, blockSize, kernelSizeSobel, borderType.code());
            setEndProcessingTimeStamp();
            for (ResultValue resultValue : ResultValue.values()) {
                // No check for necessity of this output port: extracting channel is a quick operation,
                // so, all output data are always available.
                if (hasOutputPort(resultValue.valueName)) {
                    logDebug(() -> "Eigenvalues and eigenvectors: returning " + resultValue.valueName);
                    final UMat resultChannel = new UMat();
                    opencv_core.extractChannel(result, resultChannel, resultValue.valueIndex);
                    O2SMat.setTo(getMat(resultValue.valueName), resultChannel);
                }
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }
}
