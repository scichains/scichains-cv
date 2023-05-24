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

package net.algart.executors.modules.opencv.matrices.misc;

import net.algart.executors.modules.opencv.common.VoidResultMatFilter;
import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.executors.modules.util.opencv.enums.OGrabCutMode;
import net.algart.arrays.Arrays;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.util.stream.Stream;

public final class GrabCut extends VoidResultMatFilter {
    public static final String INPUT_MASK = "mask";
    public static final String INPUT_FIGURE = "figure";
    public static final String OUTPUT_MASK = "mask";
    public static final String OUTPUT_SUCCESS = "success";

    public enum FigureKind {
        NONE(-1),
        GC_BGD(opencv_imgproc.GC_BGD),
        GC_FGD(opencv_imgproc.GC_FGD),
        GC_PR_BGD(opencv_imgproc.GC_PR_BGD),
        GC_PR_FGD(opencv_imgproc.GC_PR_FGD);

        private final int maskCode;

        FigureKind(int maskCode) {
            this.maskCode = maskCode;
        }
    }

    public enum MaskPixelClass {
        GC_BGD(opencv_imgproc.GC_BGD, "mask_bgd"),
        GC_FGD(opencv_imgproc.GC_FGD, "mask_fgd"),
        GC_PR_BGD(opencv_imgproc.GC_PR_BGD, "mask_pr_bgd"),
        GC_PR_FGD(opencv_imgproc.GC_PR_FGD, "mask_pr_fgd");
        // - Note: these values must be enumerated in increasing order of "code"!
        // It is used in makeBinaryMasks()

        private final int maskPixelClass;
        private final String outputPortName;

        public int maskPixelClass() {
            return maskPixelClass;
        }

        public String outputPortName() {
            return outputPortName;
        }

        MaskPixelClass(int maskPixelClass, String outputPortName) {
            this.maskPixelClass = maskPixelClass;
            this.outputPortName = outputPortName;
        }
    }

    private boolean reset = true;
    private OGrabCutMode mode = OGrabCutMode.GC_INIT_WITH_RECT;
    private FigureKind figureKind = FigureKind.GC_FGD;
    private MaskPixelClass initialMaskFiller = MaskPixelClass.GC_PR_BGD;
    private boolean requireNonTrivialSamples = true;
    private boolean percents = false;
    private double startX = 0;
    private double startY = 0;
    private double sizeX = 1;
    private double sizeY = 1;
    private int iterCount = 1;
    private boolean packBits = true;

    private Mat storedMask = new Mat();
    private Mat storedBgdModel = new Mat();
    private Mat storedFgdModel = new Mat();

    public GrabCut() {
        addInputMat(INPUT_MASK);
        addInputMat(INPUT_FIGURE);
        setDefaultOutputMat(OUTPUT_MASK);
        for (MaskPixelClass maskPixelClass : MaskPixelClass.values()) {
            addOutputMat(maskPixelClass.outputPortName);
        }
        addOutputScalar(OUTPUT_SUCCESS);
    }

    public boolean isReset() {
        return reset;
    }

    public GrabCut setReset(boolean reset) {
        this.reset = reset;
        return this;
    }

    public OGrabCutMode getMode() {
        return mode;
    }

    public GrabCut setMode(OGrabCutMode mode) {
        this.mode = nonNull(mode);
        return this;
    }

    public FigureKind getFigureKind() {
        return figureKind;
    }

    public GrabCut setFigureKind(FigureKind figureKind) {
        this.figureKind = nonNull(figureKind);
        return this;
    }

    public MaskPixelClass getInitialMaskFiller() {
        return initialMaskFiller;
    }

    public GrabCut setInitialMaskFiller(MaskPixelClass initialMaskFiller) {
        this.initialMaskFiller = nonNull(initialMaskFiller);
        return this;
    }

    public boolean isRequireNonTrivialSamples() {
        return requireNonTrivialSamples;
    }

    public GrabCut setRequireNonTrivialSamples(boolean requireNonTrivialSamples) {
        this.requireNonTrivialSamples = requireNonTrivialSamples;
        return this;
    }

