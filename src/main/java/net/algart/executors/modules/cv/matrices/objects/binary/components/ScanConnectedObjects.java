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

package net.algart.executors.modules.cv.matrices.objects.binary.components;

import net.algart.arrays.*;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;

public final class ScanConnectedObjects extends ConnectedComponentScanning {
    public static final String INPUT_OBJECTS = "objects";
    public static final String OUTPUT_LABELS = "labels";

    public ScanConnectedObjects() {
        super(INPUT_OBJECTS);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(
        List<Matrix<? extends UpdatablePArray>> bitMatrices,
        List<MultiMatrix2D> sources)
    {
        final Matrix<UpdatableBitArray> objects = asBit(bitMatrices.get(0));
        final Matrix<UpdatableIntArray> result = Arrays.SMM.newIntMatrix(objects.dimensions());
        assert result.array() instanceof DirectAccessible;
        final DirectAccessible da = (DirectAccessible) result.array();
        assert da.hasJavaArray();
        final int[] labels = (int[]) da.javaArray();
        final int ofs = da.javaArrayOffset();
        final ConnectedObjectScanner scanner = connectedObjectScanner(objects);
        long[] coordinates = new long[objects.dimCount()]; // zero-filled
        class Painter implements ConnectedObjectScanner.ElementVisitor {
            private int currentLabel = 1;

            public void visit(long[] coordinatesInMatrix, long indexInArray) {
                labels[(int) (ofs + indexInArray)] = currentLabel;
            }
        }
        Painter painter = new Painter();
        while (scanner.nextUnitBit(coordinates)) {
            scanner.clear(null, painter, coordinates, false);
            painter.currentLabel++;
        }
        return result;
    }
}
