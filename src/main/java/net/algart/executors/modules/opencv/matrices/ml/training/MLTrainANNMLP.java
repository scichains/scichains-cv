/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import net.algart.executors.modules.opencv.matrices.ml.*;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RNG;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_ml.ANN_MLP;
import org.bytedeco.opencv.opencv_ml.StatModel;
import org.bytedeco.opencv.opencv_ml.TrainData;

import java.util.Locale;

public final class MLTrainANNMLP extends AbstractMLTrain {
    public static final String OUTPUT_LAYER_WEIGHTS = "layer_weights";

    public enum TrainingMethod {
        BACKPROP(ANN_MLP.BACKPROP),
        RPROP(ANN_MLP.RPROP), // - default
        ANNEAL(ANN_MLP.ANNEAL);

        private final int code;

        public int code() {
            return code;
        }

        TrainingMethod(int code) {
            this.code = code;
        }
    }

    public enum ActivationFunction {
        IDENTITY(ANN_MLP.IDENTITY),
        SIGMOID_SYM(ANN_MLP.SIGMOID_SYM),
        GAUSSIAN(ANN_MLP.GAUSSIAN),
        RELU(ANN_MLP.RELU),
        LEAKYRELU(ANN_MLP.LEAKYRELU);

        private final int code;

        public int code() {
            return code;
        }

        ActivationFunction(int code) {
            this.code = code;
        }
    }

    private TrainingMethod trainingMethod = TrainingMethod.RPROP;
    private double trainingMethodParam1 = 0.0;
    private double trainingMethodParam2 = 0.0;
    private ActivationFunction activationFunction = ActivationFunction.SIGMOID_SYM;
    private double activationFunctionParam1 = 0.0;
    private double activationFunctionParam2 = 0.0;
    private int[] hiddenLayerSizes = {};
    private double backpropMomentumScale = 0.1;
    private double backpropWeightScale = 0.1;
    private double rpropDW0 = 0.1;
    private double rpropDWMax = 50.0;
    private double rpropDWMin = Float.MIN_VALUE;
    private double rpropDWMinus = 0.5;
    private double rpropDWPlus = 1.2;
    private double annealCoolingRatio = 0.95;
    private double annealFinalT = 0.1;
    private double annealInitialT = 10.0;
    private int annealItePerStep = 10;
    private Integer annealEnergyRandSeed = null;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.0;
    private int layerIndexToGetWeights = 0;

    private MLTrainANNMLP(MLSamplesType inputType) {
        super(inputType);
        addOutputNumbers(OUTPUT_LAYER_WEIGHTS);
    }

    public static MLTrainANNMLP newTrainNumbers() {
        return new MLTrainANNMLP(MLSamplesType.NUMBERS);
    }

    public static MLTrainANNMLP newTrainPixels() {
        return new MLTrainANNMLP(MLSamplesType.PIXELS);
    }

    public TrainingMethod getTrainingMethod() {
        return trainingMethod;
    }

    public MLTrainANNMLP setTrainingMethod(TrainingMethod trainingMethod) {
        this.trainingMethod = nonNull(trainingMethod);
        return this;
    }

    public double getTrainingMethodParam1() {
        return trainingMethodParam1;
    }

    public MLTrainANNMLP setTrainingMethodParam1(double trainingMethodParam1) {
        this.trainingMethodParam1 = trainingMethodParam1;
        return this;
    }

    public double getTrainingMethodParam2() {
        return trainingMethodParam2;
    }

    public MLTrainANNMLP setTrainingMethodParam2(double trainingMethodParam2) {
        this.trainingMethodParam2 = trainingMethodParam2;
        return this;
    }

    public ActivationFunction getActivationFunction() {
        return activationFunction;
    }

    public MLTrainANNMLP setActivationFunction(ActivationFunction activationFunction) {
        this.activationFunction = nonNull(activationFunction);
        return this;
    }

    public double getActivationFunctionParam1() {
        return activationFunctionParam1;
    }

    public MLTrainANNMLP setActivationFunctionParam1(double activationFunctionParam1) {
        this.activationFunctionParam1 = activationFunctionParam1;
        return this;
    }

    public double getActivationFunctionParam2() {
        return activationFunctionParam2;
    }

    public MLTrainANNMLP setActivationFunctionParam2(double activationFunctionParam2) {
        this.activationFunctionParam2 = activationFunctionParam2;
        return this;
    }

    public int[] getHiddenLayerSizes() {
        return hiddenLayerSizes.clone();
    }

    public MLTrainANNMLP setHiddenLayerSizes(int[] hiddenLayerSizes) {
        this.hiddenLayerSizes = nonNull(hiddenLayerSizes).clone();
        return this;
    }

    public MLTrainANNMLP setHiddenLayerSizes(String layerSizes) {
        return setHiddenLayerSizes(new SScalar(nonNull(layerSizes)).toInts());
    }

    public double getBackpropMomentumScale() {
        return backpropMomentumScale;
    }

