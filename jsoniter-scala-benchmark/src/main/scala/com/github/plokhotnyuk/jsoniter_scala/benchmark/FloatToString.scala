package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class FloatToString extends CommonParams {
    private[this] val stringify: Float => String = {
        import com.github.plokhotnyuk.jsoniter_scala.core._

        new ThreadLocal[Array[Byte]] with JsonValueCodec[Float] with (Float => String) {
            def apply(x: Float): String =
                if (java.lang.Float.isFinite(x)) {
                    val buf = get
                    val len = writeToSubArray(x, buf, 0, 32)(this)
                    new String(buf, 0, 0, len)
                } else java.lang.Float.toString(x)

            override def decodeValue(in: JsonReader, default: Float): Float = in.readFloat()

            override def encodeValue(x: Float, out: JsonWriter): Unit = out.writeVal(x)

            override def initialValue(): Array[Byte] = new Array[Byte](32)

            override val nullValue: Float = 0.0f
        }
    }

    @Benchmark
    def longMantissaBase(): String = java.lang.Float.toString(123.45678f)

    @Benchmark
    def longMantissaRyu(): String = stringify(123.45678f)

    @Benchmark
    def longWholeNumberBase(): String = java.lang.Float.toString(12345678.0f)

    @Benchmark
    def longWholeNumberRyu(): String = stringify(12345678.0f)

    @Benchmark
    def shortMantissaBase(): String = java.lang.Float.toString(1.2f)

    @Benchmark
    def shortMantissaRyu(): String = stringify(1.2f)

    @Benchmark
    def shortWholeNumberBase(): String = java.lang.Float.toString(12.0f)

    @Benchmark
    def shortWholeNumberRyu(): String = stringify(12.0f)
}
