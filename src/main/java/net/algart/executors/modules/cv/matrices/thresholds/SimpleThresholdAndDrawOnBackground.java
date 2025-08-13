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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.executors.modules.core.matrices.misc.Selector;
import net.algart.multimatrix.MultiMatrix;

public final class SimpleThresholdAndDrawOnBackground extends SimpleThreshold {
    public static final String INPUT_BACKGROUND = "background";

    private String colorOnBackground = "#FFFFFF";

    public SimpleThresholdAndDrawOnBackground() {
        addInputMat(INPUT_BACKGROUND);
    }

    public String getColorOnBackground() {
        return colorOnBackground;
    }

    public SimpleThresholdAndDrawOnBackground setColorOnBackground(String colorOnBackground) {
        this.colorOnBackground = nonNull(colorOnBackground);
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        return process(
                source,
                getInputMat(INPUT_MASK, true).toMultiMatrix(),
                getInputMat(INPUT_BACKGROUND, true).toMultiMatrix());
    }

    public MultiMatrix process(MultiMatrix source, MultiMatrix mask, MultiMatrix background) {
        MultiMatrix result = super.process(source, mask);
        if (background != null) {
            final Selector selector = new Selector();
            selector.setFiller(1, colorOnBackground);
            result = selector.process(result, background, null);
        }
        return result;
    }
}
