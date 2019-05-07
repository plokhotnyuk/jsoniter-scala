package io.circe

import java.util

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Json._

object CirceJsoniter {
  implicit val codec: JsonValueCodec[Json] = new JsonValueCodec[Json] {
    override def decodeValue(in: JsonReader, default: Json): Json = {
      var b = in.nextToken()
      if (b == 'n') in.readNullOrError(default, "expected `null` value")
      else if (b == '"') {
        in.rollbackToken()
        new JString(in.readString(null))
      } else if (b == 'f' || b == 't') {
        in.rollbackToken()
        if (in.readBoolean()) Json.True
        else Json.False
      } else if ((b >= '0' && b <= '9') || b == '-') {
        new JNumber({
          in.rollbackToken()
          in.setMark() // TODO: add in.readNumberAsString() to Core API of jsoniter-scala
          try {
            do b = in.nextByte()
            while (b >= '0' && b <= '9')
          } catch { case _: JsonReaderException => /* ignore end of input error */} finally in.rollbackToMark()
          if (b == '.' || b == 'e' || b == 'E') new JsonDouble(in.readDouble())
          else new JsonLong(in.readLong())
        })
      } else if (b == '[') {
        new JArray(if (in.isNextToken(']')) Vector.empty
        else {
          in.rollbackToken()
          var x = new Array[Json](4)
          var i = 0
          do {
            if (i == x.length) x = java.util.Arrays.copyOf(x, i << 1)
            x(i) = decodeValue(in, default)
            i += 1
          } while (in.isNextToken(','))
          (if (in.isCurrentToken(']'))
            if (i == x.length) x
            else java.util.Arrays.copyOf(x, i)
          else in.arrayEndOrCommaError()).to[Vector]
        })
      } else if (b == '{') {
        new JObject(if (in.isNextToken('}')) JsonObject.empty
        else {
          val x = new util.LinkedHashMap[String, Json]
          in.rollbackToken()
          do x.put(in.readKeyAsString(), decodeValue(in, default))
          while (in.isNextToken(','))
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          JsonObject.fromLinkedHashMap(x)
        })
      } else in.decodeError("expected JSON value")
    }

    override def encodeValue(x: Json, out: JsonWriter): Unit = x match {
      case JNull => out.writeNull()
      case JString(s) => out.writeVal(s)
      case JBoolean(b) => out.writeVal(b)
      case JNumber(n) => n match {
        case JsonLong(l) => out.writeVal(l)
        case _ => out.writeVal(n.toDouble)
      }
      case JArray(a) =>
        out.writeArrayStart()
        a.foreach(v => encodeValue(v, out))
        out.writeArrayEnd()
      case JObject(o) =>
        out.writeObjectStart()
        o.toIterable.foreach { case (k, v) =>
          out.writeKey(k)
          encodeValue(v, out)
        }
        out.writeObjectEnd()
    }

    override def nullValue: Json = Json.Null
  }
}