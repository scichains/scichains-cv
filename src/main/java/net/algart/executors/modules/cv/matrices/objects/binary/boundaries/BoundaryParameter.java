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

import net.algart.arrays.*;
import net.algart.math.IRectangularArea;
import net.algart.math.RectangularArea;
import net.algart.matrices.scanning.Boundary2DSimpleMeasurer;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

public enum BoundaryParameter {
    AREA(Boundary2DSimpleMeasurer.ObjectParameter.AREA) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return measurer.area() * (pixelSize * pixelSize);
        }
    },

    SQRT_AREA(Boundary2DSimpleMeasurer.ObjectParameter.AREA) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return Math.sqrt(Math.abs(measurer.area())) * pixelSize;
        }
    },

    PERIMETER(Boundary2DSimpleMeasurer.ObjectParameter.PERIMETER) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return measurer.perimeter() * pixelSize;
        }
    },

    SIZE(Boundary2DSimpleMeasurer.ObjectParameter.COORD_RANGES) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return octagonBasedSize(measurer) * pixelSize;
        }
    },

    SHAPE_FACTOR(
            Boundary2DSimpleMeasurer.ObjectParameter.AREA,
            Boundary2DSimpleMeasurer.ObjectParameter.PERIMETER) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return shapeFactorCircularity(measurer.area(), measurer.perimeter());
        }
    },

    CONTAINING_OCTAGON_AREA(Boundary2DSimpleMeasurer.ObjectParameter.COORD_RANGES) {
        // the result will be equal to AREA for really drawn octagons

        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            double x1 = measurer.minX(), x2 = measurer.maxX();
            double y1 = measurer.minY(), y2 = measurer.maxY();
            double xPy1 = measurer.minXPlusY(), xPy2 = measurer.maxXPlusY();
            double xMy1 = measurer.minXMinusY(), xMy2 = measurer.maxXMinusY();
            double rectanglesArea = (x2 - x1) * (y2 - y1);
            double trianglesArea = 0.5 * (x2 + y2 - xPy2) * (x2 + y2 - xPy2 + 1)
                    + 0.5 * (xPy1 - (x1 + y1)) * (xPy1 - (x1 + y1) + 1)
                    + 0.5 * (x2 - y1 - xMy2) * (x2 - y1 - xMy2 + 1)
                    + 0.5 * (xMy1 - (x1 - y2)) * (xMy1 - (x1 - y2) + 1);
            return (rectanglesArea - trianglesArea) * (pixelSize * pixelSize);
        }
    },

    CENTROID(Boundary2DSimpleMeasurer.ObjectParameter.CENTROID) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            throw new UnsupportedOperationException("getStatistics() has no sense for " + this);
        }
    },

    CONTAINING_RECTANGLE(Boundary2DSimpleMeasurer.ObjectParameter.COORD_RANGES) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            throw new UnsupportedOperationException("getStatistics() has no sense for " + this);
        }
    },

    NESTING_LEVEL(
            Boundary2DSimpleMeasurer.ObjectParameter.PERIMETER
            // something simple and quick: not really important
    ) {
        double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize) {
            return measurer.nestingLevel();
        }
    };

    private static final double SQRT05 = 0.5 * Math.sqrt(2.0);
    private final EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> objectParameters;

    BoundaryParameter(Boundary2DSimpleMeasurer.ObjectParameter... objectParameters) {
        this.objectParameters = EnumSet.copyOf(Arrays.asList(objectParameters));
    }

    public EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> objectParameters() {
        return EnumSet.copyOf(objectParameters);
    }

    abstract double getStatistics(Boundary2DSimpleMeasurer measurer, double pixelSize);

    public void getStatistics(
            MutablePNumberArray result,
            Boundary2DSimpleMeasurer measurer,
            double pixelSize) {
        switch (this) {
            case CENTROID: {
                if (result instanceof FloatArray) {
                    ((MutableFloatArray) result).pushFloat((float) (measurer.centroidX() * pixelSize));
                    ((MutableFloatArray) result).pushFloat((float) (measurer.centroidY() * pixelSize));
                } else if (result instanceof DoubleArray) {
                    ((MutableDoubleArray) result).pushDouble(measurer.centroidX() * pixelSize);
                    ((MutableDoubleArray) result).pushDouble(measurer.centroidY() * pixelSize);
                } else {
                    throw new UnsupportedOperationException("Unsupported type of " + result);
                }
                break;
            }
            case CONTAINING_RECTANGLE: {
                double minX = measurer.minX() * pixelSize;
                double minY = measurer.minY() * pixelSize;
                double maxX = measurer.maxX() * pixelSize;
                double maxY = measurer.maxY() * pixelSize;
                pushRectangle(result, minX, minY, maxX, maxY);
                break;
            }
            default: {
                if (result instanceof IntArray) {
                    ((MutableIntArray) result).pushInt((int) getStatistics(measurer, pixelSize));
                } else if (result instanceof FloatArray) {
                    ((MutableFloatArray) result).pushFloat((float) getStatistics(measurer, pixelSize));
                } else if (result instanceof DoubleArray) {
                    ((MutableDoubleArray) result).pushDouble(getStatistics(measurer, pixelSize));
                } else {
                    throw new UnsupportedOperationException("Unsupported type of " + result);
                }
                break;
            }
        }
    }

    public int parameterLength() {
        return switch (this) {
            case CONTAINING_RECTANGLE -> 4;
            case CENTROID -> 2;
            default -> 1;
        };
    }

    public static void pushRectangle(MutablePNumberArray result, double minX, double minY, double maxX, double maxY) {
        if (result instanceof FloatArray) {
            MutableFloatArray r = (MutableFloatArray) result;
            r.pushFloat((float) (0.5 * (minX + maxX)));
            r.pushFloat((float) (0.5 * (minY + maxY)));
            r.pushFloat((float) (maxX - minX));
            r.pushFloat((float) (maxY - minY));
        } else if (result instanceof DoubleArray) {
            MutableDoubleArray r = (MutableDoubleArray) result;
            r.pushDouble(0.5 * (minX + maxX));
            r.pushDouble(0.5 * (minY + maxY));
            r.pushDouble(maxX - minX);
            r.pushDouble(maxY - minY);
        } else {
            throw new UnsupportedOperationException("Unsupported type of " + result);
        }
    }

    public static void pushRectangle(float[] result, int offset, double minX, double minY, double maxX, double maxY) {
        result[offset++] = (float) (0.5 * (minX + maxX));
        result[offset++] = (float) (0.5 * (minY + maxY));
        result[offset++] = (float) (maxX - minX);
        result[offset++] = (float) (maxY - minY);
    }

    public static void pushRectangle(double[] result, int offset, double minX, double minY, double maxX, double maxY) {
        result[offset++] = 0.5 * (minX + maxX);
        result[offset++] = 0.5 * (minY + maxY);
        result[offset++] = maxX - minX;
        result[offset++] = maxY - minY;
    }

    public static void pushRectangle(double[] result, int offset, IRectangularArea r) {
        result[offset++] = 0.5 * ((double) r.minX() + (double) r.maxX());
        result[offset++] = 0.5 * ((double) r.minY() + (double) r.maxY());
        result[offset++] = r.sizeX();
        result[offset++] = r.sizeY();
    }

    public static RectangularArea getRectangle(double[] array, int offset) {
        final double x = array[offset];
        final double y = array[offset + 1];
        final double sizeXHalf = 0.5 * array[offset + 2];
        final double sizeYHalf = 0.5 * array[offset + 3];
        return RectangularArea.valueOf(
                x - sizeXHalf, y - sizeYHalf,
                x + sizeXHalf, y + sizeYHalf);
    }

    public static EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> objectParameters(
            Collection<BoundaryParameter> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        final EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> result =
                EnumSet.noneOf(Boundary2DSimpleMeasurer.ObjectParameter.class);
        for (BoundaryParameter parameter : parameters) {
            result.addAll(parameter.objectParameters);
        }
        return result;
    }

    public static boolean needSecondBuffer(Collection<BoundaryParameter> parameters) {
        Objects.requireNonNull(parameters, "Null parameters");
        return parameters.contains(NESTING_LEVEL);
    }

    public static double shapeFactorCircularity(double area, double perimeter) {
        return Math.sqrt(Math.abs(area) * (4.0 * Math.PI)) / perimeter;
    }

    public static double octagonBasedSize(Boundary2DSimpleMeasurer m) {
        double straightSize = Math.max(m.maxX() - m.minX(), m.maxY() - m.minY());
        double diagonalSize = Math.max(m.maxXPlusY() - m.minXPlusY(), m.maxXMinusY() - m.minXMinusY());
        return Math.max(straightSize, SQRT05 * diagonalSize);
    }
}
