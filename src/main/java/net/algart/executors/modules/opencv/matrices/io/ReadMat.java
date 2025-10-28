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

package net.algart.executors.modules.opencv.matrices.io;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.common.io.FileOperation;
import net.algart.executors.modules.opencv.util.O2SMat;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;

public final class ReadMat extends FileOperation implements ReadOnlyExecutionInput {
    private boolean relativizePath = false;

    public ReadMat() {
        addFileOperationPorts();
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public static ReadMat getSecureInstance() {
        final ReadMat result = new ReadMat();
        result.setSecure(true);
        return result;
    }

    public boolean isRelativizePath() {
        return relativizePath;
    }

    public ReadMat setRelativizePath(boolean relativizePath) {
        this.relativizePath = relativizePath;
        return this;
    }

    @Override
    public ReadMat setFile(String file) {
        super.setFile(file);
        return this;
    }

    @Override
    public void process() {
        SMat input = getInputMat(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying " + input);
            getMat().setTo(input);
        } else {
            readMat(getMat());
        }
    }

    public SMat readMat() {
        return readMat(new SMat());
    }

    public SMat readMat(SMat result) {
        final Path path = completeOSFilePath(relativizePath);
        if (skipNonExistingFile(path)) {
            result.remove();
        } else {
            final String fileName = path.toString();
            logDebug(() -> "Reading OpenCV matrix from " + fileName);
            final Mat mat = opencv_imgcodecs.imread(fileName);
            if (mat == null || mat.data() == null) {
                throw new IOError(new IOException("Cannot read " + fileName));
            }
            O2SMat.setTo(result, mat);
        }
        return result;
    }
}
