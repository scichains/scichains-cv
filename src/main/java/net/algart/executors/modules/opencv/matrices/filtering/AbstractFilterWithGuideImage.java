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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.modules.opencv.common.VoidResultTwoUMatFilter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

abstract class AbstractFilterWithGuideImage extends VoidResultTwoUMatFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_GUIDE = "guide";

    public AbstractFilterWithGuideImage() {
        super(null, INPUT_GUIDE);
    }

    @Override
    public abstract void process(Mat result, Mat source, Mat guide);

    @Override
    public abstract void process(UMat result, UMat source, UMat guide);

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return inputIndex == 1;
    }

    @Override
    protected String inputPortName(int inputIndex) {
        switch (inputIndex) {
            case 0:
                return defaultInputPortName();
            case 1:
                return INPUT_GUIDE;
        }
        throw new AssertionError("requiredNumberOfInputs() method returns invalid number of ports");
    }
}
