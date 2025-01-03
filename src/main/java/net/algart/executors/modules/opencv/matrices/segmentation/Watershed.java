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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.cv.matrices.misc.Selector;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.executors.modules.cv.matrices.morphology.StrictMorphology;
import net.algart.executors.modules.cv.matrices.thresholds.SimpleThreshold;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.modules.core.matrices.geometry.ContinuationMode;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.UMat;

import java.util.Locale;
import java.util.Objects;

public final class Watershed extends AbstractSegmentationWithBoundaries {
    public static final String INPUT_LABELS = "labels";
    public static final String OUTPUT_LABELS = "labels";
    public static final String OUTPUT_SOURCE_LABELS = "source_labels";

    private static final Scalar zeroScalar = new Scalar(0.0, 0.0, 0.0, 0.0);
    private static final Scalar unitScalar = new Scalar(1.0);
    // - must not be public: it is mutable

    public enum SeedingMode {
        SEEDING_LABELS(false),
        ONE_FOREGROUND_AND_ONE_BACKGROUND(true),
        CONNECTED_COMPONENTS_ONLY(false),
        CONNECTED_COMPONENTS_AND_ONE_BACKGROUND(true);

        private final boolean backgroundUsed;

        SeedingMode(boolean backgroundUsed) {
            this.backgroundUsed = backgroundUsed;
        }
    }

    public enum ValuesOnBoundaries {
        MINUS_ONE,
        ZERO,
        NEAREST_LABEL
    }

    private SeedingMode seedingMode = SeedingMode.SEEDING_LABELS;
    private int autoLabellingForegroundErosionKernelSize = 0;
    private int autoLabellingBackgroundErosionKernelSize = 32;
    private ValuesOnBoundaries valuesOnBoundaries = ValuesOnBoundaries.MINUS_ONE;

    public Watershed() {
        addInputMat(INPUT_LABELS);
        setDefaultOutputMat(OUTPUT_LABELS);
        addOutputMat(OUTPUT_SOURCE_LABELS);
    }

    public SeedingMode getSeedingMode() {
        return seedingMode;
    }

    public Watershed setSeedingMode(SeedingMode seedingMode) {
        this.seedingMode = nonNull(seedingMode);
        return this;
    }

    public int getAutoLabellingForegroundErosionKernelSize() {
        return autoLabellingForegroundErosionKernelSize;
    }

    public Watershed setAutoLabellingForegroundErosionKernelSize(int autoLabellingForegroundErosionKernelSize) {
        this.autoLabellingForegroundErosionKernelSize = nonNegative(autoLabellingForegroundErosionKernelSize);
        return this;
    }

    public int getAutoLabellingBackgroundErosionKernelSize() {
        return autoLabellingBackgroundErosionKernelSize;
    }

    public Watershed setAutoLabellingBackgroundErosionKernelSize(int autoLabellingBackgroundErosionKernelSize) {
        this.autoLabellingBackgroundErosionKernelSize = nonNegative(autoLabellingBackgroundErosionKernelSize);
        return this;
    }

    public ValuesOnBoundaries getValuesOnBoundaries() {
        return valuesOnBoundaries;
    }

    public Watershed setValuesOnBoundaries(ValuesOnBoundaries valuesOnBoundaries) {
        this.valuesOnBoundaries = nonNull(valuesOnBoundaries);
        return this;
    }

    @Override
    public Mat process(Mat source) {
        final boolean labelsAreSpecifiedByUser = seedingMode == SeedingMode.SEEDING_LABELS;
        Mat labels = O2SMat.toMat(
                getInputMat(INPUT_LABELS, !labelsAreSpecifiedByUser),
                true);
        if (source == null && labels == null) {
            throw new IllegalArgumentException("Source input or labels must be specified");
        }
        return process(source, labels);
    }

    @Override
    public UMat process(UMat source) {
        final boolean labelsAreSpecifiedByUser = seedingMode == SeedingMode.SEEDING_LABELS;
        UMat labels = O2SMat.toUMat(
                getInputMat(INPUT_LABELS, !labelsAreSpecifiedByUser),
                true);
        if (source == null && labels == null) {
            throw new IllegalArgumentException("Source input or labels must be specified");
        }
        return process(source, labels);
    }

