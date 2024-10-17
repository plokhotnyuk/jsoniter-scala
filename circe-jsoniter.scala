//> using jmh
//> using jmhVersion 1.37
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-circe::2.31.2"
package bench

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec.*
import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
class CirceJsoniter {
  var jsonNumberBytes: Array[Byte] = Array(0)
  var jsonStringBytes: Array[Byte] = Array(0)

  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10

  @Setup
  def setup(): Unit = {
    jsonNumberBytes = ("1" + "0" * (size - 1)).getBytes
    jsonStringBytes = ("\"1" + "0" * (size - 1) + "\"").getBytes
  }

  @Benchmark
  def bigIntFromJsonNumber() = io.circe.Decoder[BigInt].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def bigIntFromJsonString() = io.circe.Decoder[BigInt].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def bigDecimalFromJsonNumber() = io.circe.Decoder[BigDecimal].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def bigDecimalFromJsonString() = io.circe.Decoder[BigDecimal].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def byteFromJsonNumber() = io.circe.Decoder[Byte].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def byteFromJsonString() = io.circe.Decoder[Byte].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def shortFromJsonNumber() = io.circe.Decoder[Short].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def shortFromJsonString() = io.circe.Decoder[Short].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def intFromJsonNumber() = io.circe.Decoder[Int].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def intFromJsonString() = io.circe.Decoder[Int].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def longFromJsonNumber() = io.circe.Decoder[Long].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def longFromJsonString() = io.circe.Decoder[Long].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def floatFromJsonNumber() = io.circe.Decoder[Float].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def floatFromJsonString() = io.circe.Decoder[Float].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def doubleFromJsonNumber() = io.circe.Decoder[Double].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def doubleFromJsonString() = io.circe.Decoder[Double].decodeJson(readFromArray(jsonStringBytes))
}
