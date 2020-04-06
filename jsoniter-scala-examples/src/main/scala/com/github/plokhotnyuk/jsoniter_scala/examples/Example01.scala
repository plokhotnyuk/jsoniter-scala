package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.pathikrit.dijon._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.language.dynamics._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  def main(args: Array[String]): Unit =
    scanJsonArrayFromStream(System.in) { (json: SomeJson) =>
      println(json.streamedRow.values(1))
      true
    } (codec)
}
