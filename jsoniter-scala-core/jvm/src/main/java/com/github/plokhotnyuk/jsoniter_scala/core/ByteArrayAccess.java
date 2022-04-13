package com.github.plokhotnyuk.jsoniter_scala.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class ByteArrayAccess { // FIXME: Use Java wrapper as w/a for missing support of @PolymorphicSignature methods in Scala 3, see: https://github.com/lampepfl/dotty/issues/11332
    private static final VarHandle VH_LONG =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_INT =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_SHORT =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

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
}
