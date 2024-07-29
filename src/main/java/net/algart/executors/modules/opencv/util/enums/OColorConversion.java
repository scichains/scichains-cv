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

package net.algart.executors.modules.opencv.util.enums;

import org.bytedeco.opencv.global.opencv_imgproc;

public enum OColorConversion {
    NONE(-1),

    /**
     * add alpha channel to RGB or BGR image
     */
    BGR2BGRA(opencv_imgproc.COLOR_BGR2BGRA),

    /**
     * remove alpha channel from RGB or BGR image
     */
    BGRA2BGR(opencv_imgproc.COLOR_BGRA2BGR),

    /**
     * convert between RGB and BGR color spaces (with or without alpha channel)
     */
    BGR2RGB_OR_RGB2BGR(opencv_imgproc.COLOR_BGR2RGB),
    BGRA2RGBA_OR_RGBA2BGRA(opencv_imgproc.COLOR_BGRA2RGBA),
    BGR2RGBA(opencv_imgproc.COLOR_BGR2RGBA),
    RGBA2BGR(opencv_imgproc.COLOR_RGBA2BGR),

    /**
     * convert between RGB/BGR and grayscale, \ref color_convert_rgb_gray "color conversions"
     */
    BGR2GRAY(opencv_imgproc.COLOR_BGR2GRAY),
    RGB2GRAY(opencv_imgproc.COLOR_RGB2GRAY),
    GRAY2BGR(opencv_imgproc.COLOR_GRAY2BGR, true),
    GRAY2BGRA(opencv_imgproc.COLOR_GRAY2BGRA, true),
    BGRA2GRAY(opencv_imgproc.COLOR_BGRA2GRAY),
    RGBA2GRAY(opencv_imgproc.COLOR_RGBA2GRAY),

    /**
     * convert between RGB/BGR and BGR565 (16-bit images)
     */
    BGR2BGR565(opencv_imgproc.COLOR_BGR2BGR565),
    RGB2BGR565(opencv_imgproc.COLOR_RGB2BGR565),
    BGR5652BGR(opencv_imgproc.COLOR_BGR5652BGR),
    BGR5652RGB(opencv_imgproc.COLOR_BGR5652RGB),
    BGRA2BGR565(opencv_imgproc.COLOR_BGRA2BGR565),
    RGBA2BGR565(opencv_imgproc.COLOR_RGBA2BGR565),
    BGR5652BGRA(opencv_imgproc.COLOR_BGR5652BGRA),
    BGR5652RGBA(opencv_imgproc.COLOR_BGR5652RGBA),

    /**
     * convert between grayscale to BGR565 (16-bit images)
     */
    GRAY2BGR565(opencv_imgproc.COLOR_GRAY2BGR565, true),
    BGR5652GRAY(opencv_imgproc.COLOR_BGR5652GRAY),

    /**
     * convert between RGB/BGR and BGR555 (16-bit images)
     */
    BGR2BGR555(opencv_imgproc.COLOR_BGR2BGR555),
    RGB2BGR555(opencv_imgproc.COLOR_RGB2BGR555),
    BGR5552BGR(opencv_imgproc.COLOR_BGR5552BGR),
    BGR5552RGB(opencv_imgproc.COLOR_BGR5552RGB),
    BGRA2BGR555(opencv_imgproc.COLOR_BGRA2BGR555),
    RGBA2BGR555(opencv_imgproc.COLOR_RGBA2BGR555),
    BGR5552BGRA(opencv_imgproc.COLOR_BGR5552BGRA),
    BGR5552RGBA(opencv_imgproc.COLOR_BGR5552RGBA),

    /**
     * convert between grayscale and BGR555 (16-bit images)
     */
    GRAY2BGR555(opencv_imgproc.COLOR_GRAY2BGR555, true),
    BGR5552GRAY(opencv_imgproc.COLOR_BGR5552GRAY),

