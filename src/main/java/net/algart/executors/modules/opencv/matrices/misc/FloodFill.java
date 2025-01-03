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

package net.algart.executors.modules.opencv.matrices.misc;

import net.algart.executors.modules.opencv.common.OpenCVExecutor;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OConnectivity;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Point;

import java.awt.*;
import java.util.ConcurrentModificationException;
import java.util.Locale;
import java.util.Objects;

public final class FloodFill extends OpenCVExecutor {
    public static final String INPUT_NON_FILLED_MASK = "non_filled_mask";
    public static final String OUTPUT_FILLED = "filled";
    public static final String OUTPUT_MASK = "mask";
    public static final String OUTPUT_MODIFIED_RECTANGLE = "modified_rectangle";

    public enum FillingMode {
        FILL_INITIAL_MASK(true, false),
        FILL_MASK(false, false),
        FILL(false, false),
        FILL_AND_INSERT(false, true) {
            IRectangularArea fill(FloodFill executor, IPoint seedPoint) {
                final IRectangularArea result = simpleFill(executor, executor.workMask(), seedPoint);
                executor.insertMaskToAccumulator(result);
                return result;
            }
        },
        FILL_AND_REMOVE(false, true) {
            IRectangularArea fill(FloodFill executor, IPoint seedPoint) {
                final IRectangularArea result = simpleFill(executor, executor.workMask(), seedPoint);
                executor.removeMaskFromAccumulator(result);
                return result;
            }
        };

        private final boolean needToCleanupAccumulator;
        private final boolean needToCleanupWorkMask;

        FillingMode(boolean needToCleanupAccumulator, boolean needToCleanupWorkMask) {
            this.needToCleanupAccumulator = needToCleanupAccumulator;
            this.needToCleanupWorkMask = needToCleanupWorkMask;
        }

        IRectangularArea fill(FloodFill executor, IPoint seedPoint) {
            return simpleFill(executor, executor.accumulatingMask, seedPoint);
        }

        void cleanup(FloodFill executor, IRectangularArea modifiedRectangle) {
            if (needToCleanupAccumulator) {
                executor.cleanupMask(executor.accumulatingMask, modifiedRectangle);
            }
            if (needToCleanupWorkMask) {
                executor.cleanupMask(executor.workMask, modifiedRectangle);
            }
        }

        final IRectangularArea simpleFill(FloodFill executor, Mat mask, IPoint seedPoint) {
            return executor.restrictedFloodFill(executor.storedImage, mask, seedPoint, isFillMaskOnly());
        }

        final void correctOnModeChange(FloodFill executor, FillingMode lastMode) {
            if (this == FILL_INITIAL_MASK && lastMode != FILL_INITIAL_MASK) {
                // - necessary to avoid strange behaviour: for example, in FILL_MASK / FILL modes
                // we do not cleanup accumulatingMask, so, if we switched to FILL_INITIAL_MASK,
                // accumulatingMask will contain unexpected data
                executor.resetMask(executor.accumulatingMask);
            }
        }

        private boolean needToCleanupAccumulator() {
            return this == FILL_INITIAL_MASK;
        }

        private boolean isFillMaskOnly() {
            return this != FILL;
        }
    }

    private static final boolean RETAIN_DRAWN_BORDER_FOR_DEBUGGING = false;
    // - should be false
    private static final int NORMAL_FILLER = 127;
    private static final int BORDER_FILLER = 255;

    private boolean reset = true;
    private FillingMode fillingMode = FillingMode.FILL_INITIAL_MASK;
    private boolean returnOnlyModifiedRectangle = false;
    private boolean percents = false;
    private double x = 0.0;
    private double y = 0.0;
    private double maxFillingSize = 0.0;
    //TODO!! use it
    private Double loDiff = 0.1;
    private Double upDiff = null;
    private boolean rawDiffValues = false;
    private boolean floodFillFixedRange = false;
    private Color fillColor = Color.WHITE;
    private OConnectivity connectivity = OConnectivity.CONNECTIVITY_4;
    private boolean extendedMask = false;
    private boolean includeNonFilledMask = true;
    private boolean packBits = true;

