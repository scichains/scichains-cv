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

package net.algart.executors.modules.cv.matrices.misc.extremums;

import net.algart.executors.modules.cv.matrices.misc.SortedRound2DAperture;
import net.algart.arrays.*;

import java.util.Objects;

public abstract class ExtremumsFinder {
    public static class DeepTestSettings {
        public enum Mode {
            PERCENTILE,
            MEAN
        }

        private SortedRound2DAperture depthAperture = null;
        private Mode mode = Mode.PERCENTILE;
        private double percentileLevel = 1.0;
        private double minimalDepth = 0.0;
        private Matrix<? extends BitArray> ignore = null;

        public DeepTestSettings setDepthAperture(SortedRound2DAperture depthAperture) {
            this.depthAperture = depthAperture;
            return this;
        }

        public DeepTestSettings setMode(Mode mode) {
            this.mode = Objects.requireNonNull(mode, "Null deep-testing mode");
            return this;
        }

        public DeepTestSettings setPercentileLevel(double percentileLevel) {
            if (percentileLevel < 0.0 || percentileLevel > 1.0) {
                throw new IllegalArgumentException("Illegal percentile level = " + percentileLevel
                        + " (must be in range 0..1)");
            }
            this.percentileLevel = percentileLevel;
            return this;
        }

        public DeepTestSettings setMinimalDepth(double minimalDepth) {
            this.minimalDepth = minimalDepth;
            return this;
        }

        public DeepTestSettings setIgnore(Matrix<? extends BitArray> ignore) {
            this.ignore = ignore;
            return this;
        }
    }

    private final boolean searchingForMaximums;
    final int dimX;
    final int dimY;
    final float[] values;
    final boolean[] mask;
    final SortedRound2DAperture aperture;
    final int apertureCount;
    final int apertureCountWithoutLine;
    final int[] apertureOffsets;
    private final SortedRound2DAperture depthAperture;
    private final double percentileLevel;
    private final double minimalDepth;
    private final boolean needOppositeExtremum;
    private final BitArray ignore;
    private final Matrix<UpdatableBitArray> extremums;
    private final boolean buildListOfExtremumsXY;

    private final MutableIntArray extremumsXY = Arrays.SMM.newEmptyIntArray();
    final int maxRadius;
    private final int minOptimizedY;
    private final int maxOptimizedY;
    private final int minOptimizedYHorizontal;
    private final int maxOptimizedYHorizontal;
    final int[] neighbourOffsets;
    private final OppositeExtremumFinder oppositeExtremumFinder;
    int neighboursCount;
    int y;
    int valuesStart;

