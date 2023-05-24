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

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;

import java.util.Objects;

public final class SelectLabelledObjectsByBinaryArray extends MultiMatrix2DFilter {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_BASE = "base";
    public static final String INPUT_SELECTOR = "selector";
    public static final String OUTPUT_SELECTED = "selected";

    public enum SelectorInterpretation {
        SELECT_NON_ZERO,
        SELECT_ZERO
    }

    public enum ActionOnBase {
        ADD_SELECTED_TO_BASE,
        REMOVE_SELECTED_FROM_BASE
    }

    private SelectorInterpretation selectorInterpretation = SelectorInterpretation.SELECT_NON_ZERO;
    private ActionOnBase actionOnBase = ActionOnBase.ADD_SELECTED_TO_BASE;

    public SelectLabelledObjectsByBinaryArray() {
        setDefaultInputMat(INPUT_LABELS);
        addInputMat(INPUT_BASE);
        addInputNumbers(INPUT_SELECTOR);
        addOutputMat(OUTPUT_SELECTED);
    }

    public SelectorInterpretation getSelectorInterpretation() {
        return selectorInterpretation;
    }

    public void setSelectorInterpretation(SelectorInterpretation selectorInterpretation) {
        this.selectorInterpretation = nonNull(selectorInterpretation);
    }

    public ActionOnBase getActionOnBase() {
        return actionOnBase;
    }

    public void setActionOnBase(ActionOnBase actionOnBase) {
        this.actionOnBase = nonNull(actionOnBase);
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D labelsMatrix) {
        final MultiMatrix2D base = getInputMat(INPUT_BASE, true).toMultiMatrix2D();
        final SNumbers selectors = getInputNumbers(INPUT_SELECTOR);
        return process(labelsMatrix, base, selectors);
    }

    public MultiMatrix2D process(MultiMatrix2D labelsMatrix, MultiMatrix2D baseMatrix, SNumbers selectors) {
        Objects.requireNonNull(labelsMatrix, "Null labels");
        Objects.requireNonNull(selectors, "Null selectors");
        labelsMatrix.checkDimensionEquality(baseMatrix, "labels", "base");
        final byte[] selectorsArray = selectors.column(0).toByteArray();
        final int[] labels = labelsMatrix.channelToIntArray(0);
        final boolean[] selectedArray = new boolean[labels.length];
        switch (selectorInterpretation) {
            case SELECT_NON_ZERO: {
                for (int i = 0; i < labels.length; i++) {
                    final int label = labels[i];
                    if (label > 0 && label <= selectorsArray.length) {
                        selectedArray[i] = selectorsArray[label - 1] != 0;
                    }
                }
                break;
            }
            case SELECT_ZERO:
                for (int i = 0; i < labels.length; i++) {
                    final int label = labels[i];
                    if (label > 0 && label <= selectorsArray.length) {
                        selectedArray[i] = selectorsArray[label - 1] == 0;
                    }
                }
                break;
            default: {
                throw new AssertionError("Unknown " + selectorInterpretation);
            }
        }
        final Matrix<UpdatableBitArray> selected = Arrays.SMM.newBitMatrix(labelsMatrix.dimensions());
        selected.array().setData(0, selectedArray);
        final MultiMatrix2D selectedMatrix = MultiMatrix.valueOf2DMono(selected);
        getMat(OUTPUT_SELECTED).setTo(selectedMatrix);
        if (baseMatrix == null) {
            return actionOnBase == ActionOnBase.REMOVE_SELECTED_FROM_BASE ?
                    selectedMatrix.zeroAllChannels() :
                    selectedMatrix;
        } else {
            return actionOnBase == ActionOnBase.REMOVE_SELECTED_FROM_BASE ?
                    baseMatrix.min(selectedMatrix.zeroAllChannels()) :
                    baseMatrix.max(selectedMatrix);
        }
    }
}
