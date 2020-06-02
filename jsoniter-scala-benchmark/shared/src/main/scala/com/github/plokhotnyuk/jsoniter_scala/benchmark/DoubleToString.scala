package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class DoubleToString extends CommonParams {
  private[this] val stringify: Double => String = {
    import com.github.plokhotnyuk.jsoniter_scala.core._

    new ThreadLocal[Array[Byte]] with JsonValueCodec[Double] with (Double => String) {
      def apply(x: Double): String = if (java.lang.Double.isFinite(x)) {
        val buf = get
        val len = writeToSubArray(x, buf, 0, 32)(this)
        new String(buf, 0, 0, len)
      } else java.lang.Double.toString(x)

      override def decodeValue(in: JsonReader, default: Double): Double = in.readDouble()

      override def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)

      override def initialValue(): Array[Byte] = new Array[Byte](32)

      override val nullValue: Double = 0.0
    }
  }

  @Benchmark
  def longMantissaBase(): String = java.lang.Double.toString(123456.7890123456)

  @Benchmark
  def longMantissaJsoniterScala(): String = stringify(123456.7890123456)

  @Benchmark
  def longWholeNumberBase(): String = java.lang.Double.toString(1234567890123456.0)

  @Benchmark
  def longWholeNumberJsoniterScala(): String = stringify(1234567890123456.0)

  @Benchmark
  def shortMantissaBase(): String = java.lang.Double.toString(1.2)

  @Benchmark
  def shortMantissaJsoniterScala(): String = stringify(1.2)

  @Benchmark
  def shortWholeNumberBase(): String = java.lang.Double.toString(12.0)

  @Benchmark
  def shortWholeNumberJsoniterScala(): String = stringify(12.0)
}
