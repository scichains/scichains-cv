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

package net.algart.executors.modules.cv.matrices.pixels;

import net.algart.arrays.Arrays;
import net.algart.arrays.BitArray;
import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Objects;

public final class GetLabelledPixels extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_LABELS = "labels";
    public static final String OUTPUT_PIXEL_VALUES = "pixel_values";
    public static final String OUTPUT_LABEL_VALUES = "label_values";

    private boolean labelValuesInLastColumns = false;
    private boolean rawPixelValues = false;

    public GetLabelledPixels() {
        useVisibleResultParameter();
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_LABELS);
        setDefaultOutputNumbers(OUTPUT_PIXEL_VALUES);
        addOutputNumbers(OUTPUT_LABEL_VALUES);
    }

    public boolean isLabelValuesInLastColumns() {
        return labelValuesInLastColumns;
    }

    public GetLabelledPixels setLabelValuesInLastColumns(boolean labelValuesInLastColumns) {
        this.labelValuesInLastColumns = labelValuesInLastColumns;
        return this;
    }

    public boolean isRawPixelValues() {
        return rawPixelValues;
    }

    public GetLabelledPixels setRawPixelValues(boolean rawPixelValues) {
        this.rawPixelValues = rawPixelValues;
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix2D source = getInputMat().toMultiMatrix2D(true);
        final MultiMatrix2D labels = getInputMat(INPUT_LABELS, true).toMultiMatrix2D();
        setStartProcessingTimeStamp();
        final SNumbers resultPixelValues = getNumbers(OUTPUT_PIXEL_VALUES);
        final SNumbers resultLabelValues = isOutputNecessary(OUTPUT_LABEL_VALUES) ?
                getNumbers(OUTPUT_LABEL_VALUES) :
                null;
        process(source, labels, resultPixelValues, resultLabelValues);
        setEndProcessingTimeStamp();
    }

    public void process(
            MultiMatrix2D source,
            MultiMatrix2D labels,
            SNumbers resultPixelValues,
            SNumbers resultLabelValues) {
        Objects.requireNonNull(source, "Null source");
        Objects.requireNonNull(resultPixelValues, "Null resultPixelValues");
        source.checkDimensionEquality(labels, "source", "labels");
        if (source.size() != (int) source.size()) {
            throw new TooLargeArrayException("Too large matrix: " + source);
        }
        final BitArray nonZeroArray = labels == null ?
                Arrays.nBitCopies(source.size(), true) :
                labels.nonZeroAnyChannelMatrix().array().updatableClone(Arrays.SMM);
        final int numberOfLabels = (int) (labels == null ? nonZeroArray.length() : Arrays.cardinality(nonZeroArray));
        final int numberOfSourceChannels = source.numberOfChannels();
        final int numberOfLabelsChannels = labels == null ? 1 : labels.numberOfChannels();
        resultPixelValues.setToZeros(
                float.class,
                numberOfLabels,
                numberOfSourceChannels + (labelValuesInLastColumns ? numberOfLabelsChannels : 0));
        if (resultLabelValues != null && !labelValuesInLastColumns) {
            resultLabelValues.setToZeros(int.class, numberOfLabels, numberOfLabelsChannels);
        }
        MultiMatrix.PixelValue pixelValue = null;
        MultiMatrix.PixelValue labelValue = null;
        float[] pixelValueArray = new float[numberOfSourceChannels];
        int[] labelValueIntArray = labelValuesInLastColumns ? null : new int[numberOfLabelsChannels];
        float[] labelValueArray = labelValuesInLastColumns ? new float[numberOfLabelsChannels] : null;
        final double mult = 1.0 / source.maxPossibleValue();
        for (int p = 0, dispPixels = 0, dispLabels = 0, n = (int) nonZeroArray.length(); p < n; p++) {
            if (nonZeroArray.getBit(p)) {
                pixelValue = source.getPixel(p, pixelValue);
                pixelValue.getFloatChannels(pixelValueArray);
                if (!rawPixelValues) {
                    for (int i = 0; i < pixelValueArray.length; i++) {
                        pixelValueArray[i] *= mult;
                    }
                }
                resultPixelValues.setValues(dispPixels, numberOfSourceChannels, pixelValueArray);
                dispPixels += numberOfSourceChannels;
                if (labelValuesInLastColumns) {
                    if (labels != null) {
                        labelValue = labels.getPixel(p, labelValue);
                        labelValue.getFloatChannels(labelValueArray);
                        resultPixelValues.setValues(dispPixels, numberOfLabelsChannels, labelValueArray);
                    } else {
                        resultLabelValues.setValue(dispPixels, 1.0);
                    }
                    dispPixels += numberOfLabelsChannels;
                } else if (resultLabelValues != null) {
                    if (labels != null) {
                        labelValue = labels.getPixel(p, labelValue);
                        labelValue.getIntChannels(labelValueIntArray);
                        resultLabelValues.setValues(dispLabels, numberOfLabelsChannels, labelValueIntArray);
                    } else {
                        resultLabelValues.setValue(dispLabels, 1.0);
                    }
                    dispLabels += numberOfLabelsChannels;
                }
            }
        }
    }
}