    public boolean isPercents() {
        return percents;
    }

    public GrabCut setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getStartX() {
        return startX;
    }

    public GrabCut setStartX(double startX) {
        this.startX = startX;
        return this;
    }

    public double getStartY() {
        return startY;
    }

    public GrabCut setStartY(double startY) {
        this.startY = startY;
        return this;
    }

    public double getSizeX() {
        return sizeX;
    }

    public GrabCut setSizeX(double sizeX) {
        this.sizeX = nonNegative(sizeX);
        return this;
    }

    public double getSizeY() {
        return sizeY;
    }

    public GrabCut setSizeY(double sizeY) {
        this.sizeY = nonNegative(sizeY);
        return this;
    }

    public int getIterCount() {
        return iterCount;
    }

    public GrabCut setIterCount(int iterCount) {
        this.iterCount = nonNegative(iterCount);
        return this;
    }

    public boolean isPackBits() {
        return packBits;
    }

    public GrabCut setPackBits(boolean packBits) {
        this.packBits = packBits;
        return this;
    }

    @Override
    public void process(Mat result, Mat source) {
        process(result,
                source,
                O2SMat.toMat(getInputMat(INPUT_MASK, true)),
                O2SMat.toMat(getInputMat(INPUT_FIGURE, true), true));
    }

