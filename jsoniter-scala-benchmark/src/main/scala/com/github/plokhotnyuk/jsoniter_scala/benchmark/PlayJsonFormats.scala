package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time._

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import julienrf.json.derived.flat
import play.api.libs.json._
import play.api.libs.functional.syntax._
import ai.x.play.json.Jsonx

import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.mutable
import scala.util.Try

object PlayJsonFormats {
  // Allow case classes with Tuple2 types to be represented as a Json Array with 2 elements e.g. (Double, Double)
  // Borrowed from https://gist.github.com/alexanderjarvis/4595298
  implicit def tuple2Reads[A, B](implicit aReads: Reads[A], bReads: Reads[B]): Reads[Tuple2[A, B]] =
    new Reads[Tuple2[A, B]] {
      def reads(json: JsValue): JsResult[Tuple2[A, B]] = Try {
        val JsArray(IndexedSeq(aJson, bJson)) = json
        aReads.reads(aJson).flatMap(a => bReads.reads(bJson).map(b => (a, b)))
      }.getOrElse(JsError("Expected array of two elements"))
    }

  implicit def tuple2Writes[A, B](implicit aWrites: Writes[A], bWrites: Writes[B]): Writes[Tuple2[A, B]] =
    new Writes[Tuple2[A, B]] {
      def writes(tuple: Tuple2[A, B]) = JsArray(Seq(aWrites.writes(tuple._1), bWrites.writes(tuple._2)))
    }

