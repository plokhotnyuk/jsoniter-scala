package com.github.plokhotnyuk.jsoniter_scala.benchmark

import play.api.libs.functional.syntax._
import play.api.libs.json._

object PlayJsonDerivedFormats {
  implicit val config: JsonConfiguration.Aux[Json.MacroOptions] = JsonConfiguration(discriminator = "type",
    typeNaming = JsonNaming(fullName => fullName.substring(Math.max(fullName.lastIndexOf('.') + 1, 0))))
  val adtFormat: Format[ADTBase] = {
    implicit lazy val v1: Format[X] = Json.format
    implicit lazy val v2: Format[Y] = Json.format
    implicit lazy val v3: Format[Z] = Json.format
    implicit lazy val v4: Format[ADTBase] = Json.format
    v4
  }
  val geoJSONFormat: Format[GeoJSON.GeoJSON] = {
    implicit val v1: Format[GeoJSON.Point] =
      (__ \ "coordinates").format[(Double, Double)].inmap(GeoJSON.Point.apply, _.coordinates)
    implicit lazy val v2: Format[GeoJSON.MultiPoint] = Json.format
    implicit lazy val v3: Format[GeoJSON.LineString] = Json.format
    implicit lazy val v4: Format[GeoJSON.MultiLineString] = Json.format
    implicit lazy val v5: Format[GeoJSON.Polygon] = Json.format
    implicit lazy val v6: Format[GeoJSON.MultiPolygon] = Json.format
    implicit lazy val v7: Format[GeoJSON.SimpleGeometry] = Json.format
    implicit lazy val v8: Format[GeoJSON.GeometryCollection] = Json.format
    implicit lazy val v9: Format[GeoJSON.Geometry] = Json.format
    implicit lazy val v10: Format[GeoJSON.Feature] = Json.format
    implicit lazy val v11: Format[GeoJSON.SimpleGeoJSON] = Json.format
    implicit lazy val v12: Format[GeoJSON.FeatureCollection] = Json.format
    implicit lazy val v13: Format[GeoJSON.GeoJSON] = Json.format
    v13
  }
}

