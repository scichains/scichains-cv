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

package net.algart.executors.modules.cv.matrices.morphology;

public final class PatternsTest {
    public static void main(String[] args) {
        System.out.printf("Cross 1D: %s%n", MorphologyOperation.crossPattern(1));
        System.out.printf("Cross 2D: %s%n", MorphologyOperation.crossPattern(2));
        System.out.printf("Cross 3D: %s%n", MorphologyOperation.crossPattern(3));
        System.out.printf("Cross 4D: %s%n", MorphologyOperation.crossPattern(4));
        System.out.printf("Cube 2D 3x3: %s%n",
                MorphologyFilter.Shape.CUBE.newPattern(2, 3));
        System.out.printf("Sphere 2D 3x3: %s%n",
                MorphologyFilter.Shape.SPHERE.newPattern(2, 3));
        System.out.printf("Sphere 3D 3x3: %s%n",
                MorphologyFilter.Shape.SPHERE.newPattern(3, 3));
    }
}
