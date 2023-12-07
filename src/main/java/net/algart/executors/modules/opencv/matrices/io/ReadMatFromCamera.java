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

package net.algart.executors.modules.opencv.matrices.io;

import net.algart.executors.modules.opencv.util.O2SMat;
import net.algart.executors.api.Executor;
import net.algart.executors.api.ReadOnlyExecutionInput;
import net.algart.executors.api.data.SMat;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.io.IOError;
import java.io.IOException;

public final class ReadMatFromCamera extends Executor implements ReadOnlyExecutionInput {
    public enum ApiPreference {
        AUTO(opencv_videoio.CAP_ANY),
        VFW(opencv_videoio.CAP_VFW);

        private final int code;

        public int code() {
            return code;
        }

        ApiPreference(int code) {
            this.code = code;
        }
    }

    private int cameraIndex = 0;
    private ApiPreference apiPreference = ApiPreference.AUTO;
    private boolean lockCameraForFurtherUsage = false;

    private volatile Camera savedCamera = null;

    public ReadMatFromCamera() {
        addInputMat(DEFAULT_INPUT_PORT);
        addOutputMat(DEFAULT_OUTPUT_PORT);
    }

    public int getCameraIndex() {
        return cameraIndex;
    }

    public ReadMatFromCamera setCameraIndex(int cameraIndex) {
        this.cameraIndex = nonNegative(cameraIndex);
        return this;
    }

    public ApiPreference getApiPreference() {
        return apiPreference;
    }

    public ReadMatFromCamera setApiPreference(ApiPreference apiPreference) {
        this.apiPreference = nonNull(apiPreference);
        return this;
    }

    public boolean isLockCameraForFurtherUsage() {
        return lockCameraForFurtherUsage;
    }

    public ReadMatFromCamera setLockCameraForFurtherUsage(boolean lockCameraForFurtherUsage) {
        this.lockCameraForFurtherUsage = lockCameraForFurtherUsage;
        return this;
    }

    @Override
    public void process() {
        SMat input = getInputMat(defaultInputPortName(), true);
        if (input.isInitialized()) {
            logDebug(() -> "Copying " + input);
            getMat().setTo(input);
            return;
        }
        final boolean lockCamera = this.lockCameraForFurtherUsage;
        final boolean settingsChanged = savedCamera != null
                && (cameraIndex != savedCamera.cameraIndex || apiPreference != savedCamera.apiPreference);
        if (!lockCamera || settingsChanged) {
            if (settingsChanged) {
                logInfo(() -> "Camera settings changed, need to reopen");
            }
            closeCamera();
        }
        boolean useSavedCamera = this.savedCamera != null;
        final Camera camera = useSavedCamera ? this.savedCamera : new Camera(cameraIndex, apiPreference);
        try {
            final Mat mat = camera.readFrame(useSavedCamera ? "saved camera" : "newly created camera");
            O2SMat.setTo(getMat(), mat);
            if (lockCamera) {
                this.savedCamera = camera;
            }
        } finally {
            if (!lockCamera) {
                // This thread has set savedCamera to null
                camera.release();
            }
        }
    }

    @Override
    public void close() {
        closeCamera();
        super.close();
    }

    private void closeCamera() {
        if (savedCamera != null) {
            logInfo(() -> "Closing " + savedCamera);
            savedCamera.release();
            savedCamera = null;
        }
    }

    public static class Camera {
        private final VideoCapture video;
        private final int cameraIndex;
        private final ApiPreference apiPreference;

        public Camera(int cameraIndex, ApiPreference apiPreference) {
            this.cameraIndex = cameraIndex;
            this.apiPreference = apiPreference;
            logInfo(() -> "Opening video camera #" + cameraIndex + " (preference: " + apiPreference + ")");
            this.video = new VideoCapture();
            if (!video.open(cameraIndex, apiPreference.code())) {
                throw new IOError(new IOException("Cannot open camera"));
            }
        }

        public void release() {
            video.release();
            video.close();
        }

        public Mat readFrame() {
            return readFrame(null);
        }

        // cameraTitle is useful for debugging
        private Mat readFrame(String cameraTitle) {
            if (!video.isOpened()) {
                throw new IOError(new IOException(
                        (cameraTitle == null ? "Camera" : cameraTitle) + " is not opened (" + this + ")"));
            }
            final Mat result = new Mat();
            if (!video.read(result)) {
                throw new IOError(new IOException("No frames were grabbed by "
                        + (cameraTitle == null ? "camera" : cameraTitle) + " (" + this + ")"));
            }
            return result;
        }

        @Override
        public String toString() {
            return "video camera #" + cameraIndex + ", preference: " + apiPreference;
        }
    }
}
