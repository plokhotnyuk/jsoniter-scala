package com.github.plokhotnyuk.jsoniter_scala.benchmark

import smithy4s.Timestamp
import java.time.Instant

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = ???

  def toInstant(x: Timestamp): Instant = ???
}
