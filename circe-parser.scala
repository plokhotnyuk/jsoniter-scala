//> using jmh
//> using jmhVersion 1.37
//> using dep "io.circe::circe-parser:0.14.10"
package bench

import io.circe.Decoder.*
import io.circe.parser.*
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
class CirceParser {
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
  def bigIntFromJsonNumber() = decode[BigInt](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def bigIntFromJsonString() = decode[BigInt](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def bigDecimalFromJsonNumber() = decode[BigDecimal](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def bigDecimalFromJsonString() = decode[BigDecimal](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def byteFromJsonNumber() = decode[Byte](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def byteFromJsonString() = decode[Byte](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def shortFromJsonNumber() = decode[Short](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def shortFromJsonString() = decode[Short](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def intFromJsonNumber() = decode[Int](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def intFromJsonString() = decode[Int](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def longFromJsonNumber() = decode[Long](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def longFromJsonString() = decode[Long](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def floatFromJsonNumber() = decode[Float](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def floatFromJsonString() = decode[Float](new String(jsonStringBytes, UTF_8))

  @Benchmark
  def doubleFromJsonNumber() = decode[Double](new String(jsonNumberBytes, UTF_8))

  @Benchmark
  def doubleFromJsonString() = decode[Double](new String(jsonStringBytes, UTF_8))
}
