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

package net.algart.executors.modules.util.opencv;

import net.algart.executors.api.data.SMat;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConvertibleMat extends SMat.Convertible {
    private volatile Mat mat;
    private AtomicBoolean disposed = new AtomicBoolean(false);

    public ConvertibleMat(Mat mat) {
        this.mat = Objects.requireNonNull(mat, "Null mat");
    }

    public Mat mat() {
        checkMat();
        return mat;
    }

    @Override
    public SMat.Convertible copy() {
        checkMat();
//        System.out.println("!!! Cloning " + mat);
        return new ConvertibleMat(mat.clone());
    }

    @Override
    public SMat.Convertible copyToMemoryAndDisposePrevious() {
        return this;
    }

    @Override
    public ByteBuffer toByteBuffer(SMat thisMatrix) {
        checkMat();
        return OTools.toByteBuffer(mat);
    }

    @Override
    public void dispose() {
        if (disposed.getAndSet(true)) {
            return;
        }
        // Note: we still have no guarantees that mat is not de-allocated already (!mat.isNull()).
        // For example, we call this method both for input and output ports of the same executor,
        // and it can create another ConvertibleMat for the output, but actually process the matrix
        // in-place and use the same Mat instance for input and output.
        mat.close();
        mat = null;
    }

    @Override
    public String toString() {
        return "reference to " + mat;
    }

    private void checkMat() {
        if (disposed.get()) {
            throw new IllegalStateException("mat is already deallocated");
        }
    }
}
