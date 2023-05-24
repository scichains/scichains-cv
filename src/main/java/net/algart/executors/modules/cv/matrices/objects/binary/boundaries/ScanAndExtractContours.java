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
import net.algart.arrays.*;
import net.algart.math.IPoint;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.Boundary2DWrapper;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.scanning.ContourLineType;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToNumbers;

public final class ScanAndExtractContours extends MultiMatrixToNumbers {
    public static final String INPUT_POSITION = "position";
    public static final String OUTPUT_CONTOURS = "contours";
    public static final String OUTPUT_STRICT_AREA = "strict_area";
    public static final String OUTPUT_SEGMENT_CENTERS_AREA = "segment_centers_area";
    public static final String OUTPUT_STRICT_PERIMETER = "strict_perimeter";
    public static final String OUTPUT_SEGMENT_CENTERS_PERIMETER = "segment_centers_perimeter";

    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_ONLY;
    private BoundaryType boundaryType = BoundaryType.ALL_BOUNDARIES;
    private AbstractScanAndMeasureBoundaries.ObjectValues objectsInterpretation =
            AbstractScanAndMeasureBoundaries.ObjectValues.NON_ZERO_LABELS;
    private Integer frameId = null;
    private int startX = 0;
    private int startY = 0;
    private boolean optimizeCollinearSteps = false;

    public ScanAndExtractContours() {
        useVisibleResultParameter();
        setDefaultInputMat(AbstractScanAndMeasureBoundaries.INPUT_OBJECTS);
        addInputNumbers(INPUT_POSITION);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputMat(AbstractScanAndMeasureBoundaries.OUTPUT_LABELS);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL);
        addOutputNumbers(OUTPUT_STRICT_AREA);
        addOutputNumbers(OUTPUT_SEGMENT_CENTERS_AREA);
        addOutputNumbers(OUTPUT_STRICT_PERIMETER);
        addOutputNumbers(OUTPUT_SEGMENT_CENTERS_PERIMETER);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public ScanAndExtractContours setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    public BoundaryType getBoundaryType() {
        return boundaryType;
    }

    public ScanAndExtractContours setBoundaryType(BoundaryType boundaryType) {
        this.boundaryType = nonNull(boundaryType);
        return this;
    }

    public AbstractScanAndMeasureBoundaries.ObjectValues getObjectsInterpretation() {
        return objectsInterpretation;
    }

    public ScanAndExtractContours setObjectsInterpretation(
            AbstractScanAndMeasureBoundaries.ObjectValues objectsInterpretation) {
        this.objectsInterpretation = nonNull(objectsInterpretation);
        return this;
    }

    public Integer getFrameId() {
        return frameId;
    }

    public ScanAndExtractContours setFrameId(Integer frameId) {
        this.frameId = frameId;
        return this;
    }

    public int getStartX() {
        return startX;
    }

    public ScanAndExtractContours setStartX(int startX) {
        this.startX = startX;
        return this;
    }

    public int getStartY() {
        return startY;
    }

    public ScanAndExtractContours setStartY(int startY) {
        this.startY = startY;
        return this;
    }

    public boolean isOptimizeCollinearSteps() {
        return optimizeCollinearSteps;
    }

    public ScanAndExtractContours setOptimizeCollinearSteps(boolean optimizeCollinearSteps) {
        this.optimizeCollinearSteps = optimizeCollinearSteps;
        return this;
    }

    @Override
    public SNumbers analyse(MultiMatrix source) {
        final SNumbers position = getInputNumbers(INPUT_POSITION, true);
        final IPoint originPoint = position.isProbableRectangularArea() ?
                position.toIRectangularArea().min() :
                position.toIPoint();
        return analyse(BoundariesScanner.toObjects(source, objectsInterpretation.binaryOnly()), originPoint);
    }

