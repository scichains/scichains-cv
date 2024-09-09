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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.scanning.ContourLineType;

abstract class AbstractScanAndMeasureBoundaries extends MultiMatrix2DFilter {
    public static final String INPUT_OBJECTS = "objects";
    public static final String OUTPUT_LABELS = "labels";

    private double pixelSize = 1.0;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private BoundaryType boundaryType = BoundaryType.MAIN_BOUNDARIES;
    private ContourLineType contourLineType = ContourLineType.STRICT_BOUNDARY;
    private long maxLabelLevel = 0;
    // - 0 means "all levels"
    ObjectValues objectsInterpretation = ObjectValues.BINARY;

    protected AbstractScanAndMeasureBoundaries() {
        useVisibleResultParameter();
        setDefaultInputMat(INPUT_OBJECTS);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public double getPixelSize() {
        return pixelSize;
    }

    public void setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public void setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
    }

    public BoundaryType getBoundaryType() {
        return boundaryType;
    }

    public void setBoundaryType(BoundaryType boundaryType) {
        this.boundaryType = nonNull(boundaryType);
    }

    public ContourLineType getContourLineType() {
        return contourLineType;
    }

    public void setContourLineType(ContourLineType contourLineType) {
        this.contourLineType = nonNull(contourLineType);
    }

    public long getMaxLabelLevel() {
        return maxLabelLevel;
    }

    public void setMaxLabelLevel(long maxLabelLevel) {
        this.maxLabelLevel = maxLabelLevel;
    }

    public ObjectValues getObjectsInterpretation() {
        return objectsInterpretation;
    }

    public void setObjectsInterpretation(ObjectValues objectsInterpretation) {
        this.objectsInterpretation = nonNull(objectsInterpretation);
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }

    long getMaxLabelLevelOrMaxValue() {
        return maxLabelLevel <= 0 ? Long.MAX_VALUE : maxLabelLevel;
    }

}
