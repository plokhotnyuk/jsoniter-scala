package com.github.plokhotnyuk.jsoniter_scala.benchmark

import smithy4s.Timestamp
import java.time.Instant

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp(x.getEpochSecond, x.getNano)

  def toInstant(x: Timestamp): Instant = Instant.ofEpochSecond(x.epochSecond, x.nano.toLong)
}
