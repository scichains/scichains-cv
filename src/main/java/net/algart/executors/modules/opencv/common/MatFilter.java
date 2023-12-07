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

package net.algart.executors.modules.opencv.common;

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.enums.OInterpolation;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

public abstract class MatFilter extends OpenCVExecutor {
    double optimizingScale = 1;
    OInterpolation compressionInterpolation = OInterpolation.INTER_AREA;
    OInterpolation stretchingInterpolation = OInterpolation.INTER_NEAREST;

    int sourceDimX;
    int sourceDimY;

    protected MatFilter() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public final double getOptimizingScale() {
        return optimizingScale;
    }

    public final MatFilter setOptimizingScale(double optimizingScale) {
        if (optimizingScale < 1.0) {
            throw new IllegalArgumentException("Scale for faster processing " + optimizingScale + " must be >= 1.0");
        }
        this.optimizingScale = optimizingScale;
        return this;
    }

    public OInterpolation getCompressionInterpolation() {
        return compressionInterpolation;
    }

    public MatFilter setCompressionInterpolation(OInterpolation compressionInterpolation) {
        this.compressionInterpolation = nonNull(compressionInterpolation);
        return this;
    }

    public OInterpolation getStretchingInterpolation() {
        return stretchingInterpolation;
    }

    public MatFilter setStretchingInterpolation(OInterpolation stretchingInterpolation) {
        this.stretchingInterpolation = nonNull(stretchingInterpolation);
        return this;
    }

    @Override
    public void process() {
        Mat source = O2SMat.toMat(
                getInputMat(allowUninitializedInput()),
                allowInputPackedBits());
        setOutputTo(processWithCompression(source));
    }

    public Mat processWithCompression(Mat source) {
        this.sourceDimX = source != null ? source.cols() : -1;
        this.sourceDimY = source != null ? source.rows() : -1;
        final Mat compressedSource = compressOriginal(source);
        setStartProcessingTimeStamp();
        Mat compressedResult = process(compressedSource);
        setEndProcessingTimeStamp();
        final Mat result = stretchToOriginal(compressedResult);
        if (result != compressedResult) {
            // compressedResult != null, because in other case it==result
            compressedResult.close();
            if (compressedSource != null) {
                compressedSource.close();
                // - maybe twice call, if process works in place
            }
        }
        return result;
    }

    public void setOutputTo(Mat result) {
        if (packOutputBits()) {
            getMat().setTo(O2SMat.toBinaryMatrix(result));
        } else {
            O2SMat.setTo(getMat(), result);
        }
    }

    public abstract Mat process(Mat source);

    public final Mat compressOriginal(Mat mat) {
        if (mat == null || optimizingScale <= 1.0) {
            return mat;
        }
        Mat result = new Mat();
        double fxy = 1.0 / optimizingScale;
        try (Size size = new Size()) {
            opencv_imgproc.resize(mat, result, size, fxy, fxy, compressionInterpolation.code());
        }
        return result;
    }

    public final Mat stretchToOriginal(Mat mat) {
        if (mat == null || optimizingScale <= 1.0 || (sourceDimX == mat.cols() && sourceDimY == mat.rows())) {
            return mat;
        }
        Mat result = new Mat();
        try (final Size size = new Size(sourceDimX, sourceDimY)) {
            opencv_imgproc.resize(mat, result, size, 0, 0, stretchingInterpolation.code());
        }
        return result;
    }

    // May be overridden
    protected boolean allowUninitializedInput() {
        return false;
    }

    // May be overridden
    protected boolean allowInputPackedBits() {
        return false;
    }

    // May be overridden
    protected boolean packOutputBits() {
        return false;
    }
}
