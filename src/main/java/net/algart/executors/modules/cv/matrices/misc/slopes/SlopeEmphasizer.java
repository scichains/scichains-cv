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

package net.algart.executors.modules.cv.matrices.misc.slopes;

import java.util.Objects;

public class SlopeEmphasizer {
    public interface ForType {
        void emphasize(Object values, int offset, int count);

        void emphasize(Object values, int offset, int count, int step);
    }

    private double minimalChange = 0.0;
    private long minimalChangeLong = 0;
    private int slopeWidth = 1;
    private boolean allowLongSlopes = true;
    private boolean processAscending = true;
    private boolean processDescending = true;
    private boolean exactHalfSum = true;
    // Note: procedure FExactXorYBoundaries from pascal AlgART libraries implemented the mode exactHalfSum = false
    private int exactHalfSum01 = 1;

    private SlopeEmphasizer() {
    }

    public static SlopeEmphasizer getInstance() {
        return new SlopeEmphasizer();
    }

    public ForType forElementType(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType");
        if (elementType == byte.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int offset, int count) {
                    SlopeEmphasizer.this.emphasize((byte[]) values, offset, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((byte[]) values, offset, count, step);
                }
            };
        } else if (elementType == short.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int offset, int count) {
                    SlopeEmphasizer.this.emphasize((short[]) values, offset, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((short[]) values, offset, count, step);
                }
            };
        } else if (elementType == int.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int offset, int count) {
                    SlopeEmphasizer.this.emphasize((int[]) values, offset, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((int[]) values, offset, count, step);
                }
            };
        } else if (elementType == long.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int offset, int count) {
                    SlopeEmphasizer.this.emphasize((long[]) values, offset, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((long[]) values, offset, count, step);
                }
            };
        } else if (elementType == float.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int offset, int count) {
                    SlopeEmphasizer.this.emphasize((float[]) values, offset, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((float[]) values, offset, count, step);
                }
            };
        } else if (elementType == double.class) {
            return new ForType() {
                @Override
                public void emphasize(Object values, int from, int count) {
                    SlopeEmphasizer.this.emphasize((double[]) values, from, count);
                }

                @Override
                public void emphasize(Object values, int offset, int count, int step) {
                    SlopeEmphasizer.this.emphasize((double[]) values, offset, count, step);
                }
            };
        } else {
            throw new IllegalArgumentException("The element type " + elementType + " is not supported");
        }
    }

    public double getMinimalChange() {
        return minimalChange;
    }

    public SlopeEmphasizer setMinimalChange(double minimalChange) {
        if (minimalChange < 0.0) {
            throw new IllegalArgumentException("Negative minimalChange = " + minimalChange);
        }
        this.minimalChange = minimalChange;
        this.minimalChangeLong = (long) minimalChange;
        return this;
    }

    public int getSlopeWidth() {
        return slopeWidth;
    }

    public SlopeEmphasizer setSlopeWidth(int slopeWidth) {
        if (slopeWidth <= 0) {
            throw new IllegalArgumentException("Zero or negative slopeWidth = " + slopeWidth);
        }
        this.slopeWidth = slopeWidth;
        return this;
    }

    public boolean isAllowLongSlopes() {
        return allowLongSlopes;
    }

    public SlopeEmphasizer setAllowLongSlopes(boolean allowLongSlopes) {
        this.allowLongSlopes = allowLongSlopes;
        return this;
    }

    public boolean isProcessAscending() {
        return processAscending;
    }

    public SlopeEmphasizer setProcessAscending(boolean processAscending) {
        this.processAscending = processAscending;
        return this;
    }

    public boolean isProcessDescending() {
        return processDescending;
    }

    public SlopeEmphasizer setProcessDescending(boolean processDescending) {
        this.processDescending = processDescending;
        return this;
    }

    public boolean isExactHalfSum() {
        return exactHalfSum;
    }

    public SlopeEmphasizer setExactHalfSum(boolean exactHalfSum) {
        this.exactHalfSum = exactHalfSum;
        this.exactHalfSum01 = exactHalfSum ? 1 : 0;
        return this;
    }

    /*Repeat() Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               byte ==> char,,short,,int,,long,,float,,double;;
               \(([\w\[\]\(\)\s=]+) \& 0xFF\) ==> $1,,($1 & 0xFFFF),,$1,,...
    */
    public void emphasize(byte[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(byte[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        byte left = values[p];
        for (; ; ) {
            byte v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            byte right;
            if ((v & 0xFF) > (left & 0xFF)) {
                // - ascending slope
                q++;
                byte probe;
                while (q <= last && ((probe = values[q]) & 0xFF) > (v & 0xFF)) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                byte probe;
                while (q <= last && ((probe = values[q]) & 0xFF) < (v & 0xFF)) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(byte[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        byte left = values[p];
        for (; ; ) {
            byte v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            byte right;
            if ((v & 0xFF) > (left & 0xFF)) {
                // - ascending slope
                q += step;
                byte probe;
                while ((q >= 0 && q <= last) && ((probe = values[q]) & 0xFF) > (v & 0xFF)) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                byte probe;
                while ((q >= 0 && q <= last) && ((probe = values[q]) & 0xFF) < (v & 0xFF)) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    public void emphasize(char[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(char[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        char left = values[p];
        for (; ; ) {
            char v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            char right;
            if (v > left) {
                // - ascending slope
                q++;
                char probe;
                while (q <= last && (probe = values[q]) > v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                char probe;
                while (q <= last && (probe = values[q]) < v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(char[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        char left = values[p];
        for (; ; ) {
            char v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            char right;
            if (v > left) {
                // - ascending slope
                q += step;
                char probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) > v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                char probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) < v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }


    public void emphasize(short[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(short[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        short left = values[p];
        for (; ; ) {
            short v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            short right;
            if ((v & 0xFFFF) > (left & 0xFFFF)) {
                // - ascending slope
                q++;
                short probe;
                while (q <= last && ((probe = values[q]) & 0xFFFF) > (v & 0xFFFF)) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                short probe;
                while (q <= last && ((probe = values[q]) & 0xFFFF) < (v & 0xFFFF)) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(short[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        short left = values[p];
        for (; ; ) {
            short v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            short right;
            if ((v & 0xFFFF) > (left & 0xFFFF)) {
                // - ascending slope
                q += step;
                short probe;
                while ((q >= 0 && q <= last) && ((probe = values[q]) & 0xFFFF) > (v & 0xFFFF)) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                short probe;
                while ((q >= 0 && q <= last) && ((probe = values[q]) & 0xFFFF) < (v & 0xFFFF)) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }


    public void emphasize(int[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(int[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        int left = values[p];
        for (; ; ) {
            int v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            int right;
            if (v > left) {
                // - ascending slope
                q++;
                int probe;
                while (q <= last && (probe = values[q]) > v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                int probe;
                while (q <= last && (probe = values[q]) < v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(int[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        int left = values[p];
        for (; ; ) {
            int v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            int right;
            if (v > left) {
                // - ascending slope
                q += step;
                int probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) > v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                int probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) < v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }


    public void emphasize(long[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(long[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        long left = values[p];
        for (; ; ) {
            long v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            long right;
            if (v > left) {
                // - ascending slope
                q++;
                long probe;
                while (q <= last && (probe = values[q]) > v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                long probe;
                while (q <= last && (probe = values[q]) < v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(long[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        long left = values[p];
        for (; ; ) {
            long v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            long right;
            if (v > left) {
                // - ascending slope
                q += step;
                long probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) > v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                long probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) < v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }


    public void emphasize(float[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(float[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        float left = values[p];
        for (; ; ) {
            float v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            float right;
            if (v > left) {
                // - ascending slope
                q++;
                float probe;
                while (q <= last && (probe = values[q]) > v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                float probe;
                while (q <= last && (probe = values[q]) < v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(float[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        float left = values[p];
        for (; ; ) {
            float v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            float right;
            if (v > left) {
                // - ascending slope
                q += step;
                float probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) > v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                float probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) < v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }


    public void emphasize(double[] values) {
        emphasize(values, 0, values.length);
    }

    /**
     * Emphasizes boundaries: sub-ranges of <tt>values</tt> array with high gradient. It means,
     * that slopes of the function, represented by this array, with a large difference in values
     * are replaced with a step function, where the border has a width of 1.
     *
     * <p>More formal specification. Let V(x) is the function from integer argument x, represented by <tt>values</tt>
     * array: V(i)=<tt>values[i]</tt>, <tt>from</tt>&nbsp;&le;&nbsp;i&nbsp;&lt;&nbsp;<tt>to</tt>.
     * This method finds all ranges x1..x2, where:</p>
     * <ul>
     *     <li><tt>from</tt> &le; x1 &le; x2 &lt; <tt>to</tt>;</li>
     *     <li>the function is strictly monotone inside the range: V(x1)&lt;V(x1+1)&lt;...&lt;V(x2) or
     *     V(x1)&gt;V(x1+1)&gt;...&gt;V(x2);
     *     if property {@link #setProcessAscending(boolean) processAscending} is cleared, first type of ranges
     *     is skipped (not changed),
     *     if property {@link #setProcessDescending(boolean) processDescending} is cleared, second type of ranges
     *      is skipped (not changed), if both are cleared, this method does nothing;</li>
     *      <li>total change of the function at this range is <b>large</b>, i.e. |V(x1)&minus;V(x2)|
     *      &ge; {@link #setMinimalChange(double) minimalChange};</li>
     *      <li>length of the range x2&minus;x1+1 &le; D, where D = {@link #setSlopeWidth(int) slopeWidth},
     *      <b>or</b>, maybe, it is not so, but {@link #setAllowLongSlopes(boolean) allowLongSlopes}
     *      flag is set <b>and</b> and the following condition is fulfilled:<br>
     *      &nbsp;&nbsp;&nbsp;&nbsp;for every sub-range with length D the total change of the function is also
     *      <b>large</b>, i.e. for every x, x1&le;x&le;x2&minus;D+1, we have
     *      |V(x)&minus;V(x+D&minus;1)| &ge; {@link #setMinimalChange(double) minimalChange};
     *      </li>
     *      <li>x1..x2 is a <i>maximal</i> range with this properties, i.e. for any other
     *      range x'1..x'2, where x'1&lt;x1 and x'2&gt;x2, one of the previous conditions is not fulfilled.</li>
     * </ul>
     *
     * <p>For each range x1..x2 with the specified properties this method replaces every value V(x)
     * inside this range (x1&lt;x&lt;x2) with the nearest from two values V(x1) and V(x2).
     * Values, exactly equal to (V(x1)+V(x2)/2, are replaced with V(x2).</p>
     *
     * <p>Note: default values {@link #setMinimalChange(double) minimalChange}=0 and
     * {@link #setSlopeWidth(int) slopeWidth}=1 provides emphasizing <i>every</i> slope,
     * regardless on its length and value of the function change.</p>
     *
     * @param values array to be modified.
     * @param offset index of the first element of the processed fragment of this array.
     * @param count  number of elements to process.
     */
    public void emphasize(double[] values, final int offset, final int count) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final int last = offset + count - 1;
        if (last < 0 || last >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + ((long) offset + (long) count - 1)
                    + " >= array length (" + values.length + ")");
        }
        int p = offset;
        double left = values[p];
        for (; ; ) {
            double v;
            do {
                p++;
                if (p >= last) {
                    // - overflow impossible, because last < values.length <= Integer.MAX_VALUE
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p--;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            double right;
            if (v > left) {
                // - ascending slope
                q++;
                double probe;
                while (q <= last && (probe = values[q]) > v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right);
                }
            } else {
                // - descending slope
                q++;
                double probe;
                while (q <= last && (probe = values[q]) < v) {
                    v = probe;
                    q++;
                }
                q--;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    public void emphasize(double[] values, final int offset, final int count, final int step) {
        Objects.requireNonNull(values, "Null values");
        if (count < 0) {
            throw new IllegalArgumentException("Negative number of elements (" + count + ")");
        }
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Start position " + offset + " < 0");
        }
        if (step <= 0) {
            throw new IndexOutOfBoundsException("Zero or negative step = " + step);
        }
        if (count == 0 || (!processAscending && !processDescending)) {
            return;
        }
        final long longLast = (long) offset + (long) step * ((long) count - 1);
        if (longLast >= values.length) {
            throw new IndexOutOfBoundsException("Last position " + longLast
                    + " >= array length (" + values.length + ")");
        }
        final int last = (int) longLast;
        final int slopeDistance = (int) Math.min((long) (slopeWidth - 1) * (long) step, Integer.MAX_VALUE);
        // - if (slopeWidth - 1) * step > Integer.MAX_VALUE, we will detect that actual interval left..right
        // (inside  analysed array element) is LESS OR EQUAL than slopeDistance,
        // because right - left <= Integer.MAX_VALUE always: see analyzeAscendingSlope / analyzeDescendingSlope.
        // After this check, this value will not be used, so replacing with Integer.MAX_VALUE
        // will not lead to a problem.
        int p = offset;
        double left = values[p];
        for (; ; ) {
            double v;
            do {
                p += step;
                if (p < 0 || p >= last) {
                    // - p < 0 when possible overflow
                    return;
                }
                v = values[p];
            } while (v == left);

            int q = p;
            p -= step;
            assert values[p] == left;
            // p is the left boundary of the slope
            assert values[q] == v;
            double right;
            if (v > left) {
                // - ascending slope
                q += step;
                double probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) > v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is ascending slope, q is a local maximum or beginning of series of equal values
                if (processAscending && differenceEnough(left, right)) {
                    analyzeAscendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            } else {
                // - descending slope
                q += step;
                double probe;
                while ((q >= 0 && q <= last) && (probe = values[q]) < v) {
                    v = probe;
                    q += step;
                }
                q -= step;
                right = v;
                // p..q is descending slope, q is a local minimum or beginning of series of equal values
                if (processDescending && differenceEnough(right, left)) {
                    analyzeDescendingSlope(values, p, q, left, right, step, slopeDistance);
                }
            }
            assert values[q] == right;
            left = right;
            p = q;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    /*Repeat() Byte ==> Char,,Short,,Int,,Long,,Float,,Double;;
               byte ==> char,,short,,int,,long,,float,,double;;
               \(([\w\[\]]+) \& 0xFF\) ==> $1,,($1 & 0xFFFF),,$1,,...;;
               (int\s+halfSum) ==> $1,,$1,,$1,,long halfSum,,double halfSum,,double halfSum;;
               (halfSumFloor\() ==> $1,,$1,,$1,,$1,,halfSum(,,halfSum(
    */
    private void analyzeAscendingSlope(
            byte[] values,
            final int first,
            final int last,
            final byte leftValue,
            final byte rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFF) < (rightValue & 0xFF);
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        byte left = leftValue;
        byte right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                byte probeLeft = values[probeQ - d];
                byte probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            byte[] values,
            final int first,
            final int last,
            final byte leftValue,
            final byte rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFF) < (rightValue & 0xFF);
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        byte left = leftValue;
        byte right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                byte probeLeft = values[probeQ - d];
                byte probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            byte[] values,
            final int first,
            final int last,
            final byte leftValue,
            final byte rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFF) > (rightValue & 0xFF);
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        byte left = leftValue;
        byte right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                byte probeLeft = values[probeQ - d];
                byte probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            byte[] values,
            final int first,
            final int last,
            final byte leftValue,
            final byte rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFF) > (rightValue & 0xFF);
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        byte left = leftValue;
        byte right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                byte probeLeft = values[probeQ - d];
                byte probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(byte[] values, int first, int last, byte leftValue, byte rightValue) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && (values[p] & 0xFF) < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(byte[] values, int first, int last, byte leftValue, byte rightValue, int step) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && (values[p] & 0xFF) < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(byte[] values, int first, int last, byte leftValue, byte rightValue) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && (values[p] & 0xFF) > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(byte[] values, int first, int last, byte leftValue, byte rightValue, int step) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && (values[p] & 0xFF) > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    /*Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! */
    private void analyzeAscendingSlope(
            char[] values,
            final int first,
            final int last,
            final char leftValue,
            final char rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        char left = leftValue;
        char right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                char probeLeft = values[probeQ - d];
                char probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            char[] values,
            final int first,
            final int last,
            final char leftValue,
            final char rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        char left = leftValue;
        char right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                char probeLeft = values[probeQ - d];
                char probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            char[] values,
            final int first,
            final int last,
            final char leftValue,
            final char rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        char left = leftValue;
        char right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                char probeLeft = values[probeQ - d];
                char probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            char[] values,
            final int first,
            final int last,
            final char leftValue,
            final char rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        char left = leftValue;
        char right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                char probeLeft = values[probeQ - d];
                char probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(char[] values, int first, int last, char leftValue, char rightValue) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(char[] values, int first, int last, char leftValue, char rightValue, int step) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(char[] values, int first, int last, char leftValue, char rightValue) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(char[] values, int first, int last, char leftValue, char rightValue, int step) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }


    private void analyzeAscendingSlope(
            short[] values,
            final int first,
            final int last,
            final short leftValue,
            final short rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFFFF) < (rightValue & 0xFFFF);
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        short left = leftValue;
        short right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                short probeLeft = values[probeQ - d];
                short probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            short[] values,
            final int first,
            final int last,
            final short leftValue,
            final short rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFFFF) < (rightValue & 0xFFFF);
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        short left = leftValue;
        short right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                short probeLeft = values[probeQ - d];
                short probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            short[] values,
            final int first,
            final int last,
            final short leftValue,
            final short rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFFFF) > (rightValue & 0xFFFF);
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        short left = leftValue;
        short right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                short probeLeft = values[probeQ - d];
                short probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            short[] values,
            final int first,
            final int last,
            final short leftValue,
            final short rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert (leftValue & 0xFFFF) > (rightValue & 0xFFFF);
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        short left = leftValue;
        short right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                short probeLeft = values[probeQ - d];
                short probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(short[] values, int first, int last, short leftValue, short rightValue) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && (values[p] & 0xFFFF) < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(short[] values, int first, int last, short leftValue, short rightValue, int step) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && (values[p] & 0xFFFF) < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(short[] values, int first, int last, short leftValue, short rightValue) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && (values[p] & 0xFFFF) > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(short[] values, int first, int last, short leftValue, short rightValue, int step) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && (values[p] & 0xFFFF) > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }


    private void analyzeAscendingSlope(
            int[] values,
            final int first,
            final int last,
            final int leftValue,
            final int rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        int left = leftValue;
        int right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                int probeLeft = values[probeQ - d];
                int probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            int[] values,
            final int first,
            final int last,
            final int leftValue,
            final int rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        int left = leftValue;
        int right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                int probeLeft = values[probeQ - d];
                int probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            int[] values,
            final int first,
            final int last,
            final int leftValue,
            final int rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        int left = leftValue;
        int right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                int probeLeft = values[probeQ - d];
                int probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            int[] values,
            final int first,
            final int last,
            final int leftValue,
            final int rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        int left = leftValue;
        int right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                int probeLeft = values[probeQ - d];
                int probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(int[] values, int first, int last, int leftValue, int rightValue) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(int[] values, int first, int last, int leftValue, int rightValue, int step) {
        assert first < last;
        final int halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(int[] values, int first, int last, int leftValue, int rightValue) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(int[] values, int first, int last, int leftValue, int rightValue, int step) {
        assert first < last;
        final int halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }


    private void analyzeAscendingSlope(
            long[] values,
            final int first,
            final int last,
            final long leftValue,
            final long rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        long left = leftValue;
        long right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                long probeLeft = values[probeQ - d];
                long probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            long[] values,
            final int first,
            final int last,
            final long leftValue,
            final long rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        long left = leftValue;
        long right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                long probeLeft = values[probeQ - d];
                long probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            long[] values,
            final int first,
            final int last,
            final long leftValue,
            final long rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        long left = leftValue;
        long right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                long probeLeft = values[probeQ - d];
                long probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            long[] values,
            final int first,
            final int last,
            final long leftValue,
            final long rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        long left = leftValue;
        long right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                long probeLeft = values[probeQ - d];
                long probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(long[] values, int first, int last, long leftValue, long rightValue) {
        assert first < last;
        final long halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(long[] values, int first, int last, long leftValue, long rightValue, int step) {
        assert first < last;
        final long halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(long[] values, int first, int last, long leftValue, long rightValue) {
        assert first < last;
        final long halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(long[] values, int first, int last, long leftValue, long rightValue, int step) {
        assert first < last;
        final long halfSum = halfSumFloor(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }


    private void analyzeAscendingSlope(
            float[] values,
            final int first,
            final int last,
            final float leftValue,
            final float rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        float left = leftValue;
        float right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                float probeLeft = values[probeQ - d];
                float probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            float[] values,
            final int first,
            final int last,
            final float leftValue,
            final float rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        float left = leftValue;
        float right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                float probeLeft = values[probeQ - d];
                float probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            float[] values,
            final int first,
            final int last,
            final float leftValue,
            final float rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        float left = leftValue;
        float right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                float probeLeft = values[probeQ - d];
                float probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            float[] values,
            final int first,
            final int last,
            final float leftValue,
            final float rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        float left = leftValue;
        float right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                float probeLeft = values[probeQ - d];
                float probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(float[] values, int first, int last, float leftValue, float rightValue) {
        assert first < last;
        final double halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(float[] values, int first, int last, float leftValue, float rightValue, int step) {
        assert first < last;
        final double halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(float[] values, int first, int last, float leftValue, float rightValue) {
        assert first < last;
        final double halfSum = halfSum(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(float[] values, int first, int last, float leftValue, float rightValue, int step) {
        assert first < last;
        final double halfSum = halfSum(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }


    private void analyzeAscendingSlope(
            double[] values,
            final int first,
            final int last,
            final double leftValue,
            final double rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        double left = leftValue;
        double right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                double probeLeft = values[probeQ - d];
                double probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeAscendingSlope(
            double[] values,
            final int first,
            final int last,
            final double leftValue,
            final double rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue < rightValue;
        assert d >= 0;
        if (last - first <= d) {
            fillAscendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        double left = leftValue;
        double right = values[q];
        for (; ; ) {
            while (!differenceEnough(left, right)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                double probeLeft = values[probeQ - d];
                double probeRight = values[probeQ];
                if (!differenceEnough(probeLeft, probeRight)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+step...p+step+d, ..., q-d..q
            fillAscendingSlope(values, p, q, left, right, step);
            // q+step-d ..q+step is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-step, q+2*step <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            double[] values,
            final int first,
            final int last,
            final double leftValue,
            final double rightValue) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        final int d = slopeWidth - 1;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue);
            return;
        }
        final int lastMinus1 = last - 1;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        double left = leftValue;
        double right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p++;
                q++;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + 1;
                double probeLeft = values[probeQ - d];
                double probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinus1) {
                // maybe, q == last-1, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += 2;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void analyzeDescendingSlope(
            double[] values,
            final int first,
            final int last,
            final double leftValue,
            final double rightValue,
            final int step,
            final int d) {
        assert first < last;
        // - If they are equal, it is incorrect range: no slope!
        assert leftValue > rightValue;
        assert d >= 0 : "invalid slopeWidth = " + slopeWidth;
        if (last - first <= d) {
            fillDescendingSlope(values, first, last, leftValue, rightValue, step);
            return;
        }
        final int lastMinusStep = last - step;
        final int doubleStep = step * 2;
        int p = first;
        int q = first + d;
        if (!allowLongSlopes) {
            return;
        }
        // first..last range is too long (its length last-first+1 > slopeWidth); further analysis is required
        double left = leftValue;
        double right = values[q];
        for (; ; ) {
            while (!differenceEnough(right, left)) {
                p += step;
                q += step;
                if (q > last) {
                    return;
                }
                left = values[p];
                right = values[q];
            }
            // p is beginning of subrange with good change
            while (q < last) {
                int probeQ = q + step;
                double probeLeft = values[probeQ - d];
                double probeRight = values[probeQ];
                if (!differenceEnough(probeRight, probeLeft)) {
                    break;
                }
                q = probeQ;
                right = probeRight;
            }
            // p..q is a subrange with good change at every fragment p..p+d, p+1...p+1+d, ..., q-d..q
            fillDescendingSlope(values, p, q, left, right, step);
            // q+1-d ..q+1 is already a bad fragment
            if (q >= lastMinusStep) {
                // maybe, q == last-step, but last-d..last has low difference
                return;
            }
            // - so, q < last-1, q+2 <= last
            q += doubleStep;
            p = q - d;
            left = values[p];
            right = values[q];
        }
    }

    private void fillAscendingSlope(double[] values, int first, int last, double leftValue, double rightValue) {
        assert first < last;
        final double halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillAscendingSlope(double[] values, int first, int last, double leftValue, double rightValue, int step) {
        assert first < last;
        final double halfSum = halfSum(leftValue, rightValue);
        // - for integer types in exactHalfSum it is ceil((leftValue+rightValue)/2)
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] < halfSum) {
            // - for half-integer halfSum, it REALLY means: < exact (rightValue+leftValue)/2 (because it is the ceil)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(double[] values, int first, int last, double leftValue, double rightValue) {
        assert first < last;
        final double halfSum = halfSum(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first++;
        last--;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p++;
        }
        for (; p <= last; p++) {
            values[p] = rightValue;
        }
    }

    private void fillDescendingSlope(double[] values, int first, int last, double leftValue, double rightValue, int step) {
        assert first < last;
        final double halfSum = halfSum(rightValue, leftValue);
        // - halfSumFloor is important for integer types, if exact halfSum is not integer
        first += step;
        last -= step;
        int p = first;
        while (p <= last && values[p] > halfSum) {
            // - for half-integer halfSum, it REALLY means: > exact (rightValue+leftValue)/2 (because it is the floor)
            values[p] = leftValue;
            p += step;
        }
        for (; p <= last; p += step) {
            values[p] = rightValue;
        }
    }

    /*Repeat.AutoGeneratedEnd*/

    private boolean differenceEnough(byte less, byte greater) {
        return (greater & 0xFF) - (less & 0xFF) >= minimalChangeLong;
    }

    private boolean differenceEnough(char less, char greater) {
        return (int) greater - (int) less >= minimalChangeLong;
    }

    private boolean differenceEnough(short less, short greater) {
        return (greater & 0xFFFF) - (less & 0xFFFF) >= minimalChangeLong;
    }

    private boolean differenceEnough(int less, int greater) {
        return (long) greater - (long) less >= minimalChangeLong;
    }

    private boolean differenceEnough(long less, long greater) {
        final long difference = greater - less;
        return difference < 0 || difference >= minimalChangeLong;
    }

    private boolean differenceEnough(float less, float greater) {
        return (double) greater - (double) less >= minimalChange;
    }

    private boolean differenceEnough(double less, double greater) {
        return greater - less >= minimalChange;
    }

    // This function returns floor((less+greater)/2) if !exactHalfSum
    // or ceil((less+greater)/2) if exactHalfSum
    private int halfSum(byte less, byte greater) {
        return ((less & 0xFF) + (greater & 0xFF) + exactHalfSum01) >>> 1;
    }

    private int halfSum(char less, char greater) {
        return ((int) less + (int) greater + exactHalfSum01) >>> 1;
    }

    private int halfSum(short less, short greater) {
        return ((less & 0xFFFF) + (greater & 0xFFFF) + exactHalfSum01) >>> 1;
    }

    private int halfSum(int less, int greater) {
        assert less <= greater;
        return less + ((greater - less) >>> 1) + ((greater ^ less) & exactHalfSum01);
    }

    private long halfSum(long less, long greater) {
        assert less <= greater;
        return less + ((greater - less) >>> 1) + ((greater ^ less) & exactHalfSum01);
    }

    private int halfSumFloor(byte less, byte greater) {
        return ((less & 0xFF) + (greater & 0xFF)) >>> 1;
    }

    private int halfSumFloor(char less, char greater) {
        return ((int) less + (int) greater) >>> 1;
    }

    private int halfSumFloor(short less, short greater) {
        return ((less & 0xFFFF) + (greater & 0xFFFF)) >>> 1;
    }

    private int halfSumFloor(int less, int greater) {
        assert less <= greater;
        return less + ((greater - less) >>> 1);
    }

    private long halfSumFloor(long less, long greater) {
        assert less <= greater;
        return less + ((greater - less) >>> 1);
    }

    private static double halfSum(float less, float greater) {
        return 0.5 * ((double) less + (double) greater);
    }

    private static double halfSum(double less, double greater) {
        return 0.5 * (less + greater);
    }
}
