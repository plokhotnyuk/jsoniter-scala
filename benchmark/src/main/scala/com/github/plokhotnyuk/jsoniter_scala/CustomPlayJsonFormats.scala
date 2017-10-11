package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.breakOut
import scala.collection.immutable.HashMap

object CustomPlayJsonFormats {
  implicit val oFormat1: OFormat[HashMap[String, Double]] = OFormat(
    Reads(js => JsSuccess[HashMap[String, Double]](js.as[Map[String, Double]].map(identity)(breakOut))),
    OWrites[HashMap[String, Double]](m => Json.toJsObject(m.toMap)))

  implicit val oFormat2: OFormat[Map[Long, Double]] = OFormat(
    Reads(js => JsSuccess(js.as[Map[String, Double]].map { case (k, v) => (java.lang.Long.parseLong(k), v) })),
    OWrites[Map[Long, Double]](m => Json.toJsObject(m.map { case (k, v) => (k.toString, v) })))

  implicit val oFormat3: OFormat[HashMap[Long, Double]] = OFormat(
    Reads(js => JsSuccess[HashMap[Long, Double]](js.as[Map[Long, Double]].map(identity)(breakOut))),
    OWrites[HashMap[Long, Double]](m => Json.toJsObject(m.map { case (k, v) => (k.toString, v) })))

  implicit val oFormat4: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat(
    Reads(js => JsSuccess(js.as[Map[String, HashMap[Long, Double]]].map { case (k, v) => (java.lang.Integer.parseInt(k), v) })),
    OWrites[Map[Int, HashMap[Long, Double]]](m => Json.toJsObject(m.map { case (k, v) => (k.toString, v.toMap) })))
}