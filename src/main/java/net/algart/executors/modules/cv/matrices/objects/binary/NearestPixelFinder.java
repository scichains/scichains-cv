/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.objects.binary;

import net.algart.arrays.BitArray;
import net.algart.arrays.Matrix;
import net.algart.arrays.MutableIntArray;
import net.algart.arrays.PArray;
import net.algart.executors.modules.cv.matrices.misc.SortedRound2DAperture;
import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.ContinuedMorphology;
import net.algart.matrices.morphology.Morphology;

import java.util.Objects;

public final class NearestPixelFinder {
    private static final Pattern CROSS_PATTERN = Patterns.newIntegerPattern(
            IPoint.of(0, 0),
            IPoint.of(1, 0),
            IPoint.of(0, 1),
            IPoint.of(-1, 0),
            IPoint.of(0, -1)
    );

    private final boolean[] maskArray;
    private final boolean[] boundaries;
    private final int dimX;
    private final int dimY;
    private final SortedRound2DAperture maxAperture;
    private final SortedRound2DAperture neighbourhoodForNearest;
    private final int[] maxApertureOffsets;
    private final int[] maxApertureDx;
    private final int[] maxApertureDy;
    private final int[] neighbourhoodOffsets;
    private final int[] neighbourhoodDx;
    private final int[] neighbourhoodDy;
    private final int maxRadiusSum;
    private int maxNumberOfNeighbours = 1;
    private boolean skipPositionsAtMaks = true;

    public NearestPixelFinder(
            Matrix<? extends BitArray> mask,
            SortedRound2DAperture maxAperture,
            SortedRound2DAperture neighbourhoodForNearest) {
        Objects.requireNonNull(mask, "Null mask matrix");
        this.maskArray = mask.array().toJavaArray();
        this.maxAperture = Objects.requireNonNull(maxAperture);
        this.neighbourhoodForNearest = Objects.requireNonNull(neighbourhoodForNearest);
        if (!maxAperture.isSortedByIncreasingRadius()) {
            throw new IllegalArgumentException("maxAperture must be sorted by increasing radius");
        }
        this.maxApertureOffsets = maxAperture.offsets();
        this.maxApertureDx = maxAperture.dx();
        this.maxApertureDy = maxAperture.dy();
        this.neighbourhoodOffsets = neighbourhoodForNearest.offsets();
        this.neighbourhoodDx = neighbourhoodForNearest.dx();
        this.neighbourhoodDy = neighbourhoodForNearest.dy();
        this.maxRadiusSum = maxAperture.maxRadius() + neighbourhoodForNearest.maxRadius();
        final Morphology morphology = ContinuedMorphology.getInstance(
                BasicMorphology.getInstance(null),
                Matrix.ContinuationMode.ZERO_CONSTANT);
        final Matrix<? extends PArray> boundariesMatrix =
                morphology.erosion(mask, CROSS_PATTERN, Morphology.SubtractionMode.SUBTRACT_RESULT_FROM_SRC);
        this.boundaries = (boolean[]) boundariesMatrix.toJavaArray();
        // No we are sure that dimX and dimY are <=Integer.MAX_VALUE
        this.dimY = (int) mask.dimY();
        this.dimX = (int) mask.dimX();
        if (dimX != maxAperture.dimX()) {
            throw new IllegalArgumentException("maxAperture and mask matrix have different dimX");
        }
        if (dimX != neighbourhoodForNearest.dimX()) {
            throw new IllegalArgumentException("neighbourhoodForNearest and mask matrix have different dimX");
        }
    }

    public int getMaxNumberOfNeighbours() {
        return maxNumberOfNeighbours;
    }

    public NearestPixelFinder setMaxNumberOfNeighbours(int maxNumberOfNeighbours) {
        if (maxNumberOfNeighbours <= 0) {
            throw new IllegalArgumentException("Zero or negative maxNumberOfNeighbours");
        }
        this.maxNumberOfNeighbours = maxNumberOfNeighbours;
        return this;
    }

    public boolean isSkipPositionsAtMaks() {
        return skipPositionsAtMaks;
    }

