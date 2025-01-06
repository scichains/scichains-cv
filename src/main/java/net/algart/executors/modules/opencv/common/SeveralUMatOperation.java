/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.opencv_core.UMat;

import java.util.ArrayList;
import java.util.List;

public abstract class SeveralUMatOperation extends SeveralMatOperation {
    @Override
    public void process() {
        if (!useGPU()) {
            super.process();
            return;
        }
        final Integer requiredNumberOfInputs = requiredNumberOfInputs();
        final List<UMat> sourceMats = new ArrayList<>();
        for (int k = 0; requiredNumberOfInputs == null || k < requiredNumberOfInputs; k++) {
            final String portName = inputPortName(k);
            if (requiredNumberOfInputs == null && !hasInputPort(portName)) {
                break;
            }
            sourceMats.add(getInputUMat(k));
        }
        if (dimensionsEqualityRequired()) {
            OTools.checkDimensionOfNonNullUMatEquality(sourceMats);
        }
        setStartProcessingTimeStamp();
        final UMat target = process(sourceMats.toArray(new UMat[0]));
        setEndProcessingTimeStamp();
        setOutputTo(target);
    }

    public UMat getInputUMat(int inputIndex) {
        final SMat inputMat = getInputMat(inputPortName(inputIndex), allowUninitializedInput(inputIndex));
        return inputMat.isInitialized() ? O2SMat.toUMat(inputMat, allowInputPackedBits(inputIndex)) : null;
    }

    public void setOutputTo(UMat result) {
        if (packOutputBits()) {
            getMat().setTo(O2SMat.toBinaryMatrix(result));
        } else {
            O2SMat.setTo(getMat(), result);
        }
    }

    public abstract UMat process(UMat[] sources);

}
