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

package net.algart.executors.modules.cv.matrices.camera.reflectance;

public class ReflectanceTest {
    public static void main(String[] args) {
        final double a255 = 0.010428965598205074;
        final double a = a255 * 255;
        final double b = -0.14836185426185414;
        final Class<?> elementType = byte.class;

        double x = 177.1;
        // - important! possible values depend on element type, passed below
        final ReflectanceSettings settings = new ReflectanceSettings();
        settings.setTranslation(new ReflectanceSettings.LinearTranslation().setA255(a255).setB(b));

        double y = settings.intensityToReflectanceFunc(elementType).get(x);
        double z = settings.reflectanceToIntensityFunc(elementType).get(y);
        System.out.println("Intensity ro reflectance:                    " + x + ", " + y + ", " + z);

        y = settings.intensityToReflectanceFunc(elementType, true).get(x);
        z = settings.reflectanceToIntensityFunc(elementType, true).get(y);
        System.out.println("Intensity ro reflectance, maximal precision: " + x + ", " + y + ", " + z);

        y = a * (x / 255.0) + b;
        System.out.println("Emulating the calculating for linear translation:       " + x + ", " + y);

        y = a255 * x + b;
        System.out.println("By direct byte-oriented formula for linear translation: " + x + ", " + y);
        System.out.println();

        x = 2.04;
        y = settings.reflectanceToIntensity(x);
        z = settings.intensityToReflectance(y);
        System.out.println("Reflectance to intensity 0..1:   " + x + ", " + y + ", " + z);

        y = settings.reflectanceToIntensity255(x);
        z = settings.intensity255ToReflectance(y);
        System.out.println("Reflectance to intensity 0..255: " + x + ", " + y + ", " + z);
    }
}
