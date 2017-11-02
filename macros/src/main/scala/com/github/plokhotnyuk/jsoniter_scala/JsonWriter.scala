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
      out = null // do not close output stream, just help GC instead
    }

  def writeComma(comma: Boolean): Boolean = {
    if (comma) ensureAndWrite(',')
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
    if (x) ensureAndWrite('t'.toByte, 'r'.toByte, 'u'.toByte, 'e'.toByte)
    else ensureAndWrite('f'.toByte, 'a'.toByte, 'l'.toByte, 's'.toByte, 'e'.toByte)

  def writeVal(x: Byte): Unit = writeInt(x.toInt)

  def writeVal(x: Short): Unit = writeInt(x.toInt)

  def writeVal(x: Char): Unit = writeChar(x)

  def writeVal(x: Int): Unit = writeInt(x)

  def writeVal(x: Long): Unit = writeLong(x)

  def writeVal(x: Float): Unit = writeFloat(x)

  def writeVal(x: Double): Unit = writeDouble(x)

  def writeNull(): Unit = ensureAndWrite('n'.toByte, 'u'.toByte, 'l'.toByte, 'l'.toByte)

  def writeArrayStart(): Unit = {
    indention += config.indentionStep
    ensureAndWrite('[')
  }

  def writeArrayEnd(): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    ensureAndWrite(']')
  }

  def writeObjectStart(): Unit = {
    indention += config.indentionStep
    ensureAndWrite('{')
  }

  def writeObjectEnd(): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    ensureAndWrite('}')
  }

  private def ensureAndWrite(b: Byte): Unit = {
    val pos = ensure(1)
    count = write(b, pos)
  }

  private def ensureAndWrite(b1: Byte, b2: Byte): Unit = {
    val pos = ensure(2)
    count = write(b1, b2, pos)
  }

  private def ensureAndWrite(b1: Byte, b2: Byte, b3: Byte): Unit = {
    val pos = ensure(3)
    count = write(b1, b2, b3, pos)
  }

  private def ensureAndWrite(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = {
    val pos = ensure(4)
    count = write(b1, b2, b3, b4, pos)
  }

  private def ensureAndWrite(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = {
    val pos = ensure(5)
    count = write(b1, b2, b3, b4, b5, pos)
  }

  private def writeAsciiString(s: String): Unit = count = {
    val len = s.length
    val pos = ensure(len)
    s.getBytes(0, len, buf, pos)
    pos + len
  }

  private def writeString(s: String, from: Int, to: Int): Unit = count = {
    var pos = write('"', ensure((to - from) + 2)) // 1 byte per char (suppose that they are ASCII only) + make room for the quotes
    var i = from
    var ch: Char = 0 // the fast path without utf8 and escape support
    while (i < to && {
      ch = s.charAt(i)
      ch > 31 && ch < 128 && ch != '"' && ch != '\\'
    }) pos = {
      i += 1
      write(ch.toByte, pos)
    }
    if (i == to) write('"', pos)
    else { // for the remaining parts we process with utf-8 encoding and escape unicode support
      count = pos
      writeStringSlowPath(s, i, to)
    }
  }

  private def writeStringSlowPath(s: String, from: Int, to: Int): Int = {
    val escapeUnicode = config.escapeUnicode
    var pos = ensure((to - from) * (if (escapeUnicode) 6 else 3) + 1) // max 6/3 bytes per char + the closing quotes
    var i = from
    while (i < to) pos = {
      val c1 = s.charAt(i)
      i += 1
      if (c1 < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (c1: @switch) match {
          case '"' => write('\\', '"', pos)
          case '\\' => write('\\', '\\', pos)
          case '\b' => write('\\', 'b', pos)
          case '\f' => write('\\', 'f', pos)
          case '\n' => write('\\', 'n', pos)
          case '\r' => write('\\', 'r', pos)
          case '\t' => write('\\', 't', pos)
          case _ =>
            if (escapeUnicode && c1 < 32) writeEscapedUnicode(c1, pos)
            else write(c1.toByte, pos)
        }
      } else if (escapeUnicode) { // FIXME: add surrogate pair checking for escaped unicodes
        if (c1 < 2048 || !Character.isHighSurrogate(c1)) {
          if (Character.isLowSurrogate(c1)) illegalSurrogateError(c1)
          writeEscapedUnicode(c1, pos)
        } else if (i < to) {
          val c2 = s.charAt(i)
          i += 1
          if (!Character.isLowSurrogate(c2)) illegalSurrogateError(c2)
          writeEscapedUnicode(c2, writeEscapedUnicode(c1, pos))
        } else illegalSurrogateError(c1)
      } else if (c1 < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        write((0xC0 | (c1 >> 6)).toByte, (0x80 | (c1 & 0x3F)).toByte, pos)
      } else if (!Character.isHighSurrogate(c1)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        if (Character.isLowSurrogate(c1)) illegalSurrogateError(c1)
        write((0xE0 | (c1 >> 12)).toByte, (0x80 | ((c1 >> 6) & 0x3F)).toByte, (0x80 | (c1 & 0x3F)).toByte, pos)
      } else if (i < to) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val c2 = s.charAt(i)
        i += 1
        if (!Character.isLowSurrogate(c2)) illegalSurrogateError(c2)
        val uc = Character.toCodePoint(c1, c2)
        write((0xF0 | (uc >> 18)).toByte, (0x80 | ((uc >> 12) & 0x3F)).toByte, (0x80 | ((uc >> 6) & 0x3F)).toByte, (0x80 | (uc & 0x3F)).toByte, pos)
      } else illegalSurrogateError(c1)
    }
    write('"', pos)
  }

  private def writeChar(ch: Char): Unit = count = {
    var pos = write('"', ensure(8)) // 6 bytes per char for encoded unicode + make room for the quotes
    pos = {
      if (ch < 128) { // 1 byte, 7 bits: 0xxxxxxx
        (ch: @switch) match {
          case '"' => write('\\', '"', pos)
          case '\\' => write('\\', '\\', pos)
          case '\b' => write('\\', 'b', pos)
          case '\f' => write('\\', 'f', pos)
          case '\n' => write('\\', 'n', pos)
          case '\r' => write('\\', 'r', pos)
          case '\t' => write('\\', 't', pos)
          case _ =>
            if (config.escapeUnicode && ch < 32) writeEscapedUnicode(ch, pos)
            else write(ch.toByte, pos)
        }
      } else if (config.escapeUnicode) {
        if (Character.isSurrogate(ch)) illegalSurrogateError(ch)
        writeEscapedUnicode(ch, pos)
      } else if (ch < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        write((0xC0 | (ch >> 6)).toByte, (0x80 | (ch & 0x3F)).toByte, pos)
      } else if (!Character.isSurrogate(ch)) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        write((0xE0 | (ch >> 12)).toByte,  (0x80 | ((ch >> 6) & 0x3F)).toByte, (0x80 | (ch & 0x3F)).toByte, pos)
      } else illegalSurrogateError(ch)
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeEscapedUnicode(ch: Int, pos: Int) = {
    buf(pos) = '\\'.toByte
    buf(pos + 1) = 'u'.toByte
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

  private def illegalSurrogateError(ch: Int): Nothing = encodeError {
    new StringBuilder(32).append("illegal surrogate: \\u")
      .append(toHexDigit(ch >>> 12).toChar)
      .append(toHexDigit(ch >>> 8).toChar)
      .append(toHexDigit(ch >>> 4).toChar)
      .append(toHexDigit(ch).toChar).toString()
  }

  private def writeCommaWithParentheses(comma: Boolean): Unit = {
    if (comma) ensureAndWrite(',')
    writeIndention(0)
    ensureAndWrite('"')
  }

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) ensureAndWrite('"'.toByte, ':'.toByte, ' '.toByte)
    else ensureAndWrite('"'.toByte, ':'.toByte)

  private def writeColon(): Unit =
    if (config.indentionStep > 0) ensureAndWrite(':'.toByte, ' '.toByte)
    else ensureAndWrite(':'.toByte)

  private def writeInt(x: Int): Unit = count = {
    var pos = ensure(11) // minIntBytes.length
    if (x == Integer.MIN_VALUE) writeBytes(minIntBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          pos = write('-', pos)
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
              writeRem(r3, write((q3 + '0').toByte, pos))
            }
          }))
        }
      }
    }
  }

  // TODO: consider more cache-aware algorithm from RapidJSON, see https://github.com/miloyip/itoa-benchmark/blob/master/src/branchlut.cpp
  private def writeLong(x: Long): Unit = count = {
    var pos = ensure(20) // minLongBytes.length
    if (x == java.lang.Long.MIN_VALUE) writeBytes(minLongBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          pos = write('-', pos)
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
                    writeRem(r6, write((q6 + '0').toByte, pos))
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

  private def writeFirstRem(r: Int, pos: Int) = {
    val d = digits(r)
    val skip = d >> 12
    if (skip == 0) write(((d >> 8) & 15 | '0').toByte, ((d >> 4) & 15 | '0').toByte, (d & 15 | '0').toByte, pos)
    else if (skip == 1) write(((d >> 4) & 15 | '0').toByte, (d & 15 | '0').toByte, pos)
    else write((d & 15 | '0').toByte, pos)
  }

  private def writeRem(r: Int, pos: Int) = {
    val d = digits(r)
    write(((d >> 8) & 15 | '0').toByte, ((d >> 4) & 15 | '0').toByte, (d & 15 | '0').toByte, pos)
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
    var pos = write('\n', ensure(toWrite + 1))
    val to = pos + toWrite
    while (pos < to) pos = write(' ', pos)
    pos
  }

  @inline
  private def write(b: Byte, pos: Int): Int = {
    buf(pos) = b
    pos + 1
  }

  @inline
  private def write(b1: Byte, b2: Byte, pos: Int): Int = {
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte, pos: Int): Int = {
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, pos: Int): Int = {
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  @inline
  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, pos: Int): Int = {
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    pos + 5
  }

  @inline
  private def ensure(minimal: Int): Int = {
    if (buf.length < count + minimal) growBuffer(minimal)
    count
  }

  private def flushBuffer(): Unit =
    if (out ne null) {
      out.write(buf, 0, count)
      count = 0
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
      new JsonWriter(new Array[Byte](4096), 0, 0, null, WriterConfig())

    override def get(): JsonWriter = {
      val writer = super.get()
      if (writer.buf.length > 16384) writer.buf = new Array[Byte](16384)
      writer
    }
  }
  private val digits: Array[Short] = (0 to 999).map { i =>
    (((if (i < 10) 2 else if (i < 100) 1 else 0) << 12) +
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