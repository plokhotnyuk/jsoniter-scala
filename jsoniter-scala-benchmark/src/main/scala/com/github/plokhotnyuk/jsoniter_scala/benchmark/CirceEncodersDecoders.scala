package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

import scala.util.Try

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val (adtDecoder: Decoder[ADTBase], adtEncoder: Encoder[ADTBase]) =
    (deriveDecoder[ADTBase], deriveEncoder[ADTBase])
  implicit val (anyValsDecoder: Decoder[AnyVals], anyValsEncoder: Encoder[AnyVals]) = {
    implicit val (d1, e1) = (deriveUnwrappedDecoder[ByteVal], deriveUnwrappedEncoder[ByteVal])
    implicit val (d2, e2) = (deriveUnwrappedDecoder[ShortVal], deriveUnwrappedEncoder[ShortVal])
    implicit val (d3, e3) = (deriveUnwrappedDecoder[IntVal], deriveUnwrappedEncoder[IntVal])
    implicit val (d4, e4) = (deriveUnwrappedDecoder[LongVal], deriveUnwrappedEncoder[LongVal])
    implicit val (d5, e5) = (deriveUnwrappedDecoder[BooleanVal], deriveUnwrappedEncoder[BooleanVal])
    implicit val (d6, e6) = (deriveUnwrappedDecoder[CharVal], deriveUnwrappedEncoder[CharVal])
    implicit val (d7, e7) = (deriveUnwrappedDecoder[DoubleVal], deriveUnwrappedEncoder[DoubleVal])
    implicit val (d8, e8) = (deriveUnwrappedDecoder[FloatVal], deriveUnwrappedEncoder[FloatVal])
    (deriveDecoder[AnyVals], deriveEncoder[AnyVals])
  }
  implicit val bigIntEncoder: Encoder[BigInt] = Encoder.encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val suitEncoder: Encoder[Suit] = Encoder.encodeString.contramap(_.name)
  implicit val suitDecoder: Decoder[Suit] = Decoder.decodeString
    .emap(s => Try(Suit.valueOf(s)).fold[Either[String, Suit]](_ => Left("Suit"), Right.apply))
  implicit val (suitADTDecoder, suitADTEncoder) = (deriveEnumerationDecoder[SuitADT], deriveEnumerationEncoder[SuitADT])
  implicit val (suitEnumDecoder, suitEnumEncoder) = (Decoder.enumDecoder(SuitEnum), Encoder.enumEncoder(SuitEnum))
  implicit val (geometryDecoder: Decoder[Geometry], geometryEncoder: Encoder[Geometry]) =
    (deriveDecoder[Geometry], deriveEncoder[Geometry])
  implicit val (geoJSONDecoder: Decoder[GeoJSON], geoJSONEncoder: Encoder[GeoJSON]) =
    (deriveDecoder[GeoJSON], deriveEncoder[GeoJSON])
}