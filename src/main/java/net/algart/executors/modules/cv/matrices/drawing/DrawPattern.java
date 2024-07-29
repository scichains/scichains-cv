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

package net.algart.executors.modules.cv.matrices.drawing;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.math.IPoint;
import net.algart.math.Point;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Set;

public final class DrawPattern extends MorphologyFilter {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_COLORS = "colors";

    private long x = 0;
    private long y = 0;
    private String color = "#FFFFFF";
    private boolean clearSource = false;

    public DrawPattern() {
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_COLORS);
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = nonNull(color);
    }

    public boolean isClearSource() {
        return clearSource;
    }

    public DrawPattern setClearSource(boolean clearSource) {
        this.clearSource = clearSource;
        return this;
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double maxPossibleValue = m.array().maxPossibleValue(1.0);
        final double defaultValue = colorChannel(color, maxPossibleValue);
        final PositionsAndColors positionsAndColors = new PositionsAndColors(
                positions, colors, new double[]{x, y}, defaultValue, 2);
        final UpdatablePArray updatableArray =
                clearSource ? (UpdatablePArray) Arrays.SMM.newUnresizableArray(m.array())
                        : m.array() instanceof UpdatablePArray ? (UpdatablePArray) m.array()
                        : m.array().updatableClone(Arrays.SMM);
        if (clearSource && currentChannel() == MultiMatrix2D.DEFAULT_ALPHA_CHANNEL) {
            updatableArray.fill(updatableArray.maxPossibleValue(1.0));
        }
        final Matrix<? extends UpdatablePArray> updatableMatrix = m.matrix(updatableArray);
        for (int k = 0; k < positionsAndColors.n; k++) {
            drawPattern(
                    updatableMatrix,
                    getPattern(m),
                    Point.valueOf(positionsAndColors.x(k), positionsAndColors.y(k)).toRoundedPoint(),
                    positionsAndColors.colorValue(k, currentChannel(), maxPossibleValue),
                    getContinuationMode());
        }
        return updatableMatrix;
    }

    public static void drawPattern(
            Matrix<? extends UpdatablePArray> m,
            net.algart.math.patterns.Pattern ptn,
            IPoint position,
            double value,
            Matrix.ContinuationMode continuationMode) {
        boolean is3d = ptn.dimCount() != 2;
        if (is3d) {
            ptn = ptn.maxBound(2);
        }
        final Set<Point> points = ptn.points();
        for (Point p : points) {
            long[] coordinates = (is3d ? p.projectionAlongAxis(2) : p).toRoundedPoint().add(position).coordinates();
            long index;
            if (continuationMode == Matrix.ContinuationMode.CYCLIC) {
                index = m.cyclicIndex(coordinates);
            } else if (continuationMode == null || continuationMode == Matrix.ContinuationMode.PSEUDO_CYCLIC) {
                index = m.pseudoCyclicIndex(coordinates);
            } else if (continuationMode == Matrix.ContinuationMode.MIRROR_CYCLIC) {
                index = m.mirrorCyclicIndex(coordinates);
            } else {
                if (m.inside(coordinates)) {
                    index = m.index(coordinates);
                } else {
                    continue;
                }
            }
            double v = value;
            if (is3d) {
                v += p.coord(2);
                v = Math.max(v, 0.0);
                v = Math.min(v, m.array().maxPossibleValue(1.0));
            }
            m.array().setDouble(index, v);
        }
    }
}
