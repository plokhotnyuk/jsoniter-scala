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
  implicit val (adtD5r: Decoder[ADTBase], adtE5r: Encoder[ADTBase]) =
    (deriveConfiguredDecoder[ADTBase], deriveConfiguredEncoder[ADTBase])
  implicit val (anyRefsD5r: Decoder[AnyRefs], anyRefsE5r: Encoder[AnyRefs]) =
    (deriveConfiguredDecoder[AnyRefs], deriveConfiguredEncoder[AnyRefs])
  implicit val (anyValsD5r: Decoder[AnyVals], anyValsE5r: Encoder[AnyVals]) = {
    implicit def valueClassEncoder[A <: AnyVal : UnwrappedEncoder]: Encoder[A] = implicitly

    implicit def valueClassDecoder[A <: AnyVal : UnwrappedDecoder]: Decoder[A] = implicitly

    (deriveConfiguredDecoder[AnyVals], deriveConfiguredEncoder[AnyVals])
  }
  implicit val (bidRequestD5r: Decoder[OpenRTB.BidRequest], bidRequestE5r: Encoder[OpenRTB.BidRequest]) = {
    import io.circe.generic.extras.auto._

    (deriveConfiguredDecoder[OpenRTB.BidRequest], deriveConfiguredEncoder[OpenRTB.BidRequest])
  }
  implicit val bigIntE5r: Encoder[BigInt] = encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val (distanceMatrixD5r: Decoder[GoogleMapsAPI.DistanceMatrix], distanceMatrixE5r: Encoder[GoogleMapsAPI.DistanceMatrix]) = {
    import io.circe.generic.auto._

    (deriveConfiguredDecoder[GoogleMapsAPI.DistanceMatrix], deriveConfiguredEncoder[GoogleMapsAPI.DistanceMatrix])
  }
  implicit val (extractFieldsD5r: Decoder[ExtractFields], extractFieldsE5r: Encoder[ExtractFields]) =
    (deriveConfiguredDecoder[ExtractFields], deriveConfiguredEncoder[ExtractFields])
  implicit val (simpleGeometryD5r: Decoder[GeoJSON.SimpleGeometry], simpleGeometryE5r: Encoder[GeoJSON.SimpleGeometry]) =
    (deriveConfiguredDecoder[GeoJSON.SimpleGeometry], deriveConfiguredEncoder[GeoJSON.SimpleGeometry])
  implicit val (geometryD5r: Decoder[GeoJSON.Geometry], geometryE5r: Encoder[GeoJSON.Geometry]) =
    (deriveConfiguredDecoder[GeoJSON.Geometry], deriveConfiguredEncoder[GeoJSON.Geometry])
  implicit val (simpleGeoJSOND5r: Decoder[GeoJSON.SimpleGeoJSON], simpleGeoJSONE5r: Encoder[GeoJSON.SimpleGeoJSON]) =
    (deriveConfiguredDecoder[GeoJSON.SimpleGeoJSON], deriveConfiguredEncoder[GeoJSON.SimpleGeoJSON])
  implicit val (geoJSOND5r: Decoder[GeoJSON.GeoJSON], geoJSONE5r: Encoder[GeoJSON.GeoJSON]) =
    (deriveConfiguredDecoder[GeoJSON.GeoJSON], deriveConfiguredEncoder[GeoJSON.GeoJSON])
  implicit val (missingRequiredFieldsD5r: Decoder[MissingRequiredFields], missingRequiredFieldsE5r: Encoder[MissingRequiredFields]) =
    (deriveConfiguredDecoder[MissingRequiredFields], deriveConfiguredEncoder[MissingRequiredFields])
  implicit val (nestedStructsD5r: Decoder[NestedStructs], nestedStructsE5r: Encoder[NestedStructs]) =
    (deriveConfiguredDecoder[NestedStructs], deriveConfiguredEncoder[NestedStructs])
  implicit val (suitD5r: Decoder[Suit], suitE5r: Encoder[Suit]) =
    (decodeString.emap(s => Try(Suit.valueOf(s)).fold[Either[String, Suit]](_ => Left("Suit"), Right.apply)),
      encodeString.contramap[Suit](_.name))
  implicit val (suitADTDecoder, suitADTEncoder) = (deriveEnumerationDecoder[SuitADT], deriveEnumerationEncoder[SuitADT])
  implicit val (suitEnumDecoder, suitEnumEncoder) = (decodeEnumeration(SuitEnum), encodeEnumeration(SuitEnum))
  implicit val (primitivesD5r: Decoder[Primitives], primitivesE5r: Encoder[Primitives]) =
    (deriveConfiguredDecoder[Primitives], deriveConfiguredEncoder[Primitives])
  implicit val (tweetD5r: Decoder[TwitterAPI.Tweet], tweetE5r: Encoder[TwitterAPI.Tweet]) = {
    import io.circe.generic.auto._

    (deriveConfiguredDecoder[TwitterAPI.Tweet], deriveConfiguredEncoder[TwitterAPI.Tweet])
  }
}