/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.opencv.matrices.recognition;

import net.algart.executors.modules.opencv.common.VoidResultTwoUMatFilter;
import net.algart.executors.modules.opencv.util.enums.OTemplateMatchMode;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.UMat;

public final class MatchTemplate extends VoidResultTwoUMatFilter {
    public static final String INPUT_TEMPLATE = "template";
    public static final String OUTPUT_X = "x";
    public static final String OUTPUT_Y = "y";

    private OTemplateMatchMode templateMatchMode = OTemplateMatchMode.TM_SQDIFF;

    public MatchTemplate() {
        super(null, INPUT_TEMPLATE);
        addOutputScalar(OUTPUT_X);
        addOutputScalar(OUTPUT_Y);
    }

    public OTemplateMatchMode getTemplateMatchMode() {
        return templateMatchMode;
    }

    public void setTemplateMatchMode(OTemplateMatchMode templateMatchMode) {
        this.templateMatchMode = nonNull(templateMatchMode);
    }

    @Override
    public void process(Mat result, Mat source, Mat secondMat) {
        opencv_imgproc.matchTemplate(source, secondMat, result, templateMatchMode.code());
        double[] minVal = new double[1];
        double[] maxVal = new double[1];
        try (Point minLocation = new Point();
             Point maxLocation = new Point()) {
            opencv_core.minMaxLoc(result, minVal, maxVal, minLocation, maxLocation, null);
            final double x, y;
            if (templateMatchMode == OTemplateMatchMode.TM_SQDIFF
                    || templateMatchMode == OTemplateMatchMode.TM_SQDIFF_NORMED) {
                x = minLocation.x();
                y = minLocation.y();
            } else {
                x = maxLocation.x();
                y = maxLocation.y();
            }
            getScalar(OUTPUT_X).setTo(x);
            getScalar(OUTPUT_Y).setTo(y);
        }
    }

    @Override
    public void process(UMat result, UMat source, UMat secondMat) {
        opencv_imgproc.matchTemplate(source, secondMat, result, templateMatchMode.code());
        double[] minVal = new double[1];
        double[] maxVal = new double[1];
        try (Point minLocation = new Point();
             Point maxLocation = new Point()) {
            opencv_core.minMaxLoc(result, minVal, maxVal, minLocation, maxLocation, null);
            final double x, y;
            if (templateMatchMode == OTemplateMatchMode.TM_SQDIFF
                    || templateMatchMode == OTemplateMatchMode.TM_SQDIFF_NORMED) {
                x = minLocation.x();
                y = minLocation.y();
            } else {
                x = maxLocation.x();
                y = maxLocation.y();
            }
            getScalar(OUTPUT_X).setTo(x);
            getScalar(OUTPUT_Y).setTo(y);
        }
    }

    @Override
    protected boolean dimensionsEqualityRequired() {
        return false;
    }
}
