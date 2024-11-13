package com.github.plokhotnyuk.jsoniter_scala.benchmark

import play.api.libs.json.{JsValue, StaticBinding}
import smithy4s.Timestamp
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp(x.getEpochSecond, x.getNano)

  def toInstant(x: Timestamp): Instant = Instant.ofEpochSecond(x.epochSecond, x.nano.toLong)

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = StaticBinding.prettyPrint(jsValue).getBytes(UTF_8)
}
