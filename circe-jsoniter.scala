//> using jmh
//> using jmhVersion 1.37
//> using dep "com.epam.deltix:dfp:1.0.3"
//> using dep "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-circe::2.30.15"
package bench

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import io.circe.*
import io.circe.Decoder.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.circe.JsoniterScalaCodec.*
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

/*
  implicit val longDecoder: Decoder[Long] = new Decoder[Long] {
    final def apply(c: HCursor): Result[Long] = {
      val x = c.value
      if (x.isNumber) {
        x.asNumber match {
          case Some(n) => n.toLong match {
            case Some(l) => new Right(l)
            case _ => numberError(c)
          }
          case _ => numberError(c)
        }
      } else if (x.isString) {
        x.asString match {
          case Some(s) => try new Right(java.lang.Long.valueOf(s)) catch {
            case NonFatal(_) => numberError(c)
          }
          case _ => numberError(c)
        }
      } else numberError(c)
    }

    private[this] def numberError(c: HCursor) = new Left(DecodingFailure("expected 64-bit signed whole number", c.history))
  }
*/

  @Setup
  def setup(): Unit = {
    jsonNumberBytes = ("1" + "0" * size).getBytes
    jsonStringBytes = ("\"1" + "0" * size + "\"").getBytes
  }

  @Benchmark
  def circeDecimal64FromJsonNumber() = Decoder[Decimal64].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeDecimal64FromJsonString() = Decoder[Decimal64].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeBigIntFromJsonNumber() = Decoder[BigInt].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeBigIntFromJsonString() = Decoder[BigInt].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeBigDecimalFromJsonNumber() = Decoder[BigDecimal].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeBigDecimalFromJsonString() = Decoder[BigDecimal].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeByteFromJsonNumber() = Decoder[Byte].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeByteFromJsonString() = Decoder[Byte].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeShortFromJsonNumber() = Decoder[Short].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeShortFromJsonString() = Decoder[Short].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeIntFromJsonNumber() = Decoder[Int].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeIntFromJsonString() = Decoder[Int].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeLongFromJsonNumber() = Decoder[Long].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeLongFromJsonString() = Decoder[Long].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeFloatFromJsonNumber() = Decoder[Float].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeFloatFromJsonString() = Decoder[Float].decodeJson(readFromArray(jsonStringBytes))

  @Benchmark
  def circeDoubleFromJsonNumber() = Decoder[Double].decodeJson(readFromArray(jsonNumberBytes))

  @Benchmark
  def circeDoubleFromJsonString() = Decoder[Double].decodeJson(readFromArray(jsonStringBytes))
}
