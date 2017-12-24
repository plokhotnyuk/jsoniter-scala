package com.github.plokhotnyuk.jsoniter_scala.macros

import julienrf.json.derived.flat
import play.api.libs.json._
import ai.x.play.json.Jsonx

import scala.collection.immutable.{BitSet, HashMap, IntMap, LongMap}
import scala.collection.{breakOut, mutable}

object PlayJsonFormats {
  val missingReqFieldFormat: OFormat[MissingReqFields] = Json.format[MissingReqFields]
  val anyRefsFormat: OFormat[AnyRefs] = Json.format[AnyRefs]
  val arraysFormat: OFormat[Arrays] = {
    implicit val v1: Format[Array[BigInt]] = Format(
      Reads[Array[BigInt]](js => JsSuccess(js.as[Array[JsNumber]].map(_.value.toBigInt()))),
      Writes[Array[BigInt]](a => JsArray(a.map(v => JsNumber(BigDecimal(v))))))
    Json.format[Arrays]
  }
  val bitSetsFormat: OFormat[BitSets] = {
    implicit val v1: Reads[BitSet] = Reads[BitSet](js => JsSuccess(BitSet(js.as[Array[Int]]: _*)))
    implicit val v2: Reads[mutable.BitSet] =
      Reads[mutable.BitSet](js => JsSuccess(mutable.BitSet(js.as[Array[Int]]: _*)))
    Json.format[BitSets]
  }
  val iterablesFormat: OFormat[Iterables] = Json.format[Iterables]
  val mutableIterablesFormat: OFormat[MutableIterables] = Json.format[MutableIterables]
  val mapsFormat: OFormat[Maps] = {
    implicit val v1: OFormat[HashMap[String, Double]] = OFormat(
      Reads[HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
      OWrites[HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m)))
    implicit val v2: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat(
      Reads[Map[Int, HashMap[Long, Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
        (java.lang.Integer.parseInt(kv._1), kv._2.map { kv1 =>
          (java.lang.Long.parseLong(kv1._1), kv1._2)
        }(breakOut): HashMap[Long, Double])
      }(breakOut))),
      OWrites[Map[Int, HashMap[Long, Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
        (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2)))
      })))
    Json.format[Maps]
  }
  val mutableMapsFormat: OFormat[MutableMaps] = {
    implicit val v1: OFormat[mutable.HashMap[String, Double]] = OFormat(
      Reads[mutable.HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
      OWrites[mutable.HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m.toMap)))
    implicit val v2: OFormat[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] = OFormat(
      Reads[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] { js =>
        JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
          val v = kv._2
          val newV = new mutable.OpenHashMap[Long, Double](v.size)
          v.foreach(kv1 => newV.update(java.lang.Long.parseLong(kv1._1), kv1._2))
          (java.lang.Integer.parseInt(kv._1), newV)
        }(breakOut): mutable.Map[Int, mutable.OpenHashMap[Long, Double]])
      },
      OWrites[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] { m =>
        Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
          (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
        }(breakOut): Map[String, Map[String, Double]])
      })
    Json.format[MutableMaps]
  }
  val intAndLongMapsFormat: OFormat[IntAndLongMaps] = {
    implicit val v1: OFormat[IntMap[Double]] = OFormat(
      Reads[IntMap[Double]](js => JsSuccess(js.as[Map[String, Double]].map { kv =>
        (java.lang.Integer.parseInt(kv._1), kv._2)
      }(breakOut))),
      OWrites[IntMap[Double]](m => Json.toJsObject[Map[String, Double]](m.map { kv => (kv._1.toString, kv._2) })))
    implicit val v2: OFormat[mutable.LongMap[LongMap[Double]]] = OFormat(
      Reads[mutable.LongMap[LongMap[Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
        (java.lang.Long.parseLong(kv._1), kv._2.map { kv1 =>
          (java.lang.Long.parseLong(kv1._1), kv1._2)
        }(breakOut): LongMap[Double])
      }(breakOut))),
      OWrites[mutable.LongMap[LongMap[Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
        (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
      }(breakOut): Map[String, Map[String, Double]])))
    Json.format[IntAndLongMaps]
  }
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
    implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
      override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

      override def writes(o: Option[T]): JsValue = o match {
        case Some(t) => implicitly[Writes[T]].writes(t)
        case None => JsNull
      }
    }
    implicit val v1: OFormat[Urls] = Json.format[Urls]
    implicit val v2: OFormat[Url] = Json.format[Url]
    implicit val v3: OFormat[UserEntities] = Json.format[UserEntities]
    implicit val v4: OFormat[UserMentions] = Json.format[UserMentions]
    implicit val v5: OFormat[Entities] = Json.format[Entities]
    implicit val v6: Format[User] = Jsonx.formatCaseClass[User]
    implicit val v7: Format[RetweetedStatus] = Jsonx.formatCaseClass[RetweetedStatus]
    implicit val v8: Format[Tweet] = Jsonx.formatCaseClass[Tweet]
    Format[Seq[Tweet]](
      Reads[Seq[Tweet]](js => JsSuccess(js.as[Seq[JsObject]].map(_.as[Tweet](v8)))),
      Writes[Seq[Tweet]](ts => JsArray(ts.map(t => Json.toJson(t)(v8)))))
  }
}