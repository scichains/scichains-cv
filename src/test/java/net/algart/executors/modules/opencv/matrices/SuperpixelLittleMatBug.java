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

package net.algart.executors.modules.opencv.matrices;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_ximgproc.SuperpixelSEEDS;
import org.bytedeco.opencv.opencv_ximgproc.SuperpixelSLIC;

// See https://github.com/opencv/opencv_contrib/issues/2023
public final class SuperpixelLittleMatBug {
    public static void main(String[] args) {
        opencv_core.setUseOpenCL(true);
        if (args.length < 4) {
            System.out.printf("Usage: %s source_image target_image width height%n",
                    SuperpixelLittleMatBug.class.getName());
            return;
        }
        final String sourceFile = args[0];
        final String targetFile = args[1];
        final int width = Integer.parseInt(args[2]);
        final int height = Integer.parseInt(args[3]);
        final Mat source = opencv_imgcodecs.imread(sourceFile);
        final Size size = new Size(width, height);
        opencv_imgproc.resize(source, source, size);

        Mat result = new Mat();
        if (true) {
            System.out.println("Creating SuperpixelSEEDS...");
            final SuperpixelSEEDS superpixelSEEDS = opencv_ximgproc.createSuperpixelSEEDS(
                    source.cols(),
                    source.rows(),
                    source.channels(),
                    100,
                    4,
                    4,
                    5,
                    false);
            System.out.println("Iterating...");
            superpixelSEEDS.iterate(source, 4);
            System.out.println("Getting controur mask...");
            superpixelSEEDS.getLabelContourMask(result, true);
        } else {
            System.out.println("Creating createSuperpixelSLIC...");
            final SuperpixelSLIC superpixelSLIC = opencv_ximgproc.createSuperpixelSLIC(
                    source, opencv_ximgproc.SLICO, 10, 10.0f);
            System.out.println("Iterating...");
            superpixelSLIC.iterate(10);
            System.out.println("Getting controur mask...");
            superpixelSLIC.getLabelContourMask(result, true);
        }

        System.out.println("Writing...");
        opencv_imgcodecs.imwrite(targetFile, result);
    }
}
