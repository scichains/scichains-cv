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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.executors.modules.cv.matrices.morphology.StrictMorphology;
import net.algart.arrays.Matrix;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;

public final class LabelsBoundaries extends MultiMatrix2DFilter {
    public static final String INPUT_LABELS = "labels";

    private boolean invert = false;

    public LabelsBoundaries() {
        setDefaultInputMat(INPUT_LABELS);
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D labels) {
        final StrictMorphology morphologyGradient = new StrictMorphology();
        morphologyGradient.setOperation(MorphologyOperation.BEUCHER_GRADIENT);
        morphologyGradient.setCustomPattern(MorphologyOperation.crossPattern(labels.dimCount()));

        // CROSS is the minimal pattern, that will draw at least 1 pixel from both sides of the inter-pixel boundary.
        // On the other hand, CROSS produces enough boundaries to separate objects to different 8-connected components:
        //         x  x  x! 0! 0  0
        //         x! x! x! 0! 0! 0!
        //         0! 0! 0! x! x! x!
        //         0  0  0! x! x  x
        //         0  0  0! x! x  x
        morphologyGradient.setContinuationMode(Matrix.ContinuationMode.MIRROR_CYCLIC);
        if (invert) {
            final MultiMatrix2D nonBackground = labels.nonZeroAnyChannel();
            return morphologyGradient.process(labels).zeroAllChannels().min(nonBackground).asMultiMatrix2D();
        } else {
            return morphologyGradient.process(labels).nonZeroAnyChannel().asMultiMatrix2D();
        }
    }
}
