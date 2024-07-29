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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.executors.modules.core.common.matrices.MultiMatrix2DFilter;
import net.algart.executors.modules.cv.matrices.morphology.MorphologyFilter;
import net.algart.executors.modules.cv.matrices.objects.binary.components.SmartDilatingObjects;
import net.algart.multimatrix.MultiMatrix2D;

public final class SmartHysteresisThreshold extends MultiMatrix2DFilter {
    private double surelyMin = Double.NEGATIVE_INFINITY;
    private double surelyMax = Double.POSITIVE_INFINITY;
    private double hysteresisMin = Double.NEGATIVE_INFINITY;
    private double hysteresisMax = Double.POSITIVE_INFINITY;
    private boolean invert = false;
    private boolean checkSurelyBackground = false;
    private double surelyBackgroundMin = Double.NEGATIVE_INFINITY;
    private double surelyBackgroundMax = Double.POSITIVE_INFINITY;
    private boolean invertBackground = true;
    private boolean rawValues = false;

    private MorphologyFilter.Shape surelyDilationShape = MorphologyFilter.Shape.SPHERE;
    private int surelyDilationSize = 11;
    private String surelyCustomPatternSpecification = null;

    private MorphologyFilter.Shape backgroundDilationShape = MorphologyFilter.Shape.SPHERE;
    private int backgroundDilationSize = 31;
    private String backgroundCustomPatternSpecification = null;

    public double getSurelyMin() {
        return surelyMin;
    }

    public SmartHysteresisThreshold setSurelyMin(double surelyMin) {
        this.surelyMin = surelyMin;
        return this;
    }

    public SmartHysteresisThreshold setSurelyMin(String surelyMin) {
        this.surelyMin = doubleOrNegativeInfinity(surelyMin);
        return this;
    }

    public double getSurelyMax() {
        return surelyMax;
    }

    public SmartHysteresisThreshold setSurelyMax(double surelyMax) {
        this.surelyMax = surelyMax;
        return this;
    }

    public SmartHysteresisThreshold setSurelyMax(String surelyMax) {
        this.surelyMax = doubleOrPositiveInfinity(surelyMax);
        return this;
    }

    public double getHysteresisMin() {
        return hysteresisMin;
    }

    public SmartHysteresisThreshold setHysteresisMin(double hysteresisMin) {
        this.hysteresisMin = hysteresisMin;
        return this;
    }

    public SmartHysteresisThreshold setHysteresisMin(String hysteresisMin) {
        this.hysteresisMin = doubleOrNegativeInfinity(hysteresisMin);
        return this;
    }

    public double getHysteresisMax() {
        return hysteresisMax;
    }

    public SmartHysteresisThreshold setHysteresisMax(double hysteresisMax) {
        this.hysteresisMax = hysteresisMax;
        return this;
    }

    public SmartHysteresisThreshold setHysteresisMax(String hysteresisMax) {
        this.hysteresisMax = doubleOrPositiveInfinity(hysteresisMax);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public SmartHysteresisThreshold setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    public boolean isCheckSurelyBackground() {
        return checkSurelyBackground;
    }

    public SmartHysteresisThreshold setCheckSurelyBackground(boolean checkSurelyBackground) {
        this.checkSurelyBackground = checkSurelyBackground;
        return this;
    }

    public double getSurelyBackgroundMin() {
        return surelyBackgroundMin;
    }

    public SmartHysteresisThreshold setSurelyBackgroundMin(double surelyBackgroundMin) {
        this.surelyBackgroundMin = surelyBackgroundMin;
        return this;
    }

    public SmartHysteresisThreshold setSurelyBackgroundMin(String surelyBackgroundMin) {
        this.surelyBackgroundMin = doubleOrNegativeInfinity(surelyBackgroundMin);
        return this;
    }

    public double getSurelyBackgroundMax() {
        return surelyBackgroundMax;
    }

    public SmartHysteresisThreshold setSurelyBackgroundMax(double surelyBackgroundMax) {
        this.surelyBackgroundMax = surelyBackgroundMax;
        return this;
    }

    public SmartHysteresisThreshold setSurelyBackgroundMax(String surelyBackgroundMax) {
        this.surelyBackgroundMax = doubleOrPositiveInfinity(surelyBackgroundMax);
        return this;
    }

    public boolean isInvertBackground() {
        return invertBackground;
    }

    public SmartHysteresisThreshold setInvertBackground(boolean invertBackground) {
        this.invertBackground = invertBackground;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public SmartHysteresisThreshold setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    public MorphologyFilter.Shape getSurelyDilationShape() {
        return surelyDilationShape;
    }

    public SmartHysteresisThreshold setSurelyDilationShape(MorphologyFilter.Shape surelyDilationShape) {
        this.surelyDilationShape = nonNull(surelyDilationShape);
        return this;
    }

    public int getSurelyDilationSize() {
        return surelyDilationSize;
    }

    public SmartHysteresisThreshold setSurelyDilationSize(int surelyDilationSize) {
        this.surelyDilationSize = nonNegative(surelyDilationSize);
        return this;
    }

    public String getSurelyCustomPatternSpecification() {
        return surelyCustomPatternSpecification;
    }

    public SmartHysteresisThreshold setSurelyCustomPatternSpecification(String surelyCustomPatternSpecification) {
        this.surelyCustomPatternSpecification = surelyCustomPatternSpecification;
        return this;
    }

    public MorphologyFilter.Shape getBackgroundDilationShape() {
        return backgroundDilationShape;
    }

    public SmartHysteresisThreshold setBackgroundDilationShape(MorphologyFilter.Shape backgroundDilationShape) {
        this.backgroundDilationShape = nonNull(backgroundDilationShape);
        return this;
    }

    public int getBackgroundDilationSize() {
        return backgroundDilationSize;
    }

    public SmartHysteresisThreshold setBackgroundDilationSize(int backgroundDilationSize) {
        this.backgroundDilationSize = nonNegative(backgroundDilationSize);
        return this;
    }

    public String getBackgroundCustomPatternSpecification() {
        return backgroundCustomPatternSpecification;
    }

    public SmartHysteresisThreshold setBackgroundCustomPatternSpecification(
            String backgroundCustomPatternSpecification) {
        this.backgroundCustomPatternSpecification = backgroundCustomPatternSpecification;
        return this;
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final MultiMatrix2D surely = new SimpleThreshold().setRawValues(rawValues)
                .setMin(surelyMin).setMax(surelyMax).setInvert(invert)
                .process(source);
        final MultiMatrix2D maybe = new SimpleThreshold().setRawValues(rawValues)
                .setMin(hysteresisMin).setMax(hysteresisMax).setInvert(invert)
                .process(source);
        final MultiMatrix2D unlikely = checkSurelyBackground ?
                new SimpleThreshold().setRawValues(rawValues)
                        .setMin(surelyBackgroundMin).setMax(surelyBackgroundMax).setInvert(!invertBackground)
                        .process(source) :
                null;
        // !invertBackground to produce unlikely (= !background)
        return new SmartDilatingObjects()
                .setSurelyDilationShape(surelyDilationShape)
                .setSurelyDilationSize(surelyDilationSize)
                .setSurelyCustomPatternSpecification(surelyCustomPatternSpecification)
                .setUnlikelyErosionShape(backgroundDilationShape)
                .setUnlikelyErosionSize(backgroundDilationSize)
                .setUnlikelyCustomPatternSpecification(backgroundCustomPatternSpecification)
                .process(surely, maybe, unlikely);
    }
}
