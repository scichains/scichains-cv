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

package net.algart.executors.modules.cv.matrices.objects.binary;

import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.arrays.BitArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.IRange;
import net.algart.math.Range;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.BasicMorphology;
import net.algart.matrices.morphology.ContinuedMorphology;
import net.algart.matrices.morphology.Morphology;
import net.algart.matrices.skeletons.BasicSkeletonPixelClassifier2D;
import net.algart.matrices.skeletons.SkeletonPixelClassifier;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;

import static net.algart.matrices.skeletons.BasicSkeletonPixelClassifier2D.*;
import static net.algart.matrices.skeletons.SkeletonPixelClassifier.AttachmentInformation.NEIGHBOUR_INDEX_OF_ATTACHED_NODE;

public final class SkeletonPixelTypes extends BitMultiMatrixFilter {
    public static final String INPUT_SKELETON = "skeleton";
    public static final String OUTPUT_TYPE_CODES = "type_codes";

    public enum Algorithm {
        OCTUPLE_THINNING() {
            @Override
            SkeletonPixelClassifier getSkeletonPixelClassifier() {
                return BasicSkeletonPixelClassifier2D.getOctupleThinningInstance();
            }
        },
        QUADRUPLE_3_X_5_THINNING() {
            @Override
            SkeletonPixelClassifier getSkeletonPixelClassifier() {
                return BasicSkeletonPixelClassifier2D.getQuadruple3x5ThinningInstance();
            }
        },
        STRONG_QUADRUPLE_3_X_5_THINNING() {
            @Override
            SkeletonPixelClassifier getSkeletonPixelClassifier() {
                return BasicSkeletonPixelClassifier2D.getStrongQuadruple3x5ThinningInstance();
            }
        };

        abstract SkeletonPixelClassifier getSkeletonPixelClassifier();
    }

    public enum PixelType {
        ILLEGAL(TYPE_ILLEGAL),
        BRANCHE_OR_FREE_BRANCH_END(TYPE_BRANCH_MIN, TYPE_BRANCH_MAX) {
            @Override
            boolean isThisPixelType(double pixelType) {
                return super.isThisPixelType(pixelType) || pixelType >= 0;
            }
        },
        BRANCH_WITHOUT_FREE_BRANCHE_END(TYPE_USUAL_BRANCH)  {
            @Override
            boolean isThisPixelType(double pixelType) {
                return super.isThisPixelType(pixelType) || pixelType >= 0;
            }
        },
        NODE_OR_ISOLATED(TYPE_USUAL_NODE, TYPE_ISOLATED),
        NODE_OR_ISOLATED_OR_FREE_BRANCH_END(TYPE_NODE_OR_BRANCH_END_MIN, TYPE_NODE_OR_BRANCH_END_MAX),
        USUAL_NODE(TYPE_USUAL_NODE),
        USUAL_BRANCH(TYPE_USUAL_BRANCH),
        ATTACHING_BRANCH(NEIGHBOUR_INDEX_MIN, NEIGHBOUR_INDEX_MAX),
        FREE_BRANCH_END(TYPE_FREE_BRANCH_END),
        ISOLATED(TYPE_ISOLATED),
        FREE_BRANCH_END_OR_ISOLATED(TYPE_FREE_BRANCH_END, TYPE_ISOLATED);

        private final Range range;

        PixelType(int... list) {
            int min = list[0];
            int max = list[0];
            for (int k = 1; k < list.length; k++) {
                min = Math.min(min, list[k]);
                max = Math.max(max, list[k]);
            }
            this.range = IRange.valueOf(min, max).toRange();
        }

        boolean isThisPixelType(double pixelType) {
            return range.contains(pixelType);
        }
    }

    private Algorithm algorithm = Algorithm.STRONG_QUADRUPLE_3_X_5_THINNING;
    private PixelType pixelType = PixelType.USUAL_NODE;
    private boolean invert = false;
    private int dilationSize = 0;

    public SkeletonPixelTypes() {
        setDefaultInputMat(INPUT_SKELETON);
        addOutputMat(OUTPUT_TYPE_CODES);
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public SkeletonPixelTypes setAlgorithm(Algorithm algorithm) {
        this.algorithm = nonNull(algorithm);
        return this;
    }

    public PixelType getPixelType() {
        return pixelType;
    }

    public SkeletonPixelTypes setPixelType(PixelType pixelType) {
        this.pixelType = nonNull(pixelType);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public SkeletonPixelTypes setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    public int getDilationSize() {
        return dilationSize;
    }

    public SkeletonPixelTypes setDilationSize(int dilationSize) {
        this.dilationSize = nonNegative(dilationSize);
        return this;
    }

    @Override
    public Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> bitMatrix) {
        final SkeletonPixelClassifier skeletonPixelClassifier = algorithm.getSkeletonPixelClassifier();
        Matrix<? extends PArray> pixelTypes = Matrices.clone(
                skeletonPixelClassifier.asPixelTypes(
                        bitMatrix.cast(BitArray.class), NEIGHBOUR_INDEX_OF_ATTACHED_NODE));
        getMat(OUTPUT_TYPE_CODES).setTo(MultiMatrix.valueOf2DMono(reduce(pixelTypes)));
        final double yesValue = invert ? 0.0 : 1.0;
        final double noValue = invert ? 1.0 : 0.0;
        Matrix<? extends PArray> result = Matrices.clone(Matrices.asFuncMatrix(
                new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        return pixelType.isThisPixelType(x0) ? yesValue : noValue;
                    }
                }, BitArray.class, pixelTypes));
        if (dilationSize > 0) {
            final Morphology morphology = ContinuedMorphology.getInstance(
                    BasicMorphology.getInstance(null),
                    Matrix.ContinuationMode.getConstantMode(noValue));
            final Pattern pattern = MorphologyFilter.Shape.SPHERE.newPattern(bitMatrix.dimCount(), dilationSize);
            result = invert ?
                    morphology.erosion(result, pattern) :
                    morphology.dilation(result, pattern);
        }
        return result;
    }

    @Override
    protected boolean zeroExtending() {
        return true;
    }
}