    ExtremumsFinder(
            float[] values,
            boolean[] mask,
            SortedRound2DAperture aperture,
            DeepTestSettings deepTestSettings,
            Matrix<UpdatableBitArray> resultExtremums,
            boolean buildListOfExtremumsXY,
            boolean searchingForMaximums) {
        Objects.requireNonNull(deepTestSettings, "Null deep-test settings");
        this.searchingForMaximums = searchingForMaximums;
        this.dimX = (int) resultExtremums.dimX();
        this.dimY = (int) resultExtremums.dimY();
        Objects.requireNonNull(values, "Null values");
        if (values.length != resultExtremums.size()) {
            throw new IllegalArgumentException("values array length " + values.length
                    + " does not match result extremums matrix sizes " + resultExtremums);
        }
        this.values = values;
        if (mask != null && mask.length != resultExtremums.size()) {
            throw new IllegalArgumentException("mask array length " + mask.length
                    + " does not match result extremums matrix sizes " + resultExtremums);
        }
        this.mask = mask;
        this.aperture = Objects.requireNonNull(aperture, "Null aperture");
        this.apertureCount = aperture.count();
        this.apertureCountWithoutLine = aperture.countWithoutLine();
        this.apertureOffsets = aperture.offsets();
        this.depthAperture = deepTestSettings.depthAperture;
        this.extremums = Objects.requireNonNull(resultExtremums, "Null resulting extremums matrix");
        this.percentileLevel = deepTestSettings.percentileLevel;
        this.minimalDepth = deepTestSettings.minimalDepth;
        this.needOppositeExtremum = minimalDepth > 0.0;
        if (deepTestSettings.ignore != null && !resultExtremums.dimEquals(deepTestSettings.ignore)) {
            throw new IllegalArgumentException("ignore matrix sizes " + deepTestSettings.ignore
                + " do not match result extremums matrix sizes " + resultExtremums);
        }
        this.ignore = deepTestSettings.ignore == null ?
                Arrays.nBitCopies(resultExtremums.size(), false) :
                deepTestSettings.ignore.array();
        this.buildListOfExtremumsXY = buildListOfExtremumsXY;
        this.maxRadius = aperture.maxRadius();
        this.minOptimizedYHorizontal = dimX == 0 ? Integer.MAX_VALUE : maxRadius / dimX + 1;
        this.maxOptimizedYHorizontal = dimY - 1 - minOptimizedYHorizontal;
        // - It guarantees that (x+dx,y) will be always inside values/mask arrays.
        this.minOptimizedY = maxRadius + minOptimizedYHorizontal;
        this.maxOptimizedY = dimY - 1 - minOptimizedY;
        // - It guarantees that any (x+dx,y+dy) will be always inside values/mask arrays.
        this.neighbourOffsets = new int[aperture.count()];
        this.neighboursCount = 0;
        switch (deepTestSettings.mode) {
            case PERCENTILE: {
                if (this.depthAperture != null) {
                    final double level = searchingForMaximums ? 1.0 - percentileLevel : percentileLevel;
                    if (level == 0.0) {
                        this.oppositeExtremumFinder = new MinimumInDepthApertureFinder();
                    } else if (level == 1.0) {
                        this.oppositeExtremumFinder = new MaximumInDepthApertureFinder();
                    } else {
                        this.oppositeExtremumFinder = new PercentileInDepthApertureFinder();
                    }
                } else {
                    if (percentileLevel == 1.0) {
                        this.oppositeExtremumFinder = new StrictOppositeExtremumInMainApertureFinder();
                    } else {
                        this.oppositeExtremumFinder = new PercentileInMainApertureFinder();
                    }
                }
                break;
            }
            case MEAN: {
                if (this.depthAperture != null) {
                    this.oppositeExtremumFinder = new MeanInDepthApertureFinder();
                } else {
                    this.oppositeExtremumFinder = new MeanInMainApertureFinder();
                }
                break;
            }
            default:
                throw new AssertionError("Unsupported mode " + deepTestSettings.mode);
        }
    }

    public static ExtremumsFinder getMaximumsFinder(
            float[] values,
            boolean[] mask,
            SortedRound2DAperture aperture,
            DeepTestSettings deepTestSettings,
            Matrix<UpdatableBitArray> resultExtremums,
            boolean buildListOfExtremumsXY) {
        return mask == null ?
                new MaximumsFinderWithoutMask(
                        values, aperture, deepTestSettings, resultExtremums, buildListOfExtremumsXY) :
                new MaximumsFinder(
                        values, mask, aperture, deepTestSettings, resultExtremums, buildListOfExtremumsXY);
    }

    public static ExtremumsFinder getMinimumsFinder(
            float[] values,
            boolean[] mask,
            SortedRound2DAperture aperture,
            DeepTestSettings deepTestSettings,
            Matrix<UpdatableBitArray> resultExtremums,
            boolean buildListOfExtremumsXY) {
        return mask == null ?
                new MinimumsFinderWithoutMask(
                        values, aperture, deepTestSettings, resultExtremums, buildListOfExtremumsXY) :
                new MinimumsFinder(
                        values, mask, aperture, deepTestSettings, resultExtremums, buildListOfExtremumsXY);
    }

    public MutableIntArray getExtremumsXY() {
        return extremumsXY;
    }

