package com.github.plokhotnyuk.jsoniter_scala

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.JsonWriter.{escapedChars, _}

import scala.annotation.{switch, tailrec}
import scala.collection.breakOut

case class WriterConfig(
    indentionStep: Int = 0,
    escapeUnicode: Boolean = false,
    preferredBufSize: Int = 16384)

final class JsonWriter private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](1024),
    private[this] var count: Int = 0,
    private[this] var indention: Int = 0,
    private[this] var comma: Boolean = false,
    private[this] var isBufGrowingAllowed: Boolean = true,
    private[this] var out: OutputStream = null,
    private[this] var config: WriterConfig = null) {
  def writeComma(): Unit = {
    if (comma) writeBytes(',')
    else comma = true
    writeIndention(0)
  }

  def writeKey(x: Boolean): Unit = {
    writeCommaWithParentheses()
    writeVal(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Byte): Unit = {
    writeCommaWithParentheses()
    writeInt(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Char): Unit = {
    writeComma()
    writeChar(x)
    writeColon()
  }

  def writeKey(x: Short): Unit = {
    writeCommaWithParentheses()
    writeInt(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Int): Unit = {
    writeCommaWithParentheses()
    writeInt(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Long): Unit = {
    writeCommaWithParentheses()
    writeLong(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Float): Unit = {
    writeCommaWithParentheses()
    writeFloat(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Double): Unit = {
    writeCommaWithParentheses()
    writeDouble(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: BigInt): Unit =
    if (x ne null) {
      writeCommaWithParentheses()
      writeNonEscapedAsciiStringWithoutParentheses(x.toString)
      writeParenthesesWithColon()
    } else encodeError("key cannot be null")

  def writeKey(x: BigDecimal): Unit =
    if (x ne null) {
      writeCommaWithParentheses()
      writeNonEscapedAsciiStringWithoutParentheses(x.toString)
      writeParenthesesWithColon()
    } else encodeError("key cannot be null")

  def writeKey(x: String): Unit =
    if (x ne null) {
      writeComma()
      writeString(x)
      writeColon()
    } else encodeError("key cannot be null")

  def writeNonEscapedAsciiKey(x: String): Unit =
    if (x ne null) {
      writeComma()
      writeNonEscapedAsciiString(x)
      writeColon()
    } else encodeError("key cannot be null")

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(x: BigDecimal): Unit =
    if (x eq null) writeNull() else writeNonEscapedAsciiStringWithoutParentheses(x.toString)

  def writeVal(x: BigInt): Unit =
    if (x eq null) writeNull() else writeNonEscapedAsciiStringWithoutParentheses(x.toString)

  def writeVal(x: String): Unit = if (x eq null) writeNull() else writeString(x)

  def writeNonEscapedAsciiVal(x: String): Unit = if (x eq null) writeNull() else writeNonEscapedAsciiString(x)

  def writeVal(x: Boolean): Unit = if (x) writeBytes('t', 'r', 'u', 'e') else writeBytes('f', 'a', 'l', 's', 'e')

  def writeVal(x: Byte): Unit = writeInt(x.toInt)

  def writeVal(x: Short): Unit = writeInt(x.toInt)

  def writeVal(x: Char): Unit = writeChar(x)

  def writeVal(x: Int): Unit = writeInt(x)

  def writeVal(x: Long): Unit = writeLong(x)

  def writeVal(x: Float): Unit = writeFloat(x)

  def writeVal(x: Double): Unit = writeDouble(x)

  def writeNull(): Unit = writeBytes('n', 'u', 'l', 'l')

  def writeArrayStart(): Unit = writeNestedStart('[')

  def writeArrayEnd(): Unit = writeNestedEnd(']')

  def writeObjectStart(): Unit = writeNestedStart('{')

  def writeObjectEnd(): Unit = writeNestedEnd('}')

  private def write[A](codec: JsonCodec[A], x: A, out: OutputStream, config: WriterConfig): Unit = {
    if ((out eq null) || (config eq null)) throw new NullPointerException
    this.config = config
    this.out = out
    count = 0
    indention = 0
    try {
      codec.encode(x, this) // also checks that `codec` is not null before any serialization
      flushBuffer() // do not flush buffer in case of exception during encoding to avoid hiding it by possible new one
    } finally {
      this.out = null // do not close output stream, just help GC instead
      freeTooLongBuf()
    }
  }

  private def write[A](codec: JsonCodec[A], x: A, config: WriterConfig): Array[Byte] = {
    if (config eq null) throw new NullPointerException
    this.config = config
    this.count = 0
    this.indention = 0
    try {
      codec.encode(x, this) // also checks that `codec` is not null before any serialization
      val arr = new Array[Byte](count)
      System.arraycopy(buf, 0, arr, 0, arr.length)
      arr
    } finally freeTooLongBuf()
  }

  private def write[A](codec: JsonCodec[A], x: A, buf: Array[Byte], from: Int, config: WriterConfig): Int = {
    if (config eq null) throw new NullPointerException
    if (from > buf.length || from < 0) // also checks that `buf` is not null before any serialization
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `buf` length")
    val currBuf = this.buf
    this.config = config
    this.buf = buf
    count = from
    indention = 0
    isBufGrowingAllowed = false
    try {
      codec.encode(x, this) // also checks that `codec` is not null before any serialization
      count
    } finally {
      this.buf = currBuf
      isBufGrowingAllowed = true
    }
  }

  private def writeNestedStart(b: Byte): Unit = {
    indention += config.indentionStep
    comma = false
    writeBytes(b)
  }

  private def writeNestedEnd(b: Byte): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    comma = true
    writeBytes(b)
  }

  private def writeBytes(b: Byte): Unit = count = {
    val pos = ensureBufferCapacity(1)
    buf(pos) = b
    pos + 1
  }

  private def writeBytes(b1: Byte, b2: Byte): Unit = count = {
    val pos = ensureBufferCapacity(2)
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  private def writeBytes(b1: Byte, b2: Byte, b3: Byte): Unit = count = {
    val pos = ensureBufferCapacity(3)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  private def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = count = {
    val pos = ensureBufferCapacity(4)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  private def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = count = {
    val pos = ensureBufferCapacity(5)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    pos + 5
  }

  private def writeNonEscapedAsciiStringWithoutParentheses(s: String): Unit = count = {
    val len = s.length
    val pos = ensureBufferCapacity(len)
    s.getBytes(0, len, buf, pos)
    pos + len
  }

  private def writeNonEscapedAsciiString(s: String): Unit = count = {
    val len = s.length
    val pos = ensureBufferCapacity(len + 2)
    buf(pos) = '"'
    s.getBytes(0, len, buf, pos + 1)
    buf(pos + len + 1) = '"'
    pos + len + 2
  }

  private def writeString(s: String): Unit = count = {
    var pos = ensureBufferCapacity(2)
    buf(pos) = '"'
    pos = {
      val bs = UnsafeUtils.getLatin1Array(s)
      if (bs eq null) writeString(s, 0, s.length, pos + 1, buf.length, escapedChars)
      else writeString(bs, 0, s.length, pos + 1, buf.length, escapedChars)
    }
    buf(pos) = '"'
    pos + 1
  }

  @tailrec
  private def writeString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 2 > posLim) writeString(s, from, to, growBuffer(2, pos), buf.length, escapedChars)
    else {
      val ch = s.charAt(from)
      if (ch < 128 && escapedChars(ch) == 0) {
        buf(pos) = ch.toByte
        writeString(s, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(s, from, to, pos, posLim, escapedChars)
      else writeEncodedString(s, from, to, pos, posLim, escapedChars)
    }

  @tailrec
  private def writeEncodedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 7 > posLim) writeEncodedString(s, from, to, growBuffer(7, pos), buf.length, escapedChars)
    else {
      val ch1 = s.charAt(from)
      if (ch1 < 128) { // 1 byte, 7 bits: 0xxxxxxx
        val esc = escapedChars(ch1)
        if (esc == 0) {
          buf(pos) = ch1.toByte
          writeEncodedString(s, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEncodedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos), posLim, escapedChars)
      } else if (ch1 < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (ch1 >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch1 & 0x3F)).toByte
        writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
      } else if (ch1 < 0xD800 || ch1 > 0xDFFF) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        buf(pos) = (0xE0 | (ch1 >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch1 >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch1 & 0x3F)).toByte
        writeEncodedString(s, from + 1, to, pos + 3, posLim, escapedChars)
      } else { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        if (ch1 >= 0xDC00 || from + 1 >= to) illegalSurrogateError()
        val ch2 = s.charAt(from + 1)
        if (ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        val cp = Character.toCodePoint(ch1, ch2)
        buf(pos) = (0xF0 | (cp >> 18)).toByte
        buf(pos + 1) = (0x80 | ((cp >> 12) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | ((cp >> 6) & 0x3F)).toByte
        buf(pos + 3) = (0x80 | (cp & 0x3F)).toByte
        writeEncodedString(s, from + 2, to, pos + 4, posLim, escapedChars)
      }
    }

  @tailrec
  private def writeEscapedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 13 > posLim) writeEscapedString(s, from, to, growBuffer(13, pos), buf.length, escapedChars)
    else {
      val ch1 = s.charAt(from)
      if (ch1 < 128) {
        val esc = escapedChars(ch1)
        if (esc == 0) {
          buf(pos) = ch1.toByte
          writeEscapedString(s, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEscapedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos), posLim, escapedChars)
      } else if (ch1 < 0xD800 || ch1 > 0xDFFF) {
        writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1, pos), posLim, escapedChars)
      } else {
        if (ch1 >= 0xDC00 || from + 1 >= to) illegalSurrogateError()
        val ch2 = s.charAt(from + 1)
        if (ch1 >= 0xDC00 || ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        writeEscapedString(s, from + 2, to, writeEscapedUnicode(ch2, writeEscapedUnicode(ch1, pos)), posLim, escapedChars)
      }
    }

  @tailrec
  private def writeString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 2 > posLim) writeString(bs, from, to, growBuffer(2, pos), buf.length, escapedChars)
    else {
      val b = bs(from)
      if (b >= 0 && escapedChars(b) == 0) {
        buf(pos) = b
        writeString(bs, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(bs, from, to, pos, posLim, escapedChars)
      else writeEncodedString(bs, from, to, pos, posLim, escapedChars)
    }

  @tailrec
  private def writeEncodedString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 7 > posLim) writeEncodedString(bs, from, to, growBuffer(7, pos), buf.length, escapedChars)
    else {
      val b = bs(from)
      if (b >= 0) { // 1 byte, 7 bits: 0xxxxxxx
        val esc = escapedChars(b)
        if (esc == 0) {
          buf(pos) = b
          writeEncodedString(bs, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEncodedString(bs, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEncodedString(bs, from + 1, to, writeEscapedUnicode(b, pos), posLim, escapedChars)
      } else { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | ((b & 0xFF) >> 6)).toByte
        buf(pos + 1) = (0x80 | (b & 0x3F)).toByte
        writeEncodedString(bs, from + 1, to, pos + 2, posLim, escapedChars)
      }
    }

  @tailrec
  private def writeEscapedString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos + 7 > posLim) writeEscapedString(bs, from, to, growBuffer(7, pos), buf.length, escapedChars)
    else {
      val b = bs(from)
      if (b >= 0) {
        val esc = escapedChars(b)
        if (esc == 0) {
          buf(pos) = b
          writeEscapedString(bs, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEscapedString(bs, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEscapedString(bs, from + 1, to, writeEscapedUnicode(b, pos), posLim, escapedChars)
      } else writeEscapedString(bs, from + 1, to, writeEscapedUnicode(b, pos), posLim, escapedChars)
    }

  private def writeChar(ch: Char): Unit = count = {
    var pos = ensureBufferCapacity(8) // 6 bytes per char for escaped unicode + make room for the quotes
    buf(pos) = '"'
    pos += 1
    pos = {
      if (ch < 128) { // 1 byte, 7 bits: 0xxxxxxx
        val esc = escapedChars(ch)
        if (esc == 0) {
          buf(pos) = ch.toByte
          pos + 1
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          pos + 2
        } else writeEscapedUnicode(ch.toByte, pos)
      } else if (config.escapeUnicode) {
        if (ch >= 0xD800 && ch <= 0xDFFF) illegalSurrogateError()
        writeEscapedUnicode(ch, pos)
      } else if (ch < 2048) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        buf(pos) = (0xC0 | (ch >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch & 0x3F)).toByte
        pos + 2
      } else if (ch < 0xD800 || ch > 0xDFFF) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        buf(pos) = (0xE0 | (ch >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch & 0x3F)).toByte
        pos + 3
      } else illegalSurrogateError()
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeEscapedUnicode(ch: Char, pos: Int): Int = {
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = toHexDigit(ch >>> 12)
    buf(pos + 3) = toHexDigit(ch >>> 8)
    buf(pos + 4) = toHexDigit(ch >>> 4)
    buf(pos + 5) = toHexDigit(ch)
    pos + 6
  }

  private def writeEscapedUnicode(b: Byte, pos: Int): Int = {
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = '0'
    buf(pos + 3) = '0'
    buf(pos + 4) = toHexDigit(b >>> 4)
    buf(pos + 5) = toHexDigit(b)
    pos + 6
  }

  private def toHexDigit(n: Int): Byte = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toByte

  private def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private def writeCommaWithParentheses(): Unit = {
    if (comma) writeBytes(',')
    else comma = true
    writeIndention(0)
    writeBytes('"')
  }

  private def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) writeBytes('"', ':', ' ')
    else writeBytes('"', ':')

  private def writeColon(): Unit =
    if (config.indentionStep > 0) writeBytes(':', ' ')
    else writeBytes(':')

  private def writeInt(x: Int): Unit = count = {
    var pos = ensureBufferCapacity(11) // minIntBytes.length
    if (x == Integer.MIN_VALUE) writeByteArray(minIntBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      if (q0 < 1000) writeFirstRem(q0, pos)
      else {
        val q1 = q0 / 1000
        val r1 = q0 - q1 * 1000
        if (q1 < 1000) writeRem(r1, writeFirstRem(q1, pos))
        else {
          val q2 = q1 / 1000
          val r2 = q1 - q2 * 1000
          writeRem(r1, writeRem(r2, {
            if (q2 < 1000) writeFirstRem(q2, pos)
            else {
              val q3 = q2 / 1000
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

  private def writeLong(x: Long): Unit = count = {
    var pos = ensureBufferCapacity(20) // minLongBytes.length
    if (x == java.lang.Long.MIN_VALUE) writeByteArray(minLongBytes, pos)
    else {
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      if (q0 < 1000) writeFirstRem(q0.toInt, pos)
      else {
        val q1 = q0 / 1000
        val r1 = (q0 - q1 * 1000).toInt
        if (q1 < 1000) writeRem(r1, writeFirstRem(q1.toInt, pos))
        else {
          val q2 = q1 / 1000
          val r2 = (q1 - q2 * 1000).toInt
          if (q2 < 1000) writeRem(r1, writeRem(r2, writeFirstRem(q2.toInt, pos)))
          else {
            val q3 = q2 / 1000
            val r3 = (q2 - q3 * 1000).toInt
            if (q3 < 1000) writeRem(r1, writeRem(r2, writeRem(r3, writeFirstRem(q3.toInt, pos))))
            else {
              val q4 = (q3 / 1000).toInt
              val r4 = (q3 - q4 * 1000).toInt
              if (q4 < 1000) writeRem(r1, writeRem(r2, writeRem(r3, writeRem(r4, writeFirstRem(q4, pos)))))
              else {
                val q5 = q4 / 1000
                val r5 = q4 - q5 * 1000
                writeRem(r1, writeRem(r2, writeRem(r3, writeRem(r4, writeRem(r5, {
                  if (q5 < 1000) writeFirstRem(q5, pos)
                  else {
                    val q6 = q5 / 1000
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

  private def writeByteArray(bs: Array[Byte], pos: Int): Int = {
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
    if (java.lang.Float.isFinite(x)) writeNonEscapedAsciiStringWithoutParentheses(java.lang.Float.toString(x))
    else encodeError("illegal number: " + x)

  private def writeDouble(x: Double): Unit =
    if (java.lang.Double.isFinite(x)) writeNonEscapedAsciiStringWithoutParentheses(java.lang.Double.toString(x))
    else encodeError("illegal number: " + x)

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

  private def ensureBufferCapacity(required: Int): Int = {
    val pos = count
    if (buf.length < pos + required) growBuffer(required, pos)
    else pos
  }

  private def growBuffer(required: Int, pos: Int): Int = {
    val newPos = flushBuffer(pos)
    if (buf.length < pos + required) {
      if (isBufGrowingAllowed) {
        val bs = new Array[Byte](Math.max(buf.length << 1, pos + required))
        System.arraycopy(buf, 0, bs, 0, buf.length)
        buf = bs
      } else throw new ArrayIndexOutOfBoundsException("`buf` length exceeded")
    }
    newPos
  }

  private[jsoniter_scala] def flushBuffer(): Unit = count = flushBuffer(count)

  private def flushBuffer(pos: Int): Int =
    if (out eq null) pos
    else {
      out.write(buf, 0, pos)
      0
    }

  private def freeTooLongBuf(): Unit =
    if (buf.length > config.preferredBufSize) buf = new Array[Byte](config.preferredBufSize)
}

object JsonWriter {
  private val pool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter
  }
  private val defaultConfig = new WriterConfig
  private val escapedChars: Array[Byte] = (0 to 127).map { b =>
    ((b: @switch) match {
      case '\n' => 'n'
      case '\r' => 'r'
      case '\t' => 't'
      case '\b' => 'b'
      case '\f' => 'f'
      case '\\' => '\\'
      case '\"' => '"'
      case x if x <= 31 || x >= 127 => -1 // hex escaped chars
      case _ => 0 // non-escaped chars
    }).toByte
  }(breakOut)
  private val digits: Array[Short] = (0 to 999).map { i =>
    (((if (i < 10) 2 else if (i < 100) 1 else 0) << 12) + // this nibble encodes number of leading zeroes
      ((i / 100) << 8) + (((i / 10) % 10) << 4) + i % 10).toShort // decimal digit per nibble
  }(breakOut)
  private val minIntBytes: Array[Byte] = "-2147483648".getBytes
  private val minLongBytes: Array[Byte] = "-9223372036854775808".getBytes

  final def isNonEscapedAscii(ch: Char): Boolean = ch < 128 && escapedChars(ch) == 0

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
  final def write[A](codec: JsonCodec[A], x: A, out: OutputStream): Unit = pool.get.write(codec, x, out, defaultConfig)

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
  final def write[A](codec: JsonCodec[A], x: A, out: OutputStream, config: WriterConfig): Unit =
    pool.get.write(codec, x, out, config)

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
  final def write[A](codec: JsonCodec[A], x: A): Array[Byte] = pool.get.write(codec, x, defaultConfig)

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
  final def write[A](codec: JsonCodec[A], x: A, config: WriterConfig): Array[Byte] = pool.get.write(codec, x, config)

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
  final def write[A](codec: JsonCodec[A], x: A, buf: Array[Byte], from: Int, config: WriterConfig = defaultConfig): Int =
    pool.get.write(codec, x, buf, from, config)
}