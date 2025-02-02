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

import net.algart.contours.ContourHeader;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.IRectangularArea;

import java.util.Arrays;

public final class ReviveContourContainingRectangles extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_CONTOURS = "contours";
    public static final String OUTPUT_CHANGED = "changed";
    public static final String OUTPUT_WERE_INCORRECT = "were_incorrect";

    private boolean throwExceptionIfChanged = false;

    public ReviveContourContainingRectangles() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputScalar(OUTPUT_CHANGED);
        addOutputScalar(OUTPUT_WERE_INCORRECT);
    }

    public boolean isThrowExceptionIfChanged() {
        return throwExceptionIfChanged;
    }

    public ReviveContourContainingRectangles setThrowExceptionIfChanged(boolean throwExceptionIfChanged) {
        this.throwExceptionIfChanged = throwExceptionIfChanged;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final int[] serializedContours = source.toIntArray();
        final Contours contours = Contours.deserialize(serializedContours);
        final Contours result = Contours.newInstance();
        final ContourHeader header = new ContourHeader();
        final ContourHeader correctedHeader = new ContourHeader();
        boolean someRectangleWasIncorrect = false;
        boolean someRectangleWasChanged = false;
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            contours.getHeader(header, k);
            final IRectangularArea previousRectangle = header.containingRectangle();
            header.removeContainingRectangle();
            result.addContour(header, contours.getContour(k));
            final IRectangularArea correctedRectangle = result.getHeader(correctedHeader, k).containingRectangle();
            if (!previousRectangle.equals(correctedRectangle)) {
                someRectangleWasChanged = true;
                if (!previousRectangle.contains(correctedRectangle)) {
                    someRectangleWasIncorrect = true;
                }
            }
        }
        final SNumbers serializedResult = SNumbers.of(result);
        if (!someRectangleWasChanged
                && !Arrays.equals(serializedContours, (int[]) serializedResult.arrayReference())) {
            throw new AssertionError("Rectangles were not changed, but serialized form is another!");
        }
        getScalar(OUTPUT_CHANGED).setTo(someRectangleWasChanged);
        getScalar(OUTPUT_WERE_INCORRECT).setTo(someRectangleWasIncorrect);
        if (throwExceptionIfChanged && someRectangleWasChanged) {
            throw new IllegalArgumentException("Some containined rectangles were changed; "
                    + "probably the contour array was damaged");
        }
        return serializedResult;
    }
}