    public void processLine(int y) {
        this.y = y;
        this.valuesStart = this.y * dimX;
        final int pMinSimplified = valuesStart + maxRadius;
        final int pMaxSimplified = valuesStart + dimX - 1 - maxRadius;
        // - JVM works better with local variables, not fields of an object
        int p = valuesStart;
        // x0 = 0..maxRadius-1
        for (; p < pMinSimplified; p++) {
            detailedCheck(p);
        }
        if (this.y >= minOptimizedY && this.y <= maxOptimizedY) {
            // x0 = maxRadius..dimX-1-maxRadius
            for (; p <= pMaxSimplified; p++) {
                quickCheck(p);
            }
        } else {
            for (; p <= pMaxSimplified; p++) {
                if (horizontalCheck(p)) {
                    detailedCheck(p);
                }
            }
        }
        // x0 = dimX-maxRadius..dimX-1
        for (int pTo = valuesStart + dimX; p < pTo; p++) {
            detailedCheck(p);
        }
    }

    abstract boolean horizontalCheck(int index0);

    abstract void quickCheck(int index0);

    abstract void detailedCheck(int index0);

    abstract float strictOppositeExtremum(int index0, int[] neighbourOffsets);

    void processExtremum(int index0, int[] neighbourOffsets) {
        final float extremumValue = values[index0];
        if (Float.isNaN(extremumValue)) {
            return;
        }
        if (ignore.getBit(index0)) {
            return;
        }
        if (needOppositeExtremum) {
            final float opposite = oppositeExtremumFinder.oppositeExtremum(index0, neighbourOffsets);
            if ((searchingForMaximums ? extremumValue - opposite : opposite - extremumValue) < minimalDepth) {
                // Note: if opposite is NaN, this check will be false
                return;
            }
        }
        if (buildListOfExtremumsXY) {
            extremumsXY.addInt(index0 - valuesStart);
            extremumsXY.addInt(y);
        }
        extremums.array().setBit(index0);
        // - no sense to optimize this via setBitNoSync
    }

    private static float percentileInNeighbours(double level, float[] neighbours, int neighboursCount) {
        if (neighboursCount > 0) {
            java.util.Arrays.sort(neighbours, 0, neighboursCount);
            return neighbours[(int) Math.round(level * (neighboursCount - 1))];
        } else {
            return Float.NaN;
        }
    }

