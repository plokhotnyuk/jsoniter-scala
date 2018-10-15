package com.github.plokhotnyuk.jsoniter_scala.macros

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object SprayFormats extends DefaultJsonProtocol {
  implicit val extractFieldsJsonFormat: RootJsonFormat[ExtractFields] = jsonFormat2(ExtractFields)
}
