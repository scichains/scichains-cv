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

import net.algart.arrays.*;
import net.algart.math.functions.Func;
import net.algart.matrices.scanning.Boundary2DSimpleMeasurer;
import net.algart.multimatrix.MultiMatrix;
import net.algart.multimatrix.MultiMatrix2D;
import net.algart.executors.api.Executor;
import net.algart.executors.api.data.SNumbers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ScanAndMeasureBoundaries extends AbstractScanAndMeasureBoundaries {
    public static final String OUTPUT_OBJECT_LABEL = "object_label";
    public static final String OUTPUT_AREA = "area";
    public static final String OUTPUT_SQRT_AREA = "sqrt_area";
    public static final String OUTPUT_PERIMETER = "perimeter";
    public static final String OUTPUT_SIZE = "size";
    public static final String OUTPUT_SHAPE_FACTOR = "shape_factor";
    public static final String OUTPUT_CONTAINING_OCTAGON_AREA = "containing_octagon_area";
    public static final String OUTPUT_CENTROID = "centroid";
    public static final String OUTPUT_CONTAINING_RECTANGLE = "containing_rectangle";
    public static final String OUTPUT_INTERNAL_BOUNDARY = "internal_boundary";
    public static final String OUTPUT_NESTING_LEVEL = "nesting_level";
    public static final String OUTPUT_NUMBER_OF_OBJECTS = "number_of_objects";

    private static final Map<String, BoundaryParameter> OUTPUT_STATISTICS = new LinkedHashMap<>();

    static {
        OUTPUT_STATISTICS.put(OUTPUT_AREA, BoundaryParameter.AREA);
        OUTPUT_STATISTICS.put(OUTPUT_SQRT_AREA, BoundaryParameter.SQRT_AREA);
        OUTPUT_STATISTICS.put(OUTPUT_PERIMETER, BoundaryParameter.PERIMETER);
        OUTPUT_STATISTICS.put(OUTPUT_SIZE, BoundaryParameter.SIZE);
        OUTPUT_STATISTICS.put(OUTPUT_SHAPE_FACTOR, BoundaryParameter.SHAPE_FACTOR);
        OUTPUT_STATISTICS.put(OUTPUT_CONTAINING_OCTAGON_AREA, BoundaryParameter.CONTAINING_OCTAGON_AREA);
        OUTPUT_STATISTICS.put(OUTPUT_CENTROID, BoundaryParameter.CENTROID);
        OUTPUT_STATISTICS.put(OUTPUT_CONTAINING_RECTANGLE, BoundaryParameter.CONTAINING_RECTANGLE);
        OUTPUT_STATISTICS.put(OUTPUT_NESTING_LEVEL, BoundaryParameter.NESTING_LEVEL);
    }

    public ScanAndMeasureBoundaries() {
        for (String port : OUTPUT_STATISTICS.keySet()) {
            addOutputNumbers(port);
        }
        addOutputNumbers(OUTPUT_OBJECT_LABEL);
        addOutputNumbers(OUTPUT_INTERNAL_BOUNDARY);
        addOutputScalar(OUTPUT_NUMBER_OF_OBJECTS);
    }

    @Override
    public MultiMatrix2D process(MultiMatrix2D source) {
        final Map<BoundaryParameter, SNumbers> resultStatistics = convertMap(
                allOutputContainers(SNumbers.class, true));
        return analyse(
                resultStatistics,
                BoundariesScanner.toObjects(source, objectsInterpretation.binaryOnly()),
                isOutputNecessary(defaultOutputPortName()));
    }

    public MultiMatrix2D analyse(
            final Map<BoundaryParameter, SNumbers> resultStatistics,
            final Matrix<? extends PFixedArray> objects,
            final boolean resultLabelsRequired) {
        final Set<BoundaryParameter> parameters = resultStatistics.keySet();
        BoundaryParameter[] parametersArray = parameters.toArray(new BoundaryParameter[0]);
        MutablePNumberArray[] statisticsArray = new MutablePNumberArray[resultStatistics.size()];
        for (int k = 0; k < parametersArray.length; k++) {
            statisticsArray[k] = parametersArray[k] == BoundaryParameter.NESTING_LEVEL ?
                    Arrays.SMM.newIntArray(0) : Arrays.SMM.newFloatArray(0);
        }
        final MutableIntArray objectLabelArray = Arrays.SMM.newEmptyIntArray();
        final MutableBitArray internalBoundaryFlags = Arrays.SMM.newEmptyBitArray();
        final BoundariesScanner scanner = new BoundariesScanner(
                objects,
                getConnectivityType(),
                getBoundaryType(),
                BoundaryParameter.needSecondBuffer(parameters),
                resultLabelsRequired,
                getMaxLabelLevelOrMaxValue());
        final Boundary2DSimpleMeasurer measurer = Boundary2DSimpleMeasurer.getInstance(
                scanner.getBoundaryScanner(),
                getContourLineType(),
                BoundaryParameter.objectParameters(parameters));
        scanner.setBoundaryMeasurer(measurer);
        scanner.setProcessBackgroundAsObject(getObjectsInterpretation().processBackgroundAsObject());
        while (scanner.nextBoundary()) {
            scanner.scanAndProcess();
            if (scanner.needToAnalyseThisBoundary()) {
                objectLabelArray.pushInt(scanner.currentLabel());
                internalBoundaryFlags.pushBit(scanner.internalBoundary());
                for (int k = 0; k < parametersArray.length; k++) {
                    parametersArray[k].getStatistics(statisticsArray[k], measurer, getPixelSize());
                }
            }
        }
        uploadSimpleOutputs(this, scanner, objectLabelArray, internalBoundaryFlags);
        logDebug(() -> "Scanned " + scanner.objectCounter() + " boundaries, "
                + scanner.sideCounter() + " pixel sides "
                + "for calculating " + parametersArray.length + " parameters " + parameters + " at " + objects);
        for (int k = 0; k < parametersArray.length; k++) {
            final BoundaryParameter p = parametersArray[k];
            final int blockLength = p.parameterLength();
            resultStatistics.get(p).setTo(statisticsArray[k], blockLength);
        }
        return getLabels(scanner);
    }

    static void uploadSimpleOutputs(
            Executor executor,
            BoundariesScanner scanner,
            IntArray objectLabelArray,
            BitArray internalBoundaryFlags) {
        if (executor.isOutputNecessary(OUTPUT_OBJECT_LABEL)) {
            executor.getNumbers(OUTPUT_OBJECT_LABEL).setTo(objectLabelArray, 1);
        }
        if (executor.isOutputNecessary(OUTPUT_INTERNAL_BOUNDARY)) {
            executor.getNumbers(OUTPUT_INTERNAL_BOUNDARY).setTo(Arrays.asFuncArray(
                    Func.IDENTITY, ByteArray.class, internalBoundaryFlags), 1);
        }
        executor.getScalar(OUTPUT_NUMBER_OF_OBJECTS).setTo(scanner.objectCounter());
    }

    static MultiMatrix2D getLabels(BoundariesScanner scanner) {
        final Matrix<? extends PArray> labels = scanner.getLabels();
        return labels == null ? null : MultiMatrix.valueOf2DMono(labels);
    }

    private static Map<BoundaryParameter, SNumbers> convertMap(Map<String, SNumbers> statistics) {
        Map<BoundaryParameter, SNumbers> result = new LinkedHashMap<>();
        statistics.forEach((s, numbers) -> {
            final BoundaryParameter parameter = OUTPUT_STATISTICS.get(s);
            if (parameter != null) {
                result.put(parameter, numbers);
            }
        });
        return result;
    }
}
