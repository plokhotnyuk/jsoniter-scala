package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  case class Device(id: Int, model: String)

  case class User(name: String, devices: Seq[Device])

  implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make(CodecMakerConfig)

  def main(args: Array[String]): Unit = {
    val user = readFromString("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""")
    val json = writeToString(User(name = "John", devices = Seq(Device(id = 2, model = "iPhone X"))))

    println(user)
    println(json)
  }
}
