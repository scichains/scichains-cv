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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.*;
import net.algart.executors.modules.core.common.matrices.MultiMatrixChannel2DFilter;
import net.algart.executors.modules.core.matrices.geometry.ContinuationMode;
import net.algart.math.functions.LinearFunc;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.*;
import net.algart.multimatrix.MultiMatrix2D;

public final class MorphologyTransform extends MultiMatrixChannel2DFilter {
    public enum TransformOperation {
        EROSION() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatablePArray>> getProcessor(
                    Morphology morphology,
                    Class<? extends UpdatablePArray> requiredType,
                    Matrix<? extends PArray> matrix, Pattern[] patterns) {
                return IterativeErosion.getInstance(morphology, requiredType, matrix, patterns);
            }
        },
        OPENING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatablePArray>> getProcessor(
                    Morphology morphology,
                    Class<? extends UpdatablePArray> requiredType,
                    Matrix<? extends PArray> matrix, Pattern[] patterns) {
                return IterativeOpening.getInstance(morphology, requiredType, matrix, patterns);
            }
        };

        abstract IterativeArrayProcessor<Matrix<? extends UpdatablePArray>> getProcessor(
                Morphology morphology,
                Class<? extends UpdatablePArray> requiredType,
                Matrix<? extends PArray> matrix, Pattern[] patterns);
    }

    public enum ResultElementType {
        BYTE(byte.class),
        SHORT(short.class),
        INT(int.class),
        FLOAT(float.class);

        final Class<?> elementType;

        ResultElementType(Class<?> elementType) {
            this.elementType = elementType;
        }
    }

    private TransformOperation transformOperation = TransformOperation.EROSION;
    private ResultElementType resultElementType = ResultElementType.FLOAT;
    private Long maxNumberOfIterations = null;
    private String patternsSpecification = "cross | square 3";
    private Pattern[] patterns = parsePatterns(patternsSpecification);
    private ContinuationMode continuationMode = ContinuationMode.NEGATIVE_INFINITY;
    private boolean convertToMono = true;

    public TransformOperation getTransformOperation() {
        return transformOperation;
    }

    public MorphologyTransform setTransformOperation(TransformOperation transformOperation) {
        this.transformOperation = transformOperation;
        return this;
    }

    public ResultElementType getResultElementType() {
        return resultElementType;
    }

    public MorphologyTransform setResultElementType(ResultElementType resultElementType) {
        this.resultElementType = resultElementType;
        return this;
    }

    public Long getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public MorphologyTransform setMaxNumberOfIterations(Long maxNumberOfIterations) {
        if (maxNumberOfIterations != null) {
            nonNegative(maxNumberOfIterations);
        }
        this.maxNumberOfIterations = maxNumberOfIterations;
        return this;
    }

    public MorphologyTransform setMaxNumberOfIterations(String maxNumberOfIterations) {
        return setMaxNumberOfIterations(longOrNull(maxNumberOfIterations));
    }

    public String getPatternsSpecification() {
        return patternsSpecification;
    }

    public MorphologyTransform setPatternsSpecification(String patternsSpecification) {
        if (!this.patternsSpecification.equals(patternsSpecification)) {
            this.patternsSpecification = nonNull(patternsSpecification);
            this.patterns = parsePatterns(patternsSpecification);
        }
        return this;
    }

    public ContinuationMode getContinuationMode() {
        return continuationMode;
    }

    public MorphologyTransform setContinuationMode(ContinuationMode continuationMode) {
        this.continuationMode = nonNull(continuationMode);
        return this;
    }

    public boolean isConvertToMono() {
        return convertToMono;
    }

    public MorphologyTransform setConvertToMono(boolean convertToMono) {
        this.convertToMono = convertToMono;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        if (convertToMono) {
            source = source.asMono();
        }
        return super.process(source);
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        if (currentChannel() == 0) {
            logDebug(() -> "Morphology transform " + transformOperation + " with " + patternsSpecification
                    + (continuationMode == ContinuationMode.DEFAULT ? "" : ", " + continuationMode.continuationMode())
                    + " for " + sourceMultiMatrix());
        }
        final Class<UpdatablePArray> resultType = Arrays.type(UpdatablePArray.class, resultElementType.elementType);
        IterativeArrayProcessor<Matrix<? extends UpdatablePArray>> processor = transformOperation.getProcessor(
                createMorphology(m), resultType, m, patterns);
        if (maxNumberOfIterations != null) {
            processor = processor.limitIterations(maxNumberOfIterations);
        }
        Matrix<? extends PArray> result = processor.process();
        final double maxPossibleValue = m.array().maxPossibleValue(1.0);
        if (maxPossibleValue != 1.0) {
            result = Matrices.asFuncMatrix(
                    LinearFunc.getInstance(0.0, 1.0 / maxPossibleValue),
                    FloatArray.class,
                    result);
        }
        return result;
    }

    public Morphology createMorphology(Matrix<? extends PArray> m) {
        Morphology morphology = BasicMorphology.getInstance(null);
        if (continuationMode != ContinuationMode.DEFAULT) {
            final Matrix.ContinuationMode mode =
                    this.continuationMode == ContinuationMode.NEGATIVE_INFINITY && m.elementType() == boolean.class ?
                            Matrix.ContinuationMode.ZERO_CONSTANT :
                            this.continuationMode.continuationModeOrNull();
            // For binary matrices, without this correction, negative infinity will be equivalent to 1, not to 0!
            // (For bits, any value != 0 is considered to be 1. Here it is not too logical.)
            morphology = ContinuedMorphology.getInstance(morphology, mode);
        }
        return morphology;
    }

    private static Pattern[] parsePatterns(String patternsSpecification) {
        final String[] patternsDescriptions = patternsSpecification.split("\\|");
        final Pattern[] patterns = new Pattern[patternsDescriptions.length];
        final PatternSpecificationParser parser = PatternSpecificationParser.getInstance(byte.class);
        for (int k = 0; k < patterns.length; k++) {
            patterns[k] = parser.parse(patternsDescriptions[k]);
        }
        return patterns;
    }
}
