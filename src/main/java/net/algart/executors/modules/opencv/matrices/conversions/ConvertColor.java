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

package net.algart.executors.modules.opencv.matrices.conversions;

import net.algart.executors.modules.opencv.common.UMatFilter;
import net.algart.executors.modules.util.opencv.O2SMat;
import net.algart.executors.modules.util.opencv.OTools;
import net.algart.executors.modules.util.opencv.enums.OColorConversion;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgproc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ConvertColor extends UMatFilter {
    public static final String CHANNEL_1 = "channel_1";
    public static final String CHANNEL_2 = "channel_2";
    public static final String CHANNEL_3 = "channel_3";
    public static final String CHANNEL_4 = "channel_4";
    public static final String RAW_0 = "raw_0";
    public static final String RAW_1 = "raw_1";
    public static final String RAW_2 = "raw_2";
    public static final String RAW_3 = "raw_3";

    public static final List<String> CHANNEL_PORTS = Collections.unmodifiableList(Arrays.asList(
            CHANNEL_1, CHANNEL_2, CHANNEL_3, CHANNEL_4
    ));

    public static final List<String> RAW_CHANNEL_PORTS = Collections.unmodifiableList(Arrays.asList(
            RAW_0, RAW_1, RAW_2, RAW_3
    ));
    // - both lists must have identical length (4)

    private OColorConversion colorConversion = OColorConversion.BGR2Lab;
    private boolean floatResult = true;
    private boolean rgbOrder = true;
    private boolean autoConvertGrayscaleToColor = true;
    private double scale = 1.0;

    public ConvertColor() {
        useVisibleResultParameter();
        for (String port : CHANNEL_PORTS) {
            addOutputMat(port);
        }
        for (String port : RAW_CHANNEL_PORTS) {
            addOutputMat(port);
        }
    }

    public OColorConversion getColorConversion() {
        return colorConversion;
    }

    public ConvertColor setColorConversion(OColorConversion colorConversion) {
        this.colorConversion = nonNull(colorConversion);
        return this;
    }

    public boolean isFloatResult() {
        return floatResult;
    }

    public ConvertColor setFloatResult(boolean floatResult) {
        this.floatResult = floatResult;
        return this;
    }

    public boolean isRgbOrder() {
        return rgbOrder;
    }

    public ConvertColor setRgbOrder(boolean rgbOrder) {
        this.rgbOrder = rgbOrder;
        return this;
    }

    public boolean isAutoConvertGrayscaleToColor() {
        return autoConvertGrayscaleToColor;
    }

    public ConvertColor setAutoConvertGrayscaleToColor(boolean autoConvertGrayscaleToColor) {
        this.autoConvertGrayscaleToColor = autoConvertGrayscaleToColor;
        return this;
    }

    public double getScale() {
        return scale;
    }

    public ConvertColor setScale(double scale) {
        this.scale = scale;
        return this;
    }

    /*Repeat() \bMat\b ==> UMat */
    @Override
    public Mat process(Mat source) {
        logDebug(() -> "Convert color: " + colorConversion + " conversion");
        if (colorConversion != OColorConversion.NONE) {
            if (autoConvertGrayscaleToColor
                    && !colorConversion.isSingleChannelGrayScaleSource()
                    && source.channels() == 1) {
                opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_GRAY2BGR);
            }
            if (floatResult) {
                OTools.make32FIfNot(source);
                // - before flipRBChannels: flipRBChannels calls cvtColor and may fail for unsupported depths
            }
            if (rgbOrder) {
                OTools.flipRBChannels(source);
            }
            opencv_imgproc.cvtColor(source, source, colorConversion.code());
            if (rgbOrder) {
                OTools.flipRBChannels(source);
            }
            if (scale != 1.0) {
                source.convertTo(source, -1, scale, 0.0);
            }
        }
        for (int c = 0, n = Math.min(RAW_CHANNEL_PORTS.size(), source.channels()); c < n; c++) {
            final int channel = flipIndex(c, source.channels(), true);
            final int rawChannel = flipIndex(c, source.channels(), false);
            final boolean needChannelForOpenCV = isOutputNecessary(CHANNEL_PORTS.get(channel));
            final boolean needRawChannel = isOutputNecessary(RAW_CHANNEL_PORTS.get(rawChannel));
            if (needChannelForOpenCV || needRawChannel) {
                Mat m = new Mat();
                opencv_core.extractChannel(source, m, c);
                if (needChannelForOpenCV) {
                    O2SMat.setTo(getMat(CHANNEL_PORTS.get(channel)), m);
                }
                if (needRawChannel) {
                    O2SMat.setTo(getMat(RAW_CHANNEL_PORTS.get(rawChannel)), m);
                }
            }
        }
        return source;
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    @Override
    public UMat process(UMat source) {
        logDebug(() -> "Convert color: " + colorConversion + " conversion");
        if (colorConversion != OColorConversion.NONE) {
            if (autoConvertGrayscaleToColor
                    && !colorConversion.isSingleChannelGrayScaleSource()
                    && source.channels() == 1) {
                opencv_imgproc.cvtColor(source, source, opencv_imgproc.CV_GRAY2BGR);
            }
            if (floatResult) {
                OTools.make32FIfNot(source);
                // - before flipRBChannels: flipRBChannels calls cvtColor and may fail for unsupported depths
            }
            if (rgbOrder) {
                OTools.flipRBChannels(source);
            }
            opencv_imgproc.cvtColor(source, source, colorConversion.code());
            if (rgbOrder) {
                OTools.flipRBChannels(source);
            }
            if (scale != 1.0) {
                source.convertTo(source, -1, scale, 0.0);
            }
        }
        for (int c = 0, n = Math.min(RAW_CHANNEL_PORTS.size(), source.channels()); c < n; c++) {
            final int channel = flipIndex(c, source.channels(), true);
            final int rawChannel = flipIndex(c, source.channels(), false);
            final boolean needChannelForOpenCV = isOutputNecessary(CHANNEL_PORTS.get(channel));
            final boolean needRawChannel = isOutputNecessary(RAW_CHANNEL_PORTS.get(rawChannel));
            if (needChannelForOpenCV || needRawChannel) {
                UMat m = new UMat();
                opencv_core.extractChannel(source, m, c);
                if (needChannelForOpenCV) {
                    O2SMat.setTo(getMat(CHANNEL_PORTS.get(channel)), m);
                }
                if (needRawChannel) {
                    O2SMat.setTo(getMat(RAW_CHANNEL_PORTS.get(rawChannel)), m);
                }
            }
        }
        return source;
    }

    /*Repeat.AutoGeneratedEnd*/

    private int flipIndex(int c, int numberOfChannels, boolean always) {
        if ((always || rgbOrder) && (numberOfChannels == 3 || numberOfChannels == 4)) {
            // - see OTools.flipRBChannels
            return c == 0 || c == 2 ? 2 - c : c;
        } else {
            return c;
        }
    }
}