    public MLTrainANNMLP setBackpropMomentumScale(double backpropMomentumScale) {
        this.backpropMomentumScale = backpropMomentumScale;
        return this;
    }

    public double getBackpropWeightScale() {
        return backpropWeightScale;
    }

    public MLTrainANNMLP setBackpropWeightScale(double backpropWeightScale) {
        this.backpropWeightScale = backpropWeightScale;
        return this;
    }

    public double getRpropDW0() {
        return rpropDW0;
    }

    public MLTrainANNMLP setRpropDW0(double rpropDW0) {
        this.rpropDW0 = rpropDW0;
        return this;
    }

    public double getRpropDWMax() {
        return rpropDWMax;
    }

    public MLTrainANNMLP setRpropDWMax(double rpropDWMax) {
        this.rpropDWMax = rpropDWMax;
        return this;
    }

    public double getRpropDWMin() {
        return rpropDWMin;
    }

    public MLTrainANNMLP setRpropDWMin(double rpropDWMin) {
        this.rpropDWMin = rpropDWMin;
        return this;
    }

    public double getRpropDWMinus() {
        return rpropDWMinus;
    }

    public MLTrainANNMLP setRpropDWMinus(double rpropDWMinus) {
        this.rpropDWMinus = rpropDWMinus;
        return this;
    }

    public double getRpropDWPlus() {
        return rpropDWPlus;
    }

    public MLTrainANNMLP setRpropDWPlus(double rpropDWPlus) {
        this.rpropDWPlus = rpropDWPlus;
        return this;
    }

    public double getAnnealCoolingRatio() {
        return annealCoolingRatio;
    }

    public MLTrainANNMLP setAnnealCoolingRatio(double annealCoolingRatio) {
        this.annealCoolingRatio = annealCoolingRatio;
        return this;
    }

    public double getAnnealFinalT() {
        return annealFinalT;
    }

    public MLTrainANNMLP setAnnealFinalT(double annealFinalT) {
        this.annealFinalT = annealFinalT;
        return this;
    }

    public double getAnnealInitialT() {
        return annealInitialT;
    }

    public MLTrainANNMLP setAnnealInitialT(double annealInitialT) {
        this.annealInitialT = annealInitialT;
        return this;
    }

    public int getAnnealItePerStep() {
        return annealItePerStep;
    }

    public MLTrainANNMLP setAnnealItePerStep(int annealItePerStep) {
        this.annealItePerStep = annealItePerStep;
        return this;
    }

    public Integer getAnnealEnergyRandSeed() {
        return annealEnergyRandSeed;
    }

    public MLTrainANNMLP setAnnealEnergyRandSeed(Integer annealEnergyRandSeed) {
        this.annealEnergyRandSeed = annealEnergyRandSeed;
        return this;
    }

    public boolean isUpdateWeights() {
        return getTrainingFlagByMask(ANN_MLP.UPDATE_WEIGHTS);
    }

    public MLTrainANNMLP setUpdateWeights(boolean updateWeights) {
        setTrainingFlagByMask(ANN_MLP.UPDATE_WEIGHTS, updateWeights);
        return this;
    }

    public boolean isNoInputScale() {
        return getTrainingFlagByMask(ANN_MLP.NO_INPUT_SCALE);
    }

    public MLTrainANNMLP setNoInputScale(boolean noInputScale) {
        setTrainingFlagByMask(ANN_MLP.NO_INPUT_SCALE, noInputScale);
        return this;
    }

    public boolean isNoOutputScale() {
        return getTrainingFlagByMask(ANN_MLP.NO_OUTPUT_SCALE);
    }

    public MLTrainANNMLP setNoOutputScale(boolean noOutputScale) {
        setTrainingFlagByMask(ANN_MLP.NO_OUTPUT_SCALE, noOutputScale);
        return this;
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public MLTrainANNMLP setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
        return this;
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public MLTrainANNMLP setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
        return this;
    }

    public int getLayerIndexToGetWeights() {
        return layerIndexToGetWeights;
    }

    public MLTrainANNMLP setLayerIndexToGetWeights(int layerIndexToGetWeights) {
        this.layerIndexToGetWeights = nonNegative(layerIndexToGetWeights);
        return this;
    }

