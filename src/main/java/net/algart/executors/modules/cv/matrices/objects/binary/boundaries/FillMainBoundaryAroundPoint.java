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
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.scanning.ContainingMainBoundaryFinder;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;

import java.util.Locale;
import java.util.function.BiConsumer;

public final class FillMainBoundaryAroundPoint extends BitMultiMatrixFilter {
    public static final String OUTPUT_INSIDE = "is_inside";
    public static final String OUTPUT_FOUND_X = "found_x";
    public static final String OUTPUT_FOUND_Y = "found_y";
    public static final String OUTPUT_AREA = "area";
    public static final String OUTPUT_CONTAINING_RECTANGLE = "containing_rectangle";

    public enum Mode {
        FILL_INSIDE(ContainingMainBoundaryFinder::fillBitsInside, "filling") {
            @Override
            IPoint processRectangle(
                    ContainingMainBoundaryFinder finder,
                    Matrix<? extends UpdatableBitArray> result,
                    Matrix<? extends BitArray> source,
                    IRectangularArea rectangle) {
                return finder.fillAtRectangle(result, source, rectangle);
            }
        },
        CLEAR_OUTSIDE(ContainingMainBoundaryFinder::clearBitsOutside, "clearing outside") {
            @Override
            IPoint processRectangle(
                    ContainingMainBoundaryFinder finder,
                    Matrix<? extends UpdatableBitArray> result,
                    Matrix<? extends BitArray> source,
                    IRectangularArea rectangle) {
                return finder.clearOutsideRectangle(result, source, rectangle);
            }
        },
        FILL_IN_NEW_MATRIX(ContainingMainBoundaryFinder::fillBitsInside, "drawing") {
            @Override
            IPoint processRectangle(
                    ContainingMainBoundaryFinder finder,
                    Matrix<? extends UpdatableBitArray> result,
                    Matrix<? extends BitArray> source,
                    IRectangularArea rectangle) {
                return finder.buildAroundRectangle(result, source, rectangle);
            }
        };

        private final BiConsumer<ContainingMainBoundaryFinder, Matrix<? extends UpdatableBitArray>> filler;
        private final String modeName;

        Mode(BiConsumer<ContainingMainBoundaryFinder, Matrix<? extends UpdatableBitArray>> filler, String modeName) {
            this.filler = filler;
            this.modeName = modeName;
        }

        public String modeName() {
            return modeName;
        }

        abstract IPoint processRectangle(
                ContainingMainBoundaryFinder finder,
                Matrix<? extends UpdatableBitArray> result,
                Matrix<? extends BitArray> source,
                IRectangularArea rectangle);

        private boolean fillInSource() {
            return this != FILL_IN_NEW_MATRIX;
        }
    }

    private long x = 0;
    private long y = 0;
    private Mode mode = Mode.CLEAR_OUTSIDE;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;

    public FillMainBoundaryAroundPoint() {
        addOutputScalar(OUTPUT_INSIDE);
        addOutputScalar(OUTPUT_FOUND_X);
        addOutputScalar(OUTPUT_FOUND_Y);
        addOutputScalar(OUTPUT_AREA);
        addOutputNumbers(OUTPUT_CONTAINING_RECTANGLE);
    }

    public long getX() {
        return x;
    }

    public FillMainBoundaryAroundPoint setX(long x) {
        this.x = x;
        return this;
    }

    public long getY() {
        return y;
    }

    public FillMainBoundaryAroundPoint setY(long y) {
        this.y = y;
        return this;
    }

    public Mode getMode() {
        return mode;
    }

    public FillMainBoundaryAroundPoint setMode(Mode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public FillMainBoundaryAroundPoint setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> objects) {
        long t1 = debugTime();
        final Matrix<BitArray> source = objects.cast(BitArray.class);
        final ContainingMainBoundaryFinder finder = ContainingMainBoundaryFinder.newInstance(source)
                .setConnectivityType(connectivityType);
        final Matrix<UpdatableBitArray> result = Arrays.SMM.newBitMatrix(objects.dimensions());
        if (mode.fillInSource()) {
            Matrices.copy(result, source);
        }
        long t2 = debugTime();
        final long found = finder.find(x, y);
        long t3 = debugTime();
        mode.filler.accept(finder, result);
        long t4 = debugTime();
        if (isOutputNecessary(OUTPUT_CONTAINING_RECTANGLE)) {
            final IRectangularArea rectangle = finder.findContainingRectangle();
            if (rectangle != null) {
                getNumbers(OUTPUT_CONTAINING_RECTANGLE).setTo(rectangle);
            } else {
                getNumbers(OUTPUT_CONTAINING_RECTANGLE).remove();
            }
        }
        long t5 = debugTime();
        getScalar(OUTPUT_INSIDE).setTo(found >= 0);
        if (found >= 0) {
            getScalar(OUTPUT_FOUND_X).setTo(found);
            getScalar(OUTPUT_FOUND_Y).setTo(y);
            getScalar(OUTPUT_AREA).setTo(finder.scanner().orientedArea());
        } else {
            getScalar(OUTPUT_FOUND_X).remove();
            getScalar(OUTPUT_FOUND_Y).remove();
            getScalar(OUTPUT_AREA).remove();
        }
        logDebug(() -> String.format(Locale.US,
                "%s main boundary in matrix %dx%d around point (%d,%d) in %.3f ms: "
                        + "%.6f ms allocating%s + %.6f ms finding + %.6f ms %s + %.6f finding rectangle",
                found > 0 ? "Found at " + found : "Not found",
                source.dimX(), source.dimY(), x, y,
                (t5 - t1) * 1e-6,
                (t2 - t1) * 1e-6, mode.fillInSource() ? "" : "/copying",
                (t3 - t2) * 1e-6,
                (t4 - t3) * 1e-6, mode.modeName(),
                (t5 - t4) * 1e-6));
        return result;
    }
}
