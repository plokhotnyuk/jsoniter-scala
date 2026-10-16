/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.plokhotnyuk.jsoniter_scala.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class ByteArrayAccess {
    private static final VarHandle VH_LONG =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_INT =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_SHORT =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_LONG_REVERSED =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle VH_INT_REVERSED =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    // Bound to Math.unsignedMultiplyHigh on JDK 18+ to get intrinsified 128-bit multiplications by JIT compilers,
    // otherwise to emulations with Math.multiplyHigh that are valid only for restricted ranges of arguments
    private static final MethodHandle MH_UNSIGNED_MULTIPLY_HIGH_1 = unsignedMultiplyHighHandle("unsignedMultiplyHighFallback1");
    private static final MethodHandle MH_UNSIGNED_MULTIPLY_HIGH_2 = unsignedMultiplyHighHandle("unsignedMultiplyHighFallback2");
    private static final MethodHandle MH_UNSIGNED_MULTIPLY_HIGH_3 = unsignedMultiplyHighHandle("unsignedMultiplyHighFallback3");

    static void setLong(byte[] buf, int pos, long value) {
        VH_LONG.set(buf, pos, value);
    }

    static long getLong(byte[] buf, int pos) {
        return (long) VH_LONG.get(buf, pos);
    }

    static void setInt(byte[] buf, int pos, int value) {
        VH_INT.set(buf, pos, value);
    }

    static int getInt(byte[] buf, int pos) {
        return (int) VH_INT.get(buf, pos);
    }

    static void setShort(byte[] buf, int pos, short value) {
        VH_SHORT.set(buf, pos, value);
    }

    static void setLongReversed(byte[] buf, int pos, long value) {
        VH_LONG_REVERSED.set(buf, pos, value);
    }

    static int getIntReversed(byte[] buf, int pos) {
        return (int) VH_INT_REVERSED.get(buf, pos);
    }

    static void setIntReversed(byte[] buf, int pos, int value) {
        VH_INT_REVERSED.set(buf, pos, value);
    }

    // Use only when x is negative and y is positive
    static long unsignedMultiplyHigh1(long x, long y) {
        try {
            return (long) MH_UNSIGNED_MULTIPLY_HIGH_1.invokeExact(x, y);
        } catch (Throwable e) {
            throw new IllegalStateException(e); // unreachable: the target method doesn't throw
        }
    }

    // Use only when y is positive
    static long unsignedMultiplyHigh2(long x, long y) {
        try {
            return (long) MH_UNSIGNED_MULTIPLY_HIGH_2.invokeExact(x, y);
        } catch (Throwable e) {
            throw new IllegalStateException(e); // unreachable: the target method doesn't throw
        }
    }

    // Use only when both x and y are negative
    static long unsignedMultiplyHigh3(long x, long y) {
        try {
            return (long) MH_UNSIGNED_MULTIPLY_HIGH_3.invokeExact(x, y);
        } catch (Throwable e) {
            throw new IllegalStateException(e); // unreachable: the target method doesn't throw
        }
    }

    private static long unsignedMultiplyHighFallback1(long x, long y) {
        return Math.multiplyHigh(x, y) + y;
    }

    private static long unsignedMultiplyHighFallback2(long x, long y) {
        return Math.multiplyHigh(x, y) + ((x >> 63) & y);
    }

    private static long unsignedMultiplyHighFallback3(long x, long y) {
        return Math.multiplyHigh(x, y) + x + y;
    }

    private static MethodHandle unsignedMultiplyHighHandle(String fallbackName) {
        MethodType mt = MethodType.methodType(long.class, long.class, long.class);
        try {
            return MethodHandles.publicLookup().findStatic(Math.class, "unsignedMultiplyHigh", mt); // JDK 18+
        } catch (ReflectiveOperationException e) {
            try {
                return MethodHandles.lookup().findStatic(ByteArrayAccess.class, fallbackName, mt);
            } catch (ReflectiveOperationException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }
}
