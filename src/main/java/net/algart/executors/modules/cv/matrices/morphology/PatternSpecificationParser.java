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

package net.algart.executors.modules.cv.matrices.morphology;

import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.arrays.PIntegerArray;
import net.algart.math.IPoint;
import net.algart.math.IRange;
import net.algart.math.IRectangularArea;
import net.algart.math.Point;
import net.algart.math.functions.ConstantFunc;
import net.algart.math.functions.Func;
import net.algart.math.patterns.*;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

public final class PatternSpecificationParser {
    private static final System.Logger LOG = System.getLogger(PatternSpecificationParser.class.getName());

    private static final Map<Class<?>, PatternSpecificationParser> INSTANCES = new HashMap<>();

    private final Class<?> elementType;
    private final Map<String, Reference<Pattern>> patternCache = new HashMap<>();

    private PatternSpecificationParser(Class<?> elementType) {
        this.elementType = elementType;
    }

    // elementType is important while usage 3D patterns: integer matrices allows optimization of large Minkowski sums
    public static PatternSpecificationParser getInstance(Class<?> elementType) {
        synchronized (INSTANCES) {
            PatternSpecificationParser result = INSTANCES.get(elementType);
            if (result == null) {
                result = new PatternSpecificationParser(elementType);
                INSTANCES.put(elementType, result);
            }
            return result;
        }
    }

