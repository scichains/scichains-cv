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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public final class SubtractingPatternsTest {
    private static Pattern randomRectangularPatter(int dimCount, int size, Random rnd) {
        final IRange[] sides = new IRange[dimCount];
        for (int k = 0; k < dimCount; k++) {
            sides[k] = IRange.valueOf(rnd.nextInt(size) - size / 2, rnd.nextInt(size) + size / 2);
        }
        return Patterns.newRectangularIntegerPattern(sides);
    }

    private static Pattern simpleSubtract(Pattern pattern1, Pattern pattern2) {
        final Set<IPoint> points = new LinkedHashSet<>(pattern1.roundedPoints());
        // - we prefer stable order of points
        points.removeAll(pattern2.roundedPoints());
        return points.isEmpty() ? null : Patterns.newIntegerPattern(points);
    }

    public static void main(String[] args) {
        final Random rnd = new Random(135);
        for (int dimCount = 1; dimCount < 5; dimCount++) {
            System.out.printf("Testing %d dimensions...%n", dimCount);
            for (int test = 0, testCount = 65536 >> dimCount; test < testCount; test++) {
                if (test % 100 == 0) {
                    System.out.printf("%d / %d\r", test, testCount);
                }
                final Pattern pattern1 = randomRectangularPatter(dimCount, 8, rnd);
                final Pattern pattern2 = randomRectangularPatter(dimCount, 5, rnd);
                final Pattern simpleSubtract = simpleSubtract(pattern1, pattern2);
                if (simpleSubtract != null) {
                    final Pattern subtract = PatternSpecificationParser.subtract(pattern1, pattern2);
                    if (!subtract.points().equals(simpleSubtract.points())) {
                        throw new AssertionError("Unequal results for " + pattern1 + " and " + pattern2
                                + ":\n" + subtract + " instead of\n" + simpleSubtract);
                    }
                }
            }
        }
        System.out.printf("\rO'k                %n");
    }
}
