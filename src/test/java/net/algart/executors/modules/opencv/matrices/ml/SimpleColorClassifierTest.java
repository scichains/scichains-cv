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

package net.algart.executors.modules.opencv.matrices.ml;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.UpdatableFloatArray;
import net.algart.arrays.UpdatablePNumberArray;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.opencv.matrices.ml.training.MLTrainSVM;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import org.bytedeco.opencv.global.opencv_ml;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_ml.SVM;
import org.bytedeco.opencv.opencv_ml.StatModel;
import org.bytedeco.opencv.opencv_ml.TrainData;

import java.awt.*;

public final class SimpleColorClassifierTest {
    static class ColorLabel {
        final Color color;
        final int label;

        public ColorLabel(Color color, int label) {
            this.color = color;
            this.label = label;
        }
    }

    private static Mat toSamples(ColorLabel[] colorLabels) {
        final Matrix<UpdatableFloatArray> m = Arrays.SMM.newFloatMatrix(3, colorLabels.length);
        final float[] values = new float[3];
        for (int k = 0, disp = 0; k < colorLabels.length; k++, disp += 3) {
            m.array().setData(disp, colorLabels[k].color.getRGBColorComponents(values));
        }
        return O2SMat.toMat(MultiMatrix.of2DMono(m));
    }

    private static Mat toLabels(ColorLabel[] colorLabels) {
        final Matrix<? extends UpdatablePNumberArray> m = Arrays.SMM.newIntMatrix(1, colorLabels.length);
        for (int k = 0; k < colorLabels.length; k++) {
            m.array().setInt(k, colorLabels[k].label);
        }
        return O2SMat.toMat(MultiMatrix.of2DMono(m));
    }

    private static void printSVM(SVM svm) {
        System.out.println("SVM: " + MLTrainSVM.toString(svm));
    }

    private static void printResponse(Mat mat) {
        final MultiMatrix2D m = O2SMat.toMultiMatrix(mat);
        System.out.printf("%s%n", m);
        SNumbers numbers = SNumbers.ofArray(m.channel(0).toJavaArray(), (int) m.dim(0));
        System.out.println(numbers.toString(true));
    }

    private static StatModel createSVM() {
        final SVM result = SVM.create();
        printSVM(result);
        return result;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.printf("Usage: %s some_file.xml%n", SimpleColorClassifierTest.class.getName());
            return;
        }
        final String modelFile = args[0];
        final StatModel classifier = createSVM();
        ColorLabel[] colorLabels = {
                new ColorLabel(Color.WHITE, 1),
                new ColorLabel(Color.WHITE, 1),
                new ColorLabel(new Color(250, 250, 250), 1),
                new ColorLabel(Color.BLACK, 2),
                new ColorLabel(new Color(1, 1, 1), 2),
                new ColorLabel(Color.YELLOW, 3),
        };
        TrainData trainData = TrainData.create(
                toSamples(colorLabels), opencv_ml.ROW_SAMPLE, toLabels(colorLabels));
        classifier.train(trainData);
        classifier.save(modelFile);

        final SVM loadedClassifier = SVM.load(modelFile);
        colorLabels = new ColorLabel[]{
                new ColorLabel(new Color(255, 200, 255), 0),
                new ColorLabel(new Color(200, 200, 255), 0),
                new ColorLabel(new Color(1, 1, 1), 0),
                new ColorLabel(new Color(255, 200, 255), 0),
                new ColorLabel(new Color(0, 0, 0), 0),
                new ColorLabel(new Color(190, 210 , 1), 0),
        };
        Mat result = new Mat();
        loadedClassifier.predict(toSamples(colorLabels), result, 0);
        printResponse(result);

    }
}
