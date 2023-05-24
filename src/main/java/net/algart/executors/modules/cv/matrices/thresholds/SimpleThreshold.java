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

package net.algart.executors.modules.cv.matrices.thresholds;

import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.executors.modules.cv.matrices.objects.RetainOrRemoveMode;
import net.algart.executors.modules.cv.matrices.objects.binary.components.FindConnectedWithMask;
import net.algart.arrays.BitArray;
import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.RectangularFunc;
import net.algart.multimatrix.MultiMatrix;

import java.util.Arrays;
import java.util.Locale;

public class SimpleThreshold extends MultiMatrixFilter {
    public static final String INPUT_MASK = "mask";

    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;
    private boolean hysteresis = false;
    private double hysteresisMin = Double.NEGATIVE_INFINITY;
    private double hysteresisMax = Double.POSITIVE_INFINITY;
    private boolean invert = false;
    private boolean rawValues = false;

    public SimpleThreshold() {
        addInputMat(INPUT_MASK);
    }

    public double getMin() {
        return min;
    }

    public SimpleThreshold setMin(double min) {
        this.min = min;
        return this;
    }

    public SimpleThreshold setMin(String min) {
        this.min = doubleOrNegativeInfinity(min);
        return this;
    }

    public double getMax() {
        return max;
    }

    public SimpleThreshold setMax(double max) {
        this.max = max;
        return this;
    }

    public SimpleThreshold setMax(String max) {
        this.max = doubleOrPositiveInfinity(max);
        return this;
    }

    public boolean isHysteresis() {
        return hysteresis;
    }

    public SimpleThreshold setHysteresis(boolean hysteresis) {
        this.hysteresis = hysteresis;
        return this;
    }

    public double getHysteresisMin() {
        return hysteresisMin;
    }

    public SimpleThreshold setHysteresisMin(double hysteresisMin) {
        this.hysteresisMin = hysteresisMin;
        return this;
    }

    public SimpleThreshold setHysteresisMin(String hysteresisMin) {
        this.hysteresisMin = doubleOrNegativeInfinity(hysteresisMin);
        return this;
    }

    public double getHysteresisMax() {
        return hysteresisMax;
    }

    public SimpleThreshold setHysteresisMax(double hysteresisMax) {
        this.hysteresisMax = hysteresisMax;
        return this;
    }

    public SimpleThreshold setHysteresisMax(String hysteresisMax) {
        this.hysteresisMax = doubleOrPositiveInfinity(hysteresisMax);
        return this;
    }

    public boolean isInvert() {
        return invert;
    }

    public SimpleThreshold setInvert(boolean invert) {
        this.invert = invert;
        return this;
    }

    public boolean isRawValues() {
        return rawValues;
    }

    public SimpleThreshold setRawValues(boolean rawValues) {
        this.rawValues = rawValues;
        return this;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        return process(
                source,
                getInputMat(INPUT_MASK, true).toMultiMatrix());
    }

    public MultiMatrix2D process(MultiMatrix2D source) {
        return process((MultiMatrix) source).asMultiMatrix2D();
    }

    public MultiMatrix process(MultiMatrix source, MultiMatrix mask) {
        if (hysteresis && source.dimCount() != 2) {
            throw new IllegalArgumentException("Only 2-dimensional matrices allowed for hysteresis");
        }
        final Matrix<? extends PArray> intensity = source.intensityChannel();
        final double scale = rawValues ? 1.0 : intensity.array().maxPossibleValue(1.0);
        final double inValue = invert ? 0.0 : 1.0;
        final double outValue = invert ? 1.0 : 0.0;
        long t1 = debugTime();
        MultiMatrix result = MultiMatrix.valueOfMono(Matrices.asFuncMatrix(
            RectangularFunc.getInstance(min * scale, max * scale, inValue, outValue),
            BitArray.class, intensity)).clone();
        long t2 = debugTime();
        if (hysteresis) {
            final MultiMatrix2D hysteresisResult = MultiMatrix.valueOf2DMono(Matrices.asFuncMatrix(
                RectangularFunc.getInstance(hysteresisMin * scale, hysteresisMax * scale, inValue, outValue),
                BitArray.class, intensity)).clone();
            final FindConnectedWithMask filter = new FindConnectedWithMask();
            filter.setMode(RetainOrRemoveMode.RETAIN);
            result = filter.process(Arrays.asList(hysteresisResult, result));
        }
        long t3 = debugTime();
        if (mask != null) {
            result = result.min(mask).clone();
        }
        long t4 = debugTime();
        logDebug(() -> String.format(Locale.US, "Simple threshold of %s calculated in %.3f ms: "
                        + "%.3f threshold, "
                        + "%.3f histeresis, "
                        + "%.3f masking",
                source, (t4 - t1) * 1e-6,
                (t2 - t1) * 1e-6,
                (t3 - t2) * 1e-6,
                (t4 - t3) * 1e-6));
        return result;
    }
}
