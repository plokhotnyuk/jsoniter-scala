/*
 * Copyright (c) 2017-2026 Andriy Plokhotnyuk, and respective contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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