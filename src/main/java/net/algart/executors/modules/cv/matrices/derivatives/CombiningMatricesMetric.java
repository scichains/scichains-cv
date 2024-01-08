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

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public enum CombiningMatricesMetric {
    SINGLE_CHANNEL {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            final Matrix<? extends PArray> m = matrices.get(0);
            return Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, weights[0]), requiredType, m);
        }
    },
    SINGLE_CHANNEL_PLUS_HALF {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            final Matrix<? extends PArray> m = matrices.get(0);
            final double maxValue = Arrays.maxPossibleValue(requiredType, 1.0);
            return Matrices.asFuncMatrix(LinearFunc.getInstance(0.5 * maxValue, weights[0]), requiredType, m);
        }
    },
    EUCLIDEAN {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            if (matrices.size() == 1) {
                return SUM_OF_ABSOLUTE_VALUES.perform(requiredType, matrices, weights);
            }
            final List<Matrix<? extends PArray>> sqr = new ArrayList<>();
            for (Matrix<? extends PArray> matrix : matrices) {
                sqr.add(Matrices.asFuncMatrix(PowerFunc.getInstance(2.0), DoubleArray.class, matrix));
            }
            final Matrix<? extends PArray> sum = Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0, sqr(weights)),
                    DoubleArray.class, sqr);
            return Matrices.asFuncMatrix(PowerFunc.getInstance(0.5), requiredType, sum);
        }
    },
    NORMALIZED_EUCLIDEAN {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            if (matrices.size() == 1) {
                return SUM_OF_ABSOLUTE_VALUES.perform(requiredType, matrices, weights);
            }
            final List<Matrix<? extends PArray>> sqr = new ArrayList<>();
            for (Matrix<? extends PArray> matrix : matrices) {
                sqr.add(Matrices.asFuncMatrix(PowerFunc.getInstance(2.0), DoubleArray.class, matrix));
            }
            final Matrix<? extends PArray> sum = Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0, mul(sqr(weights), 1.0 / matrices.size())),
                    DoubleArray.class, sqr);
            return Matrices.asFuncMatrix(PowerFunc.getInstance(0.5), requiredType, sum);
        }
    },
    SUM_OF_ABSOLUTE_VALUES {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            final List<Matrix<? extends PArray>> abs = new ArrayList<>();
            for (Matrix<? extends PArray> matrix : matrices) {
                abs.add(Matrices.asFuncMatrix(Func.ABS, DoubleArray.class, matrix));
            }
            return Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, abs(weights)), requiredType, abs);
        }
    },
    MEAN_ABSOLUTE_VALUE {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            final List<Matrix<? extends PArray>> abs = new ArrayList<>();
            for (Matrix<? extends PArray> matrix : matrices) {
                abs.add(Matrices.asFuncMatrix(Func.ABS, DoubleArray.class, matrix));
            }
            return Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0, mul(abs(weights), 1.0 / matrices.size())),
                    requiredType, abs);
        }
    },
    MAX_ABSOLUTE_VALUE {
        @Override
        <T extends PArray> Matrix<? extends T> perform(
                Class<? extends T> requiredType,
                List<Matrix<? extends PArray>> matrices,
                double[] weights) {
            if (matrices.size() == 1) {
                return SUM_OF_ABSOLUTE_VALUES.perform(requiredType, matrices, weights);
            }
            final List<Matrix<? extends PArray>> abs = new ArrayList<>();
            for (Matrix<? extends PArray> matrix : matrices) {
                final Matrix<DoubleArray> m = Matrices.asFuncMatrix(
                        LinearFunc.getInstance(0.0, weights[abs.size()]),
                        DoubleArray.class, matrix);
                abs.add(Matrices.asFuncMatrix(Func.ABS, DoubleArray.class, m));
            }
            return Matrices.asFuncMatrix(Func.MAX, requiredType, abs);
        }
    };

    abstract <T extends PArray> Matrix<? extends T> perform(
            Class<? extends T> requiredType,
            List<Matrix<? extends PArray>> matrices,
            double[] weights);

    public boolean isSingleChannel() {
        return this == SINGLE_CHANNEL || this == SINGLE_CHANNEL_PLUS_HALF;
    }

    public <T extends PArray> Matrix<? extends T> combine(
            Class<? extends T> requiredType,
            List<Matrix<? extends PArray>> matrices,
            double[] weights,
            double additionalMultiplier) {
        Objects.requireNonNull(matrices, "Null matrices");
        Objects.requireNonNull(weights, "Null weights");
        final double[] appendedWeights = new double[matrices.size()];
        for (int i = 0; i < appendedWeights.length; i++) {
            appendedWeights[i] = (i >= weights.length ? 1.0 : weights[i]) * additionalMultiplier;
        }
        return perform(requiredType, matrices, appendedWeights);
    }

    private static double[] sqr(double[] weights) {
        final double[] result = new double[weights.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = weights[k] * weights[k];
        }
        return result;
    }

    private static double[] abs(double[] weights) {
        final double[] result = new double[weights.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = Math.abs(weights[k]);
        }
        return result;
    }

    private static double[] mul(double[] weights, double multiplier) {
        final double[] result = new double[weights.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = weights[k] * multiplier;
        }
        return result;
    }
}
