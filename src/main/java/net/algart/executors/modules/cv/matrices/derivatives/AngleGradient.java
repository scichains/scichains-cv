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

import net.algart.arrays.FloatArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.executors.modules.core.matrices.arithmetic.AngleDistanceMetric;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class AngleGradient extends SeveralMultiMatricesOperation {
    public static final String INPUT_VECTOR_X = "vx";
    public static final String INPUT_VECTOR_Y = "vy";
    public static final String OUTPUT_ANGLE_DX = "angle_dx";
    public static final String OUTPUT_ANGLE_DY = "angle_dy";

    private AngleDistanceMetric angleDistanceMetric = AngleDistanceMetric.R_SIN_COS;
    private CombiningMatricesMetric combiningDerivativesMetric = CombiningMatricesMetric.NORMALIZED_EUCLIDEAN;
    private double additionalMultiplier = 1.0;

    private MultiMatrix2D angleGradientDX = null;
    private MultiMatrix2D angleGradientDY = null;
    private MultiMatrix2D angleGradientMagnitude = null;

    public AngleGradient() {
        super(INPUT_VECTOR_X, INPUT_VECTOR_Y);
        addOutputMat(OUTPUT_ANGLE_DX);
        addOutputMat(OUTPUT_ANGLE_DY);
    }

    public AngleDistanceMetric getAngleDistanceMetric() {
        return angleDistanceMetric;
    }

    public AngleGradient setAngleDistanceMetric(AngleDistanceMetric angleDistanceMetric) {
        this.angleDistanceMetric = nonNull(angleDistanceMetric);
        return this;
    }

    public CombiningMatricesMetric getCombiningDerivativesMetric() {
        return combiningDerivativesMetric;
    }

    public AngleGradient setCombiningDerivativesMetric(CombiningMatricesMetric combiningDerivativesMetric) {
        this.combiningDerivativesMetric = nonNull(combiningDerivativesMetric);
        return this;
    }

    public double getAdditionalMultiplier() {
        return additionalMultiplier;
    }

    public void setAdditionalMultiplier(double additionalMultiplier) {
        this.additionalMultiplier = additionalMultiplier;
    }

    public MultiMatrix2D angleGradientDX() {
        return angleGradientDX;
    }

    public MultiMatrix2D angleGradientDY() {
        return angleGradientDY;
    }

    public MultiMatrix2D angleGradientMagnitude() {
        return angleGradientMagnitude;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        final Matrix<? extends PArray> vx = extend(
                sources.get(0).asMultiMatrix2D().asFloatingPoint().intensityChannel());
        final Matrix<? extends PArray> vy = extend(
                sources.get(1).asMultiMatrix2D().asFloatingPoint().intensityChannel());
        final Matrix<? extends PArray> vxRight = shift(vx, 1, 0);
        final Matrix<? extends PArray> vyRight = shift(vy, 1, 0);
        final Matrix<? extends PArray> vxDown = shift(vx, 0, 1);
        final Matrix<? extends PArray> vyDown = shift(vy, 0, 1);
        final Matrix<? extends PArray> angleDX = reduce(
                angleDistanceMetric.asAngleDifference(vx, vy, vxRight, vyRight, FloatArray.class));
        final Matrix<? extends PArray> angleDY = reduce(
                angleDistanceMetric.asAngleDifference(vx, vy, vxDown, vyDown, FloatArray.class));
        final Matrix<? extends FloatArray> magnitude = combiningDerivativesMetric.combine(
                FloatArray.class, Arrays.asList(angleDX, angleDY), new double[0], additionalMultiplier);
        this.angleGradientDX = MultiMatrix.of2DMono(multiply(angleDX, additionalMultiplier));
        this.angleGradientDY = MultiMatrix.of2DMono(multiply(angleDY, additionalMultiplier));
        this.angleGradientMagnitude = MultiMatrix.of2DMono(magnitude);
        getMat(OUTPUT_ANGLE_DX).setTo(angleGradientDX);
        getMat(OUTPUT_ANGLE_DY).setTo(angleGradientDY);
        return this.angleGradientMagnitude;
    }

    private static Matrix<? extends PArray> extend(Matrix<? extends PArray> matrix) {
        return Matrices.clone(
                matrix.subMatrix(
                        0, 0, matrix.dimX() + 1, matrix.dimY() + 1,
                        Matrix.ContinuationMode.MIRROR_CYCLIC));
    }

    private static Matrix<? extends PArray> reduce(Matrix<? extends PArray> matrix) {
        return Matrices.clone(
                matrix.subMatrix(0, 0, matrix.dimX() - 1, matrix.dimY() - 1));
    }

    private static Matrix<? extends PArray> shift(Matrix<? extends PArray> matrix, int dx, int dy) {
        return Matrices.asShifted(matrix, dx, dy).cast(PArray.class);
    }

    private static Matrix<? extends PArray> multiply(Matrix<? extends PArray> matrix, double multiplier) {
        return multiplier == 1.0 ?
                matrix :
                Matrices.asFuncMatrix(LinearFunc.getInstance(0.0, multiplier), FloatArray.class, matrix);
    }
}
