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
import net.algart.math.Range;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.matrices.scanning.Boundary2DSimpleMeasurer;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.matrices.scanning.ContourLineType;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;

import java.util.EnumSet;
import java.util.Objects;

public final class BinaryFilterParticlesOrPoresBySizes extends BitMultiMatrixFilter {
    public enum ParticlesOrPores {
        PARTICLES,
        PORES;
    }

    public enum Mode {
        REMOVE,
        FIND,
        FIND_FILLED
    }

    private double pixelSize = 1.0;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private ParticlesOrPores particlesOrPores = ParticlesOrPores.PORES;
    private Mode mode = Mode.REMOVE;
    private double maxSize = Double.POSITIVE_INFINITY;
    private double maxArea = Double.POSITIVE_INFINITY;
    private double maxPerimeter = Double.POSITIVE_INFINITY;
    private boolean ignoreZeros = false;

    public double getPixelSize() {
        return pixelSize;
    }

    public BinaryFilterParticlesOrPoresBySizes setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public ConnectivityType poresConnectivityType() {
        return poresConnectivityType(this.connectivityType);
    }

    public BinaryFilterParticlesOrPoresBySizes setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    public ParticlesOrPores getParticlesOrPores() {
        return particlesOrPores;
    }

    public BinaryFilterParticlesOrPoresBySizes setParticlesOrPores(ParticlesOrPores particlesOrPores) {
        this.particlesOrPores = nonNull(particlesOrPores);
        return this;
    }

    public Mode getMode() {
        return mode;
    }

