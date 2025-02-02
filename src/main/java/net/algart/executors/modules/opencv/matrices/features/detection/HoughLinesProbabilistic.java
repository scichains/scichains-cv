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

package net.algart.executors.modules.opencv.matrices.features.detection;

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.opencv.common.MatToNumbers;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_imgproc.Vec4iVector;

public final class HoughLinesProbabilistic extends MatToNumbers {
    public static final String OUTPUT_LINES = "lines";

    private double rho = 1.0;
    private double thetaInDegree = 1.0;
    private int threshold = 100;
    private double minLineLength = 0.0;
    private double maxLineGap = 0.0;

    public HoughLinesProbabilistic() {
        setDefaultOutputNumbers(OUTPUT_LINES);
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public double getThetaInDegree() {
        return thetaInDegree;
    }

    public void setThetaInDegree(double thetaInDegree) {
        this.thetaInDegree = thetaInDegree;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public double getMinLineLength() {
        return minLineLength;
    }

    public void setMinLineLength(double minLineLength) {
        this.minLineLength = minLineLength;
    }

    public double getMaxLineGap() {
        return maxLineGap;
    }

    public void setMaxLineGap(double maxLineGap) {
        this.maxLineGap = maxLineGap;
    }

    @Override
    public SNumbers analyse(Mat source) {
        // source image may be modified by the function!
        try (
                Vec4iVector linesVector = new Vec4iVector()) {
            opencv_imgproc.HoughLinesP(
                    source,
                    linesVector,
                    rho,
                    Math.toRadians(thetaInDegree),
                    threshold,
                    minLineLength,
                    maxLineGap);
            return SNumbers.ofArray(OTools.toIntArray(linesVector), 4);
        }
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }
}
