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

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.executors.modules.opencv.util.enums.OInterpolation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class BilateralFilter extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    private int diameterOfNeighborhood = 5;
    private double sigmaSpace = 75;
    private double sigmaColor = 0.3;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public BilateralFilter() {
        setStretchingInterpolation(OInterpolation.INTER_LINEAR);
    }

    public int getDiameterOfNeighborhood() {
        return diameterOfNeighborhood;
    }

    public void setDiameterOfNeighborhood(int diameterOfNeighborhood) {
        this.diameterOfNeighborhood = nonNegative(diameterOfNeighborhood, "diameter of neighborhood");
    }

    public double getSigmaSpace() {
        return sigmaSpace;
    }

    public void setSigmaSpace(double sigmaSpace) {
        this.sigmaSpace = nonNegative(sigmaSpace);
    }

    public double getSigmaColor() {
        return sigmaColor;
    }

    public void setSigmaColor(double sigmaColor) {
        this.sigmaColor = nonNegative(sigmaColor);
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
    }

    @Override
    public void process(Mat result, Mat source) {
        logDebug(() -> "Bilateral filter: d = " + diameterOfNeighborhood
            + ", sigmaSpace = " + sigmaSpace+ ", sigmaColor = " + sigmaColor
            + ", borderType = " + borderType + " (source: " + source + ")");
        opencv_imgproc.bilateralFilter(
            source,
            result,
            diameterOfNeighborhood,
            sigmaColor * OTools.maxPossibleValue(source),
            sigmaSpace,
            borderType.code());
    }

    @Override
    public void process(UMat result, UMat source) {
        logDebug(() -> "Bilateral filter (GPU): d = " + diameterOfNeighborhood
                + ", sigmaSpace = " + sigmaSpace+ ", sigmaColor = " + sigmaColor
                + ", borderType = " + borderType + " (source: " + source + ")");
        opencv_imgproc.bilateralFilter(
                source,
                result,
                diameterOfNeighborhood,
                sigmaColor * OTools.maxPossibleValue(source),
                sigmaSpace,
                borderType.code());
    }
}
