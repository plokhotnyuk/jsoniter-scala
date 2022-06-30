package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time.Instant
import play.api.libs.json._
import smithy4s.Timestamp
import scala.util.Try

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp(x.getEpochSecond, x.getNano)

  def toInstant(x: Timestamp): Instant = Instant.ofEpochSecond(x.epochSecond, x.nano.toLong)

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = ???
}
