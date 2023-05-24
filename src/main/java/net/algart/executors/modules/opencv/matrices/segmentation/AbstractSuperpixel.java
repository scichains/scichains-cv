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

package net.algart.executors.modules.opencv.matrices.segmentation;

import net.algart.executors.modules.core.common.numbers.IndexingBase;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.UMat;

abstract class AbstractSuperpixel extends AbstractSegmentationWithBoundaries {
    public static final String OUTPUT_LABELS = "labels";

    public AbstractSuperpixel() {
        setDefaultOutputMat(OUTPUT_LABELS);
        setUseGPU(false);
        // - currently OpenCV super-pixels do not support UMat versions
    }

    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public void setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
    }

    void makeLabelsPositiveIfRequired(Mat labels) {
        if (indexingBase.start != 0) {
            try (Scalar startIndex = new Scalar((double) indexingBase.start)) {
                opencv_core.add(labels, startIndex).asMat().copyTo(labels);
            }
        }
    }

    void makeLabelsPositiveIfRequired(UMat labels) {
        if (indexingBase.start != 0) {
            try (Scalar startIndex = new Scalar((double) indexingBase.start)) {
                try (UMat startIndexUMat =
                             new UMat(labels.rows(), labels.cols(), labels.type(), startIndex)) {
                    opencv_core.add(labels, startIndexUMat, labels);
                }
            }
        }
    }
}
