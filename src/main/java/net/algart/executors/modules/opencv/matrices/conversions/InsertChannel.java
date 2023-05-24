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

package net.algart.executors.modules.opencv.matrices.conversions;

import net.algart.executors.modules.opencv.common.SeveralUMatOperation;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;

public final class InsertChannel extends SeveralUMatOperation {
    public static final String INPUT_CHANNEL = "channel";

    private int channelIndex = 0;

    public InsertChannel() {
        addInputMat(INPUT_CHANNEL);
    }

    public int getChannelIndex() {
        return channelIndex;
    }

    public void setChannelIndex(int channelIndex) {
        this.channelIndex = nonNegative(channelIndex);
    }

    @Override
    public UMat process(UMat[] sources) {
        opencv_core.insertChannel(sources[1], sources[0], channelIndex);
        return sources[0];
    }

    @Override
    public Mat process(Mat[] sources) {
        opencv_core.insertChannel(sources[1], sources[0], channelIndex);
        return sources[0];
    }

    @Override
    protected String inputPortName(int inputIndex) {
        switch (inputIndex) {
            case 0:
                return DEFAULT_INPUT_PORT;
            case 1:
                return INPUT_CHANNEL;
        }
        throw new AssertionError("requiredNumberOfInputs() method returns invalid number of ports");
    }
}