    public Pattern parse(String s) {
        s = s.trim();
        if (!isSurelyLowCase(s)) {
            s = s.toLowerCase();
        }
        Pattern result = null;
        synchronized (patternCache) {
            Reference<Pattern> ref = patternCache.get(s);
            if (ref != null) {
                result = ref.get();
            }
            if (result != null) {
                return result;
            }
            // cleaning
            patternCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
        }
        Class<PArray> arrayType = Arrays.type(PArray.class, elementType);
        final boolean integerType = PIntegerArray.class.isAssignableFrom(arrayType) && elementType != long.class;
        // for long.class, attempts to round the pattern will lead to TooLargePatternCoordinatesException;
        // so, optimization of possible Minkowski sums, provided by rounding, cannot be used
        final double zScale = Arrays.maxPossibleValue(arrayType, 1.0);
        boolean raw, slow, surface, bounds;
        try {
            raw = s.endsWith(" -raw");
            if (raw) {
                s = s.substring(0, s.length() - " -raw".length());
            }
            slow = s.endsWith(" -slow");
            if (slow) {
                s = s.substring(0, s.length() - " -slow".length());
            }
            surface = s.startsWith("bound ");
            if (surface) {
                s = s.substring("bound ".length());
            }
            bounds = s.startsWith("ext-bound ");
            if (bounds) {
                s = s.substring("ext-bound ".length());
            }
            final Pattern matrix01 = parseBinaryMatrix2D(s);
            String[] elements;
            if (matrix01 != null) {
                result = matrix01;
            } else if ((elements = s.split("(\\b|\\s)mult(\\b|\\s)")).length == 2) { // "m MULT xxx" means
                // multiplication
                double mult = Double.parseDouble(elements[0].trim());         // (low priority)
                result = parse(elements[1]).multiply(mult);

            } else if ((elements = s.split("(\\b|\\s)scale(\\b|\\s)")).length == 2) { // "mx my SCALE xxx" scaling
                String[] params = elements[0].split("[, ]+");                         // (low priority)
                double mX = Double.parseDouble(params[0].trim());
                double mY = Double.parseDouble(params[1].trim());
                result = parse(elements[1]).scale(mX, mY);

            } else if ((elements = s.split("\\s\\\\-\\s")).length == 2) { // "xxx \- xxx" means subtraction of erosion
                Pattern ptn1 = parse(elements[0]);
                Pattern ptn2 = parse(elements[1]);
                result = ptn1.minkowskiSubtract(ptn2);
                if (result == null) {
                    result = ptn1;
                } else {
                    result = subtract(ptn1, result);
                }

            } else if ((elements = s.split("(\\b|\\s)\\\\(\\b|\\s)")).length == 2) { // "xxx \ xxx" means subtraction
                Pattern ptn1 = parse(elements[0]);
                Pattern ptn2 = parse(elements[1]);
                result = subtract(ptn1, ptn2);

            } else if ((elements = s.split("(\\b|\\s)u(\\b|\\s)")).length > 1) { // "xxx u xxx" means union
                Pattern[] patterns = new Pattern[elements.length];
                for (int k = 0; k < patterns.length; k++)
                    patterns[k] = parse(elements[k]);
                result = Patterns.newUnion(patterns);

            } else if ((elements = s.split("\\+")).length > 1) { // "xxx + xxx" means Minkowski sum
                Pattern[] patterns = new Pattern[elements.length];
                for (int k = 0; k < patterns.length; k++)
                    patterns[k] = parse(elements[k]);
                result = Patterns.newMinkowskiSum(patterns);

            } else if ((elements = s.split("\\s-\\s")).length == 2) { // "xxx - xxx" means erosion
                Pattern ptn1 = parse(elements[0]);
                Pattern ptn2 = parse(elements[1]);
                result = ptn1.minkowskiSubtract(ptn2);
                if (result == null) {
                    throw new IllegalArgumentException("Empty pattern erosion");
                }

            } else if ((elements = s.split("(\\b|\\s)x(\\b|\\s)")).length == 2) { // "n x xxx" means Minkowski product
                double mult = Double.parseDouble(elements[0].trim());
                if (mult < 0)
                    throw new IllegalArgumentException("Negative multiplier");
                Pattern ptn = parse(elements[1]);
                double frac = mult - (int) mult;
                Pattern ptnFrac = ptn.multiply(frac).round();
                if ((int) mult > 0) {
                    result = Patterns.newMinkowskiMultiplePattern(ptn, (int) mult);
                    if (frac > 0.0)
                        result = Patterns.newMinkowskiSum(result, ptnFrac);
                } else {
                    result = ptnFrac;
                }
            } else if ((elements = s.split("(\\b|\\s)\\*(\\b|\\s)")).length == 2) { // "m * xxx" means usual product
                double mult = Double.parseDouble(elements[0].trim());
                result = parse(elements[1]).multiply(mult);

            } else if ((elements = s.split("(\\b|\\s)>>(\\b|\\s)")).length == 2) { // "m >> x y" means shift
                String[] params = elements[1].split("[, ]+");
                double x = Double.parseDouble(params[0].trim());
                double y = Double.parseDouble(params[1].trim());
                result = parse(elements[0]).shift(Point.valueOf(x, y));

            } else if (s.startsWith("circle ")) { // "circle d [x y]"
                String[] params = s.substring("circle ".length()).split("[, ]+");
                double d = Double.parseDouble(params[0].trim());
                if (d > 10000)
                    throw new IllegalArgumentException("Too large diameter: " + d +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                double x = 0, y = 0;
                if (params.length >= 3) {
                    x = Double.parseDouble(params[1].trim());
                    y = Double.parseDouble(params[2].trim());
                }
                result = Patterns.newSphereIntegerPattern(Point.valueOf(0, 0), 0.5 * d).shift(Point.valueOf(x, y));

            } else if (s.startsWith("ring ")) { // "ring d thickness [x y]"
                String[] params = s.substring("ring ".length()).split("[, ]+");
                double d = Double.parseDouble(params[0].trim());
                double th = Double.parseDouble(params[1].trim());
                if (d > 10000)
                    throw new IllegalArgumentException("Too large diameter: " + d +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                if (th >= 0.5 * d)
                    throw new IllegalArgumentException("Too large thickness: must be less than d/2");
                double x = 0, y = 0;
                if (params.length >= 4) {
                    x = Double.parseDouble(params[2].trim());
                    y = Double.parseDouble(params[3].trim());
                }
                UniformGridPattern larger = Patterns.newSphereIntegerPattern(Point.valueOf(x, y), 0.5 * d);
                UniformGridPattern smaller = Patterns.newSphereIntegerPattern(Point.valueOf(x, y), 0.5 * d - th);
                Set<IPoint> points = new HashSet<>(larger.roundedPoints());
                points.removeAll(smaller.roundedPoints());
                points.addAll(larger.surface().roundedPoints());
                result = Patterns.newIntegerPattern(points);

            } else if (s.startsWith("ellipse ")) { // "ellipse a b fi [x y]"
                String[] params = s.substring("ellipse ".length()).split("[, ]+");
                double a = Double.parseDouble(params[0].trim());
                if (a > 10000)
                    throw new IllegalArgumentException("Too large first diameter: " + a
                            + " (maximal possible value is 10000)");
                double b = Double.parseDouble(params[1].trim());
                if (b > 10000)
                    throw new IllegalArgumentException("Too large second diameter: " + b
                            + " (maximal possible value is 10000)");
                if (a <= 0.0)
                    throw new IllegalArgumentException("Zero or negative first diameter: " + a);
                if (b <= 0.0)
                    throw new IllegalArgumentException("Zero or negative second diameter: " + b);
                double fi = Math.toRadians(Double.parseDouble(params[2].trim()));
                final double x, y;
                if (params.length < 5) {
                    x = y = 0.0;
                } else {
                    x = Double.parseDouble(params[3].trim());
                    y = Double.parseDouble(params[4].trim());
                }
                if (fi == 0.0) {
                    result = Patterns.newEllipsoidIntegerPattern(Point.origin(2), 0.5 * a, 0.5 * b)
                            .shift(Point.valueOf(x, y));
                } else {
                    if (a < b) {
                        double temp = a;
                        a = b;
                        b = temp;
                        fi += 0.5 * Math.PI;
                    }
                    final Set<IPoint> points = new HashSet<>();
                    final double mult = b / a;
                    final double r = 0.5 * b;
                    final Point e = Point.valueOf(Math.cos(fi), Math.sin(fi));
                    final int ir = (int) a + 2;
                    final int xMin = (int) x - ir;
                    final int xMax = (int) x + ir;
                    final int yMin = (int) y - ir;
                    final int yMax = (int) y + ir;
                    final Thread[] threads = new Thread[Arrays.SystemSettings.cpuCount()];
                    for (int k = 0; k < threads.length; k++) {
                        final int threadIndex = k;
                        threads[k] = new Thread(() -> {
                            final Set<IPoint> threadPoints = new HashSet<>();
                            for (int iy = yMin; iy <= yMax; iy++) {
                                if ((iy - yMin) % threads.length == threadIndex) {
                                    for (int ix = xMin; ix <= xMax; ix++) {
                                        Point p = Point.valueOf(ix - x, iy - y);
                                        // we should build q = p - e*(pe) + mult*e*(pe) = p + (mult-1)*e*pe:
                                        // p is inside the ellipse if and only if q is inside the circle with r=b/2
                                        double pe = p.scalarProduct(e);
                                        p = p.add(e.multiply(pe * (mult - 1)));
                                        if (p.distanceFromOrigin() <= r)
                                            threadPoints.add(IPoint.valueOf(ix, iy));
                                    }
                                }
                            }
                            synchronized (threads) {
                                points.addAll(threadPoints);
                            }
                        });
                        threads[k].start();
                    }
                    for (Thread thread : threads) {
                        thread.join();
                    }
                    result = Patterns.newIntegerPattern(points);
                }

// Alternative, more simple, but slower algorithm, requiring much more Java memory:
//                Point center = Point.valueOf(x, y);
//                result = Patterns.newIntegerSpherePattern(center, 0.5 * a);
//                Point e = Point.valueOf( -Math.sin(fi), Math.cos(fi));
//                double mult = b / a;
//                Set<IPoint> srcPoints = result.roundedPoints();
//                Set<IPoint> destPoints = new HashSet<IPoint>();
//                for (IPoint ip : srcPoints) {
//                    Point p = Point.valueOf(ip).subtract(center);
//                    // we should replace p = p - e*(pe) + mult*e*(pe) = p + (mult-1)*e*pe
//                    double pe = p.scalarProduct(e);
//                    p = p.add(e.multiply(pe * (mult - 1)));
//                    destPoints.add(IPoint.roundOf(p.add(center)));
//                }
//                result = Patterns.newIPattern(destPoints);

            } else if (s.startsWith("rect ")) { // "rect width [height [centerX centerY]]"
                String[] params = s.substring("rect ".length()).split("[, ]+");
                long width = Long.parseLong(params[0].trim());
                long height = width;
                if (params.length >= 2) {
                    height = Long.parseLong(params[1].trim());
                }
                long x = -width / 2, y = -height / 2;
                if (params.length >= 4) {
                    x += Long.parseLong(params[2].trim());
                    y += Long.parseLong(params[3].trim());
                }
                result = Patterns.newRectangularIntegerPattern(
                        IRange.valueOf(x, x + width - 1),
                        IRange.valueOf(y, y + height - 1));

            } else if (s.startsWith("square ")) { // "square size [x y]"
                String[] params = s.substring("square ".length()).split("[, ]+");
                long size = Long.parseLong(params[0].trim());
                long x = -size / 2, y = -size / 2;
                if (params.length >= 3) {
                    x += Long.parseLong(params[1].trim());
                    y += Long.parseLong(params[2].trim());
                }
                result = Patterns.newRectangularIntegerPattern(
                        IRange.valueOf(x, x + size - 1),
                        IRange.valueOf(y, y + size - 1));

            } else if (s.startsWith("octagon ")) { // "octagon size [x y]"
                String[] params = s.substring("octagon ".length()).split("[, ]+");
                double d = Double.parseDouble(params[0].trim());
                if (d > 100000)
                    throw new IllegalArgumentException("Too large diameter: " + d +
                            " (maximal possible value is 100000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                long x = 0, y = 0;
                if (params.length >= 3) {
                    x = Long.parseLong(params[1].trim());
                    y = Long.parseLong(params[2].trim());
                }
                int rectSize = (int) Math.round(d);
                int diagSize = (int) (d / (Math.sqrt(2.0) + 2.0));
                int nAxis = Math.max(1, diagSize < 1 ? rectSize : Math.max(1, rectSize - 2 * diagSize));
                // each getSeries(0, 0, 1, 1, diagSize+1) below has sizes diagSize x diagSize
                IRange axisRange = IRange.valueOf(-nAxis / 2, -nAxis / 2 + nAxis - 1);
                result = Patterns.newRectangularIntegerPattern(axisRange, axisRange);
                if (diagSize >= 1) {
                    Pattern diagonal = Patterns.newMinkowskiSum(
                            getSeries(0, 0, 1, 1, diagSize + 1),
                            getSeries(0, 0, 1, -1, diagSize + 1));
                    IRange xr = diagonal.roundedCoordRange(0), yr = diagonal.roundedCoordRange(1);
                    diagonal = diagonal.shift(Point.valueOf(-(xr.min() + xr.max()) / 2, -(yr.min() + yr.max()) / 2));
                    if (nAxis == 1)
                        result = Patterns.newUnion(result, diagonal);
                    else
                        result = Patterns.newMinkowskiSum(result, diagonal);
                }
                result = result.shift(Point.valueOf(x, y));

            } else if (s.equals("cross")) { // "cross"
                result = Patterns.newIntegerPattern(IPoint.valueOf(0, 0),
                        IPoint.valueOf(1, 0), IPoint.valueOf(0, 1),
                        IPoint.valueOf(-1, 0), IPoint.valueOf(0, -1));

            } else if (s.startsWith("sphere-surface ")) { // "sphere-surface dCircle d zD [x y [z]]"
                String[] params = s.substring("sphere-surface ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zD = Double.parseDouble(params[2].trim()) * zScale;
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                if (dCircle > d)
                    throw new IllegalArgumentException("Circle diameter is greater than sphere diameter");
                double x = 0, y = 0, z = 0;
                if (params.length >= 5) {
                    x = Double.parseDouble(params[3].trim());
                    y = Double.parseDouble(params[4].trim());
                }
                if (params.length >= 6) {
                    z = Double.parseDouble(params[5].trim()) * zScale;
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = UpperHalfEllipsoidOfRevolutionFunc.getInstance(0.5 * d, 0.5 * zD, 0.0);
                result = Patterns.newSurface(projection, surf).shift(Point.valueOf(x, y, z));
                if (integerType) {
                    result = result.round();
                }

            } else if (s.startsWith("sphere ")) { // "sphere dCircle d zD zStep [x y [z]]", really half-sphere
                String[] params = s.substring("sphere ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zD = Double.parseDouble(params[2].trim()) * zScale;
                double zStep = Double.parseDouble(params[3].trim()) * zScale;
                if (integerType) {
                    zStep = Math.ceil(zStep);
                }
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                if (dCircle > d)
                    throw new IllegalArgumentException("Circle diameter is greater than sphere diameter");
                double x = 0, y = 0, z = 0;
                if (params.length >= 6) {
                    x = Double.parseDouble(params[4].trim());
                    y = Double.parseDouble(params[5].trim());
                }
                if (params.length >= 7) {
                    z = Double.parseDouble(params[6].trim()) * zScale;
                    if (integerType) {
                        z = Math.round(z);
                    }
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = UpperHalfEllipsoidOfRevolutionFunc.getInstance(0.5 * d, 0.5 * zD, 0.0);
                result = Patterns.newSpaceSegment(projection,
                        ConstantFunc.getInstance(surf.get(0.5 * dCircle, 0.0)),
                        surf,
                        0.0, zStep).shift(Point.valueOf(x, y, z));

            } else if (s.startsWith("hyperboloid-surface ")) { // "hyperboloid-surface dCircle d zAxis [x y [z]]"
                String[] params = s.substring("hyperboloid-surface ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zAxis = Double.parseDouble(params[2].trim()) * zScale;
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                double x = 0, y = 0, z = 0;
                if (params.length >= 5) {
                    x = Double.parseDouble(params[3].trim());
                    y = Double.parseDouble(params[4].trim());
                }
                if (params.length >= 6) {
                    z = Double.parseDouble(params[5].trim()) * zScale;
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = HyperboloidOfRevolutionFunc.getLowerInstance(0.5 * d, 0.5 * zAxis, 0.0);
                result = Patterns.newSurface(projection, surf).shift(Point.valueOf(x, y, z));
                if (integerType) {
                    result = result.round();
                }

            } else if (s.startsWith("hyperboloid ")) { // "hyperboloid dCircle d zAxis zStep [x y [z]]"
                String[] params = s.substring("hyperboloid ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zAxis = Double.parseDouble(params[2].trim()) * zScale;
                double zStep = Double.parseDouble(params[3].trim()) * zScale;
                if (integerType) {
                    zStep = Math.ceil(zStep);
                }
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                double x = 0, y = 0, z = 0;
                if (params.length >= 6) {
                    x = Double.parseDouble(params[4].trim());
                    y = Double.parseDouble(params[5].trim());
                }
                if (params.length >= 7) {
                    z = Double.parseDouble(params[6].trim()) * zScale;
                    if (integerType) {
                        z = Math.round(z);
                    }
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = HyperboloidOfRevolutionFunc.getLowerInstance(0.5 * d, 0.5 * zAxis, 0.0);
                result = Patterns.newSpaceSegment(projection,
                        ConstantFunc.getInstance(surf.get(0.5 * dCircle, 0.0)),
                        surf,
                        0.0, zStep).shift(Point.valueOf(x, y, z));

            } else if (s.startsWith("paraboloid-surface ")) { // "paraboloid-surface dCircle d zAxis [x y [z]]"
                String[] params = s.substring("paraboloid-surface ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zAxis = Double.parseDouble(params[2].trim()) * zScale;
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                double x = 0, y = 0, z = 0;
                if (params.length >= 5) {
                    x = Double.parseDouble(params[3].trim());
                    y = Double.parseDouble(params[4].trim());
                }
                if (params.length >= 6) {
                    z = Double.parseDouble(params[5].trim()) * zScale;
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = ParaboloidOfRevolutionFunc.getInstance(-zAxis / (0.5 * d * d), 0.0);
                result = Patterns.newSurface(projection, surf).shift(Point.valueOf(x, y, z));
                if (integerType) {
                    result = result.round();
                }

            } else if (s.startsWith("paraboloid ")) { // "paraboloid dCircle d zAxis zStep [x y [z]]"
                String[] params = s.substring("paraboloid ".length()).split("[, ]+");
                double dCircle = Double.parseDouble(params[0].trim());
                double d = Double.parseDouble(params[1].trim());
                double zAxis = Double.parseDouble(params[2].trim()) * zScale;
                double zStep = Double.parseDouble(params[3].trim()) * zScale;
                if (integerType) {
                    zStep = Math.ceil(zStep);
                }
                if (dCircle > 10000)
                    throw new IllegalArgumentException("Too large circle diameter: " + dCircle +
                            " (maximal possible value is 10000)");
                if (d <= 0.0)
                    throw new IllegalArgumentException("Zero or negative diameter: " + d);
                double x = 0, y = 0, z = 0;
                if (params.length >= 6) {
                    x = Double.parseDouble(params[4].trim());
                    y = Double.parseDouble(params[5].trim());
                }
                if (params.length >= 7) {
                    z = Double.parseDouble(params[6].trim()) * zScale;
                    if (integerType) {
                        z = Math.round(z);
                    }
                }
                UniformGridPattern projection = Patterns.newSphereIntegerPattern(Point.origin(2), 0.5 * dCircle);
                Func surf = ParaboloidOfRevolutionFunc.getInstance(-zAxis / (0.5 * d * d), 0.0);
                result = Patterns.newSpaceSegment(projection,
                        ConstantFunc.getInstance(surf.get(0.5 * dCircle, 0.0)),
                        surf,
                        0.0, zStep).shift(Point.valueOf(x, y, z));

            } else if (s.startsWith("series ")) { // "series [x0 y0] dx dy n" or "series [x0 y0 z0] dx dy dz n": points
                String[] params = s.substring("series ".length()).split("[, ]+");
                int paramIndex = 0;
                if (params.length >= 7 || params.length == 4) {
                    long x0 = params.length >= 7 ? Long.parseLong(params[paramIndex++].trim()) : 0;
                    long y0 = params.length >= 7 ? Long.parseLong(params[paramIndex++].trim()) : 0;
                    double z0 = params.length >= 7 ? Double.parseDouble(params[paramIndex++].trim()) * zScale : 0;
                    long dx = Long.parseLong(params[paramIndex++].trim());
                    long dy = Long.parseLong(params[paramIndex++].trim());
                    double dz = Double.parseDouble(params[paramIndex++].trim()) * zScale;
                    if (integerType) {
                        z0 = Math.round(z0);
                        dz = Math.round(dz);
                    }
                    int n = Integer.parseInt(params[paramIndex].trim());
                    result = getSeries(x0, y0, z0, dx, dy, dz, n);
                } else {
                    long x0 = params.length >= 5 ? Long.parseLong(params[paramIndex++].trim()) : 0;
                    long y0 = params.length >= 5 ? Long.parseLong(params[paramIndex++].trim()) : 0;
                    long dx = Long.parseLong(params[paramIndex++].trim());
                    long dy = Long.parseLong(params[paramIndex++].trim());
                    int n = Integer.parseInt(params[paramIndex].trim());
                    result = getSeries(x0, y0, dx, dy, n);
                }

            } else if (s.startsWith("points ")) { // "points x1 y1 [z1]; x2 y2 [z2]; ..."
                result = parsePoints(s, integerType, zScale);
                raw = false;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Illegal pattern description: \"" + s
                    + "\": " + ex.getMessage(), ex);
        }
        if (result == null)
            throw new IllegalArgumentException("Unsupported pattern description format: \"" + s + "\"");
        if (surface) {
            result = result.round().surface();
        }
        if (bounds) {
            result = Patterns.newUnion(
                    result.maxBound(0), result.maxBound(1),
                    result.minBound(0), result.minBound(1));
        }
        if (slow) {
            result = new SimplePattern(result.points());
        } else if (raw) {
            if (result instanceof UniformGridPattern) {
                UniformGridPattern ugResult = (UniformGridPattern) result;
                result = Patterns.newUniformGridPattern(ugResult.originOfGrid(),
                        ugResult.stepsOfGrid(),
                        ugResult.gridIndexPattern().roundedPoints());
            } else {
                result = Patterns.newPattern(result.points());
            }
        }
        synchronized (patternCache) {
            patternCache.put(s, new SoftReference<>(result));
        }
        final Pattern finalResult = result;
        LOG.log(System.Logger.Level.DEBUG, () -> "Pattern loaded from cache: " + finalResult);
        return result;
    }

    public static boolean isRectangularInteger(Pattern pattern) {
        return pattern instanceof UniformGridPattern
                && ((UniformGridPattern) pattern).isActuallyRectangular()
                && pattern.isSurelyInteger();
    }

    public static Pattern subtract(Pattern pattern1, Pattern pattern2) {
        if (isRectangularInteger(pattern1)
                && isRectangularInteger(pattern2)
                && pattern1.dimCount() == pattern2.dimCount()
//                && pattern1.dimCount() == 2
//                && pattern1.roundedCoordRange(0).contains(pattern2.roundedCoordRange(0))
//                && pattern1.roundedCoordRange(1).contains(pattern2.roundedCoordRange(1))
        ) {
            // this optimization allows to avoid processing patterns with a lot of points
            // and also increases speed of dilation / erosion in comparison with common O(D) algorithm
            // (because this "frame" usually leads to D operations instead log D)
            final IRectangularArea area1 = pattern1.roundedCoordArea();
            final IRectangularArea area2 = pattern2.roundedCoordArea();
            final List<Pattern> patternList = new ArrayList<>();
            for (IRectangularArea area : area1.difference(new ArrayList<>(), area2)) {
                patternList.add(Patterns.newRectangularIntegerPattern(area.ranges()));
            }
            return Patterns.newUnion(patternList);
/*
            // deprecated code
            final List<Pattern> b = new ArrayList<>();
            long x1Min = pattern1.roundedCoordRange(0).min();
            long x1Max = pattern1.roundedCoordRange(0).max();
            long x2Min = pattern2.roundedCoordRange(0).min();
            long x2Max = pattern2.roundedCoordRange(0).max();
            assert x1Min <= x2Min;
            assert x2Max <= x1Max;
            long y1Min = pattern1.roundedCoordRange(1).min();
            long y1Max = pattern1.roundedCoordRange(1).max();
            long y2Min = pattern2.roundedCoordRange(1).min();
            long y2Max = pattern2.roundedCoordRange(1).max();
            assert y1Min <= y2Min;
            assert y2Max <= y1Max;
            if (y2Min > y1Min) {
                b.add(Patterns.newRectangularIntegerPattern(pattern1.roundedCoordRange(0),
                        IRange.valueOf(y1Min, y2Min - 1)));
            }
            if (y2Max < y1Max) {
                b.add(Patterns.newRectangularIntegerPattern(pattern1.roundedCoordRange(0),
                        IRange.valueOf(y2Max + 1, y1Max)));
            }
            if (x2Min > x1Min) {
                b.add(Patterns.newRectangularIntegerPattern(IRange.valueOf(x1Min, x2Min - 1),
                        pattern2.roundedCoordRange(1)));
            }
            if (x2Max < x1Max) {
                b.add(Patterns.newRectangularIntegerPattern(IRange.valueOf(x2Max + 1, x1Max),
                        pattern2.roundedCoordRange(1)));
            }
            return Patterns.newUnion(b.toArray(new Pattern[0]));
 */
        } else {
            final Set<IPoint> points = new LinkedHashSet<>(pattern1.roundedPoints());
            // - we prefer stable order of points
            points.removeAll(pattern2.roundedPoints());
            return Patterns.newIntegerPattern(points);
        }
    }

    private static Pattern getSeries(long x0, long y0, long dx, long dy, int n) {
        if (n < 1)
            throw new IllegalArgumentException("Zero or negative length of the series: " + n);
        if (n == 1) {
            return Patterns.newIntegerPattern(IPoint.valueOf(x0, y0));
        } else {
            return Patterns.newMinkowskiMultiplePattern(
                            Patterns.newIntegerPattern(IPoint.origin(2), IPoint.valueOf(dx, dy)), n - 1)
                    .shift(Point.valueOf(x0, y0));
        }
    }

    private static Pattern parsePoints(String s, boolean integerType, double zScale) {
        Pattern result;
        String[] pointDescriptions = s.substring("points ".length()).split(";");
        Set<Point> points = new HashSet<>();
        for (String pd : pointDescriptions) {
            String[] coordDescriptions = pd.trim().split("[, ]+");
            double[] coordinates = new double[coordDescriptions.length];
            for (int j = 0; j < coordinates.length; j++) {
                coordinates[j] = Double.parseDouble(coordDescriptions[j].trim());
                if (j == 2) {
                    coordinates[j] *= zScale;
                    if (integerType) {
                        coordinates[j] = Math.round(coordinates[j]);
                    }
                }
            }
            points.add(Point.valueOf(coordinates));
        }
        result = Patterns.newPattern(points);
        return result;
    }

    private static Pattern parseBinaryMatrix2D(String s) {
        String[] lines = s.split("\\r(?!\\n)|\\n|\\r\\n");
        if (s.isEmpty() || lines.length == 0) {
            return null;
        }
        java.util.Arrays.setAll(lines, k -> lines[k].replaceAll("\\s+", ""));
        if (!java.util.Arrays.stream(lines).allMatch(line -> line.matches("^[01]+$"))) {
            return null;
        }
        final int dimX = lines[0].length();
        final int dimY = lines.length;
        for (int i = 1; i < dimY; i++) {
            if (lines[i].length() != dimX) {
                throw new IllegalArgumentException("Pattern is represented by 0/1 matrix, but lines #1 and #"
                        + (i + 1) + " of this matrix contains different number of characters 0/1");
            }
        }
        if (dimX <= 1 && dimY == 1) {
            throw new IllegalArgumentException("Pattern is represented by too small 0/1 matrix "
                    + dimX + "x" + dimY + ": at least one of dimensions must be >1");
        }
        final int centerX = dimX / 2;
        final int centerY = dimY / 2;
        final List<IPoint> points = new ArrayList<>();
        for (int y = 0; y < dimY; y++) {
            for (int x = 0; x < dimX; x++) {
                if (lines[y].charAt(x) != '0') {
                    points.add(IPoint.valueOf(x - centerX, y - centerY));
                }
            }
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Pattern is represented by 0/1 matrix, but all values are 0, "
                    + "it is prohibited: pattern must contain at least one point");
        }
        return Patterns.newIntegerPattern(points);
    }


    private static Pattern getSeries(long x0, long y0, double z0, long dx, long dy, double dz, int n) {
        if (n < 1)
            throw new IllegalArgumentException("Zero or negative length of the series: " + n);
        if (n == 1) {
            return Patterns.newPattern(Point.valueOf(x0, y0, z0));
        } else {
            return Patterns.newMinkowskiMultiplePattern(
                            Patterns.newPattern(Point.origin(3), Point.valueOf(dx, dy, dz)), n - 1)
                    .shift(Point.valueOf(x0, y0, z0));
        }
    }

    private static boolean isSurelyLowCase(String s) {
        for (int k = 0, n = s.length(); k < n; k++) {
            final char ch = s.charAt(k);
            if (ch >= 0x80 || (ch >= 'A' && ch <= 'Z')) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(isSurelyLowCase(args[0]));
        System.out.println(isSurelyLowCase(""));
        System.out.println(isSurelyLowCase("asd"));
        System.out.println(isSurelyLowCase("asF"));
    }
}
