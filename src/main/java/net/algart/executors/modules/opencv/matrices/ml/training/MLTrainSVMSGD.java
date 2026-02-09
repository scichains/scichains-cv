/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.modules.opencv.matrices.ml.AbstractMLTrain;
import net.algart.executors.modules.opencv.matrices.ml.MLKind;
import net.algart.executors.modules.opencv.matrices.ml.MLSamplesType;
import net.algart.executors.modules.opencv.matrices.ml.MLStatModelTrainer;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_ml.SVMSGD;

import java.util.Locale;

public final class MLTrainSVMSGD extends AbstractMLTrain {
    public enum SVMSGDType {
        SGD(SVMSGD.SGD), // - default
        ASGD(SVMSGD.ASGD);

        private final int code;

        public int code() {
            return code;
        }

        SVMSGDType(int code) {
            this.code = code;
        }
    }

    public enum MarginType {
        SOFT_MARGIN(SVMSGD.SOFT_MARGIN),
        HARD_MARGIN(SVMSGD.HARD_MARGIN);

        private final int code;

        public int code() {
            return code;
        }

        MarginType(int code) {
            this.code = code;
        }
    }

    private SVMSGDType svmSgdType = SVMSGDType.ASGD;
    private MarginType marginType = MarginType.SOFT_MARGIN;
    private boolean optimalParameters = true;
    private double marginRegularization = 0.00001;
    private double initialStepSize = 0.05;
    private double stepDecreasingPower = 0.75;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.0;

    private MLTrainSVMSGD(MLSamplesType inputType) {
        super(inputType);
    }

    public static MLTrainSVMSGD newTrainNumbers() {
        return new MLTrainSVMSGD(MLSamplesType.NUMBERS);
    }

    public static MLTrainSVMSGD newTrainPixels() {
        return new MLTrainSVMSGD(MLSamplesType.PIXELS);
    }

    public SVMSGDType getSvmSgdType() {
        return svmSgdType;
    }

    public MLTrainSVMSGD setSvmSgdType(SVMSGDType svmSgdType) {
        this.svmSgdType = nonNull(svmSgdType);
        return this;
    }

    public MarginType getMarginType() {
        return marginType;
    }

    public MLTrainSVMSGD setMarginType(MarginType marginType) {
        this.marginType = nonNull(marginType);
        return this;
    }

    public boolean isOptimalParameters() {
        return optimalParameters;
    }

    public MLTrainSVMSGD setOptimalParameters(boolean optimalParameters) {
        this.optimalParameters = optimalParameters;
        return this;
    }

    public double getMarginRegularization() {
        return marginRegularization;
    }

    public MLTrainSVMSGD setMarginRegularization(double marginRegularization) {
        this.marginRegularization = marginRegularization;
        return this;
    }

    public double getInitialStepSize() {
        return initialStepSize;
    }

    public MLTrainSVMSGD setInitialStepSize(double initialStepSize) {
        this.initialStepSize = initialStepSize;
        return this;
    }

    public double getStepDecreasingPower() {
        return stepDecreasingPower;
    }

    public MLTrainSVMSGD setStepDecreasingPower(double stepDecreasingPower) {
        this.stepDecreasingPower = stepDecreasingPower;
        return this;
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public MLTrainSVMSGD setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
        return this;
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public MLTrainSVMSGD setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
        return this;
    }

    @Override
    public void process() {
        try (final SVMSGD model = newStatModel();
             TermCriteria termCriteria =
                     OTools.termCriteria(terminationMaxCount, terminationEpsilon, true)) {
            if (optimalParameters) {
                model.setOptimalParameters(svmSgdType.code(), marginType.code());
            } else {
                model.setSvmsgdType(svmSgdType.code());
                model.setMarginType(marginType.code());
                model.setMarginRegularization((float) marginRegularization);
                model.setInitialStepSize((float) initialStepSize);
                model.setStepDecreasingPower((float) stepDecreasingPower);
                if (termCriteria != null) {
                    model.setTermCriteria(termCriteria);
                }
            }
            logDebug(() -> "Training SVMSGD: " + toString(model));
            final MLStatModelTrainer trainer = new MLStatModelTrainer(model, modelKind());
            setTrainingFlags(trainer);
            train(trainer);
            writeTrainer(trainer);
        }
    }

    public static String toString(SVMSGD model) {
        return String.format(Locale.US,
                "type=%s, margin=%s, marginRegularization=%s, initialStepSize=%s,"
                        + "stepDecreasingPower=%s, %s; result shift=%s",
                model.getSvmsgdType(), model.getMarginType(),
                model.getMarginRegularization(), model.getInitialStepSize(),
                model.getStepDecreasingPower(),
                OTools.toString(model.getTermCriteria()),
                model.getShift());
    }

    @Override
    protected MLKind modelKind() {
        return MLKind.StatModelBased.SVM_SGD;
    }

    @Override
    protected boolean categoricalResponses() {
        return true;
    }

    private SVMSGD newStatModel() {
        final SVMSGD result = SVMSGD.create();
        logDebug(() -> "Creating SVMSGD: " + toString(result));
        return result;
    }
}
