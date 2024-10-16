//> using jmh
//> using jmhVersion 1.37
//> using dep "com.epam.deltix:dfp:1.0.3"
//> using dep "io.circe::circe-parser:0.14.10"
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import io.circe.*
import io.circe.Decoder.*
import io.circe.parser.*
import com.epam.deltix.dfp.{Decimal64, Decimal64Utils}
import scala.util.control.NonFatal

implicit val decimal64Decoder: Decoder[Decimal64] = new Decoder[Decimal64] {
  final def apply(c: HCursor): Result[Decimal64] = try {
    val x = c.value
    if (x.isNumber) {
      x.asNumber match {
        case Some(n) => n.toBigDecimal match {
          case Some(bd) => canonizedDecimal64Result(Decimal64Utils.fromBigDecimalExact(bd.bigDecimal))
          case _ => numberError(c)
        }
        case _ => numberError(c)
      }
    } else if (x.isString) {
      x.asString match {
        case Some(s) => canonizedDecimal64Result(Decimal64Utils.parse(s))
        case _ => numberError(c)
      }
    } else numberError(c)
  } catch {
    case NonFatal(_) => numberError(c)
  }

  private[this] def canonizedDecimal64Result(x: Long) = new Right(Decimal64.fromUnderlying(Decimal64Utils.canonize(x)))

  private[this] def numberError(c: HCursor) = new Left(DecodingFailure("expected decimal64 number", c.history))
}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
class Benchmarks {
  var jsonNumberBytes: Array[Byte] = Array(0)
  var jsonStringBytes: Array[Byte] = Array(0)

  @Param(Array("10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10

  @Setup
  def setup(): Unit = {
    jsonNumberBytes = ("1" + "0" * size).getBytes
    jsonStringBytes = ("\"1" + "0" * size + "\"").getBytes
  }

  @Benchmark
  def circeDecimal64FromJsonNumber() = decode[Decimal64](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeDecimal64FromJsonString() = decode[Decimal64](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeBigIntFromJsonNumber() = decode[BigInt](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeBigIntFromJsonString() = decode[BigInt](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeBigDecimalFromJsonNumber() = decode[BigDecimal](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeBigDecimalFromJsonString() = decode[BigDecimal](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeByteFromJsonNumber() = decode[Byte](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeByteFromJsonString() = decode[Byte](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeShortFromJsonNumber() = decode[Short](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeShortFromJsonString() = decode[Short](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeIntFromJsonNumber() = decode[Int](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeIntFromJsonString() = decode[Int](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeLongFromJsonNumber() = decode[Long](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeLongFromJsonString() = decode[Long](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeFloatFromJsonNumber() = decode[Float](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeFloatFromJsonString() = decode[Float](new String(jsonStringBytes, "UTF-8"))

  @Benchmark
  def circeDoubleFromJsonNumber() = decode[Double](new String(jsonNumberBytes, "UTF-8"))

  @Benchmark
  def circeDoubleFromJsonString() = decode[Double](new String(jsonStringBytes, "UTF-8"))
}
