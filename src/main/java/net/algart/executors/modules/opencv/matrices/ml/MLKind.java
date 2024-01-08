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


import org.bytedeco.opencv.opencv_ml.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface MLKind {
    String modelName();

    MLPredictor loadPredictor(Path modelFile) throws IOException;

    enum StatModelBased implements MLKind {
        ANN_MLP("ANN_MLP", org.bytedeco.opencv.opencv_ml.ANN_MLP::load),
        BOOST("Boost", Boost::load),
        R_TREES("RTrees", RTrees::load),
        NORMAL_BAYES_CLASSIFIER("NormalBayesClassifier ", NormalBayesClassifier::load),
        SVM("SVM", org.bytedeco.opencv.opencv_ml.SVM::load),
        SVM_SGD("SVM_SGD", SVMSGD::load);

        private final String modelName;
        private final Function<String, StatModel> openCVLoader;
        // - may be null; then loadPredictor should be overridden

        StatModelBased(String modelName, Function<String, StatModel> openCVLoader) {
            this.modelName = modelName;
            this.openCVLoader = openCVLoader;
        }

        @Override
        public String modelName() {
            return modelName;
        }

        @Override
        public MLPredictor loadPredictor(Path modelFile) throws IOException {
            return MLStatModelTrainer.loadOpenCVTrainer(modelFile, openCVLoader, this);
        }

        @Override
        public String toString() {
            return modelName;
        }

        public static Optional<MLKind> valueOfModelName(String modelName) {
            Objects.requireNonNull(modelName, "Null modelName");
            for (StatModelBased kind : values()) {
                if (kind.modelName.equals(modelName)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }
}
