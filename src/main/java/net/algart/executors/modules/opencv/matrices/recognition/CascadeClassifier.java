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

package net.algart.executors.modules.opencv.matrices.recognition;

import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.opencv.common.UMatToNumbers;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Point;

import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CascadeClassifier extends UMatToNumbers {
    public static final String VISUAL_RESULTS = "visual_results";

    public enum ResultMarkerType {
        RECTANGLE,
        ELLIPSE,
        CIRCLE
    }

    private String classifierFile = "";
    private boolean relativizePath = false;
    private double scaleFactor = 1.1;
    private int minNeighbors = 3;
    private int minSizeX = 0;
    private int minSizeY = 0;
    private int maxSizeX = 0;
    private int maxSizeY = 0;
    private boolean finsOnlyCenters = false;
    private boolean drawOnInput = false;
    private ResultMarkerType resultMarkerType = ResultMarkerType.RECTANGLE;
    private int drawingLineThickness = 0;
    // - zero means filling ellipse

    public CascadeClassifier() {
        addOutputMat(VISUAL_RESULTS);
    }

    public String getClassifierFile() {
        return classifierFile;
    }

    public void setClassifierFile(String classifierFile) {
        this.classifierFile = nonNull(classifierFile);
    }

    public boolean isRelativizePath() {
        return relativizePath;
    }

    public CascadeClassifier setRelativizePath(boolean relativizePath) {
        this.relativizePath = relativizePath;
        return this;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = positive(scaleFactor);
    }

    public int getMinNeighbors() {
        return minNeighbors;
    }

    public void setMinNeighbors(int minNeighbors) {
        this.minNeighbors = positive(minNeighbors);
    }

    public int getMinSizeX() {
        return minSizeX;
    }

    public void setMinSizeX(int minSizeX) {
        this.minSizeX = nonNegative(minSizeX);
    }

    public int getMinSizeY() {
        return minSizeY;
    }

    public void setMinSizeY(int minSizeY) {
        this.minSizeY = nonNegative(minSizeY);
    }

    public int getMaxSizeX() {
        return maxSizeX;
    }

    public void setMaxSizeX(int maxSizeX) {
        this.maxSizeX = nonNegative(maxSizeX);
    }

    public int getMaxSizeY() {
        return maxSizeY;
    }

    public void setMaxSizeY(int maxSizeY) {
        this.maxSizeY = nonNegative(maxSizeY);
    }

    public boolean isFinsOnlyCenters() {
        return finsOnlyCenters;
    }

    public void setFinsOnlyCenters(boolean finsOnlyCenters) {
        this.finsOnlyCenters = finsOnlyCenters;
    }

    public boolean isDrawOnInput() {
        return drawOnInput;
    }

    public void setDrawOnInput(boolean drawOnInput) {
        this.drawOnInput = drawOnInput;
    }

    public ResultMarkerType getResultMarkerType() {
        return resultMarkerType;
    }

    public void setResultMarkerType(ResultMarkerType resultMarkerType) {
        this.resultMarkerType = resultMarkerType;
    }

    public int getDrawingLineThickness() {
        return drawingLineThickness;
    }

    public void setDrawingLineThickness(int drawingLineThickness) {
        this.drawingLineThickness = nonNegative(drawingLineThickness);
    }

    @Override
    public SNumbers analyse(Mat source) {
        final String file = nonEmpty(this.classifierFile, "classifier file name");
        Path path = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(file, this);
        path = FileOperation.simplifyOSPath(path, relativizePath);
        final List<Rectangle> rectangles;
        try (org.bytedeco.opencv.opencv_objdetect.CascadeClassifier classifier =
                     new org.bytedeco.opencv.opencv_objdetect.CascadeClassifier(path.toString())) {
            rectangles = classify(classifier, source);
            logDebug(() -> "Cascade classifier found " + rectangles.size() + " objects"
                    + " (source: " + source + ")");
        }
        if (isOutputNecessary(VISUAL_RESULTS)) {
            final Mat mask = drawOnInput ?
                    source.clone() :
                    new Mat(source.rows(), source.cols(), opencv_core.CV_8UC1);
            try (Scalar color = new Scalar(255, 255, 255, 255)) {
                drawRectangles(mask, rectangles, color);
            }
            O2SMat.setTo(getMat(VISUAL_RESULTS), mask);
        }
        return makeResult(rectangles);
    }

    @Override
    public SNumbers analyse(UMat source) {
        final String file = nonEmpty(this.classifierFile, "classifier file name");
        Path path = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(file, this);
        path = FileOperation.simplifyOSPath(path, relativizePath);
        final List<Rectangle> rectangles;
        try (org.bytedeco.opencv.opencv_objdetect.CascadeClassifier classifier =
                     new org.bytedeco.opencv.opencv_objdetect.CascadeClassifier(path.toString())) {
            rectangles = classify(classifier, source);
            logDebug(() -> "Cascade classifier (GPU) found " + rectangles.size() + " objects"
                    + " (source: " + source + ")");
        }
        if (isOutputNecessary(VISUAL_RESULTS)) {
            final Mat mask = drawOnInput ?
                    OTools.toMat(source) :
                    new Mat(source.rows(), source.cols(), opencv_core.CV_8UC1);
            try (Scalar color = new Scalar(255, 255, 255, 255)) {
                drawRectangles(mask, rectangles, color);
            }
            O2SMat.setTo(getMat(VISUAL_RESULTS), mask);
        }
        return makeResult(rectangles);
    }

    @Override
    public String visibleOutputPortName() {
        return VISUAL_RESULTS;
    }

    private List<Rectangle> classify(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier classifier, Mat mat) {
        final List<Rectangle> result = new ArrayList<>();
        try (
                Size minSize = minSizeX == 0 || minSizeY == 0 ?
                        new Size() : new Size(minSizeX, minSizeY);
                Size maxSize = maxSizeX == 0 || maxSizeY == 0 ?
                        new Size() : new Size(maxSizeX, maxSizeY);
                RectVector rectVector = new RectVector()) {
            classifier.detectMultiScale(mat, rectVector, scaleFactor, minNeighbors, 0, minSize, maxSize);
            for (long k = 0, n = rectVector.size(); k < n; k++) {
                final Rect r = rectVector.get(k);
                result.add(new Rectangle(r.x(), r.y(), r.width(), r.height()));
            }
            return result;
        }
    }

    private List<Rectangle> classify(org.bytedeco.opencv.opencv_objdetect.CascadeClassifier classifier, UMat mat) {
        final List<Rectangle> result = new ArrayList<>();
        try (
                Size minSize = minSizeX == 0 || minSizeY == 0 ?
                        new Size() : new Size(minSizeX, minSizeY);
                Size maxSize = maxSizeX == 0 || maxSizeY == 0 ?
                        new Size() : new Size(maxSizeX, maxSizeY);
                RectVector rectVector = new RectVector()) {
            classifier.detectMultiScale(mat, rectVector, scaleFactor, minNeighbors, 0, minSize, maxSize);
            for (long k = 0, n = rectVector.size(); k < n; k++) {
                final Rect r = rectVector.get(k);
                result.add(new Rectangle(r.x(), r.y(), r.width(), r.height()));
            }
            return result;
        }
    }

    private SNumbers makeResult(List<Rectangle> rectangles) {
        final int resultBlockLength = finsOnlyCenters ? 2 : 4;
        int[] result = new int[resultBlockLength * rectangles.size()];
        for (int k = 0, n = rectangles.size(), disp = 0; k < n; k++) {
            Rectangle r = rectangles.get(k);
            result[disp++] = r.x + r.width / 2;
            result[disp++] = r.y + r.height / 2;
            if (!finsOnlyCenters) {
                result[disp++] = r.width;
                result[disp++] = r.height;
            }
        }
        return SNumbers.valueOfArray(result, resultBlockLength);
    }

    private void drawRectangles(Mat mat, List<Rectangle> rectangles, Scalar color) {
        for (Rectangle r : rectangles) {
            try (Rect rect = new Rect(r.x, r.y, r.width, r.height);
                 Point center = new Point(r.x + r.width / 2, r.y + r.height / 2);
                 Size sizes = new Size(r.width / 2, r.height / 2)) {
                switch (resultMarkerType) {
                    case RECTANGLE:
                        opencv_imgproc.rectangle(
                                mat,
                                rect,
                                color,
                                drawingLineThickness == 0 ? -1 : drawingLineThickness,
                                8, 0); // 8 = opencv_imgproc.LINE_8 in new JavaCPP
                        break;
                    case CIRCLE:
                        opencv_imgproc.circle(
                                mat,
                                center,
                                Math.min(r.width / 2, r.height / 2),
                                color,
                                drawingLineThickness == 0 ? -1 : drawingLineThickness,
                                16, 0); // opencv_imgproc.LINE_AA in new JavaCPP
                        break;
                    case ELLIPSE:
                        opencv_imgproc.ellipse(
                                mat,
                                center,
                                sizes,
                                0.0, 0.0, 360.0,
                                color,
                                drawingLineThickness == 0 ? -1 : drawingLineThickness,
                                16, 0); // opencv_imgproc.LINE_AA in new JavaCPP
                        break;
                }
            }
        }
    }
}
