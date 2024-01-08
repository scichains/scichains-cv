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

public final class Gradient extends MultichannelDerivativesFilter {
    public static final String OUTPUT_DX = "dx";
    public static final String OUTPUT_DY = "dy";

    private GradientOperation operation = GradientOperation.SIMPLE_PAIR;
    private CombiningMatricesMetric combiningDerivativesMetric = CombiningMatricesMetric.NORMALIZED_EUCLIDEAN;

    private MultiMatrix2D gradientDX = null;
    private MultiMatrix2D gradientDY = null;
    private MultiMatrix2D gradientMagnitude = null;

    public Gradient() {
        addOutputMat(OUTPUT_DX);
        addOutputMat(OUTPUT_DY);
    }

    public GradientOperation getOperation() {
        return operation;
    }

    public void setOperation(GradientOperation operation) {
        this.operation = nonNull(operation);
    }

    public CombiningMatricesMetric getCombiningDerivativesMetric() {
        return combiningDerivativesMetric;
    }

    public void setCombiningDerivativesMetric(CombiningMatricesMetric combiningDerivativesMetric) {
        nonNull(combiningDerivativesMetric);
        if (combiningDerivativesMetric.isSingleChannel()) {
            throw new IllegalArgumentException("Cannot use single-channel metric for combining x/y-derivatives");
        }
        this.combiningDerivativesMetric = combiningDerivativesMetric;
    }

    public MultiMatrix2D gradientDX() {
        return gradientDX;
    }

    public MultiMatrix2D gradientDY() {
        return gradientDY;
    }

    public MultiMatrix2D magnitude() {
        return gradientMagnitude;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        source = preprocess(source);
        final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
        final List<Matrix<? extends PArray>> processed = new ArrayList<>();
        final List<Matrix<? extends PArray>> additionalResult1 = new ArrayList<>();
        final List<Matrix<? extends PArray>> additionalResult2 = new ArrayList<>();
        for (Matrix<? extends PArray> sourceChannel : sourceChannels) {
            processed.add(processChannel(sourceChannel, additionalResult1, additionalResult2));
        }
        this.gradientMagnitude = MultiMatrix.valueOf2DMono(combineResult(source.arrayType(), processed));
        this.gradientDX = MultiMatrix.valueOf2DMono(combineResult(source.arrayType(), additionalResult1));
        this.gradientDY = MultiMatrix.valueOf2DMono(combineResult(source.arrayType(), additionalResult2));
        getMat(OUTPUT_DX).setTo(gradientDX);
        getMat(OUTPUT_DY).setTo(gradientDY);
        return gradientMagnitude;
    }


    private Matrix<? extends PFloatingArray> processChannel(
            Matrix<? extends PArray> m,
            List<Matrix<? extends PArray>> additionalResult1,
            List<Matrix<? extends PArray>> additionalResult2) {
        final Class<? extends PFloatingArray> requiredType = floatingType(m.elementType());
        final List<Matrix<? extends PArray>> derivatives = new ArrayList<>();
        for (DerivativeOperation derivativeOperation : operation.derivatives()) {
            derivatives.add(derivativeOperation.process(requiredType, createConvolution(), m));
        }
        assert derivatives.size() >= 2;
        additionalResult1.add(derivatives.get(0));
        additionalResult2.add(derivatives.get(1));
        return combiningDerivativesMetric.combine(requiredType, derivatives, new double[0], 1.0);
    }
}
