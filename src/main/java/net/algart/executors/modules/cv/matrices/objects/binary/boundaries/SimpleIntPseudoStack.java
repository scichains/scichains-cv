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

package net.algart.executors.modules.cv.matrices.objects.binary.boundaries;

import net.algart.arrays.JArrays;

import java.util.Arrays;

// "Pseudo", because it allows to remove elements not only from the top.
class SimpleIntPseudoStack {
    // Note: there is no sense to optimize remove operation,
    // but almost always we remove the LAST element
    private final int[] values;
    private int size = 0;

    public SimpleIntPseudoStack(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative capacity");
        }
        this.values = new int[capacity];
    }

    public int capacity() {
        return values.length;
    }

    public int size() {
        return size;
    }

    public void clear() {
        size = 0;
    }

    public void push(int value) {
        if (size >= values.length) {
            throw new IllegalStateException("Stack overflow");
        }
        values[size++] = value;
    }

    public int remove(int value, int defaultResult) {
        if (size == 0) {
            throw new IllegalStateException("Unbalanced opening/closing brackets");
        }
        --size;
        int k = size - 1;
        int last = values[size];
        if (last == value) {
            return size == 0 ? defaultResult : values[k];
        }
//        System.out.println("Unusual situation: removing " + value + " from " + this + " (+" + values[size] + ")");
        for (; k >= 0; k--) {
            int v = values[k];
            values[k] = last;
            last = v;
            if (v == value) {
                return size == 0 ? defaultResult : values[size];
            }
        }
        throw new IllegalStateException("Invalid pseudo-stack: " + value + " not found here");
    }

    @Override
    public String toString() {
        return "pseudo-stack of " + size + "/" + values.length + " integers: ["
                + JArrays.toString(Arrays.copyOf(values, size), ", ", 1024) + "]";
    }

    public static void main(String[] args) {
        SimpleIntPseudoStack pseudoStack = new SimpleIntPseudoStack(5);
        pseudoStack.push(5);
        System.out.println(pseudoStack);
        pseudoStack.push(10);
        System.out.println(pseudoStack);
        pseudoStack.push(10);
        System.out.println(pseudoStack);
        pseudoStack.push(10);
        System.out.println(pseudoStack);
        int v = pseudoStack.remove(10, -1);
        System.out.println(pseudoStack + ": " + v);
        v = pseudoStack.remove(5, -1);
        System.out.println(pseudoStack + ": " + v+ " (removed 5)");
        v = pseudoStack.remove(10, -1);
        System.out.println(pseudoStack + ": " + v + " (removed 10)");
        v = pseudoStack.remove(10, -1);
        System.out.println(pseudoStack + ": " + v+ " (removed 10)");
        pseudoStack.push(15);
        System.out.println(pseudoStack);
        v = pseudoStack.remove(15, -1);
        System.out.println(pseudoStack + ": " + v + " (removed 15)");
//        pseudoStack.remove(15);
//        System.out.println(pseudoStack);
    }
}
