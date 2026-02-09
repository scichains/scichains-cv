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

package net.algart.executors.modules.cv.matrices.derivatives;

import net.algart.arrays.Matrices;
import net.algart.arrays.Matrix;
import net.algart.arrays.PArray;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

import java.util.List;

public enum HessianOperation {
    LAMBDA_1 {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValueFunc(true, orderEigenValues);
        }
    },
    LAMBDA_2 {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValueFunc(false, orderEigenValues);
        }
    },
    LAMBDA_1_PLUS {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValuePlusFunc(true, orderEigenValues);
        }
    },
    LAMBDA_2_PLUS {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValuePlusFunc(false, orderEigenValues);
        }
    },
    LAMBDA_1_MINUS {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValueMinusFunc(true, orderEigenValues);
        }
    },
    LAMBDA_2_MINUS {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenValueMinusFunc(false, orderEigenValues);
        }
    },
    VECTOR_1_X {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorFunc(true, true,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    },
    VECTOR_1_Y {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorFunc(true, false,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    },
    VECTOR_2_X {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorFunc(false, true,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    },
    VECTOR_2_Y {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorFunc(false, false,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    },
    VECTOR_1_SCALAR_PRODUCT {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorScalarProductFunc(true,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    },
    VECTOR_2_SCALAR_PRODUCT {
        @Override
        Func funcOfSecondDerivatives(boolean orderEigenValues, boolean stableSignumX, boolean normalizeEigenVectors) {
            return new EigenVectorScalarProductFunc(false,
                    orderEigenValues, stableSignumX, normalizeEigenVectors);
        }
    };

    private static final double DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD = 1e-7;
    private static final double COMPUTER_EPSILON = 1e-10;

    abstract Func funcOfSecondDerivatives(
            boolean orderEigenValues,
            boolean stableSignumX,
            boolean normalizeEigenVectors);

    public boolean additionalVectorRequired() {
        return this == VECTOR_1_SCALAR_PRODUCT || this == VECTOR_2_SCALAR_PRODUCT;
    }

    public <T extends PArray> Matrix<? extends T> asOperation(
            Class<? extends T> requiredType,
            Matrix<? extends PArray> d2dx2,
            Matrix<? extends PArray> d2dy2,
            Matrix<? extends PArray> d2dxdy,
            Matrix<? extends PArray> vectorX,
            Matrix<? extends PArray> vectorY,
            boolean orderEigenValues,
            boolean stableSignumX,
            boolean normalizeEigenVectors) {
        final List<Matrix<? extends PArray>> arguments = additionalVectorRequired() ?
                Matrices.several(PArray.class, d2dx2, d2dy2, d2dxdy, vectorX, vectorY) :
                Matrices.several(PArray.class, d2dx2, d2dy2, d2dxdy);
        return Matrices.asFuncMatrix(funcOfSecondDerivatives(
                orderEigenValues, stableSignumX, normalizeEigenVectors), requiredType, arguments);
    }

    private static class EigenValueFunc extends AbstractFunc {
        private final boolean firstValue;
        private final boolean orderedByDecreasingLambdaMagnitude;

        private EigenValueFunc(boolean firstValue, boolean orderedByDecreasingLambdaMagnitude) {
            this.firstValue = firstValue;
            this.orderedByDecreasingLambdaMagnitude = orderedByDecreasingLambdaMagnitude;
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1], x[2]);
        }

        // a == d2/dx2, b = d2/dy2, c = d2/dxdy
        @Override
        public double get(double a, double b, double c) {
            // (a - L) * (b - L) - c^2 = 0
            // L^2 - (a+b)L + ab-c^2 = 0
            // Discriminant: (a+b)^2 - 4*(ab-c^2) = (a-b)^2 + 4c^2
            // Eigenvalues: L = 0.5 * [(a+b) +/- sqrt((a-b)^2 + 4c^2)]
            double d = Math.sqrt((a - b) * (a - b) + 4 * c * c);
            if (orderedByDecreasingLambdaMagnitude) {
                double lambda1 = 0.5 * (a + b + d);
                double lambda2 = 0.5 * (a + b - d);
                boolean firstLess = lambda1 * lambda1 < lambda2 * lambda2;
                return firstValue == firstLess ? lambda2 : lambda1;
                // 1st value: firstLess ? lambda2 : lambda1 (max)
                // 2nd value: !firstLess ? lambda2 : lambda1 (min)
            } else {
                return firstValue ? 0.5 * (a + b + d) : 0.5 * (a + b - d);
            }
        }
    }

    private static class EigenValuePlusFunc extends AbstractFunc {
        private final boolean firstValue;
        private final boolean orderedByDecreasingLambdaMagnitude;

        private EigenValuePlusFunc(boolean firstValue, boolean orderedByDecreasingLambdaMagnitude) {
            this.firstValue = firstValue;
            this.orderedByDecreasingLambdaMagnitude = orderedByDecreasingLambdaMagnitude;
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1], x[2]);
        }

        // a == d2/dx2, b = d2/dy2, c = d2/dxdy
        @Override
        public double get(double a, double b, double c) {
            // (a - L) * (b - L) - c^2 = 0
            // L^2 - (a+b)L + ab-c^2 = 0
            // Discriminant: (a+b)^2 - 4*(ab-c^2) = (a-b)^2 + 4c^2
            // Eigenvalues: L = 0.5 * [(a+b) +/- sqrt((a-b)^2 + 4c^2)]
            double d = Math.sqrt((a - b) * (a - b) + 4 * c * c);
            // 3 cases: L1 >= L2 >= 0, L1 >= 0 >= L2, 0 >= L1 >= L2
            if (orderedByDecreasingLambdaMagnitude) {
                if (firstValue) {
                    double lambda1 = 0.5 * (a + b + d);
                    double lambda2 = 0.5 * (a + b - d);
                    // L1+: let's compare L1+ with L2-
                    if (lambda1 * lambda1 >= lambda2 * lambda2) {
                        // |L1| > |L2| only when L1 >= 0 (in other case 0 > L1 >= L2); so, |L1+| > |L2| >= |L2-|
                        return lambda1;
                    } else {
                        // |L2| > |L1| only when L2 <= 0 (in other case L1 >= L2 > 0); so, |L2-| > |L1| >= |L1+|
                        return lambda2;
                    }
                } else {
                    // L2+: let's compare L2+ with L1-
                    // If L2 >= 0, then |L1-| = 0 <= |L2+|, we choose L1- = 0
                    // If L1 <= 0, then |L2+| = 0 <= |L1-|, we choose L2+ = 0
                    // If L1 > 0 > L2, then L2+ = L1- = 0
                    return 0.0;
                }
            } else {
                double result = firstValue ? 0.5 * (a + b + d) : 0.5 * (a + b - d);
                return result >= 0.0 ? result : 0.0;
            }
        }
    }

    private static class EigenValueMinusFunc extends AbstractFunc {
        private final boolean firstValue;
        private final boolean orderedByDecreasingLambdaMagnitude;

        private EigenValueMinusFunc(boolean firstValue, boolean orderedByDecreasingLambdaMagnitude) {
            this.firstValue = firstValue;
            this.orderedByDecreasingLambdaMagnitude = orderedByDecreasingLambdaMagnitude;
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1], x[2]);
        }

        // a == d2/dx2, b = d2/dy2, c = d2/dxdy
        @Override
        public double get(double a, double b, double c) {
            // (a - L) * (b - L) - c^2 = 0
            // L^2 - (a+b)L + ab-c^2 = 0
            // Discriminant: (a+b)^2 - 4*(ab-c^2) = (a-b)^2 + 4c^2
            // Eigenvalues: L = 0.5 * [(a+b) +/- sqrt((a-b)^2 + 4c^2)]
            // 3 cases: L1 >= L2 >= 0, L1 >= 0 >= L2, 0 >= L1 >= L2
            if (orderedByDecreasingLambdaMagnitude) {
                if (firstValue) {
                    double d = Math.sqrt((a - b) * (a - b) + 4 * c * c);
                    double lambda1 = 0.5 * (a + b + d);
                    double lambda2 = 0.5 * (a + b - d);
                    // L1-: let's compare L2+ with L1-
                    if (lambda2 >= 0.0) {
                        // So, L1- = 0: choose L2 (|L2+| >= 0 = |L1-|)
                        return lambda2;
                    } else if (lambda1 <= 0.0) {
                        // So, L2+ = 0: choose L1 (|L1-| >= 0 = |L2+|)
                        return lambda1;
                    } else {
                        // L1 > 0 > L2: L2+ = 0 and L1- = 0
                        return 0.0;
                    }
                } else {
                    double d = Math.sqrt((a - b) * (a - b) + 4 * c * c);
                    double lambda1 = 0.5 * (a + b + d);
                    double lambda2 = 0.5 * (a + b - d);
                    // L2-: let's compare L1+ with L2-
                    if (lambda2 >= 0.0
                            // So, |L1| >= |L2|, we choose L2- (2nd by magnitude), and it is 0.0
                            || lambda1 <= 0.0) {
                        // So, |L2| >= |L1|, we choose L1+ (2nd by magnitude), and it is 0.0
                        return 0.0;
                    } else {
                        // L1 > 0 > L2: L2- = L2 and L1+ = L1
                        return lambda1 >= -lambda2 ? lambda2 : lambda1;
                    }
                }
            } else {
                double d = Math.sqrt((a - b) * (a - b) + 4 * c * c);
                double lambda1 = 0.5 * (a + b + d);
                double lambda2 = 0.5 * (a + b - d);
                double result = firstValue ? lambda1 : lambda2;
                return result <= 0.0 ? result : 0.0;
            }
        }
    }

    private static class EigenVectorFunc extends AbstractFunc {
        private final boolean firstValue;
        private final boolean xComponent;
        private final boolean orderedByDecreasingLambdaMagnitude;
        private final boolean stableSignumX;
        private final boolean normalize;

        private EigenVectorFunc(
                boolean firstValue,
                boolean xComponent,
                boolean orderedByDecreasingLambdaMagnitude,
                boolean stableSignumX,
                boolean normalize) {
            this.firstValue = firstValue;
            this.xComponent = xComponent;
            this.orderedByDecreasingLambdaMagnitude = orderedByDecreasingLambdaMagnitude;
            this.stableSignumX = stableSignumX;
            this.normalize = normalize;
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1], x[2]);
        }

        // a == d2/dx2, b = d2/dy2, c = d2/dxdy
        @Override
        public double get(double a, double b, double c) {
            final double abAbs = Math.abs(a - b);
            final double cAbs = Math.abs(c);
            final double d = Math.sqrt(abAbs * abAbs + 4 * c * c);
            // The following algorithm provides little better precision, but need special check a=b=c=0
            // if (abAbs > cAbs) {
            //     double temp = 2 * c / abAbs;
            //     d = abAbs * Math.sqrt(1.0 + temp * temp);
            // } else {
            //     double temp = abAbs / (2 * c);
            //     d = cAbs * Math.sqrt(temp * temp + 1.0);
            // }
            boolean useFirst = firstValue;
            if (orderedByDecreasingLambdaMagnitude) {
                final double lambda1Doubled = a + b + d;
                final double lambda2Doubled = a + b - d;
                if (lambda1Doubled * lambda1Doubled < lambda2Doubled * lambda2Doubled) {
                    // 1st must be max, 2nd must be min
                    useFirst = !useFirst;
                }
            }
            // (a - L) * (b - L) - c^2 = 0
            // L^2 - (a+b)L + ab-c^2 = 0
            // L = 0.5 * [(a+b) +/- sqrt((a-b)^2 + 4c^2)]
            // - 1st/2nd eigenvalues
            // ax+cy = Lx  <=>  cy = (L-a)x
            // cx+by = Ly  <=>  cx = (L-b)y  (for eigenvalues, it is equivalent to ax+cy=Lx)
            // y : x = (L-a) : c = [(b-a) +/- sqrt((a-b)^2 + 4c^2)] : 2c or
            // x : y = (L-b) : c = [(a-b) +/- sqrt((a-b)^2 + 4c^2)] : 2c
            // - eigenvectors (for "+" sign, i.e. 1st eigenvector,
            // the first formula is better if a<=b / a>=b (1st/2nd eigenvector),
            // the second is better if a>=b / a<=b (1st/2nd eigenvector).
            if (stableSignumX) {
                // Let
                // x1 = a - b + d (>=0, because d >= |a-b|)
                // y1 = 2 * c
                // x2 = a - b - d (<=0, because d >= |a-b|)
                // y2 = 2 * c (=y1)
                // in other words, 1st eigenvector is directed to-right, 2nd to-left.
                // We see that x1*x2 + y1*y2 = (a-b)^2 - d^2 + 4c^2 = 0 (eigenvectors must be orthogonal).
                if (useFirst) {
                    final double y = 2 * c;
                    final double x = a > b ? abAbs + d : d > COMPUTER_EPSILON ? y * y / (d + abAbs) : 0.0;
                    // x = a - b + d, a < b: (d^2 - (b-a)^2) / (d+b-a) = 4c^2 / (d+abAbs)
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            return xComponent ? 1.0 : 0.0;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return xComponent ? x / norm : y / norm;
                    } else {
                        return xComponent ? x : y;
                    }
                } else {
                    final double y = 2 * c;
                    final double x = a > b ? -y * y / (d + abAbs) : d > COMPUTER_EPSILON ? -abAbs - d : -0.0;
                    // x = a - b - d, a > b : ((a-b)^2 - d^2) / (a-b+d) = -4c^2 / (d+abAbs)
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            return xComponent ? -1.0 : 0.0;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return xComponent ? x / norm : y / norm;
                    } else {
                        return xComponent ? x : y;
                    }
                }
            } else {
                // Let's consider alternative:
                // x1 = 2 * c
                // y1 = b - a + d (>=0, because d >= |a-b|)
                // x2 = 2 * c (=x1)
                // y2 = b - a - d (<=0, because d >= |a-b|)
                if (useFirst) {
                    double x = a >= b ? a - b + d : 2 * c;
                    double y = a >= b ? 2 * c : b - a + d;
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            x = a >= b ? 1.0 : 0.0;
                            y = a >= b ? 0.0 : 1.0;
                            return xComponent ? x : y;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return xComponent ? x / norm : y / norm;
                    } else {
                        return xComponent ? x : y;
                    }
                } else {
                    double x = a >= b ? 2 * c : a - b - d;
                    double y = a >= b ? b - a - d : 2 * c;
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            x = a >= b ? 0.0 : -1.0;
                            y = a >= b ? -1.0 : 0.0;
                            return xComponent ? x : y;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return xComponent ? x / norm : y / norm;
                    } else {
                        return xComponent ? x : y;
                    }
                }
            }
        }
    }

    private static class EigenVectorScalarProductFunc extends AbstractFunc {
        private final boolean firstValue;
        private final boolean orderedByDecreasingLambdaMagnitude;
        private final boolean stableSignumX;
        private final boolean normalize;

        private EigenVectorScalarProductFunc(
                boolean firstValue,
                boolean orderedByDecreasingLambdaMagnitude,
                boolean stableSignumX,
                boolean normalize) {
            this.firstValue = firstValue;
            this.orderedByDecreasingLambdaMagnitude = orderedByDecreasingLambdaMagnitude;
            this.stableSignumX = stableSignumX;
            this.normalize = normalize;
        }

        @Override
        public double get(double... x) {
            return get(x[0], x[1], x[2], x[3], x[4]);
        }

        // a == d2/dx2, b = d2/dy2, c = d2/dxdy
        public double get(double a, double b, double c, double vX, double vY) {
            final double abAbs = Math.abs(a - b);
            final double cAbs = Math.abs(c);
            final double d = Math.sqrt(abAbs * abAbs + 4 * c * c);
            // The following algorithm provides little better precision, but need special check a=b=c=0
            // if (abAbs > cAbs) {
            //     double temp = 2 * c / abAbs;
            //     d = abAbs * Math.sqrt(1.0 + temp * temp);
            // } else {
            //     double temp = abAbs / (2 * c);
            //     d = cAbs * Math.sqrt(temp * temp + 1.0);
            // }
            boolean useFirst = firstValue;
            if (orderedByDecreasingLambdaMagnitude) {
                final double lambda1Doubled = a + b + d;
                final double lambda2Doubled = a + b - d;
                if (lambda1Doubled * lambda1Doubled < lambda2Doubled * lambda2Doubled) {
                    // 1st must be max, 2nd must be min
                    useFirst = !useFirst;
                }
            }
            if (stableSignumX) {
                // Let
                // x1 = a - b + d (>=0, because d >= |a-b|)
                // y1 = 2 * c
                // x2 = a - b - d (<=0, because d >= |a-b|)
                // y2 = 2 * c (=y1)
                // in other words, 1st eigenvector is directed to-right, 2nd to-left.
                // We see that x1*x2 + y1*y2 = (a-b)^2 - d^2 + 4c^2 = 0 (eigenvectors must be orthogonal).
                if (useFirst) {
                    final double y = 2 * c;
                    final double x = a > b ? abAbs + d : d > COMPUTER_EPSILON ? y * y / (d + abAbs) : 0.0;
                    // x = a - b + d, a < b: (d^2 - (b-a)^2) / (d+b-a) = 4c^2 / (d+abAbs)
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            return vX;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return (x * vX + y * vY) / norm;
                    } else {
                        return x * vX + y * vY;
                    }
                } else {
                    final double y = 2 * c;
                    final double x = a > b ? -y * y / (d + abAbs) : d > COMPUTER_EPSILON ? -abAbs - d : -0.0;
                    // x = a - b - d, a > b : ((a-b)^2 - d^2) / (a-b+d) = -4c^2 / (d+abAbs)
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            return -vX;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return (x * vX + y * vY) / norm;
                    } else {
                        return x * vX + y * vY;
                    }
                }
            } else {
                // Let's consider alternative:
                // x1 = 2 * c
                // y1 = b - a + d (>=0, because d >= |a-b|)
                // x2 = 2 * c (=x1)
                // y2 = b - a - d (<=0, because d >= |a-b|)
                if (useFirst) {
                    double x = a >= b ? a - b + d : 2 * c;
                    double y = a >= b ? 2 * c : b - a + d;
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            // x = a >= b ? 1.0 : 0.0;
                            // y = a >= b ? 0.0 : 1.0;
                            return a >= b ? vX : vY;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return (x * vX + y * vY) / norm;
                    } else {
                        return x * vX + y * vY;
                    }
                } else {
                    double x = a >= b ? 2 * c : a - b - d;
                    double y = a >= b ? b - a - d : 2 * c;
                    if (normalize) {
                        if (cAbs < DEFAULT_HESSIAN_D2DXDY_NORMALIZING_THRESHOLD) {
                            // x = a >= b ? 0.0 : -1.0;
                            // y = a >= b ? -1.0 : 0.0;
                            return a >= b ? -vY : -vX;
                        }
                        double norm = Math.sqrt(x * x + y * y);
                        return (x * vX + y * vY) / norm;
                    } else {
                        return x * vX + y * vY;
                    }
                }
            }
        }
    }
}
