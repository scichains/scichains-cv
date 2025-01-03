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

package net.algart.executors.modules.cv.matrices.derivatives;

import net.algart.arrays.DoubleArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.PFloatingArray;
import net.algart.matrices.linearfiltering.Convolution;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HessianEigenValuesAndVectors extends MultichannelDerivativesFilter {
    public static final String OUTPUT_LAMBDA_1 = "lambda1";
    public static final String OUTPUT_LAMBDA_2 = "lambda2";
    public static final String OUTPUT_LAMBDA_1_PLUS = "lambda1_plus";
    public static final String OUTPUT_LAMBDA_2_PLUS = "lambda2_plus";
    public static final String OUTPUT_LAMBDA_1_MINUS = "lambda1_minus";
    public static final String OUTPUT_LAMBDA_2_MINUS = "lambda2_minus";
    public static final String OUTPUT_1_X = "vx1";
    public static final String OUTPUT_1_Y = "vy1";
    public static final String OUTPUT_2_X = "vx2";
    public static final String OUTPUT_2_Y = "vy2";
    public static final String OUTPUT_GRADIENT_SCALAR_PRODUCT_1 = "gradient_v1_scalar_product";
    public static final String OUTPUT_GRADIENT_SCALAR_PRODUCT_2 = "gradient_v2_scalar_product";

    private static final Map<String, HessianOperation> HESSIAN_OPERATIONS = new LinkedHashMap<>();

    static {
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_1, HessianOperation.LAMBDA_1);
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_2, HessianOperation.LAMBDA_2);
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_1_PLUS, HessianOperation.LAMBDA_1_PLUS);
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_2_PLUS, HessianOperation.LAMBDA_2_PLUS);
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_1_MINUS, HessianOperation.LAMBDA_1_MINUS);
        HESSIAN_OPERATIONS.put(OUTPUT_LAMBDA_2_MINUS, HessianOperation.LAMBDA_2_MINUS);
        HESSIAN_OPERATIONS.put(OUTPUT_1_X, HessianOperation.VECTOR_1_X);
        HESSIAN_OPERATIONS.put(OUTPUT_1_Y, HessianOperation.VECTOR_1_Y);
        HESSIAN_OPERATIONS.put(OUTPUT_2_X, HessianOperation.VECTOR_2_X);
        HESSIAN_OPERATIONS.put(OUTPUT_2_Y, HessianOperation.VECTOR_2_Y);
        HESSIAN_OPERATIONS.put(OUTPUT_GRADIENT_SCALAR_PRODUCT_1, HessianOperation.VECTOR_1_SCALAR_PRODUCT);
        HESSIAN_OPERATIONS.put(OUTPUT_GRADIENT_SCALAR_PRODUCT_2, HessianOperation.VECTOR_2_SCALAR_PRODUCT);
    }

    private boolean orderEigenValuesByMagnitude = false;
    private boolean stableEigenVectorsSignumX = false;
    private boolean normalizeEigenVectors = false;

    public HessianEigenValuesAndVectors() {
        useVisibleResultParameter();
        removeOutputPort(DEFAULT_OUTPUT_PORT);
        for (String port : HESSIAN_OPERATIONS.keySet()) {
            addOutputMat(port);
        }
    }

    public boolean isOrderEigenValuesByMagnitude() {
        return orderEigenValuesByMagnitude;
    }

    public HessianEigenValuesAndVectors setOrderEigenValuesByMagnitude(boolean orderEigenValuesByMagnitude) {
        this.orderEigenValuesByMagnitude = orderEigenValuesByMagnitude;
        return this;
    }

    public boolean isStableEigenVectorsSignumX() {
        return stableEigenVectorsSignumX;
    }

    public HessianEigenValuesAndVectors setStableEigenVectorsSignumX(boolean stableEigenVectorsSignumX) {
        this.stableEigenVectorsSignumX = stableEigenVectorsSignumX;
        return this;
    }

    public boolean isNormalizeEigenVectors() {
        return normalizeEigenVectors;
    }

    public HessianEigenValuesAndVectors setNormalizeEigenVectors(boolean normalizeEigenVectors) {
        this.normalizeEigenVectors = normalizeEigenVectors;
        return this;
    }

    public void process(final Map<HessianOperation, MultiMatrix2D> results, MultiMatrix2D source) {
        source = preprocess(source);
        final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
        final Convolution convolution = createConvolution();
        final Class<? extends PFloatingArray> type = floatingType(source.elementType());
        final Map<HessianOperation, List<Matrix<? extends PArray>>> resultChannels = new LinkedHashMap<>();
        boolean gradientVectorNecessary = false;
        for (HessianOperation operation : results.keySet()) {
            resultChannels.put(operation, new ArrayList<>());
            gradientVectorNecessary |= operation.additionalVectorRequired();
        }
        for (final Matrix<? extends PArray> m : sourceChannels) {
            final Matrix<? extends PFloatingArray> d2dx2 = DerivativeOperation.D2_DX2.process(type, convolution, m);
            final Matrix<? extends PFloatingArray> d2dy2 = DerivativeOperation.D2_DY2.process(type, convolution, m);
            final Matrix<? extends PFloatingArray> d2dxdy = DerivativeOperation.D2_DXDY.process(type, convolution, m);
            final Matrix<? extends PFloatingArray> dx, dy;
            if (gradientVectorNecessary) {
                dx = DerivativeOperation.DX.process(type, convolution, m);
                dy = DerivativeOperation.DY.process(type, convolution, m);
            } else {
                dx = null;
                dy = null;
            }
            for (Map.Entry<HessianOperation, List<Matrix<? extends PArray>>> entry : resultChannels.entrySet()) {
                final HessianOperation operation = entry.getKey();
                entry.getValue().add(operation.asOperation(
                        DoubleArray.class,
                        // - lazy operation, so we can require maximal precision without spending memory
                        d2dx2,
                        d2dy2,
                        d2dxdy,
                        dx,
                        dy,
                        orderEigenValuesByMagnitude,
                        stableEigenVectorsSignumX,
                        normalizeEigenVectors));
            }
        }
        for (Map.Entry<HessianOperation, List<Matrix<? extends PArray>>> entry : resultChannels.entrySet()) {
            results.put(
                    entry.getKey(),
                    MultiMatrix.valueOf2DMono(combineResult(source.arrayType(), entry.getValue())));
        }
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final Map<HessianOperation, MultiMatrix2D> results = new LinkedHashMap<>();
        for (Map.Entry<String, HessianOperation> entry : HESSIAN_OPERATIONS.entrySet()) {
            if (isOutputNecessary(entry.getKey())) {
                results.put(entry.getValue(), null);
                // - requesting
            }
        }
        process(results, source);
        for (Map.Entry<String, HessianOperation> entry : HESSIAN_OPERATIONS.entrySet()) {
            final MultiMatrix2D result = results.get(entry.getValue());
            if (result != null) {
                getMat(entry.getKey()).setTo(result);
            }
        }
        return null;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }
}
