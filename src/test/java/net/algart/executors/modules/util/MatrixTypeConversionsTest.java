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

package net.algart.executors.modules.util;

import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.io.MatrixIO;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class MatrixTypeConversionsTest {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.printf("Usage: %s source_image_file%n", MatrixTypeConversionsTest.class.getName());
            return;
        }
        Path sourceFile = Paths.get(args[0]);
        MultiMatrix2D multiMatrix = MultiMatrix.valueOf2DRGBA(MatrixIO.readImage(sourceFile));
        System.out.printf("Loaded %s%n", multiMatrix);
        final SMat m = SMat.valueOf(multiMatrix);

        multiMatrix = m.toMultiMatrix2D();
        System.out.printf("-> port -> multi-matrix: %s%n", multiMatrix);
        MatrixIO.writeImage(
            Paths.get(sourceFile + ".port2mm.png"), multiMatrix.allChannelsInRGBAOrder());

        final Mat mat = O2SMat.toMat(m);
        System.out.printf("-> port -> mat: %s%n", multiMatrix);
        opencv_imgcodecs.imwrite(sourceFile +".port2mat.png", mat);

        BufferedImage bufferedImage = m.toBufferedImage();
        System.out.printf("-> port -> BufferedImage: %s%n", multiMatrix);
        ImageIO.write(bufferedImage, "png", new File(sourceFile + ".port2bb.png"));

        SMat tempPort = SMat.valueOf(bufferedImage);
        multiMatrix = tempPort.toMultiMatrix2D();
        System.out.printf("-> port -> BufferedImage -> port -> multi-matrix: %s%n", multiMatrix);
        MatrixIO.writeImage(
            Paths.get(sourceFile + ".bb2port2mm.png"), multiMatrix.allChannelsInRGBAOrder());
    }
}
