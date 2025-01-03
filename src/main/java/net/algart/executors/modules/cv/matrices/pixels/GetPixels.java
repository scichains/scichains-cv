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

package net.algart.executors.modules.cv.matrices.pixels;

import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.multimatrix.MultiMatrix2D;

public final class GetPixels extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_PIXEL_VALUES = "pixel_values";

    private boolean rawPixelValues = false;

    public GetPixels() {
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_MASK);
        setDefaultOutputNumbers(OUTPUT_PIXEL_VALUES);
    }

    public boolean isRawPixelValues() {
        return rawPixelValues;
    }

    public GetPixels setRawPixelValues(boolean rawPixelValues) {
        this.rawPixelValues = rawPixelValues;
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix2D source = getInputMat().toMultiMatrix2D(true);
        final MultiMatrix2D mask = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        setStartProcessingTimeStamp();
        final SNumbers result = getNumbers(OUTPUT_PIXEL_VALUES);
        new GetLabelledPixels()
                .setRawPixelValues(rawPixelValues)
                .process(source, mask, result, null);
        setEndProcessingTimeStamp();
        getNumbers().exchange(result);
    }
}
