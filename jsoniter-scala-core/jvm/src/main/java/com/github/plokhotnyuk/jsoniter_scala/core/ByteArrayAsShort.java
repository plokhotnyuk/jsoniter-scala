package com.github.plokhotnyuk.jsoniter_scala.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

class ByteArrayAsShort { // FIXME: Use Java wrapper as w/a for missing support of @PolymorphicSignature methods in Scala 3, see: https://github.com/lampepfl/dotty/issues/11332
    private static final VarHandle VH_SHORT =
            MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);

    static void set(byte[] buf, int pos, short value) {
        VH_SHORT.set(buf, pos, value);
    }
}
