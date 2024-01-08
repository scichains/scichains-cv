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

package net.algart.executors.modules.cv.matrices.objects.labels;

import net.algart.arrays.SimpleMemoryModel;

import java.util.Arrays;
import java.util.Objects;

public final class LabelsListsBuilder extends LabelsProcessor {
    private final int[] labels;
    private final int[] lists;
    private final int[][] threadListHeads;
    private final int[][] threadListTailsIncreased;
    private final int[][] requestedListHeads;
    private final int[][] requestedListTails;
    private int[] listHeads;
    private int maxLabel = -1;

    private LabelsListsBuilder(int[] labels, int[] lists) {
        super(SimpleMemoryModel.asUpdatableIntArray(Objects.requireNonNull(labels, "Null labels")));
        assert labels.length == lists.length;
        this.labels = labels;
        this.lists = lists;
        this.requestedListHeads = requestClearedIntArrays(numberOfTasks());
        this.requestedListTails = requestClearedIntArrays(numberOfTasks());
        this.threadListHeads = requestedListHeads.clone();
        this.threadListTailsIncreased = requestedListTails.clone();
    }

    public static LabelsListsBuilder getInstance(int[] labels, int[] lists) {
        return new LabelsListsBuilder(labels, lists);
    }

    @Override
    public void close() {
        releaseAndClearIntArrays(requestedListTails, maxLabel + 1);
        releaseAndClearIntArrays(requestedListHeads, maxLabel + 1);
    }

    public int[] listHeads() {
        return listHeads;
    }

    public int maxLabel() {
        return maxLabel;
    }

    @Override
    protected void processSubArr(int p, int count, int threadIndex) {
        int[] listHeads = this.threadListHeads[threadIndex];
        int[] listTailsIncreased = this.threadListTailsIncreased[threadIndex];
        for (int k = p, kMax = k + count; k < kMax; k++) {
            int label = labels[k];
            if (label > 0) {
                if (label >= listHeads.length) {
                    listHeads = ensureCapacityForLabel(listHeads, label);
                    listTailsIncreased = ensureCapacityForLabel(listTailsIncreased, label);
                }
                lists[k] = listHeads[label];
                listHeads[label] = k;
                // - note: really the "head" will be at the end of the object (its part for the given thread)
                if (listTailsIncreased[label] == 0) {
                    // Not too often situation
                    listTailsIncreased[label] = k + 1;
                    // - listTailsIncreased must contain POSITIVE values to avoid extra time and
                    // efforts for initializing newly allocated arrays by -1 or something like this
                    lists[k] = -1;
                    // - marks this element as the tail (zero head cannot be a marker:
                    // element is #0 is a correct element)
                }
            }
        }
        this.threadListHeads[threadIndex] = listHeads;
        this.threadListTailsIncreased[threadIndex] = listTailsIncreased;
    }

    @Override
    protected void finish() {
        int maxLabel = 0;
        for (int[] threadTails : this.threadListTailsIncreased) {
            int last = 0;
            for (int k = threadTails.length - 1; k >= 0; k--) {
                if (threadTails[k] > 0) {
                    last = k;
                    break;
                }
            }
            maxLabel = Math.max(maxLabel, last);
        }
        this.maxLabel = maxLabel;
        this.listHeads = new int[maxLabel + 1];
        // Note: in the resulting "this.cardinalities" we still skip zero element:
        // actual list head for label is in this.listHeads[label]
        assert threadListHeads.length == threadListTailsIncreased.length;
        for (int index = 0; index < threadListHeads.length; index++) {
            if (threadListHeads[index].length != threadListTailsIncreased[index].length)
                throw new AssertionError("Non-synchronous lengths of heads/tails arrays");
        }
        int[] nonEmptyHeads = new int[threadListHeads.length];
        int[] nonEmptyTails = new int[threadListTailsIncreased.length];
        for (int label = 0; label <= maxLabel; label++) {
            int nonEmptyCount = 0;
            for (int index = 0; index < threadListHeads.length; index++) {
                int[] tailsIncreased = threadListTailsIncreased[index];
                if (label < tailsIncreased.length && tailsIncreased[label] > 0) {
                    // - if label >= tailsIncreased.length, it means that this label did not occur
                    // in this thread and "virtual" tailsIncreased[label] is supposed to be 0
                    nonEmptyHeads[nonEmptyCount] = threadListHeads[index][label];
                    nonEmptyTails[nonEmptyCount] = tailsIncreased[label] - 1;
                    nonEmptyCount++;
                }
            }
            for (int k = 1; k < nonEmptyCount; k++) {
                final int previousTail = nonEmptyTails[k - 1];
                if (previousTail < 0 || previousTail >= lists.length) {
                    throw new AssertionError("List tail #" + (k - 1)
                            + " from " + nonEmptyCount + " (maximally from " + threadListHeads.length
                            + ") = " + previousTail + " is out of range 0.." + (lists.length - 1)
                            + "; label " + label + "/" + maxLabel
                            + "; non-empty heads: "
                            + Arrays.toString(Arrays.copyOfRange(nonEmptyHeads, 0, nonEmptyCount))
                            + "; non-empty tails: "
                            + Arrays.toString(Arrays.copyOfRange(nonEmptyTails, 0, nonEmptyCount)));
                }
                assert lists[previousTail] == -1;
                lists[previousTail] = nonEmptyHeads[k];
            }
            listHeads[label] = nonEmptyCount > 0 ? nonEmptyHeads[0] : -1;
//            if (nonEmptyCount > 1) { // - debugging
//                System.out.printf("%d/%d: %d non-empty from %d lists%n",
//                        label, maxLabel, nonEmptyCount, threadListHeads.length);
//            }
        }
    }
}
