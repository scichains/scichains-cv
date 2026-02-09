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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.contours.Contours;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SNumbers;
import net.algart.executors.modules.core.common.numbers.NumbersFilter;

import java.util.Locale;

public final class UnpackContours extends NumbersFilter implements ReadOnlyExecutionInput {
    public static final String INPUT_CONTOURS = "contours";
    public static final String OUTPUT_UNPACKED_CONTOURS = "contours";

    private boolean needToProcessDiagonals = true;
    private boolean cacheLastContours = false;

    private volatile Contours lastPacked = null;
    private volatile Contours lastUnpacked = null;

    private final Object lock = new Object();

    public UnpackContours() {
        setDefaultInputNumbers(INPUT_CONTOURS);
        setDefaultOutputNumbers(OUTPUT_UNPACKED_CONTOURS);
        addOutputScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS);
    }

    public boolean isNeedToProcessDiagonals() {
        return needToProcessDiagonals;
    }

    public UnpackContours setNeedToProcessDiagonals(boolean needToProcessDiagonals) {
        synchronized (lock) {
            if (needToProcessDiagonals != this.needToProcessDiagonals) {
                clearCache();
                this.needToProcessDiagonals = needToProcessDiagonals;
            }
        }
        return this;
    }

    public boolean isCacheLastContours() {
        return cacheLastContours;
    }

    public UnpackContours setCacheLastContours(boolean cacheLastContours) {
        this.cacheLastContours = cacheLastContours;
        return this;
    }

    public Contours unpackContours(int[] array) {
        return cacheLastContours ? unpackContoursWithCaching(array) : unpackContoursWithoutCaching(array);
    }

    public Contours unpackContoursWithCaching(int[] serializedContours) {
        synchronized (lock) {
            if (lastPacked == null || !lastPacked.equalsToSerialized(serializedContours)) {
                final Contours contours = Contours.deserialize(serializedContours);
                lastPacked = contours;
                lastUnpacked = unpackContoursWithoutCaching(contours);
            }
            return lastUnpacked;
        }
    }

    public Contours unpackContoursWithoutCaching(int[] serializedContours) {
        return unpackContoursWithoutCaching(Contours.deserialize(serializedContours));
    }

    public Contours unpackContours(Contours contours) {
        return cacheLastContours ? unpackContoursWithCaching(contours) : unpackContoursWithoutCaching(contours);
    }

    public Contours unpackContoursWithCaching(Contours contours) {
        synchronized (lock) {
            if (lastPacked == null || !lastPacked.equals(contours)) {
                lastPacked = contours;
                lastUnpacked = unpackContoursWithoutCaching(contours);
            }
            return lastUnpacked;
        }
    }

    public Contours unpackContoursWithoutCaching(Contours contours) {
        return contours.unpackContours(needToProcessDiagonals);
    }

    @Override
    protected SNumbers processNumbers(SNumbers source) {
        final int[] array = source.toIntArrayOrReference();
        // - maximal performance for a case if int[]
        setStartProcessingTimeStamp();
        long t1 = debugTime();
        final Contours result = unpackContours(array);
        long t2 = debugTime();
        setEndProcessingTimeStamp();
        logDebug(() -> String.format(Locale.US,
                "%d contours unpacked in %.3f ms",
                result.numberOfContours(), (t2 - t1) * 1e-6));
        getScalar(ScanAndMeasureBoundaries.OUTPUT_NUMBER_OF_OBJECTS).setTo(result.numberOfContours());
        return SNumbers.of(result);
    }

    private void clearCache() {
        lastPacked = null;
        lastUnpacked = null;
    }

}
