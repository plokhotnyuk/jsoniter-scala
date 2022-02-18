package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import java.util.{LinkedHashMap, Objects, UUID}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker._
import org.scalatest.exceptions.TestFailedException
import scala.annotation.switch
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.hashing.MurmurHash3

opaque type Year = Int

object Year {
  def apply(value: Int): Year = value

  def safe(value: Int): Option[Year] = if (value > 1900) Some(value) else None

  extension (year: Year) {
    def value: Int = year
  }
}

class JsonCodecMakerNewTypeSpec extends VerifyingSpec {
  "JsonCodecMaker.make generate codecs which" should {
    "serialize and deserialize Scala3 opaque types" in {
      case class Period(start: Year, end: Year)

      implicit val yearCodec: JsonValueCodec[Year] = new JsonValueCodec[Year] {
        def decodeValue(in: JsonReader, default: Year): Year = Year(in.readInt())

        def encodeValue(x: Year, out: JsonWriter): Unit = out.writeVal(x.value)

        val nullValue: Year = null.asInstanceOf[Year]
      }
      verifySerDeser(make[Period], Period(Year(1976), Year(2022)), """{"start":1976,"end":2022}""")
    }
    "serialize and deserialize Scala3 union types" in {
      type JsonPrimitive = String | Int | Double | Boolean | None.type

      type Rec[JA[_], JO[_], A] = A match { // FIXME: remove this workaround after adding support of recursive types
        case JsonPrimitive => JsonPrimitive | JA[Rec[JA, JO, JsonPrimitive]] | JO[Rec[JA, JO, JsonPrimitive]]
        case _ => A | JA[Rec[JA, JO, A]] | JO[Rec[JA, JO, A]]
      }

      type Json = Rec[[A] =>> mutable.Buffer[A], [A] =>> mutable.Map[String, A], JsonPrimitive]

      type JsonObject = mutable.Map[String, Json]

      type JsonArray = mutable.Buffer[Json]

      val jsonCodec: JsonValueCodec[Json] = new JsonValueCodec[Json] {
        def decodeValue(in: JsonReader, default: Json): Json = decode(in, 128)

        def encodeValue(x: Json, out: JsonWriter): Unit = encode(x, out, 128)

        val nullValue: Json = None

        private[this] def decode(in: JsonReader, depth: Int): Json =
          val b = in.nextToken()
          if (b == '"') {
            in.rollbackToken()
            in.readString(null)
          } else if (b == 't' || b == 'f') {
            in.rollbackToken()
            in.readBoolean()
          } else if ((b >= '0' && b <= '9') || b == '-') {
            in.rollbackToken()
            val d = in.readDouble()
            val i = d.toInt
            if (i.toDouble == d) i
            else d
          } else if (b == '[') {
            if (depth <= 0) in.decodeError("depth limit exceeded")
            val arr = new mutable.ArrayBuffer[Json](8)
            if (!in.isNextToken(']')) {
              in.rollbackToken()
              val dp = depth - 1
              while ({
                arr += decode(in, dp)
                in.isNextToken(',')
              }) ()
              if (!in.isCurrentToken(']')) in.arrayEndOrCommaError()
            }
            arr
          } else if (b == '{') {
            if (depth <= 0) in.decodeError("depth limit exceeded")
            val obj = new LinkedHashMap[String, Json](8)
            if (!in.isNextToken('}')) {
              in.rollbackToken()
              val dp = depth - 1
              while ({
                obj.put(in.readKeyAsString(), decode(in, dp))
                in.isNextToken(',')
              }) ()
              if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
            }
            obj.asScala
          } else in.readNullOrError(None, "expected JSON value")

        private[this] def encode(x: Json, out: JsonWriter, depth: Int): Unit = x match
          case str: String => out.writeVal(str)
          case b: Boolean => out.writeVal(b)
          case i: Int => out.writeVal(i)
          case d: Double => out.writeVal(d)
          case arr: JsonArray =>
            if (depth <= 0) out.encodeError("depth limit exceeded")
            out.writeArrayStart()
            val dp = depth - 1
            val l = arr.size
            var i = 0
            while (i < l) {
              encode(arr(i), out, dp)
              i += 1
            }
            out.writeArrayEnd()
          case obj: JsonObject =>
            if (depth <= 0) out.encodeError("depth limit exceeded")
            out.writeObjectStart()
            val dp = depth - 1
            val it = obj.iterator
            while (it.hasNext) {
              val kv = it.next()
              out.writeKey(kv._1)
              encode(kv._2, out, dp)
            }
            out.writeObjectEnd()
          case _ => out.writeNull()
      }

      def obj(values: (String, Json)*): Json =
        val len = values.length
        val map = new LinkedHashMap[String, Json](len)
        var i = 0
        while (i < len) {
          val kv = values(i)
          map.put(kv._1, kv._2)
          i += 1
        }
        map.asScala

      def arr(values: Json*): Json = mutable.ArrayBuffer[Json](values: _*)

      verifySerDeser(jsonCodec, arr("VVV", 1.2, true, obj("WWW" -> None, "XXX" -> 777)),
        """["VVV",1.2,true,{"WWW":null,"XXX":777}]""")
    }
  }
}