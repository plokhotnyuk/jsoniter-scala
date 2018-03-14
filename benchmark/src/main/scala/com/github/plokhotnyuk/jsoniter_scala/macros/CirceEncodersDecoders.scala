package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time.ZoneOffset

import cats.syntax.either._
import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true)
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val aEncoder: Encoder[A] = deriveEncoder[A]
  implicit val aDecoder: Decoder[A] = deriveDecoder[A]
  implicit val bEncoder: Encoder[B] = deriveEncoder[B]
  implicit val bDecoder: Decoder[B] = deriveDecoder[B]
  implicit val cEncoder: Encoder[C] = deriveEncoder[C]
  implicit val cDecoder: Decoder[C] = deriveDecoder[C]
  implicit val adtEncoder: Encoder[AdtBase] = deriveEncoder[AdtBase]
  implicit val adtDecoder: Decoder[AdtBase] = deriveDecoder[AdtBase]
  implicit val enumEncoder: Encoder[SuitEnum.Value] = Encoder.enumEncoder(SuitEnum)
  implicit val enumDecoder: Decoder[SuitEnum.Value] = Decoder.enumDecoder(SuitEnum)
  implicit val suitEncoder: Encoder[Suit] = Encoder.encodeString.contramap[Suit](_.name)
  implicit val suitDecoder: Decoder[Suit] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(Suit.valueOf(str)).leftMap(t => "Suit")
  }
  implicit val zoneOffsetEncoder: Encoder[ZoneOffset] = Encoder.encodeString.contramap[ZoneOffset](_.toString)
  implicit val zoneOffsetDecoder: Decoder[ZoneOffset] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(ZoneOffset.of(str)).leftMap(t => "ZoneOffset")
  }
}