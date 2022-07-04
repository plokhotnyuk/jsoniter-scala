package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time.Instant
import play.api.libs.json._
import smithy4s.Timestamp
import scala.util.Try

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = ???

  def toInstant(x: Timestamp): Instant = ???

  def prettyPrintBytes(jsValue: JsValue): Array[Byte] = ???
}
