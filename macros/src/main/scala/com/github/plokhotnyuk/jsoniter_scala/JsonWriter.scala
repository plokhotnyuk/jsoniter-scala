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
  require((buf ne null) && buf.length > 32, "buf size should be greater than 32")

  def reset(out: OutputStream): Unit = {
    this.out = out
    count = 0
  }

  def close(): Unit =
    if (out ne null) {
      if (count > 0) flushBuffer()
      out.close()
      out = null
      count = 0
    }

  def writeSep(first: Boolean): Boolean = {
    if (!first) write(',')
    writeIndention()
    false
  }

  def writeObjectField(x: Boolean): Unit = {
    writeParentheses()
    writeVal(x)
    writeParenthesesWithColon()
  }

  def writeObjectField(x: Int): Unit = {
    writeParentheses()
    writeInt(x)
    writeParenthesesWithColon()
  }

  def writeObjectField(x: Long): Unit = {
    writeParentheses()
    writeLong(x)
    writeParenthesesWithColon()
  }

  def writeObjectField(x: Float): Unit = {
    writeParentheses()
    writeFloat(x)
    writeParenthesesWithColon()
  }

  def writeObjectField(x: Double): Unit = {
    writeParentheses()
    writeDouble(x)
    writeParenthesesWithColon()
  }

  def writeObjectField(x: BigInt): Unit =
    if (x ne null) {
      writeParentheses()
      writeAsciiString(x.toString)
      writeParenthesesWithColon()
    } else encodeError("key cannot be null")

  def writeObjectField(x: BigDecimal): Unit =
    if (x ne null) {
      writeParentheses()
      writeAsciiString(x.toString)
      writeParenthesesWithColon()
    } else encodeError("key cannot be null")

  def writeObjectField(x: String): Unit =
    if (x ne null) {
      writeString(x)
      if (indention > 0) write(':'.toByte, ' '.toByte)
      else write(':')
    } else encodeError("key cannot be null")

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(x: BigDecimal): Unit = if (x eq null) writeNull() else writeAsciiString(x.toString)

  def writeVal(x: BigInt): Unit = if (x eq null) writeNull() else writeAsciiString(x.toString)

  def writeVal(x: String): Unit = if (x eq null) writeNull() else writeString(x)

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

  private def write(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte): Unit = {
    ensure(6)
    buf(count) = b1
    buf(count + 1) = b2
    buf(count + 2) = b3
    buf(count + 3) = b4
    buf(count + 4) = b5
    buf(count + 5) = b6
    count += 6
  }

  private def writeAsciiString(s: String): Unit = {
    var remaining = s.length
    if (out == null) {
      ensure(remaining)
      s.getBytes(0, remaining, buf, count)
      count += remaining
    } else {
      var i = 0
      var continue = true
      while (continue) {
        val available = buf.length - count
        if (available < remaining) {
          remaining -= available
          val j = i + available
          s.getBytes(i, j, buf, count)
          count = buf.length
          flushBuffer()
          i = j
        } else {
          val j = i + remaining
          s.getBytes(i, j, buf, count)
          count += remaining
          continue = false
        }
      }
    }
  }

  private def writeString(s: String): Unit = {
    var i = 0
    val len = s.length
    var toWriteLen = len
    val bufLengthMinusTwo = buf.length - 2 // make room for the quotes
    if (count + toWriteLen > bufLengthMinusTwo) toWriteLen = bufLengthMinusTwo - count
    if (toWriteLen < 0) {
      ensure(32)
      if (count + toWriteLen > bufLengthMinusTwo) toWriteLen = bufLengthMinusTwo - count
    }
    var pos = count
    buf(pos) = '"'
    pos += 1
    try { // write string, the fast path, without utf8 and escape support
      var continue = true
      while (continue && i < toWriteLen) {
        val c = s.charAt(i)
        i += 1
        if (c > 31 && c < 126 && c != '"' && c != '\\') {
          buf(pos) = c.toByte
          pos += 1
        } else {
          continue = false
          i -= 1
        }
      }
    } catch {
      case e: ArrayIndexOutOfBoundsException => // FIXME: not sure that it is efficient in deep calls
    }
    if (i == len) {
      buf(pos) = '"'
      count = pos + 1
    } else {
      count = pos
      writeStringSlowPath(s, i, len) // for the remaining parts, we process them char by char
      write('"')
    }
  }

  private def writeStringSlowPath(s: String, start: Int, len: Int): Unit =
    if (config.escapeUnicode) {
      var i = start
      while (i < len) {
        val c: Int = s.charAt(i)
        i += 1
        if (c > 125) writeAsSlashU(c)
        else writeAsciiChar(c)
      }
    } else writeStringSlowPathWithoutEscapeUnicode(s, start, len)

  private def writeStringSlowPathWithoutEscapeUnicode(s: String, start: Int, len: Int): Unit = {
    var i = start
    var surrogate = 0
    var continue = true
    while (continue && i < len) {
      var ch: Int = s.charAt(i)
      i += 1
      if (ch > 125) {
        if (ch < 0x800) { // 2-byte
          write((0xc0 | (ch >> 6)).toByte, (0x80 | (ch & 0x3f)).toByte)
        } else if (ch < 0xD800 || ch > 0xDFFF) {
          write((0xe0 | (ch >> 12)).toByte, (0x80 | ((ch >> 6) & 0x3f)).toByte, (0x80 | (ch & 0x3f)).toByte)
        } else {
          if (ch > 0xDBFF) encodeError("illegal surrogate: " + ch)
          surrogate = ch
          if (i > len) {
            continue = false
            i -= 1
          } else {
            val firstPart = surrogate
            surrogate = 0
            if (ch < 0xDC00 || ch > 0xDFFF) encodeError("illegal surrogate pair: \\u" + Integer.toHexString(firstPart) + " \\u" + Integer.toHexString(ch))
            ch = 0x10000 + ((firstPart - 0xD800) << 10) + (ch - 0xDC00)
            if (ch > 0x10FFFF) encodeError("illegal surrogate")
            write((0xf0 | (ch >> 18)).toByte, (0x80 | ((ch >> 12) & 0x3f)).toByte, (0x80 | ((ch >> 6) & 0x3f)).toByte, (0x80 | (ch & 0x3f)).toByte)
          }
        }
      } else writeAsciiChar(ch)
    }
  }

  private def writeAsciiChar(ch: Int): Unit =
    (ch: @switch) match {
      case '"' => write('\\'.toByte, '"'.toByte)
      case '\\' => write('\\'.toByte, '\\'.toByte)
      case '\b' => write('\\'.toByte, 'b'.toByte)
      case '\f' => write('\\'.toByte, 'f'.toByte)
      case '\n' => write('\\'.toByte, 'n'.toByte)
      case '\r' => write('\\'.toByte, 'r'.toByte)
      case '\t' => write('\\'.toByte, 't'.toByte)
      case _ => if (ch < 32) writeAsSlashU(ch) else write(ch.toByte)
    }

  private def writeAsSlashU(ch: Int): Unit =
    write('\\'.toByte, 'u'.toByte, toHexDigit(ch >>> 12), toHexDigit(ch >>> 8), toHexDigit(ch >>> 4), toHexDigit(ch))

  private def toHexDigit(n: Int): Byte = {
    val n1 = n & 15
    n1 + (if (n1 > 9) 87 else 48)
  }.toByte

  private def writeParentheses(): Unit = write('"')

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) write('"'.toByte, ':'.toByte, ' '.toByte)
    else write('"'.toByte, ':'.toByte)

  private def writeInt(x: Int): Unit = {
    ensure(12)
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
    ensure(22)
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

  private def ensure(minimal: Int): Unit = {
    val available = buf.length - count
    if (available < minimal) {
      if (count > 8192) flushBuffer()
      growAtLeast(minimal)
    }
  }

  private def growAtLeast(minimal: Int): Unit = {
    val len = buf.length
    val newBuf = new Array[Byte](len + Math.max(len, minimal))
    System.arraycopy(buf, 0, newBuf, 0, len)
    buf = newBuf
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