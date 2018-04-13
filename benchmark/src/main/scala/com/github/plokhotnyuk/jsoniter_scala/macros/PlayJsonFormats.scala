package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time.{OffsetTime, ZoneOffset}

import julienrf.json.derived.flat
import play.api.libs.json._
import ai.x.play.json.Jsonx
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.{BitSet, IntMap, Map}
import scala.collection.{breakOut, mutable}

object PlayJsonFormats {
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format[MissingReqFields]
  val anyRefsFormat: OFormat[AnyRefs] = Json.format[AnyRefs]
  val bitSetFormat: Format[BitSet] = Format(
    Reads[BitSet](js => JsSuccess(BitSet(js.as[Array[Int]]: _*))),
    Writes[BitSet]((es: BitSet) => JsArray(es.map(v => JsNumber(BigDecimal(v)))(breakOut))))
  val mutableBitSetFormat: Format[mutable.BitSet] = Format(
    Reads[mutable.BitSet](js => JsSuccess(mutable.BitSet(js.as[Array[Int]]: _*))),
    Writes[mutable.BitSet]((es: mutable.BitSet) => JsArray(es.map(v => JsNumber(BigDecimal(v)))(breakOut))))
  val intMapOfBooleansFormat: OFormat[IntMap[Boolean]] = OFormat(
    Reads[IntMap[Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toInt, e._2))(breakOut))),
    OWrites[IntMap[Boolean]](m => Json.toJsObject[mutable.LinkedHashMap[String, Boolean]](m.map(e => (e._1.toString, e._2))(breakOut))))
  val mapOfIntsToBooleansFormat: OFormat[Map[Int, Boolean]] = OFormat(
    Reads[Map[Int, Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toInt, e._2)))),
    OWrites[Map[Int, Boolean]](m => Json.toJsObject[mutable.LinkedHashMap[String, Boolean]](m.map(e => (e._1.toString, e._2))(breakOut))))
  val mutableLongMapOfBooleansFormat: OFormat[mutable.LongMap[Boolean]] = OFormat(
    Reads[mutable.LongMap[Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toLong, e._2))(breakOut))),
    OWrites[mutable.LongMap[Boolean]](m => Json.toJsObject[mutable.LinkedHashMap[String, Boolean]](m.map(e => (e._1.toString, e._2))(breakOut))))
  val mutableMapOfIntsToBooleansFormat: OFormat[mutable.Map[Int, Boolean]] = OFormat(
    Reads[mutable.Map[Int, Boolean]](js => JsSuccess(js.as[Map[String, Boolean]].map(e => (e._1.toInt, e._2))(breakOut))),
    OWrites[mutable.Map[Int, Boolean]](m => Json.toJsObject[mutable.LinkedHashMap[String, Boolean]](m.map(e => (e._1.toString, e._2))(breakOut))))
  val primitivesFormat: OFormat[Primitives] = {
    implicit val v1: Format[Char] = Format(
      Reads[Char](js => JsSuccess(js.as[String].charAt(0))),
      Writes[Char](c => JsString(c.toString)))
    Json.format[Primitives]
  }
  val extractFieldsFormat: OFormat[ExtractFields] = Json.format[ExtractFields]
  val adtFormat: OFormat[AdtBase] = {
    implicit lazy val v1: OFormat[A] = Json.format[A]
    implicit lazy val v2: OFormat[B] = Json.format[B]
    implicit lazy val v3: OFormat[C] = Json.format[C]
    implicit lazy val v4: OFormat[AdtBase] = flat.oformat((__ \ "type").format[String])
    v4
  }
  val googleMapsAPIFormat: OFormat[DistanceMatrix] = {
    implicit val v1: OFormat[Value] = Json.format[Value]
    implicit val v2: OFormat[Elements] = Json.format[Elements]
    implicit val v3: OFormat[Rows] = Json.format[Rows]
    Json.format[DistanceMatrix]
  }
  val twitterAPIFormat: Format[Seq[Tweet]] = {
    implicit val v1: OFormat[Urls] = Json.format[Urls]
    implicit val v2: OFormat[Url] = Json.format[Url]
    implicit val v3: OFormat[UserEntities] = Json.format[UserEntities]
    implicit val v4: OFormat[UserMentions] = Json.format[UserMentions]
    implicit val v5: OFormat[Entities] = Json.format[Entities]
    implicit val v6: Format[User] = Jsonx.formatCaseClass[User]
    implicit val v7: Format[RetweetedStatus] = Jsonx.formatCaseClass[RetweetedStatus]
    implicit val v8: Format[Tweet] = Jsonx.formatCaseClass[Tweet]
    Format[Seq[Tweet]](
      Reads[Seq[Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[Tweet]))),
      Writes[Seq[Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)))))
  }
  val enumArrayFormat: Format[Array[SuitEnum]] = {
    implicit val v1: Reads[SuitEnum] = Reads.enumNameReads(SuitEnum)
    implicit val v2: Writes[SuitEnum] = Writes.enumNameWrites
    Format(
      Reads[Array[SuitEnum]](js => JsSuccess(js.as[Array[JsString]].map(_.as[SuitEnum]))),
      Writes[Array[SuitEnum]](es => JsArray(es.map(t => Json.toJson(t)))))
  }
  val javaEnumArrayFormat: Format[Array[Suit]] = Format(
    Reads[Array[Suit]](js => JsSuccess(js.as[Array[JsString]].map(js => Suit.valueOf(js.value)))),
    Writes[Array[Suit]](es => JsArray(es.map(v => JsString(v.name)))))
  val charArrayFormat: Format[Array[Char]] = Format(
    Reads[Array[Char]](js => JsSuccess(js.as[Array[JsString]].map(_.value.charAt(0)))),
    Writes[Array[Char]](es => JsArray(es.map(v => JsString(v.toString)))))
  val bigIntArrayFormat: Format[Array[BigInt]] = Format(
    Reads[Array[BigInt]](js => JsSuccess(js.as[Array[JsNumber]]
      .map(js => js.value.toBigIntExact().getOrElse(throw new NumberFormatException("illegal BigInt value"))))),
    Writes[Array[BigInt]](es => JsArray(es.map(v => JsNumber(BigDecimal(v))))))
  val offsetTimeArrayFormat: Format[Array[OffsetTime]] = Format(
    Reads[Array[OffsetTime]](js => JsSuccess(js.as[Array[JsString]].map(js => OffsetTime.parse(js.value)))),
    Writes[Array[OffsetTime]](es => JsArray(es.map(v => JsString(v.toString)))))
  val zoneOffsetArrayFormat: Format[Array[ZoneOffset]] = Format(
    Reads[Array[ZoneOffset]](js => JsSuccess(js.as[Array[JsString]].map(js => ZoneOffset.of(js.value)))),
    Writes[Array[ZoneOffset]](es => JsArray(es.map(v => JsString(v.toString)))))
}