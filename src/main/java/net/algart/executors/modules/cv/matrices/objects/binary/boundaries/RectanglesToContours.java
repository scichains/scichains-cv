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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.Arrays;
import net.algart.contours.ContourHeader;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.RectangularArea;

public final class RectanglesToContours extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_RECTANGLES = "rectangles";
    public static final String INPUT_OBJECT_LABEL = ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL;
    public static final String OUTPUT_UNPACKED_CONTOURS = "contours";

    private int objectLabel = 1;
    private boolean internalContour = false;

    public RectanglesToContours() {
        setDefaultInputNumbers(INPUT_RECTANGLES);
        addInputNumbers(INPUT_OBJECT_LABEL);
        setDefaultOutputNumbers(OUTPUT_UNPACKED_CONTOURS);
    }

    public int getObjectLabel() {
        return objectLabel;
    }

    public RectanglesToContours setObjectLabel(int objectLabel) {
        this.objectLabel = objectLabel;
        return this;
    }

    public boolean isInternalContour() {
        return internalContour;
    }

    public RectanglesToContours setInternalContour(boolean internalContour) {
        this.internalContour = internalContour;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        source.requireBlockLength(4, "rectangles");
        final double[] rectangles = source.toDoubleArray();
        final int[] labels = getInputNumbers(INPUT_OBJECT_LABEL, true).toIntArray();
        final Contours contours = Contours.newInstance();
        final ContourHeader header = new ContourHeader().setInternalContour(internalContour);
        final int[] rectangularContour = new int[8];
        final int[] order = internalContour ? new int[]{6, 4, 2, 0} : new int[]{0, 2, 4, 6};
        for (int k = 0, disp = 0; disp < rectangles.length; k++, disp += 4) {
            final RectangularArea r = BoundaryParameter.getRectangle(rectangles, disp);
            final int minIntegerX = Arrays.round32(StrictMath.ceil(r.minX()));
            final int minIntegerY = Arrays.round32(StrictMath.ceil(r.minY()));
            final int maxIntegerX = Arrays.round32(StrictMath.floor(r.maxX()));
            final int maxIntegerY = Arrays.round32(StrictMath.floor(r.maxY()));
            if (minIntegerX <= maxIntegerX && minIntegerY <= maxIntegerY) {
                // - in other case, there are no any pixel centers inside this rectangle
                rectangularContour[order[0]] = minIntegerX;
                rectangularContour[order[0] + 1] = minIntegerY;
                rectangularContour[order[1]] = inc(maxIntegerX);
                rectangularContour[order[1] + 1] = minIntegerY;
                rectangularContour[order[2]] = inc(maxIntegerX);
                rectangularContour[order[2] + 1] = inc(maxIntegerY);
                rectangularContour[order[3]] = minIntegerX;
                rectangularContour[order[3] + 1] = inc(maxIntegerY);
                header.setObjectLabel(labels != null && k < labels.length ? labels[k] : objectLabel);
                contours.addContour(header, rectangularContour);
            }
        }
        return SNumbers.valueOf(contours);
    }

    private static int inc(int i) {
        return i == Integer.MAX_VALUE ? Integer.MIN_VALUE : i + 1;
    }
}
