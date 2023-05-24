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

package net.algart.executors.modules.opencv.matrices;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.*;

public final class GPUDevices {
    private static String info(Device device) {
        return String.format("%s %s by %s%n",
                device.name().getString(),
                device.version().getString(),
                device.vendorName().getString());
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        System.out.printf("OpenCL existence: %s%n", opencv_core.haveOpenCL());
        System.out.printf("OpenCL usage: %s%n", opencv_core.useOpenCL());
        try (Context context = new Context(Device.TYPE_GPU)) {
            System.out.printf("OpenCL default device: %s%n", info(Device.getDefault()));
            System.out.printf("Number of GPU devices: %d%n", context.ndevices());
            for (int k = 0; k < context.ndevices(); k++) {
                System.out.printf("GPU device #%d: %s%n", k, info(context.device(k)));
            }
            new Device(context.device(0)); //??
        }
        System.out.printf("OpenCL default device: %s%n",info(Device.getDefault()));
    }
}
