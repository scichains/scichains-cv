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

import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

import java.util.Locale;

public final class TransformContours extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_PACKED_CONTOURS = "contours";

    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private double shiftX = 0.0;
    private double shiftY = 0.0;
    private boolean removeDegeneratedContours = false;

    public TransformContours() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_PACKED_CONTOURS);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public double getScaleX() {
        return scaleX;
    }

    public TransformContours setScaleX(double scaleX) {
        this.scaleX = scaleX;
        return this;
    }

    public double getScaleY() {
        return scaleY;
    }

    public TransformContours setScaleY(double scaleY) {
        this.scaleY = scaleY;
        return this;
    }

    public double getShiftX() {
        return shiftX;
    }

    public TransformContours setShiftX(double shiftX) {
        this.shiftX = shiftX;
        return this;
    }

    public double getShiftY() {
        return shiftY;
    }

    public TransformContours setShiftY(double shiftY) {
        this.shiftY = shiftY;
        return this;
    }

    public boolean isRemoveDegeneratedContours() {
        return removeDegeneratedContours;
    }

    public TransformContours setRemoveDegeneratedContours(boolean removeDegeneratedContours) {
        this.removeDegeneratedContours = removeDegeneratedContours;
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final Contours contours = Contours.deserialize(source.toIntArray());
        long t1 = debugTime();
        final Contours result = contours.transformContours(
                scaleX, scaleY, shiftX, shiftY,
                removeDegeneratedContours);
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "%d contours transformed to %d contours in %.3f ms, %.5f mcs/contour",
                contours.numberOfContours(), result.numberOfContours(),
                (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / (double) contours.numberOfContours()));
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(result.numberOfContours());
        return SNumbers.of(result);
    }
}
