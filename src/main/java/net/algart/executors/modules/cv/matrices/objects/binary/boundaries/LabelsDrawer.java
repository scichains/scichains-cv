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
import net.algart.matrices.scanning.Boundary2DScanner;

class LabelsDrawer {
    private final BoundariesScanner scanner;
    private final Boundary2DScanner boundaryScanner;
    private final int[] labels;
    private final byte[] brackets;
    private final int dimX;
    private final int dimY;

    LabelsDrawer(BoundariesScanner scanner) {
        final Matrix<? extends PFixedArray> scannedMatrix = scanner.objects();
        if (scannedMatrix.size() > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot create labels for " + scannedMatrix + ": it is too large");
        }
        this.scanner = scanner;
        this.boundaryScanner = scanner.getBoundaryScanner();
        this.dimX = (int) scannedMatrix.dimX();
        this.dimY = (int) scannedMatrix.dimY();
        this.labels = new int[scannedMatrix.size32()];
        this.brackets = new byte[labels.length];
        // - zero-filled by Java
    }

    LabelsDrawer(long dimX, long dimY) {
        if (dimX < 0 || dimY < 0) {
            throw new IllegalArgumentException("Negative dimX or dimY");
        }
        if (dimX > Integer.MAX_VALUE || dimY > Integer.MAX_VALUE || dimX * dimY > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Cannot create labels for matrix "
                    + dimX + "x" + dimY + ": it is too large");
        }
        this.scanner = null;
        this.boundaryScanner = null;
        this.dimX = (int) dimX;
        this.dimY = (int) dimY;
        this.labels = new int[(int) (dimX * dimY)];
        this.brackets = new byte[labels.length];
        // - zero-filled by Java
    }

    public int dimX() {
        return dimX;
    }

    public int dimY() {
        return dimY;
    }

    void drawBracket(long objectLabel, boolean internalBoundary) {
        drawBracket(
                objectLabel,
                (int) boundaryScanner.currentIndexInArray(),
                boundaryScanner.side(),
                internalBoundary);
    }

    void drawBracket(
            long objectLabel,
            int currentIndexInArray,
            Boundary2DScanner.Side side,
            boolean internalBoundary) {
        assert objectLabel >= 0;
        if (objectLabel < Integer.MAX_VALUE) {
            final int value = (int) objectLabel + 1;
            if (!internalBoundary) {
                switch (side) {
                    case X_MINUS: {
                        setOpeningBracket(currentIndexInArray, value);
                        break;
                    }
                    case X_PLUS: {
                        setClosingBracket(currentIndexInArray, value);
                        break;
                    }
                }
            } else {
                switch (side) {
                    case X_MINUS: {
                        assert boundaryScanner == null || boundaryScanner.x() > 0;
                        setClosingBracket(currentIndexInArray - 1, value);
                        break;
                    }
                    case X_PLUS: {
                        setOpeningBracket(currentIndexInArray + 1, value);
                        break;
                    }
                }
            }
        }
    }

    void buildLabels() {
        final int dimX = this.dimX;
        final int dimY = this.dimY;
        final SimpleIntPseudoStack pseudoStack = new SimpleIntPseudoStack(dimX);
        for (int y = 0, disp = 0; y < dimY; y++) {
            int filler = 0;
            for (int x = 0; x < dimX; x++, disp++) {
                if (isNoBrackets(disp)) {
                    // - 0 is reserved for empty values
                    labels[disp] = filler;
                } else if (isOnlyOpeningBracket(disp)) {
                    filler = getValue(disp);
                    pseudoStack.push(filler);
                } else if (isOnlyClosingBracket(disp)) {
                    filler = pseudoStack.remove(getValue(disp), 0);
                } else {
                    // - open and close brackets at the single pixel: 1-pixel particle (or pore), nothing to do
                }
            }
            assert pseudoStack.size() == 0 : "unbalanced brackets (line " + y + ")";
        }
    }

    Matrix<? extends PArray> getLabels() {
        return Matrices.matrix(SimpleMemoryModel.asUpdatableIntArray(labels), dimX, dimY);
    }

    void debuggingPrintLabels() {
        for (int y = 0, disp = 0; y < dimY; y++) {
            System.out.printf("%n~~y=%-3d", y);
            for (int x = 0; x < dimX; x++, disp++) {
                System.out.printf(" %3d%s", getValue(disp), labels[disp] < 0 ? "]" : " ");
            }
        }
        System.out.println();
    }

    private void setOpeningBracket(int index, int value) {
        final int previous = labels[index];
        assert previous == 0 || previous == value : "invalid " + previous + " and " + value + " at " + index;
        labels[index] = value;
        brackets[index] |= 0x1;
    }

    private void setClosingBracket(int index, int value) {
        final int previous = labels[index];
        assert previous == 0 || previous == value : "invalid " + previous + " and " + value + " at " + index;
        labels[index] = value;
        brackets[index] |= 0x2;
    }

    private int getValue(int index) {
        return labels[index];
    }

    private boolean isNoBrackets(int index) {
        return brackets[index] == 0;
    }

    private boolean isOnlyOpeningBracket(int index) {
        return brackets[index] == 0x1;
    }

    private boolean isOnlyClosingBracket(int index) {
        return brackets[index] == 0x2;
    }
}
