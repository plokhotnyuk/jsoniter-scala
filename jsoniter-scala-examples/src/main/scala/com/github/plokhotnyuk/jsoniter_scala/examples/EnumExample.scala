package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonValueCodec, JsonWriter, _}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

/**
  * Example showing how to handle enums or
  * any data that has different representation in json than in your model.
  */
object EnumExample {

  sealed abstract class HttpProtocol private(val value: Int)

  case object HttpProtocol {
    val values = Seq(Http, Https) // I normally use Enumeratum

    case object Http extends HttpProtocol(0)

    case object Https extends HttpProtocol(1)

  }

  case class Request(url: String, protocol: HttpProtocol)


  implicit lazy val requestCodec: JsonValueCodec[Request] = JsonCodecMaker.make[Request](CodecMakerConfig()) // LAZY is important

  implicit lazy val protocolCodec: JsonValueCodec[HttpProtocol] = new JsonValueCodec[HttpProtocol] {
    override def decodeValue(in: JsonReader, default: HttpProtocol): HttpProtocol = {
      val i = in.readInt()
      HttpProtocol.values.find(_.value == i).getOrElse(nullValue) // exception handling?
    }

    override def encodeValue(x: HttpProtocol, out: JsonWriter): Unit = out.writeVal(x.value)

    override def nullValue: HttpProtocol = ??? // this should never be used
  }


  def main(args: Array[String]): Unit = {
    val jsonHttp =
      """{
        |	"url": "http://google.com",
        |	"protocol": 0
        |}
        |""".stripMargin

    val http: Request = readFromString(jsonHttp)(requestCodec)
    println(http)
    println(writeToString(http))


    val jsonHttps =
      """{
        |	"url": "http://google.com",
        |	"protocol": 1
        |}
        |""".stripMargin

    val https: Request = readFromString(jsonHttps)(requestCodec)
    println(https)
    println(writeToString(https))
  }

}
