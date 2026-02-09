/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.executors.modules.cv.matrices.objects;

import net.algart.arrays.*;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.matrices.BitMultiMatrixFilter;
import net.algart.executors.modules.cv.matrices.objects.binary.components.ConnectedObjectScanningAlgorithm;
import net.algart.math.functions.DividingFunc;
import net.algart.math.functions.Func;
import net.algart.math.functions.LinearFunc;
import net.algart.math.functions.PowerFunc;
import net.algart.matrices.scanning.ConnectedObjectScanner;
import net.algart.matrices.scanning.ConnectivityType;
import net.algart.multimatrix.MultiMatrix2D;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MeasureLabelledObjects extends Executor implements ReadOnlyExecutionInput {
    public static final String INPUT_LABELS = "labels";
    public static final String INPUT_MASK = "mask";
    public static final String OUTPUT_NUMBER_OF_OBJECTS = "number_of_objects";

    public enum BoundaryLineType {
        BOUNDARY_PIXELS,
        BOUNDARY_INTERPIXEL_SEGMENTS;
    }

    public enum ObjectParameter {
        AREA("area"),
        SQRT_AREA("sqrt_area"),
        BOUNDARY("boundary"),
        THICKNESS("thickness"),
        SHAPE_FACTOR("shape_factor"),
        CENTROID("centroid"),
        CONTAINING_RECTANGLE("containing_rectangle");

        final String outputPort;

        ObjectParameter(String outputPort) {
            this.outputPort = outputPort;
        }

        public String outputPort() {
            return outputPort;
        }
    }

    private static final Map<String, ObjectParameter> OUTPUT_STATISTICS = new LinkedHashMap<>();

    static {
        for (ObjectParameter parameter : ObjectParameter.values()) {
            OUTPUT_STATISTICS.put(parameter.outputPort, parameter);
        }
    }

    private boolean autoSplitBitInputIntoConnectedComponents = false;
    private double pixelSize = 1.0;
    private ConnectivityType bitInputConnectivityType = ConnectivityType.STRAIGHT_AND_DIAGONAL;
    private BoundaryLineType boundaryLineType = BoundaryLineType.BOUNDARY_PIXELS;

    public MeasureLabelledObjects() {
        useVisibleResultParameter();
        setDefaultInputMat(INPUT_LABELS);
        setDefaultOutputNumbers(ObjectParameter.AREA.outputPort);
        addInputMat(INPUT_MASK);
        addOutputScalar(OUTPUT_NUMBER_OF_OBJECTS);
        for (ObjectParameter parameter : ObjectParameter.values()) {
            addOutputNumbers(parameter.outputPort);
        }
    }

    public boolean isAutoSplitBitInputIntoConnectedComponents() {
        return autoSplitBitInputIntoConnectedComponents;
    }

    public MeasureLabelledObjects setAutoSplitBitInputIntoConnectedComponents(
            boolean autoSplitBitInputIntoConnectedComponents) {
        this.autoSplitBitInputIntoConnectedComponents = autoSplitBitInputIntoConnectedComponents;
        return this;
    }

    public double getPixelSize() {
        return pixelSize;
    }

    public MeasureLabelledObjects setPixelSize(double pixelSize) {
        this.pixelSize = pixelSize;
        return this;
    }

    public ConnectivityType getBitInputConnectivityType() {
        return bitInputConnectivityType;
    }

    public MeasureLabelledObjects setBitInputConnectivityType(ConnectivityType bitInputConnectivityType) {
        this.bitInputConnectivityType = nonNull(bitInputConnectivityType);
        return this;
    }

    public BoundaryLineType getBoundaryLineType() {
        return boundaryLineType;
    }

    public MeasureLabelledObjects setBoundaryLineType(BoundaryLineType boundaryLineType) {
        this.boundaryLineType = nonNull(boundaryLineType);
        return this;
    }

    @Override
    public void process() {
        final MultiMatrix2D labels = getInputMat(INPUT_LABELS, false).toMultiMatrix2D();
        final MultiMatrix2D mask = getInputMat(INPUT_MASK, true).toMultiMatrix2D();
        final Map<ObjectParameter, SNumbers> resultStatistics =
                convertMap(allOutputContainers(SNumbers.class, true));
        setStartProcessingTimeStamp();
        analyse(resultStatistics, labels, mask);
        setEndProcessingTimeStamp();
    }

    public void analyse(
            final Map<ObjectParameter, SNumbers> results,
            MultiMatrix2D labels,
            final MultiMatrix2D mask) {
        labels.checkDimensionEquality(mask, "labels", "mask");
        Matrix<UpdatableBitArray> maskMatrix =
                mask == null ? null : BitMultiMatrixFilter.toBit(mask.intensityChannel());
        if (labels.elementType() == boolean.class && autoSplitBitInputIntoConnectedComponents) {
            analyseConnectedComponents(
                    results,
                    labels.channel(0).cast(BitArray.class),
                    maskMatrix);
            return;
        }
        if (mask != null) {
            labels = labels.min(mask.nonZeroRGB());
        }
        final int[] labelsArray = labels.channel(0).toInt();
        int numberOfObjects = 0;
        for (int v : labelsArray) {
            if (v > numberOfObjects) {
                numberOfObjects = v;
            }
        }
        if (LOGGABLE_DEBUG) {
            logDebug("Measuring " + numberOfObjects + " labelled objects of " + labels);
        }
        getScalar(OUTPUT_NUMBER_OF_OBJECTS).setTo(numberOfObjects);
        int[] cardinalities = null;
        if (results.containsKey(ObjectParameter.AREA)
                || results.containsKey(ObjectParameter.SQRT_AREA)
                || results.containsKey(ObjectParameter.THICKNESS)
                || results.containsKey(ObjectParameter.SHAPE_FACTOR)
                || results.containsKey(ObjectParameter.CENTROID)) {
            cardinalities = new int[numberOfObjects];
            for (int v : labelsArray) {
                if (v > 0) {
                    cardinalities[v - 1]++;
                }
            }
            if (results.containsKey(ObjectParameter.AREA)) {
                final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(cardinalities.length);
                Arrays.applyFunc(LinearFunc.getInstance(0.0, pixelSize * pixelSize),
                        result, IntArray.as(cardinalities));
                results.get(ObjectParameter.AREA).setTo(result, 1);
            }
            if (results.containsKey(ObjectParameter.SQRT_AREA)) {
                final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(cardinalities.length);
                Arrays.applyFunc(PowerFunc.getInstance(0.5, pixelSize),
                        result, IntArray.as(cardinalities));
                results.get(ObjectParameter.SQRT_AREA).setTo(result, 1);
            }
        }
        final int dimX = (int) labels.dimX();
        final int dimY = (int) labels.dimY();
        if (results.containsKey(ObjectParameter.BOUNDARY)
                || results.containsKey(ObjectParameter.THICKNESS)
                || results.containsKey(ObjectParameter.SHAPE_FACTOR)) {
            final int[] boundaries = new int[numberOfObjects];
            for (int y = 0, disp = 0; y < dimY; y++) {
                for (int x = 0; x < dimX; x++, disp++) {
                    int label = labelsArray[disp];
                    if (label > 0) {
                        switch (boundaryLineType) {
                            case BOUNDARY_PIXELS -> {
                                if (x == 0 || labelsArray[disp - 1] != label
                                        || x == dimX - 1 || labelsArray[disp + 1] != label
                                        || y == 0 || labelsArray[disp - dimX] != label
                                        || y == dimY - 1 || labelsArray[disp + dimX] != label) {
                                    boundaries[label - 1]++;
                                }
                            }
                            case BOUNDARY_INTERPIXEL_SEGMENTS -> {
                                int count = 0;
                                if (x == 0 || labelsArray[disp - 1] != label) {
                                    count++;
                                }
                                if (x == dimX - 1 || labelsArray[disp + 1] != label) {
                                    count++;
                                }
                                if (y == 0 || labelsArray[disp - dimX] != label) {
                                    count++;
                                }
                                if (y == dimY - 1 || labelsArray[disp + dimX] != label) {
                                    count++;
                                }
                                boundaries[label - 1] += count;
                            }
                            default -> throw new AssertionError("Unsupported boundary line type: "
                                    + boundaryLineType);
                        }
                    }
                }
            }
            if (results.containsKey(ObjectParameter.BOUNDARY)) {
                final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(boundaries.length);
                Arrays.applyFunc(LinearFunc.getInstance(0.0, pixelSize),
                        result, IntArray.as(boundaries));
                results.get(ObjectParameter.BOUNDARY).setTo(result, 1);
            }
            if (results.containsKey(ObjectParameter.THICKNESS)) {
                assert cardinalities != null;
                final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(boundaries.length);
                Arrays.applyFunc(DividingFunc.getInstance(2.0 * pixelSize),
                        result,
                        IntArray.as(cardinalities),
                        IntArray.as(boundaries));
                results.get(ObjectParameter.THICKNESS).setTo(result, 1);
            }
            if (results.containsKey(ObjectParameter.SHAPE_FACTOR)) {
                assert cardinalities != null;
                final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(boundaries.length);
                Arrays.applyFunc(DividingFunc.getInstance(2 * StrictMath.sqrt(Math.PI)),
                        result,
                        Arrays.asFuncArray(PowerFunc.getInstance(0.5), DoubleArray.class,
                                IntArray.as(cardinalities)),
                        IntArray.as(boundaries));
                results.get(ObjectParameter.SHAPE_FACTOR).setTo(result, 1);
            }
        }
        if (results.containsKey(ObjectParameter.CENTROID)) {
            assert cardinalities != null;
            if (2L * (long) numberOfObjects > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("numberOfObjects = " + numberOfObjects + " >= 2^31 / 2");
            }
            final double[] centroids = new double[2 * numberOfObjects];
            for (int y = 0, disp = 0; y < dimY; y++) {
                for (int x = 0; x < dimX; x++, disp++) {
                    final int v = labelsArray[disp];
                    if (v > 0) {
                        centroids[2 * (v - 1)] += x;
                        centroids[2 * (v - 1) + 1] += y;
                    }
                }
            }
            for (int k = 0; k < numberOfObjects; k++) {
                centroids[2 * k] /= cardinalities[k];
                centroids[2 * k + 1] /= cardinalities[k];
            }
            final UpdatablePNumberArray result = Arrays.SMM.newFloatArray(centroids.length);
            Arrays.applyFunc(LinearFunc.getInstance(0.0, pixelSize), result, DoubleArray.as(centroids));
            results.get(ObjectParameter.CENTROID).setTo(result, 2);
        }
        if (results.containsKey(ObjectParameter.CONTAINING_RECTANGLE)) {
            if (4L * (long) numberOfObjects > Integer.MAX_VALUE) {
                throw new TooLargeArrayException("numberOfObjects = " + numberOfObjects + " >= 2^31 / 4");
            }
            final int[] minX = new int[numberOfObjects];
            final int[] minY = new int[numberOfObjects];
            final int[] maxX = new int[numberOfObjects];
            final int[] maxY = new int[numberOfObjects];
            JArrays.fill(minX, Integer.MAX_VALUE);
            JArrays.fill(minY, Integer.MAX_VALUE);
            for (int y = 0, disp = 0; y < dimY; y++) {
                for (int x = 0; x < dimX; x++, disp++) {
                    final int v = labelsArray[disp];
                    if (v > 0) {
                        minX[v - 1] = Math.min(minX[v - 1], x);
                        minY[v - 1] = Math.min(minY[v - 1], y);
                        maxX[v - 1] = Math.max(maxX[v - 1], x);
                        maxY[v - 1] = Math.max(maxY[v - 1], y);
                    }
                }
            }
            final float[] rectangles = new float[4 * numberOfObjects];
            for (int k = 0; k < numberOfObjects; k++) {
                rectangles[4 * k] = (float) (0.5 * pixelSize * (minX[k] + maxX[k]));
                rectangles[4 * k + 1] = (float) (0.5 * pixelSize * (minY[k] + maxY[k]));
                rectangles[4 * k + 2] = (float) (pixelSize * (maxX[k] - minX[k] + 1));
                rectangles[4 * k + 3] = (float) (pixelSize * (maxY[k] - minY[k] + 1));
            }
            results.get(ObjectParameter.CONTAINING_RECTANGLE).setTo(rectangles, 4);
        }
    }

    public void analyseConnectedComponents(
            final Map<ObjectParameter, SNumbers> results,
            Matrix<? extends BitArray> source,
            Matrix<? extends BitArray> mask) {
        if (mask != null) {
            source = Matrices.asFuncMatrix(Func.MIN, BitArray.class, source, mask);
        }
        final Matrix<UpdatableBitArray> objects = BitMultiMatrixFilter.cloneBit(
                source.subMatrix(-1, -1, source.dimX() + 1, source.dimY() + 1,
                        Matrix.ContinuationMode.getConstantMode(0)));
        // - extending by 1 pixel for using unchecked ConnectedObjectScanningAlgorithm
        final UpdatableBitArray pixels = objects.array().updatableClone(Arrays.SMM);
        // - cloning is necessary to correctly count pixels: the scanner will clear visited pixels in "objects" matrix
        final ConnectedObjectScanner scanner = ConnectedObjectScanningAlgorithm.QUICKEN.connectedObjectScanner(
                objects, bitInputConnectivityType, false);
        final MutableFloatArray areas = Arrays.SMM.newFloatArray(0);
        final MutableFloatArray sqrtAreas = results.containsKey(ObjectParameter.SQRT_AREA) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final MutableFloatArray boundaries = results.containsKey(ObjectParameter.BOUNDARY) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final MutableFloatArray thicknesses = results.containsKey(ObjectParameter.THICKNESS) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final MutableFloatArray shapeFactors = results.containsKey(ObjectParameter.SHAPE_FACTOR) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final MutableFloatArray centroids = results.containsKey(ObjectParameter.CENTROID) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final MutableFloatArray rectangles = results.containsKey(ObjectParameter.CONTAINING_RECTANGLE) ?
                Arrays.SMM.newFloatArray(0) :
                null;
        final long dimX = objects.dimX();
        final long dimY = objects.dimY();

        class BoundariesAndCentroidAndRectangleCalculator implements ConnectedObjectScanner.ElementVisitor {
            final long[] coordinatesInMatrix = new long[objects.dimCount()];
            int minX, minY, maxX, maxY;
            double sumX, sumY;
            long countBoundary;

            public void visit(long[] coordinatesInMatrix, long indexInArray) {
                if (coordinatesInMatrix == null) {
                    coordinatesInMatrix = objects.coordinates(indexInArray, this.coordinatesInMatrix);
                }
                final int x = (int) coordinatesInMatrix[0] - 1;
                final int y = (int) coordinatesInMatrix[1] - 1;
                // - subtracting 1 to compensate the starting extending
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                sumX += x;
                sumY += y;
                switch (boundaryLineType) {
                    case BOUNDARY_PIXELS: {
                        if (x == 0 || !pixels.getBit(indexInArray - 1)
                                || x == dimX - 1 || !pixels.getBit(indexInArray + 1)
                                || y == 0 || !pixels.getBit(indexInArray - dimX)
                                || y == dimY - 1 || !pixels.getBit(indexInArray + dimX)) {
                            countBoundary++;
                        }
                        break;
                    }
                    case BOUNDARY_INTERPIXEL_SEGMENTS: {
                        if (x == 0 || !pixels.getBit(indexInArray - 1)) {
                            countBoundary++;
                        }
                        if (x == dimX - 1 || !pixels.getBit(indexInArray + 1)) {
                            countBoundary++;
                        }
                        if (y == 0 || !pixels.getBit(indexInArray - dimX)) {
                            countBoundary++;
                        }
                        if (y == dimY - 1 || !pixels.getBit(indexInArray + dimX)) {
                            countBoundary++;
                        }
                        break;
                    }
                    default:
                        throw new AssertionError("Unsupported boundary line type: "
                                + boundaryLineType);
                }
            }

            public void init() {
                minX = Integer.MAX_VALUE;
                minY = Integer.MAX_VALUE;
                maxX = Integer.MIN_VALUE;
                maxY = Integer.MIN_VALUE;
                sumX = 0.0;
                sumY = 0.0;
                countBoundary = 0;
            }
        }

        long pixelCounter = 0;
        long[] coordinates = new long[objects.dimCount()]; // zero-filled
        final BoundariesAndCentroidAndRectangleCalculator calculator =
                centroids != null || rectangles != null || boundaries != null
                        || thicknesses != null || shapeFactors != null ?
                        new BoundariesAndCentroidAndRectangleCalculator() : null;
        while (scanner.nextUnitBit(coordinates)) {
            if (calculator != null) {
                calculator.init();
            }
            final long cardinality = scanner.clear(null, calculator, coordinates, false);
            pixelCounter += cardinality;
            areas.addDouble(cardinality * (pixelSize * pixelSize));
            if (sqrtAreas != null) {
                sqrtAreas.addDouble(Math.sqrt(cardinality) * pixelSize);
            }
            if (boundaries != null) {
                boundaries.addDouble((double) calculator.countBoundary * pixelSize);
            }
            if (thicknesses != null) {
                thicknesses.addDouble(2.0 * (double) cardinality * pixelSize
                        / (double) calculator.countBoundary);
            }
            if (shapeFactors != null) {
                shapeFactors.addDouble(2.0 * Math.sqrt(Math.PI * cardinality)
                        / (double) calculator.countBoundary);
            }
            if (centroids != null) {
                centroids.addDouble(pixelSize * calculator.sumX / cardinality);
                centroids.addDouble(pixelSize * calculator.sumY / cardinality);
            }
            if (rectangles != null) {
                rectangles.addDouble(0.5 * pixelSize * (calculator.minX + calculator.maxX));
                rectangles.addDouble(0.5 * pixelSize * (calculator.minY + calculator.maxY));
                rectangles.addDouble(pixelSize * (calculator.maxX - calculator.minX + 1));
                rectangles.addDouble(pixelSize * (calculator.maxY - calculator.minY + 1));
            }
        }

        getScalar(OUTPUT_NUMBER_OF_OBJECTS).setTo(areas.length());
        if (LOGGABLE_DEBUG) {
            logDebug("Measuring " + areas.length() + " connected components "
                    + " (" + pixelCounter + " pixels) at " + source);
        }
        results.computeIfAbsent(ObjectParameter.AREA, k -> new SNumbers()).setTo(areas, 1);
        if (sqrtAreas != null) {
            results.get(ObjectParameter.SQRT_AREA).setTo(sqrtAreas, 1);
        }
        if (boundaries != null) {
            results.get(ObjectParameter.BOUNDARY).setTo(boundaries, 1);
        }
        if (thicknesses != null) {
            results.get(ObjectParameter.THICKNESS).setTo(thicknesses, 1);
        }
        if (shapeFactors != null) {
            results.get(ObjectParameter.SHAPE_FACTOR).setTo(shapeFactors, 1);
        }
        if (centroids != null) {
            results.get(ObjectParameter.CENTROID).setTo(centroids, 2);
        }
        if (rectangles != null) {
            results.get(ObjectParameter.CONTAINING_RECTANGLE).setTo(rectangles, 4);
        }
    }

    public static Map<ObjectParameter, SNumbers> convertMap(Map<String, SNumbers> statistics) {
        Map<ObjectParameter, SNumbers> result = new LinkedHashMap<>();
        statistics.forEach((String s, SNumbers numbers) -> result.put(OUTPUT_STATISTICS.get(s), numbers));
        return result;
    }
}
