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

import net.algart.executors.modules.opencv.matrices.ml.*;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_ml.ParamGrid;
import org.bytedeco.opencv.opencv_ml.SVM;
import org.bytedeco.opencv.opencv_ml.StatModel;
import org.bytedeco.opencv.opencv_ml.TrainData;

import java.util.Locale;

public final class MLTrainSVM extends AbstractMLTrain {
    public static final String OUTPUT_C = "c";
    public static final String OUTPUT_GAMMA = "gamma";
    public static final String OUTPUT_P = "p";
    public static final String OUTPUT_NU = "nu";
    public static final String OUTPUT_COEF = "coef";
    public static final String OUTPUT_DEGREE = "degree";

    public enum SVMType {
        C_SVC(SVM.C_SVC), // - default
        NU_SVC(SVM.NU_SVC),
        ONE_CLASS(SVM.ONE_CLASS),
        EPS_SVR(SVM.EPS_SVR),
        NU_SVR(SVM.NU_SVR);

        private final int code;

        public int code() {
            return code;
        }

        SVMType(int code) {
            this.code = code;
        }
    }

    public enum KernelType {
        LINEAR(SVM.LINEAR),
        POLY(SVM.POLY),
        RBF(SVM.RBF), // - default
        SIGMOID(SVM.SIGMOID),
        CHI2(SVM.CHI2),
        INTER(SVM.INTER);

        private final int code;

        public int code() {
            return code;
        }

        KernelType(int code) {
            this.code = code;
        }
    }

    private SVMType svmType = SVMType.C_SVC;
    private KernelType kernelType = KernelType.RBF;
    private double c = 1.0;
    private double gamma = 1.0;
    private double p = 0.0;
    private double nu = 0.0;
    private double coef = 0.0;
    private double degree = 0.0;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.0;
    private boolean autoTraining = false;
    private int kFold = 10;
    private boolean cGridCustom = false;
    private double cGridMin = 0.0;
    private double cGridMax = 1.0;
    private double cGridLogStep = 1.0;
    private boolean gammaGridCustom = false;
    private double gammaGridMin = 0.0;
    private double gammaGridMax = 1.0;
    private double gammaGridLogStep = 1.0;
    private boolean pGridCustom = false;
    private double pGridMin = 0.0;
    private double pGridMax = 1.0;
    private double pGridLogStep = 1.0;
    private boolean nuGridCustom = false;
    private double nuGridMin = 0.0;
    private double nuGridMax = 1.0;
    private double nuGridLogStep = 1.0;
    private boolean coefGridCustom = false;
    private double coefGridMin = 0.0;
    private double coefGridMax = 1.0;
    private double coefGridLogStep = 1.0;
    private boolean degreeGridCustom = false;
    private double degreeGridMin = 0.0;
    private double degreeGridMax = 1.0;
    private double degreeGridLogStep = 1.0;
    private boolean balanced = false;


    private MLTrainSVM(MLSamplesType inputType) {
        super(inputType);
        addOutputScalar(OUTPUT_C);
        addOutputScalar(OUTPUT_GAMMA);
        addOutputScalar(OUTPUT_P);
        addOutputScalar(OUTPUT_NU);
        addOutputScalar(OUTPUT_COEF);
        addOutputScalar(OUTPUT_DEGREE);
    }

    public static MLTrainSVM newTrainNumbers() {
        return new MLTrainSVM(MLSamplesType.NUMBERS);
    }

    public static MLTrainSVM newTrainPixels() {
        return new MLTrainSVM(MLSamplesType.PIXELS);
    }

    public SVMType getSvmType() {
        return svmType;
    }

    public MLTrainSVM setSvmType(SVMType svmType) {
        this.svmType = nonNull(svmType);
        return this;
    }

    public KernelType getKernelType() {
        return kernelType;
    }

    public MLTrainSVM setKernelType(KernelType kernelType) {
        this.kernelType = nonNull(kernelType);
        return this;
    }

