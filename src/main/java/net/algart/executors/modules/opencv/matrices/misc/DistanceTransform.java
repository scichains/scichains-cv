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

package net.algart.executors.modules.opencv.matrices.misc;

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.enums.ODepth;
import net.algart.executors.modules.opencv.util.enums.ODistanceLabelType;
import net.algart.executors.modules.opencv.util.enums.ODistanceType;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class DistanceTransform extends VoidResultUMatFilter {
    public static final String OUTPUT_LABELS = "labels";

    private ODistanceType distanceType = ODistanceType.DIST_L2;
    private int maskSize = 3;
    private ODepth resultDepth = ODepth.CV_32F;
    private ODistanceLabelType labelType = ODistanceLabelType.DIST_LABEL_CCOMP;

    public DistanceTransform() {
        addOutputMat(OUTPUT_LABELS);
    }

    public ODistanceType getDistanceType() {
        return distanceType;
    }

    public void setDistanceType(ODistanceType distanceType) {
        this.distanceType = nonNull(distanceType);
    }

    public int getMaskSize() {
        return maskSize;
    }

    public void setMaskSize(int maskSize) {
        this.maskSize = nonNegative(maskSize);
    }

    public ODepth getResultDepth() {
        return resultDepth;
    }

    public void setResultDepth(ODepth resultDepth) {
        this.resultDepth = nonNull(resultDepth);
    }

    public ODistanceLabelType getLabelType() {
        return labelType;
    }

    public void setLabelType(ODistanceLabelType labelType) {
        this.labelType = nonNull(labelType);
    }

    @Override
    public void process(Mat result, Mat source) {
        if (isOutputNecessary(OUTPUT_LABELS)) {
            logDebug(() -> "Distance transform with labels:"
                + " distanceType = " + distanceType + ", maskSize = " + maskSize
                + ", labelType = " + labelType + " (source: " + source + ")");
            Mat labels = new Mat();
            opencv_imgproc.distanceTransformWithLabels(
                source, result, labels, distanceType.code(), maskSize, labelType.code());
            setEndProcessingTimeStamp();
            O2SMat.setTo(getMat(OUTPUT_LABELS), labels);
        } else{
            logDebug(() -> "Distance transform without labels:"
                + " distanceType = " + distanceType + ", maskSize = " + maskSize
                + ", resultDepth = " + resultDepth + " (source: " + source + ")");
            opencv_imgproc.distanceTransform(source, result, distanceType.code(), maskSize, resultDepth.code());
        }
    }

    @Override
    public void process(UMat result, UMat source) {
        if (isOutputNecessary(OUTPUT_LABELS)) {
            logDebug(() -> "Distance transform with labels (GPU):"
                    + " distanceType = " + distanceType + ", maskSize = " + maskSize
                    + ", labelType = " + labelType + " (source: " + source + ")");
            UMat labels = new UMat();
            opencv_imgproc.distanceTransformWithLabels(
                    source, result, labels, distanceType.code(), maskSize, labelType.code());
            setEndProcessingTimeStamp();
            O2SMat.setTo(getMat(OUTPUT_LABELS), labels);
        } else{
            logDebug(() -> "Distance transform without labels (GPU):"
                    + " distanceType = " + distanceType + ", maskSize = " + maskSize
                    + ", resultDepth = " + resultDepth + " (source: " + source + ")");
            opencv_imgproc.distanceTransform(source, result, distanceType.code(), maskSize, resultDepth.code());
        }
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }
}
