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
      Decoder[Byte].decodeJson(Json.fromFloatOrNull(1.0f)) shouldBe Right(1.toByte)
      Decoder[Byte].decodeJson(Json.fromDoubleOrNull(1.0)) shouldBe Right(1.toByte)
      Decoder[Byte].decodeJson(Json.fromBigInt(1)) shouldBe Right(1.toByte)
      Decoder[Byte].decodeJson(Json.fromBigDecimal(1.0)) shouldBe Right(1.toByte)
      Decoder[Byte].decodeJson(Json.fromString("001")) shouldBe Right(1.toByte)
      Decoder[Byte].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("128")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromString("-129")) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(0.123)) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547758080"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("9223372036854775808"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("-9223372036854775809"))) shouldBe Left(DecodingFailure("Byte", Nil))
      Decoder[Byte].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0.toByte)
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
      Decoder[Short].decodeJson(Json.fromFloatOrNull(1.0f)) shouldBe Right(1.toShort)
      Decoder[Short].decodeJson(Json.fromDoubleOrNull(1.0)) shouldBe Right(1.toShort)
      Decoder[Short].decodeJson(Json.fromBigInt(1)) shouldBe Right(1.toShort)
      Decoder[Short].decodeJson(Json.fromBigDecimal(1.0)) shouldBe Right(1.toShort)
      Decoder[Short].decodeJson(Json.fromString("001")) shouldBe Right(1.toShort)
      Decoder[Short].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("32768")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromString("-32769")) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(0.123)) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547758080"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("9223372036854775808"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("-9223372036854775809"))) shouldBe Left(DecodingFailure("Short", Nil))
      Decoder[Short].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0.toShort)
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
      Decoder[Int].decodeJson(Json.fromFloatOrNull(1.0f)) shouldBe Right(1)
      Decoder[Int].decodeJson(Json.fromDoubleOrNull(1.0)) shouldBe Right(1)
      Decoder[Int].decodeJson(Json.fromBigInt(1)) shouldBe Right(1)
      Decoder[Int].decodeJson(Json.fromBigDecimal(1.0)) shouldBe Right(1)
      Decoder[Int].decodeJson(Json.fromString("001")) shouldBe Right(1)
      Decoder[Int].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("2147483648")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromString("-2147483649")) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(0.123)) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547758080"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("9223372036854775808"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("-9223372036854775809"))) shouldBe Left(DecodingFailure("Int", Nil))
      Decoder[Int].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0)
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
      Decoder[Long].decodeJson(Json.fromFloatOrNull(1.0f)) shouldBe Right(1L)
      Decoder[Long].decodeJson(Json.fromDoubleOrNull(1.0)) shouldBe Right(1L)
      Decoder[Long].decodeJson(Json.fromBigInt(1)) shouldBe Right(1L)
      Decoder[Long].decodeJson(Json.fromBigDecimal(1.0)) shouldBe Right(1L)
      Decoder[Long].decodeJson(Json.fromString("001")) shouldBe Right(1L)
      Decoder[Long].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(0.123)) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("92233720368547758080"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("9223372036854775808"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("-9223372036854775809"))) shouldBe Left(DecodingFailure("Long", Nil))
      Decoder[Long].decodeJson(Json.fromBigDecimal(BigDecimal("0000000000000000000"))) shouldBe Right(0)
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
      Decoder[Float].decodeJson(Json.fromLong(1L)).getOrElse(0.0f) shouldBe 1.0f
      Decoder[Float].decodeJson(Json.fromDoubleOrNull(1.0)).getOrElse(0.0f) shouldBe 1.0f
      Decoder[Float].decodeJson(Json.fromBigInt(1)).getOrElse(0.0f) shouldBe 1.0f
      Decoder[Float].decodeJson(Json.fromBigDecimal(1.0)).getOrElse(0.0f) shouldBe 1.0f
      Decoder[Float].decodeJson(Json.fromString("001.0")).getOrElse(0.0f) shouldBe 1.0f
      Decoder[Float].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("Float", Nil))
      Decoder[Float].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("Float", Nil))
      Decoder[Float].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Float", Nil))
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
      Decoder[Double].decodeJson(Json.fromLong(1L)).getOrElse(0.0) shouldBe 1.0
      Decoder[Double].decodeJson(Json.fromFloatOrNull(1.0f)).getOrElse(0.0) shouldBe 1.0
      Decoder[Double].decodeJson(Json.fromBigInt(1)).getOrElse(0.0) shouldBe 1.0
      Decoder[Double].decodeJson(Json.fromBigDecimal(1.0)).getOrElse(0.0) shouldBe 1.0
      Decoder[Double].decodeJson(Json.fromString("001.0")).getOrElse(0.0) shouldBe 1.0
      Decoder[Double].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("Double", Nil))
      Decoder[Double].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("Double", Nil))
      Decoder[Double].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("Double", Nil))
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
      Decoder[BigInt].decodeJson(Json.fromLong(1L)).getOrElse(null) shouldBe BigInt(1)
      Decoder[BigInt].decodeJson(Json.fromFloatOrNull(1.0f)).getOrElse(null) shouldBe BigInt(1)
      Decoder[BigInt].decodeJson(Json.fromDoubleOrNull(1.0)).getOrElse(null) shouldBe BigInt(1)
      Decoder[BigInt].decodeJson(Json.fromBigDecimal(BigDecimal("1.0e+5"))).getOrElse(null) shouldBe BigInt(100000)
      Decoder[BigInt].decodeJson(Json.fromString("001")).getOrElse(null) shouldBe BigInt(1)
      Decoder[BigInt].decodeJson(Json.fromString(" 1")) shouldBe Left(DecodingFailure("BigInt", Nil))
      Decoder[BigInt].decodeJson(Json.fromString("1 ")) shouldBe Left(DecodingFailure("BigInt", Nil))
      Decoder[BigInt].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("BigInt", Nil))
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
      Decoder[BigDecimal].decodeJson(Json.fromLong(1L)).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromFloatOrNull(1.0f)).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromDoubleOrNull(1.0)).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromBigInt(BigInt("1"))).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromString("001.0")).getOrElse(null) shouldBe BigDecimal(1.0)
      Decoder[BigDecimal].decodeJson(Json.fromString(" 1.0")) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      Decoder[BigDecimal].decodeJson(Json.fromString("1.0 ")) shouldBe Left(DecodingFailure("BigDecimal", Nil))
      Decoder[BigDecimal].decodeJson(Json.fromBoolean(true)) shouldBe Left(DecodingFailure("BigDecimal", Nil))
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