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
   * @return a JSON number value
   */
  val defaultNumberParser: JsonReader => Json = in => new JNumber({
    in.setMark()
    var b = in.nextByte()
    if (b == '-') b = in.nextByte()
    var digits = 0
    while ((b >= '0' && b <= '9') && {
      digits += 1
      in.hasRemaining()
    }) b = in.nextByte()
    in.rollbackToMark()
    if ((b | 0x20) != 'e' && b != '.') {
      if (digits < 10) new JsonLong(in.readInt())
      else if (digits < 19) new JsonLong(in.readLong())
      else {
        val x = in.readBigInt(null)
        if (x.isValidLong) new JsonLong(x.longValue)
        else new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger))
      }
    } else new JsonBigDecimal(in.readBigDecimal(null).bigDecimal)
  })

  val defaultNumberSerializer: (JsonWriter, JsonNumber) => Unit = (out: JsonWriter, x: JsonNumber) => x match {
    case l: JsonLong => out.writeVal(l.value)
    case f: JsonFloat => out.writeVal(f.value)
    case d: JsonDouble => out.writeVal(d.value)
    case bd: JsonBigDecimal => out.writeVal(bd.value)
    case _ => out.writeRawVal(x.toString.getBytes(StandardCharsets.UTF_8))
  }

  val jsCompatibleNumberSerializer: (JsonWriter, JsonNumber) => Unit = (out: JsonWriter, x: JsonNumber) => x match {
    case l: JsonLong =>
      val v = l.value
      if (v >= -4503599627370496L && v < 4503599627370496L) out.writeVal(v)
      else out.writeValAsString(v)
    case f: JsonFloat => out.writeVal(f.value)
    case d: JsonDouble => out.writeVal(d.value)
    case bd: JsonBigDecimal =>
      val v = bd.value
      val bl = v.unscaledValue.bitLength
      val s = v.scale
      if (bl <= 52 && s >= -256 && s <= 256) out.writeVal(v)
      else out.writeValAsString(v)
    case _ => x.toBigDecimal match {
      case Some(bd) =>
        val u = bd.bigDecimal
        val bl = u.unscaledValue.bitLength
        val s = u.scale
        if (bl <= 52 && s >= -256 && s <= 256) out.writeVal(u)
        else out.writeValAsString(u)
      case _ =>
        out.writeNonEscapedAsciiVal(x.toString)
    }
  }

  /**
   * Converts an ASCII byte array to a JSON string.
   *
   * @param buf the ASCII byte array
   * @param len the length of the byte array
   * @return a JSON string
   */
  @inline
  def asciiStringToJString(buf: Array[Byte], len: Int): Json = new JString(StringUtil.toString(buf, len))

  /**
   * Extracts a `String` value from a JSON cursor.
   *
   * @param c the JSON cursor
   * @return the `String` value, or null if the cursor does not point to a string
   */
  @inline
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
  @inline
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
  @inline
  def jsonValue(x: BigInt): Json = new JNumber({
    if (x.isValidLong) new JsonLong(x.longValue)
    else new JsonBigDecimal(new java.math.BigDecimal(x.bigInteger))
  })
}

/**
 * A JSON value codec that parses and serialize to/from circe's JSON AST.
 *
 * @param maxDepth the maximum depth for decoding
 * @param initialSize the initial size hint for object and array collections
 * @param doSerialize a predicate that determines whether a value should be serialized
 * @param numberParser a function that parses JSON numbers
 * @param numberSerializer a function that serializes JSON numbers
 * @return The JSON codec
 */
final class JsoniterScalaCodec(
    maxDepth: Int,
    initialSize: Int,
    doSerialize: Json => Boolean,
    numberParser: JsonReader => Json,
    numberSerializer: (JsonWriter, JsonNumber) => Unit) extends JsonValueCodec[Json] {

  /**
   * An auxiliary constructor for backward binary compatibility.
   *
   * @param maxDepth the maximum depth for decoding
   * @param initialSize the initial size hint for object and array collections
   * @param doSerialize a predicate that determines whether a value should be serialized
   * @param numberParser a function that parses JSON numbers
   */
  def this(maxDepth: Int, initialSize: Int, doSerialize: Json => Boolean, numberParser: JsonReader => Json) =
    this(maxDepth, initialSize, doSerialize, numberParser, JsoniterScalaCodec.defaultNumberSerializer)

  private[this] val trueValue = True
  private[this] val falseValue = False
  private[this] val emptyArrayValue = new JArray(Vector.empty)
  private[this] val emptyObjectValue = new JObject(JsonObject.empty)

  override val nullValue: Json = JNull

  @inline
  override def decodeValue(in: JsonReader, default: Json): Json = decode(in, maxDepth)

  @inline
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
        val x = new util.LinkedHashMap[String, Json](initialSize, 0.75f)
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
    case n: JNumber => numberSerializer(out, n.value)
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
        val kv = it.next()
        val v = kv._2
        if (doSerialize(v)) {
          out.writeKey(kv._1)
          encode(v, out, depthM1)
        }
      }
      out.writeObjectEnd()
    case _ => out.writeNull()
  }
}