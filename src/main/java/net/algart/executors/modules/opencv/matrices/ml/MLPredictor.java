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

import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.*;

public interface MLPredictor extends AutoCloseable {
    // This method is necessary to pass some flags to unknown predictor, loaded from file.
    // There is no need to do the same for training: in this case, we always know what do we train.
    MLPredictor setPredictionFlags(int predictionFlags);

    boolean isClassifier();

    void predict(Mat samples, Mat result);

    default void predict(UMat samples, UMat result) {
        try (Mat samplesMat = OTools.toMat(samples);
             Mat resultMat = new Mat()) {
            predict(samplesMat, resultMat);
            resultMat.copyTo(result);
        }
    }

    @Override
    void close();
}
