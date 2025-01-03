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

package net.algart.executors.modules.opencv.matrices.video;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_video;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_video.BackgroundSubtractor;

public final class BackgroundSubtractorKNN extends AbstractBackgroundSubtractor {
    private double dist2Threshold = 400;

    public double getDist2Threshold() {
        return dist2Threshold;
    }

    public void setDist2Threshold(double dist2Threshold) {
        this.dist2Threshold = dist2Threshold;
    }

    @Override
    BackgroundSubtractor createBackgroundSubtractor() {
        logDebug(() -> "Creating BackgroundSubtractorKNN: dist2Threshold " + dist2Threshold);
        return opencv_video.createBackgroundSubtractorKNN(500, dist2Threshold, isDetectShadows());
    }

    public static void main(String[] args) {
        final String sourceFile = args[0];
        final String targetFile = args[1];
        final String backgroundFile = args[2];
        final Mat source = opencv_imgcodecs.imread(sourceFile);
        final BackgroundSubtractorKNN subtractor = new BackgroundSubtractorKNN();
        subtractor.requestOutput(OUTPUT_BACKGROUND);
        final Mat target = subtractor.process(source);
        opencv_imgcodecs.imwrite(targetFile, target);
        opencv_imgcodecs.imwrite(backgroundFile, subtractor.getBackground());
    }
}
