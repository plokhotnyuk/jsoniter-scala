package com.github.plokhotnyuk.jsoniter_scala

import play.api.libs.json._

import scala.collection.immutable.HashMap

object CustomPlayJsonFormats {

  implicit val hmStrFormat: OFormat[HashMap[String, Double]] = OFormat[HashMap[String, Double]] (
    Reads { js =>
      js.asOpt[Map[String, Double]]
        .fold[JsResult[HashMap[String, Double]]](JsError(s"Cannot parse Json as Map $js"))(m =>
        JsSuccess(HashMap(m.toSeq: _*))
      )
    },
    OWrites[HashMap[String, Double]] { v =>
      Json.toJsObject(Map(v.toSeq: _*))
    }
  )

  implicit val mapLongFormat: OFormat[Map[Long, Double]] = OFormat(
    Reads[Map[Long, Double]] {js =>
      JsSuccess(js.as[Map[String, Double]].map{case (k, v) => k.toLong -> v})
    },
    OWrites[Map[Long, Double]] {map =>
      Json.toJsObject(map.map{case (k, value) => (k.toString, value)})
    }
  )

  implicit val hmLongFormat: OFormat[HashMap[Long, Double]] = OFormat(
    Reads { js =>
      js.asOpt[Map[Long, Double]]
        .fold[JsResult[HashMap[Long, Double]]](JsError(s"Cannot parse Json as Map: $js"))(m =>
        JsSuccess(HashMap(m.toSeq: _*))
      )
    },
    OWrites[HashMap[Long, Double]] { v =>
      Json.toJsObject(v.map{case (k, value) => (k.toString, value)})
    }
  )

  implicit val hmIntFormat: OFormat[Map[Int, HashMap[Long, Double]]] = OFormat[Map[Int, HashMap[Long, Double]]](
    Reads { js =>
      JsSuccess(js.as[Map[String, HashMap[Long, Double]]].map{case (k, v) => k.toInt -> v})
    },
    OWrites[Map[Int, HashMap[Long, Double]]] { v =>
      Json.toJsObject(v.map{case (k, value) => k.toString -> Map(value.toSeq: _*)})
    }
  )
}