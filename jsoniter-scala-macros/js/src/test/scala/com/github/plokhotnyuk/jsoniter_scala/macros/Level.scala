package com.github.plokhotnyuk.jsoniter_scala.macros

object Level {
  lazy val HIGH: Level = new Level("HIGH", 0)
  lazy val LOW: Level = new Level("LOW", 1)
  lazy val values: Array[Level] = Array(HIGH, LOW)
}

final class Level private (name: String, ordinal: Int) extends Enum[Level](name, ordinal)