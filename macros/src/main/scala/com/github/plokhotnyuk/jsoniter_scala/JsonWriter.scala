package com.github.plokhotnyuk.jsoniter_scala

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.JsonWriter._

import scala.annotation.switch

case class WriterConfig(
  indentionStep: Int = 0,
  escapeUnicode: Boolean = false)

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
    writeIndention()
    true
  }

  def writeObjectField(comma: Boolean, x: Boolean): Boolean = {
    writeCommaWithParentheses(comma)
    writeVal(x)
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

  def writeVal(x: Char): Unit = writeInt(x.toInt)

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

  private def write(b: Byte): Unit = {
    ensure(1)
    buf(count) = b
    count += 1
  }

  private def write(b1: Byte, b2: Byte): Unit = {
    ensure(2)
    buf(count) = b1
    buf(count + 1) = b2
    count += 2
  }

  private def write(b1: Byte, b2: Byte, b3: Byte): Unit = {
    ensure(3)
    buf(count) = b1
    buf(count + 1) = b2
    buf(count + 2) = b3
    count += 3
  }

  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = {
    ensure(4)
    buf(count) = b1
    buf(count + 1) = b2
    buf(count + 2) = b3
    buf(count + 3) = b4
    count += 4
  }

  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = {
    ensure(5)
    buf(count) = b1
    buf(count + 1) = b2
    buf(count + 2) = b3
    buf(count + 3) = b4
    buf(count + 4) = b5
    count += 5
  }

  private def writeAsciiString(s: String): Unit = {
    val len = s.length
    ensure(len)
    s.getBytes(0, len, buf, count)
    count += len
  }

  private def writeString(s: String, from: Int, to: Int): Unit = {
    ensure((to - from) + 2) // 1 byte per char (suppose that they are ASCII only) + make room for the quotes
    var pos = count
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
    if (i < to) { // for the remaining parts we process with utf-8 encoding and escape unicode support
      count = pos
      writeStringSlowPath(s, i, to)
      pos = count
    }
    buf(pos) = '"'
    count = pos + 1
  }

  private def writeStringSlowPath(s: String, from: Int, to: Int): Unit = {
    val escapeUnicode = config.escapeUnicode
    ensure((to - from) * (if (escapeUnicode) 6 else 3) + 1) // max 6/3 bytes per char + the closing quotes
    var pos = count
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
    count = pos
  }

  private def toHexDigit(n: Int): Byte = {
    val n1 = n & 15
    n1 + (if (n1 > 9) 87 else 48)
  }.toByte

  private def illegalSurrogateError(ch: Int): Nothing = encodeError("illegal surrogate: \\u" + Integer.toHexString(ch))

  private def writeCommaWithParentheses(comma: Boolean): Unit = {
    if (comma) write(',')
    writeIndention()
    write('"')
  }

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) write('"'.toByte, ':'.toByte, ' '.toByte)
    else write('"'.toByte, ':'.toByte)

  private def writeInt(x: Int): Unit = {
    ensure(11) // minIntBytes.length
    var value = x
    var pos = count
    count =
      if (value == Integer.MIN_VALUE) {
        System.arraycopy(minIntBytes, 0, buf, pos, minIntBytes.length)
        pos + minIntBytes.length
      } else {
        if (value < 0) {
          value = -value
          buf(pos) = '-'
          pos += 1
        }
        val q1 = value / 1000
        if (q1 == 0) writeFirstBuf(buf, digits(value), pos)
        else {
          val r1 = value - q1 * 1000
          val q2 = q1 / 1000
          if (q2 == 0) {
            pos = writeFirstBuf(buf, digits(q1), pos)
            writeBuf(buf, digits(r1), pos)
            pos + 3
          } else {
            val r2 = q1 - q2 * 1000
            val q3 = q2 / 1000
            if (q3 == 0) pos = writeFirstBuf(buf, digits(q2), pos)
            else {
              val r3 = q2 - q3 * 1000
              buf(pos) = (q3 + '0').toByte
              writeBuf(buf, digits(r3), pos + 1)
              pos += 4
            }
            writeBuf(buf, digits(r2), pos)
            writeBuf(buf, digits(r1), pos + 3)
            pos + 6
          }
        }
      }
  }

  // TODO: consider more cache-aware algorithm from RapidJSON, see https://github.com/miloyip/itoa-benchmark/blob/master/src/branchlut.cpp
  private def writeLong(x: Long): Unit = {
    ensure(20) // minLongBytes.length
    var value = x
    var pos = count
    count =
      if (value == java.lang.Long.MIN_VALUE) {
        System.arraycopy(minLongBytes, 0, buf, pos, minLongBytes.length)
        pos + minLongBytes.length
      } else {
        if (value < 0) {
          value = -value
          buf(pos) = '-'
          pos += 1
        }
        val q1 = value / 1000
        if (q1 == 0) writeFirstBuf(buf, digits(value.toInt), pos)
        else {
          val r1 = (value - q1 * 1000).toInt
          val q2 = q1 / 1000
          if (q2 == 0) {
            pos = writeFirstBuf(buf, digits(q1.toInt), pos)
            writeBuf(buf, digits(r1), pos)
            pos + 3
          } else {
            val r2 = (q1 - q2 * 1000).toInt
            val q3 = q2 / 1000
            if (q3 == 0) {
              pos = writeFirstBuf(buf, digits(q2.toInt), pos)
              writeBuf(buf, digits(r2), pos)
              writeBuf(buf, digits(r1), pos + 3)
              pos + 6
            } else {
              val r3 = (q2 - q3 * 1000).toInt
              val q4 = (q3 / 1000).toInt
              if (q4 == 0) {
                pos = writeFirstBuf(buf, digits(q3.toInt), pos)
                writeBuf(buf, digits(r3), pos)
                writeBuf(buf, digits(r2), pos + 3)
                writeBuf(buf, digits(r1), pos + 6)
                pos + 9
              } else {
                val r4 = (q3 - q4 * 1000).toInt
                val q5 = q4 / 1000
                if (q5 == 0) {
                  pos = writeFirstBuf(buf, digits(q4), pos)
                  writeBuf(buf, digits(r4), pos)
                  writeBuf(buf, digits(r3), pos + 3)
                  writeBuf(buf, digits(r2), pos + 6)
                  writeBuf(buf, digits(r1), pos + 9)
                  pos + 12
                } else {
                  val r5 = q4 - q5 * 1000
                  val q6 = q5 / 1000
                  if (q6 == 0) pos = writeFirstBuf(buf, digits(q5), pos)
                  else {
                    val r6 = q5 - q6 * 1000
                    buf(pos) = (q6 + '0').toByte
                    writeBuf(buf, digits(r6), pos + 1)
                    pos += 4
                  }
                  writeBuf(buf, digits(r5), pos)
                  writeBuf(buf, digits(r4), pos + 3)
                  writeBuf(buf, digits(r3), pos + 6)
                  writeBuf(buf, digits(r2), pos + 9)
                  writeBuf(buf, digits(r1), pos + 12)
                  pos + 15
                }
              }
            }
          }
        }
      }
  }

  private def writeFirstBuf(buf: Array[Byte], v: Int, p: Int): Int = {
    var pos = p
    val start = v >> 24
    if (start == 0) {
      buf(pos) = (v >> 16).toByte
      buf(pos + 1) = (v >> 8).toByte
      pos += 2
    } else if (start == 1) {
      buf(pos) = (v >> 8).toByte
      pos += 1
    }
    buf(pos) = v.toByte
    pos + 1
  }

  private def writeBuf(buf: Array[Byte], v: Int, pos: Int): Unit = {
    buf(pos) = (v >> 16).toByte
    buf(pos + 1) = (v >> 8).toByte
    buf(pos + 2) = v.toByte
  }

  private def writeFloat(x: Float): Unit =
    if (java.lang.Float.isFinite(x)) writeAsciiString(java.lang.Float.toString(x))
    else encodeError("illegal number: " + x)

  // TODO: use more efficient algorithm from RapidJSON, see https://github.com/miloyip/dtoa-benchmark
  private def writeDouble(x: Double): Unit =
    if (java.lang.Double.isFinite(x)) writeAsciiString(java.lang.Double.toString(x))
    else encodeError("illegal number: " + x)

  private def writeIndention(delta: Int = 0): Unit =
    if (indention != 0) {
      write('\n')
      val toWrite = indention - delta
      ensure(toWrite)
      var i = 0
      while (i < toWrite && count < buf.length) {
        buf(count) = ' '
        count += 1
        i += 1
      }
    }

  private def flushBuffer(): Unit =
    if (out ne null) {
      out.write(buf, 0, count)
      count = 0
    }

  @inline
  private def ensure(minimal: Int): Unit = if (buf.length < count + minimal) growBuffer(minimal)

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
  private val digits = new Array[Int](1000)
  private val minIntBytes = "-2147483648".getBytes
  private val minLongBytes = "-9223372036854775808".getBytes

  {
    var i = 0
    while (i < 1000) {
      digits(i) =
        (if (i < 10) 2 << 24
        else if (i < 100) 1 << 24
        else 0) + (((i / 100) + '0') << 16) + ((((i / 10) % 10) + '0') << 8) + i % 10 + '0'
      i += 1
    }
  }

  final def write[A](codec: JsonCodec[A], obj: A, out: OutputStream): Unit = {
    val writer = pool.get
    writer.reset(out)
    try codec.write(obj, writer)
    finally writer.close()
  }

  final def write[A](codec: JsonCodec[A], obj: A): Array[Byte] = {
    val writer = pool.get
    writer.reset(null)
    codec.write(obj, writer)
    val arr = new Array[Byte](writer.count)
    System.arraycopy(writer.buf, 0, arr, 0, arr.length)
    arr
  }

  final def write[A](codec: JsonCodec[A], obj: A, out: OutputStream, cfg: WriterConfig): Unit = {
    val writer = pool.get
    val currCfg = writer.config
    writer.reset(out)
    writer.config = cfg
    try codec.write(obj, writer)
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
    try codec.write(obj, writer)
    finally writer.config = currCfg
    val arr = new Array[Byte](writer.count)
    System.arraycopy(writer.buf, 0, arr, 0, arr.length)
    arr
  }
}