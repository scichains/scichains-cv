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

import net.algart.arrays.*;
import net.algart.contours.Contours;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.multimatrix.MultiMatrix;

import java.util.Objects;

class BoundariesScanner {
    private static final int BACKGROUND = 0;

    private final ConnectivityType connectivityType;
    private final BoundaryType boundaryType;

    private final Matrix<? extends PFixedArray> objects;
    private final PFixedArray objectsArray;
    private final SwitchableBitMatrices switchable;
    private final boolean binary;
    private final Boundary2DScanner boundaryScanner;
    private final LabelsDrawer labelsDrawer;
    private final long maxLevelForLabelsDrawer;

    private boolean processBackgroundAsObject = false;
    private Boundary2DScanner boundaryMeasurer;

    private long sideCounter = 0;
    private long objectCounter = 0;
    private int currentLabel = 0;
    private boolean internalBoundary = false;

    BoundariesScanner(
            final Matrix<? extends PFixedArray> objects,
            ConnectivityType connectivityType,
            BoundaryType boundaryType,
            boolean needSecondBuffer) {
        this(objects, connectivityType, boundaryType, needSecondBuffer, false, Long.MAX_VALUE);
    }

    BoundariesScanner(
            final Matrix<? extends PFixedArray> objects,
            ConnectivityType connectivityType,
            BoundaryType boundaryType,
            boolean needSecondBufferForBinary,
            boolean needLabels,
            long maxLevelForLabelsDrawer) {
        this.objects = Objects.requireNonNull(objects);
        this.objectsArray = objects.array();
        if (maxLevelForLabelsDrawer != Long.MAX_VALUE) {
            needSecondBufferForBinary = true;
        }
        this.switchable = new SwitchableBitMatrices(objects, needSecondBufferForBinary);
        this.binary = switchable.isBinary();
        if (!binary && !boundaryType.supportsLabels()) {
            throw new IllegalArgumentException("The mode " + boundaryType + " is supported for binary matrices only");
        }
        this.connectivityType = Objects.requireNonNull(connectivityType);
        this.boundaryType = Objects.requireNonNull(boundaryType);
        this.boundaryScanner = binary ?
                boundaryType.newScanner(switchable, this.connectivityType) :
                Boundary2DScanner.getSingleBoundaryScanner(switchable.bits(), connectivityType);
        this.boundaryMeasurer = boundaryScanner;
        // - by default, no any additional actions
        this.labelsDrawer = needLabels ? new LabelsDrawer(this) : null;
        this.maxLevelForLabelsDrawer = maxLevelForLabelsDrawer;
    }

    public Boundary2DScanner getBoundaryScanner() {
        return boundaryScanner;
    }

    public Matrix<? extends PFixedArray> objects() {
        return objects;
    }

    public BoundaryType getBoundaryType() {
        return boundaryType;
    }

    public Boundary2DScanner getBoundaryMeasurer() {
        return boundaryMeasurer;
    }

    public void setBoundaryMeasurer(Boundary2DScanner measurer) {
        this.boundaryMeasurer = measurer;
    }

    public boolean isProcessBackgroundAsObject() {
        return processBackgroundAsObject;
    }

    public void setProcessBackgroundAsObject(boolean processBackgroundAsObject) {
        this.processBackgroundAsObject = processBackgroundAsObject;
    }

    public boolean isBinary() {
        return binary;
    }

    public long sideCounter() {
        return sideCounter;
    }

    public long objectCounter() {
        return objectCounter;
    }

    public int currentLabel() {
        return currentLabel;
    }

    public boolean internalBoundary() {
        return internalBoundary;
    }

    public boolean nextBoundary() {
        return binary ?
                boundaryMeasurer.nextBoundary() :
                nextBoundaryForLabels();
    }

    public boolean needToAnalyseThisBoundary() {
        if (binary) {
            return boundaryType.needToAnalyseThisBoundary(boundaryScanner);
        } else {
            return (processBackgroundAsObject || currentLabel != BACKGROUND)
                    && boundaryType.needToAnalyseThisBoundary(boundaryScanner.orientedArea() < 0);
        }
    }