    private class StrictOppositeExtremumInMainApertureFinder implements OppositeExtremumFinder {
        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            return ExtremumsFinder.this.strictOppositeExtremum(index0, neighbourFromMainApertureOffsets);
        }
    }

    private class PercentileInMainApertureFinder implements OppositeExtremumFinder {
        final float[] neighbours = new float[apertureCount];

        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final double level = searchingForMaximums ? 1.0 - percentileLevel : percentileLevel;
            final float extremumValue = values[index0];
            int count = 0;
            for (int k = 0; k < neighboursCount; k++) {
                final float v = values[index0 - neighbourFromMainApertureOffsets[k]];
                if (!Float.isNaN(v)) {
                    assert searchingForMaximums ? v <= extremumValue : v >= extremumValue :
                            "Strange neighbour value = " + v + " for extremum " + extremumValue
                                    + ", searchingForMaximums=" + searchingForMaximums
                                    + ", neighboursCount=" + neighboursCount
                                    + ", x=" + (index0 - valuesStart) + ", y=" + y;
                    neighbours[count++] = v;
                }
            }
            return percentileInNeighbours(level, neighbours, count);
        }
    }

    // Note: this class is not absolutely correct in a case of NaN values.
    // If aperture contains NaN, result will be NaN and depth will not be checked.
    private class MeanInMainApertureFinder implements OppositeExtremumFinder {
        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final float extremumValue = values[index0];
            int count = 1;
            double sum = extremumValue;
            // (0,0) is not included in the aperture
            for (int k = 0; k < neighboursCount; k++) {
                sum += values[index0 - neighbourFromMainApertureOffsets[k]];
                count++;
            }
            return (float) (sum / count);
        }
    }

    private class MinimumInDepthApertureFinder implements OppositeExtremumFinder {
        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final int x0 = index0 - valuesStart;
            final int y0 = ExtremumsFinder.this.y;
            final int dimX = ExtremumsFinder.this.dimX;
            final int dimY = ExtremumsFinder.this.dimY;
            final int[] dx = depthAperture.dx();
            final int[] dy = depthAperture.dy();
            final int[] offsets  = depthAperture.offsets();
            float result = Float.POSITIVE_INFINITY;
            boolean found = false;
            for (int k = 0, m = depthAperture.count(); k < m; k++) {
                final int x = x0 - dx[k];
                final int y = y0 - dy[k];
                if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                    final int index = index0 - offsets[k];
                    if (mask == null || mask[index]) {
                        final float v = values[index];
                        if (!Float.isNaN(v)) {
                            found = true;
                            if (v < result) {
                                result = v;
                            }
                        }
                    }
                }
            }
            return found ? result : Float.NaN;
        }
    }

    private class MaximumInDepthApertureFinder implements OppositeExtremumFinder {
        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final int x0 = index0 - valuesStart;
            final int y0 = ExtremumsFinder.this.y;
            final int dimX = ExtremumsFinder.this.dimX;
            final int dimY = ExtremumsFinder.this.dimY;
            final int[] dx = depthAperture.dx();
            final int[] dy = depthAperture.dy();
            final int[] offsets  = depthAperture.offsets();
            float result = Float.NEGATIVE_INFINITY;
            boolean found = false;
            for (int k = 0, m = depthAperture.count(); k < m; k++) {
                final int x = x0 - dx[k];
                final int y = y0 - dy[k];
                if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                    final int index = index0 - offsets[k];
                    if (mask == null || mask[index]) {
                        final float v = values[index];
                        if (!Float.isNaN(v)) {
                            found = true;
                            if (v > result) {
                                result = v;
                            }
                        }
                    }
                }
            }
            return found ? result : Float.NaN;
        }
    }

    private class PercentileInDepthApertureFinder implements OppositeExtremumFinder {
        final float[] neighbours = new float[depthAperture.count()];

        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final double level = searchingForMaximums ? 1.0 - percentileLevel : percentileLevel;
            final int x0 = index0 - valuesStart;
            final int y0 = ExtremumsFinder.this.y;
            final int dimX = ExtremumsFinder.this.dimX;
            final int dimY = ExtremumsFinder.this.dimY;
            final int[] dx = depthAperture.dx();
            final int[] dy = depthAperture.dy();
            final int[] offsets  = depthAperture.offsets();
            int count = 0;
            for (int k = 0, m = depthAperture.count(); k < m; k++) {
                final int x = x0 - dx[k];
                final int y = y0 - dy[k];
                if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                    final int index = index0 - offsets[k];
                    if (mask == null || mask[index]) {
                        final float v = values[index];
                        if (!Float.isNaN(v)) {
                            neighbours[count++] = v;
                        }
                    }
                }
            }
            return percentileInNeighbours(level, neighbours, count);
        }
    }

    // Note: this class is not absolutely correct in a case of NaN values.
    // If aperture contains NaN, result will be NaN and depth will not be checked.
    private class MeanInDepthApertureFinder implements OppositeExtremumFinder {
        @Override
        public float oppositeExtremum(int index0, int[] neighbourFromMainApertureOffsets) {
            final int x0 = index0 - valuesStart;
            final int y0 = ExtremumsFinder.this.y;
            final int dimX = ExtremumsFinder.this.dimX;
            final int dimY = ExtremumsFinder.this.dimY;
            final int[] dx = depthAperture.dx();
            final int[] dy = depthAperture.dy();
            final int[] offsets  = depthAperture.offsets();
            int count = 1;
            double sum = values[index0];
            // (0,0) is not included in the aperture
            for (int k = 0, m = depthAperture.count(); k < m; k++) {
                final int x = x0 - dx[k];
                final int y = y0 - dy[k];
                if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                    final int index = index0 - offsets[k];
                    if (mask == null || mask[index]) {
                        sum += values[index];
                        count++;
                    }
                }
            }
            return (float) (sum / count);
        }
    }
}
