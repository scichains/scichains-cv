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

package net.algart.executors.modules.opencv.matrices.geometry;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.enums.OInterpolation;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class Resize extends UMatFilter implements ReadOnlyExecutionInput {
    public static final String OUTPUT_DIM_X = "dim_x";
    public static final String OUTPUT_DIM_Y = "dim_y";

    private double dimX = 0.0;
    private double dimY = 0.0;
    private boolean percents = false;
    private OInterpolation interpolation = OInterpolation.INTER_LINEAR;

    public Resize() {
        addOutputScalar(OUTPUT_DIM_X);
        addOutputScalar(OUTPUT_DIM_Y);
    }

    public double getDimX() {
        return dimX;
    }

    public void setDimX(double dimX) {
        this.dimX = nonNegative(dimX);
    }

    public double getDimY() {
        return dimY;
    }

    public void setDimY(double dimY) {
        this.dimY = nonNegative(dimY);
    }

    public boolean isPercents() {
        return percents;
    }

    public void setPercents(boolean percents) {
        this.percents = percents;
    }

    public OInterpolation getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(OInterpolation interpolation) {
        this.interpolation = nonNull(interpolation);
    }

    @Override
    public Mat process(Mat source) {
        int newDimX = source.cols();
        int newDimY = source.rows();
        if (dimX != 0.0 || dimY != 0.0) {
            if (dimX != 0.0) {
                newDimX = percents ? Math.round((float) (dimX / 100.0 * source.cols())) : Math.round((float) dimX);
            }
            if (dimY != 0.0) {
                newDimY = percents ? Math.round((float) (dimY / 100.0 * source.rows())) : Math.round((float) dimY);
            }
            if (dimX == 0.0) {
                newDimX = Math.round(newDimX * (float) newDimY / (float) source.rows());
            }
            if (dimY == 0.0) {
                newDimY = Math.round(newDimY * (float) newDimX / (float) source.cols());
            }
        }
        if (LOGGABLE_DEBUG) {
            logDebug("Resizing to " + newDimX + "x" + newDimY + " (source: " + source + ")");
        }
        try (Size size = new Size(newDimX, newDimY)) {
            final Mat result = new Mat(size, source.type());
            opencv_imgproc.resize(source, result, size);
            return result;
        }
    }

    @Override
    public UMat process(UMat source) {
        int newDimX = source.cols();
        int newDimY = source.rows();
        if (dimX != 0.0 || dimY != 0.0) {
            if (dimX != 0.0) {
                newDimX = percents ? Math.round((float) (dimX / 100.0 * source.cols())) : Math.round((float) dimX);
            }
            if (dimY != 0.0) {
                newDimY = percents ? Math.round((float) (dimY / 100.0 * source.rows())) : Math.round((float) dimY);
            }
            if (dimX == 0.0) {
                newDimX = Math.round(newDimX * (float) newDimY / (float) source.rows());
            }
            if (dimY == 0.0) {
                newDimY = Math.round(newDimY * (float) newDimX / (float) source.cols());
            }
        }
        if (LOGGABLE_DEBUG) {
            logDebug("Resizing (GPU) to " + newDimX + "x" + newDimY + " (source: " + source + ")");
        }
        try (Size size = new Size(newDimX, newDimY)) {
            final UMat result = new UMat(size, source.type());
            opencv_imgproc.resize(source, result, size);
            getScalar(OUTPUT_DIM_X).setTo(newDimX);
            getScalar(OUTPUT_DIM_Y).setTo(newDimY);
            return result;
        }
    }
}