    /**
     * convert RGB/BGR to CIE XYZ, \ref color_convert_rgb_xyz "color conversions"
     */
    BGR2XYZ(opencv_imgproc.COLOR_BGR2XYZ),
    RGB2XYZ(opencv_imgproc.COLOR_RGB2XYZ),
    XYZ2BGR(opencv_imgproc.COLOR_XYZ2BGR),
    XYZ2RGB(opencv_imgproc.COLOR_XYZ2RGB),

    /**
     * convert RGB/BGR to luma-chroma (aka YCC), \ref color_convert_rgb_ycrcb "color conversions"
     */
    BGR2YCrCb(opencv_imgproc.COLOR_BGR2YCrCb),
    RGB2YCrCb(opencv_imgproc.COLOR_RGB2YCrCb),
    YCrCb2BGR(opencv_imgproc.COLOR_YCrCb2BGR),
    YCrCb2RGB(opencv_imgproc.COLOR_YCrCb2RGB),

    /**
     * convert RGB/BGR to HSV (hue saturation value), \ref color_convert_rgb_hsv "color conversions"
     */
    BGR2HSV(opencv_imgproc.COLOR_BGR2HSV),
    RGB2HSV(opencv_imgproc.COLOR_RGB2HSV),

    /**
     * convert RGB/BGR to CIE Lab, \ref color_convert_rgb_lab "color conversions"
     */
    BGR2Lab(opencv_imgproc.COLOR_BGR2Lab),
    RGB2Lab(opencv_imgproc.COLOR_RGB2Lab),

    /**
     * convert RGB/BGR to CIE Luv, \ref color_convert_rgb_luv "color conversions"
     */
    BGR2Luv(opencv_imgproc.COLOR_BGR2Luv),
    RGB2Luv(opencv_imgproc.COLOR_RGB2Luv),

    /**
     * convert RGB/BGR to HLS (hue lightness saturation), \ref color_convert_rgb_hls "color conversions"
     */
    BGR2HLS(opencv_imgproc.COLOR_BGR2HLS),
    RGB2HLS(opencv_imgproc.COLOR_RGB2HLS),

    /**
     * backward conversions to RGB/BGR
     */
    HSV2BGR(opencv_imgproc.COLOR_HSV2BGR),
    HSV2RGB(opencv_imgproc.COLOR_HSV2RGB),

    Lab2BGR(opencv_imgproc.COLOR_Lab2BGR),
    Lab2RGB(opencv_imgproc.COLOR_Lab2RGB),
    Luv2BGR(opencv_imgproc.COLOR_Luv2BGR),
    Luv2RGB(opencv_imgproc.COLOR_Luv2RGB),
    HLS2BGR(opencv_imgproc.COLOR_HLS2BGR),
    HLS2RGB(opencv_imgproc.COLOR_HLS2RGB),

    BGR2HSV_FULL(opencv_imgproc.COLOR_BGR2HSV_FULL),
    RGB2HSV_FULL(opencv_imgproc.COLOR_RGB2HSV_FULL),
    BGR2HLS_FULL(opencv_imgproc.COLOR_BGR2HLS_FULL),
    RGB2HLS_FULL(opencv_imgproc.COLOR_RGB2HLS_FULL),

    HSV2BGR_FULL(opencv_imgproc.COLOR_HSV2BGR_FULL),
    HSV2RGB_FULL(opencv_imgproc.COLOR_HSV2RGB_FULL),
    HLS2BGR_FULL(opencv_imgproc.COLOR_HLS2BGR_FULL),
    HLS2RGB_FULL(opencv_imgproc.COLOR_HLS2RGB_FULL),

    LBGR2Lab(opencv_imgproc.COLOR_LBGR2Lab),
    LRGB2Lab(opencv_imgproc.COLOR_LRGB2Lab),
    LBGR2Luv(opencv_imgproc.COLOR_LBGR2Luv),
    LRGB2Luv(opencv_imgproc.COLOR_LRGB2Luv),

