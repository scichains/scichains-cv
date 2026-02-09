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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.multimatrix.MultiMatrix2D;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.UMat;

public class GaussianBlur extends UMatFilter {
    private int kernelSizeX = 15;
    private int kernelSizeY = 0;
    private double sigmaX = 0.0;
    private double sigmaY = 0.0;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;
    private boolean floatResult = false;
    private boolean convertToGrayscale = false;

    public GaussianBlur() {
    }

    public int getKernelSizeX() {
        return kernelSizeX;
    }

    public GaussianBlur setKernelSizeX(int kernelSizeX) {
        this.kernelSizeX = nonNegative(kernelSizeX);
        return this;
    }

    public GaussianBlur setKernelSizeX(String kernelSizeX) {
        return setKernelSizeX(intOrDefault(kernelSizeX, 0));
    }

    public int getKernelSizeY() {
        return kernelSizeY;
    }

    public GaussianBlur setKernelSizeY(int kernelSizeY) {
        this.kernelSizeY = nonNegative(kernelSizeY);
        return this;
    }

    public GaussianBlur setKernelSizeY(String kernelSizeY) {
        return setKernelSizeY(intOrDefault(kernelSizeY, 0));
    }

    public double getSigmaX() {
        return sigmaX;
    }

    public GaussianBlur setSigmaX(double sigmaX) {
        this.sigmaX = nonNegative(sigmaX);
        return this;
    }

    public GaussianBlur setSigmaX(String sigmaX) {
        return setSigmaX(doubleOrDefault(sigmaX, 0.0));
    }

    public double getSigmaY() {
        return sigmaY;
    }

    public GaussianBlur setSigmaY(double sigmaY) {
        this.sigmaY = nonNegative(sigmaY);
        return this;
    }

    public GaussianBlur setSigmaY(String sigmaY) {
        return setSigmaY(doubleOrDefault(sigmaY, 0.0));
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public GaussianBlur setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
        return this;
    }

    public boolean isFloatResult() {
        return floatResult;
    }

    public GaussianBlur setFloatResult(boolean floatResult) {
        this.floatResult = floatResult;
        return this;
    }

    public boolean isConvertToGrayscale() {
        return convertToGrayscale;
    }

    public GaussianBlur setConvertToGrayscale(boolean convertToGrayscale) {
        this.convertToGrayscale = convertToGrayscale;
        return this;
    }


