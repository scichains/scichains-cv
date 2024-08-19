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

package net.algart.executors.modules.cv.matrices.objects.binary;

import net.algart.arrays.*;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixToNumbers;
import net.algart.executors.modules.cv.matrices.drawing.DrawLine;
import net.algart.executors.modules.cv.matrices.misc.SortedRound2DAperture;
import net.algart.math.IRange;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class NearestMatrixPixels extends MultiMatrixToNumbers {
    private static final int MULTITHREADING_POSITIONS_BLOCK_LENGTH = 128;
    // - zero value disables multithreading

    public static final String INPUT_MASK = "mask";
    public static final String INPUT_POSITIONS = "positions";
    public static final String OUTPUT_NEAREST_PIXELS = "nearest_pixels";
    public static final String OUTPUT_NUMBERS_OF_NEAREST = "numbers_of_nearest";
    public static final String OUTPUT_LINES_TO_NEAREST = "lines_to_nearest";

    private int maxApertureSize = 10;
    private int neighbourhoodSizeForNearest = 3;
    private int maxNumberOfNeighbours = 1;
    private boolean invertSourceMask = false;
    private boolean skipPositionsAtMaks = true;
    private boolean returnPairsOfThisAndNearestPixel = true;
    private int drawingLinesThickness = 1;
    private Color drawingLinesColor = Color.WHITE;
    private boolean convertMonoToColorForDrawingLines = false;
    private boolean visibleLinesToNearest = false;

    public NearestMatrixPixels() {
        setDefaultInputMat(INPUT_MASK);
        setDefaultOutputNumbers(OUTPUT_NEAREST_PIXELS);
        addOutputNumbers(OUTPUT_NUMBERS_OF_NEAREST);
        addOutputMat(OUTPUT_LINES_TO_NEAREST);
    }

    public int getMaxApertureSize() {
        return maxApertureSize;
    }

    public NearestMatrixPixels setMaxApertureSize(int maxApertureSize) {
        this.maxApertureSize = positive(maxApertureSize);
        return this;
    }

    public int getNeighbourhoodSizeForNearest() {
        return neighbourhoodSizeForNearest;
    }

    public NearestMatrixPixels setNeighbourhoodSizeForNearest(int neighbourhoodSizeForNearest) {
        this.neighbourhoodSizeForNearest = positive(neighbourhoodSizeForNearest);
        return this;
    }

    public int getMaxNumberOfNeighbours() {
        return maxNumberOfNeighbours;
    }

    public NearestMatrixPixels setMaxNumberOfNeighbours(int maxNumberOfNeighbours) {
        this.maxNumberOfNeighbours = positive(maxNumberOfNeighbours);
        return this;
    }

    public boolean isInvertSourceMask() {
        return invertSourceMask;
    }

    public NearestMatrixPixels setInvertSourceMask(boolean invertSourceMask) {
        this.invertSourceMask = invertSourceMask;
        return this;
    }

    public boolean isSkipPositionsAtMaks() {
        return skipPositionsAtMaks;
    }

    public NearestMatrixPixels setSkipPositionsAtMaks(boolean skipPositionsAtMaks) {
        this.skipPositionsAtMaks = skipPositionsAtMaks;
        return this;
    }

    public boolean isReturnPairsOfThisAndNearestPixel() {
        return returnPairsOfThisAndNearestPixel;
    }

    public NearestMatrixPixels setReturnPairsOfThisAndNearestPixel(boolean returnPairsOfThisAndNearestPixel) {
        this.returnPairsOfThisAndNearestPixel = returnPairsOfThisAndNearestPixel;
        return this;
    }

    public int getDrawingLinesThickness() {
        return drawingLinesThickness;
    }

    public NearestMatrixPixels setDrawingLinesThickness(int drawingLinesThickness) {
        this.drawingLinesThickness = positive(drawingLinesThickness);
        return this;
    }

    public Color getDrawingLinesColor() {
        return drawingLinesColor;
    }

    public NearestMatrixPixels setDrawingLinesColor(Color drawingLinesColor) {
        this.drawingLinesColor = nonNull(drawingLinesColor);
        return this;
    }

    public boolean isConvertMonoToColorForDrawingLines() {
        return convertMonoToColorForDrawingLines;
    }

    public NearestMatrixPixels setConvertMonoToColorForDrawingLines(boolean convertMonoToColorForDrawingLines) {
        this.convertMonoToColorForDrawingLines = convertMonoToColorForDrawingLines;
        return this;
    }

    public boolean isVisibleLinesToNearest() {
        return visibleLinesToNearest;
    }

    public NearestMatrixPixels setVisibleLinesToNearest(boolean visibleLinesToNearest) {
        this.visibleLinesToNearest = visibleLinesToNearest;
        return this;
    }

    @Override
    public SNumbers analyse(MultiMatrix sourceMask) {
        Objects.requireNonNull(sourceMask, "Null source mask");
        return analyse(sourceMask.asMultiMatrix2D(), getInputNumbers(INPUT_POSITIONS));
    }

    public SNumbers analyse(MultiMatrix2D sourceMask, SNumbers positions) {
        positions.requireBlockLength(2, "positions");
        final int[] positionsArray = positions.toIntArray();
        long t1 = System.nanoTime();
        final Matrix<BitArray> mask = invertSourceMask ? sourceMask.zeroRGBMatrix() : sourceMask.nonZeroRGBMatrix();
        final SortedRound2DAperture maxAperture = SortedRound2DAperture.getCircle(maxApertureSize, sourceMask.dimX());
        final SortedRound2DAperture neighbourhoodForNearest =
                SortedRound2DAperture.getCircle(neighbourhoodSizeForNearest, sourceMask.dimX());
        final NearestPixelFinder finder = new NearestPixelFinder(mask, maxAperture, neighbourhoodForNearest)
                .setMaxNumberOfNeighbours(maxNumberOfNeighbours)
                .setSkipPositionsAtMaks(skipPositionsAtMaks);
        long t2 = System.nanoTime();
        MutableIntArray nearestXY = Arrays.SMM.newEmptyIntArray();
        final int n = positions.n();
        final int[] numbersOfNearest = new int[n];
        if (n > 0) {
            if (MULTITHREADING_POSITIONS_BLOCK_LENGTH == 0 || Arrays.SystemSettings.cpuCount() == 1) {
                nearestXY = findNearestInRange(finder, positionsArray, IRange.valueOf(0, n - 1), numbersOfNearest);
            } else {
                final List<IRange> ranges = new ArrayList<>();
                final int blockLength = Math.max(1, Math.min(MULTITHREADING_POSITIONS_BLOCK_LENGTH,
                        n / Runtime.getRuntime().availableProcessors()));
                for (int i = 0; i < n; i += blockLength) {
                    ranges.add(IRange.valueOf(i, Math.min(i + blockLength, n) - 1));
                }
                final List<? extends IntArray> results = ranges.parallelStream().map(
                        range -> findNearestInRange(finder, positionsArray, range, numbersOfNearest)).toList();
                for (IntArray a : results) {
                    nearestXY.append(a);
                }
            }
        }
        long t3 = System.nanoTime();
        final boolean outputLines = isOutputNecessary(OUTPUT_LINES_TO_NEAREST);
        int[] resultPositions = null;
        if (outputLines || returnPairsOfThisAndNearestPixel) {
            SNumbers.checkDimensions(nearestXY.length() / 2, 4);
            int[] linesPositions = new int[2 * (int) nearestXY.length()];
            for (int i = 0, disp = 0, p = 0; i < n; i++) {
                int x = positionsArray[2 * i];
                int y = positionsArray[2 * i + 1];
                for (int j = 0, m = numbersOfNearest[i]; j < m; j++) {
                    linesPositions[disp++] = x;
                    linesPositions[disp++] = y;
                    linesPositions[disp++] = nearestXY.getInt(p++);
                    linesPositions[disp++] = nearestXY.getInt(p++);
                }
            }
            if (outputLines) {
                final SMat mat;
                try (DrawLine drawLine = new DrawLine()) {
                    drawLine.setAntialiasing(false);
                    drawLine.setThickness(drawingLinesThickness);
                    drawLine.setConvertMonoToColor(convertMonoToColorForDrawingLines);
                    drawLine.setColor(drawingLinesColor);
                    drawLine.putNumbers(INPUT_POSITIONS, linesPositions, 4);
                    drawLine.putMat(DEFAULT_INPUT_PORT, SMat.valueOf(sourceMask));
                    drawLine.process();
                    mat = drawLine.getMat();
                    if (convertMonoToColorForDrawingLines) {
                        getMat(OUTPUT_LINES_TO_NEAREST).setTo(mat.clone());
                        // - clone necessary due to using try-with-resources
                    } else {
                        getMat(OUTPUT_LINES_TO_NEAREST).setTo(mat.toMultiMatrix2D().nonZeroRGB());
                    }
                }
            }
            if (returnPairsOfThisAndNearestPixel) {
                resultPositions = linesPositions;
            }
        }
        if (resultPositions == null) {
            resultPositions = nearestXY.toJavaArray();
        }
        getNumbers(OUTPUT_NUMBERS_OF_NEAREST).setTo(numbersOfNearest, 1);
        final SNumbers result = SNumbers.valueOfArray(resultPositions, returnPairsOfThisAndNearestPixel ? 4 : 2);
        long t4 = System.nanoTime();
        logDebug(() -> String.format(Locale.US, "Nearest matrix pixels for %d points at %s, "
                        + "aperture size %d (%d points): %.3f ms = "
                        + "%.3f preparing, %.3f search, %.3f making results",
                n, sourceMask, maxApertureSize, maxAperture.count(),
                (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, (t3 - t2) * 1e-6, (t4 - t3) * 1e-6));

        return result;
    }

    @Override
    public String visibleOutputPortName() {
        return visibleLinesToNearest ? OUTPUT_LINES_TO_NEAREST : OUTPUT_NEAREST_PIXELS;
    }

    private static MutableIntArray findNearestInRange(
            NearestPixelFinder finder,
            int[] positions,
            IRange range,
            int[] resultNumbersOfNearest) {
        final MutableIntArray result = Arrays.SMM.newEmptyIntArray();
        for (int k = (int) range.min(), max = (int) range.max(); k <= max; k++) {
            final int x = positions[2 * k];
            final int y = positions[2 * k + 1];
            resultNumbersOfNearest[k] = finder.findNearest(x, y, result);
        }
        return result;
    }

}