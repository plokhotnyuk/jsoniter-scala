package com.github.plokhotnyuk.jsoniter_scala.examples

/**
  * Example of basic usage from README.md
  */
object Example01 {
  import com.github.plokhotnyuk.jsoniter_scala.core._
  import com.github.plokhotnyuk.jsoniter_scala.macros._

  case class Foo(id: Int)
  implicit val foo: JsonValueCodec[Foo] = JsonCodecMaker.make

  def main(args: Array[String]): Unit = {
    val user = readFromArray("""{"id":1}""".getBytes("UTF-8"))
    val json = writeToArray(Foo(1))

    println(user)
    println(new String(json, "UTF-8"))
  }
}
