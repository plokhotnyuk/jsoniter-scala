package com.github.plokhotnyuk.jsoniter_scala.core;

import java.lang.reflect.Field;

// FIXME: remove when perf. degradation of String.charAt when iterating through strings will be fixed in JDK 9+:
// https://bugs.openjdk.java.net/browse/JDK-8013655
public class UnsafeUtils {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long STRING_CODER_OFFSET;
    private static final long STRING_VALUE_OFFSET;

    static {
        sun.misc.Unsafe unsafe = null;
        long stringCoderOffset = 0, stringValueOffset = 0;
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (sun.misc.Unsafe) field.get(null);
            stringValueOffset = unsafe.objectFieldOffset(String.class.getDeclaredField("value"));
            stringCoderOffset = unsafe.objectFieldOffset(String.class.getDeclaredField("coder"));
        } catch (Throwable e) {
            // ignore
        }
        UNSAFE = unsafe;
        STRING_CODER_OFFSET = stringCoderOffset;
        STRING_VALUE_OFFSET = stringValueOffset;
    }

    static byte[] getLatin1Array(String s) {
        if (STRING_CODER_OFFSET == 0 || s == null || UNSAFE.getByte(s, STRING_CODER_OFFSET) != 0) {
            return null;
        }
        return (byte[]) UNSAFE.getObject(s, STRING_VALUE_OFFSET);
    }
}