    /*Repeat() Mat ==> UMat */
    @Override
    public Mat process(Mat source) {
        final int sizeX, sizeY;
        if (kernelSizeX == 0 && kernelSizeY == 0) {
            sizeX = 0;
            sizeY = 0;
        } else {
            sizeX = (kernelSizeX > 0 ? kernelSizeX : kernelSizeY) | 0x1;
            sizeY = (kernelSizeY > 0 ? kernelSizeY : kernelSizeX) | 0x1;
        }
        if (sizeX == 0 && sizeY == 0 && sigmaX == 0.0 && sigmaY == 0.0) {
            return source;
        }
        if (convertToGrayscale) {
            // - maximally quick solution: 1st converting to grayscale, 2nd converting to float
            OTools.makeMonoIfNot(source);
        }
        if (floatResult) {
            OTools.make32FIfNot(source);
        }
        try (Size size = new Size(sizeX, sizeY)) {
//            System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
//            System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
            opencv_imgproc.GaussianBlur(source, source, size, sigmaX, sigmaY,
                    getBorderType().code());
                    //opencv_core.ALGO_HINT_DEFAULT);
            return source;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    @Override
    public UMat process(UMat source) {
        final int sizeX, sizeY;
        if (kernelSizeX == 0 && kernelSizeY == 0) {
            sizeX = 0;
            sizeY = 0;
        } else {
            sizeX = (kernelSizeX > 0 ? kernelSizeX : kernelSizeY) | 0x1;
            sizeY = (kernelSizeY > 0 ? kernelSizeY : kernelSizeX) | 0x1;
        }
        if (sizeX == 0 && sizeY == 0 && sigmaX == 0.0 && sigmaY == 0.0) {
            return source;
        }
        if (convertToGrayscale) {
            // - maximally quick solution: 1st converting to grayscale, 2nd converting to float
            OTools.makeMonoIfNot(source);
        }
        if (floatResult) {
            OTools.make32FIfNot(source);
        }
        try (Size size = new Size(sizeX, sizeY)) {
//            System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
//            System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
            opencv_imgproc.GaussianBlur(source, source, size, sigmaX, sigmaY,
                    getBorderType().code());
                    // opencv_core.ALGO_HINT_DEFAULT);
            return source;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    public static MultiMatrix2D blur(MultiMatrix2D source, int kernelSizeX, int kernelSizeY, boolean floatResult) {
        return blur(source, kernelSizeX, kernelSizeY, 0.0, 0.0, floatResult, false);
    }

    public static MultiMatrix2D blur(MultiMatrix2D source, int kernelSize, boolean floatResult) {
        return blur(source, kernelSize, kernelSize, 0.0, 0.0, floatResult, false);
    }

    public static MultiMatrix2D blurGrayscale(
            MultiMatrix2D source,
            int kernelSizeX,
            int kernelSizeY,
            boolean floatResult) {
        return blur(source, kernelSizeX, kernelSizeY, 0.0, 0.0, floatResult, true);
    }

    public static MultiMatrix2D blurGrayscale(MultiMatrix2D source, int kernelSize, boolean floatResult) {
        return blur(source, kernelSize, kernelSize, 0.0, 0.0, floatResult, true);
    }

    public static MultiMatrix2D blur(
            MultiMatrix2D source,
            int kernelSizeX,
            int kernelSizeY,
            double sigmaX,
            double sigmaY,
            boolean floatResult,
            boolean convertToGrayscale) {
        try (GaussianBlur gaussianBlur = new GaussianBlur() {
            @Override
            protected boolean loggingEnabled() {
                return false;
            }
        }.setKernelSizeX(kernelSizeX)
                .setKernelSizeY(kernelSizeY)
                .setSigmaX(sigmaX)
                .setSigmaY(sigmaY)
                .setFloatResult(floatResult)
                .setConvertToGrayscale(convertToGrayscale)) {
            if (OTools.isGPUOptimizationEnabled()) {
                final UMat mat = O2SMat.toUMat(source);
                // Note: do not close it! process() method works in place and returns this matrix!
                return O2SMat.toMultiMatrix(gaussianBlur.process(mat));
            } else {
                final Mat mat = O2SMat.toMat(source);
                // Note: do not close it! process() method works in place and returns this matrix!
                return O2SMat.toMultiMatrix(gaussianBlur.process(mat));
            }
        }
    }

    public static void blurIמPlace(SMat sourceAndResult, int kernelSizeX, int kernelSizeY, boolean floatResult) {
        blurIמPlace(
                sourceAndResult, kernelSizeX, kernelSizeY, 0.0, 0.0,
                floatResult, false);
    }

    public static void blurIמPlace(SMat sourceAndResult, int kernelSize, boolean floatResult) {
        blurIמPlace(
                sourceAndResult, kernelSize, kernelSize, 0.0, 0.0,
                floatResult, false);
    }

    public static void blurGrayscaleInPlace(
            SMat sourceAndResult,
            int kernelSizeX,
            int kernelSizeY, boolean floatResult) {
        blurIמPlace(
                sourceAndResult, kernelSizeX, kernelSizeY, 0.0, 0.0,
                floatResult, true);
    }

    public static void blurGrayscaleInPlace(SMat sourceAndResult, int kernelSize, boolean floatResult) {
        blurIמPlace(
                sourceAndResult, kernelSize, kernelSize, 0.0, 0.0,
                floatResult, true);
    }

    /**
     * <b>IMPORTANT!</b> If you call this method for some {@link #getInputMat(String)} matrix,
     * and if your executor implements {@link ReadOnlyExecutionInput}
     * (like <b>any</b> subclass of {@link MultiMatrix2DFilter}),
     * you usually <b>must</b> override {@link ReadOnlyExecutionInput#isReadOnly()} method
     * or perform manual cloning the blurred matrix.
     */
    public static void blurIמPlace(
            SMat sourceAndResult,
            int kernelSizeX,
            int kernelSizeY,
            double sigmaX,
            double sigmaY,
            boolean floatResult,
            boolean convertToGrayscale) {
        try (GaussianBlur gaussianBlur = new GaussianBlur() {
            @Override
            protected boolean loggingEnabled() {
                return false;
            }
        }.setKernelSizeX(kernelSizeX)
                .setKernelSizeY(kernelSizeY)
                .setSigmaX(sigmaX)
                .setSigmaY(sigmaY)
                .setFloatResult(floatResult)
                .setConvertToGrayscale(convertToGrayscale)) {
            // Note: though we use try-with-resources, really we don't fill output ports
            // of GaussianBlur executor (because we directly call "process(UMat/Mat)" method),
            // so, there is no risk that closing this object will dispose the returned SMat.
            if (OTools.isGPUOptimizationEnabled()) {
                final UMat mat = O2SMat.toUMat(sourceAndResult);
                // Note: do not close it! process() method works in place and returns this matrix!
                O2SMat.setTo(sourceAndResult, gaussianBlur.process(mat));
                // Note: this call renews information about the mat in SMat object, like the depth.
            } else {
                final Mat mat = O2SMat.toMat(sourceAndResult);
                // Note: do not close it! process() method works in place and returns this matrix!
                O2SMat.setTo(sourceAndResult, gaussianBlur.process(mat));
            }
        }
    }
}
