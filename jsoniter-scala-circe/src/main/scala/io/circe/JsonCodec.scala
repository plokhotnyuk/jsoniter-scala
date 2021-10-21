package io.circe

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonReaderException, JsonValueCodec, JsonWriter}
import io.circe.Json._
import java.util

object JsonCodec {
  val defaultNumberParser: JsonReader => Json = in => {
    in.setMark()
    var b: Byte = 0
    try {
      while ({
        b = in.nextByte()
        b >= '0' && b <= '9' || b == '-'
      }) ()
    } catch {
      case _: JsonReaderException => // ignore the end of input error for now
    } finally in.rollbackToMark()
    new JNumber({
      if (b == '.' || b == 'e' || b == 'E') new JsonBigDecimal(in.readBigDecimal(null).bigDecimal)
      else new JsonLong(in.readLong())
    })
  }

  def jsonCodec(
    maxDepth: Int,
    initialSize: Int,
    doSerialize: Json => Boolean,
    numberParser: JsonReader => Json): JsonValueCodec[Json] = new JsonValueCodec[Json] {
    override def decodeValue(in: JsonReader, default: Json): Json = decode(in, maxDepth)

    override def encodeValue(x: Json, out: JsonWriter): Unit = encode(x, out, maxDepth)

    override val nullValue: Json = Null

    private[this] def decode(in: JsonReader, depth: Int): Json = {
      val b = in.nextToken()
      if (b == 'n') in.readNullOrError(Null, "expected `null` value")
      else if (b == '"') new JString({
        in.rollbackToken()
        in.readString(null)
      }) else if (b == 'f' || b == 't') {
        in.rollbackToken()
        if (in.readBoolean()) True
        else False
      } else if (b >= '0' && b <= '9' || b == '-') {
        in.rollbackToken()
        numberParser(in)
      } else if (b == '[') new JArray({
        val depthM1 = depth - 1
        if (depthM1 < 0) in.decodeError("depth limit exceeded")
        if (in.isNextToken(']')) Vector.empty
        else {
          in.rollbackToken()
          var x = new Array[Json](initialSize)
          var i = 0
          while ({
            if (i == x.length) x = java.util.Arrays.copyOf(x, i << 1)
            x(i) = decode(in, depthM1)
            i += 1
            in.isNextToken(',')
          }) ()
          (if (in.isCurrentToken(']'))
            if (i == x.length) x
            else java.util.Arrays.copyOf(x, i)
          else in.arrayEndOrCommaError()).toVector
        }
      }) else if (b == '{') new JObject({
        val depthM1 = depth - 1
        if (depthM1 < 0) in.decodeError("depth limit exceeded")
        if (in.isNextToken('}')) JsonObject.empty
        else {
          in.rollbackToken()
          val x = new util.LinkedHashMap[String, Json](8)
          while ({
            x.put(in.readKeyAsString(), decode(in, depthM1))
            in.isNextToken(',')
          }) ()
          if (!in.isCurrentToken('}')) in.objectEndOrCommaError()
          JsonObject.fromLinkedHashMap(x)
        }
      }) else in.decodeError("expected JSON value")
    }

    private[this] def encode(x: Json, out: JsonWriter, depth: Int): Unit = x match {
      case JNull => out.writeNull()
      case s: JString => out.writeVal(s.value)
      case b: JBoolean => out.writeVal(b.value)
      case n: JNumber => encodeJsonNumber(n.value, out)
      case a: JArray =>
        val depthM1 = depth - 1
        if (depthM1 < 0) out.encodeError("depth limit exceeded")
        out.writeArrayStart()
        a.value.foreach(x => encode(x, out, depthM1))
        out.writeArrayEnd()
      case o: JObject =>
        val depthM1 = depth - 1
        if (depthM1 < 0) out.encodeError("depth limit exceeded")
        out.writeObjectStart()
        o.value.toIterable.foreach { kv =>
          if (doSerialize(kv._2)) {
            out.writeKey(kv._1)
            encode(kv._2, out, depthM1)
          }
        }
        out.writeObjectEnd()
    }

    private[this] def encodeJsonNumber(x: JsonNumber, out: JsonWriter): Unit = x match {
      case l: JsonLong => out.writeVal(l.value)
      case f: JsonFloat => out.writeVal(f.value)
      case d: JsonDouble => out.writeVal(d.value)
      case bd: JsonBigDecimal => out.writeVal(bd.value)
      case _ => out.writeRawVal(x.toString.getBytes)
    }
  }
}

