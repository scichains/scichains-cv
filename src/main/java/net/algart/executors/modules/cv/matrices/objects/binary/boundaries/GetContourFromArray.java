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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.IntArray;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

public final class GetContourFromArray extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_POINTS = "points";
    public static final String OUTPUT_OBJECT_LABEL = "object_label";
    public static final String OUTPUT_INTERNAL_BOUNDARY = "internal_boundary";
    public static final String OUTPUT_FRAME_ID = "frame_id";

    private int contourIndex = 0;
    private boolean unpackContour = true;
    private boolean unpackDiagonals = true;

    public GetContourFromArray() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_POINTS);
        addOutputScalar(OUTPUT_OBJECT_LABEL);
        addOutputScalar(OUTPUT_INTERNAL_BOUNDARY);
        addOutputScalar(OUTPUT_FRAME_ID);
    }

    public int getContourIndex() {
        return contourIndex;
    }

    public GetContourFromArray setContourIndex(int contourIndex) {
        this.contourIndex = nonNegative(contourIndex);
        return this;
    }

    public boolean isUnpackContour() {
        return unpackContour;
    }

    public GetContourFromArray setUnpackContour(boolean unpackContour) {
        this.unpackContour = unpackContour;
        return this;
    }

    public boolean isUnpackDiagonals() {
        return unpackDiagonals;
    }

    public GetContourFromArray setUnpackDiagonals(boolean unpackDiagonals) {
        this.unpackDiagonals = unpackDiagonals;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final Contours contours = Contours.deserialize(source.toIntArray());
        final IntArray result = unpackContour ?
                contours.unpackContour(contourIndex, unpackDiagonals) :
                contours.getContour(contourIndex);
        getScalar(OUTPUT_OBJECT_LABEL).setTo(contours.getObjectLabel(contourIndex));
        getScalar(OUTPUT_INTERNAL_BOUNDARY).setTo(contours.isInternalContour(contourIndex) ? 1 : 0);
        final Integer frameId = contours.getFrameIdOrNull(contourIndex);
        if (frameId != null) {
            getScalar(OUTPUT_FRAME_ID).setTo(frameId);
        }
        return new SNumbers().setTo(result, 2);
    }
}
