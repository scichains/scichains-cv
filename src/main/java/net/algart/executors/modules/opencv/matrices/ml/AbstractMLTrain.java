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

package net.algart.executors.modules.opencv.matrices.ml;

import net.algart.executors.modules.cv.matrices.pixels.GetLabelledPixels;
import net.algart.executors.modules.cv.matrices.pixels.SetPixels;
import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.parameters.Parameters;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_ml;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_ml.TrainData;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.Arrays;

public abstract class AbstractMLTrain extends AbstractMLOperation {
    public static final String OUTPUT_ACTUAL_TRAINING_RESPONSES = "training_responses";
    public static final String OUTPUT_TRAINING_MODEL_FILE = "model_file";
    public static final String OUTPUT_TRAINING_METADATA = "metadata";
    public static final String OUTPUT_TRAINING_ERROR = "error";
    public static final String OUTPUT_IS_CLASSIFIER = "is_classifier";

    private boolean convertCategoricalResponses = false;
    private boolean calculateError = false;
    private boolean testPredictTrainedSamples = false;
    private boolean trainingCombinedSamplesAndResponses = false;
    // - this flag can be used in MLInputType.NUMBERS mode only
    private int trainingFlags = 0;

    protected AbstractMLTrain(MLSamplesType samplesType) {
        super(samplesType);
        addPort(Port.newInput(INPUT_SAMPLES, samplesType.portDataType));
        addPort(Port.newOutput(DEFAULT_OUTPUT_PORT, samplesType.portDataType));
        addPort(Port.newInput(INPUT_TRAINING_RESPONSES, samplesType.portDataType));
        addOutputNumbers(OUTPUT_ACTUAL_TRAINING_RESPONSES);
        addOutputScalar(OUTPUT_TRAINING_MODEL_FILE);
        addOutputScalar(OUTPUT_TRAINING_METADATA);
        addOutputScalar(OUTPUT_TRAINING_ERROR);
        addOutputScalar(OUTPUT_IS_CLASSIFIER);
    }

    public final boolean isConvertCategoricalResponses() {
        return convertCategoricalResponses;
    }

    public final void setConvertCategoricalResponses(
            boolean convertCategoricalResponses) {
        this.convertCategoricalResponses = convertCategoricalResponses;
    }

    public final boolean isCalculateError() {
        return calculateError;
    }

    public final void setCalculateError(boolean calculateError) {
        this.calculateError = calculateError;
    }

    public final boolean isTestPredictTrainedSamples() {
        return testPredictTrainedSamples;
    }

    public final void setTestPredictTrainedSamples(boolean testPredictTrainedSamples) {
        this.testPredictTrainedSamples = testPredictTrainedSamples;
    }

    public final boolean isTrainingCombinedSamplesAndResponses() {
        return trainingCombinedSamplesAndResponses;
    }

    public final void setTrainingCombinedSamplesAndResponses(boolean trainingCombinedSamplesAndResponses) {
        this.trainingCombinedSamplesAndResponses = trainingCombinedSamplesAndResponses;
    }

    public final int getTrainingFlags() {
        return trainingFlags;
    }

    public final void setTrainingFlags(int trainingFlags) {
        this.trainingFlags = trainingFlags;
    }

    public final void setTrainingFlagByMask(int bitMask, boolean value) {
        if (value) {
            this.trainingFlags |= bitMask;
        } else {
            this.trainingFlags &= ~bitMask;
        }
    }

    public final boolean getTrainingFlagByMask(int bitMask) {
        return (trainingFlags & bitMask) != 0;
    }

    // High-level function
    public final void train(MLTrainer trainer) {
        Objects.requireNonNull(trainer, "Null trainer");
        samplesType().train(this, trainer);
    }

