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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import net.algart.executors.modules.opencv.util.enums.OMorphShape;
import net.algart.executors.modules.opencv.util.enums.OMorphType;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

public final class Morphology extends UMatFilter {
    private OMorphType operation = OMorphType.DILATE;
    private OMorphShape patternShape = OMorphShape.CIRCLE;
    private int patternSize = 15;
    private int numberOfIterations = 1;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public OMorphType getOperation() {
        return operation;
    }

    public Morphology setOperation(OMorphType operation) {
        this.operation = nonNull(operation);
        return this;
    }

    public OMorphShape getPatternShape() {
        return patternShape;
    }

    public Morphology setPatternShape(OMorphShape patternShape) {
        this.patternShape = nonNull(patternShape);
        return this;
    }

    public int getPatternSize() {
        return patternSize;
    }

    public Morphology setPatternSize(int patternSize) {
        this.patternSize = nonNegative(patternSize);
        return this;
    }

    public int getNumberOfIterations() {
        return numberOfIterations;
    }

    public Morphology setNumberOfIterations(int numberOfIterations) {
        this.numberOfIterations = positive(numberOfIterations);
        return this;
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public Morphology setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
        return this;
    }

    @Override
    public Mat process(Mat source) {
        logDebug(() -> "Morphology " + operation + " with shape " + patternShape
                + " " + patternSize + "x" + patternSize + " (source: " + source + ")");
        if (patternSize > 0) {
            try (Size size = new Size(patternSize, patternSize)) {
                try (Mat element = opencv_imgproc.getStructuringElement(patternShape.code(), size)) {
                    opencv_imgproc.morphologyEx(source, source, operation.code(), element,
                            null, numberOfIterations, borderType.code(), null);
                }
            }
        }
        return source;
    }

    @Override
    public UMat process(UMat source) {
        logDebug(() -> "Morphology " + operation + " (GPU) with shape " + patternShape
                + " " + patternSize + "x" + patternSize + " (source: " + source + ")");
        if (patternSize > 0) {
            try (Size size = new Size(patternSize, patternSize)) {
                try (Mat element = opencv_imgproc.getStructuringElement(patternShape.code(), size)) {
                    try (UMat elementUMat = OTools.toUMat(element)) {
                        opencv_imgproc.morphologyEx(source, source, operation.code(), elementUMat,
                                null, numberOfIterations, borderType.code(), null);
                    }
                }
            }
        }
        return source;
    }

    @Override
    protected boolean allowInputPackedBits() {
        return true;
    }
}
