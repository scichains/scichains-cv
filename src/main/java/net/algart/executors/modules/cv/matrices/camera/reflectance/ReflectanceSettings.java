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

import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import net.algart.arrays.Arrays;
import net.algart.arrays.PArray;
import net.algart.executors.api.parameters.ParameterValueType;
import net.algart.json.AbstractConvertibleToJson;
import net.algart.json.Jsons;
import net.algart.math.functions.AbstractFunc;
import net.algart.math.functions.Func;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

public class ReflectanceSettings extends AbstractConvertibleToJson {
    public static final String APP_NAME = "reflectance-settings";
    public static final String CURRENT_VERSION = "1.0";

    public abstract static class Translation extends AbstractConvertibleToJson {
        private final double byteScale = 1.0 / 255.0;

        protected abstract double intensityToReflectance(double intensity);

        protected abstract double reflectanceToIntensity(double reflectance);

        protected double intensity255ToReflectance(double intensity) {
            return intensityToReflectance(intensity * byteScale);
        }

        protected double reflectanceToIntensity255(double reflectance) {
            return reflectanceToIntensity(reflectance) * 255.0;
        }
    }

    public static class LinearTranslation extends Translation {
        double a;
        double a255;
        double aInv;
        double b;
        double bInv;
        // - "a" was "Linear.G" in terms of 16-bit COALCLAS from 1995-98 years, "b" was "Linear.A"

        public LinearTranslation() {
            initialize();
            correct();
        }

        public LinearTranslation(JsonObject json) {
            initialize();
            correct();
            if (json.containsKey("a255")) {
                // - for compatibility with "Linear.G" value in 16-bit COALCLAS;
                // this key has priority over "a"
                setA255(Jsons.reqDouble(json, "a255"));
            } else {
                setA(Jsons.reqDouble(json, "a"));
            }
            setB(Jsons.reqDouble(json, "b"));
        }

        public double getA() {
            return a;
        }

        public LinearTranslation setA(double a) {
            this.a = a;
            this.a255 = a / 255.0;
            correct();
            return this;
        }

        public LinearTranslation setA255(double a255) {
            this.a = a255 * 255.0;
            this.a255 = a255;
            correct();
            return this;
        }

        public double getB() {
            return b;
        }

        public LinearTranslation setB(double b) {
            this.b = b;
            correct();
            return this;
        }

        @Override
        public void buildJson(JsonObjectBuilder builder) {
            builder.add("a", a);
            builder.add("a255", a / 255.0);
            builder.add("b", b);
        }

        @Override
        public String toString() {
            return "LinearTranslationSettings{" +
                    "a=" + a +
                    ", b=" + b +
                    '}';
        }

        void initialize() {
            a255 = 0.02005625;
            a = a255 * 255.0;
            b = -2.1108;
        }

        @Override
        protected double intensityToReflectance(double intensity) {
            return a * intensity + b;
        }

        @Override
        protected double reflectanceToIntensity(double reflectance) {
            return (reflectance - b) * aInv;
        }

        @Override
        protected double intensity255ToReflectance(double intensity) {
            return a255 * intensity + b;
        }

        @Override
        protected double reflectanceToIntensity255(double reflectance) {
            return (reflectance - b) / a255;
        }

        private void correct() {
            this.aInv = 1.0 / a;
            this.bInv = 1.0 / b;
        }
    }

    public static class LogLinearTranslation extends LinearTranslation {
        public LogLinearTranslation() {
        }

        public LogLinearTranslation(JsonObject json) {
            super(json);
        }

        @Override
        void initialize() {
            a255 = 0.02430;
            a = a255 * 255.0;
            b = 0.0198888;
        }

        @Override
        protected double intensityToReflectance(double intensity) {
            return b * Math.exp(intensity * a);
        }

        @Override
        protected double reflectanceToIntensity(double reflectance) {
            return Math.log(reflectance * bInv) * aInv;
        }

        @Override
        protected double intensity255ToReflectance(double intensity) {
            return b * Math.exp(intensity * a255);
        }

