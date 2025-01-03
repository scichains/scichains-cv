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

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.ConnectivityType;

public enum BoundaryType {
    MAIN_BOUNDARIES() {
        @Override
        public Boundary2DScanner newScanner(
                Matrix<? extends BitArray> matrix,
                Matrix<? extends UpdatablePFixedArray> buffer1,
                Matrix<? extends UpdatablePFixedArray> buffer2,
                ConnectivityType connectivityType) {
            return Boundary2DScanner.getMainBoundariesScanner(matrix, buffer1, connectivityType);
        }
    },
    ALL_BOUNDARIES,
    ALL_EXTERNAL_BOUNDARIES() {
        @Override
        public boolean needToAnalyseThisBoundary(Boundary2DScanner scanner) {
            return scanner.side() == Boundary2DScanner.Side.X_MINUS;
        }

        @Override
        public boolean needToAnalyseThisBoundary(boolean isInternalBoundary) {
            return !isInternalBoundary;
        }

        @Override
        Func maskFunc(double labelsMaxValue) {
            return LinearFunc.getInstance(0.0, labelsMaxValue);
            // - simple scaling bits
        }
    },
    ALL_INTERNAL_BOUNDARIES() {
        @Override
        public boolean needToAnalyseThisBoundary(Boundary2DScanner scanner) {
            return scanner.side() == Boundary2DScanner.Side.X_PLUS;
        }

        @Override
        public boolean needToAnalyseThisBoundary(boolean isInternalBoundary) {
            return isInternalBoundary;
        }

        @Override
        public boolean includesExternalBoundary() {
            return false;
        }

        @Override
        Func maskFunc(double labelsMaxValue) {
            return LinearFunc.getInstance(labelsMaxValue, -labelsMaxValue);
            // - reversing bits + scaling
        }
    };

    public boolean needToAnalyseThisBoundary(Boundary2DScanner scanner) {
        return true;
    }

    public boolean needToAnalyseThisBoundary(boolean isInternalBoundary) {
        return true;
    }

    public boolean includesExternalBoundary() {
        return true;
    }

    public final boolean supportsLabels() {
        return this != MAIN_BOUNDARIES;
    }

    public Boundary2DScanner newScanner(
            Matrix<? extends BitArray> matrix,
            Matrix<? extends UpdatablePFixedArray> buffer1,
            Matrix<? extends UpdatablePFixedArray> buffer2,
            ConnectivityType connectivityType) {
        return Boundary2DScanner.getAllBoundariesScanner(matrix, buffer1, buffer2, connectivityType);
    }

    public final Boundary2DScanner newScanner(
            SwitchableBitMatrices switchable,
            ConnectivityType connectivityType) {
        return newScanner(switchable.bits(), switchable.buffer1(), switchable.buffer2(), connectivityType);
    }

    // For external boundaries, removes background area from drawn labels;
    // for internal boundaries, removes objects (retains pores only)
    Matrix<? extends PArray> onlyActualLabels(Matrix<? extends PArray> labels, Matrix<? extends BitArray> objects) {
        final Func maskFunc = maskFunc(labels.array().maxPossibleValue(1.0));
        if (maskFunc == null) {
            return labels;
        }
        final Class<? extends PArray> type = labels.type(PArray.class);
        final Matrix<? extends PArray> mask = Matrices.asFuncMatrix(maskFunc, type, objects);
        return Matrices.asFuncMatrix(Func.MIN, type, mask, labels);
    }

    Func maskFunc(double labelsMaxValue) {
        return null;
    }
}