    public NearestPixelFinder setSkipPositionsAtMaks(boolean skipPositionsAtMaks) {
        this.skipPositionsAtMaks = skipPositionsAtMaks;
        return this;
    }

    public int findNearest(int centerX, int centerY, MutableIntArray resultXY) {
        final int dimX = this.dimX;
        final int dimY = this.dimY;
        // - JVM works better with local variables, not fields of an object
        final int centerIndex = centerY * dimX + centerX;
        int count = 0;
        if (maskArray[centerIndex]) {
            if (skipPositionsAtMaks) {
                return 0;
            } else {
                addPoint(centerX, centerY, resultXY);
                if (maxNumberOfNeighbours == 1) {
                    return 1;
                }
                count++;
            }
        }
        if (centerX >= maxRadiusSum && centerX < dimX - maxRadiusSum
                && centerY >= maxRadiusSum && centerY < dimY - maxRadiusSum) {
            return quickCheckAperture(centerX, centerY, centerIndex, count, resultXY);
        } else {
            return checkAperture(centerX, centerY, centerIndex, count, resultXY);
        }
    }

    private int quickCheckAperture(int centerX, int centerY, int centerIndex, int count, MutableIntArray resultXY) {
        search:
        for (int k = 0, m = maxAperture.count(), n = neighbourhoodForNearest.count(); k < m; k++) {
            final int indexProbe = centerIndex + maxApertureOffsets[k];
            if (boundaries[indexProbe]) {
                final int dxProbe = maxApertureDx[k];
                final int dyProbe = maxApertureDy[k];
                final int dProbe = maxAperture.hypotSqr(k);
                for (int j = 0; j < n; j++) {
                    final int index = indexProbe + neighbourhoodOffsets[j];
                    if (maskArray[index]) {
                        final int dx = dxProbe + neighbourhoodDx[j];
                        final int dy = dyProbe + neighbourhoodDy[j];
                        final int d = dx * dx + dy * dy;
                        if (d < dProbe || (d == dProbe && (dy < dyProbe || (dy == dyProbe && dx < dxProbe)))) {
                            // - from several equidistance (d==dProbe) we also keep only one
                            continue search;
                        }
                    }
                }
                addPoint(centerX + dxProbe, centerY + dyProbe, resultXY);
                count++;
                if (count >= maxNumberOfNeighbours) {
                    return count;
                }
            }
        }
        return count;
    }

    private int checkAperture(int centerX, int centerY, int centerIndex, int count, MutableIntArray resultXY) {
        search:
        for (int k = 0, m = maxAperture.count(), n = neighbourhoodForNearest.count(); k < m; k++) {
            final int xProbe = centerX + maxApertureDx[k];
            final int yProbe = centerY + maxApertureDy[k];
            if (xProbe >= 0 && yProbe >= 0 && xProbe < dimX && yProbe < dimY) {
                final int indexProbe = centerIndex + maxApertureOffsets[k];
                assert indexProbe >= 0 : indexProbe + "center " + centerX + ", " + centerY
                        + ", point=" + xProbe + ", " + yProbe + " at " + k;
                if (boundaries[indexProbe]) {
                    final int dProbe = maxAperture.hypotSqr(k);
                    for (int j = 0; j < n; j++) {
                        final int x = xProbe + neighbourhoodDx[j];
                        final int y = yProbe + neighbourhoodDy[j];
                        if (x >= 0 && y >= 0 && x < dimX && y < dimY) {
                            final int index = indexProbe + neighbourhoodOffsets[j];
                            if (maskArray[index]) {
                                final int dx = x - centerX;
                                final int dy = y - centerY;
                                final int d = dx * dx + dy * dy;
                                if (d < dProbe || (d == dProbe && (y < yProbe || (y == yProbe && x < xProbe)))) {
                                    // - from several equidistance (d==dProbe) we also keep only one
                                    continue search;
                                }
                            }
                        }
                    }
                    addPoint(xProbe, yProbe, resultXY);
                    count++;
                    if (count >= maxNumberOfNeighbours) {
                        return count;
                    }
                }
            }
        }
        return count;
    }

    private void addPoint(int nearestX, int nearestY, MutableIntArray resultXY) {
        resultXY.addInt(nearestX);
        resultXY.addInt(nearestY);
    }
}
