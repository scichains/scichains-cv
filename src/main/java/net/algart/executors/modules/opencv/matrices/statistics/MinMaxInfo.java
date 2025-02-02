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

package net.algart.executors.modules.opencv.matrices.statistics;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.opencv.common.UMatToNumbers;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class MinMaxInfo extends UMatToNumbers implements ReadOnlyExecutionInput {
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_MIN_MAX = "min_max";

    public MinMaxInfo() {
        setDefaultOutputNumbers(OUTPUT_MIN_MAX);
    }

    @Override
    public SNumbers analyse(Mat source) {
        return SNumbers.ofArray(minMax(
                source,
                O2SMat.toMat(getInputMat(INPUT_MASK, true), true)));
    }

    @Override
    public SNumbers analyse(UMat source) {
        return SNumbers.ofArray(minMax(
                source,
                O2SMat.toUMat(getInputMat(INPUT_MASK, true), true)));
    }

    public static double[] minMax(UMat source, UMat mask) {
        UMat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            // - multi-channel matrices are not supported by minMaxLoc
            double[] min = new double[mat.channels()];
            double[] max = new double[mat.channels()];
            opencv_core.minMaxLoc(mat, min, max, null, null, mask);
            return joinMinMax(min, max);
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    public static double[] minMax(Mat source, Mat mask) {
        Mat mat = source;
        try {
            mat = OTools.toMonoIfNot(mat);
            // - multi-channel matrices are not supported by minMaxLoc
            double[] min = new double[mat.channels()];
            double[] max = new double[mat.channels()];
            opencv_core.minMaxLoc(mat, min, max, null, null, mask);
            return joinMinMax(min, max);
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    // Actually too complex function: in normal situation both arrays are double[1]
    private static double[] joinMinMax(double[] min, double[] max) {
        final double[] result = new double[min.length + max.length];
        System.arraycopy(min, 0, result, 0, min.length);
        System.arraycopy(max, 0, result, min.length, max.length);
        return result;
    }
}
