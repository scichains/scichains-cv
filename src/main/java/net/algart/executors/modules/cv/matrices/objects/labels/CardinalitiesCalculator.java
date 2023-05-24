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

package net.algart.executors.modules.cv.matrices.objects.labels;

import net.algart.arrays.JArrays;
import net.algart.arrays.SimpleMemoryModel;

import java.util.Objects;

class CardinalitiesCalculator extends LabelsProcessor {
    final int[] labels;
    final int[][] threadCardinalities;
    private final int[][] requestedCardinalities;
    int[] cardinalities;
    int maxLabel = -1;

    CardinalitiesCalculator(int[] labels) {
        super(SimpleMemoryModel.asUpdatableIntArray(Objects.requireNonNull(labels, "Null labels")));
        this.labels = labels;
        this.requestedCardinalities = requestClearedIntArrays(numberOfTasks());
        this.threadCardinalities = requestedCardinalities.clone();
    }

    @Override
    public void close() {
        releaseAndClearIntArrays(requestedCardinalities, maxLabel + 1);
    }

    @Override
    protected void processSubArr(int p, int count, int threadIndex) {
        int[] cardinalities = this.threadCardinalities[threadIndex];
        for (int k = p, kMax = k + count; k < kMax; k++) {
            int label = labels[k];
            if (label > 0) {
                if (label >= cardinalities.length) {
                    cardinalities = ensureCapacityForLabel(cardinalities, label);
                }
                cardinalities[label]++;
                // Note: for better performance, skip cardinalities[0]=0
            }
        }
        this.threadCardinalities[threadIndex] = cardinalities;
    }

    @Override
    protected void finish() {
        int maxLabel = 0;
        for (int[] threadCardinalities : this.threadCardinalities) {
            int last = 0;
            for (int k = threadCardinalities.length - 1; k >= 0; k--) {
                if (threadCardinalities[k] > 0) {
                    last = k;
                    break;
                }
            }
            maxLabel = Math.max(maxLabel, last);
        }
        this.cardinalities = new int[maxLabel];
        this.maxLabel = maxLabel;
        for (int[] cardinalities : this.threadCardinalities) {
            final int length = Math.min(this.cardinalities.length, cardinalities.length - 1);
            JArrays.addIntArray(this.cardinalities, 0, cardinalities, 1, length);
            // Note: in the resulting "this.cardinalities" we use zero element:
            // actual information for label is in this.cardinalities[label-1]
        }
    }
}