  implicit val charFormat: Format[Char] = Format(
    Reads(js => JsSuccess(js.as[String].charAt(0))),
    Writes(c => JsString(c.toString)))
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format
  val nestedStructsFormat: OFormat[NestedStructs] = Json.format
  val anyRefsFormat: OFormat[AnyRefs] = Json.format
  val anyValsFormat: OFormat[AnyVals] = {
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
  val bitSetFormat: Format[BitSet] = Format(
    Reads(js => JsSuccess(BitSet(js.as[Array[Int]]:_*))), // WARNING: don't do this for open-system
    Writes((es: BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  val mutableBitSetFormat: Format[mutable.BitSet] = Format(
    Reads(js => JsSuccess(mutable.BitSet(js.as[Array[Int]]:_*))), // WARNING: don't do this for open-system
    Writes((es: mutable.BitSet) => JsArray(es.toArray.map(v => JsNumber(BigDecimal(v))))))
  val intMapOfBooleansFormat: OFormat[IntMap[Boolean]] = OFormat(
    Reads[IntMap[Boolean]](js => JsSuccess(IntMap(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toInt, e._2)):_*))),
    OWrites[IntMap[Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val mapOfIntsToBooleansFormat: OFormat[Map[Int, Boolean]] = OFormat(
    Reads[Map[Int, Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toInt, e._2)))),
    OWrites[Map[Int, Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val mutableLongMapOfBooleansFormat: OFormat[mutable.LongMap[Boolean]] = OFormat(
    Reads[mutable.LongMap[Boolean]](js => JsSuccess(mutable.LongMap(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toLong, e._2)):_*))),
    OWrites[mutable.LongMap[Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val mutableMapOfIntsToBooleansFormat: OFormat[mutable.Map[Int, Boolean]] = OFormat(
    Reads[mutable.Map[Int, Boolean]](js => JsSuccess(mutable.Map(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toInt, e._2)):_*))),
    OWrites[mutable.Map[Int, Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val openHashMapOfIntsToBooleansFormat: OFormat[mutable.OpenHashMap[Int, Boolean]] = OFormat(
    Reads[mutable.OpenHashMap[Int, Boolean]](js => JsSuccess(mutable.OpenHashMap(js.as[Map[String, Boolean]].toSeq.map(e => (e._1.toInt, e._2)):_*))),
    OWrites[mutable.OpenHashMap[Int, Boolean]](m => Json.toJsObject(mutable.LinkedHashMap[String, Boolean](m.toSeq.map(e => (e._1.toString, e._2)):_*))))
  val primitivesFormat: OFormat[Primitives] = Json.format[Primitives]
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format
  val adtFormat: OFormat[ADTBase] = {
    implicit lazy val v1: OFormat[X] = Json.format
    implicit lazy val v2: OFormat[Y] = Json.format
    implicit lazy val v3: OFormat[Z] = Json.format
    implicit lazy val v4: OFormat[ADTBase] = flat.oformat((__ \ "type").format[String])
    v4
  }
  val geoJSONFormat: OFormat[GeoJSON] = {
    implicit lazy val v1: Format[Point] = (__ \ "coordinates").format[(Double, Double)].inmap(Point.apply, _.coordinates)
    implicit lazy val v2: OFormat[MultiPoint] = Json.format
    implicit lazy val v3: OFormat[LineString] = Json.format
    implicit lazy val v4: OFormat[MultiLineString] = Json.format
    implicit lazy val v5: OFormat[Polygon] = Json.format
    implicit lazy val v6: OFormat[MultiPolygon] = Json.format
    implicit lazy val v7: OFormat[GeometryCollection] = Json.format
    implicit lazy val v8: OFormat[Geometry] = flat.oformat((__ \ "type").format[String])
    implicit lazy val v9: OFormat[Feature] = Json.format
    implicit lazy val v10: OFormat[FeatureCollection] = Json.format
    implicit lazy val v11: OFormat[GeoJSON] = flat.oformat((__ \ "type").format[String])
    v11
  }
  val googleMapsAPIFormat: OFormat[DistanceMatrix] = {
    implicit val v1: OFormat[Value] = Json.format
    implicit val v2: OFormat[Elements] = Json.format
    implicit val v3: OFormat[Rows] = Json.format
    Json.format[DistanceMatrix]
  }
  val twitterAPIFormat: Format[Seq[Tweet]] = {
    implicit val v1: OFormat[Urls] = Json.format
    implicit val v2: OFormat[Url] = Json.format
    implicit val v3: OFormat[UserEntities] = Json.format
    implicit val v4: OFormat[UserMentions] = Json.format
    implicit val v5: OFormat[Entities] = Json.format
    implicit val v6: Format[User] = Jsonx.formatCaseClass
    implicit val v7: Format[RetweetedStatus] = Jsonx.formatCaseClass
    implicit val v8: Format[Tweet] = Jsonx.formatCaseClass
    Format(
      Reads[Seq[Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[Tweet]))),
      Writes[Seq[Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)))))
  }
  val enumArrayFormat: Format[Array[SuitEnum]] = {
    implicit val v1: Format[SuitEnum] = Format(Reads.enumNameReads(SuitEnum), Writes.enumNameWrites)
    Format(
      Reads(js => JsSuccess(js.as[Array[JsString]].map(_.as[SuitEnum]))),
      Writes(es => JsArray(es.map(t => Json.toJson(t)))))
  }
  val enumADTArrayFormat: Format[Array[SuitADT]] = {
    val suite = Map(
      "Hearts" -> Hearts,
      "Spades" -> Spades,
      "Diamonds" -> Diamonds,
      "Clubs" -> Clubs
    )
    Format(
      Reads(js => Try(js.as[Array[JsString]].map(s => suite(s.value))).fold[JsResult[Array[SuitADT]]](_ => JsError("SuitADT"), s => JsSuccess(s))),
      Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  }
  val javaEnumArrayFormat: Format[Array[Suit]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => Suit.valueOf(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.name)))))
  val charArrayFormat: Format[Array[Char]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(_.value.charAt(0)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val bigIntArrayFormat: Format[Array[BigInt]] = Format(
    Reads(js => Try(js.as[Array[JsNumber]].map(_.value.toBigIntExact.get)).fold[JsResult[Array[BigInt]]](_ => JsError("BigInt"), s => JsSuccess(s))),
    Writes(es => JsArray(es.map(v => JsNumber(BigDecimal(v))))))
  val monthDayArrayFormat: Format[Array[MonthDay]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => MonthDay.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val offsetTimeArrayFormat: Format[Array[OffsetTime]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => OffsetTime.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val yearArrayFormat: Format[Array[Year]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => Year.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val yearMonthArrayFormat: Format[Array[YearMonth]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => YearMonth.parse(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val zoneOffsetArrayFormat: Format[Array[ZoneOffset]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => ZoneOffset.of(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
}