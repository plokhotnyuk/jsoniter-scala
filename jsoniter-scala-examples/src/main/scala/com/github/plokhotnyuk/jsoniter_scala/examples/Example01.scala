package com.github.plokhotnyuk.jsoniter_scala.examples

object Example01 {
  def main(args: Array[String]): Unit = {
    import com.github.plokhotnyuk.jsoniter_scala.macros._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import java.nio.ByteBuffer

    case class TushareResponse(
                                request_id: String,
                                code: Int,
                                msg: String,
                                data: Data)

    object TushareResponse {
      implicit val codec: JsonValueCodec[TushareResponse] = JsonCodecMaker.make
    }

    case class Data(
                     fields: Array[String],
                     items: Array[Array[Option[String]]],
                     has_more: Boolean)

    object Data {
      implicit val codec: JsonValueCodec[Data] = {
        implicit val optStringCodec: JsonValueCodec[Option[String]] = new JsonValueCodec[Option[String]] {
          override def decodeValue(in: JsonReader, default: Option[String]): Option[String] = {
            val b = in.nextToken()
            if (b == 'n') {
              in.readNullOrError(None, "expected `null` value")
              None
            } else if (b == '"') {
              in.rollbackToken()
              Some(in.readString(null))
            } else if (b == 't' || b == 'f') {
              in.rollbackToken()
              Some(in.readBoolean().toString)
            } else if ((b >= '0' && b <= '9') || b == '-') {
              in.rollbackToken()
              Some(in.readBigDecimal(null).toString)
            } else in.decodeError("unexpected value")
          }

          override def encodeValue(x: Option[String], out: JsonWriter): Unit =
            x.fold(out.writeNull())(out.writeVal)

          override def nullValue: Option[String] = None
        }
        JsonCodecMaker.make
      }
    }

    val response = readFromByteBuffer[TushareResponse](ByteBuffer.wrap(
    """{
       |    "request_id": "",
       |    "code": 0,
       |    "msg": "",
       |    "data": {
       |        "fields": [
       |            "exchange",
       |            "cal_date",
       |            "is_open",
       |            "pretrade_date"
       |        ],
       |        "items": [
       |            [
       |                "SSE",
       |                "19901219",
       |                1,
       |                null
       |            ],
       |            [
       |                "SSE",
       |                "19941222",
       |                1,
       |                "19941221"
       |            ],
       |            [
       |                "SSE",
       |                "19941223",
       |                1,
       |                "19941222"
       |            ]
       |        ],
       |        "has_more": false
       |    }
       |}""".stripMargin.getBytes("UTF-8")))

    println(writeToString(response, WriterConfig.withIndentionStep(4)))
  }
}
