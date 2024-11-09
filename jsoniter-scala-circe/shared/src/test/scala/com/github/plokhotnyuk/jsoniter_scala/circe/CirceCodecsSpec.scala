package com.github.plokhotnyuk.jsoniter_scala.circe

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.time._
import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CirceCodecsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  "CirceCodecsSpec codecs" should {
    "decode and encode Byte" in {
      Decoder[Byte].decodeJson(Json.fromFloatOrNull(123.0f)) shouldBe Right(123.toByte)
      Decoder[Byte].decodeJson(Json.fromDoubleOrNull(123.0)) shouldBe Right(123.toByte)
      Decoder[Byte].decodeJson(Json.fromBigInt(123)) shouldBe Right(123.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(123.0)) shouldBe Right(123.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(127)) shouldBe Right(127.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(-128)) shouldBe Right(-128.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("0.00000000127E+11"))) shouldBe Right(127.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("-0.00000000128E+11"))) shouldBe Right(-128.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("12700000000E-8"))) shouldBe Right(127.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("-12800000000E-8"))) shouldBe Right(-128.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(0.123)) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("214748.36470E+5"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83647E+5"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83648E+5"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83648E+5"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83649E+5"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("00123")) shouldBe Right(123.toByte)
      Decoder[Byte].decodeJson(Json.fromString("127")) shouldBe Right(127.toByte)
      Decoder[Byte].decodeJson(Json.fromString("-128")) shouldBe Right(-128.toByte)
      Decoder[Byte].decodeJson(Json.fromString("128")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("-129")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Byte", Nil))
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Decoder[Byte].decodeJson(Json.fromString(x.toString)) shouldBe Right(x)
      }
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Decoder[Byte].decodeJson(Json.fromInt(x)) shouldBe Right(x)
      }
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Encoder[Byte].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Short" in {
      Decoder[Short].decodeJson(Json.fromFloatOrNull(12345.0f)) shouldBe Right(12345.toShort)
      Decoder[Short].decodeJson(Json.fromDoubleOrNull(12345.0)) shouldBe Right(12345.toShort)
      Decoder[Short].decodeJson(Json.fromBigInt(12345)) shouldBe Right(12345.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(12345.0)) shouldBe Right(12345.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(32767)) shouldBe Right(32767.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(-32768)) shouldBe Right(-32768.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("0.32767E+5"))) shouldBe Right(32767)
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("-0.32768E+5"))) shouldBe Right(-32768)
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("3276700000E-5"))) shouldBe Right(32767)
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("-3276800000E-5"))) shouldBe Right(-32768)
      Decoder[Short].decodeJson(Json.fromBigDecimal(0.12345)) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("214748.36470E+5"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83647E+5"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83648E+5"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83648E+5"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83649E+5"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("0012345")) shouldBe Right(12345.toShort)
      Decoder[Short].decodeJson(Json.fromString("32767")) shouldBe Right(32767.toShort)
      Decoder[Short].decodeJson(Json.fromString("-32768")) shouldBe Right(-32768.toShort)
      Decoder[Short].decodeJson(Json.fromString("32768")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("-32769")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Short", Nil))
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Decoder[Short].decodeJson(Json.fromString(x.toString)) shouldBe Right(x)
      }
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Decoder[Short].decodeJson(Json.fromInt(x)) shouldBe Right(x)
      }
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Encoder[Short].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Int" in {
      Decoder[Int].decodeJson(Json.fromBigDecimal(1234567890)) shouldBe Right(1234567890)
      Decoder[Int].decodeJson(Json.fromFloatOrNull(12345678.0f)) shouldBe Right(12345678)
      Decoder[Int].decodeJson(Json.fromDoubleOrNull(1234567890.0)) shouldBe Right(1234567890)
      Decoder[Int].decodeJson(Json.fromBigInt(1234567890)) shouldBe Right(1234567890)
      Decoder[Int].decodeJson(Json.fromBigDecimal(2147483647)) shouldBe Right(2147483647)
      Decoder[Int].decodeJson(Json.fromBigDecimal(-2147483648)) shouldBe Right(-2147483648)
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83647E+5"))) shouldBe Right(2147483647)
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83648E+5"))) shouldBe Right(-2147483648)
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("214748364700000E-5"))) shouldBe Right(2147483647)
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("-214748364800000E-5"))) shouldBe Right(-2147483648)
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0)
      Decoder[Int].decodeJson(Json.fromBigDecimal(0.123456789)) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("0.1234567890"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("214748.36470E+5"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("21474.83648E+5"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("-21474.83649E+5"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("001234567890")) shouldBe Right(1234567890)
      Decoder[Int].decodeJson(Json.fromString("2147483647")) shouldBe Right(2147483647)
      Decoder[Int].decodeJson(Json.fromString("-2147483648")) shouldBe Right(-2147483648)
      Decoder[Int].decodeJson(Json.fromString("2147483648")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("-2147483649")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Int", Nil))
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Decoder[Int].decodeJson(Json.fromString(x.toString)) shouldBe Right(x)
      }
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Decoder[Int].decodeJson(Json.fromInt(x)) shouldBe Right(x)
      }
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Encoder[Int].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Long" in {
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("1234567890123456789"))) shouldBe Right(1234567890123456789L)
      Decoder[Long].decodeJson(Json.fromFloatOrNull(12345678.0f)) shouldBe Right(12345678L)
      Decoder[Long].decodeJson(Json.fromDoubleOrNull(1234567890123456.0)) shouldBe Right(1234567890123456L)
      Decoder[Long].decodeJson(Json.fromBigInt(1234567890123456789L)) shouldBe Right(1234567890123456789L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547.75807E+5"))) shouldBe Right(9223372036854775807L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("-92233720368547.75808E+5"))) shouldBe Right(-9223372036854775808L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("922337203685477580700000E-5"))) shouldBe Right(9223372036854775807L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("-922337203685477580800000E-5"))) shouldBe Right(-9223372036854775808L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0)
      Decoder[Long].decodeJson(Json.fromBigDecimal(0.1234567890123456)) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("922337203685477.58080E+5"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547.75808E+5"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("-92233720368547.75809E+5"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromString("001234567890123456789")) shouldBe Right(1234567890123456789L)
      Decoder[Long].decodeJson(Json.fromString("9223372036854775807")) shouldBe Right(9223372036854775807L)
      Decoder[Long].decodeJson(Json.fromString("-9223372036854775808")) shouldBe Right(-9223372036854775808L)
      Decoder[Long].decodeJson(Json.fromString("9223372036854775808")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromString("-9223372036854775809")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Long", Nil))
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Decoder[Long].decodeJson(Json.fromString(x.toString)) shouldBe Right(x)
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Decoder[Long].decodeJson(Json.fromLong(x)) shouldBe Right(x)
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Encoder[Long].apply(x) shouldBe Json.fromLong(x)
      }
    }
    "decode and encode Float" in {
      Decoder[Float].decodeJson(Json.fromLong(12345678L)) shouldBe Right(12345678.0f)
      Decoder[Float].decodeJson(Json.fromDoubleOrNull(12345678.0)) shouldBe Right(12345678.0f)
      Decoder[Float].decodeJson(Json.fromBigInt(12345678)) shouldBe Right(12345678.0f)
      Decoder[Float].decodeJson(Json.fromBigDecimal(BigDecimal("12345678.0"))) shouldBe Right(12345678.0f)
      Decoder[Float].decodeJson(Json.fromString("0012345678.0")) shouldBe Right(12345678.0f)
      Decoder[Float].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("Float", Nil))
      Decoder[Float].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("Float", Nil))
      Decoder[Float].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Float", Nil))
      Decoder[Float].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Float", Nil))
      forAll(arbitrary[Float], minSuccessful(10000)) { x =>
        Decoder[Float].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Float], minSuccessful(10000)) { x =>
        Json.fromFloat(x).foreach(Decoder[Float].decodeJson(_).getOrElse(null) shouldBe x)
      }
      forAll(arbitrary[Float], minSuccessful(10000)) { x =>
        Encoder[Float].apply(x) shouldBe Json.fromFloat(x).getOrElse(Json.Null)
      }
    }
    "decode and encode Double" in {
      Decoder[Double].decodeJson(Json.fromLong(1234567890123456L)) shouldBe Right(1234567890123456.0)
      Decoder[Double].decodeJson(Json.fromFloatOrNull(12345678.0f)) shouldBe Right(12345678.0)
      Decoder[Double].decodeJson(Json.fromBigInt(1234567890123456L)) shouldBe Right(1234567890123456.0)
      Decoder[Double].decodeJson(Json.fromBigDecimal(BigDecimal("1234567890123456"))) shouldBe Right(1234567890123456.0)
      Decoder[Double].decodeJson(Json.fromString("001234567890123456.0")) shouldBe Right(1234567890123456.0)
      Decoder[Double].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("Double", Nil))
      Decoder[Double].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("Double", Nil))
      Decoder[Double].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Double", Nil))
      Decoder[Double].decodeJson(Json.Null) shouldBe Left(DecodingFailure("Double", Nil))
      forAll(arbitrary[Double], minSuccessful(10000)) { x =>
        Decoder[Double].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Double], minSuccessful(10000)) { x =>
        Json.fromDouble(x).foreach(Decoder[Double].decodeJson(_).getOrElse(null) shouldBe x)
      }
      forAll(arbitrary[Double], minSuccessful(10000)) { x =>
        Encoder[Double].apply(x) shouldBe Json.fromDouble(x).getOrElse(Json.Null)
      }
    }
    "decode and encode BigInt" in {
      Decoder[BigInt].decodeJson(Json.fromLong(1234567890123456789L)).getOrElse(null) shouldBe BigInt(1234567890123456789L)
      Decoder[BigInt].decodeJson(Json.fromFloatOrNull(12345678.0f)).getOrElse(null) shouldBe BigInt(12345678)
      Decoder[BigInt].decodeJson(Json.fromDoubleOrNull(1234567890123456.0)).getOrElse(null) shouldBe BigInt(1234567890123456L)
      Decoder[BigInt].decodeJson(Json.fromBigDecimal(BigDecimal("12345678901234567890"))).getOrElse(null) shouldBe BigInt("12345678901234567890")
      Decoder[BigInt].decodeJson(Json.fromString("001")).getOrElse(null) shouldBe BigInt(1)
      Decoder[BigInt].decodeJson(Json.fromString("12345678901234567890")).getOrElse(null) shouldBe BigInt("12345678901234567890")
      Decoder[BigInt].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("BigInt", Nil))
      Decoder[BigInt].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("BigInt", Nil))
      Decoder[BigInt].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("BigInt", Nil))
      Decoder[BigInt].decodeJson(Json.Null) shouldBe Left(DecodingFailure("BigInt", Nil))
      forAll(arbitrary[BigInt], minSuccessful(10000)) { x =>
        Decoder[BigInt].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[BigInt], minSuccessful(10000)) { x =>
        Decoder[BigInt].decodeJson(Json.fromBigInt(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[BigInt], minSuccessful(10000)) { x =>
        Encoder[BigInt].apply(x) shouldBe Json.fromBigInt(x)
      }
    }
    "decode and encode BigDecimal" in {
      Decoder[BigDecimal].decodeJson(Json.fromLong(1234567890123456789L)).getOrElse(null) shouldBe BigDecimal(1234567890123456789L)
      Decoder[BigDecimal].decodeJson(Json.fromFloatOrNull(12345678.0f)).getOrElse(null) shouldBe BigDecimal(12345678)
      Decoder[BigDecimal].decodeJson(Json.fromDoubleOrNull(1234567890123456.0)).getOrElse(null) shouldBe BigDecimal(1234567890123456L)
      Decoder[BigDecimal].decodeJson(Json.fromBigInt(BigInt("12345678901234567890"))).getOrElse(null) shouldBe BigDecimal("12345678901234567890")
      Decoder[BigDecimal].decodeJson(Json.fromString("001.0")).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromString("12345678901234567890")).getOrElse(null) shouldBe BigDecimal("12345678901234567890")
      Decoder[BigDecimal].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      Decoder[BigDecimal].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      Decoder[BigDecimal].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      Decoder[BigDecimal].decodeJson(Json.Null) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      forAll(arbitrary[BigDecimal], minSuccessful(10000)) { x =>
        val y = x.apply(JsonReader.bigDecimalMathContext)
        Decoder[BigDecimal].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe y
      }
      forAll(arbitrary[BigDecimal], minSuccessful(10000)) { x =>
        Decoder[BigDecimal].decodeJson(Json.fromBigDecimal(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[BigDecimal], minSuccessful(10000)) { x =>
        Encoder[BigDecimal].apply(x) shouldBe Json.fromBigDecimal(x)
      }
    }
    "decode and encode Duration" in {
      verifyDecoding[Duration]("Duration", Duration.ofMillis(System.currentTimeMillis()))
      verifyEncoding[Duration](Duration.ofMillis(System.currentTimeMillis()))
    }
    "decode and encode Instant" in {
      verifyDecoding[Instant]("Instant", Instant.now())
      verifyEncoding[Instant](Instant.now())
    }
    "decode and encode LocalDate" in {
      verifyDecoding[LocalDate]("LocalDate", LocalDate.now())
      verifyEncoding[LocalDate](LocalDate.now())
    }
    "decode and encode LocalDateTime" in {
      verifyDecoding[LocalDateTime]("LocalDateTime", LocalDateTime.now())
      verifyEncoding[LocalDateTime](LocalDateTime.now())
    }
    "decode and encode LocalTime" in {
      verifyDecoding[LocalTime]("LocalTime", LocalTime.now())
      verifyEncoding[LocalTime](LocalTime.now())
    }
    "decode and encode MonthDay" in {
      verifyDecoding[MonthDay]("MonthDay", MonthDay.now())
      verifyEncoding[MonthDay](MonthDay.now())
    }
    "decode and encode OffsetDateTime" in {
      verifyDecoding[OffsetDateTime]("OffsetDateTime", OffsetDateTime.now())
      verifyEncoding[OffsetDateTime](OffsetDateTime.now())
    }
    "decode and encode OffsetTime" in {
      verifyDecoding[OffsetTime]("OffsetTime", OffsetTime.now())
      verifyEncoding[OffsetTime](OffsetTime.now())
    }
    "decode and encode Period" in {
      verifyDecoding[Period]("Period", Period.ofYears(Year.now().getValue))
      verifyEncoding[Period](Period.ofYears(Year.now().getValue))
    }
    "decode and encode YearMonth" in {
      verifyDecoding[YearMonth]("YearMonth", YearMonth.now())
      verifyEncoding[YearMonth](YearMonth.now())
    }
    "decode and encode Year" in {
      verifyDecoding[Year]("Year", Year.now())
      verifyEncoding[Year](Year.now())
    }
    "decode and encode ZonedDateTime" in {
      verifyDecoding[ZonedDateTime]("ZonedDateTime", ZonedDateTime.now())
      verifyEncoding[ZonedDateTime](ZonedDateTime.now())
    }
  }

  def verifyDecoding[A: Decoder](name: String, x: A): Unit = {
    Decoder[A].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
    Decoder[A].decodeJson(Json.Null).left.getOrElse(null) shouldBe DecodingFailure(name, Nil)
    Decoder[A].decodeJson(Json.fromString("X")).left.getOrElse(null) shouldBe DecodingFailure(name, Nil)
    Decoder[A].decodeJson(Json.fromString("X" * 200)).left.getOrElse(null) shouldBe DecodingFailure(name, Nil)
    Decoder[A].decodeJson(Json.fromString("Ї")).left.getOrElse(null) shouldBe DecodingFailure(name, Nil)
  }

  def verifyEncoding[A: Encoder](x: A): Unit = Encoder[A].apply(x) shouldBe Json.fromString(x.toString)
}