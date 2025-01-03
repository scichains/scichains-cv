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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannelFilter;
import net.algart.executors.modules.core.matrices.geometry.ContinuationMode;
import net.algart.math.IRange;
import net.algart.math.Point;
import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.ContinuedMorphology;
import net.algart.matrices.morphology.Morphology;

import java.util.Arrays;
import java.util.Objects;

public abstract class MorphologyFilter extends MultiMatrixChannelFilter {
    public enum Shape {
        SPHERE("CIRCLE") {
            @Override
            public Pattern newPattern(int numberOfDimensions, int patternSize) {
                return Patterns.newSphereIntegerPattern(
                        Point.origin(numberOfDimensions),
                        Math.max(0.0, 0.5 * (patternSize + 1) - 0.2));
            }
        },
        CUBE("SQUARE") {
            @Override
            public Pattern newPattern(int numberOfDimensions, int patternSize) {
                final IRange[] sides = new IRange[numberOfDimensions];
                Arrays.fill(sides, IRange.valueOf(-patternSize / 2, -patternSize / 2 + patternSize - 1));
                return Patterns.newRectangularIntegerPattern(sides);
            }
        };

        private final String[] alternameNames;
        // - for compatibility for old solutions

        Shape(String... alternameNames) {
            this.alternameNames = alternameNames;
        }

        public abstract Pattern newPattern(int numberOfDimensions, int patternSize);

        public static Shape valueOfName(String name) {
            for (Shape shape : values()) {
                if (shape.name().equals(name)) {
                    return shape;
                }
                for (String alternateName : shape.alternameNames) {
                    if (alternateName.equals(name)) {
                        return shape;
                    }
                }
            }
            throw new IllegalArgumentException("Unknown shape name " + name);
        }
    }

    private static final int OPTIMIZED_CIRCLE_SIZE_STEP = 16;
    private static final double OPTIMIZED_CIRCLE_SIZE_BASE = 16.4; // little greater for better boundary

    private Shape shape = Shape.SPHERE;
    private int patternSize = 3;
    private Pattern customPattern = null;
    private String customPatternSpecification = null;
    Matrix.ContinuationMode continuationMode = null;

    public final Pattern getPattern(Matrix<? extends PArray> m) {
        return getPattern(m, false);
    }

    public final Pattern getPattern(Matrix<? extends PArray> m, boolean minkowskiOptimizeCircle) {
        final int dimCount = m.dimCount();
        if (customPattern != null) {
            return customPattern;
        }
        final long t1 = System.nanoTime();
        try {
            if (customPatternSpecification != null && !customPatternSpecification.isEmpty()) {
                final PatternSpecificationParser parser = PatternSpecificationParser.getInstance(m.elementType());
                return parser.parse(customPatternSpecification);
            } else {
                if (minkowskiOptimizeCircle && shape == Shape.SPHERE && patternSize > OPTIMIZED_CIRCLE_SIZE_STEP) {
                    final Point origin = Point.origin(dimCount);
                    final Pattern baseSphere = Patterns.newSphereIntegerPattern(
                            origin, Math.max(0.0, 0.5 * OPTIMIZED_CIRCLE_SIZE_BASE));
                    final Pattern baseSpherePlus1 = Patterns.newSphereIntegerPattern(
                            origin, Math.max(0.0, 0.5 * (OPTIMIZED_CIRCLE_SIZE_BASE + 1.0)));
                    final int multiplier = patternSize / OPTIMIZED_CIRCLE_SIZE_STEP;
                    final int remainder = patternSize % OPTIMIZED_CIRCLE_SIZE_STEP;
                    if (remainder == 0) {
                        return Patterns.newMinkowskiMultiplePattern(baseSphere, multiplier);
                    } else if (remainder == 1) {
                        // Accurate: not increasing projection of the circle,
                        // but adding some points inside larger radius
                        Pattern result = baseSpherePlus1;
                        if (multiplier > 1) {
                            result = Patterns.newMinkowskiSum(
                                    Patterns.newMinkowskiMultiplePattern(baseSphere, multiplier - 1),
                                    result);
                        }
                        return result;
                    } else {
                        Pattern additionalSphere = Patterns.newSphereIntegerPattern(origin, 0.5 * remainder);
                        return Patterns.newMinkowskiSum(
                                Patterns.newMinkowskiMultiplePattern(baseSphere, multiplier),
                                additionalSphere);
                    }
                }
                return shape.newPattern(dimCount, patternSize);
            }
        } finally {
            final long t2 = System.nanoTime();
            addServiceTime(t2 - t1);
        }
    }

    public Shape getShape() {
        return shape;
    }

    public int getPatternSize() {
        return patternSize;
    }

    public final MorphologyFilter setPattern(Shape shape, int patternSize) {
        this.shape = Objects.requireNonNull(shape, "Null shape");
        this.patternSize = nonNegative(patternSize, "pattern size");
        return this;
    }

    public final MorphologyFilter setCustomPattern(Pattern pattern) {
        this.customPattern = pattern;
        return this;
    }

    public String getCustomPatternSpecification() {
        return customPatternSpecification;
    }

    public MorphologyFilter setCustomPatternSpecification(String customPatternSpecification) {
        this.customPatternSpecification = customPatternSpecification;
        return this;
    }

    public Matrix.ContinuationMode getContinuationMode() {
        return continuationMode;
    }

    public MorphologyFilter setContinuationMode(Matrix.ContinuationMode continuationMode) {
        this.continuationMode = continuationMode;
        return this;
    }

    public MorphologyFilter setContinuationMode(ContinuationMode continuationMode) {
        Objects.requireNonNull(continuationMode, "Null continuation mode");
        this.continuationMode = continuationMode.continuationModeOrNull();
        return this;
    }

    @SuppressWarnings("resource")
    @Override
    public void onChangeParameter(String name) {
        switch (name) {
            case "shape" -> {
                setPattern(Shape.valueOfName(parameters().getString(name).toUpperCase()), patternSize);
            }
            case "patternSize" -> {
                setPattern(shape, parameters().getInteger(name));
            }
            default -> super.onChangeParameter(name);
        }
    }

    public Morphology createMorphology(Matrix<? extends PArray> m) {
        Morphology morphology = BasicMorphology.getInstance(null);
        if (continuationMode != null) {
            morphology = ContinuedMorphology.getInstance(morphology, continuationMode);
        }
        return morphology;
    }
}
