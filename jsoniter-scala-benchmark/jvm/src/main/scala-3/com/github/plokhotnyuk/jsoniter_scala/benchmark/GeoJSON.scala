package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.rallyhealth.weepickle.v1.implicits.{discriminator, dropDefault, key}
import zio.json.jsonDiscriminator
import scala.collection.immutable.IndexedSeq

object GeoJSON {
  @discriminator("type")
  @jsonDiscriminator("type")
  sealed trait Geometry extends Product with Serializable

  @discriminator("type")
  @jsonDiscriminator("type")
  sealed trait SimpleGeometry extends Geometry

  @key("Point")
  case class Point(coordinates: (Double, Double)) extends SimpleGeometry

  @key("MultiPoint")
  case class MultiPoint(coordinates: IndexedSeq[(Double, Double)]) extends SimpleGeometry

  @key("LineString")
  case class LineString(coordinates: IndexedSeq[(Double, Double)]) extends SimpleGeometry

  @key("MultiLineString")
  case class MultiLineString(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends SimpleGeometry

  @key("Polygon")
  case class Polygon(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends SimpleGeometry

  @key("MultiPolygon")
  case class MultiPolygon(coordinates: IndexedSeq[IndexedSeq[IndexedSeq[(Double, Double)]]]) extends SimpleGeometry

  @key("GeometryCollection")
  case class GeometryCollection(geometries: IndexedSeq[SimpleGeometry]) extends Geometry

  @discriminator("type")
  @jsonDiscriminator("type")
  sealed trait GeoJSON extends Product with Serializable

  @discriminator("type")
  @jsonDiscriminator("type")
  sealed trait SimpleGeoJSON extends GeoJSON

  @key("Feature")
  @dropDefault
  case class Feature(
    properties: Map[String, String] = Map.empty,
    geometry: Geometry,
    bbox: Option[(Double, Double, Double, Double)] = None) extends SimpleGeoJSON

  @key("FeatureCollection")
  @dropDefault
  case class FeatureCollection(
    features: IndexedSeq[SimpleGeoJSON],
    bbox: Option[(Double, Double, Double, Double)] = None) extends GeoJSON
}