    public double trainNumbers(MLTrainer trainer, SNumbers samples, SNumbers responses, SNumbers autoTestResult) {
        Objects.requireNonNull(trainer, "Null trainer");
        Objects.requireNonNull(samples, "Null samples");
        Objects.requireNonNull(responses, "Null responses");
        if (samples.n() != responses.n()) {
            throw new IllegalArgumentException("Source and training responses arrays have different length: "
                    + samples + " and " + responses);
        }
        double error;
        if (isUseGPU()) {
            try (
                    UMat samplesMat = O2SMat.numbersToMulticolumn32BitUMat(samples, false);
                    UMat responsesMat = O2SMat.numbersToMulticolumn32BitUMat(responses,
                            categoricalResponses());
                    UMat binary = convertCategoricalResponses ?
                            categoricalToMultiBinaryResponses(responsesMat) : null;
                    UMat autoTestResultsMat = calculateError ? new UMat() : null) {
                final UMat actualResponses = binary != null ? binary : responsesMat;
                error = doTrain(trainer, samplesMat, actualResponses, autoTestResultsMat);
                if (isOutputNecessary(OUTPUT_ACTUAL_TRAINING_RESPONSES)) {
                    getNumbers(OUTPUT_ACTUAL_TRAINING_RESPONSES).exchange(
                            O2SMat.multicolumnMatToNumbers(actualResponses));
                }
                if (autoTestResultsMat != null) {
                    autoTestResult.exchange(O2SMat.multicolumnMatToNumbers(autoTestResultsMat));
                }
            }
        } else {
            try (
                    Mat samplesMat = O2SMat.numbersToMulticolumn32BitMat(samples, false);
                    Mat responsesMat = O2SMat.numbersToMulticolumn32BitMat(responses,
                            categoricalResponses());
                    Mat binary = convertCategoricalResponses ?
                            categoricalToMultiBinaryResponses(responsesMat) : null;
                    Mat autoTestResultsMat = calculateError ? new Mat() : null) {
                final Mat actualResponses = binary != null ? binary : responsesMat;
                error = doTrain(trainer, samplesMat, actualResponses, autoTestResultsMat);
                if (isOutputNecessary(OUTPUT_ACTUAL_TRAINING_RESPONSES)) {
                    getNumbers(OUTPUT_ACTUAL_TRAINING_RESPONSES).exchange(
                            O2SMat.multicolumnMatToNumbers(actualResponses));
                }
                if (autoTestResultsMat != null) {
                    autoTestResult.exchange(O2SMat.multicolumnMatToNumbers(autoTestResultsMat));
                }
            }
        }
        return error;
    }

    public double trainPixels(MLTrainer trainer, SMat samples, SMat responses, SMat autoTestResult) {
        Objects.requireNonNull(trainer, "Null trainer");
        Objects.requireNonNull(samples, "Null samples");
        Objects.requireNonNull(responses, "Null responses");
        final SNumbers sampleNumbers = new SNumbers();
        final SNumbers responseNumbers = new SNumbers();
        final MultiMatrix2D labels = responses.toMultiMatrix2D();
        new GetLabelledPixels().process(samples.toMultiMatrix2D(), labels, sampleNumbers, responseNumbers);
        // Important to call GetLabelledPixels: we need to train only NON-ZERO labels,
        // according to logic of GetLabelledPixels.
        // It is not a quick solution, but it is not too important, because training usually
        // works relatively slow.
        final SNumbers autoTestResultNumbers = new SNumbers();
        final double error = trainNumbers(trainer, sampleNumbers, responseNumbers, autoTestResultNumbers);
        if (autoTestResultNumbers.isInitialized()) {
            autoTestResult.setTo(new SetPixels().process(autoTestResultNumbers, labels, null));
            // - labels in a role of the mask (non-zero only)
        }
        return error;
    }

