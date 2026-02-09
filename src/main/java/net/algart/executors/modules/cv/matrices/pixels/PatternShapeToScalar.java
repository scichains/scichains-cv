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

package net.algart.executors.modules.cv.matrices.pixels;

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.PFixedArray;
import net.algart.arrays.TooLargeArrayException;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.math.functions.LinearFunc;
import net.algart.multimatrix.MultiMatrix2D;

public final class PatternShapeToScalar extends Executor implements ReadOnlyExecutionInput {
    public static final String OUTPUT_SHAPE_DESCRIPTION = "shape_specification";

    private boolean invert = false;

    public PatternShapeToScalar() {
        addInputMat(DEFAULT_INPUT_PORT);
        setDefaultOutputScalar(OUTPUT_SHAPE_DESCRIPTION);
    }

    public boolean isInvert() {
        return invert;
    }

    public PatternShapeToScalar setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix2D source = getInputMat().toMultiMatrix2D(true);
        if (source.dimX() > Integer.MAX_VALUE || source.dimY() > Integer.MAX_VALUE
                || (source.dimX() + 1) * source.dimY() > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large source matrix to be converted to scalar: " + source);
        }
        PArray array = source.intensityChannel().array();
        if (invert) {
            array = Arrays.asFuncArray(
                    LinearFunc.getInstance(array.maxPossibleValue(1.0), -1.0),
                    array.type(),
                    array);
        }
        final PFixedArray integerArray = array instanceof PFixedArray ? (PFixedArray) array : null;
        final StringBuilder sb = new StringBuilder();
        for (int y = 0, dimY = (int) source.dimY(), disp = 0; y < dimY; y++) {
            for (int x = 0, dimX = (int) source.dimX(); x < dimX; x++, disp++) {
                if (x > 0) {
                    sb.append(' ');
                }
                if (integerArray != null) {
                    sb.append(integerArray.getLong(disp));
                } else {
                    sb.append(array.getDouble(disp));
                }
            }
            sb.append('\n');
        }
        getScalar().setTo(sb.toString());
    }
}
