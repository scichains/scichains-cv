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

package net.algart.executors.modules.cv.matrices.derivatives;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.PFloatingArray;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.List;

public final class Derivatives extends MultichannelDerivativesFilter {
    private DerivativeOperation operation = DerivativeOperation.DX_PAIR;

    public Derivatives() {
    }

    public DerivativeOperation getOperation() {
        return operation;
    }

    public void setOperation(DerivativeOperation operation) {
        this.operation = nonNull(operation);
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        source = preprocess(source);
        final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
        final List<Matrix<? extends PArray>> processed = new ArrayList<>();
        for (Matrix<? extends PArray> sourceChannel : sourceChannels) {
            processed.add(processChannel(sourceChannel));
        }
        return MultiMatrix.valueOf2DMono(combineResult(source.arrayType(), processed));
    }

    public Matrix<? extends PFloatingArray> processChannel(Matrix<? extends PArray> m) {
        return operation.process(floatingType(m.elementType()), createConvolution(), m);
    }
}
