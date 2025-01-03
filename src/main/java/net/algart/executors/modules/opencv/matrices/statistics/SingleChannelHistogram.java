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

import net.algart.executors.modules.opencv.common.UMatToNumbers;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class SingleChannelHistogram extends UMatToNumbers implements ReadOnlyExecutionInput {
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_HISTOGRAM = "histogram";

    private static Mat EMPTY_MAT = new Mat();
    private static UMat EMPTY_UMAT = new UMat();

    private int channelIndex = 0;
    private int histogramSize = 256;
    private double min = 0.0;
    private double max = 256.0;

    public SingleChannelHistogram() {
        addInputMat(INPUT_MASK);
        setDefaultOutputNumbers(OUTPUT_HISTOGRAM);
    }

    public int getChannelIndex() {
        return channelIndex;
    }

    public SingleChannelHistogram setChannelIndex(int channelIndex) {
        this.channelIndex = nonNegative(channelIndex);
        return this;
    }

    public int getHistogramSize() {
        return histogramSize;
    }

    public SingleChannelHistogram setHistogramSize(int histogramSize) {
        this.histogramSize = positive(histogramSize);
        return this;
    }

    public double getMin() {
        return min;
    }

    public SingleChannelHistogram setMin(double min) {
        this.min = min;
        return this;
    }

    public double getMax() {
        return max;
    }

    public SingleChannelHistogram setMax(double max) {
        this.max = max;
        return this;
    }

    @Override
    public SNumbers analyse(Mat source) {
        return analyse(
                source,
                O2SMat.toMat(getInputMat(INPUT_MASK, true), true));
    }

    @Override
    public SNumbers analyse(UMat source) {
        return analyse(
                source,
                O2SMat.toUMat(getInputMat(INPUT_MASK, true), true));
    }

    public SNumbers analyse(Mat source, Mat mask) {
        if (mask == null) {
            mask = EMPTY_MAT;
        }
        try (Mat hist = new Mat()) {
            opencv_imgproc.calcHist(
                    source,
                    1,
                    new int[]{channelIndex},
                    mask,
                    hist,
                    1,
                    new int[]{histogramSize},
                    new float[]{(float) min, (float) max},
                    true,
                    false);
            return O2SMat.toRawNumbers(hist, 1);
        }
    }

    public SNumbers analyse(UMat source, UMat mask) {
        if (mask == null) {
            mask = EMPTY_UMAT;
        }
        IntBuffer channels = IntBuffer.wrap(new int[]{channelIndex});
        IntBuffer histSize = IntBuffer.wrap(new int[]{histogramSize});
        FloatBuffer ranges = FloatBuffer.wrap(new float[]{(float) min, (float) max});
        try (UMatVector matVector = new UMatVector(source);
             UMat hist = new UMat()) {
            opencv_imgproc.calcHist(
                    matVector,
                    channels,
                    mask,
                    hist,
                    histSize,
                    ranges);
            return O2SMat.toRawNumbers(hist, 1);
        }
    }
}
