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

package net.algart.executors.modules.cv.matrices.objects.binary;

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.RectangularFunc;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;

import java.util.ArrayList;
import java.util.List;

public final class NeighbourPixelsCount extends BitMultiMatrixFilter {
    public static final String OUTPUT_Q = "Q";

    private static final long[][] NEIGHBOUR_SHIFTS_4 = {
            {1, 0},
            {0, 1},
            {-1, 0},
            {0, -1},
    };
    private static final long[][] NEIGHBOUR_SHIFTS_8 = {
            {1, 0},
            {0, 1},
            {-1, 0},
            {0, -1},
            {1, 1},
            {-1, 1},
            {-1, -1},
            {1, -1},
    };

    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private int minNumberOfNeighbours = 0;
    private int maxNumberOfNeighbours = 0;
    // - for zero central pixel this value is decreased by 1
    private boolean unitPixelsOnly = true;


    public NeighbourPixelsCount() {
        addOutputMat(OUTPUT_Q);
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public NeighbourPixelsCount setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    public int getMinNumberOfNeighbours() {
        return minNumberOfNeighbours;
    }

    public NeighbourPixelsCount setMinNumberOfNeighbours(int minNumberOfNeighbours) {
        this.minNumberOfNeighbours = minNumberOfNeighbours;
        return this;
    }

    public int getMaxNumberOfNeighbours() {
        return maxNumberOfNeighbours;
    }

    public NeighbourPixelsCount setMaxNumberOfNeighbours(int maxNumberOfNeighbours) {
        this.maxNumberOfNeighbours = maxNumberOfNeighbours;
        return this;
    }

    public boolean isUnitPixelsOnly() {
        return unitPixelsOnly;
    }

    public NeighbourPixelsCount setUnitPixelsOnly(boolean unitPixelsOnly) {
        this.unitPixelsOnly = unitPixelsOnly;
        return this;
    }

    @Override
    public Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> bitMatrix) {
        final long[][] neighbourShifts;
        switch (connectivityType) {
            case STRAIGHT_ONLY:
                neighbourShifts = NEIGHBOUR_SHIFTS_4;
                break;

            case STRAIGHT_AND_DIAGONAL:
                neighbourShifts = NEIGHBOUR_SHIFTS_8;
                break;
            default:
                throw new UnsupportedOperationException("Unsuported " + connectivityType);
        }
        final List<Matrix<? extends PArray>> shifted = new ArrayList<Matrix<? extends PArray>>();
        shifted.add(bitMatrix);
        for (long[] shift : neighbourShifts) {
            shifted.add(Matrices.asShifted(bitMatrix, shift).cast(PArray.class));
        }
        final Matrix<? extends PArray> counter = Matrices.clone(
                Matrices.asFuncMatrix(
                        LinearFunc.getNonweightedInstance(0.0, 1.0, shifted.size()),
                        IntArray.class, shifted));
        getMat(OUTPUT_Q).setTo(MultiMatrix.valueOf2DMono(reduce(counter)));
        final Matrix<BitArray> result = Matrices.asFuncMatrix(
                RectangularFunc.getInstance(
                        minNumberOfNeighbours + 1, maxNumberOfNeighbours + 1, 1.0, 0.0),
                BitArray.class, counter);
        return unitPixelsOnly ?
                Matrices.asFuncMatrix(Func.MIN, BitArray.class, result, bitMatrix) :
                result;
    }

    @Override
    protected boolean zeroExtending() {
        return true;
    }
}
