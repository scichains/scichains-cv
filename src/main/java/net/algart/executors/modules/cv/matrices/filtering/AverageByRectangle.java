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

package net.algart.executors.modules.cv.matrices.filtering;

import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.math.IRectangularArea;
import net.algart.matrices.morphology.Quick2DAverager;
import net.algart.multimatrix.MultiMatrix;

import java.util.Locale;

public final class AverageByRectangle extends MultiMatrixFilter {
    private int sizeX = 15;
    private int sizeY = 15;
    private int centerX = 0;
    private int centerY = 0;
    boolean strictDivision = false;
    private boolean rounding = true;
    boolean twoStage = false;

    public AverageByRectangle() {
    }

    public int getSizeX() {
        return sizeX;
    }

    public AverageByRectangle setSizeX(int sizeX) {
        this.sizeX = positive(sizeX);
        return this;
    }

    public int getSizeY() {
        return sizeY;
    }

    public AverageByRectangle setSizeY(int sizeY) {
        this.sizeY = nonNegative(sizeY);
        return this;
    }

    public int getCenterX() {
        return centerX;
    }

    public AverageByRectangle setCenterX(int centerX) {
        this.centerX = centerX;
        return this;
    }

    public int getCenterY() {
        return centerY;
    }

    public AverageByRectangle setCenterY(int centerY) {
        this.centerY = centerY;
        return this;
    }

    public boolean isStrictDivision() {
        return strictDivision;
    }

    public AverageByRectangle setStrictDivision(boolean strictDivision) {
        this.strictDivision = strictDivision;
        return this;
    }

    public boolean isRounding() {
        return rounding;
    }

    public AverageByRectangle setRounding(boolean rounding) {
        this.rounding = rounding;
        return this;
    }

    public boolean isTwoStage() {
        return twoStage;
    }

    public AverageByRectangle setTwoStage(boolean twoStage) {
        this.twoStage = twoStage;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        long t1 = debugTime();
        final Quick2DAverager averager = Quick2DAverager.newInstance(
                        source.elementType(),
                        source.dimensions(),
                        twoStage)
                .setRounding(rounding)
                .setStrictDivision(strictDivision);
        final int sizeX = this.sizeX;
        final int sizeY = this.sizeY <= 0 ? this.sizeX : this.sizeY;
        final int minX = centerX - sizeX / 2;
        final int minY = centerY - sizeY / 2;
        final IRectangularArea rectangle = IRectangularArea.valueOf(
                minX, minY, minX + sizeX - 1, minY + sizeY - 1);
        long t2 = debugTime();
        final MultiMatrix result = source.mapChannels(m -> averager.filter(m, rectangle));
        long t3 = debugTime();
        logDebug(() -> String.format(Locale.US, "Averaging of %s by %s using %s calculated in %.3f ms: "
                        + "%.3f initializing, "
                        + "%.3f averaging",
                source,
                rectangle,
                averager,
                (t3 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6));
        return result;
    }

}
