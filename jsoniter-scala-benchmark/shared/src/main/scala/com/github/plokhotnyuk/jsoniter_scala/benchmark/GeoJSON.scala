package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.avsystem.commons.serialization.{flatten, transientDefault}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.rallyhealth.weepickle.v1.implicits.{discriminator, dropDefault, key}
import zio.json.jsonDiscriminator

import scala.collection.immutable.IndexedSeq

object GeoJSON {
  @discriminator("type")
  @flatten("type")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(Array(
    new Type(value = classOf[Point], name = "Point"),
    new Type(value = classOf[MultiPoint], name = "MultiPoint"),
    new Type(value = classOf[LineString], name = "LineString"),
    new Type(value = classOf[MultiLineString], name = "MultiLineString"),
    new Type(value = classOf[Polygon], name = "Polygon"),
    new Type(value = classOf[MultiPolygon], name = "MultiPolygon"),
    new Type(value = classOf[GeometryCollection], name = "GeometryCollection")))
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
  @flatten("type")
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(Array(
    new Type(value = classOf[Feature], name = "Feature"),
    new Type(value = classOf[FeatureCollection], name = "FeatureCollection")))
  @jsonDiscriminator("type")
  sealed trait GeoJSON extends Product with Serializable

  @discriminator("type")
  @jsonDiscriminator("type")
  sealed trait SimpleGeoJSON extends GeoJSON

  @key("Feature")
  @dropDefault
  case class Feature(
    @transientDefault properties: Map[String, String] = Map.empty,
    geometry: Geometry,
    @transientDefault bbox: Option[(Double, Double, Double, Double)] = None) extends SimpleGeoJSON

  @key("FeatureCollection")
  @dropDefault
  case class FeatureCollection(
    features: IndexedSeq[SimpleGeoJSON],
    @transientDefault bbox: Option[(Double, Double, Double, Double)] = None) extends GeoJSON
}