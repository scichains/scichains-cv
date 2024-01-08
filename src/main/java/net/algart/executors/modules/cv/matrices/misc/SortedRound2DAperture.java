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

package net.algart.executors.modules.cv.matrices.misc;

import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.math.IPoint;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;

import java.util.*;

// Sorted, points with y=0 are last
public final class SortedRound2DAperture {
    public static final int MAX_SIZE = 10000;
    // There is a guarantee that dx*dx+dy*dy <= Integer.MAX_VALUE / 4

    private final int dimX;
    private final int[] dx;
    private final int[] dy;
    private final int[] offsets;
    private final int[] hypotSqr;
    private final int count;
    private final int countWithoutLine;
    private final int maxRadius;
    private final boolean sortedByIncreasingRadius;

    private SortedRound2DAperture(int apertureSize, long dimX, boolean boundary, boolean specialOrderForAxes) {
        if (apertureSize < 0) {
            throw new IllegalArgumentException("Zero or negative aperture size = " + apertureSize);
        }
        if (apertureSize >= MAX_SIZE) {
            throw new IllegalArgumentException("Too large aperture size = " + apertureSize + " > " + MAX_SIZE);
        }
        if (dimX < 0) {
            throw new IllegalArgumentException("Negative dimX = " + dimX);
        }
        if (dimX > Integer.MAX_VALUE - 2 * MAX_SIZE) {
            throw new IllegalArgumentException("Too large dimX = " + dimX + " > "
                    + (Integer.MAX_VALUE - 2 * MAX_SIZE));
        }
        this.dimX = (int) dimX;
        Pattern pattern = MorphologyFilter.Shape.SPHERE.newPattern(2, apertureSize);
        if (boundary) {
            pattern = boundary(pattern);
        }
        final List<IPoint> points = new ArrayList<>(pattern.roundedPoints());
        // It must be circle, square or something like this for correct processing maxRadius
        this.sortedByIncreasingRadius = !specialOrderForAxes;
        Collections.sort(points, (o1, o2) -> {
            if (specialOrderForAxes) {
                if (o1.x() == 0 && o2.x() != 0) {
                    return -1;
                }
                if (o1.x() != 0 && o2.x() == 0) {
                    return 1;
                }
                // - All x=0 will be first
                if (o1.y() == 0 && o2.y() != 0) {
                    return 1;
                }
                if (o1.y() != 0 && o2.y() == 0) {
                    return -1;
                }
                // - All y=0 will be last (were already checked).
                // They are still necessary even while optimizing horizontal line,
                // for building "neighbourOffsets" array and further opposite extremum processing.
                return Double.compare(o2.distanceFromOrigin(), o1.distanceFromOrigin());
                // In other cases, sorting by decreasing radius
            } else {
                return Double.compare(o1.distanceFromOrigin(), o2.distanceFromOrigin());
                // In other cases, sorting by normal order (increasing radius)
            }
        });
//                Collections.shuffle(points); - leads to comparable results!
        this.count = points.size();
        this.dx = new int[count];
        this.dy = new int[count];
        this.offsets = new int[count];
        this.hypotSqr = new int[count];
        int k = 0;
        int minXAtZeroY = 0;
        int maxXAtZeroY = 0;
        for (IPoint point : points) {
            if (point.isOrigin()) {
                continue;
            }
            final int dx = (int) point.x();
            final int dy = (int) point.y();
            if (dy == 0) {
                assert dx != 0;
                // - see above
                minXAtZeroY = Math.min(minXAtZeroY, dx);
                maxXAtZeroY = Math.max(maxXAtZeroY, dx);
            }
            this.dx[k] = dx;
            this.dy[k] = dy;
            final long offset = (long) this.dy[k] * dimX + (long) this.dx[k];
            if (offset != (int) offset) {
                throw new IllegalArgumentException("Too large aperture: offsets > Integer.MAX_VALUE");
            }
            offsets[k] = (int) offset;
            hypotSqr[k] = dx * dx + dy * dy;
            k++;
        }
        if (maxXAtZeroY + minXAtZeroY != 0) {
            throw new AssertionError("Asymmetric pattern generator " + MorphologyFilter.Shape.SPHERE);
        }
        assert maxXAtZeroY <= MAX_SIZE : "Strange pattern generator " + MorphologyFilter.Shape.SPHERE;
        this.maxRadius = maxXAtZeroY;
        if (!specialOrderForAxes) {
            this.countWithoutLine = count;
        } else {
            this.countWithoutLine = count - 2 * maxXAtZeroY;
            for (int i = 0; i < 2 * maxXAtZeroY; i++) {
                if (dx[i] != 0) {
                    throw new AssertionError("Problem while sorting or pattern generator");
                }
            }
            for (int i = countWithoutLine; i < count; i++) {
                if (dy[i] != 0) {
                    throw new AssertionError("Problem while sorting");
                }
            }
        }
    }

    public static SortedRound2DAperture getCircle(int apertureSize, long dimX) {
        return new SortedRound2DAperture(apertureSize, dimX, false, false);
    }

    public static SortedRound2DAperture getRing(int apertureSize, long dimX) {
        return new SortedRound2DAperture(apertureSize, dimX, true, false);
    }

    public static SortedRound2DAperture getCircleWithSpeciallyOrderedPointsAtAxes(int apertureSize, long dimX) {
        return new SortedRound2DAperture(apertureSize, dimX, false, true);
    }

    public int dimX() {
        return dimX;
    }

    /**
     * Returns a reference to the internal array of x-offsets. Please do not change the content of the returned array!
     *
     * @return array of x-offsets of all aperture points.
     */
    public int[] dx() {
        return dx;
    }

    /**
     * Returns a reference to the internal array of y-offsets. Please do not change the content of the returned array!
     *
     * @return array of y-offsets of all aperture points.
     */
    public int[] dy() {
        return dy;
    }

    /**
     * Returns a reference to the internal array of offsets. Please do not change the content of the returned array!
     *
     * @return array of relative offsets of all aperture points.
     */
    public int[] offsets() {
        return offsets;
    }

    public int hypotSqr(int k) {
        return hypotSqr[k];
    }

    public int count() {
        return count;
    }

    public int countWithoutLine() {
        return countWithoutLine;
    }

    public int maxRadius() {
        return maxRadius;
    }

    public boolean isSortedByIncreasingRadius() {
        return sortedByIncreasingRadius;
    }

    private static Pattern boundary(Pattern main) {
        Pattern erosion = main.minkowskiSubtract(MorphologyOperation.crossPattern(main.dimCount()));
        if (erosion == null) {
            return main;
        }
        Set<IPoint> points = new LinkedHashSet<>(main.roundedPoints());
        points.removeAll(erosion.roundedPoints());
        return Patterns.newIntegerPattern(points);
    }
}
