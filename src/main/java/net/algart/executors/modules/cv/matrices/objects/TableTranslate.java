/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.Matrix;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.numbers.misc.InvertTable;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Objects;
import java.util.stream.IntStream;

public final class TableTranslate extends MultiMatrix2DFilter {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_TABLE = "table";
    public static final String OUTPUT_LABELS = "labels";

    public enum ResultElementType {
        INT(int.class),
        FLOAT(float.class);

        final Class<?> elementType;

        ResultElementType(Class<?> elementType) {
            this.elementType = elementType;
        }
    }

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private ResultElementType resultElementType = ResultElementType.INT;
    private Double replacementForNotExisting = null;
    private boolean invertTable = false;

    public TableTranslate() {
        setDefaultInputMat(INPUT_LABELS);
        addInputNumbers(INPUT_TABLE);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public TableTranslate setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public ResultElementType getResultElementType() {
        return resultElementType;
    }

    public TableTranslate setResultElementType(ResultElementType resultElementType) {
        this.resultElementType = resultElementType;
        return this;
    }

    public Double replacementForNotExisting() {
        return replacementForNotExisting;
    }

    public TableTranslate setReplacementForNotExisting(Double replacementForNotExisting) {
        this.replacementForNotExisting = replacementForNotExisting;
        return this;
    }

    public boolean isInvertTable() {
        return invertTable;
    }

    public TableTranslate setInvertTable(boolean invertTable) {
        this.invertTable = invertTable;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D labelsMatrix) {
        final SNumbers table = getInputNumbers(INPUT_TABLE);
        return switch (resultElementType) {
            case INT -> process(labelsMatrix, table.toIntArray());
            case FLOAT -> process(labelsMatrix, table.toFloatArray());
        };
    }

    public MultiMatrix2D process(MultiMatrix2D labelsMatrix, int[] translationTable) {
        Objects.requireNonNull(labelsMatrix, "Null labels");
        Objects.requireNonNull(translationTable, "Null translation table");
        final int[] labels = labelsMatrix.channel(0).toInt();
        final int[] table = invertTable ?
                InvertTable.invert(translationTable, indexingBase.start) :
                translationTable;
        if (replacementForNotExisting == null) {
            IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
                // note: splitting to blocks helps to provide normal speed
                for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                    final int label = labels[i] - indexingBase.start;
                    if (label >= 0 && label < table.length) {
                        labels[i] = table[label];
                    }
                }
            });
        } else {
            final int replacement = (int) replacementForNotExisting.doubleValue();
            IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
                // note: splitting to blocks helps to provide normal speed
                for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                    final int label = labels[i] - indexingBase.start;
                    labels[i] = label >= 0 && label < table.length ?
                            table[label] :
                            replacement;
                }
            });
        }
        return MultiMatrix.of2DMono(Matrix.as(labels, labelsMatrix.dimensions()));
    }

    public MultiMatrix2D process(MultiMatrix2D labelsMatrix, float[] translationTable) {
        Objects.requireNonNull(labelsMatrix, "Null labels");
        Objects.requireNonNull(translationTable, "Null translation table");
        if (invertTable) {
            throw new IllegalArgumentException("\"Invert table\" mode requires \"int\" result elements, "
                    + "but \"float\" result type is specified");
        }
        final int[] labels = labelsMatrix.channel(0).jaInt();
        // - note: we don't modify the labels below, so we can use jaInt()
        final float[] result = new float[labels.length];
        if (replacementForNotExisting == null) {
            IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
                // note: splitting to blocks helps to provide normal speed
                for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                    final int originalLabel = labels[i];
                    final int label = originalLabel - indexingBase.start;
                    result[i] = label >= 0 && label < translationTable.length ?
                            translationTable[label] :
                            originalLabel;
                }
            });
        } else {
            final float replacement = replacementForNotExisting.floatValue();
            IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
                // note: splitting to blocks helps to provide normal speed
                for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                    final int label = labels[i] - indexingBase.start;
                    result[i] = label >= 0 && label < translationTable.length ?
                            translationTable[label] :
                            replacement;
                }
            });
        }
        return MultiMatrix.of2DMono(Matrix.as(result, labelsMatrix.dimensions()));
    }
}
