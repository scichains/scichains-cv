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

package net.algart.executors.modules.cv.matrices.camera.reflectance;

import jakarta.json.JsonObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ReflectanceTranslationType {
    LINEAR("linear",
            ReflectanceSettings.LinearTranslation::new,
            ReflectanceSettings.LinearTranslation::new),
    LOG_LINEAR("loglinear",
            ReflectanceSettings.LogLinearTranslation::new,
            ReflectanceSettings.LogLinearTranslation::new);
    // - LOG_LINEAR was "coal" in terms of 16-bit COALCLAS from 1995-98 years

    private final String typeName;
    private final Supplier<ReflectanceSettings.Translation> factory;
    private final Function<JsonObject, ReflectanceSettings.Translation> jsonBasedFactory;

    private static final Map<String, ReflectanceTranslationType> ALL_TYPES = Stream.of(values()).collect(
            Collectors.toMap(ReflectanceTranslationType::typeName, e -> e));

    ReflectanceTranslationType(
            String typeName,
            Supplier<ReflectanceSettings.Translation> factory,
            Function<JsonObject, ReflectanceSettings.Translation> jsonBasedFactory) {
        this.typeName = typeName;
        this.factory = factory;
        this.jsonBasedFactory = jsonBasedFactory;
    }

    public static Collection<String> typeNames() {
        return Collections.unmodifiableCollection(ALL_TYPES.keySet());
    }

    public String typeName() {
        return typeName;
    }

    public ReflectanceSettings.Translation newSettings() {
        return factory.get();
    }

    public ReflectanceSettings.Translation newSettings(JsonObject json) {
        return jsonBasedFactory.apply(json);
    }

    public static Optional<ReflectanceTranslationType> fromTypeName(String typeName) {
        return Optional.ofNullable(ALL_TYPES.get(typeName));
    }
}
