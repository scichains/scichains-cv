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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.*;
import net.algart.matrices.scanning.Boundary2DProjectionMeasurer;

import java.util.Collection;
import java.util.Objects;

public enum ProjectionParameter {
    AREA() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.area() * (pixelSize * pixelSize);
        }
    },
    PERIMETER() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.perimeter() * pixelSize;
        }
    },
    ALL_PROJECTIONS() {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            if (result instanceof FloatArray) {
                final MutableFloatArray a = (MutableFloatArray) result;
                for (int k = 0, m = measurer.numberOfDirections(); k < m; k++) {
                    a.pushFloat((float) (measurer.projectionLength(k) * pixelSize));
                }
            } else if (result instanceof DoubleArray) {
                final MutableDoubleArray a = (MutableDoubleArray) result;
                for (int k = 0, m = measurer.numberOfDirections(); k < m; k++) {
                    a.pushDouble(measurer.projectionLength(k) * pixelSize);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported type of " + result);
            }
        }
    },
    SELECTED_PROJECTION() {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            pushProjectionPair(result, measurer, 0, pixelSize, second, third);
        }
    },
    MEAN_PROJECTION() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.meanProjectionLength() * pixelSize;
        }
    },
    MAX_PROJECTION() {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            pushProjectionPair(result, measurer, measurer.indexOfMaxProjectionLength(), pixelSize, second, third);
        }
    },
    MIN_PROJECTION() {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            pushProjectionPair(result, measurer, measurer.indexOfMinProjectionLength(), pixelSize, second, third);
        }
    },
    MAX_SIZE_RELATION() {
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
            pushProjectionPair(result, measurer, index, pixelSize, second, third);
        }
    },
    MIN_CIRCUMSCRIBED_SQUARE_SIDE() {
        @Override
        public void getStatistics(
                MutablePNumberArray result,
                Boundary2DProjectionMeasurer measurer,
                double pixelSize,
                SecondProjectionValue second,
                ThirdProjectionValue third) {
            pushProjectionPair(
                    result,
                    measurer,
                    measurer.indexOfMinCircumscribedRhombus(measurer.numberOfDirections() / 2),
                    pixelSize,
                    second,
                    third);
        }
    },
    SHAPE_FACTOR() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return BoundaryParameter.shapeFactorCircularity(measurer.area(), measurer.perimeter());
        }
    },
    COMPACT_FACTOR() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            final double meanProjection = measurer.meanProjectionLength();
            final double meanCircleArea = 0.25 * Math.PI * meanProjection * meanProjection;
            return measurer.area() / meanCircleArea;
        }
    },
    IRREGULARITY_FACTOR() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            final double meanCircleLength = Math.PI * measurer.meanProjectionLength();
            return meanCircleLength / measurer.perimeter();
        }
    },
    NESTING_LEVEL() {
        @Override
        double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
            return measurer.nestingLevel();
        }
    };

    double getStatistics(Boundary2DProjectionMeasurer measurer, double pixelSize) {
        throw new UnsupportedOperationException();
    }

    public void getStatistics(
            MutablePNumberArray result,
            Boundary2DProjectionMeasurer measurer,
            double pixelSize,
            SecondProjectionValue second,
            ThirdProjectionValue third)
    {
        if (result instanceof IntArray) {
            ((MutableIntArray) result).pushInt((int) getStatistics(measurer, pixelSize));
        } else if (result instanceof FloatArray) {
            ((MutableFloatArray) result).pushFloat((float) getStatistics(measurer, pixelSize));
        } else if (result instanceof DoubleArray) {
            ((MutableDoubleArray) result).pushDouble(getStatistics(measurer, pixelSize));
        } else {
            throw new UnsupportedOperationException("Unsupported type of " + result);
        }
    }

    public static boolean needSecondBuffer(Collection<ProjectionParameter> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        return parameters.contains(NESTING_LEVEL);
    }

    private static void pushProjectionPair(
            MutablePNumberArray result,
            Boundary2DProjectionMeasurer measurer,
            int i1,
            double pixelSize,
            SecondProjectionValue second,
            ThirdProjectionValue third) {
        final int m = measurer.numberOfDirections();
        final int i2 = (i1 + m / 2) % m;
        final double pr1 = measurer.projectionLength(i1) * pixelSize;
        final double pr2 = measurer.projectionLength(i2) * pixelSize;
        if (result instanceof FloatArray) {
            final MutableFloatArray a = (MutableFloatArray) result;
            a.pushFloat((float) pr1);
            a.pushFloat((float) second.secondProjectionValue(pr1, pr2));
            final Double thirdProjectionValue = third.thirdProjectionValue(measurer, i1, pixelSize);
            if (thirdProjectionValue != null) {
                a.pushFloat((float) thirdProjectionValue.doubleValue());
            }
        } else if (result instanceof DoubleArray) {
            final MutableDoubleArray a = (MutableDoubleArray) result;
            a.pushDouble(pr1);
            a.pushDouble(second.secondProjectionValue(pr1, pr2));
            final Double thirdProjectionValue = third.thirdProjectionValue(measurer, i1, pixelSize);
            if (thirdProjectionValue != null) {
                a.pushDouble(thirdProjectionValue);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported type of " + result);
        }
    }
}
