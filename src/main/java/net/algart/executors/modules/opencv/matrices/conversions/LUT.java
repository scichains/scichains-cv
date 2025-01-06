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

package net.algart.executors.modules.opencv.matrices.conversions;

import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.cv.matrices.objects.TableTranslate;
import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class LUT extends VoidResultUMatFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_TABLE = "table";
    public static final String OUTPUT_LABELS = "labels";

    private boolean castTo8U = false;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;

    public LUT() {
        setDefaultInputMat(INPUT_LABELS);
        addInputNumbers(INPUT_TABLE);
        setDefaultOutputMat(OUTPUT_LABELS);
    }

    public boolean isCastTo8U() {
        return castTo8U;
    }

    public LUT setCastTo8U(boolean castTo8U) {
        this.castTo8U = castTo8U;
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public LUT setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    @Override
    public void process() {
        final SNumbers lut = getInputNumbers(INPUT_TABLE).requireBlockLengthOne("table");
        if (lut.n() > 256 - indexingBase.start) {
            final TableTranslate.ResultElementType resultElementType =
                    lut.isFloatingPoint() ? TableTranslate.ResultElementType.FLOAT :
                            TableTranslate.ResultElementType.INT;
            final TableTranslate tableTranslate = new TableTranslate()
                    .setIndexingBase(IndexingBase.ZERO_BASED)
                    .setResultElementType(resultElementType);
            tableTranslate.putMat(TableTranslate.INPUT_LABELS, getInputMat(INPUT_LABELS));
            tableTranslate.putNumbers(TableTranslate.INPUT_TABLE, lut);
            setStartProcessingTimeStamp();
            tableTranslate.process();
            setEndProcessingTimeStamp();
            getMat(OUTPUT_LABELS).exchange(tableTranslate.getMat(TableTranslate.OUTPUT_LABELS));
            return;
        }
        super.process();
    }

    @Override
    public void process(Mat result, Mat source) {
        process(result, source, getInputNumbers(INPUT_TABLE).requireBlockLengthOne("table"));
    }

    @Override
    public void process(UMat result, UMat source) {
        process(result, source, getInputNumbers(INPUT_TABLE).requireBlockLengthOne("table"));
    }

    public void process(Mat result, Mat source, SNumbers translationTable) {
        try (Mat lut = O2SMat.numbersToMulticolumnMat(toLut256(translationTable))) {
            Mat mat = source;
            try {
                if (castTo8U && mat.depth() != opencv_core.CV_8U && mat.depth() != opencv_core.CV_8S) {
                    mat = new Mat();
                    source.convertTo(mat, opencv_core.CV_8U);
                    // - note: alpha=1 (raw cast), unlike OTools.to8UIfNot
                }
                opencv_core.LUT(mat, lut, result);
            } finally {
                OTools.closeFirstIfDiffersFromSecond(mat, source);
            }
        }
    }

    public void process(UMat result, UMat source, SNumbers translationTable) {
        try (UMat lut = O2SMat.numbersToMulticolumnUMat(toLut256(translationTable))) {
            UMat mat = source;
            try {
                if (castTo8U && mat.depth() != opencv_core.CV_8U) {
                    mat = new UMat();
                    source.convertTo(mat, opencv_core.CV_8U);
                    // - note: alpha=1 (raw cast), unlike OTools.to8UIfNot
                }
                opencv_core.LUT(mat, lut, result);
            } finally {
                OTools.closeFirstIfDiffersFromSecond(mat, source);
            }
        }
    }

    private SNumbers toLut256(SNumbers lut) {
        final SNumbers result = SNumbers.zeros(lut.elementType(), 256, 1);
        final int n = lut.n();
        for (int k = 0; k < 256; k++) {
            int index = k - indexingBase.start;
            if (index < 0 || index >= n) {
                result.setValue(k, k);
            } else {
                result.setValue(k, lut.getValue(index, 0));
            }
        }
        return result;
    }
}
