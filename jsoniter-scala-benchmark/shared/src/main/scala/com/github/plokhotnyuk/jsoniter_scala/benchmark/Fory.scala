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

import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI.Tweet
import org.apache.fory.json.ForyJson
import org.apache.fory.json.annotation.{JsonByteArray, JsonFormat, JsonInclude, JsonMixin, JsonSubTypes}
import org.apache.fory.json.annotation.JsonProperty.Include
import org.apache.fory.json.scala.{ForyJsonScala, ScalaJsonCodec, ScalaTypeRef}
import org.apache.fory.reflect.TypeRef
import scala.collection.immutable.{ArraySeq, IntMap}
import scala.collection.mutable

object Fory {
  val requiredFieldsJson: ForyJson = ForyJsonScala.builder()
    .failOnMissingRequiredProperties(true).build()
  val escapingJson: ForyJson = ForyJsonScala.builder().escapeNonAscii(true).build()
  val arrayBytesJson: ForyJson = ForyJsonScala.builder()
    .byteArrayFormat(JsonByteArray.Format.ARRAY).build()
  val base16Json: ForyJson = ForyJsonScala.builder()
    .byteArrayFormat(JsonByteArray.Format.BASE16).build()

  @JsonMixin(target = classOf[GitHubActionsAPI.Artifact])
  abstract class ArtifactMixin {
    @JsonFormat(shape = JsonFormat.Shape.STRING) var expired: Boolean = false
  }

  @JsonMixin(target = classOf[OpenRTB.BidRequest])
  @JsonInclude(Include.NON_DEFAULT)
  abstract class BidRequestMixin

  @JsonMixin(target = classOf[OpenRTB.Imp])
  @JsonInclude(Include.NON_DEFAULT)
  abstract class ImpMixin

  @JsonMixin(target = classOf[OpenRTB.Video])
  @JsonInclude(Include.NON_DEFAULT)
  abstract class VideoMixin

  @JsonMixin(target = classOf[OpenRTB.Pmp])
  @JsonInclude(Include.NON_DEFAULT)
  abstract class PmpMixin

  @JsonMixin(target = classOf[OpenRTB.Deal])
  @JsonInclude(Include.NON_DEFAULT)
  abstract class DealMixin

  @JsonMixin(target = classOf[GeoJSON.GeoJSON])
  @JsonSubTypes(property = "type", value = Array(
    new JsonSubTypes.Type(value = classOf[GeoJSON.Feature], name = "Feature"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.FeatureCollection], name = "FeatureCollection")))
  abstract class GeoJSONMixin

  @JsonMixin(target = classOf[GeoJSON.SimpleGeoJSON])
  @JsonSubTypes(property = "type", value = Array(
    new JsonSubTypes.Type(value = classOf[GeoJSON.Feature], name = "Feature")))
  abstract class SimpleGeoJSONMixin

  @JsonMixin(target = classOf[GeoJSON.Geometry])
  @JsonSubTypes(property = "type", value = Array(
    new JsonSubTypes.Type(value = classOf[GeoJSON.Point], name = "Point"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiPoint], name = "MultiPoint"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.LineString], name = "LineString"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiLineString], name = "MultiLineString"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.Polygon], name = "Polygon"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiPolygon], name = "MultiPolygon"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.GeometryCollection], name = "GeometryCollection")))
  abstract class GeometryMixin

  @JsonMixin(target = classOf[GeoJSON.SimpleGeometry])
  @JsonSubTypes(property = "type", value = Array(
    new JsonSubTypes.Type(value = classOf[GeoJSON.Point], name = "Point"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiPoint], name = "MultiPoint"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.LineString], name = "LineString"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiLineString], name = "MultiLineString"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.Polygon], name = "Polygon"),
    new JsonSubTypes.Type(value = classOf[GeoJSON.MultiPolygon], name = "MultiPolygon")))
  abstract class SimpleGeometryMixin

  val omittingJson: ForyJson = ForyJsonScala.builder()
    .maxDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
    .defaultPropertyInclusion(Include.NON_EMPTY)
    // These model defaults are stable constants; authorize their evaluation for omission.
    .registerMixin(classOf[BidRequestMixin])
    .registerMixin(classOf[ImpMixin])
    .registerMixin(classOf[VideoMixin])
    .registerMixin(classOf[PmpMixin])
    .registerMixin(classOf[DealMixin])
    .registerMixin(classOf[GeoJSONMixin])
    .registerMixin(classOf[SimpleGeoJSONMixin])
    .registerMixin(classOf[GeometryMixin])
    .registerMixin(classOf[SimpleGeometryMixin])
    .build()

  val foryJson: ForyJson =
    ForyJsonScala.builder()
      .registerCodec(classOf[SuitADT], ScalaJsonCodec.stringEnum[SuitADT])
      .registerMixin(classOf[ArtifactMixin])
      .maxDepth(Int.MaxValue) // WARNING: It is an unsafe option for open systems
      .writeNullFields(false)
      .build()
  val arraySeqOfBooleansType: TypeRef[ArraySeq[Boolean]] = ScalaTypeRef[ArraySeq[Boolean]]
  val arrayOfEnumsType: TypeRef[Array[SuitEnum.Value]] = ScalaTypeRef[Array[SuitEnum.Value]]
  val arrayOfEnumADTsType: TypeRef[Array[SuitADT]] = ScalaTypeRef[Array[SuitADT]]
  val intMapOfBooleansType: TypeRef[IntMap[Boolean]] = ScalaTypeRef[IntMap[Boolean]]
  val listOfBooleansType: TypeRef[List[Boolean]] = ScalaTypeRef[List[Boolean]]
  val mapOfIntsToBooleansType: TypeRef[Map[Int, Boolean]] = ScalaTypeRef[Map[Int, Boolean]]
  val mutableLongMapOfBooleansType: TypeRef[mutable.LongMap[Boolean]] = ScalaTypeRef[mutable.LongMap[Boolean]]
  val mutableMapOfIntsToBooleansType: TypeRef[mutable.Map[Int, Boolean]] = ScalaTypeRef[mutable.Map[Int, Boolean]]
  val mutableSetOfIntsType: TypeRef[mutable.Set[Int]] = ScalaTypeRef[mutable.Set[Int]]
  val setOfIntsType: TypeRef[Set[Int]] = ScalaTypeRef[Set[Int]]
  val seqOfTweetsType: TypeRef[Seq[Tweet]] = ScalaTypeRef[Seq[Tweet]]
  val vectorOfBooleansType: TypeRef[Vector[Boolean]] = ScalaTypeRef[Vector[Boolean]]
}