    Lab2LBGR(opencv_imgproc.COLOR_Lab2LBGR),
    Lab2LRGB(opencv_imgproc.COLOR_Lab2LRGB),
    Luv2LBGR(opencv_imgproc.COLOR_Luv2LBGR),
    Luv2LRGB(opencv_imgproc.COLOR_Luv2LRGB),

    /**
     * convert between RGB/BGR and YUV
     */
    BGR2YUV(opencv_imgproc.COLOR_BGR2YUV),
    RGB2YUV(opencv_imgproc.COLOR_RGB2YUV),
    YUV2BGR(opencv_imgproc.COLOR_YUV2BGR),
    YUV2RGB(opencv_imgproc.COLOR_YUV2RGB),

    /**
     * YUV 4:2:0 family to RGB
     */
    YUV2RGB_NV12(opencv_imgproc.COLOR_YUV2RGB_NV12),
    YUV2BGR_NV12(opencv_imgproc.COLOR_YUV2BGR_NV12),
    YUV2RGB_NV21(opencv_imgproc.COLOR_YUV2RGB_NV21),
    YUV2BGR_NV21(opencv_imgproc.COLOR_YUV2BGR_NV21),

    YUV2RGBA_NV12(opencv_imgproc.COLOR_YUV2RGBA_NV12),
    YUV2BGRA_NV12(opencv_imgproc.COLOR_YUV2BGRA_NV12),
    YUV2RGBA_NV21(opencv_imgproc.COLOR_YUV2RGBA_NV21),
    YUV2BGRA_NV21(opencv_imgproc.COLOR_YUV2BGRA_NV21),

    YUV2RGB_YV12(opencv_imgproc.COLOR_YUV2RGB_YV12),
    YUV2BGR_YV12(opencv_imgproc.COLOR_YUV2BGR_YV12),
    YUV2RGB_IYUV(opencv_imgproc.COLOR_YUV2RGB_IYUV),
    YUV2BGR_IYUV(opencv_imgproc.COLOR_YUV2BGR_IYUV),

    YUV2RGBA_YV12(opencv_imgproc.COLOR_YUV2RGBA_YV12),
    YUV2BGRA_YV12(opencv_imgproc.COLOR_YUV2BGRA_YV12),
    YUV2RGBA_IYUV(opencv_imgproc.COLOR_YUV2RGBA_IYUV),
    YUV2BGRA_IYUV(opencv_imgproc.COLOR_YUV2BGRA_IYUV),

    YUV2GRAY_420(opencv_imgproc.COLOR_YUV2GRAY_420),

    /**
     * YUV 4:2:2 family to RGB
     */
    YUV2RGB_UYVY(opencv_imgproc.COLOR_YUV2RGB_UYVY),
    YUV2BGR_UYVY(opencv_imgproc.COLOR_YUV2BGR_UYVY),

    YUV2RGBA_UYVY(opencv_imgproc.COLOR_YUV2RGBA_UYVY),
    YUV2BGRA_UYVY(opencv_imgproc.COLOR_YUV2BGRA_UYVY),

    YUV2RGB_YUY2(opencv_imgproc.COLOR_YUV2RGB_YUY2),
    YUV2BGR_YUY2(opencv_imgproc.COLOR_YUV2BGR_YUY2),
    YUV2RGB_YVYU(opencv_imgproc.COLOR_YUV2RGB_YVYU),
    YUV2BGR_YVYU(opencv_imgproc.COLOR_YUV2BGR_YVYU),

    YUV2RGBA_YUY2(opencv_imgproc.COLOR_YUV2RGBA_YUY2),
    YUV2BGRA_YUY2(opencv_imgproc.COLOR_YUV2BGRA_YUY2),
    YUV2RGBA_YVYU(opencv_imgproc.COLOR_YUV2RGBA_YVYU),
    YUV2BGRA_YVYU(opencv_imgproc.COLOR_YUV2BGRA_YVYU),

