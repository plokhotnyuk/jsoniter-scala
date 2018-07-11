package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined._

case class Device(id: Int Refined NonNegative, model: String Refined NonEmpty)

case class User(name: String, devices: Seq[Device])

object Examples extends JsoniterRefined {
  def main(args: Array[String]): Unit = {

    // comment out to see compile error
    implicit val codecForIntNonNegative: JsonCodec[Refined[Int, NonNegative]] = intCodec[Refined, NonNegative]
    implicit val codecForStringNonEmpty: JsonCodec[Refined[String, NonEmpty]] = stringCodec[Refined, NonEmpty]

    implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make[User](CodecMakerConfig())

    val user = readFromArray[User]("""{"name":"John","devices":[{"id":1,"model":"HTC One X"}]}""".getBytes("UTF-8"))
    val json = writeToArray(User(name = "John", devices = Seq(Device(id = refineMV(2), model = refineMV("iPhone X")))))

    println(user)
    println(new String(json, "UTF-8"))
  }
}
