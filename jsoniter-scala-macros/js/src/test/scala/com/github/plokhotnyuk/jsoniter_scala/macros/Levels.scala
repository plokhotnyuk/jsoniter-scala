package com.github.plokhotnyuk.jsoniter_scala.macros

object Levels {
  object InnerLevel {
    val HIGH: InnerLevel = new InnerLevel("HIGH", 0)
    val LOW: InnerLevel = new InnerLevel("LOW", 1)

    val values: Array[InnerLevel] = Array(HIGH, LOW)

    def valueOf(name: String): InnerLevel =
      if (HIGH.name() == name) HIGH
      else if (LOW.name() == name) LOW
      else throw new IllegalArgumentException(s"Unrecognized InnerLevel name: $name")
  }

  final class InnerLevel private (name: String, ordinal: Int) extends Enum[InnerLevel](name, ordinal)
}