    public SNumbers analyse(final Matrix<? extends PFixedArray> objects, IPoint originPoint) {
        if (originPoint != null && originPoint.coordCount() != objects.dimCount()) {
            throw new IllegalArgumentException("Different number of dimensions: " + originPoint.coordCount()
                    + "-dimensional point (" + originPoint + ") and " + objects.dimCount()
                    + "-dimensional matrix (" + objects + ")");
        }
        final long startX = originPoint == null ? this.startX : originPoint.x();
        final long startY = originPoint == null ? this.startY : originPoint.y();
        final boolean resultLabelsRequired = isOutputNecessary(AbstractScanAndMeasureBoundaries.OUTPUT_LABELS);
        final MutableIntArray objectLabelArray = Arrays.SMM.newEmptyIntArray();
        final MutableBitArray internalBoundaryFlags = Arrays.SMM.newEmptyBitArray();
        final MutableLongArray strictArea = Arrays.SMM.newEmptyLongArray();
        final MutableDoubleArray segmentCentersArea = Arrays.SMM.newEmptyDoubleArray();
        final MutableIntArray contourLength = Arrays.SMM.newEmptyIntArray();
        final MutableDoubleArray segmentCentersPerimeter = Arrays.SMM.newEmptyDoubleArray();
        final BoundariesScanner scanner = new BoundariesScanner(
                objects,
                getConnectivityType(),
                getBoundaryType(),
                false,
                resultLabelsRequired,
                Long.MAX_VALUE);
        final Boundary2DScanner boundaryScanner = scanner.getBoundaryScanner();
        final Contours contours = Contours.newInstance().setOptimizeCollinearSteps(optimizeCollinearSteps);
        scanner.setBoundaryMeasurer(new Boundary2DWrapper(boundaryScanner) {
            @Override
            public void next() {
                super.next();
                contours.addPoint(parent, startX, startY);
            }
        });
        scanner.setProcessBackgroundAsObject(getObjectsInterpretation().processBackgroundAsObject());
        ContourHeader header = new ContourHeader();
        if (frameId != null) {
            header.setFrameId(frameId);
        }
        while (scanner.nextBoundary()) {
            contours.openContourForAdding(header);
            scanner.scanAndProcess();
            final long stepCount = boundaryScanner.stepCount();
            assert stepCount == (int) stepCount : "Contour2DArray.closeContour did not step count";
            header.setObjectLabel(scanner.currentLabel());
            // - important: scanner's current label is set only in scanAndProcess()!
            header.setInternalContour(boundaryScanner.orientedArea() < 0);
            contours.closeContour(header);
            if (scanner.needToAnalyseThisBoundary()) {
                objectLabelArray.pushInt(scanner.currentLabel());
                internalBoundaryFlags.pushBit(scanner.internalBoundary());
                strictArea.pushLong(boundaryScanner.orientedArea());
                segmentCentersArea.pushDouble(boundaryScanner.area(ContourLineType.SEGMENT_CENTERS_POLYLINE));
                contourLength.pushInt((int) stepCount);
                segmentCentersPerimeter.pushDouble(boundaryScanner.perimeter(
                        ContourLineType.SEGMENT_CENTERS_POLYLINE));
            } else {
                contours.removeLastContour();
            }
        }
        ScanAndMeasureBoundaries.uploadSimpleOutputs(this, scanner, objectLabelArray, internalBoundaryFlags);
        logDebug(() -> "Scanned " + scanner.objectCounter() + " contours, "
                + scanner.sideCounter() + " pixel sides");
        if (resultLabelsRequired) {
            getMat(AbstractScanAndMeasureBoundaries.OUTPUT_LABELS).setTo(ScanAndMeasureBoundaries.getLabels(scanner));
        }
        if (isOutputNecessary(OUTPUT_STRICT_AREA)) {
            getNumbers(OUTPUT_STRICT_AREA).setTo(strictArea, 1);
        }
        if (isOutputNecessary(OUTPUT_SEGMENT_CENTERS_AREA)) {
            getNumbers(OUTPUT_SEGMENT_CENTERS_AREA).setTo(segmentCentersArea, 1);
        }
        if (isOutputNecessary(OUTPUT_STRICT_PERIMETER)) {
            getNumbers(OUTPUT_STRICT_PERIMETER).setTo(contourLength, 1);
        }
        if (isOutputNecessary(OUTPUT_SEGMENT_CENTERS_PERIMETER)) {
            getNumbers(OUTPUT_SEGMENT_CENTERS_PERIMETER).setTo(segmentCentersPerimeter, 1);
        }
        return SNumbers.valueOf(contours);
    }
}