    public double getC() {
        return c;
    }

    public MLTrainSVM setC(double c) {
        this.c = c;
        return this;
    }

    public double getGamma() {
        return gamma;
    }

    public MLTrainSVM setGamma(double gamma) {
        this.gamma = gamma;
        return this;
    }

    public double getP() {
        return p;
    }

    public MLTrainSVM setP(double p) {
        this.p = p;
        return this;
    }

    public double getNu() {
        return nu;
    }

    public MLTrainSVM setNu(double nu) {
        this.nu = nu;
        return this;
    }

    public double getCoef() {
        return coef;
    }

    public MLTrainSVM setCoef(double coef) {
        this.coef = coef;
        return this;
    }

    public double getDegree() {
        return degree;
    }

    public MLTrainSVM setDegree(double degree) {
        this.degree = degree;
        return this;
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public MLTrainSVM setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
        return this;
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public MLTrainSVM setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
        return this;
    }

    public boolean isAutoTraining() {
        return autoTraining;
    }

    public MLTrainSVM setAutoTraining(boolean autoTraining) {
        this.autoTraining = autoTraining;
        return this;
    }

    public int getKFold() {
        return kFold;
    }

    public MLTrainSVM setKFold(int kFold) {
        this.kFold = nonNegative(kFold);
        return this;
    }

    public boolean isCGridCustom() {
        return cGridCustom;
    }

    public MLTrainSVM setCGridCustom(boolean cGridCustom) {
        this.cGridCustom = cGridCustom;
        return this;
    }

    public double getCGridMin() {
        return cGridMin;
    }

    public MLTrainSVM setCGridMin(double cGridMin) {
        this.cGridMin = cGridMin;
        return this;
    }

    public double getCGridMax() {
        return cGridMax;
    }

    public MLTrainSVM setCGridMax(double cGridMax) {
        this.cGridMax = cGridMax;
        return this;
    }

    public double getCGridLogStep() {
        return cGridLogStep;
    }

    public MLTrainSVM setCGridLogStep(double cGridLogStep) {
        this.cGridLogStep = positive(cGridLogStep);
        return this;
    }

    public boolean isGammaGridCustom() {
        return gammaGridCustom;
    }

    public MLTrainSVM setGammaGridCustom(boolean gammaGridCustom) {
        this.gammaGridCustom = gammaGridCustom;
        return this;
    }

    public double getGammaGridMin() {
        return gammaGridMin;
    }

    public MLTrainSVM setGammaGridMin(double gammaGridMin) {
        this.gammaGridMin = gammaGridMin;
        return this;
    }

    public double getGammaGridMax() {
        return gammaGridMax;
    }

    public MLTrainSVM setGammaGridMax(double gammaGridMax) {
        this.gammaGridMax = gammaGridMax;
        return this;
    }

    public double getGammaGridLogStep() {
        return gammaGridLogStep;
    }

    public MLTrainSVM setGammaGridLogStep(double gammaGridLogStep) {
        this.gammaGridLogStep = positive(gammaGridLogStep);
        return this;
    }

    public boolean isPGridCustom() {
        return pGridCustom;
    }

    public MLTrainSVM setPGridCustom(boolean pGridCustom) {
        this.pGridCustom = pGridCustom;
        return this;
    }

    public double getPGridMin() {
        return pGridMin;
    }

    public MLTrainSVM setPGridMin(double pGridMin) {
        this.pGridMin = pGridMin;
        return this;
    }

    public double getPGridMax() {
        return pGridMax;
    }

    public MLTrainSVM setPGridMax(double pGridMax) {
        this.pGridMax = pGridMax;
        return this;
    }

    public double getPGridLogStep() {
        return pGridLogStep;
    }

    public MLTrainSVM setPGridLogStep(double pGridLogStep) {
        this.pGridLogStep = nonNegative(pGridLogStep);
        return this;
    }

    public boolean isNuGridCustom() {
        return nuGridCustom;
    }

