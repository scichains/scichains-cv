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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.JArrays;
import net.algart.arrays.Matrix;
import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

public final class ReindexLabels extends MultiMatrix2DFilter {
    public static final String INPUT_LABELS = "labels";
    public static final String OUTPUT_LABELS = "labels";
    public static final String OUTPUT_RESTORING_TABLE = "restoring_table";

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private boolean includeReservedInRestoringTable = false;

    public ReindexLabels() {
        setDefaultInputMat(INPUT_LABELS);
        setDefaultOutputMat(OUTPUT_LABELS);
        addOutputNumbers(OUTPUT_RESTORING_TABLE);
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public ReindexLabels setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public boolean isIncludeReservedInRestoringTable() {
        return includeReservedInRestoringTable;
    }

    public ReindexLabels setIncludeReservedInRestoringTable(boolean includeReservedInRestoringTable) {
        this.includeReservedInRestoringTable = includeReservedInRestoringTable;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D labels) {
        Objects.requireNonNull(labels, "Null labels");
        long t1 = debugTime();
        final int[] labelsArray = labels.channel(0).toInt();
        final int[] reindexTable = reindex(labelsArray, indexingBase.start, includeReservedInRestoringTable);
        long t2 = debugTime();
        logDebug(() -> String.format(Locale.US,
                "Labels %s reindexed: %.3f ms",
                labels, (t2 - t1) * 1e-6));
        getNumbers(OUTPUT_RESTORING_TABLE).setTo(reindexTable, 1);
        return MultiMatrix.of2DMono(Matrix.as(labelsArray, labels.dimensions()));
    }

    public static int[] reindex(int[] labels, int indexingBase, boolean includeReservedInRestoringTable) {
        Objects.requireNonNull(labels, "Null labels");
        if (indexingBase < 0) {
            throw new IllegalArgumentException("Negative indexing base " + indexingBase);
        }
        int maxLabel = Integer.MIN_VALUE;
        for (int label : labels) {
            maxLabel = Math.max(maxLabel, label);
        }
        if (maxLabel <= indexingBase) {
            final int[] result = new int[maxLabel + 1];
            JArrays.fillIntProgression(result, 0, 1);
            return result;
        }
        final int[] map = new int[maxLabel + 1 - indexingBase];
        Arrays.fill(map, -1);
        IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
            // note: splitting to blocks helps to provide normal speed
            for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                int label = labels[i] - indexingBase;
                if (label >= 0) {
                    map[label] = 0;
                }
            }
        });
        int count = indexingBase;
        for (int label = 0; label < map.length; label++) {
            if (map[label] != -1) {
                map[label] = count++;
            }
        }
        final int[] restoringTable = includeReservedInRestoringTable ?
                buildRestoringTableWithReserved(map, count, indexingBase) :
                buildRestoringTableWithoutReserved(map, count, indexingBase);
        IntStream.range(0, (labels.length + 255) >>> 8).parallel().forEach(block -> {
            // note: splitting to blocks helps to provide normal speed
            for (int i = block << 8, to = (int) Math.min((long) i + 256, labels.length); i < to; i++) {
                final int label = labels[i] - indexingBase;
                if (label >= 0) {
                    labels[i] = map[label];
                }
            }
        });
        return restoringTable;
    }

    private static int[] buildRestoringTableWithReserved(int[] map, int count, int indexingBase) {
        final int[] restoringTable = new int[count];
        for (int reserved = 0; reserved < indexingBase; reserved++) {
            restoringTable[reserved] = reserved;
        }
        for (int label = 0; label < map.length; label++) {
            final int newLabel = map[label];
            if (newLabel != -1) {
                restoringTable[newLabel] = indexingBase + label;
            }
        }
        return restoringTable;
    }

    private static int[] buildRestoringTableWithoutReserved(int[] map, int count, int indexingBase) {
        final int[] restoringTable = new int[count - indexingBase];
        for (int label = 0; label < map.length; label++) {
            final int newLabel = map[label];
            if (newLabel != -1) {
                restoringTable[newLabel - indexingBase] = indexingBase + label;
            }
        }
        return restoringTable;
    }
}