    @Override
    public void process() {
        try (final ANN_MLP model = newStatModel();
             TermCriteria termCriteria =
                     OTools.termCriteria(terminationMaxCount, terminationEpsilon, true);
             RNG rng = annealEnergyRandSeed != null ? new RNG(annealEnergyRandSeed) : null) {
            model.setTrainMethod(trainingMethod.code(), trainingMethodParam1, trainingMethodParam2);
            model.setBackpropMomentumScale(backpropMomentumScale);
            model.setBackpropWeightScale(backpropWeightScale);
            model.setRpropDW0(rpropDW0);
            model.setRpropDWMax(rpropDWMax);
            model.setRpropDWMin(rpropDWMin);
            model.setRpropDWMinus(rpropDWMinus);
            model.setRpropDWPlus(rpropDWPlus);
            model.setAnnealCoolingRatio(annealCoolingRatio);
            model.setAnnealFinalT(annealFinalT);
            model.setAnnealInitialT(annealInitialT);
            model.setAnnealItePerStep(annealItePerStep);
            if (termCriteria != null) {
                model.setTermCriteria(termCriteria);
            }
            if (rng != null) {
                model.setAnnealEnergyRNG(rng);
            }
            logDebug(() -> "Training ANN_MLP: " + toString(model));
            final MLStatModelTrainer trainer = new MLStatModelTrainer(model, modelKind());
            setTrainingFlags(trainer);
            train(trainer);
            writeTrainer(trainer);
            try (final Mat layerWeights = model.getWeights(layerIndexToGetWeights)) {
                getNumbers(OUTPUT_LAYER_WEIGHTS).exchange(O2SMat.toRawNumbers(layerWeights, 1));
            }
        }
    }

    @Override
    protected void doTrain(MLTrainer trainer, TrainData trainData, int sampleLength, int responseLength) {
        assert trainer instanceof MLStatModelTrainer : "Illegal usage of doTrain method";
        final StatModel model = ((MLStatModelTrainer) trainer).statModel();
        assert model instanceof ANN_MLP : "Illegal usage of doTrain method";
        ANN_MLP ann = (ANN_MLP) model;
        // We can set layer sizes only here, when responses are already converted to multi-column matrix
        // (if convertCategoricalResponses is set)
        try (Mat layerSizesMat = layerSizes(sampleLength, responseLength)) {
            ann.setLayerSizes(layerSizesMat);
            ann.setActivationFunction(activationFunction.code(), activationFunctionParam1, activationFunctionParam2);
            // - should be the last operation in customization
            super.doTrain(trainer, trainData, sampleLength, responseLength);
        }
    }

    public static String toString(ANN_MLP model) {
        return String.format(Locale.US,
                "method=%s, "
                        + "backpropMomentumScale=%s, "
                        + "backpropWeightScale=%s, "
                        + "rpropDW0=%s, "
                        + "rpropDWMax=%s, "
                        + "rpropDWMin=%s, "
                        + "rpropDWMinus=%s, "
                        + "rpropDWPlus=%s, "
                        + "annealCoolingRatio=%s, "
                        + "annealFinalT=%s, "
                        + "annealInitialT=%s, "
                        + "annealItePerStep=%s, "
                        + "%s",
                model.getTrainMethod(),
                model.getBackpropMomentumScale(),
                model.getBackpropWeightScale(),
                model.getRpropDW0(),
                model.getRpropDWMax(),
                model.getRpropDWMin(),
                model.getRpropDWMinus(),
                model.getRpropDWPlus(),
                model.getAnnealCoolingRatio(),
                model.getAnnealFinalT(),
                model.getAnnealInitialT(),
                model.getAnnealItePerStep(),
                OTools.toString(model.getTermCriteria()));
    }

    @Override
    protected MLKind modelKind() {
        return MLKind.StatModelBased.ANN_MLP;
    }

    @Override
    protected boolean categoricalResponses() {
        return false;
    }

    private ANN_MLP newStatModel() {
        final ANN_MLP result = ANN_MLP.create();
        logDebug(() -> "Creating ANN_MLP: " + toString(result));
        return result;
    }

    private Mat layerSizes(int sampleLength, int responseLength) {
        final int[] layerSizes = new int[hiddenLayerSizes.length + 2];
        System.arraycopy(hiddenLayerSizes, 0, layerSizes, 1, hiddenLayerSizes.length);
        layerSizes[0] = sampleLength;
        layerSizes[layerSizes.length - 1] = responseLength;
        final SNumbers result = SNumbers.ofArray(layerSizes, 1);
        return O2SMat.numbersToMulticolumn32BitMat(result, true);
    }

    public static void main(String[] args) {
        ANN_MLP model = ANN_MLP.create();
        final int[] layersSizes = {5};
        final MLTrainANNMLP training = new MLTrainANNMLP(MLSamplesType.NUMBERS);
        training.setHiddenLayerSizes(layersSizes);
        training.setUseGPU(false);
        training.trainNumbers(new MLStatModelTrainer(model, MLKind.StatModelBased.ANN_MLP),
                SNumbers.ofArray(new float[]{1.0f, 1.0f}, 1),
                SNumbers.ofArray(new float[]{1.0f, 1.0f}, 1), null);
        System.out.println("OK 2");
        for (int k = 0; k < layersSizes.length + 2; k++) {
            final Mat weightsMat = model.getWeights(k);
            final SNumbers weights = O2SMat.toRawNumbers(weightsMat, weightsMat.cols());
            System.out.printf("Weights for layer #%d: %dx%d%n%s%n",
                    k, weightsMat.cols(), weightsMat.rows(), weights.toString(true));
        }
    }
}