    YUV2GRAY_UYVY(opencv_imgproc.COLOR_YUV2GRAY_UYVY),
    YUV2GRAY_YUY2(opencv_imgproc.COLOR_YUV2GRAY_YUY2),

    /**
     * alpha premultiplication
     */
    RGBA2mRGBA(opencv_imgproc.COLOR_RGBA2mRGBA),
    mRGBA2RGBA(opencv_imgproc.COLOR_mRGBA2RGBA),

    /**
     * RGB to YUV 4:2:0 family
     */
    RGB2YUV_I420(opencv_imgproc.COLOR_RGB2YUV_I420),
    BGR2YUV_I420(opencv_imgproc.COLOR_BGR2YUV_I420),

    RGBA2YUV_I420(opencv_imgproc.COLOR_RGBA2YUV_I420),
    BGRA2YUV_I420(opencv_imgproc.COLOR_BGRA2YUV_I420),
    RGB2YUV_YV12(opencv_imgproc.COLOR_RGB2YUV_YV12),
    BGR2YUV_YV12(opencv_imgproc.COLOR_BGR2YUV_YV12),
    RGBA2YUV_YV12(opencv_imgproc.COLOR_RGBA2YUV_YV12),
    BGRA2YUV_YV12(opencv_imgproc.COLOR_BGRA2YUV_YV12),

    /**
     * Demosaicing
     */
    BayerBG2BGR(opencv_imgproc.COLOR_BayerBG2BGR),
    BayerGB2BGR(opencv_imgproc.COLOR_BayerGB2BGR),
    BayerRG2BGR(opencv_imgproc.COLOR_BayerRG2BGR),
    BayerGR2BGR(opencv_imgproc.COLOR_BayerGR2BGR),

    BayerBG2GRAY(opencv_imgproc.COLOR_BayerBG2GRAY),
    BayerGB2GRAY(opencv_imgproc.COLOR_BayerGB2GRAY),
    BayerRG2GRAY(opencv_imgproc.COLOR_BayerRG2GRAY),
    BayerGR2GRAY(opencv_imgproc.COLOR_BayerGR2GRAY),

    /**
     * Demosaicing using Variable Number of Gradients
     */
    BayerBG2BGR_VNG(opencv_imgproc.COLOR_BayerBG2BGR_VNG),
    BayerGB2BGR_VNG(opencv_imgproc.COLOR_BayerGB2BGR_VNG),
    BayerRG2BGR_VNG(opencv_imgproc.COLOR_BayerRG2BGR_VNG),
    BayerGR2BGR_VNG(opencv_imgproc.COLOR_BayerGR2BGR_VNG),


    /**
     * Edge-Aware Demosaicing
     */
    BayerBG2BGR_EA(opencv_imgproc.COLOR_BayerBG2BGR_EA),
    BayerGB2BGR_EA(opencv_imgproc.COLOR_BayerGB2BGR_EA),
    BayerRG2BGR_EA(opencv_imgproc.COLOR_BayerRG2BGR_EA),
    BayerGR2BGR_EA(opencv_imgproc.COLOR_BayerGR2BGR_EA),


    /**
     * Demosaicing with alpha channel
     */
    BayerBG2BGRA(opencv_imgproc.COLOR_BayerBG2BGRA),
    BayerGB2BGRA(opencv_imgproc.COLOR_BayerGB2BGRA),
    BayerRG2BGRA(opencv_imgproc.COLOR_BayerRG2BGRA),
    BayerGR2BGRA(opencv_imgproc.COLOR_BayerGR2BGRA);

    private final int code;
    private final boolean singleChannelGrayScaleSource;

    public int code() {
        return code;
    }

    public boolean isSingleChannelGrayScaleSource() {
        return singleChannelGrayScaleSource;
    }

    OColorConversion(int code) {
        this(code, false);
    }

    OColorConversion(int code, boolean singleChannelGrayScaleSource) {
        this.code = code;
        this.singleChannelGrayScaleSource = singleChannelGrayScaleSource;
    }
}
