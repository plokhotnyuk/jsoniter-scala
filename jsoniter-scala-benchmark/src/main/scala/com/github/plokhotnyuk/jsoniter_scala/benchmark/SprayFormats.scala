package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import pl.iterators.kebs.json.KebsSpray
import spray.json._

import scala.collection.immutable.Map
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.util.control.NonFatal

// Based on the code found: https://github.com/spray/spray-json/issues/200
case class EnumJsonFormat[T <: scala.Enumeration](e: T) extends RootJsonFormat[T#Value] {
  override def read(json: JsValue): T#Value =
    e.values.iterator.find { ev =>
      json.isInstanceOf[JsString] && json.asInstanceOf[JsString].value == ev.toString
    }.getOrElse(deserializationError(s"Expected JSON string of value from enum $e, but got $json"))

  override def write(ev: T#Value): JsValue = JsString(ev.toString)
}

object FlatSprayFormats extends DefaultJsonProtocol with KebsSpray {
  implicit val anyValsJsonFormat: RootJsonFormat[AnyVals] = jsonFormatN
}

object SprayFormats extends DefaultJsonProtocol with KebsSpray.NoFlat {
  val jsonParserSettings: JsonParserSettings = JsonParserSettings.default
    .withMaxDepth(Int.MaxValue).withMaxNumberCharacters(Int.MaxValue) /*WARNING: don't do this for open-systems*/
  // Based on the Cat/Dog sample: https://gist.github.com/jrudolph/f2d0825aac74ed81c92a
  val adtBaseJsonFormat: RootJsonFormat[ADTBase] = {
    implicit lazy val jf1: RootJsonFormat[X] = jsonFormatN
    implicit lazy val jf2: RootJsonFormat[Y] = jsonFormatN
    implicit lazy val jf3: RootJsonFormat[Z] = jsonFormatN
    implicit lazy val jf4: RootJsonFormat[ADTBase] = new RootJsonFormat[ADTBase] {
      override def read(json: JsValue): ADTBase = Try(json.asJsObject.getFields("type") match {
        case Seq(JsString("X")) => json.convertTo[X]
        case Seq(JsString("Y")) => json.convertTo[Y]
        case Seq(JsString("Z")) => json.convertTo[Z]
      }).getOrElse(deserializationError(s"Cannot deserialize ADTBase"))

      override def write(obj: ADTBase): JsValue = JsObject((obj match {
        case x: X => x.toJson
        case y: Y => y.toJson
        case z: Z => z.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }
    jf4
  }
  implicit val anyRefsJsonFormat: RootJsonFormat[AnyRefs] = jsonFormatN
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormatN
  val geoJSONJsonFormat: RootJsonFormat[GeoJSON] = {
    implicit lazy val jf1: RootJsonFormat[Point] = jsonFormatN
    implicit lazy val jf2: RootJsonFormat[MultiPoint] = jsonFormatN
    implicit lazy val jf3: RootJsonFormat[LineString] = jsonFormatN
    implicit lazy val jf4: RootJsonFormat[MultiLineString] = jsonFormatN
    implicit lazy val jf5: RootJsonFormat[Polygon] = jsonFormatN
    implicit lazy val jf6: RootJsonFormat[MultiPolygon] = jsonFormatN
    implicit lazy val jf7: RootJsonFormat[GeometryCollection] = jsonFormatN
    implicit lazy val jf8: RootJsonFormat[Geometry] = new RootJsonFormat[Geometry] {
      override def read(json: JsValue): Geometry = Try(json.asJsObject.getFields("type") match {
        case Seq(JsString("Point")) => json.convertTo[Point]
        case Seq(JsString("MultiPoint")) => json.convertTo[MultiPoint]
        case Seq(JsString("LineString")) => json.convertTo[LineString]
        case Seq(JsString("MultiLineString")) => json.convertTo[MultiLineString]
        case Seq(JsString("Polygon")) => json.convertTo[Polygon]
        case Seq(JsString("MultiPolygon")) => json.convertTo[MultiPolygon]
        case Seq(JsString("GeometryCollection")) => json.convertTo[GeometryCollection]
      }).getOrElse(deserializationError(s"Cannot deserialize Geometry"))

      override def write(obj: Geometry): JsValue = JsObject((obj match {
        case x: Point => x.toJson
        case x: MultiPoint => x.toJson
        case x: LineString => x.toJson
        case x: MultiLineString => x.toJson
        case x: Polygon => x.toJson
        case x: MultiPolygon => x.toJson
        case x: GeometryCollection => x.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }
    implicit lazy val jf9: RootJsonFormat[Feature] = jsonFormatN
    implicit lazy val jf10: RootJsonFormat[FeatureCollection] = jsonFormatN
    implicit lazy val jf11: RootJsonFormat[GeoJSON] = new RootJsonFormat[GeoJSON] {
      override def read(json: JsValue): GeoJSON = Try(json.asJsObject.getFields("type") match {
        case Seq(JsString("Feature")) => json.convertTo[Feature]
        case Seq(JsString("FeatureCollection")) => json.convertTo[FeatureCollection]
      }).getOrElse(deserializationError(s"Cannot deserialize GeoJSON"))

      override def write(obj: GeoJSON): JsValue = JsObject((obj match {
        case x: Feature => x.toJson
        case y: FeatureCollection => y.toJson
      }).asJsObject.fields + ("type" -> JsString(obj.productPrefix)))
    }
    jf11
  }
  implicit val googleMapsAPIJsonFormat: RootJsonFormat[DistanceMatrix] = jsonFormatN
  implicit val missingReqFieldsJsonFormat: RootJsonFormat[MissingReqFields] = jsonFormatN
  implicit val nestedStructsJsonFormat: RootJsonFormat[NestedStructs] = jsonFormatRec
  implicit val primitivesJsonFormat: RootJsonFormat[Primitives] = jsonFormatN
  implicit val suitEnumADTJsonFormat: RootJsonFormat[SuitADT] = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    stringJsonFormat(suite.apply)
  }
  implicit val suitEnumJsonFormat: RootJsonFormat[SuitEnum] = EnumJsonFormat(SuitEnum)
  implicit val suitJavaEnumJsonFormat: RootJsonFormat[Suit] = stringJsonFormat(Suit.valueOf)
  implicit val tweetJsonFormat: RootJsonFormat[Tweet] = jsonFormatN
  implicit val uuidJsonFormat: RootJsonFormat[UUID] = stringJsonFormat(UUID.fromString)

  def stringJsonFormat[T](construct: String => T): RootJsonFormat[T] = new RootJsonFormat[T] {
    def read(json: JsValue): T =
      if (!json.isInstanceOf[JsString]) deserializationError(s"Expected JSON string, but got $json")
      else {
        val s = json.asInstanceOf[JsString].value
        try construct(s) catch { case NonFatal(e) => deserializationError(s"Illegal value: $json", e) }
      }

    def write(obj: T): JsValue = new JsString(obj.toString)
  }

  implicit def arrayBufferJsonFormat[T : JsonFormat]: RootJsonFormat[ArrayBuffer[T]] =
    new RootJsonFormat[mutable.ArrayBuffer[T]] {
      def read(json: JsValue): mutable.ArrayBuffer[T] =
        if (!json.isInstanceOf[JsArray]) deserializationError(s"Expected JSON array, but got $json")
        else {
          val es = json.asInstanceOf[JsArray].elements
          val buf = new mutable.ArrayBuffer[T](es.size)
          es.foreach(e => buf += e.convertTo[T])
          buf
        }

      def write(buf: mutable.ArrayBuffer[T]): JsValue = JsArray(buf.map(_.toJson).toVector)
    }
}