    public void scanAndProcess() {
//            System.out.println(boundaryScanner.x() + ", " + boundaryScanner.y());
        currentLabel = objectsArray.getInt(boundaryScanner.currentIndexInArray());
        if (binary) {
            scanBinary();
        } else {
            scanLabels();
        }
        sideCounter += boundaryMeasurer.stepCount();
        if (needToAnalyseThisBoundary()) {
            objectCounter++;
        }
    }


    public Matrix<UpdatableBitArray> getMainBuffer() {
        return switchable.buffer1();
    }

    public Matrix<? extends PArray> getLabels() {
        if (labelsDrawer != null) {
            labelsDrawer.buildLabels();
            return binary ?
                    boundaryType.onlyActualLabels(labelsDrawer.getLabels(), boundaryScanner.matrix()) :
                    labelsDrawer.getLabels();
        } else {
            return null;
        }
    }

    public static Matrix<? extends PFixedArray> toObjects(MultiMatrix source, boolean binaryOnly) {
        checkObjects(source, binaryOnly);
        if (binaryOnly) {
            return BitMultiMatrixFilter.toBit(source.intensityChannel());
        } else {
            final Matrix<? extends PArray> channel = Matrices.clone(source.channel(0));
            assert channel.array() instanceof PFixedArray;
            return channel.cast(PFixedArray.class);
        }
    }

    public static void checkObjects(MultiMatrix matrix, boolean binaryOnly) {
        if (matrix.dimCount() != 2) {
            throw new IllegalArgumentException("Objects must be represented by 2-dimensional matrix "
                    + "(multidimensional matrices are not supported), but we have " + matrix);
        }
        if (matrix.dim(0) >= Contours.MAX_ABSOLUTE_COORDINATE
                || matrix.dim(1) >= Contours.MAX_ABSOLUTE_COORDINATE) {
            // - note: maximal x-coordinate for boundary pixel will be dimX, not dimX-1
            throw new TooLargeArrayException("Matrices with sizes > "
                    + (Contours.MAX_ABSOLUTE_COORDINATE - 1) + " = 0x"
                    + Integer.toHexString(Contours.MAX_ABSOLUTE_COORDINATE - 1)
                    + " are not supported by boundaries scanner, but we have " + matrix);
        }
        if (!binaryOnly) {
            if (!matrix.isMono() || matrix.isFloatingPoint() || matrix.bitsPerElement() > 32) {
                throw new IllegalArgumentException("Objects must be represented by 1-channel integer matrix "
                        + "with <=32 bits/element, but we have " + matrix);
                // 64-bit long cannot be saved in Contour2DArray
            }
        }
    }

    private boolean needToDrawLabel() {
        if (binary) {
            return boundaryType.needToAnalyseThisBoundary(boundaryScanner);
        } else {
            return (processBackgroundAsObject || currentLabel != BACKGROUND)
                    && (boundaryScanner.orientedArea() >= 0) == boundaryType.includesExternalBoundary();
            // - Internal boundaries (negative integerArea) are drawn ONLY in ALL_INTERNAL_BOUNDARIES mode.
            // In other modes it is senseless: pore in object A is (at the same time) another object B,
            // so, pores A will be re-filled by corresponding objects B.
        }
    }

    private boolean nextBoundaryForLabels() {
        for (; ; ) {
            if (!nextSingleBoundaryForLabels()) {
                return false;
            }
            assert boundaryScanner.side() == Boundary2DScanner.Side.X_MINUS;
            final boolean bracket1 = switchable.buffer1().array().getBit(boundaryScanner.currentIndexInArray());
            if (!bracket1) {
                return true;
            }
        }
    }

