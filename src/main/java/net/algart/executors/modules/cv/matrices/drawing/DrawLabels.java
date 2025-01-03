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

import net.algart.arrays.Matrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.executors.modules.core.matrices.geometry.Resize;
import net.algart.executors.modules.cv.matrices.misc.Selector;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.executors.modules.cv.matrices.morphology.StrictMorphology;
import net.algart.executors.modules.cv.matrices.objects.markers.PaintLabelledObjects;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.List;
import java.util.Objects;

public final class DrawLabels extends SeveralMultiMatricesOperation {
    public static final String LABELS = "labels";

    public enum DrawnFeatures {
        NON_ZERO_LABELS(false, false, false),
        RANDOMLY_COLORED_LABELS(false, false, false),
        BOUNDARIES_BETWEEN_LABELS(true, false, false),
        ZERO_BACKGROUND_AND_BOUNDARIES_BETWEEN_NONZERO_LABELS(true, false, true),
        THICK_BOUNDARIES_BETWEEN_LABELS(true, true, false),
        ZERO_BACKGROUND_AND_THICK_BOUNDARIES_BETWEEN_NONZERO_LABELS(true, true, true);

        private final boolean boundaries;
        private final boolean thick;
        private final boolean zeroBackground;

        DrawnFeatures(boolean boundaries, boolean thick, boolean zeroBackground) {
            this.boundaries = boundaries;
            this.thick = thick;
            this.zeroBackground = zeroBackground;
        }
    }

    public DrawLabels() {
        super(DEFAULT_INPUT_PORT, LABELS);
    }

    private DrawnFeatures drawnFeatures = DrawnFeatures.BOUNDARIES_BETWEEN_LABELS;
    private String color = "#FFFFFF";
    private boolean convertMonoToColor = true;
    private int scale = 1;
    private long randSeed = 0;
    // 0 means really random: new sequence for each call

    public DrawnFeatures getDrawnFeatures() {
        return drawnFeatures;
    }

    public DrawLabels setDrawnFeatures(DrawnFeatures drawnFeatures) {
        this.drawnFeatures = nonNull(drawnFeatures);
        return this;
    }

    public String getColor() {
        return color;
    }

    public DrawLabels setColor(String color) {
        this.color = nonNull(color);
        return this;
    }

    public boolean isConvertMonoToColor() {
        return convertMonoToColor;
    }

    public DrawLabels setConvertMonoToColor(boolean convertMonoToColor) {
        this.convertMonoToColor = convertMonoToColor;
        return this;
    }

    public int getScale() {
        return scale;
    }

    public DrawLabels setScale(int scale) {
        this.scale = positive(scale);
        return this;
    }

    public long getRandSeed() {
        return randSeed;
    }

    public DrawLabels setRandSeed(long randSeed) {
        this.randSeed = randSeed;
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        Objects.requireNonNull(sources, "Null sources");
        MultiMatrix2D source = sources.get(0) == null ? null : sources.get(0).asMultiMatrix2D();
        if (convertMonoToColor && source != null && source.isMono()) {
            source = source.asOtherNumberOfChannels(3);
            if (source.bitsPerElement() == 1) {
                source = source.asPrecision(byte.class);
            }
        }
        Objects.requireNonNull(sources.get(1), "Null sources.get(1) (labels matrix)");
        MultiMatrix2D labels = sources.get(1).asMultiMatrix2D();
        labels.checkDimensionEquality(source, "labels", "source");
        if (scale > 1) {
            final Resize resize = new Resize();
            resize.setResizingMode(Resize.ResizingMode.NEAREST);
            resize.setDimX(labels.dimX() * scale);
            resize.setDimY(labels.dimY() * scale);
            labels = resize.process(labels);
            if (source != null) {
                source = resize.process(source);
            }
        }
        if (drawnFeatures == DrawnFeatures.RANDOMLY_COLORED_LABELS) {
            try (PaintLabelledObjects paintLabelledObjects = new PaintLabelledObjects()) {
                paintLabelledObjects.setRandomPalette(true).setRandSeed(randSeed);
                return paintLabelledObjects.process(labels, source, null);
            }
        }
        final MultiMatrix2D drawn;
        if (drawnFeatures.boundaries) {
            //TODO!! replace with sphere for multidimensional
            try (StrictMorphology morphologyGradient = new StrictMorphology()) {
                morphologyGradient.setOperation(MorphologyOperation.BEUCHER_GRADIENT);
                morphologyGradient.setCustomPattern(drawnFeatures.thick ?
                        MorphologyFilter.Shape.SPHERE.newPattern(labels.dimCount(), 4) :
                        MorphologyOperation.crossPattern(labels.dimCount()));
                assert MorphologyOperation.crossPattern(labels.dimCount()).equals(
                        MorphologyFilter.Shape.SPHERE.newPattern(labels.dimCount(), 2)) :
                        MorphologyOperation.crossPattern(labels.dimCount()) + "!="
                                + MorphologyFilter.Shape.SPHERE.newPattern(labels.dimCount(), 2);
                morphologyGradient.setContinuationMode(Matrix.ContinuationMode.MIRROR_CYCLIC);
                drawn = morphologyGradient.process(labels).asMultiMatrix2D().nonZeroPixels(false);
            }
        } else {
            drawn = labels.nonZeroPixels(false);
        }
        MultiMatrix2D result;
        try (Selector selector = new Selector()) {
            selector.setFiller(0, "#000000");
            selector.setFiller(1, color);
            if (convertMonoToColor) {
                selector.setMinimalRequiredNumberOfChannels(3);
            }
            result = selector.process(drawn, source, null).asMultiMatrix2D();
        }
        if (drawnFeatures.zeroBackground) {
            result = result.min(labels.nonZeroAnyChannel()).clone();
        }
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex == 0;
        // - source can be omitted
    }
}
