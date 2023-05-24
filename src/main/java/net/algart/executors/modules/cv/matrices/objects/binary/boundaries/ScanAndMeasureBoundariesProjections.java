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

import net.algart.executors.api.data.SNumbers;
import net.algart.arrays.Arrays;
import net.algart.arrays.*;
import net.algart.matrices.scanning.Boundary2DProjectionMeasurer;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.*;

public final class ScanAndMeasureBoundariesProjections extends AbstractScanAndMeasureBoundaries {
    public static final String OUTPUT_AREA = "area";
    public static final String OUTPUT_PERIMETER = "perimeter";
    public static final String OUTPUT_ALL_PROJECTION = "all_projections";
    public static final String OUTPUT_SELECTED_PROJECTION = "selected_projection";
    public static final String OUTPUT_MEAN_PROJECTION = "mean_projection";
    public static final String OUTPUT_MAX_PROJECTION = "max_projection";
    public static final String OUTPUT_MIN_PROJECTION = "min_projection";
    public static final String OUTPUT_MAX_SIZE_RELATION = "max_size_relation";
    public static final String OUTPUT_MIN_CIRCUMSCRIBED_SQUARE_SIDE = "min_circumscribed_square_side";
    public static final String OUTPUT_SHAPE_FACTOR = "shape_factor";
    public static final String OUTPUT_COMPACT_FACTOR = "compact_factor";
    public static final String OUTPUT_IRREGULARITY_FACTOR = "irregularity_factor";
    // The last parameter is always available for any measurer:
    public static final String OUTPUT_NESTING_LEVEL = ScanAndMeasureBoundaries.OUTPUT_NESTING_LEVEL;

    private static final Map<String, ProjectionParameter> OUTPUT_STATISTICS = new LinkedHashMap<>();

    static {
        OUTPUT_STATISTICS.put(OUTPUT_AREA, ProjectionParameter.AREA);
        OUTPUT_STATISTICS.put(OUTPUT_PERIMETER, ProjectionParameter.PERIMETER);
        OUTPUT_STATISTICS.put(OUTPUT_ALL_PROJECTION, ProjectionParameter.ALL_PROJECTIONS);
        OUTPUT_STATISTICS.put(OUTPUT_SELECTED_PROJECTION, ProjectionParameter.SELECTED_PROJECTION);
        OUTPUT_STATISTICS.put(OUTPUT_MEAN_PROJECTION, ProjectionParameter.MEAN_PROJECTION);
        OUTPUT_STATISTICS.put(OUTPUT_MAX_PROJECTION, ProjectionParameter.MAX_PROJECTION);
        OUTPUT_STATISTICS.put(OUTPUT_MIN_PROJECTION, ProjectionParameter.MIN_PROJECTION);
        OUTPUT_STATISTICS.put(OUTPUT_MAX_SIZE_RELATION, ProjectionParameter.MAX_SIZE_RELATION);
        OUTPUT_STATISTICS.put(OUTPUT_MIN_CIRCUMSCRIBED_SQUARE_SIDE,
                ProjectionParameter.MIN_CIRCUMSCRIBED_SQUARE_SIDE);
        OUTPUT_STATISTICS.put(OUTPUT_SHAPE_FACTOR, ProjectionParameter.SHAPE_FACTOR);
        OUTPUT_STATISTICS.put(OUTPUT_COMPACT_FACTOR, ProjectionParameter.COMPACT_FACTOR);
        OUTPUT_STATISTICS.put(OUTPUT_IRREGULARITY_FACTOR, ProjectionParameter.IRREGULARITY_FACTOR);
        OUTPUT_STATISTICS.put(OUTPUT_NESTING_LEVEL, ProjectionParameter.NESTING_LEVEL);
    }

    public ScanAndMeasureBoundariesProjections() {
        for (String port : OUTPUT_STATISTICS.keySet()) {
            addOutputNumbers(port);
        }
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_OBJECT_LABEL);
        addOutputNumbers(ScanAndMeasureBoundaries.OUTPUT_INTERNAL_BOUNDARY);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    private SecondProjectionValue secondProjectionValue = SecondProjectionValue.ORTHOGONAL_PROJECTION_DIVIDED_BY_THIS;
    private ThirdProjectionValue thirdProjectionValue = ThirdProjectionValue.NONE;
    private double startDirectionAngleInDegree = 0.0;
    private int numberOfDirections = 2;
    // - should be even for MIN_CIRCUMSCRIBED_SQUARE_SIDE: in other case, it will be not precise square, but rhombus


    public SecondProjectionValue getSecondProjectionValue() {
        return secondProjectionValue;
    }

    public void setSecondProjectionValue(SecondProjectionValue secondProjectionValue) {
        this.secondProjectionValue = nonNull(secondProjectionValue);
    }

