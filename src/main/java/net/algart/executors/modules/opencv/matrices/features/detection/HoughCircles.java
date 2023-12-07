/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.modules.opencv.common.MatToNumbers;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OHoughMode;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_imgproc.Vec4fVector;

public final class HoughCircles extends MatToNumbers {
    public static final String OUTPUT_CIRCLES = "circles";
    public static final String OUTPUT_VOTES = "votes";

    private OHoughMode method = OHoughMode.HOUGH_GRADIENT;
    // - note: only HOUGH_GRADIENT and HOUGH_GRADIENT_ALT are supported in OpenCV 4.4.0
    private double dp = 1.0;
    private double minDist = 10.0;
    private double param1 = 100.0;
    private double param2 = 20.0;
    private int minRadius = 0;
    private int maxRadius = 0;

    public HoughCircles() {
        setDefaultOutputNumbers(OUTPUT_CIRCLES);
        addOutputNumbers(OUTPUT_VOTES);
    }

    public OHoughMode getMethod() {
        return method;
    }

    public double getDp() {
        return dp;
    }

    public HoughCircles  setDp(double dp) {
        this.dp = dp;
        return this;
    }

    public HoughCircles setMethod(OHoughMode method) {
        this.method = nonNull(method);
        return this;
    }

    public double getMinDist() {
        return minDist;
    }

    public HoughCircles  setMinDist(double minDist) {
        this.minDist = minDist;
        return this;
    }

    public double getParam1() {
        return param1;
    }

    public HoughCircles  setParam1(double param1) {
        this.param1 = param1;
        return this;
    }

    public double getParam2() {
        return param2;
    }

    public HoughCircles  setParam2(double param2) {
        this.param2 = param2;
        return this;
    }

    public int getMinRadius() {
        return minRadius;
    }

    public HoughCircles  setMinRadius(int minRadius) {
        this.minRadius = nonNegative(minRadius);
        return this;
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public HoughCircles  setMaxRadius(int maxRadius) {
        this.maxRadius = maxRadius;
        return this;
    }

    @Override
    public SNumbers analyse(Mat source) {
        Mat mat = source;
        try {
            mat = OTools.toMono8UIfNot(mat);
            try (Vec4fVector circlesVector = new Vec4fVector()) {
                opencv_imgproc.HoughCircles(
                        mat,
                        circlesVector,
                        method.code(),
                        dp,
                        minDist,
                        param1,
                        param2,
                        minRadius,
                        maxRadius);
                final float[] circlesAndVotes = OTools.toFloatArray(circlesVector);
                for (int disp = 0; disp < circlesAndVotes.length; disp += 4) {
                    circlesAndVotes[disp + 2] *= 2.0;
                    // - replace radius with diameter
                }
                final SNumbers result = SNumbers.valueOfArray(circlesAndVotes, 4);
                getNumbers(OUTPUT_VOTES).exchange(result.columnRange(3 ,1));
                return result.columnRange(0, 3);
            }
        } finally {
            if (mat != source) {
                mat.close();
            }
        }
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }
}