        @Override
        protected double reflectanceToIntensity255(double reflectance) {
            return Math.log(reflectance / b) * a255;
        }
    }

    private Path reflectanceJsonFile = null;
    private String version = CURRENT_VERSION;
    private ReflectanceTranslationType type = ReflectanceTranslationType.LINEAR;
    private Translation translation = new LinearTranslation();

    public ReflectanceSettings() {
    }

    public ReflectanceSettings(JsonObject json, Path file) {
        if (!isReflectanceJson(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a reflectance settings: no \"app\":\""
                    + APP_NAME + "\" element");
        }
        this.reflectanceJsonFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        final String type = json.getString("type", ParameterValueType.STRING.typeName());
        this.type = ReflectanceTranslationType.valueOfTypeNameOrNull(type);
        Jsons.requireNonNull(this.type, json,
                "type", "unknown translation type (\"" + type + "\")", file);
        this.translation = this.type.newSettings(Jsons.reqJsonObject(json, "translation", file));
    }

    public static ReflectanceSettings read(Path reflectanceJsonFile) throws IOException {
        Objects.requireNonNull(reflectanceJsonFile, "Null reflectanceJsonFile");
        final JsonObject json = Jsons.readJson(reflectanceJsonFile);
        return new ReflectanceSettings(json, reflectanceJsonFile);
    }

    public void write(Path reflectanceJsonFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(reflectanceJsonFile, "Null reflectanceJsonFile");
        Files.writeString(reflectanceJsonFile, Jsons.toPrettyString(toJson()), options);
    }

    public static ReflectanceSettings valueOf(JsonObject reflectanceJson) {
        return new ReflectanceSettings(reflectanceJson, null);
    }

    public static ReflectanceSettings valueOf(String reflectanceJsonString) {
        return valueOf(Jsons.toJson(reflectanceJsonString));
    }

    public static boolean isReflectanceJson(JsonObject reflectanceJson) {
        Objects.requireNonNull(reflectanceJson, "Null reflectance JSON");
        return APP_NAME.equals(reflectanceJson.getString("app", null));
    }

    public Path getReflectanceJsonFile() {
        return reflectanceJsonFile;
    }

    public String getVersion() {
        return version;
    }

    public ReflectanceSettings setVersion(String version) {
        this.version = nonNull(version);
        return this;
    }

    public ReflectanceTranslationType getType() {
        return type;
    }

    public ReflectanceSettings setType(ReflectanceTranslationType type) {
        this.type = nonNull(type);
        this.translation = this.type.newSettings();
        return this;
    }

    public Translation getTranslation() {
        return translation;
    }

    public ReflectanceSettings setTranslation(Translation translation) {
        this.translation = nonNull(translation);
        return this;
    }

    /**
     * Converts intensity of pixels to reflection coefficient.
     *
     * @param intensity intensity of pixels, from 0.0 (black) to 1.0 (white).
     * @return reflection coefficient, usually from 0 (black) to 100 (ideal mirror).
     */
    public double intensityToReflectance(double intensity) {
        return translation.intensityToReflectance(intensity);
    }

    /**
     * Converts reflection coefficient to intensity of pixels.
     *
     * @param reflectance reflection coefficient, usually from 0 (black) to 100 (ideal mirror).
     * @return intensity of pixels, from 0.0 (black) to 1.0 (white).
     */
    public double reflectanceToIntensity(double reflectance) {
        return translation.reflectanceToIntensity(reflectance);
    }

    /**
     * An analog of {@link #intensityToReflectance(double)}, providing absolute precision
     * when the translation parameters are specified in terms of 8-bit intensity 0..255,
     * like in the method {@link LinearTranslation#setA255(double)}.
     *
     * @param intensity intensity of pixels, from 0.0 (black) to 255.0 (white).
     * @return reflection coefficient, usually from 0 (black) to 100 (ideal mirror).
     */
    public double intensity255ToReflectance(double intensity) {
        return translation.intensity255ToReflectance(intensity);
    }

