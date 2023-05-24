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

package net.algart.executors.modules.opencv.matrices.ml.prediction;

import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.opencv.matrices.ml.AbstractMLPredict;
import net.algart.executors.modules.opencv.matrices.ml.MLKind;
import net.algart.executors.modules.opencv.matrices.ml.MLPredictor;
import net.algart.executors.modules.opencv.matrices.ml.MLSamplesType;
import org.bytedeco.opencv.opencv_ml.DTrees;

public final class MLPredict extends AbstractMLPredict {
    private MLKind.StatModelBased defaultPredictor = MLKind.StatModelBased.SVM;

    private MLPredict(MLSamplesType samplesType) {
        super(samplesType);
    }

    public static MLPredict newPredictNumbers() {
        return new MLPredict(MLSamplesType.NUMBERS);
    }

    public static MLPredict newPredictPixels() {
        return new MLPredict(MLSamplesType.PIXELS);
    }

    public MLKind.StatModelBased getDefaultPredictor() {
        return defaultPredictor;
    }

    public MLPredict setDefaultPredictor(MLKind.StatModelBased defaultPredictor) {
        this.defaultPredictor = nonNull(defaultPredictor);
        return this;
    }

    public boolean isPredictionDTreesSum() {
        return getPredictionFlagByMask(DTrees.PREDICT_SUM);
    }

    public MLPredict setPredictionDTreesSum(boolean predictionDTreesSum) {
        setPredictionFlagByMask(DTrees.PREDICT_SUM, predictionDTreesSum);
        return this;
    }

    public boolean isPredictionDTreesMaxVote() {
        return getPredictionFlagByMask(DTrees.PREDICT_MAX_VOTE);
    }

    public MLPredict setPredictionDTreesMaxVote(boolean predictionDTreesMaxVote) {
        setPredictionFlagByMask(DTrees.PREDICT_MAX_VOTE, predictionDTreesMaxVote);
        return this;
    }

    @Override
    public void process() {
        try (MLPredictor predictor = readStandardPredictor(defaultPredictor)) {
            setPredictionFlags(predictor);
            predict(predictor);
        }
    }

    public static SNumbers predict(
            MLPredictor predictor,
            SNumbers samples,
            boolean selectIndexesOfMaximalResponses,
            boolean useGPU) {
        try (MLPredict predict = MLPredict.newPredictNumbers()) {
            predict.setSelectIndexesOfMaximalResponses(selectIndexesOfMaximalResponses);
            predict.setUseGPU(useGPU);
            return predict.predictNumbers(predictor, samples);
        }
    }

    public static SMat predict(
            MLPredictor predictor,
            SMat samples,
            boolean selectIndexesOfMaximalResponses,
            boolean useGPU) {
        try (MLPredict predict = MLPredict.newPredictPixels()) {
            predict.setSelectIndexesOfMaximalResponses(selectIndexesOfMaximalResponses);
            predict.setUseGPU(useGPU);
            return predict.predictPixels(predictor, samples);
        }
    }
}