    public MLTrainSVM setNuGridCustom(boolean nuGridCustom) {
        this.nuGridCustom = nuGridCustom;
        return this;
    }

    public double getNuGridMin() {
        return nuGridMin;
    }

    public MLTrainSVM setNuGridMin(double nuGridMin) {
        this.nuGridMin = nuGridMin;
        return this;
    }

    public double getNuGridMax() {
        return nuGridMax;
    }

    public MLTrainSVM setNuGridMax(double nuGridMax) {
        this.nuGridMax = nuGridMax;
        return this;
    }

    public double getNuGridLogStep() {
        return nuGridLogStep;
    }

    public MLTrainSVM setNuGridLogStep(double nuGridLogStep) {
        this.nuGridLogStep = nonNegative(nuGridLogStep);
        return this;
    }

    public boolean isCoefGridCustom() {
        return coefGridCustom;
    }

    public MLTrainSVM setCoefGridCustom(boolean coefGridCustom) {
        this.coefGridCustom = coefGridCustom;
        return this;
    }

    public double getCoefGridMin() {
        return coefGridMin;
    }

    public MLTrainSVM setCoefGridMin(double coefGridMin) {
        this.coefGridMin = coefGridMin;
        return this;
    }

    public double getCoefGridMax() {
        return coefGridMax;
    }

    public MLTrainSVM setCoefGridMax(double coefGridMax) {
        this.coefGridMax = coefGridMax;
        return this;
    }

    public double getCoefGridLogStep() {
        return coefGridLogStep;
    }

    public MLTrainSVM setCoefGridLogStep(double coefGridLogStep) {
        this.coefGridLogStep = nonNegative(coefGridLogStep);
        return this;
    }

    public boolean isDegreeGridCustom() {
        return degreeGridCustom;
    }

    public MLTrainSVM setDegreeGridCustom(boolean degreeGridCustom) {
        this.degreeGridCustom = degreeGridCustom;
        return this;
    }

    public double getDegreeGridMin() {
        return degreeGridMin;
    }

    public MLTrainSVM setDegreeGridMin(double degreeGridMin) {
        this.degreeGridMin = degreeGridMin;
        return this;
    }

    public double getDegreeGridMax() {
        return degreeGridMax;
    }

    public MLTrainSVM setDegreeGridMax(double degreeGridMax) {
        this.degreeGridMax = degreeGridMax;
        return this;
    }

    public double getDegreeGridLogStep() {
        return degreeGridLogStep;
    }

