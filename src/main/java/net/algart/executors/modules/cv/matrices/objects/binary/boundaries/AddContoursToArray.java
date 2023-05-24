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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.contours.Contours;
import net.algart.contours.ContourHeader;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.List;
import java.util.stream.Stream;

public final class AddContoursToArray extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_ADDED = "added";
    public static final String OUTPUT_CONTOURS = "contours";

    private int objectLabel = 1;
    private boolean internalContour = false;
    private String points = "";

    public AddContoursToArray() {
        super(INPUT_CONTOURS, INPUT_ADDED);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public int getObjectLabel() {
        return objectLabel;
    }

    public AddContoursToArray setObjectLabel(int objectLabel) {
        this.objectLabel = objectLabel;
        return this;
    }

    public boolean isInternalContour() {
        return internalContour;
    }

    public AddContoursToArray setInternalContour(boolean internalContour) {
        this.internalContour = internalContour;
        return this;
    }

    public String getPoints() {
        return points;
    }

    public AddContoursToArray setPoints(String points) {
        this.points = nonNull(points);
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        final SNumbers source = sources.get(0);
        final SNumbers added = sources.get(1);
        final Contours contours = source != null ?
                Contours.deserialize(source.toIntArray()) :
                Contours.newInstance();
        if (added != null) {
            final int[] contourData = added.toIntArray();
            if (Contours.isSerializedContours(contourData)) {
                contours.addContours(Contours.deserialize(contourData));
            } else {
                contours.addContour(new ContourHeader(objectLabel, internalContour), contourData);
            }
        } else {
            for (int[] points : parseContours(this.points)) {
                contours.addContour(new ContourHeader(objectLabel, internalContour), points);
            }
        }
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(contours.numberOfContours());
        return SNumbers.valueOf(contours);
    }

    @Override
    protected boolean allowAllUninitializedInputs() {
        return true;
    }

    @Override
    protected boolean numberOfBlocksEqualityRequired() {
        return false;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

    private static int[][] parseContours(String scalar) {
        scalar = scalar.trim();
        if (scalar.isEmpty()) {
            return new int[0][];
        }
        return Stream.of(scalar.split(";")).map(AddContoursToArray::parseSingleContour).toArray(int[][]::new);
    }

    private static int[] parseSingleContour(String scalar) {
        return Stream.of(scalar.trim().split("[,\\s]+"))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
