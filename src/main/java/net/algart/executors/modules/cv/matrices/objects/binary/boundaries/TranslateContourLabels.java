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
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

import java.util.Locale;

public final class TranslateContourLabels extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_TABLE = "table";
    public static final String OUTPUT_CONTOURS = "contours";

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public TranslateContourLabels() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        addInputNumbers(INPUT_TABLE);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public TranslateContourLabels setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final Contours contours = Contours.deserialize(source.toIntArray());
        final int[] table = getInputNumbers(INPUT_TABLE).requireBlockLengthOne("table").toIntArray();
        long t1 = debugTime();
        final int start = indexingBase.start;
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            int label = contours.getObjectLabel(k) - start;
            if (label >= 0 && label < table.length) {
                label = table[label];
                contours.setObjectLabel(k, label);
            }
        }
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "%d contour labels translated in %.3f ms",
                contours.numberOfContours(), (t2 - t1) * 1e-6));
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(contours.numberOfContours());
        return SNumbers.valueOf(contours);
    }
}