    public final void writeTrainer(MLTrainer trainer) {
        Objects.requireNonNull(trainer, "Null trainer");
        try {
            getScalar(OUTPUT_IS_CLASSIFIER).setTo(trainer.isClassifier());
            final Path file = statModelFile();
            trainer.save(file);
            getScalar(OUTPUT_TRAINING_MODEL_FILE).setTo(statModelFile());
            final MLMetadataJson metadata = metadata(trainer);
            if (metadata != null) {
                getScalar(OUTPUT_TRAINING_METADATA).setTo(metadata.jsonString());
                metadata.write(MLMetadataJson.metadataFile(file));
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public final void setTrainingFlags(MLStatModelTrainer trainer) {
        Objects.requireNonNull(trainer, "Null trainer");
        trainer.setTrainingFlags(trainingFlags);
    }

    protected abstract MLKind modelKind();

    // Note: trainer is not actually used in current implementations
    protected MLMetadataJson metadata(MLTrainer trainer) {
        final Map<String, Object> parameters = new LinkedHashMap<>(parameters());
        final Set<String> propertyNames = new HashSet<>(parameters.keySet());
        propertyNames.stream().filter(SystemParameter::isSystemParameter).forEach(parameters::remove);
        // - removing not interesting properties
        return new MLMetadataJson()
                .setModelKind(modelKind())
                .setCreatedBy(getClass().getCanonicalName())
                .setParameters(Parameters.toJson(parameters));
    }

    protected abstract boolean categoricalResponses();

    protected double doTrain(
            MLTrainer trainer,
            Mat samples,
            Mat responses,
            Mat autoTestResults) {
        final int sampleLength = samples.cols();
        final int responseLength = responses.cols();
        try (final Mat varType = toMat(varType(sampleLength, responseLength));
             final TrainData trainData = TrainData.create(
                samples, O2SMat.ML_LAYOUT, responses,
                null,
                null,
                null,
                varType)) {
            doTrain(trainer, trainData, sampleLength, responseLength);
            if (calculateError) {
                return doCalculateError(trainer, trainData, autoTestResults);
            } else {
                return Double.NaN;
            }
        }
    }

    protected double doTrain(
            MLTrainer trainer,
            UMat samples,
            UMat responses,
            UMat autoTestResults) {
        final int sampleLength = samples.cols();
        final int responseLength = responses.cols();
        try (final UMat varType = toUMat(varType(sampleLength, responseLength));
             final TrainData trainData = TrainData.create(
                     samples, O2SMat.ML_LAYOUT, responses,
                     null,
                     null,
                     null,
                     varType)) {
            doTrain(trainer, trainData, sampleLength, responseLength);
            if (calculateError) {
                return doCalculateError(trainer, trainData, autoTestResults);
            } else {
                return Double.NaN;
            }
        }
    }

    // - May be overridden in a case of non-standard processing
    protected void doTrain(MLTrainer trainer, TrainData trainData, int sampleLength, int responseLength) {
        trainer.train(trainData);
    }

    // - May be overridden to implement some non-standard way of calculating error
    protected double doCalculateError(MLTrainer trainer, TrainData trainData, Mat result) {
        return trainer.calculateError(trainData, result);
    }

    // - May be overridden to implement some non-standard way of calculating error
    protected double doCalculateError(MLTrainer trainer, TrainData trainData, UMat result) {
        return trainer.calculateError(trainData, result);
    }

    protected byte[] varType(int numberOfSamples, int numberOfResponses) {
        byte[] result = new byte[numberOfSamples + numberOfResponses];
        Arrays.fill(result, (byte) opencv_ml.VAR_ORDERED);
        if (categoricalResponses() && numberOfResponses == 1) {
            // - like in OpenCV TrainData class, setData method
            Arrays.fill(result, numberOfSamples, result.length, (byte) opencv_ml.VAR_CATEGORICAL);
        }
        return result;
    }

    private static Mat toMat(byte[] array) {
        return OTools.toMat(1, array.length, opencv_core.CV_8UC1, array);
    }

    private static UMat toUMat(byte[] array) {
        return OTools.toUMat(1, array.length, opencv_core.CV_8UC1, array);
    }
}
