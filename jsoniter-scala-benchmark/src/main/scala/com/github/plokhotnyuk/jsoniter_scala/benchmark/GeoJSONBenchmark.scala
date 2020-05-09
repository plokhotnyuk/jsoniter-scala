package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.{flatten, transientDefault}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weepickle.v1.implicits.{discriminator, dropDefault, key}

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
  sealed trait Geometry extends Product with Serializable

  @discriminator("type")
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
  sealed trait GeoJSON extends Product with Serializable

  @discriminator("type")
  sealed trait SimpleGeoJSON extends GeoJSON

  @key("Feature")
  case class Feature(
    @transientDefault @dropDefault properties: Map[String, String] = Map.empty,
    geometry: Geometry,
    @transientDefault @dropDefault bbox: Option[(Double, Double, Double, Double)] = None) extends SimpleGeoJSON

  @key("FeatureCollection")
  case class FeatureCollection(
    features: IndexedSeq[SimpleGeoJSON],
    @transientDefault @dropDefault bbox: Option[(Double, Double, Double, Double)] = None) extends GeoJSON
}

abstract class GeoJSONBenchmark extends CommonParams {
  //Borders of Switzerland, from: https://github.com/mledoze/countries/blob/master/data/che.geo.json
  var jsonBytes: Array[Byte] = bytes(getClass.getResourceAsStream("che-1.geo.json"))
  var obj: GeoJSON = readFromArray[GeoJSON](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  var jsonString1: String = new String(jsonBytes, UTF_8)
  var jsonString2: String = new String(bytes(getClass.getResourceAsStream("che-2.geo.json")), UTF_8)
  var jsonString3: String = new String(bytes(getClass.getResourceAsStream("che-3.geo.json")), UTF_8)
  var jsonString4: String = new String(bytes(getClass.getResourceAsStream("che-4.geo.json")), UTF_8)
}