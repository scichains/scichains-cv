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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.matrices.scanning.Boundary2DScanner;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.modules.core.matrices.io.ReadImage;
import net.algart.executors.modules.core.matrices.io.WriteImage;
import net.algart.executors.modules.core.matrices.misc.Contrast;

public final class ScanAndMeasureBoundariesTest {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.printf("Usage: %s source_image target_labels_image%n",
                    ScanAndMeasureBoundariesTest.class.getName());
            return;
        }

        double sum1 = 0.0;
        sum1 += 5;
        sum1 += Boundary2DScanner.Step.DIAGONAL_LENGTH;
        sum1 += 3;
        sum1 += Boundary2DScanner.Step.DIAGONAL_LENGTH;
        sum1 += 5;
        sum1 += Boundary2DScanner.Step.DIAGONAL_LENGTH;
        sum1 += 3;
        sum1 += Boundary2DScanner.Step.DIAGONAL_LENGTH;
        double sum2 = 16 * 1.0 + 4 * Boundary2DScanner.Step.DIAGONAL_LENGTH;
        System.out.printf("sum1: %.20f%nsum2: %.20f%n", sum1, sum2);
        // - different ways to calculate perimeter lead to different results,
        // but they will be become identical after conversino to "float" type

        final MultiMatrix2D multiMatrix;
        try (ReadImage readImage = new ReadImage().setFile(args[0])) {
            readImage.process();
            multiMatrix = readImage.getMat().toMultiMatrix2D();
        }

        MultiMatrix2D labels;
        try (ScanAndMeasureBoundaries scan = new ScanAndMeasureBoundaries()) {
            scan.requestOutput(ScanAndMeasureBoundaries.OUTPUT_LABELS);
            scan.requestOutput(ScanAndMeasureBoundaries.OUTPUT_AREA, ScanAndMeasureBoundaries.OUTPUT_PERIMETER);
            scan.process(multiMatrix);
            System.out.printf("Areas:%n%s%n",
                    scan.getNumbers(ScanAndMeasureBoundaries.OUTPUT_AREA));
            System.out.printf("Perimeters:%n%s%n",
                    scan.getNumbers(ScanAndMeasureBoundaries.OUTPUT_PERIMETER));

            labels = scan.process(multiMatrix);
        }
        labels = new Contrast().process(labels).asMultiMatrix2D();

        try (WriteImage writeImage = new WriteImage()) {
            writeImage.getInputMatContainer().setTo(labels);
            writeImage.setFile(args[1]).process();
        }
    }
}
