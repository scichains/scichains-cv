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

import java.util.Objects;

class SwitchableBitMatrices {
    private final Matrix<? extends PFixedArray> objects;
    private final Matrix<? extends BitArray> bits;
    private final Matrix<UpdatableBitArray> buffer1;
    private final Matrix<UpdatableBitArray> buffer2;
    private final UpdatableBitArray buffer1Array;
    private final boolean binary;

    private int currentLabel = 0;

    public SwitchableBitMatrices(Matrix<? extends PFixedArray> objects, boolean needSecondBufferForBinary) {
        this.objects = Objects.requireNonNull(objects, "Null objects matrix");
        this.binary = objects.elementType() == boolean.class;
        if (this.binary) {
            this.bits = objects.cast(BitArray.class);
            this.buffer1 = Arrays.SMM.newBitMatrix(objects.dimensions());
            this.buffer2 = needSecondBufferForBinary ?
                    Arrays.SMM.newBitMatrix(objects.dimensions()) :
                    buffer1;
            ;
        } else {
            final PFixedArray objectsArray = objects.array();
            this.bits = objects.matrix(new AbstractBitArray(objects.size(), true) {
                @Override
                public boolean getBit(long index) {
                    return (objectsArray.getInt(index) == currentLabel);
                }
            });
            this.buffer1 = Arrays.SMM.newBitMatrix(objects.dimensions());
            this.buffer2 = null;
            // - not used
        }
        this.buffer1Array = buffer1.array();
    }

    public Matrix<? extends PFixedArray> objects() {
        return objects;
    }

    public Matrix<? extends BitArray> bits() {
        return bits;
    }

    public Matrix<UpdatableBitArray> buffer1() {
        return buffer1;
    }

    public Matrix<UpdatableBitArray> buffer2() {
        return buffer2;
    }

    public boolean isBinary() {
        return binary;
    }

    public int getCurrentLabel() {
        return currentLabel;
    }

    public void setCurrentLabel(int currentLabel) {
        this.currentLabel = currentLabel;
    }

    public void setBuffer1Bit(long index) {
        buffer1Array.setBitNoSync(index);
    }
}
