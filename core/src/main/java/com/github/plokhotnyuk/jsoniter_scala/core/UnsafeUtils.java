package com.github.plokhotnyuk.jsoniter_scala.core;

import java.lang.reflect.Field;

// FIXME: remove when perf. degradation of String.charAt when iterating through strings will be fixed in JDK 9+:
// https://bugs.openjdk.java.net/browse/JDK-8013655
public class UnsafeUtils {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long STRING_CODER_OFFSET;
    private static final long STRING_VALUE_OFFSET;

    static {
        sun.misc.Unsafe u = null;
        long sco = 0, svo = 0;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = (sun.misc.Unsafe) f.get(null);
            sco = u.objectFieldOffset(String.class.getDeclaredField("coder"));
            svo = u.objectFieldOffset(String.class.getDeclaredField("value"));
        } catch (Throwable e) {
            // ignore
        }
        UNSAFE = u;
        STRING_CODER_OFFSET = sco;
        STRING_VALUE_OFFSET = svo;
    }

    static byte[] getLatin1Array(String s) {
        if (STRING_CODER_OFFSET == 0 || s == null || UNSAFE.getByte(s, STRING_CODER_OFFSET) != 0) {
            return null;
        }
        return (byte[]) UNSAFE.getObject(s, STRING_VALUE_OFFSET);
    }
}
