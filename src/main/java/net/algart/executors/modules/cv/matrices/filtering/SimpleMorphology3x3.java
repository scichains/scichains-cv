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

package net.algart.executors.modules.cv.matrices.filtering;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.matrices.filters3x3.*;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;

import java.util.Locale;
import java.util.function.Function;

public final class SimpleMorphology3x3 extends MultiMatrixFilter {
    public enum Shape {
        CROSS(matrix -> DilationByCross3x3.newInstance(matrix.elementType(), matrix.dimensions()),
                matrix -> ErosionByCross3x3.newInstance(matrix.elementType(), matrix.dimensions())),
        SQUARE(matrix -> DilationBySquare3x3.newInstance(matrix.elementType(), matrix.dimensions()),
                matrix -> ErosionBySquare3x3.newInstance(matrix.elementType(), matrix.dimensions()));

        private final Function<MultiMatrix, AbstractQuickFilter3x3> getDilation;
        private final Function<MultiMatrix, AbstractQuickFilter3x3> getErosion;

        Shape(
                Function<MultiMatrix, AbstractQuickFilter3x3> getDilation,
                Function<MultiMatrix, AbstractQuickFilter3x3> getErosion) {
            this.getDilation = getDilation;
            this.getErosion = getErosion;
        }
    }

    public enum Operation {
        DILATION {
            @Override
            Matrix<? extends UpdatablePArray> perform(
                    Matrix<? extends PArray> matrix,
                    AbstractQuickFilter3x3 dilation,
                    AbstractQuickFilter3x3 erosion) {
                return dilation.filter(matrix);
            }
        },
        EROSION {
            @Override
            Matrix<? extends UpdatablePArray> perform(
                    Matrix<? extends PArray> matrix,
                    AbstractQuickFilter3x3 dilation,
                    AbstractQuickFilter3x3 erosion) {
                return erosion.filter(matrix);
            }
        },
        CLOSING {
            @Override
            Matrix<? extends UpdatablePArray> perform(
                    Matrix<? extends PArray> matrix,
                    AbstractQuickFilter3x3 dilation,
                    AbstractQuickFilter3x3 erosion) {
                return erosion.filter(dilation.filter(matrix));
            }
        },
        OPENING {
            @Override
            Matrix<? extends UpdatablePArray> perform(
                    Matrix<? extends PArray> matrix,
                    AbstractQuickFilter3x3 dilation,
                    AbstractQuickFilter3x3 erosion) {
                return dilation.filter(erosion.filter(matrix));
            }
        };

        abstract Matrix<? extends UpdatablePArray> perform(
                Matrix<? extends PArray> matrix,
                AbstractQuickFilter3x3 dilation,
                AbstractQuickFilter3x3 erosion);
    }

    private Operation operation = Operation.DILATION;
    private Shape shape = Shape.CROSS;

    public SimpleMorphology3x3() {
    }

    public Operation operation() {
        return operation;
    }

    public SimpleMorphology3x3 setOperation(Operation operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public Shape shape() {
        return shape;
    }

    public SimpleMorphology3x3 setShape(Shape shape) {
        this.shape = nonNull(shape);
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        long t1 = debugTime();
        final AbstractQuickFilter3x3 dilation = shape.getDilation.apply(source);
        final AbstractQuickFilter3x3 erosion = shape.getErosion.apply(source);
        long t2 = debugTime();
        final MultiMatrix result = source.mapChannels(matrix -> operation.perform(matrix, dilation, erosion));
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US, "Simple 3x3 %s by %s of %s calculated in %.3f ms: "
                        + "%.3f initializing, "
                        + "%.3f processing",
                operation.name().toLowerCase(), shape.name().toLowerCase(),
                source,
                (t3 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        return result;
    }

}
