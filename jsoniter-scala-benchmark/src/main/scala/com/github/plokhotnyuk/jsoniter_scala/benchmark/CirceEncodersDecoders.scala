package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import io.circe._
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto._

import scala.util.Try

object CirceEncodersDecoders {
  val printer: Printer = Printer.noSpaces.copy(dropNullValues = true, reuseWriters = true)
  val escapingPrinter: Printer = printer.copy(escapeNonAscii = true)
  implicit val config: Configuration = Configuration.default.withDiscriminator("type")
  implicit val adtEncoder: Encoder[ADTBase] = {
    implicit val aEncoder: Encoder[X] = deriveEncoder
    implicit val bEncoder: Encoder[Y] = deriveEncoder
    implicit val cEncoder: Encoder[Z] = deriveEncoder
    deriveEncoder
  }
  implicit val adtDecoder: Decoder[ADTBase] = {
    implicit val aDecoder: Decoder[X] = deriveDecoder
    implicit val bDecoder: Decoder[Y] = deriveDecoder
    implicit val cDecoder: Decoder[Z] = deriveDecoder
    deriveDecoder
  }
  implicit val anyValsEncoder: Encoder[AnyVals] = {
    implicit val byteValEncoder: Encoder[ByteVal] = deriveUnwrappedEncoder
    implicit val shortValEncoder: Encoder[ShortVal] = deriveUnwrappedEncoder
    implicit val intValEncoder: Encoder[IntVal] = deriveUnwrappedEncoder
    implicit val longValEncoder: Encoder[LongVal] = deriveUnwrappedEncoder
    implicit val booleanValEncoder: Encoder[BooleanVal] = deriveUnwrappedEncoder
    implicit val charValEncoder: Encoder[CharVal] = deriveUnwrappedEncoder
    implicit val doubleValEncoder: Encoder[DoubleVal] = deriveUnwrappedEncoder
    implicit val floatValEncoder: Encoder[FloatVal] = deriveUnwrappedEncoder
    deriveEncoder
  }
  implicit val anyValsDecoder: Decoder[AnyVals] = {
    implicit val byteValDecoder: Decoder[ByteVal] = deriveUnwrappedDecoder
    implicit val shortValDecoder: Decoder[ShortVal] = deriveUnwrappedDecoder
    implicit val intValDecoder: Decoder[IntVal] = deriveUnwrappedDecoder
    implicit val longValDecoder: Decoder[LongVal] = deriveUnwrappedDecoder
    implicit val booleanValDecoder: Decoder[BooleanVal] = deriveUnwrappedDecoder
    implicit val charValDecoder: Decoder[CharVal] = deriveUnwrappedDecoder
    implicit val doubleValDecoder: Decoder[DoubleVal] = deriveUnwrappedDecoder
    implicit val floatValDecoder: Decoder[FloatVal] = deriveUnwrappedDecoder
    deriveDecoder
  }
  implicit val bigIntEncoder: Encoder[BigInt] = Encoder.encodeJsonNumber
    .contramap(x => JsonNumber.fromDecimalStringUnsafe(new java.math.BigDecimal(x.bigInteger).toPlainString))
  implicit val suitEncoder: Encoder[Suit] = Encoder.encodeString.contramap(_.name)
  implicit val suitDecoder: Decoder[Suit] = Decoder.decodeString
    .emap(s => Try(Suit.valueOf(s)).fold[Either[String, Suit]](_ => Left("Suit"), Right.apply))
  implicit val suitADTEncoder: Encoder[SuitADT] = deriveEnumerationEncoder
  implicit val suitADTDecoder: Decoder[SuitADT] = deriveEnumerationDecoder
  implicit val suitEnumEncoder: Encoder[SuitEnum] = Encoder.enumEncoder(SuitEnum)
  implicit val suitEnumDecoder: Decoder[SuitEnum] = Decoder.enumDecoder(SuitEnum)
  implicit val geometryEncoder: Encoder[Geometry] = {
    implicit val pointEncoder: Encoder[Point] = deriveEncoder
    implicit val multiPointEncoder: Encoder[MultiPoint] = deriveEncoder
    implicit val lineStringEncoder: Encoder[LineString] = deriveEncoder
    implicit val multiLineStringEncoder: Encoder[MultiLineString] = deriveEncoder
    implicit val polygonEncoder: Encoder[Polygon] = deriveEncoder
    implicit val multiPolygonEncoder: Encoder[MultiPolygon] = deriveEncoder
    implicit val geometryCollectionEncoder: Encoder[GeometryCollection] = deriveEncoder
    deriveEncoder
  }
  implicit val geometryDecoder: Decoder[Geometry] = {
    implicit val pointDecoder: Decoder[Point] = deriveDecoder
    implicit val multiPointDecoder: Decoder[MultiPoint] = deriveDecoder
    implicit val lineStringDecoder: Decoder[LineString] = deriveDecoder
    implicit val multiLineStringDecoder: Decoder[MultiLineString] = deriveDecoder
    implicit val polygonDecoder: Decoder[Polygon] = deriveDecoder
    implicit val multiPolygonDecoder: Decoder[MultiPolygon] = deriveDecoder
    implicit val geometryCollectionDecoder: Decoder[GeometryCollection] = deriveDecoder
    deriveDecoder
  }
  implicit val geoJSONEncoder: Encoder[GeoJSON] = {
    implicit val featureEncoder: Encoder[Feature] = deriveEncoder
    implicit val featureCollectionEncoder: Encoder[FeatureCollection] = deriveEncoder
    deriveEncoder
  }
  implicit val geoJSONDecoder: Decoder[GeoJSON] = {
    implicit val featureDecoder: Decoder[Feature] = deriveDecoder
    implicit val featureCollectionDecoder: Decoder[FeatureCollection] = deriveDecoder
    deriveDecoder
  }
}