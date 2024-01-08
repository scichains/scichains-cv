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

import net.algart.executors.modules.cv.matrices.objects.RetainOrRemoveMode;
import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public final class FindConnectedWithMask extends ConnectedComponentScanning {
    public static final String INPUT_OBJECTS = "objects";
    public static final String INPUT_MASK = "mask";

    private RetainOrRemoveMode mode = RetainOrRemoveMode.RETAIN;
    private boolean invertMask = false;
    private boolean includeMaskInRetained = false;

    public FindConnectedWithMask() {
        super(INPUT_OBJECTS, INPUT_MASK);
    }

    public RetainOrRemoveMode getMode() {
        return mode;
    }

    public void setMode(RetainOrRemoveMode mode) {
        this.mode = nonNull(mode);
    }

    public boolean isInvertMask() {
        return invertMask;
    }

    public void setInvertMask(boolean invertMask) {
        this.invertMask = invertMask;
    }

    public boolean isIncludeMaskInRetained() {
        return includeMaskInRetained;
    }

    public void setIncludeMaskInRetained(boolean includeMaskInRetained) {
        this.includeMaskInRetained = includeMaskInRetained;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(
        List<Matrix<? extends UpdatablePArray>> bitMatrices,
        List<MultiMatrix2D> sources)
    {
        final Matrix<UpdatableBitArray> objects = asBit(bitMatrices.get(0));
        final Matrix<UpdatableBitArray> mask = asBit(bitMatrices.get(1));
        final Matrix<UpdatableBitArray> objectsClone = mode == RetainOrRemoveMode.RETAIN ? cloneBit(objects) : null;
        if (invertMask) {
            Matrices.applyFunc(null, Func.REVERSE, mask, mask);
            clearBorderInExtended(mask);
        }
        final Matrix<UpdatableBitArray> maskClone = includeMaskInRetained ? cloneBit(mask) : null;
        final ConnectedObjectScanner scanner = connectedObjectScanner(mask);
        final long pixelCounter = scanner.clearAllConnected(null, objects);
        logDebug(() -> "FindConnectedWithMask: " + pixelCounter + " pixels scanned for " + objects);
        if (mode == RetainOrRemoveMode.RETAIN) {
            Matrices.applyFunc(null, Func.ABS_DIFF, objects, objects, objectsClone);
            if (includeMaskInRetained) {
                assert maskClone != null;
                Matrices.applyFunc(null, Func.MAX, objects, objects, maskClone);
            }
        }
        return objects;
    }
}