    public MLTrainSVM setDegreeGridLogStep(double degreeGridLogStep) {
        this.degreeGridLogStep = nonNegative(degreeGridLogStep);
        return this;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public MLTrainSVM setBalanced(boolean balanced) {
        this.balanced = balanced;
        return this;
    }

    @Override
    public void process() {
        try (final SVM model = newStatModel();
             TermCriteria termCriteria =
                     OTools.termCriteria(terminationMaxCount, terminationEpsilon, true)) {
            model.setType(svmType.code());
            model.setKernel(kernelType.code());
            model.setC(c);
            model.setGamma(gamma);
            model.setP(p);
            model.setNu(nu);
            model.setCoef0(coef);
            model.setDegree(degree);
            if (termCriteria != null) {
                model.setTermCriteria(termCriteria);
            }
            logDebug(() -> "Training SVM: " + toString(model));
            final MLStatModelTrainer trainer = new MLStatModelTrainer(model, modelKind());
            setTrainingFlags(trainer);
            train(trainer);
            writeTrainer(trainer);
        }
    }

    public static String toString(SVM model) {
        return String.format(Locale.US,
                "type=%s, kernel=%s, c=%s, gamma=%s, p=%s, nu=%s, coef=%s, degree=%s, %s",
                model.getType(), model.getKernelType(),
                model.getC(), model.getGamma(), model.getP(), model.getNu(), model.getCoef0(), model.getDegree(),
                OTools.toString(model.getTermCriteria()));
    }

    @Override
    protected MLKind modelKind() {
        return MLKind.StatModelBased.SVM;
    }

    @Override
    protected boolean categoricalResponses() {
        return true;
    }

    @Override
    protected void doTrain(MLTrainer trainer, TrainData trainData, int sampleLength, int responseLength) {
        assert trainer instanceof MLStatModelTrainer : "Illegal usage of doTrain method";
        final StatModel model = ((MLStatModelTrainer) trainer).statModel();
        assert model instanceof SVM : "Illegal usage of doTrain method";
        final SVM svm = (SVM) model;
        if (autoTraining) {
            try (final ParamGrid cGrid =
                         gridOrNull(cGridCustom, cGridMin, cGridMax, cGridLogStep);
                 final ParamGrid gammaGrid =
                         gridOrNull(gammaGridCustom, gammaGridMin, gammaGridMax, gammaGridLogStep);
                 final ParamGrid pGrid =
                         gridOrNull(pGridCustom, pGridMin, pGridMax, pGridLogStep);
                 final ParamGrid nuGrid =
                         gridOrNull(nuGridCustom, nuGridMin, nuGridMax, nuGridLogStep);
                 final ParamGrid coefGrid =
                         gridOrNull(coefGridCustom, coefGridMin, coefGridMax, coefGridLogStep);
                 final ParamGrid degreeGrid =
                         gridOrNull(degreeGridCustom, degreeGridMin, degreeGridMax, degreeGridLogStep)) {
                logDebug(() -> String.format(
                        "Auto-training SVM: %s%n  kFold=%d%n  %s%n  %s%n  %s%n  %s%n  %s%n  %s%n  balanced=%s",
                        toString(svm),
                        kFold,
                        gridToString("cGrid", cGrid, svm, SVM.C),
                        gridToString("gammaGrid", gammaGrid, svm, SVM.GAMMA),
                        gridToString("pGrid", pGrid, svm, SVM.P),
                        gridToString("nuGrid", nuGrid, svm, SVM.NU),
                        gridToString("coefGrid", coefGrid, svm, SVM.COEF),
                        gridToString("degreeGrid", degreeGrid, svm, SVM.DEGREE),
                        balanced));
                svm.trainAuto(trainData, kFold, cGrid, gammaGrid, pGrid, nuGrid, coefGrid, degreeGrid, balanced);
                logDebug(() -> String.format("Auto-training SVM result: %s", toString(svm)));
            }
        } else {
            super.doTrain(trainer, trainData, sampleLength, responseLength);
        }
        getScalar(OUTPUT_C).setTo(svm.getC());
        getScalar(OUTPUT_GAMMA).setTo(svm.getGamma());
        getScalar(OUTPUT_P).setTo(svm.getP());
        getScalar(OUTPUT_NU).setTo(svm.getNu());
        getScalar(OUTPUT_COEF).setTo(svm.getCoef0());
        getScalar(OUTPUT_DEGREE).setTo(svm.getDegree());
    }

    private SVM newStatModel() {
        final SVM result = SVM.create();
        logDebug(() -> "Creating SVM: " + toString(result));
        return result;
    }

    private static ParamGrid gridOrNull(boolean custom, double min, double max, double logStep) {
        return custom ? new ParamGrid(min, max, logStep) : null;
    }

    private static String gridToString(String name, ParamGrid grid, SVM svm, int paramId) {
        final boolean custom = grid != null;
        try {
            if (!custom) {
                grid = SVM.getDefaultGrid(paramId);
                if (grid == null) {
                    return "No default grid?"; // - strange situation
                }
            }
            return String.format(Locale.US, "%s: %s min=%.5f, max=%.5f, logStep=%.3f",
                    name, custom ? "(custom)" : "(default)", grid.minVal(), grid.maxVal(), grid.logStep());
        } finally {
            if (!custom && grid != null) {
                grid.close();
            }
        }
    }
}
