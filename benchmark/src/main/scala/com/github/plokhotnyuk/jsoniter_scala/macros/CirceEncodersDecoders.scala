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
  // GeoJSON
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder[Feature]
  implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]
  implicit val featureCollectionEncoder: Encoder[FeatureCollection] = deriveEncoder[FeatureCollection]
  implicit val featureCollectionDecoder: Decoder[FeatureCollection] = deriveDecoder[FeatureCollection]
  implicit val geoJSONEncoder: Encoder[GeoJSON] = deriveEncoder[GeoJSON]
  implicit val geoJSONDecoder: Decoder[GeoJSON] = deriveDecoder[GeoJSON]
  implicit val pointEncoder: Encoder[Point] = deriveEncoder[Point]
  implicit val pointDecoder: Decoder[Point] = deriveDecoder[Point]
  implicit val multiPointEncoder: Encoder[MultiPoint] = deriveEncoder[MultiPoint]
  implicit val multiPointDecoder: Decoder[MultiPoint] = deriveDecoder[MultiPoint]
  implicit val lineStringEncoder: Encoder[LineString] = deriveEncoder[LineString]
  implicit val lineStringDecoder: Decoder[LineString] = deriveDecoder[LineString]
  implicit val multiLineStringEncoder: Encoder[MultiLineString] = deriveEncoder[MultiLineString]
  implicit val multiLineStringDecoder: Decoder[MultiLineString] = deriveDecoder[MultiLineString]
  implicit val polygonEncoder: Encoder[Polygon] = deriveEncoder[Polygon]
  implicit val polygonDecoder: Decoder[Polygon] = deriveDecoder[Polygon]
  implicit val multiPolygonEncoder: Encoder[MultiPolygon] = deriveEncoder[MultiPolygon]
  implicit val multiPolygonDecoder: Decoder[MultiPolygon] = deriveDecoder[MultiPolygon]
  implicit val geometryCollectionEncoder: Encoder[GeometryCollection] = deriveEncoder[GeometryCollection]
  implicit val geometryCollectionDecoder: Decoder[GeometryCollection] = deriveDecoder[GeometryCollection]
  implicit val geometryEncoder: Encoder[Geometry] = deriveEncoder[Geometry]
  implicit val geometryDecoder: Decoder[Geometry] = deriveDecoder[Geometry]
}