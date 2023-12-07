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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

import java.awt.*;

public abstract class AbstractSegmentationWithBoundaries extends UMatFilter {
    public static final String OUTPUT_BOUNDARIES = "boundaries";

    DrawingBoundariesStyle drawingBoundariesStyle = DrawingBoundariesStyle.BOUNDARIES;
    private Color drawingBoundariesColor = Color.WHITE;
    private boolean visibleBoundaries = false;

    public AbstractSegmentationWithBoundaries() {
        addOutputMat(OUTPUT_BOUNDARIES);
    }

    public DrawingBoundariesStyle getDrawingBoundariesStyle() {
        return drawingBoundariesStyle;
    }

    public void setDrawingBoundariesStyle(DrawingBoundariesStyle drawingBoundariesStyle) {
        this.drawingBoundariesStyle = nonNull(drawingBoundariesStyle);
    }

    public Color getDrawingBoundariesColor() {
        return drawingBoundariesColor;
    }

    public void setDrawingBoundariesColor(Color drawingBoundariesColor) {
        this.drawingBoundariesColor = nonNull(drawingBoundariesColor);
    }

    public boolean isVisibleBoundaries() {
        return visibleBoundaries;
    }

    public void setVisibleBoundaries(boolean visibleBoundaries) {
        this.visibleBoundaries = visibleBoundaries;
    }

    @Override
    public String visibleOutputPortName() {
        return visibleBoundaries ? OUTPUT_BOUNDARIES : super.visibleOutputPortName();
    }

    public void setToOutputBoundaries(
            BoundariesExtractor boundariesExtractor,
            Mat source,
            Mat labels) {
        if (isBoundariesRequested()) {
            setEndProcessingTimeStamp();
            Mat boundaries = new Mat();
            boundariesExtractor.findBoundaries(boundaries, labels, drawingBoundariesStyle.thickBoundaries);
            final Mat drawnBoundaries = drawingBoundariesStyle.drawOnSourceIfRequested(
                    boundaries, source, drawingBoundariesColor);
            if (drawnBoundaries != boundaries) {
                boundaries.close();
                boundaries = drawnBoundaries;
            }
            final Mat stretchedBoundaries = stretchToOriginal(boundaries);
            if (stretchedBoundaries != boundaries) {
                boundaries.close();
            }
            boundaries = stretchedBoundaries;
            if (drawingBoundariesStyle.binary) {
                getMat(OUTPUT_BOUNDARIES).setTo(O2SMat.toBinaryMatrix(boundaries));
            } else {
                O2SMat.setTo(getMat(OUTPUT_BOUNDARIES), boundaries);
            }
        }
    }

    public void setToOutputBoundaries(
            BoundariesExtractor boundariesExtractor,
            UMat source,
            UMat labels) {
        if (isBoundariesRequested()) {
            try (Mat sourceMat = OTools.toMat(source);
                 Mat labelsMat = OTools.toMat(labels)) {
                setToOutputBoundaries(boundariesExtractor, sourceMat, labelsMat);
            }
        }
    }

    private boolean isBoundariesRequested() {
        return hasOutputPort(OUTPUT_BOUNDARIES) && (visibleBoundaries || isOutputNecessary(OUTPUT_BOUNDARIES));

    }
}
