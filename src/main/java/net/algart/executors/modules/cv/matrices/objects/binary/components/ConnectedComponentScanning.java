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

package net.algart.executors.modules.cv.matrices.objects.binary.components;

import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableBitArray;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixOperationWithRequiredResult;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.matrices.scanning.ConnectivityType;

public abstract class ConnectedComponentScanning extends BitMultiMatrixOperationWithRequiredResult {
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private ConnectedObjectScanningAlgorithm bitScanningAlgorithm = ConnectedObjectScanningAlgorithm.QUICKEN;

    protected ConnectedComponentScanning(String... inputPortNames) {
        super(inputPortNames);
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public void setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
    }

    public ConnectedObjectScanningAlgorithm getBitScanningAlgorithm() {
        return bitScanningAlgorithm;
    }

    public void setBitScanningAlgorithm(ConnectedObjectScanningAlgorithm bitScanningAlgorithm) {
        this.bitScanningAlgorithm = nonNull(bitScanningAlgorithm);
    }

    public ConnectedObjectScanner connectedObjectScanner(Matrix<? extends UpdatableBitArray> bitMatrix) {
        return bitScanningAlgorithm.connectedObjectScanner(bitMatrix, connectivityType, !zeroExtending());
    }

    @Override
    protected boolean zeroExtending() {
        return true;
    }
}
