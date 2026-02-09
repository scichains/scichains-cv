/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.api.Executor;
import net.algart.math.Point;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.Morphology;

public enum MorphologyOperation {
    DILATION {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.dilation(matrix, pattern);
        }
    },
    EROSION {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.erosion(matrix, pattern);
        }
    },
    CLOSING {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.closing(matrix, pattern, Morphology.SubtractionMode.NONE);
        }
    },
    OPENING {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.opening(matrix, pattern, Morphology.SubtractionMode.NONE);
        }
    },
    WEAK_DILATION {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.weakDilation(matrix, pattern);
        }
    },
    WEAK_EROSION {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.weakErosion(matrix, pattern);
        }
    },
    WEAK_CLOSING {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.maskedErosionDilation(
                    matrix,
                    MorphologyOperation.boundary(pattern, executor),
                    pattern);
        }
    },
    WEAK_OPENING {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.maskedDilationErosion(
                    matrix,
                    MorphologyOperation.boundary(pattern, executor),
                    pattern);
        }
    },
    EXTERNAL_GRADIENT {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.dilation(matrix, pattern, Morphology.SubtractionMode.SUBTRACT_SRC_FROM_RESULT);
        }
    },
    INTERNAL_GRADIENT {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.erosion(matrix, pattern, Morphology.SubtractionMode.SUBTRACT_RESULT_FROM_SRC);
        }
    },
    BLACK_HAT {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.closing(matrix, pattern, Morphology.SubtractionMode.SUBTRACT_SRC_FROM_RESULT);
        }
    },
    TOP_HAT {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.opening(matrix, pattern, Morphology.SubtractionMode.SUBTRACT_RESULT_FROM_SRC);
        }
    },
    CRATERS {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.erosionDilation(
                    matrix,
                    MorphologyOperation.boundary(pattern, executor),
                    pattern,
                    Morphology.SubtractionMode.SUBTRACT_SRC_FROM_RESULT);
        }
    },
    PEAKS {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.dilationErosion(
                    matrix,
                    MorphologyOperation.boundary(pattern, executor),
                    pattern,
                    Morphology.SubtractionMode.SUBTRACT_RESULT_FROM_SRC);
        }
    },
    BEUCHER_GRADIENT {
        @Override
        Matrix<? extends UpdatablePArray> perform(
                Morphology morphology,
                Matrix<? extends PArray> matrix,
                Pattern pattern,
                Executor executor) {
            return morphology.beucherGradient(matrix, pattern);
        }
    };

    public Matrix<? extends UpdatablePArray> perform(
            Morphology morphology,
            Matrix<? extends PArray> matrix,
            Pattern pattern) {
        return perform(morphology, matrix, pattern, null);
    }

    public static Pattern crossPattern(int numberOfDimensions) {
        return Patterns.newSphereIntegerPattern(Point.origin(numberOfDimensions), 1.000001);
    }

    public static Pattern cube3Pattern(int numberOfDimensions) {
        return MorphologyFilter.Shape.CUBE.newPattern(numberOfDimensions, 3);
    }

    abstract Matrix<? extends UpdatablePArray> perform(
            Morphology morphology,
            Matrix<? extends PArray> matrix,
            Pattern pattern,
            Executor executor);

    private static Pattern boundary(Pattern main, Executor executor) {
        final long t1 = executor == null ? 0 : System.nanoTime();
        final Pattern carcassPattern = PatternSpecificationParser.isRectangularInteger(main) ?
                cube3Pattern(main.dimCount()) :
                crossPattern(main.dimCount());
        // - for rectangular patters, substracting cube 3x3x... will have the same effect and will work much faster
        final Pattern erosion = main.minkowskiSubtract(carcassPattern);
        final Pattern result = erosion == null ? main : PatternSpecificationParser.subtract(main, erosion);
        if (executor != null) {
            final long t2 = System.nanoTime();
            executor.addServiceTime(t2 - t1);
        }
        return result;
    }

}
