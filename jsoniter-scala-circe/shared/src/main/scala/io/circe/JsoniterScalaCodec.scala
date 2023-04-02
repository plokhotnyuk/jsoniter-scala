package io.circe

import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Json._
import java.nio.charset.StandardCharsets
import java.util
import scala.collection.immutable.VectorBuilder

object JsoniterScalaCodec {
  /**
   * Default number parser that detects integers vs floating-point values
   * and chooses an appropriate JSON number representation.
   *
   * @param in the JSON reader
   * @return a JSON number value
   */
  val defaultNumberParser: JsonReader => Json = in => new JNumber({
    in.setMark()
    var digits = 0
    var b = in.nextByte()
    if (b == '-') b = in.nextByte()
    while ((b >= '0' && b <= '9') && in.hasRemaining()) {
      b = in.nextByte()
      digits += 1
    }
    in.rollbackToMark()
    if ((b | 0x20) != 'e' && b != '.') {
      if (digits < 19) new JsonLong({
        if (digits < 10) in.readInt()
        else in.readLong()
      }) else {
        val x = in.readBigInt(null)
        if (x.isValidLong) new JsonLong(x.longValue)
        else new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger))
      }
    } else new JsonBigDecimal(in.readBigDecimal(null).bigDecimal)
  })

  /**
   * Converts an ASCII byte array to a JSON string.
   *
   * @param buf the ASCII byte array
   * @param len the length of the byte array
   * @return a JSON string
   */
  def asciiStringToJString[A](buf: Array[Byte], len: Int): Json = new JString(StringUtil.toString(buf, len))

  /**
   * Extracts a `String` value from a JSON cursor.
   *
   * @param c the JSON cursor
   * @return the `String` value, or null if the cursor does not point to a string
   */
  def stringValue(c: HCursor): String = c.value match {
    case s: JString => s.value
    case _ => null
  }

  /**
   * Extracts a `BigInt` value from a JSON cursor.
   *
   * @param c the JSON cursor
   * @return the `BigInt` value, or null if the cursor does not point to a number with an integer value
   */  
  def bigIntValue(c: HCursor): BigInt = c.value match {
    case n: JNumber => n.value match {
      case jl: JsonLong => BigInt(jl.value)
      case jbd: JsonBigDecimal =>
        val bd = jbd.value
        if (bd.scale == 0) new BigInt(bd.unscaledValue)
        else null
      case _ => null
    }
    case _ => null
  }

  /**
   * Encodes a `BigInt` as a JSON number.
   *
   * Uses a `JsonLong` if the value fits in a Long, otherwise uses a `JsonBigDecimal`.
   *
   * @param x the BigInt to encode
   * @return a JSON number representing the BigInt
   */  
  def jsonValue(x: BigInt): Json = new JNumber({
    if (x.isValidLong) new JsonLong(x.longValue)
    else new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger))
  })
}

/**
 * Encodes a `BigInt` as a JSON number.
 *
 * Uses a JsonLong if the value fits in a `Long`, otherwise uses a `JsonBigDecimal`.
 *
 * @param x the `BigInt` to encode
 * @return a JSON number representing the `BigInt`
 */
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
    case s: JString =>
      val str = s.value
      if (str.length == 1) out.writeVal(str.charAt(0))
      else out.writeVal(s.value)
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