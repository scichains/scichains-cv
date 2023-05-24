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

package net.algart.executors.modules.opencv.matrices;

import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.modules.util.opencv.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_ml;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_ml.RTrees;

public final class RTreesBug {
    public static void main(String[] args) {
        RTrees model = RTrees.create();
        model.setCalculateVarImportance(true);
        Mat samplesMat = new Mat(10, 1, opencv_core.CV_32F,
                new Scalar(0.0));
        Mat responsesMat = new Mat(10, 1, opencv_core.CV_32F,
                new Scalar(1.0));
        model.train(samplesMat, opencv_ml.ROW_SAMPLE, responsesMat);
        System.out.println("OK");

        final Mat varImportanceMat = model.getVarImportance();
        System.out.println(OTools.toString(varImportanceMat));
        System.out.println("varImportance: " + O2SMat.multicolumnMatToNumbers(varImportanceMat).toString(true));

        Mat resultMat = new Mat();
        model.predict(samplesMat, resultMat, RTrees.PREDICT_MAX_VOTE);
        // - access violation!
        System.out.println("prediction: " + OTools.toString(resultMat));
    }
}
