package com.github.plokhotnyuk.jsoniter_scala

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.JsonWriter._

import scala.annotation.switch
import scala.collection.breakOut

case class WriterConfig(
    indentionStep: Int = 0,
    escapeUnicode: Boolean = false,
    preferredBufSize: Int = 16384)

final class JsonWriter private[jsoniter_scala](
    private var buf: Array[Byte] = new Array[Byte](4096),
    private var count: Int = 0,
    private var indention: Int = 0,
    private var out: OutputStream = null,
    private var isBufGrowingAllowed: Boolean = true,
    private var config: WriterConfig = WriterConfig()) {
  def writeComma(comma: Boolean): Boolean = {
    if (comma) write(',')
    writeIndention(0)
    true
  }

  def writeObjectField(comma: Boolean, x: Boolean): Boolean = {
    writeCommaWithParentheses(comma)
    writeVal(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Byte): Boolean = {
    writeCommaWithParentheses(comma)
    writeInt(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Char): Boolean = {
    writeComma(comma)
    writeChar(x)
    writeColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Short): Boolean = {
    writeCommaWithParentheses(comma)
    writeInt(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Int): Boolean = {
    writeCommaWithParentheses(comma)
    writeInt(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Long): Boolean = {
    writeCommaWithParentheses(comma)
    writeLong(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Float): Boolean = {
    writeCommaWithParentheses(comma)
    writeFloat(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: Double): Boolean = {
    writeCommaWithParentheses(comma)
    writeDouble(x)
    writeParenthesesWithColon()
    true
  }

  def writeObjectField(comma: Boolean, x: BigInt): Boolean =
    if (x ne null) {
      writeCommaWithParentheses(comma)
      writeAsciiString(x.toString)
      writeParenthesesWithColon()
      true
    } else encodeError("key cannot be null")

  def writeObjectField(comma: Boolean, x: BigDecimal): Boolean =
    if (x ne null) {
      writeCommaWithParentheses(comma)
      writeAsciiString(x.toString)
      writeParenthesesWithColon()
      true
    } else encodeError("key cannot be null")

  def writeObjectField(comma: Boolean, x: String): Boolean =
    if (x ne null) {
      writeComma(comma)
      writeString(x, 0, x.length)
      writeColon()
      true
    } else encodeError("key cannot be null")

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(x: BigDecimal): Unit = if (x eq null) writeNull() else writeAsciiString(x.toString)

  def writeVal(x: BigInt): Unit = if (x eq null) writeNull() else writeAsciiString(x.toString)

  def writeVal(x: String): Unit = if (x eq null) writeNull() else writeString(x, 0, x.length)

  def writeVal(x: Boolean): Unit =
    if (x) write('t'.toByte, 'r'.toByte, 'u'.toByte, 'e'.toByte)
    else write('f'.toByte, 'a'.toByte, 'l'.toByte, 's'.toByte, 'e'.toByte)

  def writeVal(x: Byte): Unit = writeInt(x.toInt)

  def writeVal(x: Short): Unit = writeInt(x.toInt)

  def writeVal(x: Char): Unit = writeChar(x)

  def writeVal(x: Int): Unit = writeInt(x)

  def writeVal(x: Long): Unit = writeLong(x)

  def writeVal(x: Float): Unit = writeFloat(x)

  def writeVal(x: Double): Unit = writeDouble(x)

  def writeNull(): Unit = write('n'.toByte, 'u'.toByte, 'l'.toByte, 'l'.toByte)

  def writeArrayStart(): Unit = {
    indention += config.indentionStep
    write('[')
  }

  def writeArrayEnd(): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    write(']')
  }

  def writeObjectStart(): Unit = {
    indention += config.indentionStep
    write('{')
  }

  def writeObjectEnd(): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    write('}')
  }

  @inline
  private def write(b: Byte): Unit = count = {
    val pos = ensureBufferCapacity(1)
    buf(pos) = b
    pos + 1
  }

  @inline
  private def write(b1: Byte, b2: Byte): Unit = count = {
    val pos = ensureBufferCapacity(2)
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte): Unit = count = {
    val pos = ensureBufferCapacity(3)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = count = {
    val pos = ensureBufferCapacity(4)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = count = {
    val pos = ensureBufferCapacity(5)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    pos + 5
  }

  private def writeAsciiString(s: String): Unit = count = {
    val len = s.length
    val pos = ensureBufferCapacity(len)
    s.getBytes(0, len, buf, pos)
    pos + len
  }

  private def writeString(s: String, from: Int, to: Int): Unit = count = {
    var pos = ensureBufferCapacity((to - from) + 2) // 1 byte per char (suppose that they are ASCII only) + make room for the quotes
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    var i = from
    var ch: Char = 0 // the fast path without utf8 encoding and unicode escaping
    while (i < to && {
      ch = s.charAt(i)
      ch > 31 && ch < 127 && ch != '"' && ch != '\\'
    }) pos = {
      i += 1
      buf(pos) = ch.toByte
      pos + 1
    }
    if (i == to) {
      buf(pos) = '"'
      pos + 1
    } else { // for the remaining parts we process with utf-8 encoding and unicode escaping
      count = pos
      if (config.escapeUnicode) writeEscapedString(s, i, to)
      else writeEncodedString(s, i, to)
    }
  }

  private def writeEncodedString(s: String, from: Int, to: Int): Int = {
    var pos = ensureBufferCapacity((to - from) * 3 + 1) // max 3 bytes per char + the closing quotes
    val buf = this.buf
    var i = from
    while (i < to) pos = {
      val ch1 = s.charAt(i)
      i += 1
      if (ch1 < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (ch1: @switch) match {
          case '"' =>
            buf(pos) = '\\'
            buf(pos + 1) = '"'
            pos + 2
          case '\n' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'n'
            pos + 2
          case '\r' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'r'
            pos + 2
          case '\t' =>
            buf(pos) = '\\'
            buf(pos + 1) = 't'
            pos + 2
          case '\b' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'b'
            pos + 2
          case '\f' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'f'
            pos + 2
          case '\\' => // TODO: consider should '/' be escaped too?
            buf(pos) = '\\'
            buf(pos + 1) = '\\'
            pos + 2
          case _ =>
            buf(pos) = ch1.toByte
            pos + 1
        }
      } else if (ch1 < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (ch1 >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch1 & 0x3F)).toByte
        pos + 2
      } else if (!Character.isHighSurrogate(ch1)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        if (Character.isLowSurrogate(ch1)) illegalSurrogateError()
        buf(pos) = (0xE0 | (ch1 >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch1 >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch1 & 0x3F)).toByte
        pos + 3
      } else if (i < to) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val ch2 = s.charAt(i)
        i += 1
        if (!Character.isLowSurrogate(ch2)) illegalSurrogateError()
        val cp = Character.toCodePoint(ch1, ch2)
        buf(pos) = (0xF0 | (cp >> 18)).toByte
        buf(pos + 1) = (0x80 | ((cp >> 12) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | ((cp >> 6) & 0x3F)).toByte
        buf(pos + 3) = (0x80 | (cp & 0x3F)).toByte
        pos + 4
      } else illegalSurrogateError()
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeEscapedString(s: String, from: Int, to: Int): Int = {
    var pos = ensureBufferCapacity((to - from) * 6 + 1) // max 6 bytes per char + the closing quotes
    val buf = this.buf
    var i = from
    while (i < to) pos = {
      val ch1 = s.charAt(i)
      i += 1
      if (ch1 < 128) {
        (ch1: @switch) match {
          case '"' =>
            buf(pos) = '\\'
            buf(pos + 1) = '"'
            pos + 2
          case '\n' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'n'
            pos + 2
          case '\r' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'r'
            pos + 2
          case '\t' =>
            buf(pos) = '\\'
            buf(pos + 1) = 't'
            pos + 2
          case '\b' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'b'
            pos + 2
          case '\f' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'f'
            pos + 2
          case '\\' => // TODO: consider should '/' be escaped too?
            buf(pos) = '\\'
            buf(pos + 1) = '\\'
            pos + 2
          case _ => if (ch1 <= 31 || ch1 >= 127) writeEscapedUnicode(ch1, buf, pos) else {
            buf(pos) = ch1.toByte
            pos + 1
          }
        }
      } else if (ch1 < 2048 || !Character.isHighSurrogate(ch1)) {
        if (Character.isLowSurrogate(ch1)) illegalSurrogateError()
        writeEscapedUnicode(ch1, buf, pos)
      } else if (i < to) {
        val ch2 = s.charAt(i)
        i += 1
        if (!Character.isLowSurrogate(ch2)) illegalSurrogateError()
        writeEscapedUnicode(ch2, buf, writeEscapedUnicode(ch1, buf, pos))
      } else illegalSurrogateError()
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeChar(ch: Char): Unit = count = {
    var pos = ensureBufferCapacity((if (config.escapeUnicode) 6 else 3) + 2) // 6 or 3 bytes per char for encoded unicode + make room for the quotes
    buf(pos) = '"'
    pos += 1
    pos = {
      if (ch < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (ch: @switch) match {
          case '"' =>
            buf(pos) = '\\'
            buf(pos + 1) = '"'
            pos + 2
          case '\n' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'n'
            pos + 2
          case '\r' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'r'
            pos + 2
          case '\t' =>
            buf(pos) = '\\'
            buf(pos + 1) = 't'
            pos + 2
          case '\b' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'b'
            pos + 2
          case '\f' =>
            buf(pos) = '\\'
            buf(pos + 1) = 'f'
            pos + 2
          case '\\' => // TODO: consider should '/' be escaped too?
            buf(pos) = '\\'
            buf(pos + 1) = '\\'
            pos + 2
          case _ => if (config.escapeUnicode && (ch <= 31 || ch >= 127)) writeEscapedUnicode(ch, buf, pos) else {
            buf(pos) = ch.toByte
            pos + 1
          }
        }
      } else if (config.escapeUnicode) {
        if (Character.isSurrogate(ch)) illegalSurrogateError()
        writeEscapedUnicode(ch, buf, pos)
      } else if (ch < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (ch >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch & 0x3F)).toByte
        pos + 2
      } else if (!Character.isSurrogate(ch)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        buf(pos) = (0xE0 | (ch >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch & 0x3F)).toByte
        pos + 3
      } else illegalSurrogateError()
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeEscapedUnicode(ch: Char, buf: Array[Byte], pos: Int): Int = {
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = toHexDigit(ch >>> 12)
    buf(pos + 3) = toHexDigit(ch >>> 8)
    buf(pos + 4) = toHexDigit(ch >>> 4)
    buf(pos + 5) = toHexDigit(ch)
    pos + 6
  }

  private def toHexDigit(n: Int): Byte = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toByte

  private def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private def writeCommaWithParentheses(comma: Boolean): Unit = {
    if (comma) write(',')
    writeIndention(0)
    write('"')
  }

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) write('"'.toByte, ':'.toByte, ' '.toByte)
    else write('"'.toByte, ':'.toByte)

  private def writeColon(): Unit =
    if (config.indentionStep > 0) write(':'.toByte, ' '.toByte)
    else write(':'.toByte)

  private def writeInt(x: Int): Unit = count = {
    var pos = ensureBufferCapacity(11) // minIntBytes.length
    if (x == Integer.MIN_VALUE) writeBytes(minIntBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      val q1 = q0 / 1000
      if (q1 == 0) writeFirstRem(q0, pos)
      else {
        val r1 = q0 - q1 * 1000
        val q2 = q1 / 1000
        if (q2 == 0) writeRem(r1, writeFirstRem(q1, pos))
        else {
          val r2 = q1 - q2 * 1000
          val q3 = q2 / 1000
          writeRem(r1, writeRem(r2, {
            if (q3 == 0) writeFirstRem(q2, pos)
            else {
              val r3 = q2 - q3 * 1000
              writeRem(r3, {
                buf(pos) = (q3 + '0').toByte
                pos + 1
              })
            }
          }))
        }
      }
    }
  }

  // TODO: consider more cache-aware algorithm from RapidJSON, see https://github.com/miloyip/itoa-benchmark/blob/master/src/branchlut.cpp
  private def writeLong(x: Long): Unit = count = {
    var pos = ensureBufferCapacity(20) // minLongBytes.length
    if (x == java.lang.Long.MIN_VALUE) writeBytes(minLongBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      val q1 = q0 / 1000
      if (q1 == 0) writeFirstRem(q0.toInt, pos)
      else {
        val r1 = (q0 - q1 * 1000).toInt
        val q2 = q1 / 1000
        if (q2 == 0) writeRem(r1, writeFirstRem(q1.toInt, pos))
        else {
          val r2 = (q1 - q2 * 1000).toInt
          val q3 = q2 / 1000
          if (q3 == 0) writeRem(r1, writeRem(r2, writeFirstRem(q2.toInt, pos)))
          else {
            val r3 = (q2 - q3 * 1000).toInt
            val q4 = (q3 / 1000).toInt
            if (q4 == 0) writeRem(r1, writeRem(r2, writeRem(r3, writeFirstRem(q3.toInt, pos))))
            else {
              val r4 = (q3 - q4 * 1000).toInt
              val q5 = q4 / 1000
              if (q5 == 0) writeRem(r1, writeRem(r2, writeRem(r3, writeRem(r4, writeFirstRem(q4, pos)))))
              else {
                val r5 = q4 - q5 * 1000
                val q6 = q5 / 1000
                writeRem(r1, writeRem(r2, writeRem(r3, writeRem(r4, writeRem(r5, {
                  if (q6 == 0) writeFirstRem(q5, pos)
                  else {
                    val r6 = q5 - q6 * 1000
                    writeRem(r6, {
                      buf(pos) = (q6 + '0').toByte
                      pos + 1
                    })
                  }
                })))))
              }
            }
          }
        }
      }
    }
  }

  private def writeBytes(bs: Array[Byte], pos: Int): Int = {
    System.arraycopy(bs, 0, buf, pos, bs.length)
    pos + bs.length
  }

  private def writeFirstRem(r: Int, pos: Int): Int = {
    val d = digits(r)
    val skip = d >> 12
    if (skip == 0) {
      buf(pos) = ((d >> 8) & 15 | '0').toByte
      buf(pos + 1) = ((d >> 4) & 15 | '0').toByte
      buf(pos + 2) = (d & 15 | '0').toByte
      pos + 3
    } else if (skip == 1) {
      buf(pos) = ((d >> 4) & 15 | '0').toByte
      buf(pos + 1) = (d & 15 | '0').toByte
      pos + 2
    } else {
      buf(pos) = (d & 15 | '0').toByte
      pos + 1
    }
  }

  private def writeRem(r: Int, pos: Int): Int = {
    val d = digits(r)
    buf(pos) = ((d >> 8) & 15 | '0').toByte
    buf(pos + 1) = ((d >> 4) & 15 | '0').toByte
    buf(pos + 2) = (d & 15 | '0').toByte
    pos + 3
  }

  private def writeFloat(x: Float): Unit =
    if (java.lang.Float.isFinite(x)) writeAsciiString(java.lang.Float.toString(x))
    else encodeError("illegal number: " + x)

  // TODO: use more efficient algorithm, see https://github.com/Tencent/rapidjson/blob/fe550f38669fe0f488926c1ef0feb6c101f586d6/include/rapidjson/internal/dtoa.h
  private def writeDouble(x: Double): Unit =
    if (java.lang.Double.isFinite(x)) writeAsciiString(java.lang.Double.toString(x))
    else encodeError("illegal number: " + x)

  @inline
  private def writeIndention(delta: Int): Unit = if (indention != 0) writeNewLineAndSpaces(delta)

  private def writeNewLineAndSpaces(delta: Int): Unit = count = {
    val toWrite = indention - delta
    var pos = ensureBufferCapacity(toWrite + 1)
    buf(pos) = '\n'
    pos += 1
    val to = pos + toWrite
    while (pos < to) pos = {
      buf(pos) = ' '
      pos + 1
    }
    pos
  }

  @inline
  private def ensureBufferCapacity(required: Int): Int = {
    if (buf.length < count + required) growBuffer(required)
    count
  }

  private def growBuffer(required: Int): Unit = {
    flushBuffer()
    if (buf.length < count + required) {
      if (isBufGrowingAllowed) {
        val bs = new Array[Byte](Math.max(buf.length << 1, count + required))
        System.arraycopy(buf, 0, bs, 0, buf.length)
        buf = bs
      } else throw new ArrayIndexOutOfBoundsException("`buf` length exceeded")
    }
  }

  private[jsoniter_scala] def flushBuffer(): Unit = if (out ne null) {
    out.write(buf, 0, count)
    count = 0
  }

  private def freeTooLongBuf(): Unit =
    if (buf.length > config.preferredBufSize) buf = new Array[Byte](config.preferredBufSize)
}

object JsonWriter {
  private val pool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter()
  }
  private val defaultConfig = WriterConfig()
  private val digits: Array[Short] = (0 to 999).map { i =>
    (((if (i < 10) 2 else if (i < 100) 1 else 0) << 12) + // this nibble encodes number of leading zeroes
      ((i / 100) << 8) + (((i / 10) % 10) << 4) + i % 10).toShort // decimal digit per nibble
  }(breakOut)
  private val minIntBytes: Array[Byte] = "-2147483648".getBytes
  private val minLongBytes: Array[Byte] = "-9223372036854775808".getBytes

  /**
    * Serialize the `x` argument to the provided output stream in UTF-8 encoding of JSON format
    * with default configuration options that minimizes output size & time to serialize.
    *
    * @param codec a codec for the given value
    * @param x the value to serialize
    * @param out an output stream to serialize into
    * @tparam A type of value to serialize
    * @throws NullPointerException if the `codec` or `config` is null
    */
  final def write[A](codec: JsonCodec[A], x: A, out: OutputStream): Unit = write(codec, x, out, defaultConfig)

  /**
    * Serialize the `x` argument to the provided output stream in UTF-8 encoding of JSON format
    * that specified by provided configuration options.
    *
    * @param codec a codec for the given value
    * @param x the value to serialize
    * @param out an output stream to serialize into
    * @param config a serialization configuration
    * @tparam A type of value to serialize
    * @throws NullPointerException if the `codec`, `out` or `config` is null
    */
  final def write[A](codec: JsonCodec[A], x: A, out: OutputStream, config: WriterConfig): Unit = {
    if ((out eq null) || (config eq null)) throw new NullPointerException
    val writer = pool.get
    writer.config = config
    writer.out = out
    writer.count = 0
    writer.indention = 0
    try codec.encode(x, writer) // also checks that `codec` is not null before any serialization
    finally {
      writer.flushBuffer()
      writer.out = null // do not close output stream, just help GC instead
      writer.freeTooLongBuf()
    }
  }

  /**
    * Serialize the `x` argument to a new allocated instance of byte array in UTF-8 encoding of JSON format
    * with default configuration options that minimizes output size & time to serialize.
    *
    * @param codec a codec for the given value
    * @param x the value to serialize
    * @tparam A type of value to serialize
    * @return a byte array with `x` serialized to JSON
    * @throws NullPointerException if the `codec` is null
    */
  final def write[A](codec: JsonCodec[A], x: A): Array[Byte] = write(codec, x, defaultConfig)

  /**
    * Serialize the `x` argument to a new allocated instance of byte array in UTF-8 encoding of JSON format,
    * that specified by provided configuration options.
    *
    * @param codec a codec for the given value
    * @param x the value to serialize
    * @param config a serialization configuration
    * @tparam A type of value to serialize
    * @return a byte array with `x` serialized to JSON
    * @throws NullPointerException if the `codec` or `config` is null
    */
  final def write[A](codec: JsonCodec[A], x: A, config: WriterConfig): Array[Byte] = {
    if (config eq null) throw new NullPointerException
    val writer = pool.get
    writer.config = config
    writer.count = 0
    writer.indention = 0
    try {
      codec.encode(x, writer) // also checks that `codec` is not null before any serialization
      val arr = new Array[Byte](writer.count)
      System.arraycopy(writer.buf, 0, arr, 0, arr.length)
      arr
    } finally {
      writer.freeTooLongBuf()
    }
  }

  /**
    * Serialize the `x` argument to the given instance of byte array in UTF-8 encoding of JSON format
    * that specified by provided configuration options or defaults that minimizes output size & time to serialize.
    *
    * @param codec a codec for the given value
    * @param x the value to serialize
    * @param buf a byte array where the value should be serialized
    * @param from a position in the byte array from which serialization of the value should start
    * @param config a serialization configuration
    * @tparam A type of value to serialize
    * @return number of next position after last byte serialized to `buf`
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    * @throws ArrayIndexOutOfBoundsException if the `from` is greater than `buf` length or negative,
    *                                        or `buf` length was exceeded during serialization
    */
  final def write[A](codec: JsonCodec[A], x: A, buf: Array[Byte], from: Int, config: WriterConfig = defaultConfig): Int = {
    if (config eq null) throw new NullPointerException
    if (from > buf.length || from < 0) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `buf` length")
    val writer = pool.get
    val currBuf = writer.buf
    writer.config = config
    writer.buf = buf
    writer.count = from
    writer.indention = 0
    writer.isBufGrowingAllowed = false
    try {
      codec.encode(x, writer) // also checks that `codec` is not null before any serialization
      writer.count
    } finally {
      writer.buf = currBuf
      writer.isBufGrowingAllowed = true
    }
  }
}