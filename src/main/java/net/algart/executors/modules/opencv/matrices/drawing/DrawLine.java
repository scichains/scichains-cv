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

package net.algart.executors.modules.opencv.matrices.drawing;

import net.algart.executors.api.data.SMat;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.IndexingBase;
import net.algart.executors.modules.cv.matrices.drawing.PositionsAndColors;
import net.algart.executors.modules.opencv.common.OpenCVExecutor;
import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.modules.opencv.util.OTools;
import net.algart.executors.modules.opencv.util.enums.OLineType;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.awt.*;

import static net.algart.executors.modules.cv.matrices.drawing.DrawLine.increaseLength;
import static net.algart.executors.modules.cv.matrices.drawing.DrawLine.translatePairsOfIndexes;

// No sense to implement UMatFilter: drawing functions work with UMat VERY slowly.
public final class DrawLine extends OpenCVExecutor {
    public static final String INPUT_POSITIONS = "positions";
    public static final String INPUT_PAIRS_OF_INDEXES_OF_POINTS = "pairs_of_indexes_of_points";
    public static final String INPUT_COLORS = "colors";

    private boolean percents = false;
    private double x1 = 0.0;
    private double y1 = 0.0;
    private double x2 = 0.0;
    private double y2 = 0.0;
    private int thickness = 1;
    private double lengthIncrement = 0.0;
    private IndexingBase indexingBase = IndexingBase.ONE_BASED;
    private OLineType lineType = OLineType.LINE_8;
    private Color color = Color.WHITE;
    private String backgroundColor = "";

    public DrawLine() {
        setDefaultInputMat(DEFAULT_INPUT_PORT);
        addInputNumbers(INPUT_POSITIONS);
        addInputNumbers(INPUT_PAIRS_OF_INDEXES_OF_POINTS);
        addInputNumbers(INPUT_COLORS);
        setDefaultOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public boolean isPercents() {
        return percents;
    }

    public DrawLine setPercents(boolean percents) {
        this.percents = percents;
        return this;
    }

    public double getX1() {
        return x1;
    }

    public DrawLine setX1(double x1) {
        this.x1 = x1;
        return this;
    }

    public double getY1() {
        return y1;
    }

    public DrawLine setY1(double y1) {
        this.y1 = y1;
        return this;
    }

    public double getX2() {
        return x2;
    }

    public DrawLine setX2(double x2) {
        this.x2 = x2;
        return this;
    }

    public double getY2() {
        return y2;
    }

    public DrawLine setY2(double y2) {
        this.y2 = y2;
        return this;
    }

    public int getThickness() {
        return thickness;
    }

    public DrawLine setThickness(int thickness) {
        this.thickness = nonNegative(thickness);
        return this;
    }

    public double getLengthIncrement() {
        return lengthIncrement;
    }

    public DrawLine setLengthIncrement(double lengthIncrement) {
        this.lengthIncrement = lengthIncrement;
        return this;
    }

    public IndexingBase getIndexingBase() {
        return indexingBase;
    }

    public DrawLine setIndexingBase(IndexingBase indexingBase) {
        this.indexingBase = nonNull(indexingBase);
        return this;
    }

    public OLineType getLineType() {
        return lineType;
    }

    public DrawLine setLineType(OLineType lineType) {
        this.lineType = nonNull(lineType);
        return this;
    }

    public Color getColor() {
        return color;
    }

    public DrawLine setColor(Color color) {
        this.color = nonNull(color);
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public DrawLine setBackgroundColor(String backgroundColor) {
        this.backgroundColor = nonNull(backgroundColor);
        return this;
    }

    public boolean hasBackgroundColor() {
        return !backgroundColor.trim().isEmpty();
    }

    @Override
    public void process() {
        final SMat m = getInputMat(true);
        final Mat source;
        if (hasBackgroundColor() && !m.getDepth().isOpenCVCompatible()) {
            // - in this case, there is no sense to convert unsupported BIT type to byte:
            // all previous content will be erased in any case
            if (m.getDimX() > Integer.MAX_VALUE || m.getDimY() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Too large matrix sizes (>2^31): " + m);
            }
            final int type = opencv_core.CV_MAKE_TYPE(opencv_core.CV_8U, m.getNumberOfChannels());
            source = new Mat((int) m.getDimY(), (int) m.getDimX(), type);
            // - note: (int) cast is very important here, without it another constructor will be called!
        } else {
            source = O2SMat.toMat(m, true);
        }
        O2SMat.setTo(getMat(), process(source));
    }

    public Mat process(Mat source) {
        final SNumbers positions = getInputNumbers(INPUT_POSITIONS, true);
        final SNumbers pairsOfIndexes = getInputNumbers(INPUT_PAIRS_OF_INDEXES_OF_POINTS, true);
        if (hasBackgroundColor()) {
            try (Scalar scalar = OTools.scalarBGRA(backgroundColor, OTools.maxPossibleValue(source))) {
                source.put(scalar);
            }
        }
        if (pairsOfIndexes.isInitialized()) {
            translatePairsOfIndexes(positions, pairsOfIndexes, indexingBase);
        }
        final SNumbers colors = getInputNumbers(INPUT_COLORS, true);
        final double defaultX1 = percents ? this.x1 / 100.0 * (source.cols() - 1) : this.x1;
        final double defaultY1 = percents ? this.y1 / 100.0 * (source.rows() - 1) : this.y1;
        final double defaultX2 = percents ? this.x2 / 100.0 * (source.cols() - 1) : this.x2;
        final double defaultY2 = percents ? this.y2 / 100.0 * (source.rows() - 1) : this.y2;

        final PositionsAndColors positionsAndColors = new PositionsAndColors(
                positions, colors,
                new double[]{defaultX1, defaultY1, defaultX2, defaultY2},
                Double.NaN, 4);
        final double scale = OTools.maxPossibleValue(source);
        for (int k = 0, n = positionsAndColors.n(); k < n; k++) {
            final double[] x1y1x2y2 = positionsAndColors.xyAndOthers(k);
            increaseLength(x1y1x2y2, lengthIncrement);
            try (Scalar color = rgbToScalar(positionsAndColors.colorRGB(k), scale);
                 Point p1 = new Point((int) x1y1x2y2[0], (int) x1y1x2y2[1]);
                 Point p2 = new Point((int) x1y1x2y2[2], (int) x1y1x2y2[3])) {
                opencv_imgproc.line(source, p1, p2, color, thickness, lineType.code(), 0);
            }
        }
        return source;
    }

    protected boolean allowInputPackedBits() {
        return true;
    }

    private Scalar rgbToScalar(float[] rgb, double scale) {
        if (rgb == null) {
            rgb = color.getRGBColorComponents(null);
        }
        final double r = truncateColor01(rgb[0]);
        final double g = truncateColor01(rgb[1]);
        final double b = truncateColor01(rgb[2]);
        return new Scalar(b * scale, g * scale, r * scale, scale);
    }

    private static double truncateColor01(double v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }
}
