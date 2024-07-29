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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.MutablePNumberArray;
import net.algart.matrices.scanning.Boundary2DProjectionMeasurer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

public enum ProjectionParameter {
    AREA((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.area() * (pixelSize * pixelSize);
        }
    },
    PERIMETER((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.perimeter() * pixelSize;
        }
    },
    ALL_PROJECTIONS((measurer, third) -> measurer.numberOfDirections()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            for (int k = 0, m = measurer.numberOfDirections(); k < m; k++) {
                result.addDouble(measurer.projectionLength(k) * pixelSize);
            }
        }
    },
    SELECTED_PROJECTION((measurer, third) -> 2 + third.oneIfExist()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            addProjectionPair(result, measurer, 0, pixelSize, second, third);
        }
    },
    MEAN_PROJECTION((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.meanProjectionLength() * pixelSize;
        }
    },
    MAX_PROJECTION((measurer, third) -> 2 + third.oneIfExist()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            addProjectionPair(result, measurer, measurer.indexOfMaxProjectionLength(), pixelSize, second, third);
        }
    },
    MIN_PROJECTION((measurer, third) -> 2 + third.oneIfExist()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            addProjectionPair(result, measurer, measurer.indexOfMinProjectionLength(), pixelSize, second, third);
        }
    },
    MAX_SIZE_RELATION((measurer, third) -> 2 + third.oneIfExist()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            final int m = measurer.numberOfDirections();
            int index = -1;
            double maxRelation = Double.NEGATIVE_INFINITY;
            for (int k = 0, i = m / 2; k < m; k++, i = (i + 1) % m) {
                final double relation = measurer.projectionLength(k) / measurer.projectionLength(i);
                if (relation > maxRelation) {
                    index = k;
                    maxRelation = relation;
                }
            }
            addProjectionPair(result, measurer, index, pixelSize, second, third);
        }
    },
    MIN_CIRCUMSCRIBED_SQUARE_SIDE((measurer, third) -> 2 + third.oneIfExist()) {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            addProjectionPair(
                    result,
                    measurer,
                    measurer.indexOfMinCircumscribedRhombus(measurer.numberOfDirections() / 2),
                    pixelSize,
                    second,
                    third);
        }
    },
    SHAPE_FACTOR((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return BoundaryParameter.shapeFactorCircularity(measurer.area(), measurer.perimeter());
        }
    },
    COMPACT_FACTOR((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            final double meanProjection = measurer.meanProjectionLength();
            final double meanCircleArea = 0.25 * Math.PI * meanProjection * meanProjection;
            return measurer.area() / meanCircleArea;
        }
    },
    IRREGULARITY_FACTOR((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            final double meanCircleLength = Math.PI * measurer.meanProjectionLength();
            return meanCircleLength / measurer.perimeter();
        }
    },
    NESTING_LEVEL((measurer, third) -> 1) {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.nestingLevel();
        }
    };

    private final BiFunction<Boundary2DProjectionMeasurer, ThirdProjectionValue, Integer> parameterLength;

    ProjectionParameter(BiFunction<Boundary2DProjectionMeasurer, ThirdProjectionValue, Integer> parameterLength) {
        this.parameterLength = parameterLength;
    }

    double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
        throw new UnsupportedOperationException();
    }

    public void getStatistics(
            MutablePNumberArray result,
            Boundary2DProjectionMeasurer measurer,
            double pixelSize,
            SecondProjectionValue second,
            ThirdProjectionValue third) {
        Objects.requireNonNull(result, "Null result");
        result.addDouble(getStatistics(measurer, pixelSize));
    }

    public int parameterLength(Boundary2DProjectionMeasurer measurer, ThirdProjectionValue third) {
        return parameterLength.apply(measurer, third);
    }

    public static boolean needSecondBuffer(Collection<ProjectionParameter> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        return parameters.contains(NESTING_LEVEL);
    }

    private static void addProjectionPair(
            MutablePNumberArray result,
            Boundary2DProjectionMeasurer measurer,
            int i1,
            double pixelSize,
            SecondProjectionValue second,
            ThirdProjectionValue third) {
        Objects.requireNonNull(result, "Null result");
        final int m = measurer.numberOfDirections();
        final int i2 = (i1 + m / 2) % m;
        final double pr1 = measurer.projectionLength(i1) * pixelSize;
        final double pr2 = measurer.projectionLength(i2) * pixelSize;
        result.addDouble(pr1);
        result.addDouble(second.secondProjectionValue(pr1, pr2));
        final Double thirdProjectionValue = third.thirdProjectionValue(measurer, i1, pixelSize);
        if (thirdProjectionValue != null) {
            result.addDouble(thirdProjectionValue);
        }
    }
}
