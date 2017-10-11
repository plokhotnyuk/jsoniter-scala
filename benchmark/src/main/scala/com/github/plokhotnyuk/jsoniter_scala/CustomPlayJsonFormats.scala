package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.breakOut
import scala.collection.immutable.HashMap
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
      (java.lang.Integer.parseInt(kv._1), mutable.OpenHashMap[Long, Double](kv._2.toSeq.map { kv1 => (java.lang.Long.parseLong(kv1._1), kv1._2)}: _*))
    }(breakOut): mutable.Map[Int, mutable.OpenHashMap[Long, Double]])),
    OWrites[mutable.Map[Int, mutable.OpenHashMap[Long, Double]]](m => Json.toJsObject[Map[String, Map[String, Double]]](m.map { kv =>
      (kv._1.toString, kv._2.map(kv1 => (kv1._1.toString, kv1._2))(breakOut): Map[String, Double])
    }(breakOut): Map[String, Map[String, Double]]))
  )
}