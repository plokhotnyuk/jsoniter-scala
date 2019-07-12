package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._

import ai.x.play.json.Jsonx
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BitMask.toBitMask
import julienrf.json.derived.flat
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule

import scala.collection.immutable.{BitSet, IntMap, Map, Seq}
import scala.collection.mutable
import scala.util.Try

object PlayJsonFormats {
  private[this] val indenter = new DefaultIndenter("  ", "\n")
  private[this] val prettyPrintMapper = new ObjectMapper {
    registerModule(new PlayJsonModule(JsonParserSettings.settings))
    configure(SerializationFeature.INDENT_OUTPUT, true)
    setDefaultPrettyPrinter(new DefaultPrettyPrinter {
      indentObjectsWith(indenter)
      indentArraysWith(indenter)
    })
  }

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = prettyPrintMapper.writeValueAsBytes(jsValue)

  // Allow case classes with Tuple2 types to be represented as a Json Array with 2 elements e.g. (Double, Double)
  // Borrowed from https://gist.github.com/alexanderjarvis/4595298
  implicit def tuple2Reads[A, B](implicit aReads: Reads[A], bReads: Reads[B]): Reads[Tuple2[A, B]] =
    (json: JsValue) => Try {
      import scala.language.existentials
      val JsArray(arr) = json
      aReads.reads(arr(0)).flatMap(a => bReads.reads(arr(1)).map(b => (a, b)))
    }.getOrElse(JsError("Expected array of two elements, but got: " + json))

  implicit def tuple2Writes[A, B](implicit aWrites: Writes[A], bWrites: Writes[B]): Writes[Tuple2[A, B]] =
    (tuple: Tuple2[A, B]) => JsArray(Seq(aWrites.writes(tuple._1), bWrites.writes(tuple._2)))

