package com.github.plokhotnyuk.jsoniter_scala.examples

import java.nio.charset.StandardCharsets
import java.time.LocalDate

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  case class Input(nextDate: LocalDate, days: Int)

  implicit val inputCodec: JsonValueCodec[Input] =
    new JsonValueCodec[Input] {
      def nullValue: Input = null
      def decodeValue(in: JsonReader, default: Input): Input =
        if (in.isNextToken('{')) {
          var _nextDate: LocalDate = null
          var _days: Int = 0
          var p0 = 3
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            var l = -1
            while (l < 0 || in.isNextToken(',')) {
              l = in.readKeyAsCharBuf()
              if (in.isCharBufEqualsTo(l, "nextDate") || in.isCharBufEqualsTo(l, "next_date")) {
                if ((p0 & 1) != 0) p0 ^= 1
                else in.duplicatedKeyError(l)
                _nextDate = in.readLocalDate(_nextDate)
              } else if (in.isCharBufEqualsTo(l, "days")) {
                if ((p0 & 2) != 0) p0 ^= 2
                else in.duplicatedKeyError(l)
                _days = in.readInt()
              } else in.skip()
            }
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
          if ((p0 & 3) != 0) in.requiredFieldError(f0(Integer.numberOfTrailingZeros(p0 & 3)))
          Input(nextDate = _nextDate, days = _days)
        } else in.readNullOrTokenError(default, '{')

      def encodeValue(x: Input, out: JsonWriter): _root_.scala.Unit = {
        out.writeObjectStart()
        out.writeNonEscapedAsciiKey("nextDate")
        out.writeVal(x.nextDate)
        out.writeNonEscapedAsciiKey("days")
        out.writeVal(x.days)
        out.writeObjectEnd()
      }

      private[this] def f0(i: Int): String = i match {
        case 0 => "nextDate"
        case 1 => "days"
      }
    }

  implicit val inputsCodec: JsonValueCodec[Seq[Input]] = JsonCodecMaker.make[Seq[Input]](CodecMakerConfig)

  def main(args: Array[String]): Unit = {
    val inputs = readFromArray[Seq[Input]](
      """[
        |    {
        |        "next_date": "2019-10-10",
        |        "days": 2
        |    },
        |    {
        |        "nextDate": "2019-10-10",
        |        "days": 2
        |    }
        |]""".stripMargin.getBytes("UTF-8"))

    println(inputs)
    println(new String(writeToArray(inputs), StandardCharsets.UTF_8))
  }
}
