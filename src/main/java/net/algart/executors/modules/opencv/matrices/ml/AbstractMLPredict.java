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

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.Port;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;
import org.bytedeco.opencv.opencv_ml.StatModel;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractMLPredict extends AbstractMLOperation {
    public static final String OUTPUT_PREDICTION_MODEL_KIND = "model_kind";
    public static final String OUTPUT_IS_CLASSIFIER = "is_classifier";

    private boolean selectIndexesOfMaximalResponses = true;
    private boolean predictionRoundResponses = false;
    private int predictionFlags = 0;

    protected AbstractMLPredict(MLSamplesType samplesType) {
        super(samplesType);
        addPort(Port.newInput(INPUT_SAMPLES, samplesType.portDataType));
        addPort(Port.newOutput(DEFAULT_OUTPUT_PORT, samplesType.portDataType));
        addOutputScalar(OUTPUT_PREDICTION_MODEL_KIND);
        addOutputScalar(OUTPUT_IS_CLASSIFIER);
    }

    public final boolean isSelectIndexesOfMaximalResponses() {
        return selectIndexesOfMaximalResponses;
    }

    public final void setSelectIndexesOfMaximalResponses(boolean selectIndexesOfMaximalResponses) {
        this.selectIndexesOfMaximalResponses = selectIndexesOfMaximalResponses;
    }

    public final boolean isPredictionRoundResponses() {
        return predictionRoundResponses;
    }

    public final void setPredictionRoundResponses(boolean predictionRoundResponses) {
        this.predictionRoundResponses = predictionRoundResponses;
    }

    public final boolean isPredictionRawOutput() {
        return getPredictionFlagByMask(StatModel.RAW_OUTPUT);
    }

    public final void setPredictionRawOutput(boolean predictionRawOutput) {
        setPredictionFlagByMask(StatModel.RAW_OUTPUT, predictionRawOutput);
    }

    public final int getPredictionFlags() {
        return predictionFlags;
    }

    public final void setPredictionFlags(int predictionFlags) {
        this.predictionFlags = predictionFlags;
    }

    public final void setPredictionFlagByMask(int bitMask, boolean value) {
        if (value) {
            this.predictionFlags |= bitMask;
        } else {
            this.predictionFlags &= ~bitMask;
        }
    }

    public final boolean getPredictionFlagByMask(int bitMask) {
        return (predictionFlags & bitMask) != 0;
    }

    public final boolean selectIndexesOfMaximalResponses(MLPredictor predictor, int responseLength) {
        return selectIndexesOfMaximalResponses && responseLength > 1 && !predictor.isClassifier();
    }

    // High-level function
    public final void predict(MLPredictor predictor) {
        Objects.requireNonNull(predictor, "Null predictor");
        samplesType().predict(this, predictor);
    }

    public SNumbers predictNumbers(MLPredictor predictor, SNumbers samples) {
        Objects.requireNonNull(predictor, "Null predictor");
        Objects.requireNonNull(samples, "Null samples");
        if (isUseGPU()) {
            try (UMat samplesMat = O2SMat.numbersToMulticolumn32BitUMat(samples, false);
                 UMat resultMat = new UMat()) {
                doPredict(predictor, samplesMat, resultMat);
                if (selectIndexesOfMaximalResponses(predictor, resultMat.cols())) {
                    try (Mat categoricalResponses = selectIndexesOfMaximalMultiResponses(resultMat)) {
                        return O2SMat.multicolumnMatToNumbers(categoricalResponses);
                    }
                }
                predictionRoundResponses(resultMat);
                return O2SMat.multicolumnMatToNumbers(resultMat);
            }
        } else {
            try (Mat samplesMat = O2SMat.numbersToMulticolumn32BitMat(samples, false);
                 Mat resultMat = new Mat()) {
                doPredict(predictor, samplesMat, resultMat);
                if (selectIndexesOfMaximalResponses(predictor, resultMat.cols())) {
                    try (Mat categoricalResponses = selectIndexesOfMaximalMultiResponses(resultMat)) {
                        return O2SMat.multicolumnMatToNumbers(categoricalResponses);
                    }
                }
                predictionRoundResponses(resultMat);

//                System.out.println("prediction samples for " + samples);
//                new PrintSubMatrix().setSizeY(500).process(OTools.toMultiMatrix(samplesMat));
//                System.out.println("prediction:");
//                new PrintSubMatrix().setSizeY(500).process(OTools.toMultiMatrix(predictionRowPerEverySampleResults));

                return O2SMat.multicolumnMatToNumbers(resultMat);
            }
        }
    }

    public SMat predictPixels(MLPredictor predictor, SMat samples) {
        Objects.requireNonNull(predictor, "Null predictor");
        Objects.requireNonNull(samples, "Null samples");
        if (isUseGPU()) {
            try (UMat samplesMat = O2SMat.matrixToMlSamplesOrResponsesUMat(samples, false);
                 UMat resultMat = new UMat()) {
                doPredict(predictor, samplesMat, resultMat);
                if (selectIndexesOfMaximalResponses(predictor, resultMat.cols())) {
                    try (Mat categoricalResponses = selectIndexesOfMaximalMultiResponses(resultMat)) {
                        return O2SMat.mlResultsToMatrix(categoricalResponses, samples.getDimX(), samples.getDimY());
                    }
                }
                predictionRoundResponses(resultMat);
                return O2SMat.mlResultsToMatrix(resultMat, samples.getDimX(), samples.getDimY());
            }
        } else {
            try (Mat samplesMat = O2SMat.matrixToMlSamplesOrResponsesMat(samples, false);
                 Mat resultMat = new Mat()) {
                doPredict(predictor, samplesMat, resultMat);
                if (selectIndexesOfMaximalResponses(predictor, resultMat.cols())) {
                    try (Mat categoricalResponses = selectIndexesOfMaximalMultiResponses(resultMat)) {
                        return O2SMat.mlResultsToMatrix(categoricalResponses, samples.getDimX(), samples.getDimY());
                    }
                }
                predictionRoundResponses(resultMat);

//                System.out.println("prediction samples for " + samples);
//                new PrintSubMatrix().setSizeY(500).process(OTools.toMultiMatrix(samplesMat));
//                System.out.println("prediction:");
//                new PrintSubMatrix().setSizeY(500).process(OTools.toMultiMatrix(resultMat));

                return O2SMat.mlResultsToMatrix(resultMat, samples.getDimX(), samples.getDimY());
            }
        }
    }

    public final MLPredictor readStandardPredictor(MLKind defaultKind) {
        return readPredictor(defaultKind, MLKind.StatModelBased::valueOfModelName);
    }

    public final MLPredictor readPredictor(MLKind defaultKind, Function<String, Optional<MLKind>> modelNameToKind) {
        Objects.requireNonNull(defaultKind, "Null defaultKind");
        Objects.requireNonNull(modelNameToKind, "Null modelNameToKind function");
        Path file = statModelFile();
        Objects.requireNonNull(file, "Null file");
        try {
            final Path metadataJsonFile = MLMetadataJson.metadataFile(file);
            MLKind kind = null;
            if (Files.exists(metadataJsonFile)) {
                final MLMetadataJson metadata = MLMetadataJson.read(metadataJsonFile, modelNameToKind);
                kind = metadata.getModelKind();
            }
            if (kind == null) {
                kind = defaultKind;
            }
            final MLPredictor predictor = kind.loadPredictor(file);
            getScalar(OUTPUT_PREDICTION_MODEL_KIND).setTo(kind.modelName());
            getScalar(OUTPUT_IS_CLASSIFIER).setTo(predictor.isClassifier());
            return predictor;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public final void setPredictionFlags(MLPredictor predictor) {
        Objects.requireNonNull(predictor, "Null predictor");
        predictor.setPredictionFlags(predictionFlags);
    }

    // - May be overridden in a case of non-standard processing
    protected void doPredict(MLPredictor predictor, Mat samples, Mat result) {
        predictor.predict(samples, result);
    }

    protected void doPredict(MLPredictor predictor, UMat samples, UMat result) {
        predictor.predict(samples, result);
    }

    private void predictionRoundResponses(Mat responses) {
        if (isPredictionRoundResponses() && OTools.isFloatingPoint(responses.depth())) {
            responses.convertTo(responses, opencv_core.CV_32S);
            // - note: it is actually rounding, not just cast
        }
    }

    private void predictionRoundResponses(UMat responses) {
        if (isPredictionRoundResponses() && OTools.isFloatingPoint(responses.depth())) {
            responses.convertTo(responses, opencv_core.CV_32S);
            // - note: it is actually rounding, not just cast
        }
    }
}