  implicit val charFormat: Format[Char] = Format(
    Reads(js => JsSuccess(js.as[String].charAt(0))),
    Writes(c => JsString(c.toString)))
  implicit val missingReqFieldsFormat: OFormat[MissingRequiredFields] = Json.format
  implicit val nestedStructsFormat: OFormat[NestedStructs] = Json.format
  implicit val anyRefsFormat: OFormat[AnyRefs] = Json.format
  implicit val anyValsFormat: OFormat[AnyVals] = {
    implicit val v1: Format[ByteVal] = Jsonx.formatInline
    implicit val v2: Format[ShortVal] = Jsonx.formatInline
    implicit val v3: Format[IntVal] = Jsonx.formatInline
    implicit val v4: Format[LongVal] = Jsonx.formatInline
    implicit val v5: Format[BooleanVal] = Jsonx.formatInline
    implicit val v6: Format[CharVal] = Jsonx.formatInline
    implicit val v7: Format[DoubleVal] = Jsonx.formatInline
    implicit val v8: Format[FloatVal] = Jsonx.formatInline
    Json.format[AnyVals]
  }
  implicit val bitSetFormat: Format[BitSet] = Format(
    Reads(js => JsSuccess(BitSet.fromBitMaskNoCopy(toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: don't do this for open-systems */)))),
    Writes((es: BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val mutableBitSetFormat: Format[mutable.BitSet] = Format(
    Reads(js => JsSuccess(mutable.BitSet.fromBitMaskNoCopy(toBitMask(js.as[Array[Int]], Int.MaxValue /* WARNING: don't do this for open-systems */)))),
    Writes((es: mutable.BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  implicit val intMapOfBooleansFormat: OFormat[IntMap[Boolean]] = OFormat(
    Reads[IntMap[Boolean]](js => JsSuccess(IntMap(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toInt, e._2)):_*))),
    OWrites[IntMap[Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val mapOfIntsToBooleansFormat: OFormat[Map[Int, Boolean]] = OFormat(
    Reads[Map[Int, Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toInt, e._2)))),
    OWrites[Map[Int, Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  implicit val mutableLongMapOfBooleansFormat: OFormat[mutable.LongMap[Boolean]] = OFormat(
    Reads[mutable.LongMap[Boolean]](js => JsSuccess(mutable.LongMap(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toLong, e._2)):_*))),
    OWrites[mutable.LongMap[Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val mutableMapOfIntsToBooleansFormat: OFormat[mutable.Map[Int, Boolean]] = OFormat(
    Reads[mutable.Map[Int, Boolean]](js => JsSuccess(mutable.Map(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toInt, e._2)):_*))),
    OWrites[mutable.Map[Int, Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  implicit val primitivesFormat: OFormat[Primitives] = Json.format
  implicit val extractFieldsFormat: OFormat[ExtractFields] = Json.format
  val adtFormat: OFormat[ADTBase] = {
    implicit lazy val v1: OFormat[X] = Json.format
    implicit lazy val v2: OFormat[Y] = Json.format
    implicit lazy val v3: OFormat[Z] = Json.format
    implicit lazy val v4: OFormat[ADTBase] = flat.oformat((__ \ "type").format[String])
    v4
  }
  val geoJSONFormat: OFormat[GeoJSON.GeoJSON] = {
    implicit lazy val v1: Format[GeoJSON.Point] =
      (__ \ "coordinates").format[(Double, Double)].inmap(GeoJSON.Point.apply, _.coordinates)
    implicit lazy val v2: OFormat[GeoJSON.MultiPoint] = Json.format
    implicit lazy val v3: OFormat[GeoJSON.LineString] = Json.format
    implicit lazy val v4: OFormat[GeoJSON.MultiLineString] = Json.format
    implicit lazy val v5: OFormat[GeoJSON.Polygon] = Json.format
    implicit lazy val v6: OFormat[GeoJSON.MultiPolygon] = Json.format
    implicit lazy val v7: OFormat[GeoJSON.GeometryCollection] = Json.format
    implicit lazy val v8: OFormat[GeoJSON.Geometry] = flat.oformat((__ \ "type").format[String])
    implicit lazy val v9: OFormat[GeoJSON.Feature] = Json.format
    implicit lazy val v10: OFormat[GeoJSON.FeatureCollection] = Json.format
    implicit lazy val v11: OFormat[GeoJSON.GeoJSON] = flat.oformat((__ \ "type").format[String])
    v11
  }
  implicit val googleMapsAPIFormat: OFormat[GoogleMapsAPI.DistanceMatrix] = {
    implicit val v1: OFormat[GoogleMapsAPI.Value] = Json.format
    implicit val v2: OFormat[GoogleMapsAPI.Elements] = Json.format
    implicit val v3: OFormat[GoogleMapsAPI.Rows] = Json.format
    Json.format[GoogleMapsAPI.DistanceMatrix]
  }
  implicit val twitterAPIFormat: Format[Seq[TwitterAPI.Tweet]] = {
    implicit val v1: OFormat[TwitterAPI.Urls] = Json.format
    implicit val v2: OFormat[TwitterAPI.Url] = Json.format
    implicit val v3: OFormat[TwitterAPI.UserEntities] = Json.format
    implicit val v4: OFormat[TwitterAPI.UserMentions] = Json.format
    implicit val v5: OFormat[TwitterAPI.Entities] = Json.format
    implicit val v6: Format[TwitterAPI.User] = Jsonx.formatCaseClass
    implicit val v7: Format[TwitterAPI.RetweetedStatus] = Jsonx.formatCaseClass
    implicit val v8: Format[TwitterAPI.Tweet] = Jsonx.formatCaseClass
    Format(
      Reads[Seq[TwitterAPI.Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[TwitterAPI.Tweet]))),
      Writes[Seq[TwitterAPI.Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)))))
  }
  implicit val enumArrayFormat: Format[Array[SuitEnum]] = {
    implicit val v1: Format[SuitEnum] = Format(Reads.enumNameReads(SuitEnum), Writes.enumNameWrites)
    Format(
      Reads(js => JsSuccess(js.as[Array[JsString]].map(_.as[SuitEnum]))),
      Writes(es => JsArray(es.map(t => Json.toJson(t)))))
  }
  implicit val enumADTArrayFormat: Format[Array[SuitADT]] = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs)
    Format(
      Reads(js => Try(js.as[Array[JsString]].map(s => suite(s.value))).fold[JsResult[Array[SuitADT]]](_ => JsError("SuitADT"), s => JsSuccess(s))),
      Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  }
  implicit val javaEnumArrayFormat: Format[Array[Suit]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => Suit.valueOf(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.name)))))
  implicit val charArrayFormat: Format[Array[Char]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(_.value.charAt(0)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  implicit val bigIntArrayFormat: Format[Array[BigInt]] = Format(
    Reads(js => Try(js.as[Array[JsNumber]].map(_.value.toBigIntExact.get)).fold[JsResult[Array[BigInt]]](_ => JsError("BigInt"), s => JsSuccess(s))),
    Writes(es => JsArray(es.map(v => JsNumber(BigDecimal(v))))))
  implicit val monthDayArrayFormat: Format[Array[MonthDay]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => MonthDay.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  implicit val offsetTimeArrayFormat: Format[Array[OffsetTime]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => OffsetTime.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  implicit val yearArrayFormat: Format[Array[Year]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => Year.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  implicit val yearMonthArrayFormat: Format[Array[YearMonth]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => YearMonth.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  implicit val zoneOffsetArrayFormat: Format[Array[ZoneOffset]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => ZoneOffset.of(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
}