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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.contours.Contours;
import net.algart.contours.ContourHeader;
import net.algart.arrays.Arrays;
import net.algart.arrays.IntArray;
import net.algart.arrays.MutableIntArray;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;

import java.util.List;
import java.util.Locale;

public final class ChangeContourHeaders extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_OBJECT_LABEL = ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL;
    public static final String INPUT_INTERNAL_BOUNDARY = ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY;
    public static final String OUTPUT_CONTOURS = "contours";

    private boolean removeFrameId = false;
    private boolean reverseContours = false;

    public ChangeContourHeaders() {
        super(INPUT_CONTOURS, INPUT_OBJECT_LABEL, INPUT_INTERNAL_BOUNDARY);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
    }

    public boolean isRemoveFrameId() {
        return removeFrameId;
    }

    public ChangeContourHeaders setRemoveFrameId(boolean removeFrameId) {
        this.removeFrameId = removeFrameId;
        return this;
    }

    public boolean isReverseContours() {
        return reverseContours;
    }

    public ChangeContourHeaders setReverseContours(boolean reverseContours) {
        this.reverseContours = reverseContours;
        return this;
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        final Runtime rt = Runtime.getRuntime();
        long m1 = rt.totalMemory() - rt.freeMemory();
        long t1 = debugTime();
        final Contours contours = Contours.deserialize(sources.get(0).toIntArray());
        final int[] labels = getInputNumbers(INPUT_OBJECT_LABEL, true).toIntArray();
        final byte[] internal = getInputNumbers(INPUT_INTERNAL_BOUNDARY, true).toByteArray();
        long m2 = rt.totalMemory() - rt.freeMemory();
        long t2 = debugTime();
        final Contours resultContours = Contours.newInstance();
        final ContourHeader header = new ContourHeader();
        final MutableIntArray workArray = Arrays.SMM.newEmptyIntArray();
        for (int k = 0, n = contours.numberOfContours(); k < n; k++) {
            contours.getHeader(header, k);
            IntArray contour = contours.getContour(k);
            if (labels != null && k < labels.length) {
                header.setObjectLabel(labels[k]);
            }
            boolean newInternal = header.isInternalContour();
            if (internal != null) {
                if (k < internal.length) {
                    newInternal = internal[k] != 0;
                }
            } else {
                if (reverseContours) {
                    newInternal = !newInternal;
                }
            }
            if (header.isInternalContour() != newInternal) {
                header.setInternalContour(newInternal);
                contour = Contours.reverseContour(workArray, contour);
            }
            if (removeFrameId) {
                header.removeFrameId();
            }
            resultContours.addContour(header, contour);
        }
        long m3 = rt.totalMemory() - rt.freeMemory();
        long t3 = debugTime();
        final SNumbers result = SNumbers.valueOf(resultContours);
        long m4 = rt.totalMemory() - rt.freeMemory();
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Headers of %d contours changed in %.3f ms = "
                        + "%.3f reading (%.1f + %.3f MB) + %.3f changing (+ %.3f MB) + %.3f serializing (+ %.3f MB)",
                contours.numberOfContours(), (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6, m1 / 1048576.0, (m2 - m1) / 1048576.0,
                (t3 - t2) * 1e-6, (m3 - m2) / 1048576.0,
                (t4 - t3) * 1e-6, (m4 - m3) / 1048576.0));
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex != 0;
    }

    @Override
    protected boolean numberOfBlocksEqualityRequired() {
        return false;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

}