    public ThirdProjectionValue getThirdProjectionValue() {
        return thirdProjectionValue;
    }

    public void setThirdProjectionValue(ThirdProjectionValue thirdProjectionValue) {
        this.thirdProjectionValue = nonNull(thirdProjectionValue);
    }

    public double getStartDirectionAngleInDegree() {
        return startDirectionAngleInDegree;
    }

    public void setStartDirectionAngleInDegree(double startDirectionAngleInDegree) {
        this.startDirectionAngleInDegree = startDirectionAngleInDegree;
    }

    public int getNumberOfDirections() {
        return numberOfDirections;
    }

    public void setNumberOfDirections(int numberOfDirections) {
        this.numberOfDirections = inRange(numberOfDirections, 2, 65536);
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final Map<ProjectionParameter, SNumbers> resultStatistics = convertMap(
                allOutputContainers(SNumbers.class, true));
        return analyse(
                resultStatistics,
                BoundariesScanner.toObjects(source, objectsInterpretation.binaryOnly()),
                isOutputNecessary(defaultOutputPortName()));
    }

    public MultiMatrix2D analyse(
            final Map<ProjectionParameter, SNumbers> resultStatistics,
            final Matrix<? extends PFixedArray> objects,
            final boolean resultLabelsRequired) {
        final Set<ProjectionParameter> parameters = resultStatistics.keySet();
        ProjectionParameter[] parametersArray = parameters.toArray(new ProjectionParameter[0]);
        MutablePNumberArray[] statisticsArray = new MutablePNumberArray[resultStatistics.size()];
        for (int k = 0; k < parametersArray.length; k++) {
            statisticsArray[k] = parametersArray[k] == ProjectionParameter.NESTING_LEVEL ?
                    Arrays.SMM.newIntArray(0) : Arrays.SMM.newFloatArray(0);
        }
        final MutableIntArray objectLabelArray = Arrays.SMM.newEmptyIntArray();
        final MutableBitArray internalBoundaryFlags = Arrays.SMM.newEmptyBitArray();
        final BoundariesScanner scanner = new BoundariesScanner(
                objects,
                getConnectivityType(),
                getBoundaryType(),
                ProjectionParameter.needSecondBuffer(parameters),
                resultLabelsRequired,
                getMaxLabelLevelOrMaxValue());
        double normalizedStartAngleInDegree = startDirectionAngleInDegree % 180.0;
        if (normalizedStartAngleInDegree < 0.0) {
            normalizedStartAngleInDegree += 180.0;        }
        final Boundary2DProjectionMeasurer measurer = Boundary2DProjectionMeasurer.getInstance(
                scanner.getBoundaryScanner(),
                getContourLineType(),
                Math.toRadians(normalizedStartAngleInDegree),
                numberOfDirections);
        scanner.setBoundaryMeasurer(measurer);
        scanner.setProcessBackgroundAsObject(getObjectsInterpretation().processBackgroundAsObject());
        long count = 0;
        while (scanner.nextBoundary()) {
            scanner.scanAndProcess();
            if (scanner.needToAnalyseThisBoundary()) {
                objectLabelArray.pushInt(scanner.currentLabel());
                internalBoundaryFlags.pushBit(scanner.internalBoundary());
                for (int k = 0; k < parametersArray.length; k++) {
                    parametersArray[k].getStatistics(
                            statisticsArray[k],
                            measurer,
                            getPixelSize(),
                            secondProjectionValue,
                            thirdProjectionValue);
                }
                count++;
            }
        }
        ScanAndMeasureBoundaries.uploadSimpleOutputs(this, scanner, objectLabelArray, internalBoundaryFlags);
        logDebug(() -> "Scanned projections of " + scanner.objectCounter() + " boundaries, "
                + scanner.sideCounter() + " pixel sides "
                + "for calculating " + parametersArray.length + " parameters " + parameters + " at " + objects);
        for (int k = 0; k < parametersArray.length; k++) {
            final ProjectionParameter p = parametersArray[k];
            final MutablePNumberArray a = statisticsArray[k];
            assert a.length() % count == 0 : "Non-even number of statistics elements";
            resultStatistics.get(p).setTo(a, (int) (a.length() / count));
        }
        return ScanAndMeasureBoundaries.getLabels(scanner);
    }


    private static Map<ProjectionParameter, SNumbers> convertMap(Map<String, SNumbers> statistics) {
        Map<ProjectionParameter, SNumbers> result = new LinkedHashMap<>();
        statistics.forEach((s, numbers) -> {
            final ProjectionParameter parameter = OUTPUT_STATISTICS.get(s);
            if (parameter != null) {
                result.put(parameter, numbers);
            }
        });
        return result;
    }
}
