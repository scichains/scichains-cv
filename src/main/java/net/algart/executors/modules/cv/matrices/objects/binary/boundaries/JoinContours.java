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

import net.algart.arrays.TooLargeArrayException;
import net.algart.contours.ContourJoiner;
import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;
import net.algart.math.IRectangularArea;

import java.util.stream.IntStream;

public final class JoinContours extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String INPUT_JOINING_MAP = "joining_map";
    public static final String OUTPUT_CONTOURS = "contours";
    public static final String OUTPUT_CONTAINING_ALL_RECTANGLE = "containing_all_rectangle";

    private ContourJoiner.JoiningOrder joiningOrder = ContourJoiner.JoiningOrder.UNORDERED;
    private Integer gridStepLog = null;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private Integer defaultJoinedLabel = null;
    private boolean automaticallyPackResultContours = true;
    private int measuringTimingLevel = 0;

    public JoinContours() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        addInputNumbers(INPUT_JOINING_MAP);
        setDefaultOutputNumbers(OUTPUT_CONTOURS);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY);
        addOutputNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public ContourJoiner.JoiningOrder getJoiningOrder() {
        return joiningOrder;
    }

    public JoinContours setJoiningOrder(ContourJoiner.JoiningOrder joiningOrder) {
        this.joiningOrder = joiningOrder;
        return this;
    }

    public Integer getGridStepLog() {
        return gridStepLog;
    }

    public JoinContours setGridStepLog(Integer gridStepLog) {
        this.gridStepLog = gridStepLog == null ? null : nonNegative(gridStepLog);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public JoinContours setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public Integer getDefaultJoinedLabel() {
        return defaultJoinedLabel;
    }

    public JoinContours setDefaultJoinedLabel(Integer defaultJoinedLabel) {
        this.defaultJoinedLabel = defaultJoinedLabel;
        return this;
    }

    public JoinContours setDefaultJoinedLabel(String defaultJoinedLabel) {
        return setDefaultJoinedLabel(intOrNull(defaultJoinedLabel));
    }

    public boolean isAutomaticallyPackResultContours() {
        return automaticallyPackResultContours;
    }

    public JoinContours setAutomaticallyPackResultContours(boolean automaticallyPackResultContours) {
        this.automaticallyPackResultContours = automaticallyPackResultContours;
        return this;
    }

    public int getMeasuringTimingLevel() {
        return measuringTimingLevel;
    }

    public JoinContours setMeasuringTimingLevel(int measuringTimingLevel) {
        this.measuringTimingLevel = nonNegative(measuringTimingLevel);
        return this;
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final Contours contours = Contours.deserialize(source.toIntArray());
        final int[] joinedLabelsMap = joiningMap();
        if (joinedLabelsMap == null && defaultJoinedLabel == null) {
            throw new IllegalArgumentException("The port \"" + INPUT_JOINING_MAP + "\" has no initialized data; "
                    + "in this case, you must specify some non-empty default joined label");
        }
        final ContourJoiner contourJoiner = defaultJoinedLabel == null ?
                ContourJoiner.newInstance(contours, gridStepLog, joinedLabelsMap) :
                ContourJoiner.newInstance(contours, gridStepLog, joinedLabelsMap, defaultJoinedLabel);
        contourJoiner.setJoiningOrder(joiningOrder);
        contourJoiner.setPackResultContours(automaticallyPackResultContours);
        contourJoiner.setInterrupter(this::isInterrupted);
        if (LOGGABLE_DEBUG) {
            contourJoiner.setMeasureTimingLevel(measuringTimingLevel);
        }
        final Contours result = contourJoiner.joinContours();
        logDebug(contourJoiner::timingInfo);
        getNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL).setTo(result.getAllObjectLabels(), 1);
        getNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY).setTo(
                MeasureContours.booleansToBytes(result.getAllInternalContour()), 1);
        if (isOutputNecessary(OUTPUT_CONTAINING_ALL_RECTANGLE)) {
            final IRectangularArea containingRectangle = contourJoiner.containingRectangle();
            if (containingRectangle != null) {
                getNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE).setTo(containingRectangle);
            }
        }
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(result.numberOfContours());
        return SNumbers.valueOf(result);
    }

    private int[] joiningMap() {
        final int[] map = getInputNumbers(INPUT_JOINING_MAP, true).toIntArray();
        if (map == null) {
            return null;
        }
        final int base = indexingBase.start;
        if (base == 0) {
            return map;
        }
        assert base > 0;
        if ((long) map.length + (long) base > Integer.MAX_VALUE) {
            throw new TooLargeArrayException("Too large joining map: > " + (Integer.MAX_VALUE - base));
        }
        int[] result = new int[map.length + base];
        for (int reserved = 0; reserved < base; reserved++) {
            result[reserved] = reserved;
        }
        IntStream.range(0, (map.length + 255) >>> 8).parallel().forEach(block -> {
            // note: splitting to blocks helps to provide normal speed
            for (int i = block << 8, to = (int) Math.min((long) i + 256, map.length); i < to; i++) {
                final int label = map[i];
                if (label < base) {
                    throw new IllegalArgumentException("Objects in contours have labels, less than "
                            + "indexing base: " + label + " < " + base);
                }
                result[base + i] = label;
            }
        });
        // - this "complex" loop just shifts the map by indexingBase.start and checks that
        // there are no too little labels (for more correct error message)
        return result;
    }
}
