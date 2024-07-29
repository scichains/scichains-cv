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

package net.algart.executors.modules.cv.matrices.objects.markers;

import net.algart.arrays.Arrays;
import net.algart.arrays.BitArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.UpdatablePArray;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.matrices.arithmetic.CheckMatrixEquality;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class LabelPaintedMarkers extends MultiMatrix2DFilter {
    public static final String INPUT_PALETTE = "palette";
    public static final String INPUT_IMAGE_WITH_MARKERS = "image_with_markers";
    public static final String INPUT_IMAGE_WITHOUT_MARKERS = "image_without_markers";
    public static final String OUTPUT_PALETTE = INPUT_PALETTE;
    public static final String OUTPUT_LABELS = "labels";

    public LabelPaintedMarkers() {
        useVisibleResultParameter();
        addInputNumbers(INPUT_PALETTE);
        setDefaultInputMat(INPUT_IMAGE_WITH_MARKERS);
        addInputMat(INPUT_IMAGE_WITHOUT_MARKERS);
        setDefaultOutputMat(OUTPUT_LABELS);
        addOutputNumbers(OUTPUT_PALETTE);
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final SNumbers colorMap = getInputNumbers(INPUT_PALETTE, true);
        final SMat imageWithoutMarkers = getInputMat(INPUT_IMAGE_WITHOUT_MARKERS, true);
        final MultiMatrix2D result = process(source, imageWithoutMarkers.toMultiMatrix2D(), colorMap);
        getNumbers(OUTPUT_PALETTE).setTo(colorMap);
        return result;
    }

    // colorMap is in-out argument; may be uninitialized on input.
    public MultiMatrix2D process(
            MultiMatrix2D imageWithMarkers,
            MultiMatrix2D imageWithoutMarkers,
            SNumbers colorMap) {
        Objects.requireNonNull(imageWithMarkers, "Null imageWithMarkers");
        Objects.requireNonNull(colorMap, "Null colorMap");
        imageWithMarkers.checkDimensionEquality(imageWithoutMarkers,
                "image with markers", "image without markers");
        final int numberOfChannels = imageWithMarkers.numberOfChannels();
        if (colorMap.isInitialized() && colorMap.getBlockLength() < numberOfChannels) {
            throw new IllegalArgumentException("Too little blocks in colorMap: at least "
                    + numberOfChannels + " columns required");
        }
        if (imageWithoutMarkers != null) {
            try (CheckMatrixEquality checkMatrixEquality = new CheckMatrixEquality()) {
                imageWithMarkers = checkMatrixEquality.setOperation(CheckMatrixEquality.Operation.E0_NX)
                        .process(imageWithMarkers, imageWithoutMarkers).asMultiMatrix2D();
            }
        }
        assert imageWithMarkers.numberOfChannels() == numberOfChannels;
        // Note: imageWithMarkers must be 1st argument of CheckEquality to preserve number of channels!

        final BitArray nonZerosArray = imageWithMarkers.nonZeroRGBMatrix().array().updatableClone(Arrays.SMM);
        imageWithMarkers = imageWithMarkers.asPrecision(double.class);
        final Map<MultiMatrix.PixelValue, Integer> map = new HashMap<>();
        int markerCount = 0;
        if (colorMap.isInitialized()) {
            double[] channels = null;
            for (int k = 0, n = colorMap.n(); k < n; k++) {
                channels = colorMap.getBlockDoubleValues(k, channels);
                final MultiMatrix.PixelValue pixel = MultiMatrix.PixelValue.valueOf(channels);
                if (map.putIfAbsent(pixel, markerCount + 1) == null) {
                    markerCount++;
                }
            }
        }
        final UpdatablePArray result = Arrays.SMM.newUnresizableIntArray(nonZerosArray.length());
        for (long index = 0, n = nonZerosArray.length(); index < n; index++) {
            if (nonZerosArray.getBit(index)) {
                final MultiMatrix.PixelValue pixel = imageWithMarkers.getPixel(index);
                assert pixel.numberOfChannels() == imageWithMarkers.numberOfChannels();
                Integer currentMarkerIndex = map.putIfAbsent(pixel, markerCount + 1);
                if (currentMarkerIndex != null) {
                    result.setInt(index, currentMarkerIndex);
                } else {
                    result.setInt(index, markerCount + 1);
                    markerCount++;
                }
            }
        }
        assert markerCount == map.size();
        colorMap.setToZeros(double.class, markerCount, numberOfChannels);
        for (Map.Entry<MultiMatrix.PixelValue, Integer> entry : map.entrySet()) {
            final int markerIndex = entry.getValue();
            assert markerIndex - 1 < markerCount;
            colorMap.setBlockValues(markerIndex - 1, entry.getKey().getDoubleChannels());
        }
        return MultiMatrix.valueOf2DMono(Matrices.matrix(result, imageWithMarkers.dimensions()));
    }
}
