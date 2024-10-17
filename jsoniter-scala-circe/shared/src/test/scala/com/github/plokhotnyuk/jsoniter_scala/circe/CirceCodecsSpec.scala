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
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Decoder[Byte].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Decoder[Byte].decodeJson(Json.fromInt(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Byte], minSuccessful(10000)) { x =>
        Encoder[Byte].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Short" in {
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Decoder[Short].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Decoder[Short].decodeJson(Json.fromInt(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Short], minSuccessful(10000)) { x =>
        Encoder[Short].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Int" in {
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Decoder[Int].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Decoder[Int].decodeJson(Json.fromInt(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Int], minSuccessful(10000)) { x =>
        Encoder[Int].apply(x) shouldBe Json.fromInt(x)
      }
    }
    "decode and encode Long" in {
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Decoder[Long].decodeJson(Json.fromString(x.toString)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Decoder[Long].decodeJson(Json.fromLong(x)).getOrElse(null) shouldBe x
      }
      forAll(arbitrary[Long], minSuccessful(10000)) { x =>
        Encoder[Long].apply(x) shouldBe Json.fromLong(x)
      }
    }
    "decode and encode Float" in {
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
    "decode and encode duration" in {
      verifyDecoding[Duration]("duration", Duration.ofMillis(System.currentTimeMillis()))
      verifyEncoding[Duration](Duration.ofMillis(System.currentTimeMillis()))
    }
    "decode and encode instant" in {
      verifyDecoding[Instant]("instant", Instant.now())
      verifyEncoding[Instant](Instant.now())
    }
    "decode and encode local date" in {
      verifyDecoding[LocalDate]("local date", LocalDate.now())
      verifyEncoding[LocalDate](LocalDate.now())
    }
    "decode and encode local date time" in {
      verifyDecoding[LocalDateTime]("local date time", LocalDateTime.now())
      verifyEncoding[LocalDateTime](LocalDateTime.now())
    }
    "decode and encode local time" in {
      verifyDecoding[LocalTime]("local time", LocalTime.now())
      verifyEncoding[LocalTime](LocalTime.now())
    }
    "decode and encode month day" in {
      verifyDecoding[MonthDay]("month day", MonthDay.now())
      verifyEncoding[MonthDay](MonthDay.now())
    }
    "decode and encode offset date time" in {
      verifyDecoding[OffsetDateTime]("offset date time", OffsetDateTime.now())
      verifyEncoding[OffsetDateTime](OffsetDateTime.now())
    }
    "decode and encode offset time" in {
      verifyDecoding[OffsetTime]("offset time", OffsetTime.now())
      verifyEncoding[OffsetTime](OffsetTime.now())
    }
    "decode and encode period" in {
      verifyDecoding[Period]("period", Period.ofYears(Year.now().getValue))
      verifyEncoding[Period](Period.ofYears(Year.now().getValue))
    }
    "decode and encode year month" in {
      verifyDecoding[YearMonth]("year month", YearMonth.now())
      verifyEncoding[YearMonth](YearMonth.now())
    }
    "decode and encode year" in {
      verifyDecoding[Year]("year", Year.now())
      verifyEncoding[Year](Year.now())
    }
    "decode and encode zoned date time" in {
      verifyDecoding[ZonedDateTime]("zoned date time", ZonedDateTime.now())
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