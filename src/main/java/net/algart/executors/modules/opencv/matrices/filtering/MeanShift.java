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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OInterpolation;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_core.UMat;

public final class MeanShift extends UMatFilter {
    private double spatialRadius = 15;
    private double colorRadius = 0.1;
    private int maxPyramidLevel = 0;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.0;

    public MeanShift() {
        setStretchingInterpolation(OInterpolation.INTER_LINEAR);
    }

    public double getSpatialRadius() {
        return spatialRadius;
    }

    public void setSpatialRadius(double spatialRadius) {
        this.spatialRadius = positive(spatialRadius);
    }

    public double getColorRadius() {
        return colorRadius;
    }

    public void setColorRadius(double colorRadius) {
        this.colorRadius = positive(colorRadius);
    }

    public int getMaxPyramidLevel() {
        return maxPyramidLevel;
    }

    public void setMaxPyramidLevel(int maxPyramidLevel) {
        this.maxPyramidLevel = nonNegative(maxPyramidLevel);
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public void setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public void setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
    }

    @Override
    public Mat process(Mat source) {
        logDebug(() -> "MeanShift segmentation: spatialRadius = " + spatialRadius
            + ", colorRadius = " + colorRadius
            + " (source: " + source + ")");
        final Mat result = new Mat();
        final TermCriteria termCriteria = OTools.termCriteria(
                terminationMaxCount, terminationEpsilon, true);
        try {
            if (source.channels() == 1) {
                // MeanShift filter does not work with grayscale images
                opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_GRAY2BGR);
            }
            opencv_imgproc.pyrMeanShiftFiltering(
                source,
                result,
                spatialRadius,
                colorRadius * OTools.maxPossibleValue(source),
                maxPyramidLevel,
                termCriteria);
        } finally {
            if (termCriteria != null) {
                termCriteria.close();
            }
        }
        return result;
    }

    @Override
    public UMat process(UMat source) {
        logDebug(() -> "MeanShift segmentation (GPU): spatialRadius = " + spatialRadius
                + ", colorRadius = " + colorRadius
                + " (source: " + source + ")");
        UMat result = new UMat();
        TermCriteria termCriteria = null;
        try {
            if (terminationMaxCount != 0 || terminationEpsilon != 0.0) {
                int type = (terminationMaxCount != 0 ? TermCriteria.COUNT : 0)
                        + (terminationEpsilon != 0.0 ? TermCriteria.EPS : 0);
                termCriteria = new TermCriteria(type, terminationMaxCount, terminationEpsilon);
            }
            if (source.channels() == 1) {
                // MeanShift filter does not work with grayscale images
                opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_GRAY2BGR);
            }
            opencv_imgproc.pyrMeanShiftFiltering(
                    source,
                    result,
                    spatialRadius,
                    colorRadius * OTools.maxPossibleValue(source),
                    maxPyramidLevel,
                    termCriteria);
        } finally {
            if (termCriteria != null) {
                termCriteria.close();
            }
        }
        return result;
    }
}
