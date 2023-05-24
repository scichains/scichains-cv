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

package net.algart.executors.modules.opencv.numbers.misc;

import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.core.common.numbers.SeveralNumbersOperation;
import net.algart.executors.modules.core.numbers.misc.ValuesDistanceMetric;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.TermCriteria;
import org.bytedeco.opencv.opencv_core.UMat;

import java.util.Arrays;
import java.util.List;

public final class KMeans extends SeveralNumbersOperation implements ReadOnlyExecutionInput {
    public static final String INPUT_LABELS = "labels";
    public static final String OUTPUT_LABELS = "labels";
    public static final String OUTPUT_CENTERS = "centers";
    public static final String OUTPUT_DISTANCES = "distances";

    public enum CentersMode {
        KMEANS_RANDOM_CENTERS(opencv_core.KMEANS_RANDOM_CENTERS),
        KMEANS_PP_CENTERS(opencv_core.KMEANS_PP_CENTERS);

        private final int kmeansFlag;

        CentersMode(int kmeansFlag) {
            this.kmeansFlag = kmeansFlag;
        }
    }

    private int numberOfClusters = 1;
    private CentersMode centersMode = CentersMode.KMEANS_PP_CENTERS;
    private int attempts = 3;
    private int terminationMaxCount = 0;
    private double terminationEpsilon = 0.1;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public KMeans() {
        super(DEFAULT_INPUT_PORT, INPUT_LABELS);
        setDefaultOutputNumbers(OUTPUT_LABELS);
        addOutputNumbers(OUTPUT_CENTERS);
        addOutputNumbers(OUTPUT_DISTANCES);
    }

    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    public KMeans setNumberOfClusters(int numberOfClusters) {
        this.numberOfClusters = nonNegative(numberOfClusters);
        return this;
    }

    public CentersMode getCentersMode() {
        return centersMode;
    }

    public void setCentersMode(CentersMode centersMode) {
        this.centersMode = nonNull(centersMode);
    }

    public int getAttempts() {
        return attempts;
    }

    public KMeans setAttempts(int attempts) {
        this.attempts = nonNegative(attempts);
        return this;
    }

    public int getTerminationMaxCount() {
        return terminationMaxCount;
    }

    public KMeans setTerminationMaxCount(int terminationMaxCount) {
        this.terminationMaxCount = nonNegative(terminationMaxCount);
        return this;
    }

    public double getTerminationEpsilon() {
        return terminationEpsilon;
    }

    public KMeans setTerminationEpsilon(double terminationEpsilon) {
        this.terminationEpsilon = nonNegative(terminationEpsilon);
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public KMeans setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public SNumbers processNumbers(
            List<SNumbers> sources,
            SNumbers resultCenters,
            SNumbers resultDistancesToCenters) {
        return OTools.isGPUOptimizationEnabled() ?
                processNumbersUMat(sources, resultCenters, resultDistancesToCenters) :
                processNumbersMat(sources, resultCenters, resultDistancesToCenters);
    }

    @Override
    protected SNumbers processNumbers(List<SNumbers> sources) {
        return processNumbers(
                sources,
                getNumbers(OUTPUT_CENTERS),
                getNumbers(OUTPUT_DISTANCES));
    }

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex >= 1;
    }

    @Override
    protected boolean blockLengthEqualityRequired() {
        return false;
    }

    private SNumbers processNumbersMat(
            List<SNumbers> sources,
            SNumbers resultCenters,
            SNumbers resultDistancesToCenters) {
        final SNumbers data = sources.get(0);
        final int dimCount = data.getBlockLength();
        final Mat dataMat = O2SMat.numbersToMulticolumn32BitMat(data, false);
        final SNumbers labels = sources.get(1);
        final Mat labelsMat;
        if (labels == null) {
            labelsMat = new Mat();
        } else {
            labelsMat = O2SMat.numbersToMulticolumn32BitMat(labels, true);
            if (indexingBase.start != 0) {
                try (Scalar startIndex = new Scalar((double) indexingBase.start)) {
                    opencv_core.subtract(labelsMat, startIndex).asMat().copyTo(labelsMat);
                }
            }
        }
        try (TermCriteria termCriteria =
                     OTools.termCriteria(terminationMaxCount, terminationEpsilon, false)) {
            final int flags = centersMode.kmeansFlag |
                    (labels == null ? 0 : opencv_core.KMEANS_USE_INITIAL_LABELS);
            Mat centersMat = new Mat();
            logDebug(() -> "KMeans array segmentation: K = " + numberOfClusters
                    + ", attempts = " + attempts
                    + ", maxCount = " + termCriteria.maxCount()
                    + ", epsilon = " + termCriteria.epsilon()
                    + ", flags = " + flags);
            opencv_core.kmeans(dataMat, numberOfClusters, labelsMat, termCriteria, attempts, flags, centersMat);
            final SNumbers resultLabels = O2SMat.multicolumnMatToNumbers(labelsMat);
            if (resultLabels.getBlockLength() != 1) {
                throw new AssertionError("Strange kmeans behaviour: it returned matrix with "
                        + resultLabels.getBlockLength() + "!=1 columns");
            }
            final int numberOfLabels = resultLabels.n();
            if (data.n() != numberOfLabels) {
                throw new AssertionError("Strange kmeans behaviour: different number of labels "
                        + numberOfLabels + " and data " + data.n());
            }
            final SNumbers centers = O2SMat.multicolumnMatToNumbers(centersMat);
            if (resultCenters != null) {
                resultCenters.setTo(centers);
            }
            if (resultDistancesToCenters != null) {
                if (centers.getBlockLength() != dimCount) {
                    throw new AssertionError("Strange kmeans behaviour: different number of dimensions "
                            + "in source data " + dimCount + " and cluster centers " + centers.getBlockLength());
                }
                final int numberOfCenters = centers.n();
                final float[] distances = new float[data.n()];
                final double[] weights = new double[dimCount];
                Arrays.fill(weights, 1.0);
                final double[] a = new double[dimCount];
                final double[] b = new double[dimCount];
                for (int k = 0; k < distances.length; k++) {
                    int label = (int) resultLabels.getValue(k);
                    if (label < 0 || label >= numberOfCenters) {
                        throw new AssertionError("Strange kmeans behaviour: it returned label " + label
                                + ", which is out of range 0..number_of_result_clusters-1=" + (numberOfCenters - 1));
                    }
                    data.getBlockDoubleValues(k, a);
                    centers.getBlockDoubleValues(label, b);
                    distances[k] = (float) ValuesDistanceMetric.EUCLIDEAN.distance(a, b, weights);
                }
                resultDistancesToCenters.setTo(distances, 1);
            }
            if (indexingBase.start != 0) {
                for (int k = 0; k < numberOfLabels; k++) {
                    resultLabels.setValue(k, resultLabels.getValue(k) + indexingBase.start);
                }
            }
            return resultLabels;
        }
    }

