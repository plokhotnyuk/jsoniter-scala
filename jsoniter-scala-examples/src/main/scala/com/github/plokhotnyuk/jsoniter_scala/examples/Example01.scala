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
      def decodeValue(in: JsonReader, default: Leaf): Leaf = in.nextToken() match {
        case '"' =>
          in.rollbackToken()
          StringLeaf(in.readString(null))
        case _ =>
          in.rollbackToken()
          in.setMark()
          var b = 0
          try {
            do b = in.nextByte()
            while (b == '-' || b >= '0' && b <= '9')
          } catch {
            case _: JsonReaderException => /* ignore end of input error here */
          } finally in.rollbackToMark()
          if (b == '.' || b == 'e' || b == 'E') FloatLeaf(in.readFloat())
          else IntLeaf(in.readInt())
      }

    def encodeValue(x: Leaf, out: JsonWriter): Unit = x match {
      case IntLeaf(v) => out.writeVal(v)
      case StringLeaf(v) => out.writeVal(v)
      case FloatLeaf(v) => out.writeVal(v)
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
        |    "value": 0.0
        |},
        |{
        |    "name": "ID",
        |    "state": "STOP",
        |    "timestamp": 1587216049003,
        |    "value": "4105f527-69dc-4a4c-ab84-918b256c7dc0"
        |}
        |]""".stripMargin.getBytes("UTF-8"))

    println(elements)
    println(new String(writeToArray(elements), StandardCharsets.UTF_8))
  }
}