    private boolean nextSingleBoundaryForLabels() {
        final PFixedArray array = switchable.objects().array();
        final long dimX = switchable.objects().dimX();
        if (array.length() == 0) {
            return false;
        }
        if (!boundaryScanner.isInitialized()) {
            boundaryMeasurer.goTo(0, 0, Boundary2DScanner.Side.X_MINUS);
            // boundaryMeasurer, not boundaryScanner! we need to reset it
            return true;
        }
        long index = boundaryScanner.currentIndexInArray();
        // = y * dimX + x
        final int currentLabel = objectsArray.getInt(index);
        index++;
        long y = boundaryScanner.y();
        for (long x = boundaryScanner.x() + 1; x < dimX; x++, index++) {
            if (objectsArray.getInt(index) != currentLabel) {
                boundaryMeasurer.goTo(x, y, Boundary2DScanner.Side.X_MINUS);
                // boundaryMeasurer, not boundaryScanner! we need to reset it
                return true;
            }
        }
        y++;
        if (y == switchable.objects().dimY()) {
            return false;
        }
        // Unlike binary scanner, we should consider the beginning of the new line as a possible start of new object
        boundaryScanner.goTo(0, y, Boundary2DScanner.Side.X_MINUS);
        return true;
    }

    private void scanBinary() {
        if (labelsDrawer != null && needToDrawLabel()) {
            internalBoundary = boundaryScanner.isInternalBoundary();
//            System.out.printf("**   %d: %s%n", objectCounter,  boundaryMeasurer);
            do {
                boundaryMeasurer.next();
                if (boundaryScanner.nestingLevel() <= maxLevelForLabelsDrawer) {
                    // if maxLevelForLabelsDrawer == Long.MAX_VALUE, result of nestingLevel() does not matter
                    labelsDrawer.drawBracket(objectCounter, internalBoundary);
                }
//                System.out.printf("**   %d/%d: %s%n", objectCounter, boundaryMeasurer.stepCount(), boundaryMeasurer);
            } while (!boundaryMeasurer.boundaryFinished());
        } else {
            do {
                boundaryMeasurer.next();
            } while (!boundaryMeasurer.boundaryFinished());
        }
    }

    // Note: always calculates integerArea
    private void scanLabels() {
        switchable.setCurrentLabel(currentLabel);
        if (labelsDrawer != null) {
//            System.out.printf("!!   %d: %d, %s%n", objectCounter,  currentLabel, boundaryMeasurer);
            do {
                boundaryMeasurer.next();
                setHorizontalBracketForLabels();
//                System.out.printf("!!   %d/%d: %d, %s, %s, %s, %s%n",
//                        objectCounter, boundaryMeasurer.stepCount(), currentLabel, boundaryMeasurer,
//                        boundaryMeasurer.x() + boundaryMeasurer.lastStep().increasedPixelVertexX(),
//                        boundaryMeasurer.y() + boundaryMeasurer.lastStep().increasedPixelVertexY(),
//                        boundaryMeasurer.atMatrixBoundary());
            } while (!boundaryMeasurer.boundaryFinished());
            internalBoundary = boundaryMeasurer.orientedArea() < 0;
            // - in this case we need the second pass: standard boundaryScanner.isInternalBoundary()
            // does not work properly. and we need the integer area to determine boundary kind
            if (needToDrawLabel()) {
                // - note: for labels, needToDrawLabel will be true EITHER for external, OR for internal
                // boundaries, but never for both kinds
                boundaryScanner.resetCounters();
                // - but preserving counters of boundaryMeasurer
                do {
                    boundaryScanner.next();
                    // - no measuring!
                    labelsDrawer.drawBracket(objectCounter, internalBoundary);
                } while (!boundaryScanner.boundaryFinished());
            }
//            labelsDrawer.debuggingPrintLabels();
        } else {
            do {
                boundaryMeasurer.next();
                setHorizontalBracketForLabels();
//                System.out.println("   " + boundaryMeasurer.stepCount() + ": " + boundaryMeasurer);
            } while (!boundaryMeasurer.boundaryFinished());
            internalBoundary = boundaryMeasurer.orientedArea() < 0;
        }
    }

    private void setHorizontalBracketForLabels() {
        if (boundaryScanner.side() == Boundary2DScanner.Side.X_MINUS) {
            switchable.setBuffer1Bit(boundaryScanner.currentIndexInArray());
        }
    }
}
