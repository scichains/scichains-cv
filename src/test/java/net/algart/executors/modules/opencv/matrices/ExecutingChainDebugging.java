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

package net.algart.executors.modules.opencv.matrices;

import net.algart.executors.api.ExecutionBlock;
import net.algart.executors.api.chains.Chain;
import net.algart.executors.api.chains.ChainBlock;
import net.algart.executors.api.chains.ChainInputPort;
import net.algart.executors.api.chains.ChainSpecification;
import net.algart.executors.api.data.DataType;
import net.algart.executors.api.data.SMat;
import net.algart.executors.api.system.ExecutorFactory;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ExecutingChainDebugging {
    private static final String SESSION_ID = "~~DUMMY_SESSION";
    private static final boolean readUMat = false; // - if false, useOpenCL is unstable

    public static void main(String[] args) throws IOException {
        final Path chainFile = Paths.get(args[0]);
        final String sourceFile = args[1];
        final int numberOfTests = 150;

        final var executorProvider = ExecutorFactory.newDefaultInstance(SESSION_ID);
        var chainJson = ChainSpecification.read(chainFile);
        System.out.printf("Reading %s%n", chainFile);
        @SuppressWarnings("resource")
        Chain chain = Chain.of(null, executorProvider, chainJson);
        chain.setMultithreading(true);
        chain.setExecuteAll(true);
        chain.reinitializeAll();
        final SMat sourceMat = readUMat ?
                O2SMat.toSMat(OTools.toUMat(opencv_imgcodecs.imread(sourceFile))) :
                O2SMat.toSMat(opencv_imgcodecs.imread(sourceFile));
        System.out.printf("Reading source image %s%n", sourceFile);

        for (int test = 1; test <= numberOfTests; test++) {
            System.out.printf("%nTest #%d/%d...%n", test, numberOfTests);
            for (ChainBlock block : chain.getAllInputs()) {
                final ChainInputPort inputPort = block.reqStandardInputPort();
                final String executorPortName = block.getStandardInputOutputName();
                if (ExecutionBlock.DEFAULT_INPUT_PORT.equalsIgnoreCase(executorPortName)
                        && inputPort.getDataType() == DataType.MAT) {
                    inputPort.getData().setTo(sourceMat, true);
                    System.out.printf("Default input block initialized by the source image %s%n", sourceMat);
                    break;
                }
            }
            chain.execute();
            System.out.printf("Executed %d/%d blocks%n", chain.numberOfReadyBlocks(), chain.numberOfBlocks());
            chain.freeData();
            final boolean haveOpenCL = opencv_core.haveOpenCL();
            final boolean useOpenCL = opencv_core.useOpenCL();
            System.out.println((useOpenCL != haveOpenCL ? "!!! STRANGE SITUATION! " : "")
                    + "haveOpenCL: " + haveOpenCL + "; useOpenCL: " + useOpenCL);
        }
    }
}
