package io.circe

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReader, JsonReaderException, JsonValueCodec, JsonWriter}
import io.circe.Json._
import java.nio.charset.StandardCharsets
import java.util
import scala.collection.immutable.VectorBuilder

object JsoniterScalaCodec {
  val defaultNumberParser: JsonReader => Json = in => {
    in.setMark()
    var digits = 0
    var b = in.nextByte()
    if (b == '-') b = in.nextByte()
    try {
      while (b >= '0' && b <= '9') {
        b = in.nextByte()
        digits += 1
      }
    } catch {
      case _: JsonReaderException => // ignore the end of input error for now
    } finally in.rollbackToMark()
    new JNumber({
      if ((b | 0x20) != 'e' && b != '.') {
        if (digits < 19) new JsonLong(in.readLong())
        else {
          val x = in.readBigInt(null)
          if (x.bitLength < 64) new JsonLong(x.longValue)
          else new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger))
        }
      } else new JsonBigDecimal(in.readBigDecimal(null).bigDecimal)
    })
  }

  def asciiStringToJString[A](buf: Array[Byte], len: Int): Json = new JString(StringUtil.toString(buf, len))

  def stringValue(c: HCursor): String = c.value match {
    case s: JString => s.value
    case _ => null
  }

  def bigIntValue(c: HCursor): BigInt = c.value match {
    case n: JNumber =>
      n.value match {
        case jbd: JsonBigDecimal =>
          val bd = jbd.value
          if (bd.scale == 0) new BigInt(bd.unscaledValue)
          else null
        case _ => null
      }
    case _ => null
  }

  def jsonValue(x: BigInt): Json = new JNumber(new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger)))
}

final class JsoniterScalaCodec(
    maxDepth: Int,
    initialSize: Int,
    doSerialize: Json => Boolean,
    numberParser: JsonReader => Json) extends JsonValueCodec[Json] {
  private[this] val trueValue = True
  private[this] val falseValue = False
  private[this] val emptyArrayValue = new JArray(Vector.empty)
  private[this] val emptyObjectValue = new JObject(JsonObject.empty)

  override val nullValue: Json = JNull

  override def decodeValue(in: JsonReader, default: Json): Json = decode(in, maxDepth)

  override def encodeValue(x: Json, out: JsonWriter): Unit = encode(x, out, maxDepth)

  private[this] def decode(in: JsonReader, depth: Int): Json = {
    val b = in.nextToken()
    if (b == '"') {
      in.rollbackToken()
      new JString(in.readString(null))
    } else if (b == 'f' || b == 't') {
      in.rollbackToken()
      if (in.readBoolean()) trueValue
      else falseValue
    } else if (b >= '0' && b <= '9' || b == '-') {
      in.rollbackToken()
      numberParser(in)
    } else if (b == '[') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      if (in.isNextToken(']')) emptyArrayValue
      else {
        in.rollbackToken()
        val x = new VectorBuilder[Json]
        while ({
          x += decode(in, depthM1)
          in.isNextToken(',')
        }) ()
        if (in.isCurrentToken(']')) new JArray(x.result())
        else in.arrayEndOrCommaError()
      }
    } else if (b == '{') {
      val depthM1 = depth - 1
      if (depthM1 < 0) in.decodeError("depth limit exceeded")
      if (in.isNextToken('}')) emptyObjectValue
      else {
        in.rollbackToken()
        val x = new util.LinkedHashMap[String, Json](initialSize)
        while ({
          x.put(in.readKeyAsString(), decode(in, depthM1))
          in.isNextToken(',')
        }) ()
        if (in.isCurrentToken('}')) new JObject(JsonObject.fromLinkedHashMap(x))
        else in.objectEndOrCommaError()
      }
    } else in.readNullOrError(nullValue, "expected JSON value")
  }

  private[this] def encode(x: Json, out: JsonWriter, depth: Int): Unit = x match {
    case s: JString => out.writeVal(s.value)
    case b: JBoolean => out.writeVal(b.value)
    case n: JNumber => encodeJsonNumber(n.value, out)
    case a: JArray =>
      val depthM1 = depth - 1
      if (depthM1 < 0) out.encodeError("depth limit exceeded")
      out.writeArrayStart()
      a.value.foreach(v => encode(v, out, depthM1))
      out.writeArrayEnd()
    case o: JObject =>
      val depthM1 = depth - 1
      if (depthM1 < 0) out.encodeError("depth limit exceeded")
      out.writeObjectStart()
      val it = o.value.toIterable.iterator
      while (it.hasNext) {
        val (k, v) = it.next()
        if (doSerialize(v)) {
          out.writeKey(k)
          encode(v, out, depthM1)
        }
      }
      out.writeObjectEnd()
    case _ => out.writeNull()
  }

  private[this] def encodeJsonNumber(x: JsonNumber, out: JsonWriter): Unit = x match {
    case l: JsonLong => out.writeVal(l.value)
    case f: JsonFloat => out.writeVal(f.value)
    case d: JsonDouble => out.writeVal(d.value)
    case bd: JsonBigDecimal => out.writeVal(bd.value)
    case _ => out.writeRawVal(x.toString.getBytes(StandardCharsets.UTF_8))
  }
}