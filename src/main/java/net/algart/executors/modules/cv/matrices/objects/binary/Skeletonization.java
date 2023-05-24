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

import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;
import net.algart.arrays.*;
import net.algart.matrices.skeletons.OctupleThinningSkeleton2D;
import net.algart.matrices.skeletons.Quadruple3x5ThinningSkeleton2D;
import net.algart.matrices.skeletons.StrongQuadruple3x5ThinningSkeleton2D;
import net.algart.matrices.skeletons.WeakOctupleThinningSkeleton2D;

public final class Skeletonization extends BitMultiMatrixFilter {
    public enum Algorithm {
        OCTUPLE_PLUS_QUADRUPLE_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning,
                boolean topological)
            {
                return OctupleThinningSkeleton2D.getInstance(
                    null, m, diagonalThinning, topological)
                    .chain(Quadruple3x5ThinningSkeleton2D.getInstance(null, m), 0.01);
            }

            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getPostprocessingTopologicalProcessor(
                    Matrix<? extends UpdatableBitArray> m,
                    boolean diagonalThinning) {
                return OctupleThinningSkeleton2D.getInstance(null, m, diagonalThinning, true);
            }
        },
        OCTUPLE_PLUS_STRONG_QUADRUPLE_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning,
                boolean topological)
            {
                return OctupleThinningSkeleton2D.getInstance(
                    null, m, diagonalThinning, topological)
                    .chain(StrongQuadruple3x5ThinningSkeleton2D.getInstance(null, m), 0.01);
            }

            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getPostprocessingTopologicalProcessor(
                    Matrix<? extends UpdatableBitArray> m,
                    boolean diagonalThinning) {
                return OctupleThinningSkeleton2D.getInstance(null, m, diagonalThinning, true);
            }
        },
        OCTUPLE_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning,
                boolean topological)
            {
                return OctupleThinningSkeleton2D.getInstance(
                    null, m, diagonalThinning, topological);
            }
        },
        WEAK_OCTUPLE_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
            boolean diagonalThinning,
            boolean topological)
            {
                return WeakOctupleThinningSkeleton2D.getInstance(
                    null, m, true, diagonalThinning, topological);
            }
        },
        QUADRUPLE_3_X_5_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning,
                boolean topological)
            {
                return Quadruple3x5ThinningSkeleton2D.getInstance(
                    null, m);
            }

            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getPostprocessingTopologicalProcessor(
                    Matrix<? extends UpdatableBitArray> m,
                    boolean diagonalThinning) {
                return OctupleThinningSkeleton2D.getInstance(null, m, diagonalThinning, true);
            }
        },
        STRONG_QUADRUPLE_3_X_5_THINNING() {
            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning,
                boolean topological)
            {
                return StrongQuadruple3x5ThinningSkeleton2D.getInstance(
                    null, m);
            }

            @Override
            IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getPostprocessingTopologicalProcessor(
                    Matrix<? extends UpdatableBitArray> m,
                    boolean diagonalThinning) {
                return OctupleThinningSkeleton2D.getInstance(null, m, diagonalThinning, true);
            }
        };

        abstract IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getSkeletonProcessor(
            Matrix<? extends UpdatableBitArray> m,
            boolean diagonalThinning,
            boolean topological);

        IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> getPostprocessingTopologicalProcessor(
                Matrix<? extends UpdatableBitArray> m,
                boolean diagonalThinning) {
            return getSkeletonProcessor(m, diagonalThinning, true);
        }
    }

    private Algorithm algorithm = Algorithm.OCTUPLE_PLUS_STRONG_QUADRUPLE_THINNING;
    private boolean diagonalThinning = true;
    // - ignored in QUADRUPLE_3_X_5_THINNING and STRONG_QUADRUPLE_3_X_5_THINNING
    private boolean topological = false;
    // - ignored in QUADRUPLE_3_X_5_THINNING and STRONG_QUADRUPLE_3_X_5_THINNING
    private long maxNumberOfIterations = -1;
    private long numberOfAdditionalTopologicalIterations = 0;
    private boolean addBorder = false;

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public Skeletonization setAlgorithm(Algorithm algorithm) {
        this.algorithm = nonNull(algorithm);
        return this;
    }

    public boolean isDiagonalThinning() {
        return diagonalThinning;
    }

    public Skeletonization setDiagonalThinning(boolean diagonalThinning) {
        this.diagonalThinning = diagonalThinning;
        return this;
    }

    public boolean isTopological() {
        return topological;
    }

    public Skeletonization setTopological(boolean topological) {
        this.topological = topological;
        return this;
    }

    public long getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public Skeletonization setMaxNumberOfIterations(long maxNumberOfIterations) {
        this.maxNumberOfIterations = maxNumberOfIterations;
        return this;
    }

    public long getNumberOfAdditionalTopologicalIterations() {
        return numberOfAdditionalTopologicalIterations;
    }

    public Skeletonization setNumberOfAdditionalTopologicalIterations(long numberOfAdditionalTopologicalIterations) {
        this.numberOfAdditionalTopologicalIterations = numberOfAdditionalTopologicalIterations;
        return this;
    }

    public boolean isAddBorder() {
        return addBorder;
    }

    public void setAddBorder(boolean addBorder) {
        this.addBorder = addBorder;
    }

    @Override
    public Matrix<? extends PArray> processMatrix(Matrix<? extends PArray> bitMatrix) {
        final Matrix<UpdatableBitArray> updatableBitMatrix = cloneBit(bitMatrix.cast(BitArray.class));
        if (addBorder) {
            drawBorder(updatableBitMatrix, zeroExtendingValue());
        }
        IterativeArrayProcessor<Matrix<? extends UpdatableBitArray>> processor =
            algorithm.getSkeletonProcessor(updatableBitMatrix, diagonalThinning, topological)
            .limitIterations(maxNumberOfIterations);
        logDebug(() -> "Skeletonization: " + processor
            + " for " + bitMatrix);
        Matrix<? extends UpdatableBitArray> result = processor.process();
        if (numberOfAdditionalTopologicalIterations != 0) {
            result = algorithm.getPostprocessingTopologicalProcessor(result, diagonalThinning)
                    .limitIterations(numberOfAdditionalTopologicalIterations).process();
        }
        return result;
    }

    private static void drawBorder(Matrix<UpdatableBitArray> bitMatrix, int d) {
        final long dimX = bitMatrix.dimX() - 2 * d;
        final long dimY = bitMatrix.dimY() - 2 * d;
        if (dimX > 0 && dimY > 0) {
            bitMatrix.subMatr(d, d, dimX, 1).array().fill(true);
            bitMatrix.subMatr(d, dimY + d - 1, dimX, 1).array().fill(true);
            bitMatrix.subMatr(d, d, 1, dimY).array().fill(true);
            bitMatrix.subMatr(dimX + d - 1, d, 1, dimY).array().fill(true);
        }
    }

    @Override
    protected boolean zeroExtending() {
        return true;
    }
}
