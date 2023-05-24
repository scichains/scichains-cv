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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToNumbers;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GetLabelledObjects extends MultiMatrixToNumbers {
    public static final String INPUT_LABELS = ValuesAtLabelledObjects.INPUT_LABELS;
    public static final String OUTPUT_PAINT_LABELLED = ValuesAtLabelledObjects.OUTPUT_PAINT_LABELLED;

    private boolean rawValues = true;
    private boolean paintLabelledOnSource = false;
    private boolean visiblePaintLabelled = false;

    public GetLabelledObjects() {
        addInputMat(INPUT_LABELS);
        addOutputMat(OUTPUT_PAINT_LABELLED);
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public GetLabelledObjects setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public boolean isPaintLabelledOnSource() {
        return paintLabelledOnSource;
    }

    public GetLabelledObjects setPaintLabelledOnSource(boolean paintLabelledOnSource) {
        this.paintLabelledOnSource = paintLabelledOnSource;
        return this;
    }

    public boolean isVisiblePaintLabelled() {
        return visiblePaintLabelled;
    }

    public GetLabelledObjects setVisiblePaintLabelled(boolean visiblePaintLabelled) {
        this.visiblePaintLabelled = visiblePaintLabelled;
        return this;
    }

    public GetLabelledObjects requestFillLabelled() {
        requestOutput(OUTPUT_PAINT_LABELLED);
        return this;
    }

    public SMat resultFillLabelled() {
        return getMat(OUTPUT_PAINT_LABELLED);
    }

    @Override
    public SNumbers analyse(final MultiMatrix source) {
        Objects.requireNonNull(source, "Null source");
        final ValuesAtLabelledObjects valuesAtLabelledObjects = new ValuesAtLabelledObjects()
                .setRawValues(rawValues)
                .setPaintLabelledOnSource(paintLabelledOnSource);
        Map<ValuesAtLabelledObjects.ObjectParameter, SNumbers> results = new HashMap<>();
        final SNumbers result = new SNumbers();
        results.put(ValuesAtLabelledObjects.ObjectParameter.FIRST_NON_ZERO, result);
        final MultiMatrix2D source2D = source.asMultiMatrix2D();
        final MultiMatrix2D labelsMatrix = getInputMat(INPUT_LABELS).toMultiMatrix2D();
        valuesAtLabelledObjects.analyse(
                results,
                source2D,
                labelsMatrix,
                null,
                null);
        if (isOutputNecessary(OUTPUT_PAINT_LABELLED)) {
            getMat(OUTPUT_PAINT_LABELLED).setTo(
                    ValuesAtLabelledObjects.paintLabelledObjects(
                            result,
                            source2D,
                            labelsMatrix,
                            null,
                            false,
                            paintLabelledOnSource));
        }
        return result;
    }

    @Override
    public String visibleOutputPortName() {
        return visiblePaintLabelled ? OUTPUT_PAINT_LABELLED : super.visibleOutputPortName();
    }

}