    private final CirclePointsBuilder circlePointsBuilder = new CirclePointsBuilder();
    private FillingMode lastFillingMode = null;
    private Mat storedImage = null;
    private Mat storedNonFilledMask = null;
    private Mat accumulatingMask = null;
    private Mat workMask = null;

    public FloodFill() {
        useVisibleResultParameter();
        addInputMat(DEFAULT_INPUT_PORT);
        addInputMat(INPUT_NON_FILLED_MASK);
        addOutputMat(OUTPUT_MASK);
        addOutputMat(OUTPUT_FILLED);
        addOutputScalar(TestBorderPixels.OUTPUT_DIM_X);
        addOutputScalar(TestBorderPixels.OUTPUT_DIM_Y);
        addOutputNumbers(OUTPUT_MODIFIED_RECTANGLE);
        addOutputScalar(TestBorderPixels.OUTPUT_MAX_BORDER);
        addOutputScalar(TestBorderPixels.OUTPUT_MAX_TOP);
        addOutputScalar(TestBorderPixels.OUTPUT_MAX_BOTTOM);
        addOutputScalar(TestBorderPixels.OUTPUT_MAX_LEFT);
        addOutputScalar(TestBorderPixels.OUTPUT_MAX_RIGHT);
    }

    public boolean isReset() {
        return reset;
    }

    public FloodFill setReset(boolean reset) {
        this.reset = reset;
        return this;
    }

    public FillingMode getFillingMode() {
        return fillingMode;
    }

    public FloodFill setFillingMode(FillingMode fillingMode) {
        this.fillingMode = nonNull(fillingMode);
        return this;
    }

    public boolean isReturnOnlyModifiedRectangle() {
        return returnOnlyModifiedRectangle;
    }

    public FloodFill setReturnOnlyModifiedRectangle(boolean returnOnlyModifiedRectangle) {
        this.returnOnlyModifiedRectangle = returnOnlyModifiedRectangle;
        return this;
    }

    public boolean isPercents() {
        return percents;
    }

    public FloodFill setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX() {
        return x;
    }

    public FloodFill setX(double x) {
        this.x = x;
        return this;
    }

    public double getY() {
        return y;
    }

    public FloodFill setY(double y) {
        this.y = y;
        return this;
    }

    public double getMaxFillingSize() {
        return maxFillingSize;
    }

    public FloodFill setMaxFillingSize(double maxFillingSize) {
        this.maxFillingSize = nonNegative(maxFillingSize);
        return this;
    }

    public Double getLoDiff() {
        return loDiff;
    }

    public FloodFill setLoDiff(Double loDiff) {
        this.loDiff = loDiff == null ? null : nonNegative(loDiff);
        return this;
    }

    public Double getUpDiff() {
        return upDiff;
    }

    public FloodFill setUpDiff(Double upDiff) {
        this.upDiff = upDiff == null ? null : nonNegative(upDiff);
        return this;
    }

    public boolean isRawDiffValues() {
        return rawDiffValues;
    }

    public FloodFill setRawDiffValues(boolean rawDiffValues) {
        this.rawDiffValues = rawDiffValues;
        return this;
    }

    public boolean isFloodFillFixedRange() {
        return floodFillFixedRange;
    }

