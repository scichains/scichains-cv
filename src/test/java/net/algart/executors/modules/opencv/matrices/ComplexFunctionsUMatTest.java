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

package net.algart.executors.modules.opencv.matrices;

import net.algart.executors.modules.opencv.matrices.filtering.GuidedFilter;
import net.algart.executors.modules.opencv.matrices.segmentation.SuperpixelSLIC;
import net.algart.executors.modules.util.opencv.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class ComplexFunctionsUMatTest {
    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("OpenCL existence: " + opencv_core.haveOpenCL());
        System.out.println("OpenCL usage: " + opencv_core.useOpenCL());
        System.out.println("OpenCL enabled: " + OTools.isGPUOptimizationEnabled());
        if (args.length == 0) {
            System.out.printf("Usage: %s source_image%n", ComplexFunctionsUMatTest.class.getName());
            return;
        }
        final String sourceFile = args[0];
        if (!Files.exists(Paths.get(sourceFile))) {
            throw new FileNotFoundException(sourceFile);
        }
        for (int testCount = 0; testCount < 10; testCount++) {
            final Mat m = opencv_imgcodecs.imread(sourceFile);
            final UMat u = OTools.toUMat(m);
            System.out.println("Test #" + testCount);

            long t1 = System.nanoTime();
            Mat mr = new GuidedFilter().process(new Mat[]{m, null});
            long t2 = System.nanoTime();
            System.out.printf("GuidedFilter for Mat: %.3f ms for %s%n", (t2 - t1) * 1e-6, OTools.toString(u));

            t1 = System.nanoTime();
            UMat ur = new GuidedFilter().process(new UMat[]{u, null});
            t2 = System.nanoTime();
            System.out.printf("GuidedFilter for UMat: %.3f ms for %s%n", (t2 - t1) * 1e-6, OTools.toString(u));

            opencv_imgcodecs.imwrite(sourceFile + ".GuidedFilter.bmp", OTools.toMat(ur));

            t1 = System.nanoTime();
            mr = new SuperpixelSLIC().process(m);
            t2 = System.nanoTime();
            System.out.printf("SuperpixelSLIC for Mat: %.3f ms for %s%n", (t2 - t1) * 1e-6, OTools.toString(u));

            t1 = System.nanoTime();
//            ur = new SuperpixelSLIC().process(u); // leads to exception
            t2 = System.nanoTime();
            System.out.printf("SuperpixelSLIC for UMat: %.3f ms for %s%n", (t2 - t1) * 1e-6, OTools.toString(u));

            opencv_imgcodecs.imwrite(sourceFile + ".SuperpixelSLIC.bmp", OTools.toMat(ur));
        }
    }
}
