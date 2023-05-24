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

package net.algart.executors.modules.opencv.matrices.geometry;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.opencv_core.*;

public final class SubMatrix extends UMatFilter implements ReadOnlyExecutionInput {
    private int left = 0;
    private int top = 0;
    private int right = 0;
    private int bottom = 0;
    // right/bottom are used if width/height <=0; negative right/bottom are added to dimX-1/dimY-1
    private int width = 0;
    private int height = 0;
    private String outsideColor = "#FFFFFF";

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getOutsideColor() {
        return outsideColor;
    }

    public void setOutsideColor(String outsideColor) {
        this.outsideColor = nonNull(outsideColor);
    }

    public Mat process(Mat source) {
        final int right = this.right >= 0 ? this.right : source.cols() - 1 + this.right;
        final int bottom = this.bottom >= 0 ? this.bottom : source.rows() - 1 + this.bottom;
        final int width = this.width > 0 ? this.width : right - left + 1;
        final int height = this.height > 0 ? this.height : bottom - top + 1;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Empty or negative-size result matrix " + width + "x" + height);
        }
        Mat result = new Mat(height, width, source.type());
        final int xFrom = Math.max(this.left, 0);
        final int yFrom = Math.max(this.top, 0);
        final int xTo = Math.min(this.left + width, source.cols());
        final int yTo = Math.min(this.top + height, source.rows());
        final int w = xTo - xFrom;
        final int h = yTo - yFrom;
        if (w > 0 && h > 0) {
            try (Scalar scalar =  OTools.scalarBGRA(outsideColor, OTools.maxPossibleValue(source));
                 Rect sourceRect = new Rect(xFrom, yFrom, w, h);
                 Rect resultRect = new Rect(xFrom - this.left, yFrom - this.top, w, h)
            ) {
                if (w < width || h < height) {
                    result.put(scalar);
                }
                try (Mat s = source.apply(sourceRect);
                     Mat r = result.apply(resultRect)) {
                    s.copyTo(r);
                }
            }
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        final int right = this.right >= 0 ? this.right : source.cols() - 1 + this.right;
        final int bottom = this.bottom >= 0 ? this.bottom : source.rows() - 1 + this.bottom;
        final int width = this.width > 0 ? this.width : right - left + 1;
        final int height = this.height > 0 ? this.height : bottom - top + 1;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Empty or negative-size result matrix " + width + "x" + height);
        }
        UMat result = new UMat(height, width, source.type());
        final int xFrom = Math.max(this.left, 0);
        final int yFrom = Math.max(this.top, 0);
        final int xTo = Math.min(this.left + width, source.cols());
        final int yTo = Math.min(this.top + height, source.rows());
        final int w = xTo - xFrom;
        final int h = yTo - yFrom;
        if (w > 0 && h > 0) {
            try (Scalar scalar =  OTools.scalarBGRA(outsideColor, OTools.maxPossibleValue(source));
                 Rect sourceRect = new Rect(xFrom, yFrom, w, h);
                 Rect resultRect = new Rect(xFrom - this.left, yFrom - this.top, w, h)
            ) {
//                System.out.printf("result: %s;%nrect: %d,%d..%d,%d%n", OTools.toString(result),
//                        resultRect.x(), resultRect.y(),
//                        resultRect.x() + resultRect.width(), resultRect.y() + resultRect.height());
                if (w < width || h < height) {
                    result.put(scalar);
                }
                try (UMat s = source.apply(sourceRect);
                     UMat r = result.apply(resultRect)) {
                    s.copyTo(r);
                }
            }
        }
        return result;
    }
}
