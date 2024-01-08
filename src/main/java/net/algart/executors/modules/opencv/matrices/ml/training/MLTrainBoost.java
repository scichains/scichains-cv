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

package net.algart.executors.modules.opencv.matrices.ml.training;

import net.algart.executors.modules.opencv.matrices.ml.MLKind;
import net.algart.executors.modules.opencv.matrices.ml.MLSamplesType;
import net.algart.executors.modules.opencv.matrices.ml.MLStatModelTrainer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_ml.Boost;

import java.util.Locale;

public final class MLTrainBoost extends MLTrainDTrees {
    public enum BoostType {
        DISCRETE(Boost.DISCRETE),
        REAL(Boost.REAL), // - default
        LOGIT(Boost.LOGIT),
        GENTLE(Boost.GENTLE);

        private final int code;

        public int code() {
            return code;
        }

        BoostType(int code) {
            this.code = code;
        }
        }

    private BoostType boostType = BoostType.REAL;
    private int weakCount = 100;
    private double weightTrimRate = 0.95;

    private MLTrainBoost(MLSamplesType inputType) {
        super(inputType);
    }

    public static MLTrainBoost newTrainNumbers() {
        return new MLTrainBoost(MLSamplesType.NUMBERS);
    }

    public static MLTrainBoost newTrainPixels() {
        return new MLTrainBoost(MLSamplesType.PIXELS);
    }

    public BoostType getBoostType() {
        return boostType;
    }

    public MLTrainBoost setBoostType(BoostType boostType) {
        this.boostType = nonNull(boostType);
        return this;
    }

    public int getWeakCount() {
        return weakCount;
    }

    public MLTrainBoost setWeakCount(int weakCount) {
        this.weakCount = weakCount;
        return this;
    }

    public double getWeightTrimRate() {
        return weightTrimRate;
    }

    public MLTrainBoost setWeightTrimRate(double weightTrimRate) {
        this.weightTrimRate = weightTrimRate;
        return this;
    }

    @Override
    public void process() {
        try (final Boost model = newStatModel();
             final Mat priors = priors()) {
            model.setBoostType(boostType.code());
            model.setWeakCount(weakCount);
            model.setWeightTrimRate(weightTrimRate);
            customizeDTrees(model, priors);
            logDebug(() -> "Training " + modelKind().modelName() + ": " + toString(model));
            final MLStatModelTrainer trainer = new MLStatModelTrainer(model, modelKind());
            setTrainingFlags(trainer);
            train(trainer);
            writeTrainer(trainer);
        }
    }

    public static String toString(Boost model) {
        return String.format(Locale.US,
                "%s, "
                        + "boostType=%s, "
                        + "weakCount=%s, "
                        + "weightTrimRate=%s",
                MLTrainDTrees.toString(model),
                model.getBoostType(),
                model.getWeakCount(),
                model.getWeightTrimRate());
    }

    @Override
    protected MLKind modelKind() {
        return MLKind.StatModelBased.BOOST;
    }

    @Override
    protected boolean categoricalResponses() {
        return true;
    }

    private Boost newStatModel() {
        final Boost result = Boost.create();
        logDebug(() -> "Creating Boost: " + toString(result));
        return result;
    }
}
