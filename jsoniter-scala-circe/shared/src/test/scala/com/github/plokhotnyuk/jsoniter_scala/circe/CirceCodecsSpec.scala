package com.github.plokhotnyuk.jsoniter_scala.circe

import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.time._
import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._

class CirceCodecsSpec extends AnyWordSpec with Matchers {
  "CirceCodecsSpec codecs" should {
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
