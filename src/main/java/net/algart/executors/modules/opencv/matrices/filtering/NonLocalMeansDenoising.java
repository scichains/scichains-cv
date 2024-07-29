/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.opencv.matrices.filtering;

import net.algart.executors.modules.opencv.common.VoidResultUMatFilter;
import net.algart.executors.modules.opencv.util.enums.OBorderType;
import org.bytedeco.opencv.global.opencv_photo;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.UMat;

public final class NonLocalMeansDenoising extends VoidResultUMatFilter {
    public enum Algorithm {
        FAST_NL_MEANS,
        FAST_NL_MEANS_COLORED,
        CUDA_NON_LOCAL_MEANS,
        CUDA_FAST_NL_MEANS,
        CUDA_FAST_NL_MEANS_COLORED
    }

    private Algorithm algorithm = Algorithm.CUDA_FAST_NL_MEANS;
    private double h = 3;
    private double hColor = 10;
    private int searchWindowSize = 21;
    private int blockSize = 7;
    private OBorderType borderType = OBorderType.BORDER_DEFAULT;

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = nonNull(algorithm);
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public double gethColor() {
        return hColor;
    }

    public void sethColor(double hColor) {
        this.hColor = hColor;
    }

    public int getSearchWindowSize() {
        return searchWindowSize;
    }

    public void setSearchWindowSize(int searchWindowSize) {
        this.searchWindowSize = positive(searchWindowSize);
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = positive(blockSize);
    }

    public OBorderType getBorderType() {
        return borderType;
    }

    public void setBorderType(OBorderType borderType) {
        this.borderType = nonNull(borderType);
    }

    @Override
    public void process(Mat result, Mat source) {
        switch (algorithm) {
            case FAST_NL_MEANS:
                opencv_photo.fastNlMeansDenoising(
                        source,
                        result,
                        (float) h,
                        blockSize,
                        searchWindowSize);
                break;
            case FAST_NL_MEANS_COLORED:
                opencv_photo.fastNlMeansDenoisingColored(
                        source,
                        result,
                        (float) h,
                        (float) hColor,
                        blockSize,
                        searchWindowSize);
                break;
            case CUDA_NON_LOCAL_MEANS:
                opencv_photo.nonLocalMeans(
                        source,
                        result,
                        (float) h,
                        searchWindowSize,
                        blockSize,
                        borderType.code(),
                        null);
                break;
            case CUDA_FAST_NL_MEANS:
                opencv_photo.fastNlMeansDenoising(
                        source,
                        result,
                        (float) h,
                        searchWindowSize,
                        blockSize,
                        null);
                break;
            case CUDA_FAST_NL_MEANS_COLORED:
                opencv_photo.fastNlMeansDenoisingColored(
                        source,
                        result,
                        (float) h,
                        (float) hColor,
                        searchWindowSize,
                        blockSize,
                        null);
                break;
            default:
                throw new AssertionError("Unknown algorithm");
        }
    }

    @Override
    public void process(UMat result, UMat source) {
        switch (algorithm) {
            case FAST_NL_MEANS:
                opencv_photo.fastNlMeansDenoising(
                        source,
                        result,
                        (float) h,
                        blockSize,
                        searchWindowSize);
                break;
            case FAST_NL_MEANS_COLORED:
                opencv_photo.fastNlMeansDenoisingColored(
                        source,
                        result,
                        (float) h,
                        (float) hColor,
                        blockSize,
                        searchWindowSize);
                break;
            case CUDA_NON_LOCAL_MEANS:
                opencv_photo.nonLocalMeans(
                        source,
                        result,
                        (float) h,
                        searchWindowSize,
                        blockSize,
                        borderType.code(),
                        null);
                break;
            case CUDA_FAST_NL_MEANS:
                opencv_photo.fastNlMeansDenoising(
                        source,
                        result,
                        (float) h,
                        searchWindowSize,
                        blockSize,
                        null);
                break;
            case CUDA_FAST_NL_MEANS_COLORED:
                opencv_photo.fastNlMeansDenoisingColored(
                        source,
                        result,
                        (float) h,
                        (float) hColor,
                        searchWindowSize,
                        blockSize,
                        null);
                break;
            default:
                throw new AssertionError("Unknown algorithm");
        }
    }
}
