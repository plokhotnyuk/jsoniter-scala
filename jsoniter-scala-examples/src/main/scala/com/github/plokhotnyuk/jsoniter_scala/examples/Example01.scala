package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  implicit val seqCodec: JsonValueCodec[List[Int]] =
    JsonCodecMaker.make(CodecMakerConfig)

  def main(args: Array[String]): Unit =
    Console.out.write(writeToArray(List(1, 2, 3, 4, 5)))
}
