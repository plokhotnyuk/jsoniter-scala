package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  def main(args: Array[String]): Unit = {
    case class Category(id: Int, name: String)

    case class Categories(categories: Seq[Category])

    implicit val codec: JsonValueCodec[Categories] = JsonCodecMaker.make

    val cats = readFromStream(System.in)

    cats.categories.foreach(c => println(c.productIterator.mkString(", ")))
  }
}
