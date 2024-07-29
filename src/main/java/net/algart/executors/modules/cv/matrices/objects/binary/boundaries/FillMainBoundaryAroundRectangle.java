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
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.scanning.ContainingMainBoundaryFinder;

import java.util.Locale;

public final class FillMainBoundaryAroundRectangle extends BitMultiMatrixFilter {
    public static final String OUTPUT_FOUND_X = "found_x";
    public static final String OUTPUT_FOUND_Y = "found_y";
    public static final String OUTPUT_AREA = "area";

    private long startX = 0;
    private long startY = 0;
    private long sizeX = 1;
    private long sizeY = 1;
    private FillMainBoundaryAroundPoint.Mode mode = FillMainBoundaryAroundPoint.Mode.CLEAR_OUTSIDE;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;

    public FillMainBoundaryAroundRectangle() {
        addOutputScalar(OUTPUT_FOUND_X);
        addOutputScalar(OUTPUT_FOUND_Y);
        addOutputScalar(OUTPUT_AREA);
    }

    public long getStartX() {
        return startX;
    }

    public FillMainBoundaryAroundRectangle setStartX(long startX) {
        this.startX = startX;
        return this;
    }

    public long getStartY() {
        return startY;
    }

    public FillMainBoundaryAroundRectangle setStartY(long startY) {
        this.startY = startY;
        return this;
    }

    public long getSizeX() {
        return sizeX;
    }

    public FillMainBoundaryAroundRectangle setSizeX(long sizeX) {
        this.sizeX = nonNegative(sizeX);
        return this;
    }

    public long getSizeY() {
        return sizeY;
    }

    public FillMainBoundaryAroundRectangle setSizeY(long sizeY) {
        this.sizeY = nonNegative(sizeY);
        return this;
    }

    public FillMainBoundaryAroundPoint.Mode getMode() {
        return mode;
    }

    public FillMainBoundaryAroundRectangle setMode(FillMainBoundaryAroundPoint.Mode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public FillMainBoundaryAroundRectangle setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> objects) {
        if (sizeX <= 0 || sizeY <= 0) {
            return new FillMainBoundaryAroundPoint()
                    .setX(startX)
                    .setY(startY)
                    .setMode(mode)
                    .setConnectivityType(connectivityType)
                    .processMatrix(objects);
        }
        long t1 = debugTime();
        final Matrix<BitArray> source = objects.cast(BitArray.class);
        final ContainingMainBoundaryFinder finder = ContainingMainBoundaryFinder
                .newInstance(source.dimensions())
                .setConnectivityType(connectivityType);
        final Matrix<UpdatableBitArray> result = Arrays.SMM.newBitMatrix(objects.dimensions());
        long t2 = debugTime();
        final IRectangularArea rectangle = IRectangularArea.valueOf(
                startX, startY, Math.addExact(startX, sizeX - 1), Math.addExact(startY, sizeY - 1));
        final IPoint p = mode.processRectangle(finder, result, source, rectangle);
        long t3 = debugTime();
        getScalar(OUTPUT_FOUND_X).setTo(p.x());
        getScalar(OUTPUT_FOUND_Y).setTo(p.y());
        getScalar(OUTPUT_AREA).setTo(finder.scanner().orientedArea());
        logDebug(() -> String.format(Locale.US,
                "Finding main boundary in matrix %dx%d around rectangle %s in %.3f ms: "
                        + "%.6f ms allocating + %.6f ms %s",
                source.dimX(), source.dimY(), rectangle,
                (t3 - t1) * 1e-6,
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6, mode.modeName()));
        return result;
    }
}
