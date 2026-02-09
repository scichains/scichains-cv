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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannel2DFilter;
import net.algart.executors.modules.cv.matrices.objects.binary.boundaries.BinaryFilterParticlesOrPoresBySizes;
import net.algart.math.Range;
import net.algart.math.functions.Func;
import net.algart.matrices.scanning.ConnectivityType;

import java.util.HashMap;
import java.util.Map;

public final class FilterParticlesOrPoresBySizes extends MultiMatrixChannel2DFilter {
    public enum Mode {
        REMOVE,
        FIND
    }

    private double pixelSize = 1.0;
    private ConnectivityType connectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private BinaryFilterParticlesOrPoresBySizes.ParticlesOrPores particlesOrPores =
            BinaryFilterParticlesOrPoresBySizes.ParticlesOrPores.PORES;
    private Mode mode = Mode.REMOVE;
    private double maxSize = Double.POSITIVE_INFINITY;
    private double maxArea = Double.POSITIVE_INFINITY;
    private double maxPerimeter = Double.POSITIVE_INFINITY;
    private BinaryFilterParticlesOrPoresBySizes.ConditionLogic conditionLogic =
            BinaryFilterParticlesOrPoresBySizes.ConditionLogic.AND;
    private boolean ignoreZeros = false;
    private int numberOfSlices = 256;

    public double getPixelSize() {
        return pixelSize;
    }

