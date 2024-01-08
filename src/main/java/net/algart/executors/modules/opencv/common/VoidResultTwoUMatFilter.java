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

import org.bytedeco.opencv.opencv_core.*;

import java.util.Objects;

public abstract class VoidResultTwoUMatFilter extends SeveralUMatOperation {
    private final String firstInputPort;
    private final String secondInputPort;

    protected VoidResultTwoUMatFilter() {
        this(INPUT_PORT_PREFIX + "1", INPUT_PORT_PREFIX + "2");
    }

    protected VoidResultTwoUMatFilter(String firstInputPort, String secondInputPort) {
        this.firstInputPort = firstInputPort;
        this.secondInputPort = Objects.requireNonNull(secondInputPort,"Null second port name");
        addInputMat(firstInputPort == null ? DEFAULT_INPUT_PORT : firstInputPort);
        addInputMat(secondInputPort);
    }

    @Override
    public Mat process(Mat[] sources) {
        Mat source = sources[0];
        Mat secondMat = sources[1] != null ? sources[1] : source;
        Mat result = new Mat();
//        System.out.println("MMM " + getClass() + " !" + opencv_core.useOpenCL() + "! created " + result);
        process(result, source, secondMat);
        return result;
    }

    @Override
    public UMat process(UMat[] sources) {
        UMat source = sources[0];
        UMat secondMat = sources[1] != null ? sources[1] : source;
        UMat result = new UMat();
        process(result, source, secondMat);
        return result;
    }

    public abstract void process(Mat result, Mat source, Mat secondMat);

    public abstract void process(UMat result, UMat source, UMat secondMat);

    @Override
    protected boolean allowUninitializedInput(int inputIndex) {
        return false;
    }

    @Override
    protected String inputPortName(int inputIndex) {
        switch (inputIndex) {
            case 0:
                return firstInputPort == null ? defaultInputPortName() : firstInputPort;
            case 1:
                return secondInputPort;
        }
        throw new AssertionError("requiredNumberOfInputs() method returns invalid number of ports");
    }
}
