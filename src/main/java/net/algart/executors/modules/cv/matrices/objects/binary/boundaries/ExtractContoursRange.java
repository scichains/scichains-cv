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

import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

public final class ExtractContoursRange extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_CONTOURS = "contours";
    public static final String OUTPUT_OTHER_CONTOURS = "other_contours";

    private int firstIndex = 0;
    private int numberOfContours = 0;

    public ExtractContoursRange() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputNumbers(OUTPUT_OTHER_CONTOURS);
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public ExtractContoursRange setFirstIndex(int firstIndex) {
        this.firstIndex = nonNegative(firstIndex);
        return this;
    }

    public int getNumberOfContours() {
        return numberOfContours;
    }

    public ExtractContoursRange setNumberOfContours(int numberOfContours) {
        this.numberOfContours = nonNegative(numberOfContours);
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final Contours contours = Contours.deserialize(source.toIntArray());
        final int length = this.numberOfContours != 0 ?
                this.numberOfContours :
                contours.numberOfContours() - firstIndex;
        final int toIndex = Math.addExact(firstIndex, length);
        final Contours result = contours.contoursRange(firstIndex, toIndex);
        if (isOutputNecessary(OUTPUT_OTHER_CONTOURS)) {
            contours.removeContoursRange(firstIndex, toIndex);
            getNumbers(OUTPUT_OTHER_CONTOURS).setTo(contours);
        }
        return SNumbers.of(result);
    }
}
