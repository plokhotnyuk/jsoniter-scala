package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}

import scala.annotation.switch

/**
  * When json structure becomes tricky, you can actually use different codecs,
  * even in the codecs itself.
  */
object EncodedExample {

  case class RequestData(id: String, load: Int, important: Boolean)

  case class Request(url: String, data: RequestData)

  implicit lazy val requestCodec: JsonValueCodec[Request] = JsonCodecMaker.make[Request](CodecMakerConfig()) // LAZY is important

  lazy private val requestDataDirectCodec: JsonValueCodec[RequestData] = JsonCodecMaker.make[RequestData](CodecMakerConfig()) // not implicit!
  lazy private val requestDataEncodedCodec: JsonValueCodec[RequestData] = new JsonValueCodec[RequestData] { // not implicit!
    override def decodeValue(in: JsonReader, default: RequestData): RequestData = {
      val json = in.readString("")
      readFromString(json)(requestDataDirectCodec)
    }

    override def encodeValue(x: RequestData, out: JsonWriter): Unit = requestDataDirectCodec.encodeValue(x, out)

    override def nullValue: RequestData = requestDataDirectCodec.nullValue
  }

  implicit lazy private val requestDataCodec: JsonValueCodec[RequestData] = new JsonValueCodec[RequestData] { // implicit

    @scala.annotation.tailrec
    override def decodeValue(in: JsonReader, default: RequestData): RequestData = {
      (in.nextToken(): @switch) match {
        case ' ' =>
          this.decodeValue(in, default)
        case '{' =>
          in.rollbackToken()
          requestDataDirectCodec.decodeValue(in, default)
        case '"' =>
          in.rollbackToken()
          requestDataEncodedCodec.decodeValue(in, default)
        case _ =>
          in.decodeError("""expected '{' or '"' in request>data field""")
      }
    }

    override def encodeValue(x: RequestData, out: JsonWriter): Unit = requestDataDirectCodec.encodeValue(x, out)

    override def nullValue: RequestData = requestDataDirectCodec.nullValue
  }


  def main(args: Array[String]): Unit = {
    val normalJson =
      """{
        |  "url": "http://google.com",
        |  "data": {
        |  	"id": "abcdefghijklmn",
        |  	"load": 10,
        |  	"important": true
        |  }
        |}
      """.stripMargin

    val normal: Request = readFromString(normalJson)(requestCodec) // same codec!
    println(normal)
    println(writeToString(normal))

    val escapedJson =
      """{
        |  "url": "http://google.com",
        |  "data": "{\"id\":\"abcdefghijklmn\",\"load\":10,\"important\":true}"
        |}
      """.stripMargin
    val escaped: Request = readFromString(escapedJson)(requestCodec) // same codec!
    println(escaped)
    println(writeToString(escaped))
  }

}
