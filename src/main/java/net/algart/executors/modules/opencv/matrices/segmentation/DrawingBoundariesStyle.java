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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.*;

import java.awt.*;

public enum DrawingBoundariesStyle {
    BOUNDARIES(true, false, false),
    THICK_BOUNDARIES(true, true, false),
    BOUNDARIES_ON_SOURCE(false, false, true),
    THICK_BOUNDARIES_ON_SOURCE(false, true, true);

    final boolean binary;
    final boolean thickBoundaries;
    final boolean drawOnSource;

    DrawingBoundariesStyle(boolean binary, boolean thickBoundaries, boolean drawOnSource) {
        this.binary = binary;
        this.thickBoundaries = thickBoundaries;
        this.drawOnSource = drawOnSource;
    }

    public boolean isThickBoundaries() {
        return thickBoundaries;
    }

    public Mat drawOnSourceIfRequested(Mat boundaries, Mat source, Color color) {
        if (drawOnSource) {
            return OTools.drawBitMaskOnMat(source, boundaries, color);
        } else {
            return boundaries;
        }
    }
}
