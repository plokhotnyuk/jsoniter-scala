package com.github.plokhotnyuk.jsoniter_scala.macros

object Level {
  val HIGH: Level = new Level("HIGH", 0)
  val LOW: Level = new Level("LOW", 1)

  val values: Array[Level] = Array(HIGH, LOW)

  def valueOf(name: String): Level =
    if (HIGH.name() == name) HIGH
    else if (LOW.name() == name) LOW
    else throw new IllegalArgumentException(s"Unrecognized Level name: $name")
}

final class Level private(name: String, ordinal: Int) extends Enum[Level](name, ordinal)