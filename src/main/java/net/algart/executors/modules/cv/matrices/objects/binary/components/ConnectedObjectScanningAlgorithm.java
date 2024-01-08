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

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.matrices.scanning.ConnectivityType;

// Note: please use this factory only for bit matrices, appended by zero pixels from all 4 sides
public enum ConnectedObjectScanningAlgorithm {
    QUICKEN,
    BREADTH_FIRST,
    DEPTH_FIRST;

    public static final long MAX_MEMORY_FOR_QUICKEN_VERSION = Arrays.SystemSettings.maxTempJavaMemory();

    public ConnectedObjectScanner connectedObjectScanner(
        Matrix<? extends UpdatableBitArray> bitMatrix,
        ConnectivityType connectivityType,
        boolean checked)
    {
        boolean littleEnough = bitMatrix.size() <= MAX_MEMORY_FOR_QUICKEN_VERSION;
        switch (this) {
            case DEPTH_FIRST:
                return checked ?
                    ConnectedObjectScanner.getDepthFirstScanner(bitMatrix, connectivityType) :
                    ConnectedObjectScanner.getUncheckedDepthFirstScanner(bitMatrix, connectivityType);
            case QUICKEN:
                if (littleEnough) {
                    return checked ?
                        ConnectedObjectScanner.getStacklessDepthFirstScanner(bitMatrix, connectivityType) :
                        ConnectedObjectScanner.getUncheckedStacklessDepthFirstScanner(bitMatrix, connectivityType);
                }
            case BREADTH_FIRST:
                return checked ?
                    ConnectedObjectScanner.getBreadthFirstScanner(bitMatrix, connectivityType) :
                    ConnectedObjectScanner.getUncheckedBreadthFirstScanner(bitMatrix, connectivityType);
            default:
                throw new AssertionError("Unknown algorithm " + this);
        }
    }
}
