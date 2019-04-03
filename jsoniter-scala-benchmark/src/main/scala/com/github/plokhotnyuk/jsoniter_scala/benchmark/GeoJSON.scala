package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.flatten
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

import scala.collection.immutable.IndexedSeq
import scala.reflect.io.Streamable

//FIXME: the GeoJSON spec require to avoid ability to create nested Geometry or Feature collections, also it can be a security issue for untrusted input
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
case class Point(coordinates: (Double, Double)) extends Geometry
case class MultiPoint(coordinates: IndexedSeq[(Double, Double)]) extends Geometry
case class LineString(coordinates: IndexedSeq[(Double, Double)]) extends Geometry
case class MultiLineString(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends Geometry
case class Polygon(coordinates: IndexedSeq[IndexedSeq[(Double, Double)]]) extends Geometry
case class MultiPolygon(coordinates: IndexedSeq[IndexedSeq[IndexedSeq[(Double, Double)]]]) extends Geometry
case class GeometryCollection(geometries: IndexedSeq[Geometry]) extends Geometry

@flatten("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[Feature], name = "Feature"),
  new Type(value = classOf[FeatureCollection], name = "FeatureCollection")))
sealed trait GeoJSON extends Product with Serializable
case class Feature(properties: Map[String, String], geometry: Geometry) extends GeoJSON
case class FeatureCollection(features: IndexedSeq[GeoJSON]) extends GeoJSON

object GeoJSON {
  //Borders of Switzerland, from: https://github.com/mledoze/countries/blob/master/data/che.geo.json
  var jsonBytes: Array[Byte] = Streamable.bytes(getClass.getResourceAsStream("che-1.geo.json"))
  var jsonString1: String = new String(jsonBytes, UTF_8)
  //the same as previous but changed position of the `type` fields
  var jsonString2: String = new String(Streamable.bytes(getClass.getResourceAsStream("che-2.geo.json")), UTF_8)
  var jsonString3: String = new String(Streamable.bytes(getClass.getResourceAsStream("che-3.geo.json")), UTF_8)
  var jsonString4: String = new String(Streamable.bytes(getClass.getResourceAsStream("che-4.geo.json")), UTF_8)
}