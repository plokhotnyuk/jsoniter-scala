package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.immutable.HashMap

object CustomPlayJsonFormats {
  private val hashMapStrReads: Reads[HashMap[String, Double]] = Reads { js =>
    js.asOpt[Map[String, Double]]
      .fold[JsResult[HashMap[String, Double]]](JsError("Cannot parse Json as Map"))(m => JsSuccess(m.asInstanceOf[HashMap[String, Double]]))
  }

  private val hashMapStrWrites: OWrites[HashMap[String, Double]] = OWrites { v =>
    Json.toJson(v).as[JsObject]
  }

  implicit val hmStrFormat: OFormat[HashMap[String, Double]] = OFormat[HashMap[String, Double]](hashMapStrReads, hashMapStrWrites)

  private val mapReads: Reads[Map[Long, Double]] = Reads[Map[Long, Double]] {js =>
    JsSuccess(js.as[Map[String, Double]].map{case (k, v) => k.toLong -> v})
  }

  private val mapWrites: Writes[Map[Long, Double]] = new Writes[Map[Long, Double]] {
    def writes(map: Map[Long, Double]): JsValue = Json.toJson(map)
  }

  private implicit val mapFormat: Format[Map[Long, Double]] = Format(mapReads, mapWrites)

  private val hashMapLongReads: Reads[HashMap[Long, Double]] = Reads { js =>
    js.asOpt[Map[Long, Double]]
      .fold[JsResult[HashMap[Long, Double]]](JsError("Cannot parse Json as Map"))(m => JsSuccess(m.asInstanceOf[HashMap[Long, Double]]))
  }

  private val hashMapLongWrites: OWrites[HashMap[Long, Double]] = OWrites { v =>
    Json.toJson(v.asInstanceOf[Map[Long, Double]].map{case (k, value) => (k.toString, value)}).as[JsObject]
  }

  private implicit val hmLongFormat: OFormat[HashMap[Long, Double]] = OFormat[HashMap[Long, Double]](hashMapLongReads, hashMapLongWrites)

  private val hashMapIntReads: Reads[Map[Int, HashMap[Long, Double]]] = Reads { js =>
    JsSuccess(js.as[Map[String, HashMap[Long, Double]]].map{case (k, v) => k.toInt -> v})
  }

  private val hashMapIntWrites: OWrites[Map[Int, HashMap[Long, Double]]] = OWrites { v =>
    Json.toJson(v).as[JsObject]
  }

  implicit val hmIntFormat: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat[Map[Int, HashMap[Long, Double]]](hashMapIntReads, hashMapIntWrites)
}