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

package net.algart.executors.modules.cv.matrices.objects.markers;

import net.algart.arrays.*;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;
import net.algart.executors.modules.core.numbers.conversions.JsonToColorPalette;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// Can be used for filtering objects (like SelectLabelledObjectsByBinaryArray), if elementType is boolean and colors
// are 0/1
public final class PaintLabelledObjects extends MultiMatrix2DFilter {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_BACKGROUND = "background";
    public static final String INPUT_PALETTE = "palette";
    public static final String INPUT_JSON_PALETTE = "json_palette";
    public static final String INPUT_JSON_NAMED_INDEXES = "json_named_indexes";

    private static final int ALPHA_CHANNEL = 3;

    private Class<?> elementType = byte.class;
    private boolean rawValues = false;
    private boolean randomPalette = false;
    private long randSeed = 0;
    // 0 means really random: new sequence for each call
    private int indexingBase = 1;
    private boolean processAlpha = false;
    private int defaultNumberOfChannels = 3;

    public PaintLabelledObjects() {
        setDefaultInputMat(INPUT_LABELS);
        addInputMat(INPUT_BACKGROUND);
        addInputNumbers(INPUT_PALETTE);
        addInputScalar(INPUT_JSON_PALETTE);
        addInputScalar(INPUT_JSON_NAMED_INDEXES);
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public PaintLabelledObjects setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public final PaintLabelledObjects setElementType(String elementType) {
        return setElementType(MultiMatrixGenerator.elementType(elementType));
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public PaintLabelledObjects setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public boolean isRandomPalette() {
        return randomPalette;
    }

    public PaintLabelledObjects setRandomPalette(boolean randomPalette) {
        this.randomPalette = randomPalette;
        return this;
    }

    public long getRandSeed() {
        return randSeed;
    }

    public PaintLabelledObjects setRandSeed(long randSeed) {
        this.randSeed = randSeed;
        return this;
    }

    public int getIndexingBase() {
        return indexingBase;
    }

    public PaintLabelledObjects setIndexingBase(int indexingBase) {
        this.indexingBase = nonNegative(indexingBase);
        return this;
    }

    public boolean isProcessAlpha() {
        return processAlpha;
    }

    public PaintLabelledObjects setProcessAlpha(boolean processAlpha) {
        this.processAlpha = processAlpha;
        return this;
    }

    public int getDefaultNumberOfChannels() {
        return defaultNumberOfChannels;
    }

    public PaintLabelledObjects setDefaultNumberOfChannels(int defaultNumberOfChannels) {
        this.defaultNumberOfChannels = defaultNumberOfChannels;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D labelsMatrix) {
        final MultiMatrix2D background = getInputMat(INPUT_BACKGROUND, true).toMultiMatrix2D();
        SNumbers colors = getInputNumbers(INPUT_PALETTE, true);
        if (!colors.isInitialized()) {
            final SScalar jsonPalette = getInputScalar(INPUT_JSON_PALETTE, true);
            if (jsonPalette.isInitialized()) {
                final SScalar jsonNamedIndexes = getInputScalar(INPUT_JSON_NAMED_INDEXES, true);
                try (JsonToColorPalette executor = new JsonToColorPalette()
                        .setIndexingBase(indexingBase)
                        .setNumberOfChannels(defaultNumberOfChannels)) {
                    colors = executor.process(jsonPalette.getValue(), jsonNamedIndexes.getValue());
                }
            } else if (!randomPalette) {
                throw new IllegalArgumentException("Input \"" + INPUT_PALETTE + "\" or \"" + INPUT_JSON_PALETTE
                        + "\" must have initialized data, if random palette flag is not set");
            }
        }
        return process(labelsMatrix, background, colors);
    }

    public MultiMatrix2D process(MultiMatrix2D labelsMatrix, MultiMatrix2D background, SNumbers palette) {
        Objects.requireNonNull(labelsMatrix, "Null labels");
        if (!randomPalette) {
            Objects.requireNonNull(palette, "Null palette");
        } else {
            if (palette == null || !palette.isInitialized()) {
                palette = SNumbers.zeros(double.class, 0, defaultNumberOfChannels);
            }
        }
        labelsMatrix.checkDimensionEquality(background, "labels", "background");
        final int blockLength = palette.getBlockLength();
        assert blockLength >= 1;
        if (processAlpha && ALPHA_CHANNEL >= blockLength) {
            throw new IllegalArgumentException("Too low number of channels in the palette (" + blockLength
                    + ") to process alpha: if you sets \"process alpha-channel\" flag, "
                    + "palette must have at least 4 channels");
        }
        final int numberOfColors = palette.n();
        final int numberOfChannels = background != null ?
                background.numberOfChannels() :
                Math.min(blockLength, MultiMatrix2D.MAX_NUMBER_OF_CHANNELS);

        final int[] labels = labelsMatrix.channel(0).toInt();
        float[] paletteForMissing = null;
        if (randomPalette) {
            int maxLabel = 0;
            for (int v : labels) {
                if (v > maxLabel) {
                    maxLabel = v;
                }
            }
            maxLabel -= indexingBase;
            paletteForMissing = new float[Math.max(0, maxLabel + 1 - numberOfColors)];
        }
        final List<Matrix<? extends PArray>> coloredChannels = new ArrayList<>();
        final Random rnd = randSeed == 0 ? new Random() : new Random(randSeed);
        float[] alphaForMissing = null;
        if (processAlpha && randomPalette) {
            alphaForMissing = new float[paletteForMissing.length];
            for (int j = 0; j < paletteForMissing.length; j++) {
                alphaForMissing[j] = (float) (0.2 + rnd.nextDouble() * 0.8);
            }
        }
        for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
            final int colorChannelIndex = Math.min(channelIndex, blockLength - 1);
            final float[] values = background == null ?
                    new float[labels.length] :
                    background.asPrecision(float.class).channel(channelIndex).toFloat();
            if (randomPalette) {
                assert paletteForMissing != null;
                for (int j = 0; j < paletteForMissing.length; j++) {
                    paletteForMissing[j] = (float) rnd.nextDouble();
                }
            }
            if (processAlpha) {
                if (channelIndex != ALPHA_CHANNEL) {
                    for (int i = 0; i < values.length; i++) {
                        final int label = labels[i] - indexingBase;
                        double newValue;
                        double alpha;
                        if (label >= 0 && label < numberOfColors) {
                            newValue = palette.getValue(label, colorChannelIndex);
                            alpha = palette.getValue(label, ALPHA_CHANNEL);
                        } else if (randomPalette && label >= numberOfColors) {
                            assert paletteForMissing != null;
                            newValue = paletteForMissing[label - numberOfColors];
                            alpha = alphaForMissing[label - numberOfColors];
                        } else {
                            continue;
                        }
                        values[i] = (float) (alpha >= 1.0 ?
                                newValue :
                                alpha * newValue + (1.0 - alpha) * values[i]);
                    }
                } else {
                    for (int i = 0; i < values.length; i++) {
                        final int label = labels[i] - indexingBase;
                        if ((label >= 0 && label < numberOfColors) || (randomPalette && label >= numberOfColors)) {
                            values[i] = 1.0f;
                        }
                    }
                }
            } else {
                for (int i = 0; i < values.length; i++) {
                    final int label = labels[i] - indexingBase;
                    if (label >= 0 && label < numberOfColors) {
                        values[i] = (float) palette.getValue(label, colorChannelIndex);
                    } else if (randomPalette && label >= numberOfColors) {
                        assert paletteForMissing != null;
                        values[i] = paletteForMissing[label - numberOfColors];
                    }
                }
            }
            Matrix<? extends PArray> matrix = Matrices.matrix(
                    FloatArray.as(values),
                    labelsMatrix.dimensions());
            if (rawValues) {
                matrix = Matrices.asFuncMatrix(Func.IDENTITY, Arrays.type(PArray.class, elementType), matrix);
            }
            coloredChannels.add(matrix);
        }
        final MultiMatrix2D result = MultiMatrix.valueOf2D(coloredChannels);
        return rawValues ? result : result.asPrecision(elementType);
    }
}
