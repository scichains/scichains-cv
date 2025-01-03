/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.FloatArray;
import net.algart.executors.api.Executor;
import net.algart.executors.modules.core.common.io.PathPropertyReplacement;
import net.algart.executors.modules.core.common.matrices.MultiMatrixFilter;
import net.algart.executors.modules.cv.matrices.camera.reflectance.ReflectanceSettings;
import net.algart.math.functions.Func;
import net.algart.multimatrix.MultiMatrix;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class IntensityToReflectance extends MultiMatrixFilter {
    public static final String OUTPUT_REFLECTANCE_SETTINGS = "reflectance_settings";

    private String reflectanceSettingsFile = "";
    private boolean maximalPrecision = false;

    public IntensityToReflectance() {
        addOutputScalar(OUTPUT_REFLECTANCE_SETTINGS);
    }

    public String getReflectanceSettingsFile() {
        return reflectanceSettingsFile;
    }

    public IntensityToReflectance setReflectanceSettingsFile(String reflectanceSettingsFile) {
        this.reflectanceSettingsFile = nonEmpty(reflectanceSettingsFile);
        return this;
    }

    public boolean isMaximalPrecision() {
        return maximalPrecision;
    }

    public void setMaximalPrecision(boolean maximalPrecision) {
        this.maximalPrecision = maximalPrecision;
    }

    @Override
    public MultiMatrix process(MultiMatrix source) {
        final ReflectanceSettings settings = loadReflectanceSettings(
                this, reflectanceSettingsFile);
        final Func f = settings.intensityToReflectanceFunc(source.elementType(), maximalPrecision);
        return source.asMono().asFunc(f, FloatArray.class).clone();
    }

    public static Path translateReflectanceSettingsFile(Executor executor, String file) {
        Objects.requireNonNull(executor, "Null executor");
        Objects.requireNonNull(file, "Null file");
        if ((file = file.trim()).isEmpty()) {
            throw new IllegalArgumentException("Empty file name");
        }
        return PathPropertyReplacement.translatePropertiesAndCurrentDirectory(file, executor);
    }

    public static ReflectanceSettings loadReflectanceSettings(Executor executor, String file) {
        final Path path = translateReflectanceSettingsFile(executor, file);
        try {
            final ReflectanceSettings settings = ReflectanceSettings.read(path);
            if (executor.hasOutputPort(OUTPUT_REFLECTANCE_SETTINGS)) {
                executor.getScalar(OUTPUT_REFLECTANCE_SETTINGS).setTo(settings.jsonString());
            }
            return settings;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
