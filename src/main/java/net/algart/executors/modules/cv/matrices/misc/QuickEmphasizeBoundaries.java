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

package net.algart.executors.modules.cv.matrices.misc;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannel2DFilter;
import net.algart.executors.modules.cv.matrices.misc.slopes.MatrixBoundariesEmphasizer;
import net.algart.executors.modules.cv.matrices.misc.slopes.SlopeEmphasizer;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Locale;

public final class QuickEmphasizeBoundaries extends MultiMatrixChannel2DFilter {
    public enum EmphasizedDirections {
        HORIZONTAL() {
            @Override
            void process(MatrixBoundariesEmphasizer emphasizer, Matrix<? extends UpdatablePArray> matrix) {
                emphasizer.setDirectionToEmphasize(0).emphasize(matrix);
            }
        },
        VERTICAL() {
            @Override
            void process(MatrixBoundariesEmphasizer emphasizer, Matrix<? extends UpdatablePArray> matrix) {
                emphasizer.setDirectionToEmphasize(1).emphasize(matrix);
            }
        },
        HORIZONTAL_AND_VERTICAL() {
            @Override
            void process(MatrixBoundariesEmphasizer emphasizer, Matrix<? extends UpdatablePArray> matrix) {
                emphasizer.setDirectionToEmphasize(0).emphasize(matrix);
                emphasizer.setDirectionToEmphasize(1).emphasize(matrix);
            }
        },
        VERTICAL_AND_HORIZONTAL() {
            @Override
            void process(MatrixBoundariesEmphasizer emphasizer, Matrix<? extends UpdatablePArray> matrix) {
                emphasizer.setDirectionToEmphasize(1).emphasize(matrix);
                emphasizer.setDirectionToEmphasize(0).emphasize(matrix);
            }
        };

        abstract void process(MatrixBoundariesEmphasizer emphasizer, Matrix<? extends UpdatablePArray> matrix);
    }

    private boolean autoConvertToGrayscale = false;
    private double minimalChange = 0.0;
    private int slopeWidth = 1;
    private boolean allowLongSlopes = true;
    private boolean processAscending = true;
    private boolean processDescending = true;
    private boolean exactHalfSum = true;
    private EmphasizedDirections emphasizedDirections = EmphasizedDirections.HORIZONTAL_AND_VERTICAL;
    private boolean rawValues = false;

    public QuickEmphasizeBoundaries() {
    }

    public boolean isAutoConvertToGrayscale() {
        return autoConvertToGrayscale;
    }

    public QuickEmphasizeBoundaries setAutoConvertToGrayscale(boolean autoConvertToGrayscale) {
        this.autoConvertToGrayscale = autoConvertToGrayscale;
        return this;
    }

    public double getMinimalChange() {
        return minimalChange;
    }

    public QuickEmphasizeBoundaries setMinimalChange(double minimalChange) {
        this.minimalChange = nonNegative(minimalChange);
        return this;
    }

    public int getSlopeWidth() {
        return slopeWidth;
    }

    public QuickEmphasizeBoundaries setSlopeWidth(int slopeWidth) {
        this.slopeWidth = positive(slopeWidth);
        return this;
    }

    public boolean isAllowLongSlopes() {
        return allowLongSlopes;
    }

    public QuickEmphasizeBoundaries setAllowLongSlopes(boolean allowLongSlopes) {
        this.allowLongSlopes = allowLongSlopes;
        return this;
    }

    public boolean isProcessAscending() {
        return processAscending;
    }

    public QuickEmphasizeBoundaries setProcessAscending(boolean processAscending) {
        this.processAscending = processAscending;
        return this;
    }

    public boolean isProcessDescending() {
        return processDescending;
    }

    public QuickEmphasizeBoundaries setProcessDescending(boolean processDescending) {
        this.processDescending = processDescending;
        return this;
    }

    public boolean isExactHalfSum() {
        return exactHalfSum;
    }

    public QuickEmphasizeBoundaries setExactHalfSum(boolean exactHalfSum) {
        this.exactHalfSum = exactHalfSum;
        return this;
    }

    public EmphasizedDirections getEmphasizedDirections() {
        return emphasizedDirections;
    }

    public QuickEmphasizeBoundaries setEmphasizedDirections(EmphasizedDirections emphasizedDirections) {
        this.emphasizedDirections = nonNull(emphasizedDirections);
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public QuickEmphasizeBoundaries setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        if (autoConvertToGrayscale) {
            source = source.toMonoIfNot();
        }
        return super.process(source);
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        if (m.elementType() == boolean.class) {
            return m;
        }
        long t1 = debugTime();
        final double scale = rawValues ? 1.0 : m.maxPossibleValue();
        final SlopeEmphasizer slopeEmphasizer = SlopeEmphasizer.getInstance()
                .setMinimalChange(minimalChange * scale)
                .setSlopeWidth(slopeWidth)
                .setAllowLongSlopes(allowLongSlopes)
                .setProcessAscending(processAscending)
                .setProcessDescending(processDescending)
                .setExactHalfSum(exactHalfSum);
        final Matrix<? extends UpdatablePArray> result = Matrices.clone(m);
        long t2 = debugTime();
        emphasizedDirections.process(MatrixBoundariesEmphasizer.getInstance(slopeEmphasizer), result);
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Emphasizing boundaries for %s, channel %d: %.3f ms = %.3f ms cloning + %.3f ms processing",
                m, currentChannel(),
                (t3 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        return result;
    }
}