    public Mat threshold(Mat mat, boolean matIsLabels) {
        final Mat result = new Mat();
        if (mat.channels() == 1) {
            mat.copyTo(result);
        } else {
            opencv_imgproc.cvtColor(mat, result, opencv_imgproc.CV_BGR2GRAY);
        }
        if (result.depth() != opencv_core.CV_8U) {
            result.convertTo(result, opencv_core.CV_8U);
        }
        opencv_imgproc.threshold(result, result, 0, 255,
                matIsLabels ? opencv_imgproc.CV_THRESH_BINARY : opencv_imgproc.CV_THRESH_OTSU);
        return result;
    }

    public UMat threshold(UMat mat, boolean matIsLabels) {
        final UMat result = new UMat();
        if (mat.channels() == 1) {
            mat.copyTo(result);
        } else {
            opencv_imgproc.cvtColor(mat, result, opencv_imgproc.CV_BGR2GRAY);
        }
        if (result.depth() != opencv_core.CV_8U) {
            result.convertTo(result, opencv_core.CV_8U);
        }
        opencv_imgproc.threshold(result, result, 0, 255,
                matIsLabels ? opencv_imgproc.CV_THRESH_BINARY : opencv_imgproc.CV_THRESH_OTSU);
        return result;
    }

    public Mat createLabels(Mat foreground) {
        final Mat background = seedingMode.backgroundUsed ? new Mat() : null;
        try {
            if (background != null) {
                opencv_core.bitwise_not(foreground, background);
                OTools.morphology(background, opencv_imgproc.MORPH_ERODE, opencv_imgproc.CV_SHAPE_RECT,
                        autoLabellingBackgroundErosionKernelSize);
            }
            OTools.morphology(foreground, opencv_imgproc.MORPH_ERODE, opencv_imgproc.CV_SHAPE_RECT,
                    autoLabellingForegroundErosionKernelSize);
            Mat result = OTools.newCompatibleMat(foreground, opencv_core.CV_32S);
            switch (seedingMode) {
                case ONE_FOREGROUND_AND_ONE_BACKGROUND: {
                    opencv_core.addWeighted(
                            background, 1.0 / 255.0,
                            foreground, 2.0 / 255.0,
                            0.0,
                            result,
                            opencv_core.CV_32S);
                    return result;
                }
                case CONNECTED_COMPONENTS_ONLY: {
                    int n = opencv_imgproc.connectedComponents(foreground, result, 8, opencv_core.CV_32S);
                    logDebug(() -> "Watershed: automatic creating " + (n - 1) + " foreground labels");
                    return result;
                }
                case CONNECTED_COMPONENTS_AND_ONE_BACKGROUND: {
                    int n = opencv_imgproc.connectedComponents(foreground, result, 8, opencv_core.CV_32S);
                    logDebug(() -> "Watershed: automatic creating " + (n - 1) + " foreground + 1 background labels");
                    try (final Mat constant1 = new Mat(
                            result.rows(), result.cols(), result.type(), unitScalar)) {
                        opencv_core.add(result, constant1, result, foreground, opencv_core.CV_32S);
                        // - increasing by 1, but only at foreground (0 stays to be 0)
                        opencv_core.add(result, constant1, result, background, opencv_core.CV_32S);
                        // - increasing by 1 at background
                    }
                    return result;
                }
                default: {
                    throw new AssertionError("Unallowed mode " + seedingMode);
                }
            }
        } finally {
            if (background != null) {
                background.close();
            }
        }
    }