    public BinaryFilterParticlesOrPoresBySizes setMode(Mode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public double getMaxSize() {
        return maxSize;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxSize(double maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxSize(String maxSize) {
        return setMaxSize(doubleOrPositiveInfinity(maxSize));
    }

    public double getMaxArea() {
        return maxArea;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxArea(double maxArea) {
        this.maxArea = maxArea;
        return this;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxArea(String maxArea) {
        return setMaxArea(doubleOrPositiveInfinity(maxArea));
    }

    public double getMaxPerimeter() {
        return maxPerimeter;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxPerimeter(double maxPerimeter) {
        this.maxPerimeter = maxPerimeter;
        return this;
    }

    public BinaryFilterParticlesOrPoresBySizes setMaxPerimeter(String maxPerimeter) {
        return setMaxPerimeter(doubleOrPositiveInfinity(maxPerimeter));
    }

    public boolean isIgnoreZeros() {
        return ignoreZeros;
    }

    public BinaryFilterParticlesOrPoresBySizes setIgnoreZeros(boolean ignoreZeros) {
        this.ignoreZeros = ignoreZeros;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> objects) {
        logDebug(() -> "Filtering " + particlesOrPores + " at " + objects);
        return filter(objects.cast(BitArray.class));
    }

    public Matrix<? extends UpdatableBitArray> filter(Matrix<? extends BitArray> objects) {
        final boolean pores = particlesOrPores == ParticlesOrPores.PORES;
        if (pores) {
            objects = poresToParticlesWithExtending(objects, 0).cast(BitArray.class);
        }
        final Matrix<? extends UpdatableBitArray> result = Arrays.SMM.newBitMatrix(objects.dimensions());
        filterParticles(result, objects, null,
                pores ? poresConnectivityType() : getConnectivityType(),
                pores);
        // - to interpret pores as particles, we need to invert connectivity
        if (pores) {
            return asBit(particlesToPoresWithReducing(result, mode == Mode.REMOVE));
        } else {
            return result;
        }
    }

    public void filterParticles(
            Matrix<? extends UpdatableBitArray> result,
            Matrix<? extends BitArray> source,
            Matrix<? extends UpdatableBitArray> workMemory,
            ConnectivityType connectivityType,
            boolean skipFirstParticle) {
        Objects.requireNonNull(result, "Null result matrix");
        Objects.requireNonNull(result, "Null source matrix");
        Matrices.checkDimensionEquality(result, source);
        if (workMemory == null) {
            workMemory = Arrays.SMM.newBitMatrix(source.dimensions());
        }
        final UpdatableBitArray resultArray = result.array();
        workMemory.array().fill(false);
        resultArray.fill(false);
        final Boundary2DScanner allScanner =
                Boundary2DScanner.getAllBoundariesScanner(source, workMemory, workMemory, connectivityType);
        final Boundary2DSimpleMeasurer measurer = Boundary2DSimpleMeasurer.getInstance(
                allScanner,
                ContourLineType.SEGMENT_CENTERS_POLYLINE,
                necessaryParameters());
        // SEGMENT_CENTERS_POLYLINE is necessary for correct perimeter calculation
        final Boundary2DScanner mainScanner = Boundary2DScanner.getMainBoundariesScanner(source, result, connectivityType);
        final boolean checkArea = isLimitActual(maxArea);
        final boolean checkSize = isLimitActual(maxSize);
        final boolean checkPerimeter = isLimitActual(maxPerimeter);
        while (measurer.nextBoundary()) {
            assert measurer.get();
            measurer.scanBoundary(null);
            if (skipFirstParticle) {
                skipFirstParticle = false;
                continue;
            }
            if (measurer.isInternalBoundary()) {
                continue;
            }
            if (checkArea && Math.abs(measurer.orientedArea()) * pixelSize * pixelSize > maxArea) {
                continue;
            }
            if (checkSize && BoundaryParameter.octagonBasedSize(measurer) * pixelSize > maxSize) {
                continue;
            }
            if (checkPerimeter && measurer.perimeter() * pixelSize > maxPerimeter) {
                continue;
            }
            final long currentIndex = measurer.currentIndexInArray();
            while (!mainScanner.isInitialized() || mainScanner.currentIndexInArray() < currentIndex) {
                mainScanner.nextBoundary();
            }
            if (mainScanner.currentIndexInArray() == currentIndex) {
                mainScanner.scanBoundary(null); // scanBoundary adds brackets to filler
            }
        }
        //noinspection StatementWithEmptyBody
        while (mainScanner.nextBoundary()) ;
        // - fills last objects, brackets for which are drawn by scanBoundary above
        switch (mode) {
            case FIND -> {
                Matrices.applyFunc(null, Func.MIN, result, source, result);
                // - restore pores on the found particles
            }
            case REMOVE -> {
                Matrices.applyFunc(null, Func.POSITIVE_DIFF, result, source, result);
                // - remove found particles
            }
            case FIND_FILLED -> {
                // - nothing to do
            }
        }
    }

    public static ConnectivityType poresConnectivityType(ConnectivityType connectivityType) {
        return connectivityType == ConnectivityType.STRAIGHT_AND_DIAGONAL ?
                ConnectivityType.STRAIGHT_ONLY :
                ConnectivityType.STRAIGHT_AND_DIAGONAL;
    }

    public static Matrix<? extends PArray> poresToParticlesWithExtending(
            Matrix<? extends PArray> particles,
            double extendingFiller) {
        final Matrix<UpdatablePArray> pores = Arrays.SMM.newMatrix(
                UpdatablePArray.class, particles.elementType(), particles.dimX() + 2, particles.dimY() + 2);
        final Matrix.ContinuationMode continuationMode = Matrix.ContinuationMode.getConstantMode(extendingFiller);
        switchParticlesAndPores(
                pores,
                particles.subMatr(-1, -1, pores.dimX(), pores.dimY(), continuationMode));
        return pores;
    }

    public static Matrix<? extends UpdatablePArray> particlesToPoresWithReducing(
            Matrix<? extends UpdatablePArray> result,
            boolean invertResult) {
        if (invertResult) {
            switchParticlesAndPores(result, result);
        }
        return result.subMatr(1, 1, result.dimX() - 2, result.dimY() - 2);
    }

    public static Range poresToParticlesRange(Matrix<? extends PArray> particles, Range range) {
        final double maxPossibleValue = particles.array().maxPossibleValue(1.0);
        return Range.valueOf(maxPossibleValue - range.max(), maxPossibleValue - range.min());
    }

    private static void switchParticlesAndPores(
            Matrix<? extends UpdatablePArray> result,
            Matrix<? extends PArray> source) {
        final double maxPossibleValue = source.array().maxPossibleValue(1.0);
        Matrices.applyFunc(null, LinearFunc.getInstance(maxPossibleValue, -1.0), result, source);
    }

    private EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> necessaryParameters() {
        EnumSet<Boundary2DSimpleMeasurer.ObjectParameter> result =
                EnumSet.noneOf(Boundary2DSimpleMeasurer.ObjectParameter.class);
        if (maxSize != Double.POSITIVE_INFINITY) {
            result.add(Boundary2DSimpleMeasurer.ObjectParameter.COORD_RANGES);
        }
        if (maxArea != Double.POSITIVE_INFINITY) {
            result.add(Boundary2DSimpleMeasurer.ObjectParameter.AREA);
        }
        if (maxPerimeter != Double.POSITIVE_INFINITY) {
            result.add(Boundary2DSimpleMeasurer.ObjectParameter.PERIMETER);
        }
        return result;
    }

    private boolean isLimitActual(double value) {
        return value != Double.POSITIVE_INFINITY && (!ignoreZeros || value != 0.0);
    }
}