    public FilterParticlesOrPoresBySizes setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
        return this;
    }

    public ConnectivityType getConnectivityType() {
        return connectivityType;
    }

    public ConnectivityType poresConnectivityType() {
        return BinaryFilterParticlesOrPoresBySizes.poresConnectivityType(this.connectivityType);
    }

    public FilterParticlesOrPoresBySizes setConnectivityType(ConnectivityType connectivityType) {
        this.connectivityType = nonNull(connectivityType);
        return this;
    }

    public BinaryFilterParticlesOrPoresBySizes.ParticlesOrPores getParticlesOrPores() {
        return particlesOrPores;
    }

    public FilterParticlesOrPoresBySizes setParticlesOrPores(
            BinaryFilterParticlesOrPoresBySizes.ParticlesOrPores particlesOrPores) {
        this.particlesOrPores = nonNull(particlesOrPores);
        return this;
    }

    public Mode getMode() {
        return mode;
    }

    public FilterParticlesOrPoresBySizes setMode(Mode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public double getMaxSize() {
        return maxSize;
    }

    public FilterParticlesOrPoresBySizes setMaxSize(double maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public FilterParticlesOrPoresBySizes setMaxSize(String maxSize) {
        return setMaxSize(doubleOrPositiveInfinity(maxSize));
    }

    public double getMaxArea() {
        return maxArea;
    }

    public FilterParticlesOrPoresBySizes setMaxArea(double maxArea) {
        this.maxArea = maxArea;
        return this;
    }

    public FilterParticlesOrPoresBySizes setMaxArea(String maxArea) {
        return setMaxArea(doubleOrPositiveInfinity(maxArea));
    }

    public double getMaxPerimeter() {
        return maxPerimeter;
    }

    public FilterParticlesOrPoresBySizes setMaxPerimeter(double maxPerimeter) {
        this.maxPerimeter = maxPerimeter;
        return this;
    }

    public FilterParticlesOrPoresBySizes setMaxPerimeter(String maxPerimeter) {
        return setMaxPerimeter(doubleOrPositiveInfinity(maxPerimeter));
    }

    public BinaryFilterParticlesOrPoresBySizes.ConditionLogic getConditionLogic() {
        return conditionLogic;
    }

    public FilterParticlesOrPoresBySizes setConditionLogic(
            BinaryFilterParticlesOrPoresBySizes.ConditionLogic conditionLogic) {
        this.conditionLogic = nonNull(conditionLogic);
        return this;
    }

    public boolean isIgnoreZeros() {
        return ignoreZeros;
    }

    public FilterParticlesOrPoresBySizes setIgnoreZeros(boolean ignoreZeros) {
        this.ignoreZeros = ignoreZeros;
        return this;
    }

    public int getNumberOfSlices() {
        return numberOfSlices;
    }

    public void setNumberOfSlices(int numberOfSlices) {
        this.numberOfSlices = positive(numberOfSlices);
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> matrix) {
        return filter(matrix);
    }

    public Matrix<? extends UpdatablePArray> filter(Matrix<? extends PArray> objects) {
        final boolean pores = particlesOrPores == BinaryFilterParticlesOrPoresBySizes.ParticlesOrPores.PORES;
        Range range = Arrays.rangeOf(objects.array());
        if (pores) {
            objects = BinaryFilterParticlesOrPoresBySizes.poresToParticlesWithExtending(objects, range.min());
            range = BinaryFilterParticlesOrPoresBySizes.poresToParticlesRange(objects, range);
        }
        final long[] dimensions = objects.dimensions();
        final Matrix<? extends UpdatablePArray> result = Arrays.SMM.newMatrix(UpdatablePArray.class, objects);
        int numberOfSlices = this.numberOfSlices;
        if (objects.array() instanceof PFixedArray) {
            numberOfSlices = Math.min(numberOfSlices, (int) (range.size() + 1.0));
        }
        @SuppressWarnings("resource") final BinaryFilterParticlesOrPoresBySizes binaryFilter =
                new BinaryFilterParticlesOrPoresBySizes()
                        .setPixelSize(pixelSize)
                        .setConnectivityType(connectivityType)
                        .setParticlesOrPores(particlesOrPores)
                        .setMode(BinaryFilterParticlesOrPoresBySizes.Mode.REMOVE)
                        .setMaxSize(maxSize)
                        .setMaxArea(maxArea)
                        .setMaxPerimeter(maxPerimeter)
                        .setConditionLogic(conditionLogic)
                        .setIgnoreZeros(ignoreZeros);
        // Note that GeneralizedBitProcessing works correctly only if the result bits always decrease (from 1 to 0)
        // while increasing the threshold. This is correct for REMOVE mode but may be incorrect for other modes.
        final Map<Integer, Matrix<? extends UpdatableBitArray>> workMemoryPool = new HashMap<>();
        final GeneralizedBitProcessing.SliceOperation sliceOperation = new GeneralizedBitProcessing.SliceOperation() {
            @Override
            public void processBits(
                    ArrayContext context,
                    UpdatableBitArray destBits,
                    BitArray srcBits,
                    long sliceIndex,
                    int threadIndex,
                    int numberOfThreads) {
                final Matrix<? extends UpdatableBitArray> result = Matrices.matrix(destBits, dimensions);
                final Matrix<? extends BitArray> source = Matrices.matrix(srcBits, dimensions);
                final Matrix<? extends UpdatableBitArray> workMemory;
                synchronized (workMemoryPool) {
                    workMemory = workMemoryPool.computeIfAbsent(threadIndex, k -> Arrays.SMM.newBitMatrix(dimensions));
                }
                binaryFilter.filterParticles(result, source, workMemory,
                        pores ? poresConnectivityType() : getConnectivityType(),
                        pores);
                // - to interpret pores as particles, we need to invert connectivity
            }

            @Override
            public boolean isInPlaceProcessingAllowed() {
                return false;
            }
        };
        final GeneralizedBitProcessing generalizedBitProcessing =
                GeneralizedBitProcessing.getInstance(
                        null,
                        sliceOperation,
                        GeneralizedBitProcessing.RoundingMode.ROUND_UP);
        // - ROUND_UP better precision while removing small objects
        generalizedBitProcessing.process(
                result.array(),
                objects.array(),
                range,
                numberOfSlices);
        if (objects.elementType() != boolean.class && mode == Mode.REMOVE) {
            Matrices.applyFunc(null, Func.MIN, result, result, objects);
            // increasing precision at the places where we didn't remove anything
        }
        if (mode == Mode.FIND) {
            Matrices.applyFunc(null, Func.X_MINUS_Y, result, objects, result);
        }
        if (pores) {
            return BinaryFilterParticlesOrPoresBySizes.particlesToPoresWithReducing(
                    result, mode == Mode.REMOVE);
        } else {
            return result;
        }
    }
}