    public FloodFill setFloodFillFixedRange(boolean floodFillFixedRange) {
        this.floodFillFixedRange = floodFillFixedRange;
        return this;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public FloodFill setFillColor(Color fillColor) {
        this.fillColor = nonNull(fillColor);
        return this;
    }

    public OConnectivity getConnectivity() {
        return connectivity;
    }

    public FloodFill setConnectivity(OConnectivity connectivity) {
        this.connectivity = nonNull(connectivity);
        return this;
    }

    public boolean isExtendedMask() {
        return extendedMask;
    }

    public FloodFill setExtendedMask(boolean extendedMask) {
        this.extendedMask = extendedMask;
        return this;
    }

    public boolean isIncludeNonFilledMask() {
        return includeNonFilledMask;
    }

    public FloodFill setIncludeNonFilledMask(boolean includeNonFilledMask) {
        this.includeNonFilledMask = includeNonFilledMask;
        return this;
    }

    public boolean isPackBits() {
        return packBits;
    }

    public FloodFill setPackBits(boolean packBits) {
        this.packBits = packBits;
        return this;
    }

    @Override
    public void process() {
        processImage(O2SMat.toMat(
                getInputMat(true), false), O2SMat.toMat(
                getInputMat(INPUT_NON_FILLED_MASK, true), true));
    }

    public void processImage(final Mat image, final Mat nonFilledMask) {
        long t1 = debugTime();
        resetSourceData(image, nonFilledMask);
        final boolean filledImageRequested = isOutputNecessary(OUTPUT_FILLED);
        final boolean resultMaskRequested = isOutputNecessary(OUTPUT_MASK);
        getScalar(TestBorderPixels.OUTPUT_DIM_X).setTo(storedImage.cols());
        getScalar(TestBorderPixels.OUTPUT_DIM_Y).setTo(storedImage.rows());
        final boolean boundsStatisticsNecessary = isOutputNecessary(TestBorderPixels.OUTPUT_MAX_BORDER)
                || isOutputNecessary(TestBorderPixels.OUTPUT_MAX_TOP)
                || isOutputNecessary(TestBorderPixels.OUTPUT_MAX_BOTTOM)
                || isOutputNecessary(TestBorderPixels.OUTPUT_MAX_LEFT)
                || isOutputNecessary(TestBorderPixels.OUTPUT_MAX_RIGHT);
        final boolean removeNonFilled = !includeNonFilledMask && storedNonFilledMask != null;
        final IPoint seedPoint = seedPoint(image);
        long t2 = debugTime();
        circlePointsBuilder.setDimensions(accumulatingMask.cols(), accumulatingMask.rows());
        circlePointsBuilder.setDiameter(maxFillingSize);
        final int[] pointsXY = circlePointsBuilder.pointsXY();
        // - for better measuring and logging
        Mat resultMask = null;
        try {
            long t3 = debugTime();
            final IRectangularArea modifiedRectangle = fillingMode.fill(this, seedPoint);
            long t4 = debugTime();
            if (resultMaskRequested) {
                if (returnOnlyModifiedRectangle) {
                    resultMask = extractModified(accumulatingMask, true, modifiedRectangle);
                    if (removeNonFilled) {
                        // - necessary for non-rectangular non-filled-mask
                        removeNotFilledFromModifiedPartOfTheMask(resultMask, modifiedRectangle);
                    }
                } else {
                    resultMask = reduceMask(accumulatingMask, storedImage.cols(), storedImage.rows(), removeNonFilled);
                }
            }
            long t5 = debugTime();
            getNumbers(OUTPUT_MODIFIED_RECTANGLE).setToOrRemove(modifiedRectangle);
            if (boundsStatisticsNecessary) {
                final TestBorderPixels.BorderStatistics statistics =
                        TestBorderPixels.findBorderStatistics(accumulatingMask, 1);
                getScalar(TestBorderPixels.OUTPUT_MAX_BORDER).setTo(statistics.max());
                getScalar(TestBorderPixels.OUTPUT_MAX_TOP).setTo(statistics.maxTop());
                getScalar(TestBorderPixels.OUTPUT_MAX_BOTTOM).setTo(statistics.maxBottom());
                getScalar(TestBorderPixels.OUTPUT_MAX_LEFT).setTo(statistics.maxLeft());
                getScalar(TestBorderPixels.OUTPUT_MAX_RIGHT).setTo(statistics.maxRight());
            }
            long t6 = debugTime();
            if (filledImageRequested) {
                final Mat filledResult = fillingMode.isFillMaskOnly() ?
                        null :
                        returnOnlyModifiedRectangle ?
                                extractModified(storedImage, false, modifiedRectangle) :
                                OTools.clone(storedImage);
                // - cloning necessary, and we should not close this clone:
                // output ports may be deallocated by external client
                O2SMat.setToOrRemove(getMat(OUTPUT_FILLED), filledResult);
            }
            long t7 = debugTime();
            if (resultMaskRequested) {
                if (resultMask == null) {
                    getMat(OUTPUT_MASK).remove();
                } else if (packBits) {
                    getMat(OUTPUT_MASK).setTo(O2SMat.toBinaryMatrix(resultMask));
                } else {
                    O2SMat.setTo(getMat(OUTPUT_MASK), resultMask);
                    resultMask = null;
                    // - preventing closing resultMask: output ports may be deallocated by external client
                }
            }
            long t8 = debugTime();
            fillingMode.cleanup(this, modifiedRectangle);
            lastFillingMode = fillingMode;
            long t9 = debugTime();
            logDebug(String.format(Locale.US, "Flood-filling in %s: %.3f ms:"
                            + "\n  %.3f ms %s,"
                            + "\n  %.3f ms building %d border points to restrict filling size,"
                            + "\n  %.3f ms actual filling%s%s%s,"
                            + "\n  %.3f ms %s mask,"
                            + "\n  %.3f analysing border,"
                            + "\n  %.3f returning filled image%s,"
                            + "\n  %.3f returning mask"
                            + "\n  %.3f cleaning up",
                    OTools.toString(storedImage),
                    (t9 - t1) * 1e-6,
                    (t2 - t1) * 1e-6,
                    reset ? "initial copying data and " + (nonFilledMask == null ?
                            "creating mask" : extendedMask ? "copying mask" : "extending mask")
                            : "initializing",
                    (t3 - t2) * 1e-6, pointsXY.length / 2,
                    (t4 - t3) * 1e-6,
                    fillingMode.isFillMaskOnly() ? " mask only" : " mask and image",
                    modifiedRectangle == null ? " - nothing to do" : "",
                    removeNonFilled ? " (with removing original mask)" : "",
                    (t5 - t4) * 1e-6, extendedMask ? "copying" : "reducing",
                    (t6 - t5) * 1e-6,
                    (t7 - t6) * 1e-6, filledImageRequested ? "" : " (clearing)",
                    (t8 - t7) * 1e-6,
                    (t9 - t8) * 1e-6));
        } finally {
            if (resultMask != null) {
                resultMask.close();
            }
        }
    }

    @Override
    public void close() {
        if (workMask != null) {
            workMask.close();
            workMask = null;
        }
        if (accumulatingMask != null) {
            accumulatingMask.close();
            accumulatingMask = null;
        }
        if (storedNonFilledMask != null) {
            storedNonFilledMask.close();
            storedNonFilledMask = null;
        }
        if (storedImage != null) {
            storedImage.close();
            storedImage = null;
        }
        circlePointsBuilder.clear();
        super.close();
    }

    // Not used in current implementation: too slow and useless
    private Mat filledResult(Mat mask, boolean needToInsertMask, boolean needToReduceMask) {
        final Mat result = OTools.clone(storedImage);
        if (needToInsertMask) {
            Objects.requireNonNull(mask, "Null mask");
            checkMaskSizes(mask, storedImage.cols(), storedImage.rows(), needToReduceMask);
            try (Mat filler = filledMat(storedImage)) {
                if (needToReduceMask) {
                    try (Rect rect = new Rect(1, 1, storedImage.cols(), storedImage.rows())) {
                        filler.copyTo(result, mask.apply(rect));
                    }
                } else {
                    filler.copyTo(result, mask);
                }
            }
        }
        // - we never return storedSource, but clone: output ports may be deallocated by external client
        return result;
    }

    public IPoint seedPoint(Mat image) {
        Objects.requireNonNull(image, "Null image");
        final long seedX = Math.round(percents ? this.x / 100.0 * (image.cols() - 1) : this.x);
        final long seedY = Math.round(percents ? this.y / 100.0 * (image.rows() - 1) : this.y);
        return IPoint.valueOf(seedX, seedY);
    }

    // mask (not image!) is appended by 1 pixel from each side
    public IRectangularArea restrictedFloodFill(Mat image, Mat mask, IPoint seedPoint, boolean fillMaskOnly) {
        final long seedX = seedPoint.x() + 1;
        final long seedY = seedPoint.y() + 1;
        // - shift by 1 necessary in the mask
        circlePointsBuilder.drawAndSavePrevious(mask, seedX, seedY, (byte) BORDER_FILLER);
        final IRectangularArea result = floodFill(storedImage, mask, seedPoint, fillMaskOnly);
        if (!RETAIN_DRAWN_BORDER_FOR_DEBUGGING) {
            circlePointsBuilder.restorePrevious(mask, seedX, seedY);
        }
        return result;
    }

    // mask (not image!) is appended by 1 pixel from each side
    public IRectangularArea floodFill(Mat image, Mat mask, IPoint seedPoint, boolean fillMaskOnly) {
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(mask, "Null mask");
        checkMaskSizes(mask, image.cols(), image.rows(), true);
        // Note: floodFill function requires mask to be greater by 2 pixels than source image
        checkSeedPoint(image, seedPoint);
        final double maxValue = OTools.maxPossibleValue(image);
        final double loDiff = this.loDiff != null ? this.loDiff : this.upDiff != null ? this.upDiff : 0.0;
        final double upDiff = this.upDiff != null ? this.upDiff : this.loDiff != null ? this.loDiff : 0.0;
        try (Point seed = OTools.toPoint(seedPoint);
             Scalar loDiffScalar = OTools.scalarBGR(loDiff * (rawDiffValues ? 1.0 : maxValue));
             Scalar upDiffScalar = OTools.scalarBGR(upDiff * (rawDiffValues ? 1.0 : maxValue));
             Scalar newValScalar = fillColor(image);
             Rect rect = new Rect()) {
            opencv_imgproc.floodFill(
                    image,
                    mask,
                    seed,
                    newValScalar,
                    rect,
                    loDiffScalar,
                    upDiffScalar,
                    connectivity.code()
                            | (NORMAL_FILLER << 8)
                            | (floodFillFixedRange ? opencv_imgproc.FLOODFILL_FIXED_RANGE : 0)
                            | (fillMaskOnly ? opencv_imgproc.FLOODFILL_MASK_ONLY : 0));
            return OTools.toIRectangularArea(rect);
            // - may be null, for example, if nothing to fill (the mask is already set at the seed point)
        }
    }

    public void removeNotFilledFromModifiedPartOfTheMask(Mat resultMask, IRectangularArea modifiedRectangle) {
        if (storedNonFilledMask == null || resultMask == null) {
            // - nothing to remove
            return;
        }
        modifiedRectangle = correctModifiedRectangle(storedNonFilledMask, true, modifiedRectangle);
        if (modifiedRectangle == null) {
            // - will not occur when normal usage, when resultMask is returned by extractModified()
            return;
        }
        checkMaskSizes(resultMask, modifiedRectangle.sizeX(), modifiedRectangle.sizeY(), false);
        try (Rect rect = OTools.toRect(modifiedRectangle)) {
            opencv_core.bitwise_xor(resultMask, storedNonFilledMask.apply(rect), resultMask);
        }
    }

    public void extendMask(Mat result, Mat nonFilledMask, int dimX, int dimY) {
        Objects.requireNonNull(result, "Null result");
        Objects.requireNonNull(nonFilledMask, "Null nonFilledMask");
        checkMaskSizes(nonFilledMask, dimX, dimY, extendedMask);
        Mat mask = nonFilledMask;
        try {
            mask = OTools.toMono8UIfNot(nonFilledMask);
            if (extendedMask) {
                mask.copyTo(result);
                // - input mask should be already extended
            } else {
                opencv_core.copyMakeBorder(mask, result,
                        1, 1, 1, 1, opencv_core.BORDER_CONSTANT);
                // - expanding by zero
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mask, nonFilledMask);
        }
    }

    public Mat reduceMask(Mat mask, int dimX, int dimY, boolean removeNotFilled) {
        Mat result = new Mat();
        if (extendedMask) {
            if (removeNotFilled && storedNonFilledMask != null) {
                opencv_core.bitwise_xor(mask, storedNonFilledMask, result);
            } else {
                mask.copyTo(result);
            }
        } else {
            try (Rect rect = new Rect(1, 1, dimX, dimY)) {
                if (removeNotFilled && storedNonFilledMask != null) {
                    opencv_core.bitwise_xor(mask.apply(rect), storedNonFilledMask.apply(rect), result);
                } else {
                    mask.apply(rect).copyTo(result);
                }
            }
        }
        return result;
    }

    public Mat extractModified(Mat mat, boolean matIsExtended, IRectangularArea modifiedRectangle) {
        Mat result = null;
        modifiedRectangle = correctModifiedRectangle(mat, matIsExtended, modifiedRectangle);
        if (modifiedRectangle != null) {
            result = new Mat();
            try (Rect rect = OTools.toRect(modifiedRectangle)) {
                mat.apply(rect).copyTo(result);
            }
        }
        return result;
    }

    private void createIfNecessary(boolean hasNonFilledMask) {
        if (storedImage == null) {
            storedImage = new Mat();
        }
        if (hasNonFilledMask) {
            if (storedNonFilledMask == null) {
                storedNonFilledMask = new Mat();
            }
        } else {
            if (storedNonFilledMask != null) {
                storedNonFilledMask.close();
                storedNonFilledMask = null;
            }
        }
        if (accumulatingMask == null) {
            accumulatingMask = new Mat();
        }
    }

    private void createWorkMaskIfNecessary() {
        if (workMask == null) {
            workMask = new Mat();
            resetMask(workMask);
        }
    }

    private Mat accumulatingMask() {
        return accumulatingMask;
    }

    private Mat workMask() {
        createWorkMaskIfNecessary();
        return workMask;
    }

    private void resetSourceData(Mat source, Mat nonFilledMask) {
        if (reset) {
            if (source == null) {
                throw new NullPointerException("Source matrix must be specified when \"reset\" is true");
            }
            createIfNecessary(nonFilledMask != null);
            if (source.channels() != 4) {
                source.copyTo(storedImage);
            } else {
                opencv_imgproc.cvtColor(source, storedImage, opencv_imgproc.CV_BGRA2BGR);
            }
            if (nonFilledMask != null) {
                extendMask(storedNonFilledMask, nonFilledMask, source.cols(), source.rows());
            }
            resetMask(accumulatingMask);
            if (workMask != null) {
                // - necessary to create new work mask with correct sizes while the next workMask() call
                workMask.close();
                workMask = null;
            }
        } else {
            if (storedImage == null) {
                throw new IllegalStateException("FloodFill was not initialized: it must be called at least once "
                        + "in \"reset\" mode");
            }
            if (accumulatingMask == null) {
                throw new ConcurrentModificationException("Possible illegal multithreading usage: " +
                        "one of masks is not initialized");
            }
            fillingMode.correctOnModeChange(this, lastFillingMode);
        }
    }

    private IRectangularArea correctModifiedRectangle(
            Mat mat,
            boolean matIsExtended,
            IRectangularArea modifiedRectangle) {
        if (modifiedRectangle == null) {
            // - correct situation: nothing to fill
            return null;
        }
        int dimX = mat.cols();
        int dimY = mat.rows();
        int shift = 0;
        if (matIsExtended) {
            if (dimX < 2 || dimY < 2) {
                throw new IllegalArgumentException("Too little mat: it must be at least 2x2");
            }
            dimX -= 2;
            dimY -= 2;
            shift = 1;
        }
        if (dimX <= 0 || dimY <= 0) {
            // - should not occur while normal usage
            return null;
        }
        modifiedRectangle = modifiedRectangle.intersection(IRectangularArea.valueOf(0, 0, dimX, dimY));
        // - to be on the safe side
        if (modifiedRectangle == null) {
            // - should not occur while normal usage
            return null;
        }
        return modifiedRectangle.shift(IPoint.valueOfEqualCoordinates(2, shift));
    }

    private void resetMask(Mat mask) {
        if (storedNonFilledMask != null) {
            storedNonFilledMask.copyTo(mask);
        } else {
            Mat.zeros(storedImage.rows() + 2, storedImage.cols() + 2, opencv_core.CV_8U)
                    .asMat().copyTo(mask);
        }
    }

    private void cleanupMask(Mat mask, IRectangularArea modifiedRectangle) {
        modifiedRectangle = correctModifiedRectangle(mask, true, modifiedRectangle);
        if (modifiedRectangle == null) {
            // - nothing to fill, nothing to cleanup
            return;
        }
        try (Rect rect = OTools.toRect(modifiedRectangle)) {
            mask = mask.apply(rect);
            if (storedNonFilledMask != null) {
                storedNonFilledMask.apply(rect).copyTo(mask);
            } else {
                Mat.zeros(mask.rows(), mask.cols(), opencv_core.CV_8U).asMat().copyTo(mask);
            }
        }
    }

    private void insertMaskToAccumulator(IRectangularArea modifiedRectangle) {
        modifiedRectangle = correctModifiedRectangle(accumulatingMask, true, modifiedRectangle);
        if (modifiedRectangle == null) {
            return;
        }
        try (Rect rect = OTools.toRect(modifiedRectangle)) {
            final Mat accumulatingRect = accumulatingMask.apply(rect);
            final Mat workRect = workMask.apply(rect);
            // - optimization
            opencv_core.bitwise_or(accumulatingRect, workRect, accumulatingRect);
        }
    }

    private void removeMaskFromAccumulator(IRectangularArea modifiedRectangle) {
        modifiedRectangle = correctModifiedRectangle(accumulatingMask, true, modifiedRectangle);
        if (modifiedRectangle == null) {
            return;
        }
        try (Rect rect = OTools.toRect(modifiedRectangle)) {
            final Mat accumulatingRect = accumulatingMask.apply(rect);
            final Mat workRect = workMask.apply(rect);
            // - not only optimization, it is also important not to modify workMask outside rectangle
            // (that will be cleaned): it allows to avoid 2nd bitwise_not to restore workMask
            opencv_core.bitwise_not(workRect, workRect);
            opencv_core.bitwise_and(accumulatingRect, workRect, accumulatingRect);
            if (storedNonFilledMask != null) {
                opencv_core.bitwise_or(accumulatingRect, storedNonFilledMask.apply(rect), accumulatingRect);
                // - we need to restore non-filled-mask inside this rectangle, it is important
                // both when includeNonFilledMask is true (to include it) and when it's false
                // (to correctly remove it, especially in a case returnOnlyModifiedRectangle=false)
            }
        }
    }

    private Scalar fillColor(Mat sourceImage) {
        return OTools.scalarBGR(fillColor, OTools.maxPossibleValue(sourceImage));
    }

    private Mat filledMat(Mat sourceImage) {
        final Mat result = new Mat(sourceImage.rows(), sourceImage.cols(), sourceImage.type());
        try (Scalar scalar = fillColor(sourceImage)) {
            result.put(scalar);
        }
        return result;
    }

    private static void checkSeedPoint(Mat image, IPoint seedPoint) {
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(seedPoint, "Null seedPoint");
        if (seedPoint.x() < 0 || seedPoint.x() >= image.cols() || seedPoint.y() < 0 || seedPoint.y() >= image.rows()) {
            throw new IllegalArgumentException("Seed position " + seedPoint
                    + " is out of the image image " + image.cols() + "x" + image.rows());
        }
    }

    private static void checkMaskSizes(Mat mask, long dimX, long dimY, boolean extendedMask) {
        final int extensionGap = extendedMask ? 2 : 0;
        if (mask.cols() != dimX + extensionGap || mask.rows() != dimY + extensionGap) {
            throw new IllegalArgumentException("Illegal mask sizes: the expected sizes (source image?) are "
                    + dimX + "x" + dimY + ", the mask is " + mask.cols() + "x" + mask.rows()
                    + (extendedMask ? ", but the mask must be 2 pixels wider and 2 pixels taller than it" : ""));
        }
    }
}
