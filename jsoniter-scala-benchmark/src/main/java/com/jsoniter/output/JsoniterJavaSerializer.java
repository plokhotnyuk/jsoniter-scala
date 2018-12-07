package com.jsoniter.output;

import com.jsoniter.extra.PreciseFloatSupport;
import com.jsoniter.spi.JsonException;

import java.io.IOException;

public class JsoniterJavaSerializer {
    static {
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        PreciseFloatSupport.enable();
    }

    private final static ThreadLocal<JsonStream> streams = ThreadLocal.withInitial(() -> new JsonStream(null, 16384));

    public static <T> byte[] serialize(T obj) {
        try {
            JsonStream stream = streams.get();
            stream.reset(null);
            stream.writeVal(obj.getClass(), obj);
            return java.util.Arrays.copyOf(stream.buf, stream.count);
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }
}
