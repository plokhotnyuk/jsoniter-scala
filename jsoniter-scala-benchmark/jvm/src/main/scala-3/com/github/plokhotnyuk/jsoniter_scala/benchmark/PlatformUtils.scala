package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.time.Instant
import smithy4s.Timestamp

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp.fromInstant(x)

  def toInstant(x: Timestamp): Instant = x.toInstant
}
