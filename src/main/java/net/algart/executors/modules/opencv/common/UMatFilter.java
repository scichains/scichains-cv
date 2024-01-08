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

package net.algart.executors.modules.opencv.common;

import net.algart.executors.modules.opencv.util.O2SMat;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public abstract class UMatFilter extends MatFilter {
    protected UMatFilter() {
    }

    @Override
    public void process() {
        if (!useGPU()) {
            super.process();
            return;
        }
        UMat source = O2SMat.toUMat(
                getInputMat(allowUninitializedInput()),
                allowInputPackedBits());
        final UMat result = processWithCompression(source);
        setOutputTo(result);
    }

    public UMat processWithCompression(UMat source) {
        this.sourceDimX = source != null ? source.cols() : -1;
        this.sourceDimY = source != null ? source.rows() : -1;
        final UMat compressedSource = compressOriginal(source);
        setStartProcessingTimeStamp();
        final UMat compressedResult = process(compressedSource);
        setEndProcessingTimeStamp();
        final UMat result = stretchToOriginal(compressedResult);
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

    public void setOutputTo(UMat result) {
        if (packOutputBits()) {
            getMat().setTo(O2SMat.toBinaryMatrix(result));
        } else {
            O2SMat.setTo(getMat(), result);
        }
    }

    public abstract UMat process(UMat source);

    public final UMat compressOriginal(UMat mat) {
        if (mat == null || optimizingScale <= 1.0) {
            return mat;
        }
        UMat result = new UMat();
        double fxy = 1.0 / optimizingScale;
        try (Size size = new Size()) {
            opencv_imgproc.resize(mat, result, size, fxy, fxy, compressionInterpolation.code());
        }
        return result;
    }

    public final UMat stretchToOriginal(UMat mat) {
        if (mat == null || optimizingScale <= 1.0 || (sourceDimX == mat.cols() && sourceDimY == mat.rows())) {
            return mat;
        }
        UMat result = new UMat();
        try (final Size size = new Size(sourceDimX, sourceDimY)) {
            opencv_imgproc.resize(mat, result, size, 0, 0, stretchingInterpolation.code());
        }
        return result;
    }

}
