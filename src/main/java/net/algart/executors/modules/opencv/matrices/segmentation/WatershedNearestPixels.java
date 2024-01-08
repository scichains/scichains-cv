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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.opencv.common.MatFilter;
import net.algart.executors.modules.cv.matrices.drawing.DrawPattern;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.arrays.ByteArray;
import net.algart.arrays.Matrices;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import org.bytedeco.opencv.opencv_core.*;

public final class WatershedNearestPixels extends MatFilter {
    public static final String INPUT_SAMPLE_IMAGE = "sample_image";
    public static final String INPUT_POSITIONS = "positions";
    public static final String OUTPUT_LABELS = "labels";

    private int dimX = 100;
    private int dimY = 100;

    public WatershedNearestPixels() {
        setDefaultInputMat(INPUT_SAMPLE_IMAGE);
        addInputNumbers(INPUT_POSITIONS);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public int getDimX() {
        return dimX;
    }

    public WatershedNearestPixels setDimX(int dimX) {
        this.dimX = positive(dimX);
        return this;
    }

    public int getDimY() {
        return dimY;
    }

    public WatershedNearestPixels setDimY(int dimY) {
        this.dimY = positive(dimY);
        return this;
    }


    @Override
    public Mat process(Mat source) {
        return process(source, getInputNumbers(INPUT_POSITIONS));

    }

    public Mat process(Mat source, SNumbers positions) {
        final int dimX = source == null ? this.dimX : source.cols();
        final int dimY = source == null ? this.dimY : source.rows();
        return process(dimX, dimY, positions);
    }

    public Mat process(int dimX, int dimY, SNumbers positions) {
        positions = positions.columnRange(0, 2);

        final MultiMatrix2D dummy = MultiMatrix.valueOf2DMono(
                Matrices.constantMatrix(0.0, ByteArray.class, dimX, dimY));
        // - its content is not important: setClearSource below
        final DrawPattern drawPattern = new DrawPattern();
        drawPattern.setClearSource(true).setPattern(MorphologyFilter.Shape.CUBE, 1);
        drawPattern.putNumbers(DrawPattern.INPUT_POSITIONS, positions);
        final MultiMatrix2D points = drawPattern.process(dummy).asMultiMatrix2D();

        final Watershed watershed = new Watershed();
        watershed.setSeedingMode(Watershed.SeedingMode.CONNECTED_COMPONENTS_ONLY);
        watershed.setValuesOnBoundaries(Watershed.ValuesOnBoundaries.NEAREST_LABEL);
        return watershed.process(null, O2SMat.toMat(points));
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }
}
