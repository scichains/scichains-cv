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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixOperationWithRequiredResult;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public final class FilterLabelledObjectsByIntersectionWithMask extends BitMultiMatrixOperationWithRequiredResult {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_MASK = "mask";

    public enum AreaInterpretation {
        NUMBER_OF_PIXELS,
        FRACTION_OF_OBJECT_0_1
    }

    private RetainOrRemoveMode mode = RetainOrRemoveMode.RETAIN;
    private AreaInterpretation areaInterpretation = AreaInterpretation.NUMBER_OF_PIXELS;
    private double minAreaAtMask = 0.0;
    private boolean invertMask = false;
    private int fillerForClearedAreas = 0;

    public FilterLabelledObjectsByIntersectionWithMask() {
        super(INPUT_LABELS, INPUT_MASK);
    }

    public RetainOrRemoveMode getMode() {
        return mode;
    }

    public FilterLabelledObjectsByIntersectionWithMask setMode(RetainOrRemoveMode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public AreaInterpretation getAreaInterpretation() {
        return areaInterpretation;
    }

    public FilterLabelledObjectsByIntersectionWithMask setAreaInterpretation(AreaInterpretation areaInterpretation) {
        this.areaInterpretation = nonNull(areaInterpretation);
        return this;
    }

    public double getMinAreaAtMask() {
        return minAreaAtMask;
    }

    public FilterLabelledObjectsByIntersectionWithMask setMinAreaAtMask(double minAreaAtMask) {
        this.minAreaAtMask = nonNegative(minAreaAtMask);
        return this;
    }

    public boolean isInvertMask() {
        return invertMask;
    }

    public FilterLabelledObjectsByIntersectionWithMask setInvertMask(boolean invertMask) {
        this.invertMask = invertMask;
        return this;
    }

    public int getFillerForClearedAreas() {
        return fillerForClearedAreas;
    }

    public FilterLabelledObjectsByIntersectionWithMask setFillerForClearedAreas(int fillerForClearedAreas) {
        this.fillerForClearedAreas = fillerForClearedAreas;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources) {
        final MultiMatrix2D labelsMatrix = sources.get(0);
        final int[] labelArray = labelsMatrix.channelToIntArray(0);
        final Matrix<UpdatableBitArray> mask = asBit(bitMatrices.get(1));
        if (invertMask && mask != null) {
            Matrices.applyFunc(null, Func.REVERSE, mask, mask);
        }
        int max = 0;
        for (int v : labelArray) {
            if (v > max) {
                max = v;
            }
        }
        if (LOGGABLE_DEBUG) {
            logDebug("Filtering " + max + " labelled objects at " + labelsMatrix);
        }
        final int[] histogram;
        final boolean absoluteArea = areaInterpretation == AreaInterpretation.NUMBER_OF_PIXELS || minAreaAtMask == 0.0;
        if (absoluteArea) {
            final int minNumberOfPixel = minAreaAtMask == 0.0 ? 1 : (int) minAreaAtMask;
            histogram = histogramOfPositive(max, labelArray, mask == null ? null : mask.array());
            for (int k = 0; k < histogram.length; k++) {
                if (histogram[k] < minNumberOfPixel) {
                    histogram[k] = -1;
                }
            }
        } else {
            histogram = histogramOfPositive(max, labelArray, null);
            int[] histogramAtMask = mask == null ? histogram : histogramOfPositive(max, labelArray, mask.array());
            for (int k = 0; k < histogram.length; k++) {
                if (histogramAtMask[k] < minAreaAtMask * histogram[k]) {
                    histogram[k] = -1;
                }
            }
        }
        switch (mode) {
            case RETAIN:
                for (int i = 0; i < labelArray.length; i++) {
                    int v = labelArray[i];
                    if (v > 0 && histogram[v - 1] == -1) {
                        labelArray[i] = fillerForClearedAreas;
                    }
                }
                break;
            case REMOVE:
                for (int i = 0; i < labelArray.length; i++) {
                    int v = labelArray[i];
                    if (v > 0 && histogram[v - 1] != -1) {
                        labelArray[i] = fillerForClearedAreas;
                    }
                }
                break;
        }
        return Matrices.matrix(IntArray.as(labelArray), labelsMatrix.dimensions());
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex == 1;
    }

    @Override
    protected boolean bitInput(int inputIndex) {
        return inputIndex == 1;
    }

    // histogram[k-1] is number of elements ==k
    private static int[] histogramOfPositive(int max, int[] array, BitArray mask) {
        int[] histogram = new int[max];
        // - zero-filled by Java
        if (mask == null) {
            for (int v : array) {
                if (v > 0) {
                    histogram[v - 1]++;
                }
            }
        } else {
            for (int i = 0; i < array.length; i++) {
                if (mask.getBit(i)) {
                    int v = array[i];
                    if (v > 0) {
                        histogram[v - 1]++;
                    }
                }
            }
        }
        return histogram;
    }
}
