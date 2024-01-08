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

package net.algart.executors.modules.cv.matrices.camera;

import net.algart.executors.modules.cv.matrices.camera.reflectance.ReflectanceSettings;
import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.executors.modules.core.common.matrices.MultiMatrixGenerator;

public final class ReflectanceToIntensity extends MultiMatrixFilter {
    private String reflectanceSettingsFile = "";
    private Class<?> elementType = float.class;
    private boolean maximalPrecision = false;

    public ReflectanceToIntensity() {
        addOutputScalar(IntensityToReflectance.OUTPUT_REFLECTANCE_SETTINGS);
    }

    public String getReflectanceSettingsFile() {
        return reflectanceSettingsFile;
    }

    public ReflectanceToIntensity setReflectanceSettingsFile(String reflectanceSettingsFile) {
        this.reflectanceSettingsFile = nonEmpty(reflectanceSettingsFile);
        return this;
    }

    public Class<?> getElementType() {
        return elementType;
    }

    public void setElementType(Class<?> elementType) {
        this.elementType = nonNull(elementType);
    }

    public void setElementType(String elementType) {
        setElementType(MultiMatrixGenerator.elementType(elementType));
    }

    public boolean isMaximalPrecision() {
        return maximalPrecision;
    }

    public void setMaximalPrecision(boolean maximalPrecision) {
        this.maximalPrecision = maximalPrecision;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        final ReflectanceSettings settings = IntensityToReflectance.loadReflectanceSettings(
                this, reflectanceSettingsFile);
        final Func f = settings.reflectanceToIntensityFunc(elementType, maximalPrecision);
        return source.asMono().asFunc(f, Arrays.type(PArray.class, elementType)).clone();
    }
}
