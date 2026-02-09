/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.*;
import net.algart.executors.modules.cv.matrices.objects.RetainOrRemoveMode;
import net.algart.math.functions.Func;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public final class FilterConnectedObjectsByAreas extends ConnectedComponentScanning {
    public static final String INPUT_OBJECTS = "objects";
    public static final String INPUT_MASK = "mask";

    public enum AreaInterpretation {
        NUMBER_OF_PIXELS,
        FRACTION_OF_WHOLE_IMAGE_0_1;

        long convert(double area, Matrix<?> matrix) {
            if (this == FRACTION_OF_WHOLE_IMAGE_0_1) {
                area *= matrix.size();
            }
            return StrictMath.round(area);
        }
    }

    private RetainOrRemoveMode mode = RetainOrRemoveMode.REMOVE;
    private AreaInterpretation areaInterpretation = AreaInterpretation.FRACTION_OF_WHOLE_IMAGE_0_1;
    private double minArea = Double.NEGATIVE_INFINITY;
    private double maxArea = Double.POSITIVE_INFINITY;
    private boolean invertMask = false;

    public FilterConnectedObjectsByAreas() {
        super(INPUT_OBJECTS, INPUT_MASK);
    }

    public RetainOrRemoveMode getMode() {
        return mode;
    }

    public FilterConnectedObjectsByAreas setMode(RetainOrRemoveMode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public AreaInterpretation getAreaInterpretation() {
        return areaInterpretation;
    }

    public FilterConnectedObjectsByAreas setAreaInterpretation(AreaInterpretation areaInterpretation) {
        this.areaInterpretation = nonNull(areaInterpretation);
        return this;
    }

    public double getMinArea() {
        return minArea;
    }

    public FilterConnectedObjectsByAreas setMinArea(double minArea) {
        this.minArea = nonNegative(minArea);
        return this;
    }

    public FilterConnectedObjectsByAreas setMinArea(String minArea) {
        this.minArea = doubleOrNegativeInfinity(minArea);
        return this;
    }

    public double getMaxArea() {
        return maxArea;
    }

    public FilterConnectedObjectsByAreas setMaxArea(double maxArea) {
        this.maxArea = nonNegative(maxArea);
        return this;
    }

    public FilterConnectedObjectsByAreas setMaxArea(String maxArea) {
        this.maxArea = doubleOrPositiveInfinity(maxArea);
        return this;
    }

    public boolean isInvertMask() {
        return invertMask;
    }

    public FilterConnectedObjectsByAreas setInvertMask(boolean invertMask) {
        this.invertMask = invertMask;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(
            List<Matrix<? extends UpdatablePArray>> bitMatrices,
            List<MultiMatrix2D> sources) {
        final Matrix<UpdatableBitArray> objects = asBit(bitMatrices.get(0));
        final Matrix<UpdatableBitArray> mask = asBit(bitMatrices.get(1));
        final Matrix<UpdatableBitArray> objectsClone = mode == RetainOrRemoveMode.REMOVE ? cloneBit(objects) : null;
        if (invertMask && mask != null) {
            Matrices.applyFunc(null, Func.REVERSE, mask, mask);
            clearBorderInExtended(mask);
        }
        final long minNonClearedSize = areaInterpretation.convert(minArea, objects);
        final long maxNonClearedSize = areaInterpretation.convert(maxArea, objects);
        final ConnectedObjectScanner scanner = connectedObjectScanner(objects);
        final long pixelCounter = scanner.clearAllBySizes(null, mask, minNonClearedSize, maxNonClearedSize);
        logDebug(() -> "Filtering connected objects by areas: " + pixelCounter + " pixels scanned for " + objects);
        if (mode == RetainOrRemoveMode.REMOVE) {
            Matrices.applyFunc(null, Func.ABS_DIFF, objects, objects, objectsClone);
        }
        return objects;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex == 1;
    }
}