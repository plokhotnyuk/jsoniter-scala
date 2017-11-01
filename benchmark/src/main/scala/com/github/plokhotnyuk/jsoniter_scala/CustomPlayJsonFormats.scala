package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.breakOut
import scala.collection.immutable.{BitSet, HashMap, IntMap, LongMap}
import scala.collection.mutable

object CustomPlayJsonFormats {
  implicit val oFormat1: OFormat[HashMap[String, Double]] = OFormat(
    Reads[HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
    OWrites[HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m)))

  implicit val oFormat2: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat(
    Reads[Map[Int, HashMap[Long, Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
      (java.lang.Integer.parseInt(kv._1), kv._2.map { kv1 =>
        (java.lang.Long.parseLong(kv1._1), kv1._2)
      }(breakOut): HashMap[Long, Double])
    }(breakOut))),
    OWrites[Map[Int, HashMap[Long, Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
      (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2)))
    })))

  implicit val oFormat3: OFormat[mutable.HashMap[String, Double]] = OFormat(
    Reads[mutable.HashMap[String, Double]](js => JsSuccess(js.as[Map[String, Double]].map(identity)(breakOut))),
    OWrites[mutable.HashMap[String, Double]](m => Json.toJsObject[Map[String, Double]](m.toMap)))

  implicit val oFormat4: OFormat[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]] = OFormat(
    Reads[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
      val v = kv._2
      val newV = new mutable.OpenHashMap[Long, Double](v.size)
      v.foreach(kv1 => newV.update(java.lang.Long.parseLong(kv1._1), kv1._2))
      (java.lang.Integer.parseInt(kv._1), newV)
    }(breakOut): mutable.Map[Int, mutable.OpenHashMap[Long, Double]])),
    OWrites[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
      (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
    }(breakOut): Map[String, Map[String, Double]])))

  implicit val oFormat5: OFormat[IntMap[Double]] = OFormat(
    Reads[IntMap[Double]](js => JsSuccess(js.as[Map[String, Double]].map { kv =>
      (java.lang.Integer.parseInt(kv._1), kv._2)
    }(breakOut))),
    OWrites[IntMap[Double]](m => Json.toJsObject[Map[String, Double]](m.map { kv => (kv._1.toString, kv._2) })))

  implicit val oFormat6: OFormat[mutable.LongMap[LongMap[Double]]] = OFormat(
    Reads[mutable.LongMap[LongMap[Double]]](js => JsSuccess(js.as[Map[String, Map[String, Double]]].map { kv =>
      (java.lang.Long.parseLong(kv._1), kv._2.map { kv1 =>
        (java.lang.Long.parseLong(kv1._1), kv1._2)
      }(breakOut): LongMap[Double])
    }(breakOut))),
    OWrites[mutable.LongMap[LongMap[Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
      (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
    }(breakOut): Map[String, Map[String, Double]])))

  implicit val reads7: Reads[BitSet] =
    Reads[BitSet](js => JsSuccess(BitSet(js.as[Array[Int]]: _*)))

  implicit val reads8: Reads[mutable.BitSet] =
    Reads[mutable.BitSet](js => JsSuccess(mutable.BitSet(js.as[Array[Int]]: _*)))

  implicit val format9: Format[Char] = Format(
    Reads[Char](js => JsSuccess(js.as[String].charAt(0))),
    Writes[Char](c => JsString(c.toString)))

  implicit val format10: Format[Array[BigInt]] = Format(
    Reads[Array[BigInt]]{js => JsSuccess(js.as[Array[JsNumber]].map(_.value.toBigInt()))},
    Writes[Array[BigInt]](a => JsArray(a.map(v => JsNumber(BigDecimal(v))))))
}