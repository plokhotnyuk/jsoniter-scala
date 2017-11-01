package com.github.plokhotnyuk.jsoniter_scala

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.JsonWriter._

import scala.collection.breakOut
import scala.annotation.switch

case class WriterConfig(
  indentionStep: Int = 0,
  escapeUnicode: Boolean = false)

//noinspection EmptyCheck
final class JsonWriter private[jsoniter_scala](
    private var buf: Array[Byte],
    private var count: Int,
    private var indention: Int,
    private var out: OutputStream,
    private var config: WriterConfig) {
  require((buf ne null) && buf.length > 0, "buf size should be non empty")

  def reset(out: OutputStream): Unit = {
    this.out = out
    count = 0
  }

  def close(): Unit =
    if (out ne null) {
      flushBuffer()
      out.close()
      out = null // help GC
    }

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
    if (indention > 0) write(':'.toByte, ' '.toByte)
    else write(':')
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
      if (indention > 0) write(':'.toByte, ' '.toByte)
      else write(':')
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

  private def write(b: Byte): Unit = count = {
    val pos = ensure(1)
    buf(pos) = b
    pos + 1
  }

  private def write(b1: Byte, b2: Byte): Unit = count = {
    val pos = ensure(2)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  private def write(b1: Byte, b2: Byte, b3: Byte): Unit = count = {
    val pos = ensure(3)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = count = {
    val pos = ensure(4)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = count = {
    val pos = ensure(5)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    pos + 5
  }

  private def writeAsciiString(s: String): Unit = count = {
    val len = s.length
    val pos = ensure(len)
    s.getBytes(0, len, buf, pos)
    pos + len
  }

  private def writeString(s: String, from: Int, to: Int): Unit = count = {
    var pos = ensure((to - from) + 2) // 1 byte per char (suppose that they are ASCII only) + make room for the quotes
    val buf = this.buf
    var i = from
    buf(pos) = '"'
    pos += 1
    var ch: Char = 0 // the fast path without utf8 and escape support
    while (i < to && {
      ch = s.charAt(i)
      ch > 31 && ch < 128 && ch != '"' && ch != '\\'
    }) pos += {
      i += 1
      buf(pos) = ch.toByte
      1
    }
    if (i == to) {
      buf(pos) = '"'
      pos + 1
    } else { // for the remaining parts we process with utf-8 encoding and escape unicode support
      count = pos
      writeStringSlowPath(s, i, to)
    }
  }

  private def writeStringSlowPath(s: String, from: Int, to: Int): Int = {
    val escapeUnicode = config.escapeUnicode
    var pos = ensure((to - from) * (if (escapeUnicode) 6 else 3) + 1) // max 6/3 bytes per char + the closing quotes
    val buf = this.buf
    var i = from
    while (i < to) pos += {
      val c1 = s.charAt(i)
      i += 1
      if (c1 < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (c1: @switch) match {
          case '"' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = '"'.toByte
            2
          case '\\' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = '\\'.toByte
            2
          case '\b' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'b'.toByte
            2
          case '\f' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'f'.toByte
            2
          case '\n' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'n'.toByte
            2
          case '\r' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'r'.toByte
            2
          case '\t' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 't'.toByte
            2
          case _ =>
            if (escapeUnicode && c1 < 32) {
              buf(pos) = '\\'.toByte
              buf(pos + 1) = 'u'.toByte
              buf(pos + 2) = toHexDigit(c1 >>> 12)
              buf(pos + 3) = toHexDigit(c1 >>> 8)
              buf(pos + 4) = toHexDigit(c1 >>> 4)
              buf(pos + 5) = toHexDigit(c1)
              6
            } else {
              buf(pos) = c1.toByte
              1
            }
        }
      } else if (escapeUnicode) { // FIXME: add surrogate pair checking for escaped unicodes
        buf(pos) = '\\'.toByte
        buf(pos + 1) = 'u'.toByte
        buf(pos + 2) = toHexDigit(c1 >>> 12)
        buf(pos + 3) = toHexDigit(c1 >>> 8)
        buf(pos + 4) = toHexDigit(c1 >>> 4)
        buf(pos + 5) = toHexDigit(c1)
        6
      } else if (c1 < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (c1 >> 6)).toByte
        buf(pos + 1) = (0x80 | (c1 & 0x3F)).toByte
        2
      } else if (!Character.isHighSurrogate(c1)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        if (Character.isLowSurrogate(c1)) illegalSurrogateError(c1)
        buf(pos) = (0xE0 | (c1 >> 12)).toByte
        buf(pos + 1) = (0x80 | ((c1 >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (c1 & 0x3F)).toByte
        3
      } else if (i < to) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val c2 = s.charAt(i)
        i += 1
        if (!Character.isLowSurrogate(c2)) illegalSurrogateError(c2)
        val uc = Character.toCodePoint(c1, c2)
        buf(pos) = (0xF0 | (uc >> 18)).toByte
        buf(pos + 1) = (0x80 | ((uc >> 12) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | ((uc >> 6) & 0x3F)).toByte
        buf(pos + 3) = (0x80 | (uc & 0x3F)).toByte
        4
      } else illegalSurrogateError(c1)
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeChar(ch: Char): Unit = count = {
    var pos = ensure(8) // 6 bytes per char for encoded unicode + make room for the quotes
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    pos += {
      if (ch < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (ch: @switch) match {
          case '"' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = '"'.toByte
            2
          case '\\' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = '\\'.toByte
            2
          case '\b' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'b'.toByte
            2
          case '\f' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'f'.toByte
            2
          case '\n' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'n'.toByte
            2
          case '\r' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 'r'.toByte
            2
          case '\t' =>
            buf(pos) = '\\'.toByte
            buf(pos + 1) = 't'.toByte
            2
          case _ =>
            if (config.escapeUnicode && ch < 32) {
              buf(pos) = '\\'.toByte
              buf(pos + 1) = 'u'.toByte
              buf(pos + 2) = toHexDigit(ch >>> 12)
              buf(pos + 3) = toHexDigit(ch >>> 8)
              buf(pos + 4) = toHexDigit(ch >>> 4)
              buf(pos + 5) = toHexDigit(ch)
              6
            } else {
              buf(pos) = ch.toByte
              1
            }
        }
      } else if (config.escapeUnicode) { // FIXME: add surrogate pair checking for escaped unicodes
        buf(pos) = '\\'.toByte
        buf(pos + 1) = 'u'.toByte
        buf(pos + 2) = toHexDigit(ch >>> 12)
        buf(pos + 3) = toHexDigit(ch >>> 8)
        buf(pos + 4) = toHexDigit(ch >>> 4)
        buf(pos + 5) = toHexDigit(ch)
        6
      } else if (ch < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (ch >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch & 0x3F)).toByte
        2
      } else if (!Character.isSurrogate(ch)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        buf(pos) = (0xE0 | (ch >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch & 0x3F)).toByte
        3
      } else illegalSurrogateError(ch)
    }
    buf(pos) = '"'
    pos + 1
  }

  private def toHexDigit(n: Int): Byte = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toByte

  private def illegalSurrogateError(ch: Int): Nothing = encodeError {
    new StringBuilder(32).append("illegal surrogate: \\u")
      .append(toHexDigit(ch >>> 12).toChar)
      .append(toHexDigit(ch >>> 8).toChar)
      .append(toHexDigit(ch >>> 4).toChar)
      .append(toHexDigit(ch).toChar).toString()
  }

  private def writeCommaWithParentheses(comma: Boolean): Unit = {
    if (comma) write(',')
    writeIndention(0)
    write('"')
  }

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) write('"'.toByte, ':'.toByte, ' '.toByte)
    else write('"'.toByte, ':'.toByte)

  private def writeInt(x: Int): Unit = count ={
    var pos = ensure(11) // minIntBytes.length
    val buf = this.buf
    if (x == Integer.MIN_VALUE) {
      System.arraycopy(minIntBytes, 0, buf, pos, minIntBytes.length)
      pos + minIntBytes.length
    } else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      val q1 = q0 / 1000
      if (q1 == 0) writeFirstRem(buf, q0, pos)
      else {
        val r1 = q0 - q1 * 1000
        val q2 = q1 / 1000
        if (q2 == 0) writeRem(buf, r1, writeFirstRem(buf, q1, pos))
        else {
          val r2 = q1 - q2 * 1000
          val q3 = q2 / 1000
          writeRem(buf, r1, writeRem(buf, r2, {
            if (q3 == 0) writeFirstRem(buf, q2, pos)
            else {
              val r3 = q2 - q3 * 1000
              buf(pos) = (q3 + '0').toByte
              writeRem(buf, r3, pos + 1)
            }
          }))
        }
      }
    }
  }

  // TODO: consider more cache-aware algorithm from RapidJSON, see https://github.com/miloyip/itoa-benchmark/blob/master/src/branchlut.cpp
  private def writeLong(x: Long): Unit = count = {
    var pos = ensure(20) // minLongBytes.length
    if (x == java.lang.Long.MIN_VALUE) {
      System.arraycopy(minLongBytes, 0, buf, pos, minLongBytes.length)
      pos + minLongBytes.length
    } else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      val q1 = q0 / 1000
      if (q1 == 0) writeFirstRem(buf, q0.toInt, pos)
      else {
        val r1 = (q0 - q1 * 1000).toInt
        val q2 = q1 / 1000
        if (q2 == 0) writeRem(buf, r1, writeFirstRem(buf, q1.toInt, pos))
        else {
          val r2 = (q1 - q2 * 1000).toInt
          val q3 = q2 / 1000
          if (q3 == 0) writeRem(buf, r1, writeRem(buf, r2, writeFirstRem(buf, q2.toInt, pos)))
          else {
            val r3 = (q2 - q3 * 1000).toInt
            val q4 = (q3 / 1000).toInt
            if (q4 == 0) writeRem(buf, r1, writeRem(buf, r2, writeRem(buf, r3, writeFirstRem(buf, q3.toInt, pos))))
            else {
              val r4 = (q3 - q4 * 1000).toInt
              val q5 = q4 / 1000
              if (q5 == 0) writeRem(buf, r1, writeRem(buf, r2, writeRem(buf, r3, writeRem(buf, r4, writeFirstRem(buf, q4, pos)))))
              else {
                val r5 = q4 - q5 * 1000
                val q6 = q5 / 1000
                writeRem(buf, r1, writeRem(buf, r2, writeRem(buf, r3, writeRem(buf, r4, writeRem(buf, r5, {
                  if (q6 == 0) writeFirstRem(buf, q5, pos)
                  else {
                    val r6 = q5 - q6 * 1000
                    buf(pos) = (q6 + '0').toByte
                    writeRem(buf, r6, pos + 1)
                  }
                })))))
              }
            }
          }
        }
      }
    }
  }

  private def writeFirstRem(buf: Array[Byte], r: Int, pos: Int): Int = {
    val d = digits(r)
    val off = d >> 12
    if (off == 3) {
      buf(pos) = ((d >> 8) & 15 | '0').toByte
      buf(pos + 1) = ((d >> 4) & 15 | '0').toByte
      buf(pos + 2) = (d & 15 | '0').toByte
    } else if (off == 2) {
      buf(pos) = ((d >> 4) & 15 | '0').toByte
      buf(pos + 1) = (d & 15 | '0').toByte
    } else buf(pos) = (d & 15 | '0').toByte
    pos + off
  }

  private def writeRem(buf: Array[Byte], r: Int, pos: Int): Int = {
    val d = digits(r)
    buf(pos) = ((d >> 8) & 15 | '0').toByte
    buf(pos + 1) = ((d >> 4) & 15 | '0').toByte
    buf(pos + 2) = (d & 15 | '0').toByte
    pos + 3
  }

  private def writeFloat(x: Float): Unit =
    if (java.lang.Float.isFinite(x)) writeAsciiString(java.lang.Float.toString(x))
    else encodeError("illegal number: " + x)

  // TODO: use more efficient algorithm from RapidJSON, see https://github.com/miloyip/dtoa-benchmark
  private def writeDouble(x: Double): Unit =
    if (java.lang.Double.isFinite(x)) writeAsciiString(java.lang.Double.toString(x))
    else encodeError("illegal number: " + x)

  @inline
  private def writeIndention(delta: Int): Unit = if (indention != 0) writeNewLineAndSpaces(delta)

  private def writeNewLineAndSpaces(delta: Int): Unit = count = {
    val toWrite = indention - delta
    var pos = ensure(toWrite + 1)
    val buf = this.buf
    buf(pos) = '\n'.toByte
    pos += 1
    val to = pos + toWrite
    while (pos < to) {
      buf(pos) = ' '.toByte
      pos += 1
    }
    pos
  }

  private def flushBuffer(): Unit =
    if (out ne null) {
      out.write(buf, 0, count)
      count = 0
    }

  @inline
  private def ensure(minimal: Int): Int = {
    if (buf.length < count + minimal) growBuffer(minimal)
    count
  }

  private def growBuffer(minimal: Int): Unit = {
    flushBuffer()
    if (buf.length < count + minimal) {
      val newBuf = new Array[Byte](Math.max(buf.length << 1, count + minimal))
      System.arraycopy(buf, 0, newBuf, 0, buf.length)
      buf = newBuf
    }
  }
}

object JsonWriter {
  private val pool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter =
      new JsonWriter(new Array[Byte](8192), 0, 0, null, WriterConfig())

    override def get(): JsonWriter = {
      val writer = super.get()
      if (writer.buf.length > 32768) writer.buf = new Array[Byte](8192)
      writer
    }
  }
  private val digits: Array[Short] = (0 to 999).map { i =>
    (((if (i < 10) 1 else if (i < 100) 2 else 3) << 12) +
      ((i / 100) << 8) + (((i / 10) % 10) << 4) + i % 10).toShort
  }(breakOut)
  private val minIntBytes: Array[Byte] = "-2147483648".getBytes
  private val minLongBytes: Array[Byte] = "-9223372036854775808".getBytes

  final def write[A](codec: JsonCodec[A], obj: A, out: OutputStream): Unit = {
    val writer = pool.get
    writer.reset(out)
    try codec.encode(obj, writer)
    finally writer.close()
  }

  final def write[A](codec: JsonCodec[A], obj: A): Array[Byte] = {
    val writer = pool.get
    writer.reset(null)
    codec.encode(obj, writer)
    val arr = new Array[Byte](writer.count)
    System.arraycopy(writer.buf, 0, arr, 0, arr.length)
    arr
  }

  final def write[A](codec: JsonCodec[A], obj: A, out: OutputStream, cfg: WriterConfig): Unit = {
    val writer = pool.get
    val currCfg = writer.config
    writer.reset(out)
    writer.config = cfg
    try codec.encode(obj, writer)
    finally {
      writer.config = currCfg
      writer.close()
    }
  }

  final def write[A](codec: JsonCodec[A], obj: A, cfg: WriterConfig): Array[Byte] = {
    val writer = pool.get
    val currCfg = writer.config
    writer.reset(null)
    writer.config = cfg
    try codec.encode(obj, writer)
    finally writer.config = currCfg
    val arr = new Array[Byte](writer.count)
    System.arraycopy(writer.buf, 0, arr, 0, arr.length)
    arr
  }
}