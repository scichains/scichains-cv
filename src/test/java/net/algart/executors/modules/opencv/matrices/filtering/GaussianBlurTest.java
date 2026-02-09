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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.data.SMat;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import java.io.File;
import java.io.IOException;

public final class GaussianBlurTest {
    private static void test(File sourceFile, File targetFile) throws IOException {
        SMat image = O2SMat.toSMat(opencv_imgcodecs.imread(sourceFile.getPath()));
        GaussianBlur.blurIמPlace(image, 45, false);
        opencv_imgcodecs.imwrite(targetFile.toString(), O2SMat.toMat(image));
    }

    public static void main(String[] args) throws IOException {
        System.setProperty(OTools.USE_GPU_PROPERTY_NAME, "false");
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image target_image%n", Median.class);
            return;
        }
        final File sourceFile = new File(args[0]);
        final File targetFile = new File(args[1]);
        test(sourceFile, targetFile);
        System.gc();
    }
}
