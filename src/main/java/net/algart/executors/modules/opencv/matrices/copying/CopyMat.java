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

package net.algart.executors.modules.opencv.matrices.copying;

import net.algart.executors.modules.opencv.util.ConvertibleUMat;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class CopyMat extends Executor implements ReadOnlyExecutionInput {
    public enum ResultType {
        MAT,
        UMAT,
        UMAT_FOR_UMAT_INPUT
    }

    private ResultType resultType = ResultType.UMAT_FOR_UMAT_INPUT;
    private boolean cloneData = false;

    public ResultType getResultType() {
        return resultType;
    }

    public CopyMat setResultType(ResultType resultType) {
        this.resultType = nonNull(resultType);
        return this;
    }

    public boolean isCloneData() {
        return cloneData;
    }

    public CopyMat setCloneData(boolean cloneData) {
        this.cloneData = cloneData;
        return this;
    }

    public CopyMat() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    @Override
    public void process() {
        final SMat input = getInputMat();
        boolean copyToUMat = resultType == ResultType.UMAT
                || (resultType == ResultType.UMAT_FOR_UMAT_INPUT && input.getPointer() instanceof ConvertibleUMat);
        if (copyToUMat) {
            logDebug(() -> (cloneData ? "Cloning" : "Copying") + " to OpenCV UMat (GPU): " + input);
            final UMat uMat = O2SMat.toUMat(input, true);
            assert uMat != null : "input must be initialized!";
            O2SMat.setTo(getMat(), cloneData ? uMat.clone() : uMat);
            // - clone() to be sure that the data are copied into GPU memory (UMat may be placed not there)
        } else {
            logDebug(() -> (cloneData ? "Cloning" : "Copying") + " to to OpenCV Mat: " + input);
            final Mat mat = O2SMat.toMat(input, true);
            assert mat != null : "input must be initialized!";
            O2SMat.setTo(getMat(), cloneData ? mat.clone() : mat);
        }
    }

    @Override
    public boolean isReadOnly() {
        return cloneData;
        // If we don't clone data, we cannot be sure, that this data will be not modified after executing this block.
    }
}
