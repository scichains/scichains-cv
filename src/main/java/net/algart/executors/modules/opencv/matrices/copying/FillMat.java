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

package net.algart.executors.modules.opencv.matrices.copying;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.UMat;

public final class FillMat extends UMatFilter {
    private String color = "#FFFFFF";
    private double grayscaleValue = 0.0; // - used if color is an empty string
    private boolean rawGrayscaleValue = false;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = nonNull(color);
    }

    public double getGrayscaleValue() {
        return grayscaleValue;
    }

    public void setGrayscaleValue(double grayscaleValue) {
        this.grayscaleValue = grayscaleValue;
    }

    public boolean isRawGrayscaleValue() {
        return rawGrayscaleValue;
    }

    public void setRawGrayscaleValue(boolean rawGrayscaleValue) {
        this.rawGrayscaleValue = rawGrayscaleValue;
    }

    @Override
    public UMat process(UMat source) {
        final double maxPossibleValue = OTools.maxPossibleValue(source);
        try (Scalar scalar = color.trim().isEmpty() ?
                OTools.scalarBGR(rawGrayscaleValue ? grayscaleValue : grayscaleValue * maxPossibleValue) :
                OTools.scalarBGRA(color, maxPossibleValue)) {
            source.put(scalar);
            return source;
        }
    }

    @Override
    public Mat process(Mat source) {
        final double maxPossibleValue = OTools.maxPossibleValue(source);
        try (Scalar scalar = color.trim().isEmpty() ?
                OTools.scalarBGR(rawGrayscaleValue ? grayscaleValue : grayscaleValue * maxPossibleValue) :
                OTools.scalarBGRA(color, maxPossibleValue)) {
            source.put(scalar);
            return source;
        }
    }

}
