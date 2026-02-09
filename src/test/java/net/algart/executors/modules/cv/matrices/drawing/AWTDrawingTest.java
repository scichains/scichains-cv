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

package net.algart.executors.modules.cv.matrices.drawing;

import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.matrices.drawing.DrawEllipse;
import net.algart.executors.modules.core.matrices.drawing.DrawRectangle;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.io.MatrixIO;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class AWTDrawingTest {
    private static final boolean BUFFERED_IMAGE_FROM_SMAT = true;

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.printf("Usage: %s resultFile.png%n", AWTDrawingTest.class.getName());
            return;
        }
        final String fileName = args[0];
        final DrawEllipse drawEllipse = new DrawEllipse().setWidth(150).setHeight(100).setX(200).setY(200);
        drawEllipse.setColor(new Color(0x808080));
        BufferedImage bufferedImage = new BufferedImage(500, 500, BufferedImage.TYPE_BYTE_GRAY);
        System.out.printf("bufferedImage:        %s%n  (%s: %s)%n", bufferedImage,
                bufferedImage.getColorModel().getClass().getName(), bufferedImage.getColorModel());
        if (BUFFERED_IMAGE_FROM_SMAT) {
            SMat image = SMat.of(bufferedImage);
            image = new DrawRectangle()
                    .setColor("#808080").setLeft(0).setTop(0).setWidth(100).setHeight(100)
                    .process(image);
            System.out.printf("Writing %s%n", fileName + ".opencv_rect.png");
            opencv_imgcodecs.imwrite(fileName + ".opencv_rect.png", O2SMat.toMat(image));
            bufferedImage = image.toBufferedImage();
            // - BufferedImage raster type is changed here
            System.out.printf("New bufferedImage:    %s%n  (%s: %s)%n", bufferedImage,
                    bufferedImage.getColorModel().getClass().getName(), bufferedImage.getColorModel());
        }
        final BufferedImage result = drawEllipse.process(bufferedImage);
        System.out.printf("Result bufferedImage: %s%n", bufferedImage);
        System.out.printf("Corner pixel in BufferedImage (getRGB): 0x%X%n", bufferedImage.getRGB(0, 0));
        System.out.printf("Center pixel in BufferedImage (getRGB): 0x%X%n",
                bufferedImage.getRGB((int) drawEllipse.getX(), (int) drawEllipse.getY()));
        System.out.printf("Writing %s%n", fileName);
        ImageIO.write(result, MatrixIO.extension(fileName, "png"), new File(fileName));
        SMat image = SMat.of(bufferedImage);
        System.out.printf("Corner pixel in MultiMatrix (getPixel): %s%n", image.toMultiMatrix2D().getPixel(0, 0));
        System.out.printf("Center pixel in MultiMatrix (getPixel): %s%n",
                image.toMultiMatrix2D().getPixel((long) drawEllipse.getX(), (long) drawEllipse.getY()));
        System.out.printf("Writing %s%n", fileName + ".opencv.png");
        opencv_imgcodecs.imwrite(fileName + ".opencv.png", O2SMat.toMat(image));

        bufferedImage = ImageIO.read(new File(fileName));
        System.out.printf("Loaded bufferedImage: %s%n  (%s: %s)%n", bufferedImage,
                bufferedImage.getColorModel().getClass().getName(), bufferedImage.getColorModel());
        System.out.printf("Corner pixel in BufferedImage (getRGB): 0x%X%n", bufferedImage.getRGB(0, 0));
        System.out.printf("Center pixel in BufferedImage (getRGB): 0x%X%n",
                bufferedImage.getRGB((int) drawEllipse.getX(), (int) drawEllipse.getY()));
    }
}
