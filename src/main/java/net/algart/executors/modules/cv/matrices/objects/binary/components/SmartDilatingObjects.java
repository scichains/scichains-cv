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

package net.algart.executors.modules.cv.matrices.objects.binary.components;

import net.algart.arrays.Matrix;
import net.algart.executors.modules.core.common.matrices.SeveralMultiMatricesOperation;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyOperation;
import net.algart.executors.modules.cv.matrices.morphology.StrictMorphology;
import net.algart.executors.modules.cv.matrices.objects.RetainOrRemoveMode;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class SmartDilatingObjects extends SeveralMultiMatricesOperation {
    public static final String INPUT_SURELY = "surely";
    public static final String INPUT_MAYBE = "maybe";
    public static final String INPUT_UNLIKELY = "unlikely";

    private MorphologyFilter.Shape surelyDilationShape = MorphologyFilter.Shape.SPHERE;
    private int surelyDilationSize = 11;
    private String surelyCustomPatternSpecification = null;

    private MorphologyFilter.Shape unlikelyErosionShape = MorphologyFilter.Shape.SPHERE;
    private int unlikelyErosionSize = 31;
    private String unlikelyCustomPatternSpecification = null;

    public SmartDilatingObjects() {
        super(INPUT_SURELY, INPUT_MAYBE, INPUT_UNLIKELY);
    }

    public MorphologyFilter.Shape getSurelyDilationShape() {
        return surelyDilationShape;
    }

    public SmartDilatingObjects setSurelyDilationShape(MorphologyFilter.Shape surelyDilationShape) {
        this.surelyDilationShape = nonNull(surelyDilationShape);
        return this;
    }

    public int getSurelyDilationSize() {
        return surelyDilationSize;
    }

    public SmartDilatingObjects setSurelyDilationSize(int surelyDilationSize) {
        this.surelyDilationSize = nonNegative(surelyDilationSize);
        return this;
    }

    public String getSurelyCustomPatternSpecification() {
        return surelyCustomPatternSpecification;
    }

    public SmartDilatingObjects setSurelyCustomPatternSpecification(String surelyCustomPatternSpecification) {
        this.surelyCustomPatternSpecification = surelyCustomPatternSpecification;
        return this;
    }

    public MorphologyFilter.Shape getUnlikelyErosionShape() {
        return unlikelyErosionShape;
    }

    public SmartDilatingObjects setUnlikelyErosionShape(MorphologyFilter.Shape unlikelyErosionShape) {
        this.unlikelyErosionShape = nonNull(unlikelyErosionShape);
        return this;
    }

    public int getUnlikelyErosionSize() {
        return unlikelyErosionSize;
    }

    public SmartDilatingObjects setUnlikelyErosionSize(int unlikelyErosionSize) {
        this.unlikelyErosionSize = nonNegative(unlikelyErosionSize);
        return this;
    }

    public String getUnlikelyCustomPatternSpecification() {
        return unlikelyCustomPatternSpecification;
    }

    public SmartDilatingObjects setUnlikelyCustomPatternSpecification(String unlikelyCustomPatternSpecification) {
        this.unlikelyCustomPatternSpecification = unlikelyCustomPatternSpecification;
        return this;
    }

    @Override
    public MultiMatrix process(List<MultiMatrix> sources) {
        //TODO!! multidimensional
        return process(
                sources.get(0) == null ? null : sources.get(0).asMultiMatrix2D(),
                sources.get(1) == null ? null : sources.get(1).asMultiMatrix2D(),
                sources.get(2) == null ? null : sources.get(2).asMultiMatrix2D());
    }

    public MultiMatrix2D process(MultiMatrix2D surely, MultiMatrix2D maybe, MultiMatrix2D unlikely) {
        Objects.requireNonNull(surely, "Null surely matrix");
        Objects.requireNonNull(maybe, "Null maybe matrix");
        surely = surely.nonZeroAnyChannel().clone();
        maybe = maybe.nonZeroAnyChannel().clone();
        final MultiMatrix2D possibleInPrinciple = unlikely == null ?
                null :
                unlikely.zeroAllChannels().clone();
        // - zeroAllChannels actually means "NOT" for binary matrix

        final StrictMorphology morphology = new StrictMorphology();
        morphology.setOperation(MorphologyOperation.DILATION);
        morphology.setContinuationMode(Matrix.ContinuationMode.ZERO_CONSTANT);
        morphology.setCustomPatternSpecification(surelyCustomPatternSpecification);
        morphology.setPattern(surelyDilationShape, surelyDilationSize);
        MultiMatrix2D dilatedSurely = morphology.process(surely).asMultiMatrix2D();

        final FindConnectedWithMask findConnectedWithMask = new FindConnectedWithMask();
        findConnectedWithMask.setMode(RetainOrRemoveMode.RETAIN);
        findConnectedWithMask.setIncludeMaskInRetained(false);
        // - No needs to include to mask "surely": it will be added by the last "result.max(surely)" operator
        MultiMatrix2D result = findConnectedWithMask.process(Arrays.asList(maybe, surely));

        result = result.min(dilatedSurely);
        // Stays only such connected "maybe" objects, that are not too far from surely

        maybe = null;
        dilatedSurely = null;
        // - allows to free memory

        if (possibleInPrinciple != null) {
            // Dilating possibleInPrinciple is equivalent to eroding unlikely
            morphology.setCustomPatternSpecification(unlikelyCustomPatternSpecification);
            morphology.setPattern(unlikelyErosionShape, unlikelyErosionSize);
            result = result.min(morphology.process(possibleInPrinciple).asMultiMatrix2D());
            // Stays only such connected "maybe" objects, that are close enough to possible in principle
        }
        result = result.max(surely);
        return result;
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex == 2;
    }
}
