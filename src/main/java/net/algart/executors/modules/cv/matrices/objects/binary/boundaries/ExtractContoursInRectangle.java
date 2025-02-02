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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.IntArray;
import net.algart.contours.ContourHeader;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.IRectangularArea;

public final class ExtractContoursInRectangle extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_CONTOURS = "contours";
    public static final String OUTPUT_OTHER_CONTOURS = "other_contours";
    public static final String OUTPUT_CONTAINING_ALL_RECTANGLE = "containing_all_rectangle";
    public static final String OUTPUT_NUMBER_OF_SOURCE_CONTOURS = "number_of_source_contours";
    public static final String OUTPUT_NUMBER_OF_RESULT_CONTOURS = "number_of_result_contours";

    public enum SelectionMode {
        FULLY_INSIDE() {
            @Override
            boolean accept(ContourHeader header, IntArray contour, IRectangularArea checkedRectangle) {
                return checkedRectangle.contains(header.containingRectangle());
            }
        },
        CONTAINING_RECTANGLE_INTERSECTS() {
            @Override
            boolean accept(ContourHeader header, IntArray contour, IRectangularArea checkedRectangle) {
                return checkedRectangle.intersects(header.containingRectangle());
            }
        };

        abstract boolean accept(ContourHeader header, IntArray contour, IRectangularArea checkedRectangle);
    }

    private boolean doAction = true;
    private SelectionMode selectionMode = SelectionMode.FULLY_INSIDE;
    private long left = 0;
    private long top = 0;
    private long right = 0;
    private long bottom = 0;
    private long width = 0;
    private long height = 0;

    public ExtractContoursInRectangle() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputNumbers(OUTPUT_OTHER_CONTOURS);
        addOutputNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE);
        addOutputScalar(OUTPUT_NUMBER_OF_SOURCE_CONTOURS);
        addOutputScalar(OUTPUT_NUMBER_OF_RESULT_CONTOURS);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public ExtractContoursInRectangle setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public ExtractContoursInRectangle setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = nonNull(selectionMode);
        return this;
    }

    public long getLeft() {
        return left;
    }

    public ExtractContoursInRectangle setLeft(long left) {
        this.left = left;
        return this;
    }

    public long getTop() {
        return top;
    }

    public ExtractContoursInRectangle setTop(long top) {
        this.top = top;
        return this;
    }

    public long getRight() {
        return right;
    }

    public ExtractContoursInRectangle setRight(long right) {
        this.right = right;
        return this;
    }

    public long getBottom() {
        return bottom;
    }

    public ExtractContoursInRectangle setBottom(long bottom) {
        this.bottom = bottom;
        return this;
    }

    public long getWidth() {
        return width;
    }

    public ExtractContoursInRectangle setWidth(long width) {
        this.width = nonNegative(width);
        return this;
    }

    public long getHeight() {
        return height;
    }

    public ExtractContoursInRectangle setHeight(long height) {
        this.height = nonNegative(height);
        return this;
    }

    @Override
    public boolean isReadOnly() {
        // IMPORTANT! If !doAction, we just returns a reference to the source SNumbers
        return doAction;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        if (!doAction) {
            return source;
        }
        final Contours contours = Contours.deserialize(source.toIntArray());
        final Contours result = Contours.newInstance();
        final Contours otherContours = Contours.newInstance();
        long x1 = left, y1 = top, x2 = right, y2 = bottom;
        if (width > 0) {
            x2 = Math.addExact(x1, width);
        }
        if (height > 0) {
            y2 = Math.addExact(y1, height);
        }
        final IRectangularArea checkedRectangle = IRectangularArea.valueOf(x1, y1, x2, y2);
        final boolean resultNecessary = isOutputNecessary(OUTPUT_CONTOURS);
        final boolean otherContoursNecessary = isOutputNecessary(OUTPUT_OTHER_CONTOURS);
        final ContourHeader header = new ContourHeader();
        int containingMinX = Integer.MAX_VALUE;
        int containingMaxX = Integer.MIN_VALUE;
        int containingMinY = Integer.MAX_VALUE;
        int containingMaxY = Integer.MIN_VALUE;
        final int n = contours.numberOfContours();
        for (int k = 0; k < n; k++) {
            contours.getHeader(header, k);
            if (header.minX() < containingMinX) {
                containingMinX = header.minX();
            }
            if (header.maxX() > containingMaxX) {
                containingMaxX = header.maxX();
            }
            if (header.minY() < containingMinY) {
                containingMinY = header.minY();
            }
            if (header.maxY() > containingMaxY) {
                containingMaxY = header.maxY();
            }
            final IntArray contour = contours.getContour(k);
            if (selectionMode.accept(header, contour, checkedRectangle)) {
                if (resultNecessary) {
                    result.addContour(header, contour);
                }
            } else {
                if (otherContoursNecessary) {
                    otherContours.addContour(header, contour);
                }
            }
        }
        if (n > 0) {
            getNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE).setTo(
                    IRectangularArea.valueOf(containingMinX, containingMinY, containingMaxX, containingMaxY));
        }
        getScalar(OUTPUT_NUMBER_OF_SOURCE_CONTOURS).setTo(n);
        getScalar(OUTPUT_NUMBER_OF_RESULT_CONTOURS).setTo(result.numberOfContours());
        if (otherContoursNecessary) {
            getNumbers(OUTPUT_OTHER_CONTOURS).setTo(otherContours);
        }
        return resultNecessary ? SNumbers.of(result) : null;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }
}
