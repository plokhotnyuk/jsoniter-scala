package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

/**
  * Example showing how to handle enums or any data that has different representation in json than in your model
  */
object Example02 {
  sealed abstract class HttpProtocol private(val value: Int)

  case object HttpProtocol {
    val values: Seq[HttpProtocol] = Seq(Http, Https)

    case object Http extends HttpProtocol(0)

    case object Https extends HttpProtocol(1)
  }

  case class Request(url: String, protocol: HttpProtocol)

  implicit val protocolCodec: JsonValueCodec[HttpProtocol] = new JsonValueCodec[HttpProtocol] {
    override def decodeValue(in: JsonReader, default: HttpProtocol): HttpProtocol = {
      val i = in.readInt()
      HttpProtocol.values.find(_.value == i).getOrElse(in.decodeError("illegal protocol"))
    }

    override def encodeValue(x: HttpProtocol, out: JsonWriter): Unit = out.writeVal(x.value)

    override val nullValue: HttpProtocol = null
  }

  implicit val requestCodec: JsonValueCodec[Request] = JsonCodecMaker.make(CodecMakerConfig())

  def main(args: Array[String]): Unit = {
    val jsonHttp =
      """{
        |	"url": "http://google.com",
        |	"protocol": 0
        |}
        |""".stripMargin.getBytes("UTF-8")
    val http  = readFromArray[Request](jsonHttp)
    println(http)
    println(new String(writeToArray(http), "UTF-8"))
    println()
    val jsonHttps =
      """{
        |	"url": "http://google.com",
        |	"protocol": 1
        |}
        |""".stripMargin.getBytes("UTF-8")
    val https = readFromArray[Request](jsonHttps)
    println(https)
    println(new String(writeToArray(https), "UTF-8"))
  }
}
