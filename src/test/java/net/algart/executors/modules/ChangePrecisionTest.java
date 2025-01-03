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

package net.algart.executors.modules;

import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.matrices.conversions.ChangePrecision;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.io.MatrixIO;
import net.algart.multimatrix.MultiMatrix;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public final class ChangePrecisionTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image elementType%n", ChangePrecisionTest.class.getName());
            return;
        }
        final Path sourceFile = Paths.get(args[0]);
        final String elementType = args[1];
        MultiMatrix image = MultiMatrix.valueOf2DRGBA(MatrixIO.readImage(sourceFile));
        SMat m;
        Mat mat;
        try (ChangePrecision cc = new ChangePrecision()) {
            cc.setElementType(elementType);
            image = cc.process(image);
            MatrixIO.writeImage(Paths.get(sourceFile + "." + elementType + ".aa.png"),
                    image.allChannelsInRGBAOrder());
            m = new SMat();
            m.setTo(image);
            mat = O2SMat.toMat(m);
            System.out.printf("OpenCV mat: %s%n", mat);
            opencv_imgcodecs.imwrite(sourceFile + "." + elementType + ".opencv.png", mat);
            mat.convertTo(mat, opencv_core.CV_8U);
            System.out.printf("OpenCV mat reduced to bytes: %s%n", mat);
            opencv_imgcodecs.imwrite(sourceFile + "." + elementType + ".to_byte.opencv.png", mat);

            cc.setElementType(byte.class);
            image = cc.process(image);
        }
        MatrixIO.writeImage(Paths.get(sourceFile + ".aa.byte.png")
            , image.allChannelsInRGBAOrder());
        m.setTo(image);
        mat = O2SMat.toMat(m);
        System.out.printf("OpenCV byte mat: %s%n", mat);
        opencv_imgcodecs.imwrite(sourceFile + ".byte.opencv.png", mat);
        mat.convertTo(mat, opencv_core.CV_16U);
        System.out.printf("OpenCV short mat: %s%n", mat);
        opencv_imgcodecs.imwrite(sourceFile + ".byte.to_short.opencv.png", mat);
        mat.convertTo(mat, opencv_core.CV_8U);
        System.out.printf("OpenCV byte mat: %s%n", mat);
        opencv_imgcodecs.imwrite(sourceFile + ".byte.to_short.to_byte.opencv.png", mat);
        mat.convertTo(mat, opencv_core.CV_32S);
        System.out.printf("OpenCV int mat: %s%n", mat);
        opencv_imgcodecs.imwrite(sourceFile + ".byte.to_int.opencv.png", mat);
        mat.convertTo(mat, opencv_core.CV_8U);
        System.out.printf("OpenCV byte mat: %s%n", mat);
        opencv_imgcodecs.imwrite(sourceFile + ".byte.to_int.to_byte.opencv.png", mat);
    }
}
