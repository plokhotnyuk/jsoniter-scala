package com.github.plokhotnyuk.jsoniter_scala.benchmark

import io.circe.Decoder._
import io.circe.Encoder._
import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.decoding.UnwrappedDecoder
import io.circe.generic.extras.encoding.UnwrappedEncoder
import io.circe.generic.extras.semiauto._

import scala.util.Try

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
  val prettyPrinter: Printer = Printer.spaces2.copy(dropNullValues = true, reuseWriters = true, predictSize = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val config: Configuration = Configuration.default.withDefaults.withDiscriminator("type")
  implicit val (adtDecoder: Decoder[ADTBase], adtEncoder: Encoder[ADTBase]) =
    (deriveDecoder[ADTBase], deriveEncoder[ADTBase])
  implicit val (anyValsDecoder: Decoder[AnyVals], anyValsEncoder: Encoder[AnyVals]) = {
    implicit def valueClassEncoder[A <: AnyVal : UnwrappedEncoder]: Encoder[A] = implicitly

    implicit def valueClassDecoder[A <: AnyVal : UnwrappedDecoder]: Decoder[A] = implicitly

    (deriveDecoder[AnyVals], deriveEncoder[AnyVals])
  }
  implicit val bigIntEncoder: Encoder[BigInt] = encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val (suitDecoder: Decoder[Suit], suitEncoder: Encoder[Suit]) =
    (decodeString.emap(s => Try(Suit.valueOf(s)).fold[Either[String, Suit]](_ => Left("Suit"), Right.apply)),
      encodeString.contramap[Suit](_.name))
  implicit val (suitADTDecoder, suitADTEncoder) = (deriveEnumerationDecoder[SuitADT], deriveEnumerationEncoder[SuitADT])
  implicit val (suitEnumDecoder, suitEnumEncoder) = (decodeEnumeration(SuitEnum), encodeEnumeration(SuitEnum))
  implicit val (geometryDecoder: Decoder[GeoJSON.Geometry], geometryEncoder: Encoder[GeoJSON.Geometry]) =
    (deriveDecoder[GeoJSON.Geometry], deriveEncoder[GeoJSON.Geometry])
  implicit val (geoJSONDecoder: Decoder[GeoJSON.GeoJSON], geoJSONEncoder: Encoder[GeoJSON.GeoJSON]) =
    (deriveDecoder[GeoJSON.GeoJSON], deriveEncoder[GeoJSON.GeoJSON])
}