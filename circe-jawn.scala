//> using jmh
//> using jmhVersion 1.37
//> using dep "io.circe::circe-jawn:0.14.10"
package bench

import io.circe.Decoder.*
import io.circe.jawn.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
class CirceJawn {
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
  def bigIntFromJsonNumber() = decodeByteArray[BigInt](jsonNumberBytes)

  @Benchmark
  def bigIntFromJsonString() = decodeByteArray[BigInt](jsonStringBytes)

  @Benchmark
  def bigDecimalFromJsonNumber() = decodeByteArray[BigDecimal](jsonNumberBytes)

  @Benchmark
  def bigDecimalFromJsonString() = decodeByteArray[BigDecimal](jsonStringBytes)

  @Benchmark
  def byteFromJsonNumber() = decodeByteArray[Byte](jsonNumberBytes)

  @Benchmark
  def byteFromJsonString() = decodeByteArray[Byte](jsonStringBytes)

  @Benchmark
  def shortFromJsonNumber() = decodeByteArray[Short](jsonNumberBytes)

  @Benchmark
  def shortFromJsonString() = decodeByteArray[Short](jsonStringBytes)

  @Benchmark
  def intFromJsonNumber() = decodeByteArray[Int](jsonNumberBytes)

  @Benchmark
  def intFromJsonString() = decodeByteArray[Int](jsonStringBytes)

  @Benchmark
  def longFromJsonNumber() = decodeByteArray[Long](jsonNumberBytes)

  @Benchmark
  def longFromJsonString() = decodeByteArray[Long](jsonStringBytes)

  @Benchmark
  def floatFromJsonNumber() = decodeByteArray[Float](jsonNumberBytes)

  @Benchmark
  def floatFromJsonString() = decodeByteArray[Float](jsonStringBytes)

  @Benchmark
  def doubleFromJsonNumber() = decodeByteArray[Double](jsonNumberBytes)

  @Benchmark
  def doubleFromJsonString() = decodeByteArray[Double](jsonStringBytes)
}