    public UMat createLabels(UMat foreground) {
        final UMat background = seedingMode.backgroundUsed ? new UMat() : null;
        try {
            if (background != null) {
                opencv_core.bitwise_not(foreground, background);
                OTools.morphology(background, opencv_imgproc.MORPH_ERODE, opencv_imgproc.CV_SHAPE_RECT,
                        autoLabellingBackgroundErosionKernelSize);
            }
            OTools.morphology(foreground, opencv_imgproc.MORPH_ERODE, opencv_imgproc.CV_SHAPE_RECT,
                    autoLabellingForegroundErosionKernelSize);
            UMat result = OTools.newCompatibleUMat(foreground, opencv_core.CV_32S);
            switch (seedingMode) {
                case ONE_FOREGROUND_AND_ONE_BACKGROUND: {
                    opencv_core.addWeighted(
                            background, 1.0 / 255.0,
                            foreground, 2.0 / 255.0,
                            0.0,
                            result,
                            opencv_core.CV_32S);
                    return result;
                }
                case CONNECTED_COMPONENTS_ONLY: {
                    int n = opencv_imgproc.connectedComponents(foreground, result, 8, opencv_core.CV_32S);
                    logDebug(() -> "Watershed: automatic creating " + (n - 1) + " foreground labels");
                    return result;
                }
                case CONNECTED_COMPONENTS_AND_ONE_BACKGROUND: {
                    int n = opencv_imgproc.connectedComponents(foreground, result, 8, opencv_core.CV_32S);
                    logDebug(() -> "Watershed: automatic creating " + (n - 1) + " foreground + 1 background labels");
                    try (final UMat constant1 = new UMat(
                            result.rows(), result.cols(), result.type(), unitScalar)) {
                        opencv_core.add(result, constant1, result, foreground, opencv_core.CV_32S);
                        // - increasing by 1, but only at foreground (0 stays to be 0)
                        opencv_core.add(result, constant1, result, background, opencv_core.CV_32S);
                        // - increasing by 1 at background
                    }
                    return result;
                }
                default: {
                    throw new AssertionError("Unallowed mode " + seedingMode);
                }
            }
        } finally {
            if (background != null) {
                background.close();
            }
        }
    }

