package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.breakOut
import scala.collection.immutable.HashMap

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
}