package com.github.plokhotnyuk.jsoniter_scala.core

import ch.randelshofer.fastdoubleparser.FastDoubleParser
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonParams
import org.openjdk.jmh.annotations.Benchmark

class DoubleReading extends CommonParams {
  private[this] val parseDouble: String => Double = {
    new ThreadLocal[(Array[Byte], JsonReader)] with JsonValueCodec[Double] with (String => Double) {
      def apply(x: String): Double = {
        val (buf, reader) = get
        val len = x.length
        var i = 0
        while (i < len) {
          buf(i) = x.charAt(i).toByte
          i += 1
        }
        reader.read(this, buf, 0, len, ReaderConfig)
      }

      override def decodeValue(in: JsonReader, default: Double): Double = in.readDouble()

      override def encodeValue(x: Double, out: JsonWriter): Unit = out.writeVal(x)

      override def initialValue(): (Array[Byte], JsonReader) = {
        val buf = new Array[Byte](32)
        (buf, new JsonReader(buf, charBuf = new Array[Char](32)))
      }

      override val nullValue: Double = 0.0
    }
  }

  @Benchmark
  def longMantissaBase(): Double = java.lang.Double.parseDouble("123456.7890123456")

  @Benchmark
  def longMantissaJsoniterScala(): Double = parseDouble("123456.7890123456")

  @Benchmark
  def longMantissaFastDoubleParser(): Double = FastDoubleParser.parseDouble("123456.7890123456")

  @Benchmark
  def longWholeNumberBase(): Double = java.lang.Double.parseDouble("1234567890123456.0")

  @Benchmark
  def longWholeNumberJsoniterScala(): Double = parseDouble("1234567890123456.0")

  @Benchmark
  def longWholeNumberFastDoubleParser(): Double = FastDoubleParser.parseDouble("1234567890123456.0")

  @Benchmark
  def shortMantissaBase(): Double = java.lang.Double.parseDouble("1.2")

  @Benchmark
  def shortMantissaJsoniterScala(): Double = parseDouble("1.2")

  @Benchmark
  def shortMantissaFastDoubleParser(): Double = FastDoubleParser.parseDouble("1.2")

  @Benchmark
  def shortWholeNumberBase(): Double = java.lang.Double.parseDouble("12.0")

  @Benchmark
  def shortWholeNumberJsoniterScala(): Double = parseDouble("12.0")

  @Benchmark
  def shortWholeNumberFastDoubleParser(): Double = FastDoubleParser.parseDouble("12.0")
}