    public SNumbers processNumbersUMat(
            List<SNumbers> sources,
            SNumbers resultCenters,
            SNumbers resultDistancesToCenters) {
        final SNumbers data = sources.get(0);
        final int dimCount = data.getBlockLength();
        final UMat dataUMat = OTools.toUMat(O2SMat.numbersToMulticolumn32BitMat(data, false));
        final SNumbers labels = sources.get(1);
        final UMat labelsUMat;
        if (labels == null) {
            labelsUMat = new UMat();
        } else {
            try (Mat labelsMat = O2SMat.numbersToMulticolumn32BitMat(labels, true)) {
                if (indexingBase.start != 0) {
                    try (Scalar startIndex = new Scalar((double) indexingBase.start)) {
                        opencv_core.subtract(labelsMat, startIndex).asMat().copyTo(labelsMat);
                    }
                }
                labelsUMat = OTools.toUMat(labelsMat);
            }
        }
        UMat centersUMat = null;
        try {
            try (TermCriteria termCriteria =
                         OTools.termCriteria(terminationMaxCount, terminationEpsilon, false)) {
                final int flags = centersMode.kmeansFlag |
                        (labels == null ? 0 : opencv_core.KMEANS_USE_INITIAL_LABELS);
                centersUMat = new UMat();
                logDebug(() -> "KMeans array segmentation (GPU): K = " + numberOfClusters
                        + ", attempts = " + attempts
                        + ", maxCount = " + termCriteria.maxCount()
                        + ", epsilon = " + termCriteria.epsilon()
                        + ", flags = " + flags);
                opencv_core.kmeans(dataUMat, numberOfClusters, labelsUMat, termCriteria, attempts, flags, centersUMat);
                final SNumbers resultLabels = O2SMat.multicolumnMatToNumbers(labelsUMat);
                if (resultLabels.getBlockLength() != 1) {
                    throw new AssertionError("Strange kmeans behaviour: it returned matrix with "
                            + resultLabels.getBlockLength() + "!=1 columns");
                }
                final int numberOfLabels = resultLabels.n();
                if (data.n() != numberOfLabels) {
                    throw new AssertionError("Strange kmeans behaviour: different number of labels "
                            + numberOfLabels + " and data " + data.n());
                }
                final SNumbers centers = O2SMat.multicolumnMatToNumbers(centersUMat);
                if (resultCenters != null) {
                    resultCenters.setTo(centers);
                }
                if (resultDistancesToCenters != null) {
                    if (centers.getBlockLength() != dimCount) {
                        throw new AssertionError("Strange kmeans behaviour: different number of dimensions "
                                + "in source data " + dimCount + " and cluster centers " + centers.getBlockLength());
                    }
                    final int numberOfCenters = centers.n();
                    final float[] distances = new float[data.n()];
                    final double[] weights = new double[dimCount];
                    Arrays.fill(weights, 1.0);
                    final double[] a = new double[dimCount];
                    final double[] b = new double[dimCount];
                    for (int k = 0; k < distances.length; k++) {
                        int label = (int) resultLabels.getValue(k);
                        if (label < 0 || label >= numberOfCenters) {
                            throw new AssertionError("Strange kmeans behaviour: it returned label " + label
                                    + ", which is out of range 0..number_of_result_clusters-1=" + (numberOfCenters - 1));
                        }
                        data.getBlockDoubleValues(k, a);
                        centers.getBlockDoubleValues(label, b);
                        distances[k] = (float) ValuesDistanceMetric.EUCLIDEAN.distance(a, b, weights);
                    }
                    resultDistancesToCenters.setTo(distances, 1);
                }
                if (indexingBase.start != 0) {
                    for (int k = 0; k < numberOfLabels; k++) {
                        resultLabels.setValue(k, resultLabels.getValue(k) + indexingBase.start);
                    }
                }
                return resultLabels;
            }
        } finally {
            if (centersUMat != null) {
                centersUMat.close();
            }
            labelsUMat.close();
        }
    }
}
