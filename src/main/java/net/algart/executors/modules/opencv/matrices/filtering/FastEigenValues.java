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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.common.OpenCVExecutor;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class FastEigenValues extends OpenCVExecutor implements ReadOnlyExecutionInput {

    private static final Scalar zeroScalar = new Scalar(0.0);
    // - must not be public: it is mutable

    public enum ResultValue {
        LAMBDA_1("lambda1") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                opencv_core.add(laplacian, determinant, result);
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                opencv_core.add(laplacian, determinant, result);
            }
        },
        LAMBDA_2("lambda2") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                opencv_core.subtract(laplacian, determinant, result);
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                opencv_core.subtract(laplacian, determinant, result);
            }
        },
        LAMBDA_1_PLUS("lambda1_plus") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                LAMBDA_1.eigenValue(result, laplacian, determinant);
                try (Mat zero = new Mat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.max(result, zero, result);
                }
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                LAMBDA_1.eigenValue(result, laplacian, determinant);
                try (UMat zero = new UMat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.max(result, zero, result);
                }
            }
        },
        LAMBDA_1_MINUS("lambda1_minus") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                LAMBDA_1.eigenValue(result, laplacian, determinant);
                try (Mat zero = new Mat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.min(result, zero, result);
                }
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                LAMBDA_1.eigenValue(result, laplacian, determinant);
                try (UMat zero = new UMat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.min(result, zero, result);
                }
            }
        },
        LAMBDA_2_PLUS("lambda2_plus") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                LAMBDA_2.eigenValue(result, laplacian, determinant);
                try (Mat zero = new Mat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.max(result, zero, result);
                }
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                LAMBDA_2.eigenValue(result, laplacian, determinant);
                try (UMat zero = new UMat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.max(result, zero, result);
                }
            }
        },
        LAMBDA_2_MINUS("lambda2_minus") {
            void eigenValue(Mat result, Mat laplacian, Mat determinant) {
                LAMBDA_2.eigenValue(result, laplacian, determinant);
                try (Mat zero = new Mat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)
                ) {
                    opencv_core.min(result, zero, result);
                }
            }

            void eigenValue(UMat result, UMat laplacian, UMat determinant) {
                LAMBDA_2.eigenValue(result, laplacian, determinant);
                try (UMat zero = new UMat(
                        result.rows(), result.cols(), result.depth(), zeroScalar)) {
                    opencv_core.min(result, zero, result);
                }
            }
        };

        private final String valueName;

        ResultValue(String valueName) {
            this.valueName = valueName;
        }

        public String valueName() {
            return valueName;
        }

        abstract void eigenValue(Mat result, Mat laplacian, Mat determinant);

        abstract void eigenValue(UMat result, UMat laplacian, UMat determinant);
    }

    private static final int RESULT_DEPTH = opencv_core.CV_32F;

    private OBorderType borderType = OBorderType.BORDER_DEFAULT;
    private double scale = 1.0;

    public FastEigenValues() {
        useVisibleResultParameter();
        setDefaultInputMat(DEFAULT_INPUT_PORT);
        for (ResultValue resultValue : ResultValue.values()) {
            final String portName = resultValue.valueName();
            addOutputMat(portName);
            // - necessary to provide ports for process() even if they are not specified in JSON configuration
        }
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public FastEigenValues setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
        return this;
    }

    public double getScale() {
        return scale;
    }

    public FastEigenValues setScale(double scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public void process() {
        if (isUseGPU()) {
            hessian(O2SMat.toUMat(getInputMat()));
        } else {
            hessian(O2SMat.toMat(getInputMat()));
        }
    }

    public void hessian(Mat source) {
        setStartProcessingTimeStamp();
        final int borderType = this.borderType.code();
        final double multD = 0.5 * scale / OTools.maxPossibleValue(source);
        final float multF = (float) multD;
        Mat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            try (Pointer p = new FloatPointer(0.0f, -multF, 0.0f, multF, 0.0f, multF, 0.0f, -multF, 0.0f);
                 Mat kernel = new Mat(3, 3, opencv_core.CV_32F, p);
                 Mat d2DxDy = new Mat();
                 Mat laplacian = new Mat();
                 Mat determinant = new Mat()) {
                opencv_imgproc.Laplacian(mat, laplacian, RESULT_DEPTH, 1, multD, 0.0, borderType);
                opencv_imgproc.Sobel(mat, d2DxDy,
                        RESULT_DEPTH, 1, 1, 1, 0.5 * multD, 0.0, borderType);
                opencv_imgproc.filter2D(mat, determinant, RESULT_DEPTH, kernel, null, 0.0, borderType);
                opencv_core.magnitude(d2DxDy, determinant, determinant);

                for (ResultValue resultValue : ResultValue.values()) {
                    if (isOutputNecessary(resultValue.valueName)) {
                        Mat result = new Mat();
                        resultValue.eigenValue(result, laplacian, determinant);
                        O2SMat.setTo(getMat(resultValue.valueName), result);
                    }
                }
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
        setEndProcessingTimeStamp();
    }

    public void hessian(UMat source) {
        setStartProcessingTimeStamp();
        final int borderType = this.borderType.code();
        final double multD = 0.5 * scale / OTools.maxPossibleValue(source);
        final float multF = (float) multD;
        UMat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            try (Pointer p = new FloatPointer(0.0f, -multF, 0.0f, multF, 0.0f, multF, 0.0f, -multF, 0.0f);
                 Mat k = new Mat(3, 3, opencv_core.CV_32F, p);
                 UMat kernel = OTools.toUMat(k);
                 UMat d2DxDy = new UMat();
                 UMat laplacian = new UMat();
                 UMat determinant = new UMat()) {
                opencv_imgproc.Laplacian(mat, laplacian, RESULT_DEPTH, 1, multD, 0.0, borderType);
                opencv_imgproc.Sobel(mat, d2DxDy,
                        RESULT_DEPTH, 1, 1, 1, 0.5 * multD, 0.0, borderType);
                opencv_imgproc.filter2D(mat, determinant, RESULT_DEPTH, kernel, null, 0.0, borderType);
                opencv_core.magnitude(d2DxDy, determinant, determinant);

                for (ResultValue resultValue : ResultValue.values()) {
                    // No check for necessity of this output port: extracting channel is a quick operation,
                    // so, all output data are always available.
                    if (isOutputNecessary(resultValue.valueName)) {
                        UMat result = new UMat();
                        resultValue.eigenValue(result, laplacian, determinant);
                        O2SMat.setTo(getMat(resultValue.valueName), result);
                    }
                }
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
        setEndProcessingTimeStamp();
    }
}