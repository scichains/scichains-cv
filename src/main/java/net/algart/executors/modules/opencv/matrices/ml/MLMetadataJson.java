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

package net.algart.executors.modules.opencv.matrices.ml;

import net.algart.json.Jsons;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class MLMetadataJson {
    public static final String APP_NAME = "machine-learning";
    public static final String CURRENT_VERSION = "1.0";

    public static final String METADATA_FILE_SUFFIX = ".meta";

    private Path mlMetadataJsonFile = null;
    private String version = CURRENT_VERSION;
    private MLKind modelKind = null;
    private String createdBy = null;
    private JsonObject parameters = null;

    public MLMetadataJson() {
    }

    private MLMetadataJson(JsonObject json, Function<String, Optional<MLKind>> modelNameToKind, Path file) {
        if (!isMLMetadataJson(json)) {
            throw new JsonException("JSON" + (file == null ? "" : " " + file)
                    + " is not a metadata for ML statistical model mapping configuration: no \"app\":\""
                    + APP_NAME + "\" element");
        }
        Objects.requireNonNull(modelNameToKind, "Null modelNameToKind function");
        this.mlMetadataJsonFile = file;
        this.version = json.getString("version", CURRENT_VERSION);
        final String modelKind = json.getString("model_kind", null);
        if (modelKind != null) {
            this.modelKind = modelNameToKind.apply(modelKind).orElse(null);
            Jsons.requireNonNull(this.modelKind, json, "model_kind", file);
        }
        this.createdBy = json.getString("created_by", null);
        this.parameters = json.getJsonObject("parameters");
    }

    public static MLMetadataJson read(Path mlMetadataJsonFile, Function<String, Optional<MLKind>> modelNameToKind)
            throws IOException {
        Objects.requireNonNull(mlMetadataJsonFile, "Null mlMetadataJsonFile");
        Objects.requireNonNull(modelNameToKind, "Null modelNameToKind function");
        final JsonObject json = Jsons.readJson(mlMetadataJsonFile);
        return new MLMetadataJson(json, modelNameToKind, mlMetadataJsonFile);
    }

    public static MLMetadataJson readIfValid(
            Path mlMetadataJsonFile,
            Function<String, Optional<MLKind>> modelNameToKind)
            throws IOException {
        Objects.requireNonNull(mlMetadataJsonFile, "Null mlMetadataJsonFile");
        Objects.requireNonNull(modelNameToKind, "Null modelNameToKind function");
        final JsonObject json = Jsons.readJson(mlMetadataJsonFile);
        if (!isMLMetadataJson(json)) {
            return null;
        }
        return new MLMetadataJson(json, modelNameToKind, mlMetadataJsonFile);
    }

    public void write(Path mlMetadataJsonFile, OpenOption... options) throws IOException {
        Objects.requireNonNull(mlMetadataJsonFile, "Null mlMetadataJsonFile");
        Files.write(mlMetadataJsonFile, Jsons.toPrettyString(toJson()).getBytes(StandardCharsets.UTF_8), options);
    }

    public static Path metadataFile(Path mainModelFile) throws IOException {
        Objects.requireNonNull(mainModelFile, "Null mainModelFile");
        return metadataFile(mainModelFile.toString());
    }

    public static Path metadataFile(String mainModelFile) throws IOException {
        Objects.requireNonNull(mainModelFile, "Null mainModelFile");
        return Paths.get(mainModelFile + METADATA_FILE_SUFFIX);
    }

    public static MLMetadataJson valueOf(
            JsonObject mlMetadataJson,
            Function<String, Optional<MLKind>> modelNameToKind) {
        Objects.requireNonNull(modelNameToKind, "Null modelNameToKind function");
        return new MLMetadataJson(mlMetadataJson, modelNameToKind, null);
    }

    public static boolean isMLMetadataJson(JsonObject mlMetadataJson) {
        Objects.requireNonNull(mlMetadataJson, "Null mapping JSON");
        return APP_NAME.equals(mlMetadataJson.getString("app", null));
    }

    public Path getMLMetadataJsonFile() {
        return mlMetadataJsonFile;
    }

    public String getVersion() {
        return version;
    }

    public MLMetadataJson setVersion(String version) {
        this.version = version;
        return this;
    }

    public MLKind getModelKind() {
        return modelKind;
    }

    public MLMetadataJson setModelKind(MLKind modelKind) {
        this.modelKind = modelKind;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public MLMetadataJson setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public MLMetadataJson setParameters(JsonObject parameters) {
        this.parameters = parameters;
        return this;
    }

    public final JsonObject toJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("app", APP_NAME);
        builder.add("version", version);
        if (modelKind != null) {
            builder.add("model_kind", modelKind.modelName());
        }
        if (createdBy != null) {
            builder.add("created_by", createdBy);
        }
        if (parameters != null) {
            builder.add("parameters", parameters);
        }
        return builder.build();
    }

    public String jsonString() {
        return Jsons.toPrettyString(toJson());
    }

    @Override
    public String toString() {
        return "MLMetadataJson{" +
                "mlMetadataJsonFile=" + mlMetadataJsonFile +
                ", version='" + version + '\'' +
                ", modelKind=" + modelKind +
                ", createdBy='" + createdBy + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
