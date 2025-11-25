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

package net.algart.executors.modules.cv.matrices.drawing;

import net.algart.arrays.*;
import net.algart.contours.Contours;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.matrices.geometry.Resize;
import net.algart.executors.modules.core.matrices.misc.Selector;
import net.algart.math.IPoint;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class DrawContours extends MultiMatrix2DFilter {
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_IMAGE_POSITION = "image_position";
    public static final String OUTPUT_NUMBER_OF_OBJECTS = "number_of_objects";

    public enum DrawnContourKinds {
        ALL(isInternalContour -> true),
        EXTERNAL(isInternalContour -> !isInternalContour),
        INTERNAL(isInternalContour -> isInternalContour);

        private final Predicate<Boolean> accept;

        DrawnContourKinds(Predicate<Boolean> accept) {
            this.accept = accept;
        }
    }

    public enum DrawnFeatures {
        STRICT_BYTE_CONTOURS(byte.class, true, false),
        NOT_INTERSECTED_LABELS_OF_CONTOURS(int.class, false, false),
        NOT_INTERSECTED_INDEXES_OF_CONTOURS(int.class, false, false),
        NOT_INTERSECTED_RANDOMLY_COLORED_CONTOURS(byte.class, false, true);

        private final Class<?> elementType;
        private final boolean incrementing;
        private final boolean colored;

        DrawnFeatures(Class<?> elementType, boolean incrementing, boolean colored) {
            this.elementType = elementType;
            this.incrementing = incrementing;
            this.colored = colored;
        }
    }

    private DrawnFeatures drawnFeatures = DrawnFeatures.STRICT_BYTE_CONTOURS;
    private int strictByteMultiplier = 1;
    private DrawnContourKinds drawnContourKinds = DrawnContourKinds.EXTERNAL;
    private int firstIndex = 0;
    private int numberOfContours = 0;
    private boolean needToProcessDiagonals = false;
    private int dimX = 1000;
    private int dimY = 1000;
    private int scale = 1;
    private int imageStartX = 0;
    private int imageStartY = 0;
    private long randSeed = 0;
    // 0 means really random: new sequence for each call
    private boolean contrastBackground = false;

    public DrawContours() {
        addInputNumbers(INPUT_CONTOURS);
        addInputNumbers(INPUT_IMAGE_POSITION);
        addOutputScalar(OUTPUT_NUMBER_OF_OBJECTS);
    }

    public DrawnFeatures getDrawnFeatures() {
        return drawnFeatures;
    }

    public DrawContours setDrawnFeatures(DrawnFeatures drawnFeatures) {
        this.drawnFeatures = nonNull(drawnFeatures);
        return this;
    }

    public int getStrictByteMultiplier() {
        return strictByteMultiplier;
    }

    public DrawContours setStrictByteMultiplier(int strictByteMultiplier) {
        this.strictByteMultiplier = positive(strictByteMultiplier);
        return this;
    }

    public DrawnContourKinds getDrawnContourKinds() {
        return drawnContourKinds;
    }

    public DrawContours setDrawnContourKinds(DrawnContourKinds drawnContourKinds) {
        this.drawnContourKinds = nonNull(drawnContourKinds);
        return this;
    }

    public int getFirstIndex() {
        return firstIndex;
    }

    public DrawContours setFirstIndex(int firstIndex) {
        this.firstIndex = nonNegative(firstIndex);
        return this;
    }

    public int getNumberOfContours() {
        return numberOfContours;
    }

    public DrawContours setNumberOfContours(int numberOfContours) {
        this.numberOfContours = nonNegative(numberOfContours);
        return this;
    }

    public int getDimX() {
        return dimX;
    }

    public DrawContours setDimX(int dimX) {
        this.dimX = positive(dimX);
        return this;
    }

    public int getDimY() {
        return dimY;
    }

    public DrawContours setDimY(int dimY) {
        this.dimY = positive(dimY);
        return this;
    }

    public boolean isNeedToProcessDiagonals() {
        return needToProcessDiagonals;
    }

    public DrawContours setNeedToProcessDiagonals(boolean needToProcessDiagonals) {
        this.needToProcessDiagonals = needToProcessDiagonals;
        return this;
    }

    public int getScale() {
        return scale;
    }

    public DrawContours setScale(int scale) {
        this.scale = positive(scale);
        return this;
    }

    public int getImageStartX() {
        return imageStartX;
    }

    public DrawContours setImageStartX(int imageStartX) {
        this.imageStartX = imageStartX;
        return this;
    }

    public int getImageStartY() {
        return imageStartY;
    }

    public DrawContours setImageStartY(int imageStartY) {
        this.imageStartY = imageStartY;
        return this;
    }

    public long getRandSeed() {
        return randSeed;
    }

    public DrawContours setRandSeed(long randSeed) {
        this.randSeed = randSeed;
        return this;
    }

    public boolean isContrastBackground() {
        return contrastBackground;
    }

    public DrawContours setContrastBackground(boolean contrastBackground) {
        this.contrastBackground = contrastBackground;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        Contours contours = Contours.deserialize(getInputNumbers(INPUT_CONTOURS).toIntArray());
        return process(contours, source);
    }

    public MultiMatrix2D process(Contours contours, MultiMatrix2D background) {
        if (needToProcessDiagonals) {
            contours = contours.unpackContours(true);
        }
        final long dimX = background == null ? this.dimX : background.dimX() * (long) scale;
        final long dimY = background == null ? this.dimY : background.dimY() * (long) scale;
        final SNumbers position = getInputNumbers(INPUT_IMAGE_POSITION, true);
        IPoint originPoint = position.isProbableRectangularArea() ?
                position.toIRectangularArea().min() :
                position.toIPoint();
        if (originPoint == null) {
            originPoint = IPoint.of(imageStartX, imageStartY);
        }
        List<Matrix<? extends UpdatablePArray>> resultChannels = null;
        if (background != null && !drawnFeatures.incrementing) {
            if (contrastBackground) {
                background = background.contrast();
            }
            background = background.toPrecisionIfNot(drawnFeatures.elementType);
            if (drawnFeatures.colored) {
                background = background.asOtherNumberOfChannels(3);
            }
            if (scale > 1) {
                final Resize resize = new Resize();
                resize.setResizingMode(Resize.ResizingMode.NEAREST);
                resize.setDimX(dimX);
                resize.setDimY(dimY);
                background = resize.process(background);
            }
            resultChannels = MultiMatrix.cloneMatrices(background.allChannels());
        }
        if (resultChannels == null) {
            resultChannels = new LinkedList<>();
            for (int k = 0; k < (drawnFeatures.colored ? 3 : 1); k++) {
                resultChannels.add(Arrays.SMM.newMatrix(
                        UpdatablePArray.class, drawnFeatures.elementType, dimX, dimY));
            }
        }
        Random rnd = randSeed == 0 ? new Random() : new Random(randSeed);
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            for (Matrix<? extends UpdatablePArray> channel : resultChannels) {
                drawContour(channel.cast(UpdatablePFixedArray.class), contours, k, originPoint, rnd);
                // - actually, the element type is always byte or int
            }
        }
        MultiMatrix2D result = MultiMatrix.of2D(resultChannels);
        if (background != null && drawnFeatures.incrementing) {
            final Selector selector = new Selector();
            selector.setSelectorType(Selector.SelectorType.BINARY_MATRIX);
            result = selector.process(result, background, result).asMultiMatrix2D();
        }
        getScalar(OUTPUT_NUMBER_OF_OBJECTS).setTo(contours.numberOfContours());
        return result;
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

    private void drawContour(
            Matrix<? extends UpdatablePFixedArray> result,
            Contours contours,
            int contourIndex,
            IPoint origin,
            Random rnd) {
        if (origin.coordCount() != result.dimCount()) {
            throw new IllegalArgumentException("Different number of dimensions: " + origin.coordCount()
                    + "-dimensional point (" + origin + ") and " + result.dimCount()
                    + "-dimensional matrix (" + result + ")");
        }
        final boolean internalContour = contours.isInternalContour(contourIndex);
        if (!drawnContourKinds.accept.test(internalContour)) {
            return;
        }
        if (contourIndex < firstIndex || (numberOfContours > 0 && contourIndex - firstIndex >= numberOfContours)) {
            return;
        }
        final IntArray contour = contours.getContour(contourIndex);
        final int objectLabel = contours.getObjectLabel(contourIndex);
        final long n = contour.length();
        assert n >= 2 : "empty contour is impossible";
        final long maxValue = result.array().maxPossibleValue();
        final long colorOrMaxValue;
        switch (drawnFeatures) {
            case STRICT_BYTE_CONTOURS: {
                colorOrMaxValue = maxValue;
                break;
            }
            case NOT_INTERSECTED_LABELS_OF_CONTOURS: {
                colorOrMaxValue = objectLabel;
                break;
            }
            case NOT_INTERSECTED_INDEXES_OF_CONTOURS: {
                colorOrMaxValue = contourIndex + 1;
                break;
            }
            case NOT_INTERSECTED_RANDOMLY_COLORED_CONTOURS: {
                colorOrMaxValue = rnd.nextInt((int) maxValue);
                break;
            }
            default: {
                throw new UnsupportedOperationException("Unsupported " + drawnFeatures);
            }
        }
        final Boolean internal = drawnFeatures.incrementing ? null : internalContour;
        final int increment = drawnFeatures.incrementing ? strictByteMultiplier : 0;
        long lastX = contour.getInt(n - 2) - origin.x();
        long lastY = contour.getInt(n - 1) - origin.y();
        for (long i = 0; i < n; i += 2) {
            final long x = contour.getInt(i) - origin.x();
            final long y = contour.getInt(i + 1) - origin.y();
            if (!drawHorizontalOrVerticalLine(
                    result, scale * lastX, scale * lastY, scale * x, scale * y,
                    colorOrMaxValue, internal, increment)) {
                throw new IllegalArgumentException("Cannot draw contours containing non-horizontal "
                        + "and non-vertical segments (" + lastX + "," + lastY + " - " + x + "," + y
                        + ") between points #" + (i / 2 - 1) + " and #" + i / 2);
            }
            lastX = x;
            lastY = y;
        }
    }

    private static boolean drawHorizontalOrVerticalLine(
            Matrix<? extends UpdatablePFixedArray> result,
            long x1,
            long y1,
            long x2,
            long y2,
            long colorOrMaxValue,
            Boolean internal,
            int increment) {
        // - long coordinates guarantee avoiding overflow in "scale * x* and "+ 1" operators
        // internal == null means exact contours, internal != null - boundaries
        if (y1 == y2) {
            if (x1 == x2) {
                return true;
            } else if (x1 < x2) {
                if (internal != null && internal) {
                    y1--;
                }
                drawRow(result, x1, x2 - 1, y1, colorOrMaxValue, increment);
            } else {
                if (internal != null && !internal) {
                    y1--;
                }
                if (internal == null) {
                    drawRow(result, x2 + 1, x1, y1, colorOrMaxValue, increment);
                } else {
                    drawRow(result, x2, x1 - 1, y1, colorOrMaxValue, increment);
                }
            }
            return true;
        } else if (x1 == x2) {
            if (y1 < y2) {
                if (internal != null && !internal) {
                    x1--;
                }
                drawCol(result, x1, y1, y2 - 1, colorOrMaxValue, increment);
            } else {
                if (internal != null && internal) {
                    x1--;
                }
                if (internal == null) {
                    drawCol(result, x1, y2 + 1, y1, colorOrMaxValue, increment);
                } else {
                    drawCol(result, x1, y2, y1 - 1, colorOrMaxValue, increment);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static void drawRow(
            Matrix<? extends UpdatablePFixedArray> result,
            long minX,
            long maxX,
            long y,
            long colorOrMaxValue,
            int increment) {
        if (minX == maxX) {
            if (minX >= 0 && minX < result.dimX() && y >= 0 && y < result.dimY()) {
                drawElement(result.array(), result.index(minX, y), colorOrMaxValue, increment);
            }
        } else {
            drawElements(result.subMatrix(
                            minX, y, maxX + 1, y + 1, Matrix.ContinuationMode.NAN_CONSTANT)
                    .array(), colorOrMaxValue, increment);
        }
    }

    private static void drawCol(
            Matrix<? extends UpdatablePFixedArray> result,
            long x,
            long minY,
            long maxY,
            long colorOrMaxValue,
            int increment) {
        if (minY == maxY) {
            if (minY >= 0 && minY < result.dimY() && x >= 0 && x < result.dimX()) {
                drawElement(result.array(), result.index(x, minY), colorOrMaxValue, increment);
            }
        } else {
            drawElements(result.subMatrix(
                            x, minY, x + 1, maxY + 1, Matrix.ContinuationMode.NAN_CONSTANT)
                    .array(), colorOrMaxValue, increment);
        }
    }

    private static void drawElement(UpdatablePFixedArray array, long index, long colorOrMaxValue, int increment) {
        if (increment > 0) {
            array.setLong(index, Math.min(array.getLong(index) + increment, colorOrMaxValue));
        } else {
            array.setLong(index, colorOrMaxValue);
        }
    }

    private static void drawElements(UpdatablePFixedArray array, long colorOrMaxValue, int increment) {
        if (increment > 0) {
            for (long k = 0, n = array.length(); k < n; k++) {
                array.setLong(k, Math.min(array.getLong(k) + increment, colorOrMaxValue));
            }
        } else {
            for (long k = 0, n = array.length(); k < n; k++) {
                array.setLong(k, colorOrMaxValue);
            }
        }
    }
}
