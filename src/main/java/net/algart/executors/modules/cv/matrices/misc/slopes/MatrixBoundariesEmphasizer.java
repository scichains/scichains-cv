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

package net.algart.executors.modules.cv.matrices.misc.slopes;

import net.algart.arrays.*;

import java.util.Objects;
import java.util.stream.IntStream;

public final class MatrixBoundariesEmphasizer {
    private static final boolean OPTIMIZE_DIRECT_ACCESSIBLE = true;
    // - should be true for good performance

    private final SlopeEmphasizer slopeEmphasizer;
    private int directionToEmphasize = 0;

    private MatrixBoundariesEmphasizer(SlopeEmphasizer slopeEmphasizer) {
        this.slopeEmphasizer = Objects.requireNonNull(slopeEmphasizer, "Null slopeEmphasizer");
    }

    public static MatrixBoundariesEmphasizer getInstance() {
        return getInstance(SlopeEmphasizer.getInstance());
    }

    public static MatrixBoundariesEmphasizer getInstance(SlopeEmphasizer slopeEmphasizer) {
        return new MatrixBoundariesEmphasizer(slopeEmphasizer);
    }

    public SlopeEmphasizer getSlopeEmphasizer() {
        return slopeEmphasizer;
    }

    public int getDirectionToEmphasize() {
        return directionToEmphasize;
    }

    public MatrixBoundariesEmphasizer setDirectionToEmphasize(int directionToEmphasize) {
        if (directionToEmphasize < 0) {
            throw new IllegalArgumentException("Negative directionToEmphasize = " + directionToEmphasize);
        }
        this.directionToEmphasize = directionToEmphasize;
        return this;
    }

    public void emphasize(Matrix<? extends UpdatablePArray> matrix) {
        Objects.requireNonNull(matrix, "Null matrix");
        final SlopeEmphasizer.ForType emphasizer = slopeEmphasizer.forElementType(matrix.elementType());
        final int dimCount = matrix.dimCount();
        if (dimCount > 2) {
            throw new IllegalArgumentException("Matrices with more than 2 dimensions are not supported");
        }
        if (directionToEmphasize >= dimCount) {
            throw new IllegalArgumentException("Direction to emphasize " + directionToEmphasize
                    + " is out of range 0..dimCount-1 = 0.." + (dimCount - 1));
        }
        if (tryToEmphasizeDirectAccessible(matrix, emphasizer)) {
            return;
        }
        final UpdatablePArray array = matrix.array();
        final long dimDirection = matrix.dim(directionToEmphasize);
        if (dimDirection >= Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large matrix dimension #" + directionToEmphasize + ": "
                    + dimDirection);
        }
        final int length = (int) dimDirection;
        final Object valuesArray = array.newJavaArray((int) dimDirection);
        // Slow, but stable solution:
        switch (directionToEmphasize) {
            case 0: {
                for (long y = 0, dimY = matrix.dimY(), p = 0; y < dimY; y++, p += length) {
                    // - note that it will work also for 1-dimensional matrix: dimY() will be 1
                    array.getData(p, valuesArray);
                    emphasizer.emphasize(valuesArray, 0, length);
                    array.setData(p, valuesArray);
                }
                break;
            }
            case 1: {
                for (long x = 0, dimX = matrix.dimX(); x < dimX; x++) {
                    final UpdatablePArray column = matrix.subMatr(x, 0, 1, dimDirection).array();
                    column.getData(0, valuesArray);
                    emphasizer.emphasize(valuesArray, 0, length);
                    column.setData(0, valuesArray);
                }
                break;
            }
            default: {
                throw new AssertionError("Impossible " + directionToEmphasize);
            }
        }
    }

    private boolean tryToEmphasizeDirectAccessible(
            Matrix<? extends UpdatablePArray> matrix,
            final SlopeEmphasizer.ForType emphasizer) {
        final PArray array = matrix.array();
        final DirectAccessible da;
        if (OPTIMIZE_DIRECT_ACCESSIBLE
                && array instanceof DirectAccessible
                && (da = (DirectAccessible) array).hasJavaArray()) {
            // - quick solution for direct accessible matrices
            final int startOffset = da.javaArrayOffset();
            final Object values = da.javaArray();
            final int dimX = (int) matrix.dimX();
            final int dimY = (int) matrix.dimY();
            switch (directionToEmphasize) {
                case 0: {
                    IntStream.range(0, dimY).parallel().forEach(y -> {
                        // - note that it will work also for 1-dimensional matrix: dimY() will be 1
                        emphasizer.emphasize(values, startOffset + y * dimX, dimX);
                    });
                    break;
                }
                case 1: {
                    IntStream.range(0, dimX).parallel().forEach(x -> {
                        emphasizer.emphasize(values, startOffset + x, dimY, dimX);
                    });
                    break;
                }
                default: {
                    throw new AssertionError("Impossible " + directionToEmphasize);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
