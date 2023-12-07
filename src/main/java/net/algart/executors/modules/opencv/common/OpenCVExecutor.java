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

package net.algart.executors.modules.opencv.common;

import net.algart.executors.modules.opencv.util.OTools;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.api.Executor;
import org.bytedeco.opencv.global.opencv_core;

import java.lang.ref.Cleaner;

public abstract class OpenCVExecutor extends Executor {
    static {
        if (SHOW_INFO_ON_STARTUP) {
            System.out.println();
            System.out.printf("OpenCV version %s%n", OTools.openCVVersion());
            System.out.printf("OpenCV GPU optimization: %s%n%n",
                    OTools.isGPUOptimizationEnabled() ? "enabled"
                            : opencv_core.haveOpenCL() ? "disabled"
                            : "unavailable");
        }
    }

    private static final Cleaner CLEANER = Cleaner.create();

    private final FinalizationKeeper finalizationKeeper;

    protected OpenCVExecutor() {
        this.finalizationKeeper = new FinalizationKeeper(this.toString());
        CLEANER.register(this, finalizationKeeper);
    }

    private boolean useGPU = true;

    public boolean useGPU() {
        return OTools.isGPUOptimizationEnabled() && useGPU;
    }

    public boolean isUseGPU() {
        return useGPU;
    }

    public void setUseGPU(boolean useGPU) {
        this.useGPU = useGPU;
    }

    // This additional method helps in interaction between MultiMatrix-oriented and OpenCV executors
    public final void setMat(String name, MultiMatrix mat) {
        getInputMatContainer(name).setTo(mat);
    }

    public final void putMat(String name, MultiMatrix mat) {
        addInputMat(name);
        setMat(name, mat);
    }

    @Override
    public void postprocess() {
        super.postprocess();
//
        // opencv_core.finish();
        // - Unfortunately, this solution is not enough in current OpenCV 4.0.0:
        // we must use memory serialization in ConvertibleUMat.copyToMemoryAndDisposePrevious instead


//        if (false) { // - see MultithreadingUseOpenCLCallsTest
        //
//            final boolean haveOpenCL = opencv_core.haveOpenCL();
//            final boolean useOpenCL = opencv_core.useOpenCL();
//            LOG.log(useOpenCL == haveOpenCL ? Level.CONFIG : Level.WARNING,
//                    (useOpenCL != haveOpenCL ? "STRANGE SITUATION! " : "")
//                    + "haveOpenCL: " + haveOpenCL + "; useOpenCL: " + useOpenCL + " - " + getClass());
//        }
    }

    @Override
    public void close() {
        super.close();
        finalizationKeeper.normallyClosed = true;
    }

    private static class FinalizationKeeper implements Runnable {
        private final String name;
        private boolean normallyClosed;

        private FinalizationKeeper(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            if (!normallyClosed) {
                LOG.log(System.Logger.Level.ERROR,
                        "CLEANING WARNING! OpenCV-based executor was not closed: " + name);
                // - warning, but not actual closing: maybe, someone actually uses output ports yet
                // (a correct way if their content is stored in usual Java memory)
            }
        }
    }
}
