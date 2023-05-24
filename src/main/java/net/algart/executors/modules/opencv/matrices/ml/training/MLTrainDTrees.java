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

package net.algart.executors.modules.opencv.matrices.ml.training;

import net.algart.executors.modules.opencv.matrices.ml.AbstractMLTrain;
import net.algart.executors.modules.opencv.matrices.ml.MLSamplesType;
import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.api.data.SScalar;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_ml.DTrees;

import java.util.Locale;

abstract class MLTrainDTrees extends AbstractMLTrain {
    private int cvFolds = 10;
    private int maxCategories = 10;
    private Integer maxDepth = null;
    private int minSampleCount = 10;
    private float[] priors = {};
    private double regressionAccuracy = 0.01;
    private boolean truncatePrunedTree = true;
    private boolean use1SERule = true;
    private boolean useSurrogates = false;

    MLTrainDTrees(MLSamplesType inputType) {
        super(inputType);
    }

    public int getCvFolds() {
        return cvFolds;
    }

    public MLTrainDTrees setCvFolds(int cvFolds) {
        this.cvFolds = cvFolds;
        return this;
    }

    public int getMaxCategories() {
        return maxCategories;
    }

    public MLTrainDTrees setMaxCategories(int maxCategories) {
        this.maxCategories = maxCategories;
        return this;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public MLTrainDTrees setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public int getMinSampleCount() {
        return minSampleCount;
    }

    public MLTrainDTrees setMinSampleCount(int minSampleCount) {
        this.minSampleCount = minSampleCount;
        return this;
    }

    public float[] getPriors() {
        return priors.clone();
    }

    public MLTrainDTrees setPriors(float[] priors) {
        this.priors = nonNull(priors).clone();
        return this;
    }

    public MLTrainDTrees setPriors(String priors) {
        final double[] values = new SScalar(nonNull(priors)).toDoubles();
        this.priors = new float[values.length];
        for (int k = 0; k < values.length; k++) {
            this.priors[k] = (float) values[k];
        }
        return this;
    }

    public double getRegressionAccuracy() {
        return regressionAccuracy;
    }

    public MLTrainDTrees setRegressionAccuracy(double regressionAccuracy) {
        this.regressionAccuracy = regressionAccuracy;
        return this;
    }

    public boolean isTruncatePrunedTree() {
        return truncatePrunedTree;
    }

    public MLTrainDTrees setTruncatePrunedTree(boolean truncatePrunedTree) {
        this.truncatePrunedTree = truncatePrunedTree;
        return this;
    }

    public boolean isUse1SERule() {
        return use1SERule;
    }

    public MLTrainDTrees setUse1SERule(boolean use1SERule) {
        this.use1SERule = use1SERule;
        return this;
    }

    public boolean isUseSurrogates() {
        return useSurrogates;
    }

    public MLTrainDTrees setUseSurrogates(boolean useSurrogates) {
        this.useSurrogates = useSurrogates;
        return this;
    }

    void customizeDTrees(DTrees model, Mat priors) {
        model.setCVFolds(cvFolds);
        model.setMaxCategories(maxCategories);
        if (maxDepth != null) {
            model.setMaxDepth(maxDepth);
        }
        model.setMinSampleCount(minSampleCount);
        if (priors != null) {
            model.setPriors(priors);
        }
        model.setRegressionAccuracy((float) regressionAccuracy);
        model.setTruncatePrunedTree(truncatePrunedTree);
        model.setUse1SERule(use1SERule);
        model.setUseSurrogates(useSurrogates);
    }

    void customizeRTrees(DTrees model, Mat priors) {
        model.setMaxCategories(maxCategories);
        if (maxDepth != null) {
            model.setMaxDepth(maxDepth);
        }
        model.setMinSampleCount(minSampleCount);
        if (priors != null) {
            model.setPriors(priors);
        }
        // - other parameters are not actual for RTrees
    }

    Mat priors() {
        if (priors.length == 0) {
            return null;
        }
        final SNumbers result = SNumbers.valueOfArray(priors, 1);
        return O2SMat.numbersToMulticolumn32BitMat(result, false);
    }

    public static String toString(DTrees model) {
        return String.format(Locale.US,
                "cvFolds=%s, "
                        + "maxCategories=%s, "
                        + "maxDepth=%s, "
                        + "minSampleCount=%s, "
                        + "regressionAccuracy=%s, "
                        + "truncatePrunedTree=%s, "
                        + "use1SERule=%s, "
                        + "useSurrogates=%s",
                model.getCVFolds(),
                model.getMaxCategories(),
                model.getMaxDepth(),
                model.getMinSampleCount(),
                //TODO!! priors
                model.getRegressionAccuracy(),
                model.getTruncatePrunedTree(),
                model.getUse1SERule(),
                model.getUseSurrogates());
    }
}
