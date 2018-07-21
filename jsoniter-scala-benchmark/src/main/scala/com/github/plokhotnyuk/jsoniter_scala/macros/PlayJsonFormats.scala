package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time.{OffsetTime, Year, YearMonth, ZoneOffset}

import julienrf.json.derived.flat
import play.api.libs.json._
import ai.x.play.json.Jsonx
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.{BitSet, IntMap, Map, Seq}
import scala.collection.mutable

object PlayJsonFormats {
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format
  val nestedStructsFormat: OFormat[NestedStructs] = Json.format
  val anyRefsFormat: OFormat[AnyRefs] = Json.format
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
  val primitivesFormat: OFormat[Primitives] = {
    implicit val v1: Format[Char] = Format(
      Reads(js => JsSuccess(js.as[String].charAt(0))),
      Writes(c => JsString(c.toString)))
    Json.format[Primitives]
  }
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format
  val adtFormat: OFormat[AdtBase] = {
    implicit lazy val v1: OFormat[A] = Json.format
    implicit lazy val v2: OFormat[B] = Json.format
    implicit lazy val v3: OFormat[C] = Json.format
    implicit lazy val v4: OFormat[AdtBase] = flat.oformat((__ \ "type").format[String])
    v4
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
    implicit val v1: Reads[SuitEnum] = Reads.enumNameReads(SuitEnum)
    implicit val v2: Writes[SuitEnum] = Writes.enumNameWrites
    Format(
      Reads(js => JsSuccess(js.as[Array[JsString]].map(_.as[SuitEnum]))),
      Writes(es => JsArray(es.map(t => Json.toJson(t)))))
  }
  val javaEnumArrayFormat: Format[Array[Suit]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(js => Suit.valueOf(js.value)))),
    Writes(es => JsArray(es.map(v => JsString(v.name)))))
  val charArrayFormat: Format[Array[Char]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsString]].map(_.value.charAt(0)))),
    Writes(es => JsArray(es.map(v => JsString(v.toString)))))
  val bigIntArrayFormat: Format[Array[BigInt]] = Format(
    Reads(js => JsSuccess(js.as[Array[JsNumber]]
      .map(js => js.value.toBigIntExact().getOrElse(throw new NumberFormatException("illegal BigInt value"))))),
    Writes(es => JsArray(es.map(v => JsNumber(BigDecimal(v))))))
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