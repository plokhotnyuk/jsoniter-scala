package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  case class Device(id: Int, model: String)

  case class User(name: String, devices: Seq[Device])

  implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make(CodecMakerConfig())

  def main(args: Array[String]): Unit = {
    val user = readFromArray("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""".getBytes("UTF-8"))
    val json = writeToArray(User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))

    println(user)
    println(new String(json, "UTF-8"))
  }
}
