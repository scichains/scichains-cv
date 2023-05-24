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

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_ml.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

public class MLStatModelTrainer implements MLTrainer {
    private final StatModel statModel;
    private final MLKind statModelKind;

    private int predictionFlags = 0;
    private int trainingFlags = 0;

    public MLStatModelTrainer(StatModel statModel, MLKind statModelKind) {
        this.statModel = Objects.requireNonNull(statModel, "Null statModel");
        this.statModelKind = Objects.requireNonNull(statModelKind, "Null statModelKind");
    }

    public StatModel statModel() {
        return statModel;
    }

    public MLKind modelKine() {
        return statModelKind;
    }

    public int getPredictionFlags() {
        return predictionFlags;
    }

    @Override
    public MLStatModelTrainer setPredictionFlags(int predictionFlags) {
        this.predictionFlags = predictionFlags;
        return this;
    }

    public int getTrainingFlags() {
        return trainingFlags;
    }

    public MLStatModelTrainer setTrainingFlags(int trainingFlags) {
        this.trainingFlags = trainingFlags;
        return this;
    }

    @Override
    public boolean isClassifier() {
        return statModel.isClassifier();
    }

    @Override
    public void predict(Mat samples, Mat result) {
        statModel.predict(samples, result, predictionFlags);
    }

    @Override
    public void predict(UMat samples, UMat result) {
        statModel.predict(samples, result, predictionFlags);
    }

    @Override
    public void train(TrainData trainData) {
        statModel.train(trainData, trainingFlags);
    }

    @Override
    public double calculateError(TrainData trainData, Mat result) {
        return statModel.calcError(trainData, false, result);
    }

    @Override
    public double calculateError(TrainData trainData, UMat result) {
        return statModel.calcError(trainData, false, result);
    }

    // Note: if you need to use statModel in future, you just need not to call close() method.
    @Override
    public void close() {
        statModel.close();
    }

    @Override
    public void save(Path file) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(statModel, "Null statModel");
        if (Files.isDirectory(file)) {
            // - this check is important to avoid system crash in OpenCV
            throw new IOException("Result statistic model file cannot be an existing directory: " + file);
        }
        final Path parent = file.getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            // - this check is important to avoid system crash in OpenCV
            throw new IOException("Result statistic model file cannot be saved inside non-existing directory: "
                    + parent);
        }
        statModel.save(file.toAbsolutePath().toString());
    }

    @Override
    public String toString() {
        return "MLStatModelTrainer for " + statModelKind + ", statModel " + statModel;
    }

    public static MLTrainer loadOpenCVTrainer(
            Path file,
            Function<String, StatModel> loader,
            MLKind statModelKind)
            throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(loader, "Null loader");
        Objects.requireNonNull(statModelKind, "Null statModelKind");
        if (!Files.isRegularFile(file)) {
            // - this check is important to avoid system crash in OpenCV
            throw new FileNotFoundException("Statistic model file does not exist or is not a regular file: " + file);
        }
        final StatModel statModel = loader.apply(file.toAbsolutePath().toString());
        return new MLStatModelTrainer(statModel, statModelKind);
    }
}
