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

package net.algart.executors.modules.opencv.matrices.ml;

import net.algart.executors.modules.opencv.matrices.ml.prediction.MLPredict;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;

public enum MLSamplesType {
    NUMBERS(DataType.NUMBERS) {
        @Override
        void train(AbstractMLTrain executor, MLTrainer trainer) {
            final SNumbers numbers = executor.getInputNumbers(AbstractMLOperation.INPUT_SAMPLES);
            final int blockLength = getBlockLengthAndCheckForCombinedSamplesAndResponses(numbers);
            final boolean combined = executor.isTrainingCombinedSamplesAndResponses();
            final SNumbers samples = combined ?
                    numbers.columnRange(0, blockLength - 1) :
                    numbers;
            final SNumbers responses = combined ?
                    numbers.columnRange(blockLength - 1, 1) :
                    executor.getInputNumbers(AbstractMLOperation.INPUT_TRAINING_RESPONSES);
            final SNumbers autoTestResult = new SNumbers();
            final double error = executor.trainNumbers(trainer, samples, responses, autoTestResult);
            if (autoTestResult.isInitialized()) {
                executor.getScalar(AbstractMLTrain.OUTPUT_TRAINING_ERROR).setTo(error);
            }
            if (executor.isTestPredictTrainedSamples()) {
                autoTestResult.exchange(MLPredict.predict(
                        trainer, samples, executor.isConvertCategoricalResponses(), executor.isUseGPU()));
            }
            if (autoTestResult.isInitialized()) {
                executor.getNumbers().exchange(autoTestResult);
            }
        }

        @Override
        void predict(AbstractMLPredict executor, MLPredictor predictor) {
            final SNumbers samples = executor.getInputNumbers(AbstractMLOperation.INPUT_SAMPLES);
            executor.getNumbers().setTo(executor.predictNumbers(predictor, samples));
        }
    },
    PIXELS(DataType.MAT) {
        @Override
        void train(AbstractMLTrain executor, MLTrainer trainer) {
            final SMat samples = executor.getInputMat(AbstractMLOperation.INPUT_SAMPLES);
            final SMat responses = executor.getInputMat(AbstractMLOperation.INPUT_TRAINING_RESPONSES);
            final SMat autoTestResult = new SMat();
            final double error = executor.trainPixels(trainer, samples, responses, autoTestResult);
            if (autoTestResult.isInitialized()) {
                executor.getScalar(AbstractMLTrain.OUTPUT_TRAINING_ERROR).setTo(error);
            }
            if (executor.isTestPredictTrainedSamples()) {
                autoTestResult.exchange(MLPredict.predict(
                        trainer, samples, executor.isConvertCategoricalResponses(), executor.isUseGPU()));
            }
            if (autoTestResult.isInitialized()) {
                executor.getMat().exchange(autoTestResult);
            }
        }

        @Override
        void predict(AbstractMLPredict executor, MLPredictor predictor) {
            final SMat samples = executor.getInputMat(AbstractMLOperation.INPUT_SAMPLES);
            executor.getMat().setTo(executor.predictPixels(predictor, samples));
        }
    };

    final DataType portDataType;

    MLSamplesType(DataType portDataType) {
        this.portDataType = portDataType;
    }

    abstract void train(AbstractMLTrain executor, MLTrainer trainer);

    abstract void predict(AbstractMLPredict executor, MLPredictor predictor);

    private static int getBlockLengthAndCheckForCombinedSamplesAndResponses(SNumbers samples) {
        final int blockLength = samples.getBlockLength();
        if (blockLength <= 1) {
            throw new IllegalArgumentException("Input samples must contain more than 1 column: "
                    + "the last column must contain training responses");
        }
        return blockLength;
    }
}
