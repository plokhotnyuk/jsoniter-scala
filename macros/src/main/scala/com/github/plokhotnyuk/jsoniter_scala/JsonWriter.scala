package com.github.plokhotnyuk.jsoniter_scala

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.JsonWriter._

case class WriterConfig(
  indentionStep: Int = 0,
  escapeUnicode: Boolean = false)

final class JsonWriter private[jsoniter_scala](
    private var buf: Array[Byte],
    private var count: Int,
    private var indention: Int,
    private var out: OutputStream,
    private var config: WriterConfig) {
  require(buf != null && buf.length > 32, "buf size should be greater than 32")

  def reset(out: OutputStream): Unit = {
    this.out = out
    count = 0
  }

  def close(): Unit =
    if (out != null) {
      if (count > 0) flushBuffer()
      out.close()
      out = null
      count = 0
    }

  def writeSep(first: Boolean): Boolean = {
    if (first) writeIndention()
    else writeMore()
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

  def writeObjectField(field: String): Unit =
    if (field ne null) {
      writeString(field)
      if (indention > 0) write(':'.toByte, ' '.toByte)
      else write(':')
    } else encodeError("key cannot be null")

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(v: BigDecimal): Unit = if (v eq null) writeNull() else writeAsciiString(v.toString())

  def writeVal(v: BigInt): Unit = if (v eq null) writeNull() else writeAsciiString(v.toString())

  def writeVal(v: String): Unit = if (v eq null) writeNull() else writeString(v)

  def writeVal(v: Boolean): Unit =
    if (v) write('t'.toByte, 'r'.toByte, 'u'.toByte, 'e'.toByte)
    else write('f'.toByte, 'a'.toByte, 'l'.toByte, 's'.toByte, 'e'.toByte)

  def writeVal(v: Byte): Unit = writeInt(v.toInt)

  def writeVal(v: Short): Unit = writeInt(v.toInt)

  def writeVal(v: Char): Unit = writeInt(v.toInt)

  def writeVal(v: Int): Unit = writeInt(v)

  def writeVal(v: Long): Unit = writeLong(v)

  def writeVal(v: Float): Unit = writeFloat(v)

  def writeVal(v: Double): Unit = writeDouble(v)

  def writeNull(): Unit = write('n'.toByte, 'u'.toByte, 'l'.toByte, 'l'.toByte)

  def writeArrayStart(): Unit = {
    indention += config.indentionStep
    write('[')
  }

  def writeMore(): Unit = {
    write(',')
    writeIndention()
  }

  def writeIndention(delta: Int = 0): Unit =
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

  def writeArrayEnd(): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    write(']')
  }

  def writeObjectStart(): Unit = {
    val indentionStep = config.indentionStep
    indention += indentionStep
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

  private def writeAsciiString(v: String): Unit = {
    var remaining = v.length
    if (out == null) {
      ensure(remaining)
      v.getBytes(0, remaining, buf, count)
      count += remaining
    } else {
      var i = 0
      var continue = true
      while (continue) {
        val available = buf.length - count
        if (available < remaining) {
          remaining -= available
          val j = i + available
          v.getBytes(i, j, buf, count)
          count = buf.length
          flushBuffer()
          i = j
        } else {
          val j = i + remaining
          v.getBytes(i, j, buf, count)
          count += remaining
          continue = false
        }
      }
    }
  }

  private def writeString(v: String): Unit = {
    var i = 0
    val len = v.length
    var toWriteLen = len
    val bufLengthMinusTwo = buf.length - 2 // make room for the quotes
    if (count + toWriteLen > bufLengthMinusTwo) toWriteLen = bufLengthMinusTwo - count
    if (toWriteLen < 0) {
      ensure(32)
      if (count + toWriteLen > bufLengthMinusTwo) toWriteLen = bufLengthMinusTwo - count
    }
    var n: Int = count
    buf(n) = '"'
    n += 1
    try { // write string, the fast path, without utf8 and escape support
      var continue = true
      while (continue && i < toWriteLen) {
        val c = v.charAt(i)
        i += 1
        if (c > 31 && c < 126 && c != '"' && c != '\\') {
          buf(n) = c.toByte
          n += 1
        } else {
          continue = false
          i -= 1
        }
      }
    } catch {
      case e: ArrayIndexOutOfBoundsException => // FIXME: not sure that it is efficient in deep calls
    }
    if (i == len) {
      buf(n) = '"'
      count = n + 1
    } else {
      count = n
      writeStringSlowPath(v, i, len) // for the remaining parts, we process them char by char
      write('"')
    }
  }

  private def writeStringSlowPath(v: String, start: Int, len: Int): Unit =
    if (config.escapeUnicode) {
      var i = start
      while (i < len) {
        val c: Int = v.charAt(i)
        i += 1
        if (c > 125) writeAsSlashU(c)
        else writeAsciiChar(c)
      }
    } else writeStringSlowPathWithoutEscapeUnicode(v, start, len)

  private def writeStringSlowPathWithoutEscapeUnicode(v: String, start: Int, len: Int): Unit = {
    var i = start
    var surrogate = 0
    var continue = true
    while (continue && i < len) {
      var ch: Int = v.charAt(i)
      i += 1
      if (ch > 125) {
        if (ch < 0x800) { // 2-byte
          write((0xc0 | (ch >> 6)).toByte, (0x80 | (ch & 0x3f)).toByte)
        } else if (ch < 0xD800 || ch > 0xDFFF) {
          write((0xe0 | (ch >> 12)).toByte, (0x80 | ((ch >> 6) & 0x3f)).toByte, (0x80 | (ch & 0x3f)).toByte)
        } else {
          if (ch > 0xDBFF) encodeError("illegalSurrogate")
          surrogate = ch
          if (i > len) {
            continue = false
            i -= 1
          } else {
            val firstPart = surrogate
            surrogate = 0
            if (ch < 0xDC00 || ch > 0xDFFF) encodeError("Broken surrogate pair: first char 0x" + Integer.toHexString(firstPart) + ", second 0x" + Integer.toHexString(ch) + "; illegal combination")
            ch = 0x10000 + ((firstPart - 0xD800) << 10) + (ch - 0xDC00)
            if (ch > 0x10FFFF) encodeError("illegalSurrogate")
            write((0xf0 | (ch >> 18)).toByte, (0x80 | ((ch >> 12) & 0x3f)).toByte, (0x80 | ((ch >> 6) & 0x3f)).toByte, (0x80 | (ch & 0x3f)).toByte)
          }
        }
      } else writeAsciiChar(ch)
    }
  }

  private def writeAsciiChar(ch: Int): Unit =
    ch match {
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

  private def writeInt(v: Int): Unit = {
    ensure(12)
    var value = v
    var pos = count
    if (value == Integer.MIN_VALUE) {
      System.arraycopy(minIntBytes, 0, buf, pos, minIntBytes.length)
      count = pos + minIntBytes.length
    } else {
      if (value < 0) {
        value = -value
        buf(pos) = '-'
        pos += 1
      }
      val q1 = value / 1000
      if (q1 == 0) {
        pos += writeFirstBuf(buf, digits(value), pos)
        count = pos
      } else {
        val r1 = value - q1 * 1000
        val q2 = q1 / 1000
        if (q2 == 0) {
          val v1 = digits(r1)
          val v2 = digits(q1)
          val off = writeFirstBuf(buf, v2, pos)
          writeBuf(buf, v1, pos + off)
          count = pos + 3 + off
        } else {
          val r2 = q1 - q2 * 1000
          val q3 = q2 / 1000
          val v1 = digits(r1)
          val v2 = digits(r2)
          if (q3 == 0) pos += writeFirstBuf(buf, digits(q2), pos)
          else {
            val r3: Int = q2 - q3 * 1000
            buf(pos) = (q3 + '0').toByte
            pos += 1
            writeBuf(buf, digits(r3), pos)
            pos += 3
          }
          writeBuf(buf, v2, pos)
          writeBuf(buf, v1, pos + 3)
          count = pos + 6
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
    3 - start
  }

  private def writeLong(v: Long): Unit = {
    ensure(22)
    var value = v
    var pos = count
    if (value == java.lang.Long.MIN_VALUE) {
      System.arraycopy(minLongBytes, 0, buf, pos, minLongBytes.length)
      count = pos + minLongBytes.length
    } else {
      if (value < 0) {
        value = -value
        buf(pos) = '-'
        pos += 1
      }
      val q1 = value / 1000
      if (q1 == 0) {
        pos += writeFirstBuf(buf, digits(value.toInt), pos)
        count = pos
      } else {
        val r1 = (value - q1 * 1000).toInt
        val q2 = q1 / 1000
        if (q2 == 0) {
          val v1 = digits(r1)
          val v2 = digits(q1.toInt)
          val off = writeFirstBuf(buf, v2, pos)
          writeBuf(buf, v1, pos + off)
          count = pos + 3 + off
        } else {
          val r2 = (q1 - q2 * 1000).toInt
          val q3 = q2 / 1000
          if (q3 == 0) {
            val v1 = digits(r1)
            val v2 = digits(r2)
            val v3 = digits(q2.toInt)
            pos += writeFirstBuf(buf, v3, pos)
            writeBuf(buf, v2, pos)
            writeBuf(buf, v1, pos + 3)
            count = pos + 6
          } else {
            val r3 = (q2 - q3 * 1000).toInt
            val q4 = (q3 / 1000).toInt
            if (q4 == 0) {
              val v1 = digits(r1)
              val v2 = digits(r2)
              val v3 = digits(r3)
              val v4 = digits(q3.toInt)
              pos += writeFirstBuf(buf, v4, pos)
              writeBuf(buf, v3, pos)
              writeBuf(buf, v2, pos + 3)
              writeBuf(buf, v1, pos + 6)
              count = pos + 9
            } else {
              val r4 = (q3 - q4 * 1000).toInt
              val q5 = q4 / 1000
              if (q5 == 0) {
                val v1 = digits(r1)
                val v2 = digits(r2)
                val v3 = digits(r3)
                val v4 = digits(r4)
                val v5 = digits(q4)
                pos += writeFirstBuf(buf, v5, pos)
                writeBuf(buf, v4, pos)
                writeBuf(buf, v3, pos + 3)
                writeBuf(buf, v2, pos + 6)
                writeBuf(buf, v1, pos + 9)
                count = pos + 12
              } else {
                val r5 = q4 - q5 * 1000
                val q6 = q5 / 1000
                val v1 = digits(r1)
                val v2 = digits(r2)
                val v3 = digits(r3)
                val v4 = digits(r4)
                val v5 = digits(r5)
                if (q6 == 0) pos += writeFirstBuf(buf, digits(q5), pos)
                else {
                  val r6 = q5 - q6 * 1000
                  buf(pos) = (q6 + '0').toByte
                  pos += 1
                  writeBuf(buf, digits(r6), pos)
                  pos += 3
                }
                writeBuf(buf, v5, pos)
                writeBuf(buf, v4, pos + 3)
                writeBuf(buf, v3, pos + 6)
                writeBuf(buf, v2, pos + 9)
                writeBuf(buf, v1, pos + 12)
                count = pos + 15
              }
            }
          }
        }
      }
    }
  }

  def writeFloat(v: Float): Unit = {
    var value = v
    if (value < 0) {
      write('-')
      value = -value
    }
    if (value > 0x4ffffff) writeAsciiString(java.lang.Float.toString(value))
    else { // FIXME: serializing only 6 digits?
      val exp = 1000000
      val lval = (value * exp + 0.5).toLong
      writeLong(lval / exp)
      val fval = lval % exp
      if (fval != 0) {
        write('.')
        ensure(11)
        var p = 5
        while (p > 0 && fval < pow10(p)) {
          buf(count) = '0'
          count += 1
          p -= 1
        }
        writeLong(fval)
        while (buf(count - 1) == '0') count -= 1
      }
    }
  }

  private def writeDouble(v: Double): Unit = {
    var value = v
    if (value < 0) {
      value = -value
      write('-')
    }
    if (value > 0x4ffffff) writeAsciiString(java.lang.Double.toString(value))
    else { // FIXME: serializing only 6 digits?
      val exp = 1000000
      val lval = (value * exp + 0.5).toLong
      writeLong(lval / exp)
      val fval = lval % exp
      if (fval != 0) {
        write('.')
        ensure(11)
        var p = 5
        while (p > 0 && fval < pow10(p)) {
          buf(count) = '0'
          count += 1
          p -= 1
        }
        writeLong(fval)
        while (buf(count - 1) == '0') count -= 1
      }
    }
  }

  private def writeBuf(buf: Array[Byte], v: Int, pos: Int): Unit = {
    buf(pos) = (v >> 16).toByte
    buf(pos + 1) = (v >> 8).toByte
    buf(pos + 2) = v.toByte
  }

  private def flushBuffer(): Unit =
    if (out != null) {
      out.write(buf, 0, count)
      count = 0
    }

  private def ensure(minimal: Int): Unit = {
    val available = buf.length - count
    if (available < minimal) {
      if (count > 1024) flushBuffer()
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
      var stream = super.get()
      if (stream.buf.length > 32768) {
        stream = initialValue()
        set(stream)
      }
      stream
    }
  }
  private val digits = new Array[Int](1000)
  private val pow10 = Array(1, 10, 100, 1000, 10000, 100000, 1000000)
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