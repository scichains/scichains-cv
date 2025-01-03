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

package net.algart.executors.modules.opencv.matrices.arithmetic;

import net.algart.executors.modules.opencv.common.VoidResultTwoUMatFilter;
import net.algart.executors.modules.opencv.util.enums.ODepthOrUnchanged;
import net.algart.executors.api.ReadOnlyExecutionInput;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;

public final class Multiply extends VoidResultTwoUMatFilter implements ReadOnlyExecutionInput {
    private ODepthOrUnchanged resultDepth = ODepthOrUnchanged.UNCHANGED;
    private double scale = 1.0;

    public Multiply() {
        super("x", "y");
    }

    public ODepthOrUnchanged getResultDepth() {
        return resultDepth;
    }

    public Multiply setResultDepth(ODepthOrUnchanged resultDepth) {
        this.resultDepth = nonNull(resultDepth);
        return this;
    }

    public double getScale() {
        return scale;
    }

    public Multiply setScale(double scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public void process(Mat result, Mat source, Mat secondMat) {
        opencv_core.multiply(source, secondMat, result, scale, resultDepth.code());
    }

    @Override
    public void process(UMat result, UMat source, UMat secondMat) {
        opencv_core.multiply(source, secondMat, result, scale, resultDepth.code());
    }
}
