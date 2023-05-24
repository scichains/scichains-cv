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

package net.algart.additions.matrices;

public class QuickAveragerTwoStagePrecisionOfDivisionTest {
    private final static int MAX_SIZE = 10000;
    public static void main(String[] args) {
        long maxValue = 255 * MAX_SIZE;
        // - maximal sum, that should be divided by QuickAverager in two-stage mode for byte elements
        for (int size = 1; size < MAX_SIZE; size++) {
            if ((size & (size - 1)) != 0) {
                // without this check, fails at 49/49
                continue;
            }
            System.out.print("\rTesting size " + size + "...");
            double multiplier = 1.0 / size;
            for (long sum = 0; sum < maxValue; sum++) {
                int strictDivided = (int) (sum / size);
                int multiplied = (int) (sum * multiplier);
                if (strictDivided != multiplied) {
                    throw new AssertionError("Difference for " + sum + "/" + size
                        + ": " + multiplied + " (" + sum * multiplier + ") != " + strictDivided);
                }
            }
        }
    }
}
