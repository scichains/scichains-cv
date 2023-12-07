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

package net.algart.executors.modules.opencv.matrices.features.detection;

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import org.bytedeco.opencv.global.opencv_ximgproc;
import org.bytedeco.opencv.opencv_core.*;

// See https://stackoverflow.com/questions/33317152/model-file-for-opencvs-structured-edge-detector
// how to get new models
public final class StructuredEdgeDetection extends VoidResultUMatFilter {
    private String modelFile = "";

    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = nonNull(modelFile);
    }

    @Override
    public void process(Mat result, Mat source) {
        String modelFile = nonEmpty(this.modelFile, "model file name");
        modelFile = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(modelFile, this).toString();
        Mat mat = source;
        try {
            mat = OTools.to32FIfNot(source);
            try (final org.bytedeco.opencv.opencv_ximgproc.StructuredEdgeDetection detection =
                     opencv_ximgproc.createStructuredEdgeDetection(modelFile))
            {
                detection.detectEdges(mat, result);
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }

    @Override
    public void process(UMat result, UMat source) {
        String modelFile = nonEmpty(this.modelFile, "model file name");
        modelFile = PathPropertyReplacement.translatePropertiesAndCurrentDirectory(modelFile, this).toString();
        UMat mat = source;
        try {
            mat = OTools.to32FIfNot(source);
            try (final org.bytedeco.opencv.opencv_ximgproc.StructuredEdgeDetection detection =
                         opencv_ximgproc.createStructuredEdgeDetection(modelFile))
            {
                detection.detectEdges(mat, result);
            }
        } finally {
            OTools.closeFirstIfDiffersFromSecond(mat, source);
        }
    }
}
