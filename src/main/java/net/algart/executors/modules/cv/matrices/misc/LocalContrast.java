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

package net.algart.executors.modules.cv.matrices.misc;

import net.algart.executors.modules.cv.matrices.morphology.RankMorphologyFilter;
import net.algart.arrays.*;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.ContrastingFunc;
import net.algart.math.patterns.Pattern;
import net.algart.matrices.morphology.RankMorphology;
import net.algart.multimatrix.MultiMatrix;

import java.util.ArrayList;
import java.util.List;

public final class LocalContrast extends RankMorphologyFilter {
    private double level = 1.0;
    // - 1.0 means contrasting between strict erosion (0%) and dilation (100%),
    // 0.5 means contrasting between 25% and 75%
    private double minContrastedDifference = 0.1;
    // - 0.0 means contrasting any differences, 0.1 means preserving differences less 0.1,
    // 1.0 means returning non-contrasted differences with the percentile

    public double getLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = inRange(level, 0.0, 1.0);
    }

    public double getMinContrastedDifference() {
        return minContrastedDifference;
    }

    public void setMinContrastedDifference(double minContrastedDifference) {
        this.minContrastedDifference = inRange(minContrastedDifference, 0.0, 1.0);
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        if (getPatternSize() == 0) {
            return source;
        }
        final List<Matrix<? extends PArray>> sourceChannels = source.allChannels();
        final int numberOfChannels = source.numberOfChannels();
        final boolean grayscale = numberOfChannels == 1;
        Matrix<? extends PArray> intensity = source.intensityChannel();
        final double scale = intensity.array().maxPossibleValue(1.0);
        final double minCD = minContrastedDifference * scale;
        final ContrastingFunc contrastingFunc = ContrastingFunc.getInstance(scale, minCD);
        final double fromLevel = 0.5 - 0.5 * level;
        final double toLevel = 0.5 + 0.5 * level;
        if (!grayscale) {
            intensity = intensity.matrix(intensity.array().updatableClone(Arrays.SMM));
            // - actualization for RGB image
        }
        final Pattern pattern = getPattern(intensity);
        final RankMorphology morphology = createRankMorphology(intensity.elementType(), 1.0);
        logDebug(() -> "Local contrast of range " + fromLevel + ".." + toLevel
            + ", minContrastedDifference = " + minContrastedDifference
            + rankMorphologyLogMessage()
            + " with " + pattern
            + (getContinuationMode() == null ? "" : ", " + getContinuationMode())
            + " for " + source);

        final double fromPercentileIndex = fromLevel * (double) (pattern.pointCount() - 1);
        final double toPercentileIndex = toLevel * (double) (pattern.pointCount() - 1);
        final Matrix<? extends PArray> contrasted = uncompress(
            morphology.functionOfPercentilePair(
                compress(intensity),
                fromPercentileIndex,
                toPercentileIndex,
                pattern,
                contrastingFunc));
        if (grayscale) {
            return MultiMatrix.valueOf2DMono(contrasted);
        } else {
            final List<Matrix<? extends PArray>> channels = new ArrayList<>();
            for (int currentChannel = 0; currentChannel < numberOfChannels; currentChannel++) {
                Matrix<? extends PArray> m = sourceChannels.get(currentChannel);
                final AbstractFunc f = new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return x[0] * x[1] / x[2];
                    }

                    @Override
                    public double get(double x0, double x1, double x2) {
                        return x0 * x1 / x2;
                    }
                };
                channels.add(Matrices.asFuncMatrix(f, m.type(PArray.class), m, contrasted, intensity));
            }
            return MultiMatrix.valueOf(channels);
        }
    }

    @Override
    protected Matrix<? extends PArray> processChannel(Matrix<? extends PArray> m) {
        return process(MultiMatrix.valueOfMono(m)).intensityChannel();
        // - should not be called while normal usage, but if someone call it, we provide correct result
    }

    @Override
    protected Matrix<? extends PArray> processCompressedChannel(Matrix<? extends PArray> m) {
        throw new AssertionError("Very strange: this method cannot be called!");
    }
}