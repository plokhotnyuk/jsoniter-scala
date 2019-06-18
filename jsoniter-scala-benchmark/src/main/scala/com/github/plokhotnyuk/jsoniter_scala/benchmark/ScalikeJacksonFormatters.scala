package com.github.plokhotnyuk.jsoniter_scala.benchmark

import reug.scalikejackson.ScalaJacksonFormatter
import reug.scalikejackson.play.Json

object ScalikeJacksonFormatters {
  implicit val anyRefsFormatter: ScalaJacksonFormatter[AnyRefs] = Json.format[AnyRefs]()
}