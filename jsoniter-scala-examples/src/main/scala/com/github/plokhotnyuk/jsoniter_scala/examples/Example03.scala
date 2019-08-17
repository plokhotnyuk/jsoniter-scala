package com.github.plokhotnyuk.jsoniter_scala.examples

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.annotation.switch

/**
  * Example of decoding a field value that can be optionally encoded as JSON string
  */
object Example03 {
  case class RequestData(id: String, load: Int, important: Boolean)

  case class Request(url: String, data: RequestData)

  implicit val requestDataCodec: JsonValueCodec[RequestData] = new JsonValueCodec[RequestData] {
    val requestDataDirectCodec: JsonValueCodec[RequestData] = JsonCodecMaker.make(CodecMakerConfig())
    val requestDataEncodedCodec: JsonValueCodec[RequestData] = new JsonValueCodec[RequestData] {
      override def decodeValue(in: JsonReader, default: RequestData): RequestData = {
        val json = in.readString(null)
        readFromString(json)(requestDataDirectCodec)
      }

      override def encodeValue(x: RequestData, out: JsonWriter): Unit = requestDataDirectCodec.encodeValue(x, out)

      override val nullValue: RequestData = null
    }

    override def decodeValue(in: JsonReader, default: RequestData): RequestData = (in.nextToken(): @switch) match {
      case '{' =>
        in.rollbackToken()
        requestDataDirectCodec.decodeValue(in, default)
      case '"' =>
        in.rollbackToken()
        requestDataEncodedCodec.decodeValue(in, default)
      case _ =>
        in.decodeError("""expected '{' or '"'""")
    }

    override def encodeValue(x: RequestData, out: JsonWriter): Unit = requestDataDirectCodec.encodeValue(x, out)

    override val nullValue: RequestData = null
  }

  implicit val requestCodec: JsonValueCodec[Request] = JsonCodecMaker.make(CodecMakerConfig())

  def main(args: Array[String]): Unit = {
    val normalJson =
      """{
        |  "url": "http://google.com",
        |  "data": {
        |  	"id": "abcdefghijklmn",
        |  	"load": 10,
        |  	"important": true
        |  }
        |}""".stripMargin.getBytes("UTF-8")
    val normal = readFromArray[Request](normalJson)
    println(normal)
    println(new String(writeToArray(normal), "UTF-8"))
    println()
    val escapedJson =
      """{
        |  "url": "http://google.com",
        |  "data": "{\"id\":\"abcdefghijklmn\",\"load\":10,\"important\":true}"
        |}""".stripMargin.getBytes("UTF-8")
    val escaped = readFromArray[Request](escapedJson)
    println(escaped)
    println(new String(writeToArray(escaped), "UTF-8"))
  }
}
