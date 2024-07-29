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

package net.algart.executors.modules.cv.matrices.pixels;

import net.algart.arrays.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SetPixels extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_PIXEL_VALUES = "pixel_values";
    public static final String INPUT_MASK = "mask";
    public static final String INPUT_BACKGROUND = "background";
    // - Background is added mostly for demo needs

    private Class<?> elementType = float.class;
    private boolean rawPixelValues = false;
    private boolean supposeMaskAlwaysTrue = false;

    public SetPixels() {
        setDefaultInputNumbers(INPUT_PIXEL_VALUES);
        addInputMat(INPUT_MASK);
        addInputMat(INPUT_BACKGROUND);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public final Class<?> getElementType() {
        return elementType;
    }

    public SetPixels setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType, "element type");
        return this;
    }

    public SetPixels setElementType(String elementType) {
        setElementType(MultiMatrixGenerator.elementType(elementType));
        return this;
    }

    public boolean isRawPixelValues() {
        return rawPixelValues;
    }

    public SetPixels setRawPixelValues(boolean rawPixelValues) {
        this.rawPixelValues = rawPixelValues;
        return this;
    }

    public boolean isSupposeMaskAlwaysTrue() {
        return supposeMaskAlwaysTrue;
    }

    public SetPixels setSupposeMaskAlwaysTrue(boolean supposeMaskAlwaysTrue) {
        this.supposeMaskAlwaysTrue = supposeMaskAlwaysTrue;
        return this;
    }

    @Override
    public void process() {
        final SNumbers source = getInputNumbers();
        final MultiMatrix2D mask = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        final MultiMatrix2D background = getInputMat(INPUT_BACKGROUND, true).toMultiMatrix2D();
        setStartProcessingTimeStamp();
        final MultiMatrix2D result = process(source, mask, background);
        setEndProcessingTimeStamp();
        getMat().setTo(result);
    }

    public MultiMatrix2D process(SNumbers pixels, long dimX, long dimY, MultiMatrix2D background) {
        final Matrix<BitArray> mask = Matrices.matrix(Arrays.nBitCopies(dimX * dimY, true), dimX, dimY);
        return process(pixels, MultiMatrix.valueOf2DMono(mask), background);
    }

    public MultiMatrix2D process(SNumbers pixels, MultiMatrix2D mask, MultiMatrix2D background) {
        Objects.requireNonNull(pixels, "Null pixels");
        if (mask == null && background == null) {
            throw new IllegalArgumentException("At least one of images \"mask\" or \"background\" must be initialized");
        }
        final long[] dimensions = mask != null ? mask.dimensions() : background.dimensions();
        if (mask == null) {
            mask = MultiMatrix.valueOf2DMono(
                    Matrices.constantMatrix(1.0, BitArray.class, dimensions));
        }
        if (background != null && !mask.dimEquals(background)) {
            throw new IllegalArgumentException("The mask and background multi-matrix dimensions mismatch: "
                    + mask + " and " + background);
        }
        if (mask.size() != (int) mask.size()) {
            throw new TooLargeArrayException("Too large matrix: " + mask);
        }
        final int numberOfChannels = pixels.getBlockLength();
        final BitArray maskArray = supposeMaskAlwaysTrue ?
                Arrays.nBitCopies(mask.size(), true) :
                mask.nonZeroAnyChannelMatrix().array();
        if (background != null) {
            background = background.asPrecision(float.class);
            if (numberOfChannels == 1) {
                background = background.asMono();
            }
        }
        final float[] data = pixels.toFloatArray();
        final List<Matrix<? extends PArray>> result = new ArrayList<>();
        for (int channelIndex = 0; channelIndex < numberOfChannels; channelIndex++) {
            final Matrix<? extends UpdatablePArray> channel = Arrays.SMM.newFloatMatrix(dimensions);
            final UpdatablePArray channelArray = channel.array();
            if (background != null && channelIndex < background.numberOfChannels()) {
                channelArray.copy(background.channel(channelIndex).array());
            }
            int disp = channelIndex;
            if (rawPixelValues) {
                final double mult = 1.0 / Arrays.maxPossibleValue(
                        Arrays.type(PArray.class, elementType), 1.0);
                for (long j = 0, n = channelArray.length(); j < n && disp < data.length; j++) {
                    if (maskArray.getBit(j)) {
                        channelArray.setDouble(j, data[disp] * mult);
                        disp += numberOfChannels;
                    }
                }
            } else {
                for (long j = 0, n = channelArray.length(); j < n && disp < data.length; j++) {
                    if (maskArray.getBit(j)) {
                        channelArray.setDouble(j, data[disp]);
                        disp += numberOfChannels;
                    }
                }
            }
            result.add(Matrices.clone(Matrices.asPrecision(channel, elementType)));
            // - cloneMatrix allows to free memory, allocated by channel matrix
        }
        return MultiMatrix.valueOf2D(result);
    }
}