    public void process(
            Mat resultMask,
            Mat source,
            Mat initialMask,
            Mat correctingFigureMask) {
        getScalar(OUTPUT_SUCCESS).remove();
        final boolean useFigure = correctingFigureMask != null && figureKind != FigureKind.NONE;
        final OGrabCutMode mode = this.mode;
        if (reset) {
            if (!mode.initialization()) {
                throw new IllegalStateException("Reset flag is incompatible with the mode " + mode
                        + ": you must specify GC_INIT_WITH_MASK or GC_INIT_WITH_RECT mode");
            }
            if (mode == OGrabCutMode.GC_INIT_WITH_MASK && initialMask == null && !useFigure) {
                throw new IllegalStateException("While resetting in the mode " + mode
                        + ", you must specify some initial mask and/or the input figure, "
                        + "that will be drawn over the mask/mask filler; "
                        + "the mask (after adding the figure) must contain at least one GC_BGD/GC_PR_BGD and "
                        + "at least one GC_FGD/GC_PR_FGD element");
            }
            storedMask.close();
            storedMask = OTools.constantMonoMat8U(source.cols(), source.rows(), initialMaskFiller.maskPixelClass());
            storedBgdModel.close();
            storedBgdModel = new Mat();
            storedFgdModel.close();
            storedFgdModel = new Mat();
        }
        if (source.channels() == 1) {
            // grabCut  does not work with grayscale images
            opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_GRAY2BGR);
        }
        if (initialMask != null) {
            if (initialMask.type() != opencv_core.CV_8U) {
                throw new IllegalArgumentException("Illegal initial mask type (must be CV_8U): " + initialMask);
            }
            if (mode == OGrabCutMode.GC_INIT_WITH_MASK) {
                initialMask.copyTo(storedMask);
            }
        }
        if (useFigure) {
            insertMask(storedMask, correctingFigureMask, figureKind.maskCode);
        }
        final int nonTrivialSamples = nonTrivialSamples(storedMask);
        final boolean runGrabCut = mode != OGrabCutMode.GC_INIT_WITH_MASK || nonTrivialSamples > 0;
        if (runGrabCut) {
            // - in other case, grabCut will throw its own assertion
            try (Rect rect = createRect(source.cols(), source.rows())) {
                opencv_imgproc.grabCut(
                        source, storedMask, rect, storedBgdModel, storedFgdModel, iterCount, mode.code());
            }
        } else {
            if (requireNonTrivialSamples) {
                throw new IllegalArgumentException("Illegal mask"
                        + (useFigure ? " (after adding the figure)" : "")
                        + ": all samples are "
                        + (nonTrivialSamples == 0 ?
                        "background or possible background" :
                        "foreground or possible foreground")
                        + ", but there must be both background and foreground samples");
            }
        }
        getScalar(OUTPUT_SUCCESS).setTo(runGrabCut);
        setEndProcessingTimeStamp();
        storedMask.copyTo(resultMask);
        makeBinaryMasks(storedMask);
    }

    @Override
    public void close() {
        storedMask.close();
        storedBgdModel.close();
        storedFgdModel.close();
        super.close();
    }

    private Rect createRect(int dimX, int dimY) {
        final int left = Arrays.round32(percents ? startX / 100.0 * Math.max(0, dimX - 1) : startX);
        final int top = Arrays.round32(percents ? startY / 100.0 * Math.max(0, dimY - 1) : startY);
        final int width = sizeX == 0.0 ? dimX - left : Arrays.round32(percents ? sizeX / 100.0 * dimX : sizeX);
        final int height = sizeY == 0.0 ? dimY - top : Arrays.round32(percents ? sizeY / 100.0 * dimY : sizeY);
        return new Rect(left, top, width, height);
    }

    private void makeBinaryMasks(Mat maskMat) {
        final MaskPixelClass[] maskPixelClasses = MaskPixelClass.values();
        if (Stream.of(maskPixelClasses).noneMatch(maskCode -> isOutputNecessary(maskCode.outputPortName()))) {
            return;
        }
        try (PointerScope scope = new PointerScope()) {
            final Mat[] thresholds = new Mat[maskPixelClasses.length];
            for (int k = 0; k < maskPixelClasses.length; k++) {
                //noinspection resource
                thresholds[k] = new Mat();
                opencv_imgproc.threshold(
                        maskMat,
                        thresholds[k],
                        maskPixelClasses[k].maskPixelClass,
                        255,
                        opencv_imgproc.THRESH_BINARY_INV);
                // 255 if <=code, 0 if >code
            }
            for (int k = maskPixelClasses.length - 1; k > 0; k--) {
                opencv_core.bitwise_xor(thresholds[k - 1], thresholds[k], thresholds[k]);
                // thresholds[k] = 255 if ==maskPixelClasses[k].code
                // (excepting k=0, where we already have correct result)
            }
            for (int k = 0; k < maskPixelClasses.length; k++) {
                final Mat resultMask = thresholds[k];
                if (packBits) {
                    getMat(maskPixelClasses[k].outputPortName()).setTo(O2SMat.toBinaryMatrix(resultMask));
                    // - after this conversion we can close resultMask matrix inside "scope"
                } else {
                    O2SMat.setTo(getMat(maskPixelClasses[k].outputPortName()), resultMask);
                    scope.detach(resultMask);
                    // - we need to return this mask as one of results and must not deallocate it
                }
            }
        }
    }

    private static int nonTrivialSamples(Mat mask) {
        assert mask.type() == opencv_core.CV_8U;
        final byte[] bytes = OTools.toByteArray(mask);
        boolean hasBackground = false;
        boolean hasForeground = false;
        for (int b : bytes) {
            switch (b) {
                case opencv_imgproc.GC_BGD:
                case opencv_imgproc.GC_PR_BGD: {
                    hasBackground = true;
                    break;
                }
                case opencv_imgproc.GC_FGD:
                case opencv_imgproc.GC_PR_FGD: {
                    hasForeground = true;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid mask: it contains element " + (b & 0xFF)
                            + ", differ from GC_BGD=" + opencv_imgproc.GC_BGD
                            + ", GC_FGD=" + opencv_imgproc.GC_FGD
                            + ", GC_PR_BGD=" + opencv_imgproc.GC_PR_BGD
                            + " and GC_PR_FGD=" + opencv_imgproc.GC_PR_FGD);
                }
            }
        }
        return hasBackground && hasForeground ? 1 : hasBackground ? 0 : -1;
    }

    private static void insertMask(Mat result, Mat mask, int value) {
        try (PointerScope scope = new PointerScope()) {
            mask = OTools.toMono8UIfNot(mask);
            final Mat constant = OTools.constantMonoMat8U(mask.cols(), mask.rows(), value);
            opencv_core.copyTo(constant, result, mask);
        }
    }
}