    /**
     * An analog of {@link #reflectanceToIntensity255(double)}, providing absolute precision
     * when the translation parameters are specified in terms of 8-bit intensity 0..255,
     * like in the method {@link LinearTranslation#setA255(double)}.
     *
     * @param reflectance reflection coefficient, usually from 0 (black) to 100 (ideal mirror).
     * @return intensity of pixels, from 0.0 (black) to 255.0 (white).
     */
    public double reflectanceToIntensity255(double reflectance) {
        return translation.reflectanceToIntensity255(reflectance);
    }

    public Func normalizedIntensityToReflectanceFunc() {
        return new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0]);
            }

            @Override
            public double get(double x0) {
                return intensityToReflectance(x0);
            }
        };
    }

    public Func intensityToReflectanceFunc(Class<?> intensityElementType) {
        return intensityToReflectanceFunc(intensityElementType, false);
    }

    public Func intensityToReflectanceFunc(Class<?> intensityElementType, boolean maximalPrecision) {
        Objects.requireNonNull(intensityElementType, "Null intensityElementType");
        if (Arrays.isFloatingPointElementType(intensityElementType)) {
            return normalizedIntensityToReflectanceFunc();
        }
        if (maximalPrecision) {
            if (intensityElementType == byte.class) {
                return new AbstractFunc() {
                    @Override
                    public double get(double... x) {
                        return get(x[0]);
                    }

                    @Override
                    public double get(double x0) {
                        return intensity255ToReflectance(x0);
                    }
                };
            }
            return new AbstractFunc() {
                final double scale = Arrays.maxPossibleValue(Arrays.type(PArray.class, intensityElementType));

                @Override
                public double get(double... x) {
                    return get(x[0]);
                }

                @Override
                public double get(double x0) {
                    return intensityToReflectance(x0 / scale);
                }
            };
        }
        return new AbstractFunc() {
            final double scale = 1.0 / Arrays.maxPossibleValue(Arrays.type(PArray.class, intensityElementType));

            @Override
            public double get(double... x) {
                return get(x[0]);
            }

            @Override
            public double get(double x0) {
                return intensityToReflectance(x0 * scale);
            }
        };
    }


    public Func reflectanceToNormalizedIntensityFunc() {
        return new AbstractFunc() {
            @Override
            public double get(double... x) {
                return get(x[0]);
            }

            @Override
            public double get(double x0) {
                return reflectanceToIntensity(x0);
            }
        };
    }

    public Func reflectanceToIntensityFunc(Class<?> intensityElementType) {
        return reflectanceToIntensityFunc(intensityElementType, false);
    }

    public Func reflectanceToIntensityFunc(Class<?> intensityElementType, boolean maximalPrecision) {
        Objects.requireNonNull(intensityElementType, "Null intensityElementType");
        if (Arrays.isFloatingPointElementType(intensityElementType)) {
            return reflectanceToNormalizedIntensityFunc();
        }
        if (maximalPrecision && intensityElementType == byte.class) {
            return new AbstractFunc() {
                @Override
                public double get(double... x) {
                    return get(x[0]);
                }

                @Override
                public double get(double x0) {
                    return Math.round(reflectanceToIntensity255(x0));
                }
            };
        }
        return new AbstractFunc() {
            final double scale = Arrays.maxPossibleValue(Arrays.type(PArray.class, intensityElementType));

            @Override
            public double get(double... x) {
                return get(x[0]);
            }

            @Override
            public double get(double x0) {
                return Math.round(reflectanceToIntensity(x0) * scale);
            }
        };

    }

    @Override
    public void buildJson(JsonObjectBuilder builder) {
        builder.add("app", APP_NAME);
        builder.add("version", version);
        builder.add("type", type.typeName());
        builder.add("translation", translation.toJson());
    }

    @Override
    public String toString() {
        return "ReflectanceSettings{" +
                "reflectanceJsonFile=" + reflectanceJsonFile +
                ", version='" + version + '\'' +
                ", type=" + type +
                ", translation=" + translation +
                '}';
    }
}
