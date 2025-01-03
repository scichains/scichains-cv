/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.Arrays;
import net.algart.arrays.BitArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.ConnectivityType;

public final class BinaryFillHoles extends BitMultiMatrixFilter {
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public void setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> objects) {
        var result = Boundary2DScanner.fillHoles(Arrays.SMM, objects.cast(BitArray.class), connectivityType);
        logDebug(() -> "Filling holes in " + objects);
        return result;
    }
}
