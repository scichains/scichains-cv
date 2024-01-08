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

package net.algart.executors.modules.opencv.common;

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.api.data.SMat;
import org.bytedeco.opencv.opencv_core.*;

import java.util.ArrayList;
import java.util.List;

public abstract class SeveralMatOperation extends OpenCVExecutor {
    public static final int DEFAULT_REQUIRED_NUMBER_OF_INPUTS = 2;
    public static final String INPUT_PORT_PREFIX = "input_";

    protected SeveralMatOperation() {
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        final List<Mat> sourceMats = new ArrayList<>();
        for (int k = 0; requiredNumberOfInputs == null || k < requiredNumberOfInputs; k++) {
            final String portName = inputPortName(k);
            if (requiredNumberOfInputs == null && !hasInputPort(portName)) {
                break;
            }
            sourceMats.add(getInputMat(k));
        }
        if (dimensionsEqualityRequired()) {
            OTools.checkDimensionOfNonNullMatEquality(sourceMats);
        }
        setStartProcessingTimeStamp();
        final Mat target = process(sourceMats.toArray(new Mat[0]));
        setEndProcessingTimeStamp();
        setOutputTo(target);
    }

    public Mat getInputMat(int inputIndex) {
        final SMat inputMat = getInputMat(inputPortName(inputIndex), allowUninitializedInput(inputIndex));
        return inputMat.isInitialized() ? O2SMat.toMat(inputMat, allowInputPackedBits(inputIndex)) : null;
    }

    public void setOutputTo(Mat result) {
        if (packOutputBits()) {
            getMat().setTo(O2SMat.toBinaryMatrix(result));
        } else {
            O2SMat.setTo(getMat(), result);
        }
    }

    public abstract Mat process(Mat[] sources);

    protected Integer requiredNumberOfInputs() {
        return DEFAULT_REQUIRED_NUMBER_OF_INPUTS;
    }

    // May be overridden
    protected boolean allowUninitializedInput(int inputIndex) {
        return true;
    }

    // May be overridden
    protected String inputPortName(int inputIndex) {
        return INPUT_PORT_PREFIX + (inputIndex + 1);
    }

    protected boolean allowInputPackedBits(int inputIndex) {
        return false;
    }

    // Should be overridden to process arguments of different sizes
    protected boolean dimensionsEqualityRequired() {
        return true;
    }

    protected boolean packOutputBits() {
        return false;
    }
}
