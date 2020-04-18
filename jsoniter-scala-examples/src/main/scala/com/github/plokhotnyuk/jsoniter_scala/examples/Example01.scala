package com.github.plokhotnyuk.jsoniter_scala.examples

import java.nio.charset.StandardCharsets

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

/**
  * Example of basic usage from README.md
  */
object Example01 {
  sealed trait NameEnum
  final object TEMP extends NameEnum
  final object ID extends NameEnum

  sealed trait StateEnum
  final object STOP extends StateEnum
  final object NIGHT extends StateEnum

  sealed trait Leaf
  final case class IntLeaf(value: Int) extends Leaf
  final case class StringLeaf(value: String) extends Leaf
  final case class FloatLeaf(value: Float) extends Leaf

  final case class Elmnt(value: Leaf,
                         timestamp: Long,
                         name: NameEnum,
                         state: StateEnum,
                         unit: Option[String])

  implicit val leafCodec: JsonValueCodec[Leaf] = new JsonValueCodec[Leaf] {
      def nullValue: Leaf = null
      def decodeValue(in: JsonReader, default: Leaf): Leaf =
        if (in.isNextToken('{')) {
          var value = default
          var hasValue = false
          if (!in.isNextToken('}')) {
            in.rollbackToken()
            var l = -1
            while (l < 0  || in.isNextToken(',')) {
              l = in.readKeyAsCharBuf()
              if (in.isCharBufEqualsTo(l, "value")) {
                if (hasValue) in.duplicatedKeyError(l)
                hasValue = true
                value = in.nextToken() match {
                  case '"' =>
                    in.rollbackToken()
                    StringLeaf(in.readString(null))
                  case _ =>
                    in.rollbackToken()
                    val d = in.readDouble() // use in.readBigDecimal(null) for exact precision
                    val i = d.toInt
                    if (d == i) IntLeaf(i)
                    else FloatLeaf(d.toFloat) // possible 1 ULP rounding error here
                }
              } else in.skip()
            }
            if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          }
          if (!hasValue) in.requiredFieldError("value")
          value
        } else in.readNullOrTokenError(default, '{')

    def encodeValue(x: Leaf, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("value")
      x match {
        case IntLeaf(v) => out.writeVal(v)
        case StringLeaf(v) => out.writeVal(v)
        case FloatLeaf(v) => out.writeVal(v)
      }
      out.writeObjectEnd()
    }
  }

  implicit val elmntsCodec: JsonValueCodec[Seq[Elmnt]] = JsonCodecMaker.make(CodecMakerConfig.withDiscriminatorFieldName(None))

  def main(args: Array[String]): Unit = {
    val elements = readFromArray[Seq[Elmnt]](
      """[
        |{
        |    "name": "TEMP",
        |    "state": "NIGHT",
        |    "timestamp": 1587216049012,
        |    "value": {
        |        "value": 0.05
        |    }
        |},
        |{
        |    "name": "ID",
        |    "state": "STOP",
        |    "timestamp": 1587216049003,
        |    "value": {
        |        "value": "4105f527-69dc-4a4c-ab84-918b256c7dc0"
        |    }
        |}
        |]""".stripMargin.getBytes("UTF-8"))

    println(elements)
    println(new String(writeToArray(elements), StandardCharsets.UTF_8))
  }
}
