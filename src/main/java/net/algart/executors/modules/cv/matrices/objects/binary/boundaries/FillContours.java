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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.Arrays;
import net.algart.contours.ContourFiller;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Locale;
import java.util.Objects;

public final class FillContours extends MultiMatrix2DFilter implements ReadOnlyExecutionInput {
    public static final String RECTANGLE = "rectangle";
    public static final String INPUT_BACKGROUND = "background";
    // - Background is added only for getting its dimensions
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_LABELS_MAP = "labels_map";
    public static final String INPUT_RECTANGLE = "rectangle";
    public static final String OUTPUT_LABELS = AbstractScanAndMeasureBoundaries.OUTPUT_LABELS;

    private boolean doAction = true;
    private Class<?> elementType = int.class;
    private boolean needToProcessDiagonals = true;
    private boolean cacheUnpackedContours = false;
    private boolean zeroResultForEmptyContours = false;
    private int startX = 0;
    private int startY = 0;
    private int sizeX = 1000;
    private int sizeY = 1000;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private Integer defaultFiller = null;

    private final UnpackContours unpacker = new UnpackContours().setCacheLastContours(true);

    public FillContours() {
        setDefaultInputMat(INPUT_BACKGROUND);
        addInputNumbers(INPUT_CONTOURS);
        addInputNumbers(INPUT_LABELS_MAP);
        addInputNumbers(INPUT_RECTANGLE);
        setDefaultOutputMat(OUTPUT_LABELS);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public boolean isDoAction() {
        return doAction;
    }

    public FillContours setDoAction(boolean doAction) {
        this.doAction = doAction;
        return this;
    }

    public final Class<?> getElementType() {
        return elementType;
    }

    public FillContours setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public FillContours setElementType(String elementType) {
        setElementType(MultiMatrixGenerator.elementType(elementType));
        return this;
    }

    public boolean isNeedToProcessDiagonals() {
        return needToProcessDiagonals;
    }

    public FillContours setNeedToProcessDiagonals(boolean needToProcessDiagonals) {
        this.needToProcessDiagonals = needToProcessDiagonals;
        return this;
    }

    public boolean isCacheUnpackedContours() {
        return cacheUnpackedContours;
    }

    public FillContours setCacheUnpackedContours(boolean cacheUnpackedContours) {
        this.cacheUnpackedContours = cacheUnpackedContours;
        return this;
    }

    public boolean isZeroResultForEmptyContours() {
        return zeroResultForEmptyContours;
    }

    public FillContours setZeroResultForEmptyContours(boolean zeroResultForEmptyContours) {
        this.zeroResultForEmptyContours = zeroResultForEmptyContours;
        return this;
    }

    public int getStartX() {
        return startX;
    }

    public FillContours setStartX(int startX) {
        this.startX = startX;
        return this;
    }

    public int getStartY() {
        return startY;
    }

    public FillContours setStartY(int startY) {
        this.startY = startY;
        return this;
    }

    public int getSizeX() {
        return sizeX;
    }

    public FillContours setSizeX(int sizeX) {
        this.sizeX = positive(sizeX);
        return this;
    }

    public int getSizeY() {
        return sizeY;
    }

    public FillContours setSizeY(int sizeY) {
        this.sizeY = positive(sizeY);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public FillContours setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public Integer getDefaultFiller() {
        return defaultFiller;
    }

    public FillContours setDefaultFiller(Integer defaultFiller) {
        this.defaultFiller = defaultFiller;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D background) {
        final SNumbers inputContours = getInputNumbers(INPUT_CONTOURS, !doAction);
        return process(background, inputContours.toIntArrayOrReference());
    }

    public MultiMatrix2D process(MultiMatrix2D background, int[] serializedContours) {
        if (!doAction) {
            getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(0);
            return null;
        }
        Objects.requireNonNull(serializedContours, "Null serializedContours");
        long t1 = debugTime();
        final boolean useUnpacker = this.cacheUnpackedContours;
        final Contours contours;
        if (useUnpacker) {
            unpacker.setNeedToProcessDiagonals(needToProcessDiagonals);
            contours = unpacker.unpackContours(serializedContours);
        } else {
            contours = Contours.deserialize(serializedContours);
        }
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(contours.numberOfContours());
        long t2 = debugTime();
        if (contours.isEmpty() && !zeroResultForEmptyContours) {
            return null;
        }
        final SNumbers area = getInputNumbers(RECTANGLE, true);
        final IRectangularArea rectangle = area.isProbableRectangularArea() ? area.toIRectangularArea() : null;
        final IPoint position = rectangle != null ? rectangle.min() : area.toIPoint();
        final long startX = position != null ? position.x() : this.startX;
        final long startY = position != null ? position.y() : this.startY;
        final long sizeX = background != null ? background.dimX() : rectangle != null ? rectangle.sizeX() : this.sizeX;
        final long sizeY = background != null ? background.dimY() : rectangle != null ? rectangle.sizeY() : this.sizeY;
        final int[] labelsMap = getInputNumbers(INPUT_LABELS_MAP, true).toIntArray();

        final MultiMatrix2D result;
        final ContourFiller contourFiller = ContourFiller.newInstance(
                        contours, elementType, startX, startY, sizeX, sizeY)
                .setNeedToUnpack(!useUnpacker)
                .setNeedToUnpackDiagonals(needToProcessDiagonals);
        long t3 = debugTime(), t4;
        if (sizeX == 0 || sizeY == 0) {
            result = MultiMatrix.of2DMono(Arrays.SMM.newIntMatrix(sizeX, sizeY));
            t4 = t3;
        } else {
            contourFiller.setLabelsMap(labelsMap);
            contourFiller.setIndexingBase(indexingBase.start);
            if (defaultFiller != null) {
                final int filler = defaultFiller;
                contourFiller.setLabelToFillerDefault(label -> filler);
            }
            contourFiller.findAndSortNecessaryContours();
            t4 = debugTime();
            contourFiller.fillNecessaryContours();
            result = MultiMatrix.of2DMono(contourFiller.getLabels());
        }
        long t5 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Filling %d contours from %d total contours in %.3f ms = "
                        + "%.3f %s + %.3f creating filler + %.3f preparing + %.3f filling",
                contourFiller.numberOfNecessaryContours(),
                contours.numberOfContours(),
                (t5 - t1) * 1e-6,
                (t2 - t1) * 1e-6, useUnpacker ? "cached unpacking" : "loading",
                (t3 - t2) * 1e-6, (t4 - t3) * 1e-6, (t5 - t4) * 1e-6));
        return result;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

    @Override
    protected boolean resultRequired() {
        return false;
    }
}
