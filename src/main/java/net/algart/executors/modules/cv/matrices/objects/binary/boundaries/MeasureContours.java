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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.contours.ContourHeader;
import net.algart.contours.ContourNestingAnalyser;
import net.algart.contours.Contours;
import net.algart.math.IRectangularArea;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

public final class MeasureContours extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_PARENT_CONTOUR_INDEX = "parent_contour_index";
    public static final String OUTPUT_NESTING_LEVEL = "nesting_level";
    public static final String OUTPUT_STRICT_AREA = "strict_area";
    public static final String OUTPUT_SEGMENT_CENTERS_AREA = "segment_centers_area";
    public static final String OUTPUT_STRICT_PERIMETER = "strict_perimeter";
    public static final String OUTPUT_SEGMENT_CENTERS_PERIMETER = "segment_centers_perimeter";
    public static final String OUTPUT_PRECISE_DOUBLED_AREA = "precise_doubled_area";
    public static final String OUTPUT_CONTAINING_RECTANGLE = "containing_rectangle";
    public static final String OUTPUT_INSIDE_REPRESENTATIVE = "inside_representative";
    public static final String OUTPUT_DEGENERATED = "degenerated";
    public static final String OUTPUT_TOUCHING_MATRIX_BOUNDARY_FLAGS = "matrix_boundary_flags";
    public static final String OUTPUT_FRAME_ID = "frame_id";
    public static final String OUTPUT_SORTED_INDEXES_BY_LABEL = "sorted_indexes_by_label";
    public static final String OUTPUT_SORTED_INDEXES_BY_AREA = "sorted_indexes_by_area";
    public static final String OUTPUT_CONTOUR_OFFSETS = "contour_offsets";
    public static final String OUTPUT_CONTAINING_ALL_RECTANGLE = "containing_all_rectangle";

    private boolean needToAnalyseNestingForDiagonals = false;
    private boolean pixelCoordinatesForRectangles = true;

    public MeasureContours() {
        useVisibleResultParameter();
        setDefaultInputNumbers(INPUT_CONTOURS);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL);
        addOutputNumbers(OUTPUT_PARENT_CONTOUR_INDEX);
        addOutputNumbers(OUTPUT_NESTING_LEVEL);
        addOutputNumbers(OUTPUT_STRICT_AREA);
        addOutputNumbers(OUTPUT_SEGMENT_CENTERS_AREA);
        addOutputNumbers(OUTPUT_STRICT_PERIMETER);
        addOutputNumbers(OUTPUT_SEGMENT_CENTERS_PERIMETER);
        addOutputNumbers(OUTPUT_PRECISE_DOUBLED_AREA);
        addOutputNumbers(OUTPUT_CONTAINING_RECTANGLE);
        addOutputNumbers(OUTPUT_INSIDE_REPRESENTATIVE);
        addOutputNumbers(OUTPUT_DEGENERATED);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY);
        addOutputNumbers(OUTPUT_TOUCHING_MATRIX_BOUNDARY_FLAGS);
        addOutputNumbers(OUTPUT_FRAME_ID);
        addOutputNumbers(OUTPUT_SORTED_INDEXES_BY_LABEL);
        addOutputNumbers(OUTPUT_SORTED_INDEXES_BY_AREA);
        addOutputNumbers(OUTPUT_CONTOUR_OFFSETS);
        addOutputNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public boolean isNeedToAnalyseNestingForDiagonals() {
        return needToAnalyseNestingForDiagonals;
    }

    public MeasureContours setNeedToAnalyseNestingForDiagonals(boolean needToAnalyseNestingForDiagonals) {
        this.needToAnalyseNestingForDiagonals = needToAnalyseNestingForDiagonals;
        return this;
    }

    public boolean isPixelCoordinatesForRectangles() {
        return pixelCoordinatesForRectangles;
    }

    public MeasureContours setPixelCoordinatesForRectangles(boolean pixelCoordinatesForRectangles) {
        this.pixelCoordinatesForRectangles = pixelCoordinatesForRectangles;
        return this;
    }

    @Override
    public void process() {
        final Contours contours = Contours.deserialize(getInputNumbers(INPUT_CONTOURS).toIntArray());
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(contours.numberOfContours());
        long t1 = debugTime();
        String additionalTiming = "";
        final boolean nestingLevelNecessary = isOutputNecessary(OUTPUT_NESTING_LEVEL);
        long[] doubledAreas = null;
        if (isOutputNecessary(OUTPUT_PARENT_CONTOUR_INDEX) || nestingLevelNecessary) {
            long t11 = debugTime();
            final boolean weUnpackContours = !needToAnalyseNestingForDiagonals;
            final Contours unpackedIfNeeded;
            if (weUnpackContours) {
                doubledAreas = new long[contours.numberOfContours()];
                unpackedIfNeeded = contours.unpackContours(false, doubledAreas);
            } else {
                unpackedIfNeeded = contours;
            }
            // - unpackContours will throw an exception if contours contain diagonal segments
            long t12 = debugTime();
            final ContourNestingAnalyser analyser = ContourNestingAnalyser.newInstance(
                    unpackedIfNeeded, weUnpackContours, doubledAreas);
            analyser.setNestingLevelNecessary(nestingLevelNecessary);
            long t13 = debugTime();
            analyser.analyseAllContours();
            long t14 = debugTime();
            getNumbers(OUTPUT_PARENT_CONTOUR_INDEX).setTo(analyser.getContourNestingParents(), 1);
            if (nestingLevelNecessary) {
                getNumbers(OUTPUT_NESTING_LEVEL).setTo(analyser.getContourNestingLevels(), 1);
            } else {
                getNumbers(OUTPUT_NESTING_LEVEL).setInitialized(false);
            }
            if (LOGGABLE_DEBUG) {
                final double m = 1.0 / contours.numberOfContours();
                additionalTiming += String.format(Locale.US,
                        "; nesting: %s%.3f ms initializing + %.3f ms analysis"
                                + " (total/mean number of nesting contours %d/%.4f, "
                                + "checked %d/%.4f, summary length %d/%.4f - %s)",
                        weUnpackContours ?
                                String.format(Locale.US, "%.3f ms unpacking + ", (t12 - t11) * 1e-6) :
                                "",
                        (t13 - t12) * 1e-6, (t14 - t13) * 1e-6,
                        analyser.numberOfNestingContours(), analyser.numberOfNestingContours() * m,
                        analyser.numberOfCheckedContours(), analyser.numberOfCheckedContours() * m,
                        analyser.summaryContoursLength(), analyser.summaryContoursLength() * m,
                        analyser);
            }
        }
        if (isOutputNecessary(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL)) {
            getNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL).setTo(
                    contours.getAllObjectLabels(), 1);
        }
        if (isOutputNecessary(OUTPUT_STRICT_AREA)) {
            final float[] result = new float[contours.numberOfContours()];
            IntStream.range(0, result.length).parallel().forEach(
                    k -> result[k] = (float) contours.strictArea(k));
            getNumbers(OUTPUT_STRICT_AREA).setTo(result, 1);
        }
        if (isOutputNecessary(OUTPUT_SEGMENT_CENTERS_AREA)) {
            final float[] result = new float[contours.numberOfContours()];
            IntStream.range(0, result.length).parallel().forEach(
                    k -> result[k] = (float) contours.segmentCentersArea(k));
            getNumbers(OUTPUT_SEGMENT_CENTERS_AREA).setTo(result, 1);
        }
        if (isOutputNecessary(OUTPUT_STRICT_PERIMETER)) {
            final float[] result = new float[contours.numberOfContours()];
            IntStream.range(0, result.length).parallel().forEach(
                    k -> result[k] = (float) contours.strictPerimeter(k));
            getNumbers(OUTPUT_STRICT_PERIMETER).setTo(result, 1);
        }
        if (isOutputNecessary(OUTPUT_SEGMENT_CENTERS_PERIMETER)) {
            final float[] result = new float[contours.numberOfContours()];
            IntStream.range(0, result.length).parallel().forEach(
                    k -> result[k] = (float) contours.segmentCentersPerimeter(k));
            getNumbers(OUTPUT_SEGMENT_CENTERS_PERIMETER).setTo(result, 1);
        }
        if (isOutputNecessary(OUTPUT_PRECISE_DOUBLED_AREA)) {
            final long[] result;
            if (doubledAreas != null) {
                result = doubledAreas;
            } else {
                result = new long[contours.numberOfContours()];
                IntStream.range(0, result.length).parallel().forEach(
                        k -> result[k] = contours.preciseDoubledArea(k));
            }
            getNumbers(OUTPUT_PRECISE_DOUBLED_AREA).setTo(result, 1);
        }
        long containingAllMinX = Long.MAX_VALUE;
        long containingAllMaxX = Long.MIN_VALUE;
        long containingAllMinY = Long.MAX_VALUE;
        long containingAllMaxY = Long.MIN_VALUE;
        if (isOutputNecessary(OUTPUT_CONTAINING_RECTANGLE) || isOutputNecessary(OUTPUT_CONTAINING_ALL_RECTANGLE)) {
            final float[] result = new float[4 * contours.numberOfContours()];
            final ContourHeader h = new ContourHeader();
            final int n = contours.numberOfContours();
            final double correction = pixelCoordinatesForRectangles ? -0.5 : 0.0;
            for (int k = 0, offset = 0; k < n; k++, offset += 4) {
                contours.getHeader(h, k);
                BoundaryParameter.pushRectangle(
                        result,
                        offset,
                        h.minX() + correction,
                        h.minY() + correction,
                        h.maxX() + correction,
                        h.maxY() + correction);
                containingAllMinX = Math.min(containingAllMinX, h.minX());
                containingAllMaxX = Math.max(containingAllMaxX, h.maxX());
                containingAllMinY = Math.min(containingAllMinY, h.minY());
                containingAllMaxY = Math.max(containingAllMaxY, h.maxY());
            }
            getNumbers(OUTPUT_CONTAINING_RECTANGLE).setTo(result, 4);
            if (n > 0) {
                getNumbers(OUTPUT_CONTAINING_ALL_RECTANGLE).setTo(IRectangularArea.valueOf(
                        containingAllMinX, containingAllMinY, containingAllMaxX, containingAllMaxY));
            }
        }
        if (isOutputNecessary(OUTPUT_INSIDE_REPRESENTATIVE) || isOutputNecessary(OUTPUT_DEGENERATED)) {
            long t11 = debugTime();
            final int n = contours.numberOfContours();
            final float[] result = new float[2 * n];
            final byte[] degenerated = new byte[n];
            long t12 = debugTime();
            IntStream.range(0, (n + 15) >>> 4).parallel().forEach(block -> {
                Point2D.Float point = new Point2D.Float();
                for (int i = block << 4, to = (int) Math.min((long) i + 16, n); i < to; i++) {
                    final boolean found = contours.findSomePointInside(point, i);
                    // - no sense to use here surelyUnpacked mode: it almost does not save time
                    // and can be even slower (because unpacked contour is larger than packed)
                    result[2 * i] = point.x;
                    result[2 * i + 1] = point.y;
                    degenerated[i] = found ? (byte) 0 : (byte) 1;
                }
            });
            long t13 = debugTime();
            additionalTiming += String.format(Locale.US,
                    "; representatives: %.3f ms allocation + %.3f ms analysis",
                    (t12 - t11) * 1e-6, (t13 - t12) * 1e-6);
            getNumbers(OUTPUT_INSIDE_REPRESENTATIVE).setTo(result, 2);
            getNumbers(OUTPUT_DEGENERATED).setTo(degenerated, 1);
        }
        if (isOutputNecessary(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY)) {
            getNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY).setTo(
                    booleansToBytes(contours.getAllInternalContour()), 1);
        }
        if (isOutputNecessary(OUTPUT_TOUCHING_MATRIX_BOUNDARY_FLAGS)) {
            final byte[] result = new byte[4 * contours.numberOfContours()];
            ContourHeader h = new ContourHeader();
            for (int k = 0, n = contours.numberOfContours(), offset = 0; k < n; k++) {
                contours.getHeader(h, k);
                result[offset++] = (byte) (h.isContourTouchingMinXMatrixBoundary() ? 0x1 : 0);
                result[offset++] = (byte) (h.isContourTouchingMaxXMatrixBoundary() ? 0x1 : 0);
                result[offset++] = (byte) (h.isContourTouchingMinYMatrixBoundary() ? 0x1 : 0);
                result[offset++] = (byte) (h.isContourTouchingMaxYMatrixBoundary() ? 0x1 : 0);
            }
            getNumbers(OUTPUT_TOUCHING_MATRIX_BOUNDARY_FLAGS).setTo(result, 4);
        }
        if (isOutputNecessary(OUTPUT_FRAME_ID)) {
            getNumbers(OUTPUT_FRAME_ID).setTo(contours.getAllFrameId(-1), 1);
        }
        if (isOutputNecessary(OUTPUT_SORTED_INDEXES_BY_LABEL)) {
            final int[] indexes = new int[contours.numberOfContours()];
            Arrays.setAll(indexes, k -> k);
            contours.sortIndexesByLabels(indexes);
            getNumbers(OUTPUT_SORTED_INDEXES_BY_LABEL).setTo(indexes, 1);
        }
        if (isOutputNecessary(OUTPUT_SORTED_INDEXES_BY_AREA)) {
            final int[] indexes = new int[contours.numberOfContours()];
            Arrays.setAll(indexes, k -> k);
            contours.sortIndexesByPreciseArea(indexes, true);
            getNumbers(OUTPUT_SORTED_INDEXES_BY_AREA).setTo(indexes, 1);
        }
        if (isOutputNecessary(OUTPUT_CONTOUR_OFFSETS)) {
            final int[] result = new int[contours.numberOfContours()];
            IntStream.range(0, (result.length + 255) >>> 8).parallel().forEach(block -> {
                for (int i = block << 8, to = (int) Math.min((long) i + 256, result.length); i < to; i++) {
                    result[i] = contours.getContourOffset(i);
                }
            });
            getNumbers(OUTPUT_CONTOUR_OFFSETS).setTo(result, 1);
        }
        long t2 = debugTime();
        if (LOGGABLE_DEBUG) {
            logDebug(String.format(Locale.US,
                    "%d contours measured in %.3f ms, %.5f mcs/contour%s",
                    contours.numberOfContours(),
                    (t2 - t1) * 1e-6, (t2 - t1) * 1e-3 / (double) contours.numberOfContours(), additionalTiming));
        }
    }

    static byte[] booleansToBytes(boolean[] array) {
        byte[] result = new byte[array.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = array[k] ? (byte) 1 : (byte) 0;
        }
        return result;
    }
}
