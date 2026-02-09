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

package net.algart.executors.modules.core.matrices.io.helpers;

import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SMat;
import net.algart.executors.modules.core.matrices.io.MatReader;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.io.UnsupportedImageFormatException;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.file.Path;

public class DefaultMatReaderHelper implements MatReader {
    public void readMat(SMat result, Path path) throws UnsupportedImageFormatException {
        final String file = path.toAbsolutePath().toString();
        Executor.LOG.log(System.Logger.Level.DEBUG, () -> "Java image I/O failed; reading " + file + " by OpenCV");
        final Mat mat = opencv_imgcodecs.imread(file);
        if (mat == null || mat.data() == null) {
            throw new UnsupportedImageFormatException("OpenCV does not recognize " + file);
        }
        O2SMat.setTo(result, mat);
    }
}
