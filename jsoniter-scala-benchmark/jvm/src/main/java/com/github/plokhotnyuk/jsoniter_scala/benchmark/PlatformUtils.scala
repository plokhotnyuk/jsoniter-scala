package com.github.plokhotnyuk.jsoniter_scala.benchmark

import smithy4s.Timestamp

import java.time.Instant

object PlatformUtils {
  def toTimestamp(x: Instant): Timestamp = Timestamp.fromInstant(x)

  def toInstant(x: Timestamp): Instant = x.toInstant
}
