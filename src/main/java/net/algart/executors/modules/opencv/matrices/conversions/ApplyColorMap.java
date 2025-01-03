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

package net.algart.executors.modules.opencv.matrices.conversions;

import net.algart.executors.modules.opencv.common.VoidResultMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OColorMap;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class ApplyColorMap extends VoidResultMatFilter implements ReadOnlyExecutionInput {
    private OColorMap colorMap = OColorMap.COLORMAP_HSV;

    public ApplyColorMap() {
    }

    public OColorMap getColorMap() {
        return colorMap;
    }

    public void setColorMap(OColorMap colorMap) {
        this.colorMap = nonNull(colorMap);
    }

    @Override
    public void process(Mat result, Mat source) {
        Mat mat = source;
        try {
            mat = OTools.to8UIfNot(mat);
            opencv_imgproc.applyColorMap(mat, result, colorMap.code());
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }
}
