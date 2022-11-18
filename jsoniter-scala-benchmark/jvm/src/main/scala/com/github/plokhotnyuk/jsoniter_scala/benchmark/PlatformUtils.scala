package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import java.time.Instant
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule
import smithy4s.Timestamp

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp.fromInstant(x)

  def toInstant(x: Timestamp): Instant = x.toInstant

  private[this] val prettyPrintMapper = new ObjectMapper {
    registerModule(new PlayJsonModule(JsonParserSettings.settings))
    configure(SerializationFeature.INDENT_OUTPUT, true)
    setDefaultPrettyPrinter {
      val indenter = new DefaultIndenter("  ", "\n")
      new DefaultPrettyPrinter().withObjectIndenter(indenter).withArrayIndenter(indenter)
    }
  }

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = prettyPrintMapper.writeValueAsBytes(jsValue)
}
