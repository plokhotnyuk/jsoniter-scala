package com.github.plokhotnyuk.jsoniter_scala.macros

object Levels {
  object InnerLevel {
    lazy val HIGH: InnerLevel = new InnerLevel("HIGH", 0)
    lazy val LOW: InnerLevel = new InnerLevel("LOW", 1)
    lazy val values: Array[InnerLevel] = Array(HIGH, LOW)
  }

  final class InnerLevel (name: String, ordinal: Int) extends Enum[InnerLevel](name, ordinal)
}