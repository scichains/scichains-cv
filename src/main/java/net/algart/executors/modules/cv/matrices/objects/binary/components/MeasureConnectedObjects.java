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

package net.algart.executors.modules.cv.matrices.objects.binary.components;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.cv.matrices.objects.MeasureLabelledObjects;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Map;

public final class MeasureConnectedObjects extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_OBJECTS = "objects";
    public static final String INPUT_MASK = "mask";

    private double pixelSize = 1.0;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;

    public MeasureConnectedObjects() {
        useVisibleResultParameter();
        setDefaultInputMat(INPUT_OBJECTS);
        setDefaultOutputNumbers(MeasureLabelledObjects.ObjectParameter.AREA.outputPort());
        addInputMat(INPUT_MASK);
        for (MeasureLabelledObjects.ObjectParameter parameter : MeasureLabelledObjects.ObjectParameter.values()) {
            addOutputNumbers(parameter.outputPort());
        }
    }

    public double getPixelSize() {
        return pixelSize;
    }

    public MeasureConnectedObjects setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public MeasureConnectedObjects setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix2D labels = getInputMat(INPUT_OBJECTS, false).toMultiMatrix2D();
        final MultiMatrix2D mask = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        final Map<MeasureLabelledObjects.ObjectParameter, SNumbers> resultStatistics =
                MeasureLabelledObjects.convertMap(allOutputContainers(SNumbers.class, true));
        setStartProcessingTimeStamp();
        new MeasureLabelledObjects()
                .setAutoSplitBitInputIntoConnectedComponents(true)
                .setPixelSize(pixelSize)
                .setBitInputConnectivityType(connectivityType)
                .analyse(
                        resultStatistics,
                        labels.nonZeroRGB(),
                        mask);
        setEndProcessingTimeStamp();
    }
}