    public Mat process(final Mat source, Mat labelsAndResult) {
        long t1 = debugTime(), t2;
        final boolean labelsAreSpecifiedByUser = seedingMode == SeedingMode.SEEDING_LABELS;
        final Mat labelsAndResultOriginal = labelsAndResult;
        if (labelsAreSpecifiedByUser) {
            Objects.requireNonNull(labelsAndResult, "Null labels");
            logDebug(() -> "Watershed for given labels (source: " + OTools.toString(source) + ")");
            t2 = t1;
        } else {
            long tStart = System.nanoTime();
            try (Mat bitMat = threshold(labelsAndResult != null ? labelsAndResult : source,
                    labelsAndResult != null)) {
                t2 = debugTime();
                logDebug(() -> "Watershed, automatic threshold of "
                        + (labelsAndResultOriginal == null ? "source (" + OTools.toString(source)
                        + ")" : "labels (" + OTools.toString(labelsAndResultOriginal) + ")"));
                labelsAndResult = createLabels(bitMat);
                addServiceTime(System.nanoTime() - tStart);
            }
        }
        if (labelsAndResult.depth() != opencv_core.CV_32S) {
            throw new IllegalArgumentException("Watershed labels must be 32-bit integers (CV_32S)");
        }
        long t3 = debugTime();
        Mat mat = source;
        try {
            if (mat == null) {
                mat = new Mat(
                        labelsAndResult.rows(), labelsAndResult.cols(), opencv_core.CV_8UC3, zeroScalar);
            }
            final boolean outputSourceLabelsNecessary = isOutputNecessary(OUTPUT_SOURCE_LABELS);
            final Mat labelsClone = outputSourceLabelsNecessary ? labelsAndResult.clone() : null;
            if (mat.channels() == 1) {
                // OpenCV watershed does not work with gray scale images
                opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.CV_GRAY2BGR);
            }
            long t4 = debugTime();
            opencv_imgproc.watershed(mat, labelsAndResult);
            long t5 = debugTime();

            setEndProcessingTimeStamp();
            if (outputSourceLabelsNecessary) {
                O2SMat.setTo(getMat(OUTPUT_SOURCE_LABELS), labelsClone);
            }
            setToOutputBoundaries((boundaries, labels, needThick) -> {
                labels.convertTo(boundaries, opencv_core.CV_32F);
                opencv_imgproc.threshold(boundaries, boundaries,
                        -1, 255, opencv_imgproc.CV_THRESH_BINARY_INV);
            }, mat, labelsAndResult);
            long t6 = debugTime();
            switch (valuesOnBoundaries) {
                case ZERO: {
                    opencv_core.max(labelsAndResult, 0.0).asMat().copyTo(labelsAndResult);
                    break;
                }
                case NEAREST_LABEL: {
                    MultiMatrix2D resultMatrix = O2SMat.toMultiMatrix(labelsAndResult).asMono();
                    // - Relatively quick operation: it is single-channel matrix, no repacking necessary.
                    // (asMono - to be on the safe side; really it will be single-channel.)

                    for (int iteration = 0; iteration < 2; iteration++) {
                        // 2nd iteration is necessary in rare situation near the image bounds:
                        //     y=8:  -1  89  89  -1  -1
                        //     y=9:  -1  89  -1 212 212
                        //     y=10: -1  -1 212 212 212
                        //     y=11: -1   0  -1  -1  -1
                        //     y=12: -1  -1 251 251 251
                        //     y=13: -1 291  -1 251 251
                        // For x=0,y=11 we cannot reach positive label via 1 dilation; it will be changed to 0
                        // (fomr x=1,y=11). And we need 2nd iteration to replace this 0 to positive label.

                        final MultiMatrix2D binaryBoundaries = new SimpleThreshold()
                                .setMin(Double.NEGATIVE_INFINITY).setMax(0.0)
                                .process(resultMatrix).clone();
                        // Boundaries will be not only -1, but also 0: sometimes watershed algorithm "stays" 1-pixel
                        // zero fragments from the background.
                        final MultiMatrix2D dilated = new StrictMorphology()
                                .setOperation(MorphologyOperation.DILATION)
                                .setPattern(MorphologyFilter.Shape.CUBE, 3)
                                .setContinuationMode(ContinuationMode.NEGATIVE_INFINITY)
                                .process(resultMatrix).asMultiMatrix2D();
                        resultMatrix = new Selector().process(
                                binaryBoundaries, resultMatrix, dilated).asMultiMatrix2D();
                        // - On boundaries we will use little dilated matrix, on other points - unchanged resultMatrix.
                    }
                    labelsAndResult.close();
                    labelsAndResult = O2SMat.toMat(resultMatrix);
                    break;
                }
            }
            long t7 = debugTime();
            if (LOGGABLE_DEBUG) {
                logDebug(String.format(Locale.US,
                        "Watershed for %d-bit Mat %dx%dx%d, %s: "
                                + "%.3f ms preprocess = %.3f threshold + %.3f ms finding components; "
                                + " %.3f ms preparing, %.3f OpenCV watershed; "
                                + "%.3f postprocessing: %.3f boundaries + %.3f correcting result",
                        8 * mat.elemSize1(), mat.channels(), mat.cols(), mat.rows(), seedingMode,
                        (t3 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
                        (t4 - t3) * 1e-6, (t5 - t4) * 1e-6,
                        (t7 - t5) * 1e-6, (t6 - t5) * 1e-6, (t7 - t6) * 1e-6));
            }
            return labelsAndResult;
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    public UMat process(final UMat source, UMat labelsAndResult) {
        long t1 = debugTime(), t2;
        final boolean labelsAreSpecifiedByUser = seedingMode == SeedingMode.SEEDING_LABELS;
        final UMat labelsAndResultOriginal = labelsAndResult;
        if (labelsAreSpecifiedByUser) {
            Objects.requireNonNull(labelsAndResult, "Null labels");
            logDebug(() -> "Watershed for given labels (source: " + OTools.toString(source) + ")");
            t2 = t1;
        } else {
            long tStart = System.nanoTime();
            try (UMat bitMat = threshold(labelsAndResult != null ? labelsAndResult : source,
                    labelsAndResult != null)) {
                t2 = debugTime();
                logDebug(() -> "Watershed, automatic threshold of "
                        + (labelsAndResultOriginal == null ? "source (" + OTools.toString(source)
                        + ")" : "labels (" + OTools.toString(labelsAndResultOriginal) + ")"));
                labelsAndResult = createLabels(bitMat);
                addServiceTime(System.nanoTime() - tStart);
            }
        }
        if (labelsAndResult.depth() != opencv_core.CV_32S) {
            throw new IllegalArgumentException("Watershed labels must be 32-bit integers (CV_32S)");
        }
        long t3 = debugTime();
        UMat mat = source;
        try {
            if (mat == null) {
                mat = new UMat(
                        labelsAndResult.rows(), labelsAndResult.cols(), opencv_core.CV_8UC3, zeroScalar);
            }
            final boolean outputSourceLabelsNecessary = isOutputNecessary(OUTPUT_SOURCE_LABELS);
            final UMat labelsClone = outputSourceLabelsNecessary ? labelsAndResult.clone() : null;
            if (mat.channels() == 1) {
                // OpenCV watershed does not work with gray scale images
                opencv_imgproc.cvtColor(mat, mat, opencv_imgproc.CV_GRAY2BGR);
            }
            long t4 = debugTime();
            opencv_imgproc.watershed(mat, labelsAndResult);
            long t5 = debugTime();

            setEndProcessingTimeStamp();
            if (outputSourceLabelsNecessary) {
                O2SMat.setTo(getMat(OUTPUT_SOURCE_LABELS), labelsClone);
            }
            setToOutputBoundaries((boundaries, labels, needThick) -> {
                labels.convertTo(boundaries, opencv_core.CV_32F);
                opencv_imgproc.threshold(boundaries, boundaries,
                        -1, 255, opencv_imgproc.CV_THRESH_BINARY_INV);
            }, mat, labelsAndResult);
            long t6 = debugTime();
            switch (valuesOnBoundaries) {
                case ZERO: {
                    try (UMat constant0 = OTools.newCompatibleZeros(labelsAndResult)) {
                        opencv_core.max(labelsAndResult, constant0, labelsAndResult);
                    }
                    break;
                }
                case NEAREST_LABEL: {
                    MultiMatrix2D resultMatrix = O2SMat.toMultiMatrix(labelsAndResult).asMono();
                    // - Relatively quick operation: it is single-channel matrix, no repacking necessary.
                    // (asMono - to be on the safe side; really it will be single-channel.)

                    for (int iteration = 0; iteration < 2; iteration++) {
                        // 2nd iteration is necessary in rare situation near the image bounds:
                        //     y=8:  -1  89  89  -1  -1
                        //     y=9:  -1  89  -1 212 212
                        //     y=10: -1  -1 212 212 212
                        //     y=11: -1   0  -1  -1  -1
                        //     y=12: -1  -1 251 251 251
                        //     y=13: -1 291  -1 251 251
                        // For x=0,y=11 we cannot reach positive label via 1 dilation; it will be changed to 0
                        // (fomr x=1,y=11). And we need 2nd iteration to replace this 0 to positive label.

                        final MultiMatrix2D binaryBoundaries = new SimpleThreshold()
                                .setMin(Double.NEGATIVE_INFINITY).setMax(0.0)
                                .process(resultMatrix).clone();
                        // Boundaries will be not only -1, but also 0: sometimes watershed algorithm "stays" 1-pixel
                        // zero fragments from the background.
                        final MultiMatrix2D dilated = new StrictMorphology()
                                .setOperation(MorphologyOperation.DILATION)
                                .setPattern(MorphologyFilter.Shape.CUBE, 3)
                                .setContinuationMode(ContinuationMode.NEGATIVE_INFINITY)
                                .process(resultMatrix).asMultiMatrix2D();
                        resultMatrix = new Selector().process(
                                binaryBoundaries, resultMatrix, dilated).asMultiMatrix2D();
                        // - On boundaries we will use little dilated matrix, on other points - unchanged resultMatrix.
                    }
                    labelsAndResult.close();
                    labelsAndResult = O2SMat.toUMat(resultMatrix);
                    break;
                }
            }
            long t7 = debugTime();
            if (LOGGABLE_DEBUG) {
                logDebug(String.format(Locale.US,
                        "Watershed for %d-bit UMat %dx%dx%d (GPU), %s: "
                                + "%.3f ms preprocess = %.3f threshold + %.3f ms finding components; "
                                + " %.3f ms preparing, %.3f OpenCV watershed; "
                                + "%.3f postprocessing: %.3f boundaries + %.3f correcting result",
                        8 * mat.elemSize1(), mat.channels(), mat.cols(), mat.rows(), seedingMode,
                        (t3 - t1) * 1e-6, (t2 - t1) * 1e-6, (t3 - t2) * 1e-6,
                        (t4 - t3) * 1e-6, (t5 - t4) * 1e-6,
                        (t7 - t5) * 1e-6, (t6 - t5) * 1e-6, (t7 - t6) * 1e-6));
            }
            return labelsAndResult;
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    @Override
    protected boolean allowUninitializedInput() {
        return true;
    }

}
