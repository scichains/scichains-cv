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
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.data.SNumbers;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_ml.RTrees;

import java.util.Locale;

public final class MLTrainRTrees extends MLTrainDTrees {
    public static final String OUTPUT_VAR_IMPORTANCE = "var_importance";

    private int activeVarCount = 0;
    private boolean calculateVarImportance = false;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.0;

    private MLTrainRTrees(MLSamplesType inputType) {
        super(inputType);
        addOutputNumbers(OUTPUT_VAR_IMPORTANCE);
    }

    public static MLTrainRTrees newTrainNumbers() {
        return new MLTrainRTrees(MLSamplesType.NUMBERS);
    }

    public static MLTrainRTrees newTrainPixels() {
        return new MLTrainRTrees(MLSamplesType.PIXELS);
    }

    public int getActiveVarCount() {
        return activeVarCount;
    }

    public MLTrainRTrees setActiveVarCount(int activeVarCount) {
        this.activeVarCount = activeVarCount;
        return this;
    }

    public boolean isCalculateVarImportance() {
        return calculateVarImportance;
    }

    public MLTrainRTrees setCalculateVarImportance(boolean calculateVarImportance) {
        this.calculateVarImportance = calculateVarImportance;
        return this;
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public MLTrainRTrees setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
        return this;
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public MLTrainRTrees setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
        return this;
    }

    @Override
    public void process() {
        try (final RTrees model = newStatModel();
             final Mat priors = priors();
             TermCriteria termCriteria =
                     OTools.termCriteria(terminationMaxCount, terminationEpsilon, true)) {
            model.setActiveVarCount(activeVarCount);
            model.setCalculateVarImportance(calculateVarImportance);
            if (termCriteria != null) {
                model.setTermCriteria(termCriteria);
            }
            // - should be the last operation in customization
            customizeRTrees(model, priors);
            logDebug(() -> "Training " + modelKind().modelName() + ": " + toString(model));
            final MLStatModelTrainer trainer = new MLStatModelTrainer(model, modelKind());
            setTrainingFlags(trainer);
            train(trainer);
            writeTrainer(trainer);
            if (calculateVarImportance) {
                try (final Mat varImportance = model.getVarImportance()) {
                    getNumbers(OUTPUT_VAR_IMPORTANCE).exchange(O2SMat.toRawNumbers(
                            varImportance, varImportance.cols() * varImportance.rows()));
                }
            }
        }
    }

    public static String toString(RTrees model) {
        return String.format(Locale.US,
                "%s, "
                        + "activeVarCount=%s, "
                        + "calculateVarImportance=%s, "
                        + "%s",
                MLTrainDTrees.toString(model),
                model.getActiveVarCount(),
                model.getCalculateVarImportance(),
                OTools.toString(model.getTermCriteria()));
    }

    @Override
    protected MLKind modelKind() {
        return MLKind.StatModelBased.R_TREES;
    }

    @Override
    protected boolean categoricalResponses() {
        return true;
    }

    private RTrees newStatModel() {
        final RTrees result = RTrees.create();
        logDebug(() -> "Creating RTrees: " + toString(result));
        return result;
    }

    public static void main(String[] args) {
        RTrees model = RTrees.create();
        SNumbers priors = SNumbers.valueOfArray(new double[]{2.0f, 1.0f}, 1);
        model.setPriors(O2SMat.numbersToMulticolumnMat(priors));
        model.setCalculateVarImportance(true);
        @SuppressWarnings("resource")
        final MLTrainRTrees training = new MLTrainRTrees(MLSamplesType.NUMBERS);
        training.setUseGPU(false);
        training.trainNumbers(new MLStatModelTrainer(model, MLKind.StatModelBased.R_TREES),
                SNumbers.valueOfArray(new float[]{10.0f, 30.0f}, 1),
                SNumbers.valueOfArray(new int[]{2, 3}, 1), null);
        System.out.println("OK: " + model.isClassifier());
        final Mat varImportanceMat = model.getVarImportance();
        System.out.println(OTools.toString(varImportanceMat));
        System.out.println("varImportance: " + O2SMat.multicolumnMatToNumbers(varImportanceMat)
                .toString(true));
        Mat priorsMat = model.getPriors();
        System.out.println(OTools.toString(priorsMat));
        System.out.println("priors: " + O2SMat.multicolumnMatToNumbers(priorsMat).toString(true));

        Mat samplesMat = O2SMat.numbersToMulticolumn32BitMat(
                SNumbers.valueOfArray(new float[]{10.0f, 30.0f}, 1), false);
        Mat resultMat = new Mat();
        model.predict(samplesMat, resultMat, RTrees.PREDICT_MAX_VOTE);
        // - access violation!
        System.out.println("prediction: " + OTools.toString(resultMat));
    }
}
