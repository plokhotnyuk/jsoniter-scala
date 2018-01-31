package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{IOException, OutputStream}
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter.{escapedChars, _}

import scala.annotation.{switch, tailrec}
import scala.collection.breakOut
import scala.collection.JavaConverters._

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter]] that contains params for formatting of
  * output JSON and for tuning of preferred size for internal byte buffer that created on the writer instantiation and
  * reused in runtime for serialization of messages using [[java.io.OutputStream]].
  * <br/>
  * All configuration params already initialized to default values, but in some cases they should be altered:
  * <ul>
  * <li>turn on pretty printing by specifying of indention step that is greater than 0</li>
  * <li>turn on escaping of Unicode characters to serialize with only ASCII characters</li>
  * <li>increase preferred size of an internal byte buffer to reduce allocation rate of grown and then reduced buffers
  * when writing to [[java.io.OutputStream]] lot of large (>16Kb) [[scala.math.BigDecimal]], [[scala.math.BigInt]] or
  * others non escaped ASCII strings written using `JsonWriter.writeNonEscapedAsciiKey` or
  * `JsonWriter.writeNonEscapedAsciiVal` </li>
  * </ul>
  * @param indentionStep a size of indention for pretty-printed formatting or 0 for compact output
  * @param escapeUnicode a flag to turn on hexadecimal escaping of all non-ASCII chars
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when writing to
  *                         [[java.io.OutputStream]]
  */
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
    writeByte(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Char): Unit = {
    writeComma()
    writeChar(x)
    writeColon()
  }

  def writeKey(x: Short): Unit = {
    writeCommaWithParentheses()
    writeShort(x)
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
      writeNonEscapedAsciiStringWithoutParentheses(new java.math.BigDecimal(x.bigInteger).toPlainString)
      writeParenthesesWithColon()
    } else nullKeyError()

  def writeKey(x: BigDecimal): Unit =
    if (x ne null) {
      writeCommaWithParentheses()
      writeNonEscapedAsciiStringWithoutParentheses(x.toString)
      writeParenthesesWithColon()
    } else nullKeyError()

  def writeKey(x: UUID): Unit =
    if (x ne null) {
      writeComma()
      writeUUID(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: String): Unit =
    if (x ne null) {
      writeComma()
      writeString(x)
      writeColon()
    } else nullKeyError()

  def writeNonEscapedAsciiKey(x: String): Unit =
    if (x ne null) {
      writeComma()
      writeNonEscapedAsciiString(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: Duration): Unit =
    if (x ne null) {
      writeComma()
      writeNonEscapedAsciiString(x.toString)
      writeColon()
    } else nullKeyError()

  def writeKey(x: Instant): Unit =
    if (x ne null) {
      writeComma()
      writeInstant(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: LocalDate): Unit =
    if (x ne null) {
      writeComma()
      writeLocalDate(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: LocalDateTime): Unit =
    if (x ne null) {
      writeComma()
      writeLocalDateTime(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: LocalTime): Unit =
    if (x ne null) {
      writeComma()
      writeLocalTime(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: MonthDay): Unit =
    if (x ne null) {
      writeComma()
      writeMonthDay(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: OffsetDateTime): Unit =
    if (x ne null) {
      writeComma()
      writeOffsetDateTime(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: OffsetTime): Unit =
    if (x ne null) {
      writeComma()
      writeOffsetTime(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: Period): Unit =
    if (x ne null) {
      writeComma()
      writeNonEscapedAsciiString(x.toString)
      writeColon()
    } else nullKeyError()

  def writeKey(x: Year): Unit =
    if (x ne null) {
      writeCommaWithParentheses()
      writeInt(x.getValue)
      writeParenthesesWithColon()
    } else nullKeyError()

  def writeKey(x: YearMonth): Unit =
    if (x ne null) {
      writeComma()
      writeYearMonth(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: ZonedDateTime): Unit =
    if (x ne null) {
      writeComma()
      writeZonedDateTime(x)
      writeColon()
    } else nullKeyError()

  def writeKey(x: ZoneId): Unit =
    if (x ne null) {
      writeComma()
      writeNonEscapedAsciiString(x.getId)
      writeColon()
    } else nullKeyError()

  def writeKey(x: ZoneOffset): Unit =
    if (x ne null) {
      writeComma()
      writeZoneOffset(x)
      writeColon()
    } else nullKeyError()

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(x: BigDecimal): Unit =
    if (x eq null) writeNull() else writeNonEscapedAsciiStringWithoutParentheses(x.toString)

  def writeVal(x: BigInt): Unit =
    if (x eq null) writeNull()
    else writeNonEscapedAsciiStringWithoutParentheses(new java.math.BigDecimal(x.bigInteger).toPlainString)

  def writeVal(x: UUID): Unit = if (x eq null) writeNull() else writeUUID(x)

  def writeVal(x: String): Unit = if (x eq null) writeNull() else writeString(x)

  def writeNonEscapedAsciiVal(x: String): Unit = if (x eq null) writeNull() else writeNonEscapedAsciiString(x)

  def writeVal(x: Duration): Unit = if (x eq null) writeNull() else writeNonEscapedAsciiString(x.toString)

  def writeVal(x: Instant): Unit = if (x eq null) writeNull() else writeInstant(x)

  def writeVal(x: LocalDate): Unit = if (x eq null) writeNull() else writeLocalDate(x)

  def writeVal(x: LocalDateTime): Unit = if (x eq null) writeNull() else writeLocalDateTime(x)

  def writeVal(x: LocalTime): Unit = if (x eq null) writeNull() else writeLocalTime(x)

  def writeVal(x: MonthDay): Unit = if (x eq null) writeNull() else writeMonthDay(x)

  def writeVal(x: OffsetDateTime): Unit = if (x eq null) writeNull() else writeOffsetDateTime(x)

  def writeVal(x: OffsetTime): Unit = if (x eq null) writeNull() else writeOffsetTime(x)

  def writeVal(x: Period): Unit = if (x eq null) writeNull() else writeNonEscapedAsciiString(x.toString)

  def writeVal(x: Year): Unit = if (x eq null) writeNull() else writeInt(x.getValue)

  def writeVal(x: YearMonth): Unit = if (x eq null) writeNull() else writeYearMonth(x)

  def writeVal(x: ZonedDateTime): Unit = if (x eq null) writeNull() else writeZonedDateTime(x)

  def writeVal(x: ZoneId): Unit = if (x eq null) writeNull() else writeNonEscapedAsciiString(x.getId)

  def writeVal(x: ZoneOffset): Unit = if (x eq null) writeNull() else writeZoneOffset(x)

  def writeVal(x: Boolean): Unit = if (x) writeBytes('t', 'r', 'u', 'e') else writeBytes('f', 'a', 'l', 's', 'e')

  def writeVal(x: Byte): Unit = writeByte(x)

  def writeVal(x: Short): Unit = writeShort(x)

  def writeVal(x: Char): Unit = writeChar(x)

  def writeVal(x: Int): Unit = writeInt(x)

  def writeVal(x: Long): Unit = writeLong(x)

  def writeVal(x: Float): Unit = writeFloat(x)

  def writeVal(x: Double): Unit = writeDouble(x)

  def writeValAsString(x: BigDecimal): Unit =
    if (x eq null) writeNull() else writeNonEscapedAsciiString(x.toString)

  def writeValAsString(x: BigInt): Unit =
    if (x eq null) writeNull()
    else writeNonEscapedAsciiString(new java.math.BigDecimal(x.bigInteger).toPlainString)

  def writeValAsString(x: Boolean): Unit =
    if (x) writeBytes('"', 't', 'r', 'u', 'e', '"') else writeBytes('"', 'f', 'a', 'l', 's', 'e', '"')

  def writeValAsString(x: Byte): Unit = {
    writeBytes('"')
    writeInt(x.toInt)
    writeBytes('"')
  }

  def writeValAsString(x: Short): Unit = {
    writeBytes('"')
    writeInt(x.toInt)
    writeBytes('"')
  }

  def writeValAsString(x: Int): Unit = {
    writeBytes('"')
    writeInt(x)
    writeBytes('"')
  }

  def writeValAsString(x: Long): Unit = {
    writeBytes('"')
    writeLong(x)
    writeBytes('"')
  }

  def writeValAsString(x: Float): Unit = {
    writeBytes('"')
    writeFloat(x)
    writeBytes('"')
  }

  def writeValAsString(x: Double): Unit = {
    writeBytes('"')
    writeDouble(x)
    writeBytes('"')
  }

  def writeValAsString(x: Year): Unit =
    if (x eq null) writeNull()
    else writeValAsString(x.getValue)

  def writeNull(): Unit = writeBytes('n', 'u', 'l', 'l')

  def writeArrayStart(): Unit = writeNestedStart('[')

  def writeArrayEnd(): Unit = writeNestedEnd(']')

  def writeObjectStart(): Unit = writeNestedStart('{')

  def writeObjectEnd(): Unit = writeNestedEnd('}')

  private def nullKeyError(): Nothing = throw new IOException("key cannot be null")

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

  private def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte): Unit = count = {
    val pos = ensureBufferCapacity(6)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    buf(pos + 5) = b6
    pos + 6
  }

  private def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte): Unit = count = {
    val pos = ensureBufferCapacity(7)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    buf(pos + 5) = b6
    buf(pos + 6) = b7
    pos + 7
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

  private def writeUUID(x: UUID): Unit = count = {
    val pos = ensureBufferCapacity(38)
    val mostSigBits1 = (x.getMostSignificantBits >> 32).toInt
    val mostSigBits2 = x.getMostSignificantBits.toInt
    val leastSigBits1 = (x.getLeastSignificantBits >> 32).toInt
    val leastSigBits2 = x.getLeastSignificantBits.toInt
    buf(pos) = '"'
    buf(pos + 1) = toHexDigit(mostSigBits1 >> 28)
    buf(pos + 2) = toHexDigit(mostSigBits1 >> 24)
    buf(pos + 3) = toHexDigit(mostSigBits1 >> 20)
    buf(pos + 4) = toHexDigit(mostSigBits1 >> 16)
    buf(pos + 5) = toHexDigit(mostSigBits1 >> 12)
    buf(pos + 6) = toHexDigit(mostSigBits1 >> 8)
    buf(pos + 7) = toHexDigit(mostSigBits1 >> 4)
    buf(pos + 8) = toHexDigit(mostSigBits1)
    buf(pos + 9) = '-'
    buf(pos + 10) = toHexDigit(mostSigBits2 >> 28)
    buf(pos + 11) = toHexDigit(mostSigBits2 >> 24)
    buf(pos + 12) = toHexDigit(mostSigBits2 >> 20)
    buf(pos + 13) = toHexDigit(mostSigBits2 >> 16)
    buf(pos + 14) = '-'
    buf(pos + 15) = toHexDigit(mostSigBits2 >> 12)
    buf(pos + 16) = toHexDigit(mostSigBits2 >> 8)
    buf(pos + 17) = toHexDigit(mostSigBits2 >> 4)
    buf(pos + 18) = toHexDigit(mostSigBits2)
    buf(pos + 19) = '-'
    buf(pos + 20) = toHexDigit(leastSigBits1 >> 28)
    buf(pos + 21) = toHexDigit(leastSigBits1 >> 24)
    buf(pos + 22) = toHexDigit(leastSigBits1 >> 20)
    buf(pos + 23) = toHexDigit(leastSigBits1 >> 16)
    buf(pos + 24) = '-'
    buf(pos + 25) = toHexDigit(leastSigBits1 >> 12)
    buf(pos + 26) = toHexDigit(leastSigBits1 >> 8)
    buf(pos + 27) = toHexDigit(leastSigBits1 >> 4)
    buf(pos + 28) = toHexDigit(leastSigBits1)
    buf(pos + 29) = toHexDigit(leastSigBits2 >> 28)
    buf(pos + 30) = toHexDigit(leastSigBits2 >> 24)
    buf(pos + 31) = toHexDigit(leastSigBits2 >> 20)
    buf(pos + 32) = toHexDigit(leastSigBits2 >> 16)
    buf(pos + 33) = toHexDigit(leastSigBits2 >> 12)
    buf(pos + 34) = toHexDigit(leastSigBits2 >> 8)
    buf(pos + 35) = toHexDigit(leastSigBits2 >> 4)
    buf(pos + 36) = toHexDigit(leastSigBits2)
    buf(pos + 37) = '"'
    pos + 38
  }

  private def writeString(s: String): Unit = count = {
    var pos = ensureBufferCapacity(2)
    buf(pos) = '"'
    pos = {
      val bs = UnsafeUtils.getLatin1Array(s)
      if (bs eq null) writeString(s, 0, s.length, pos + 1, buf.length - 1, escapedChars)
      else writeString(bs, 0, s.length, pos + 1, buf.length - 1, escapedChars)
    }
    buf(pos) = '"'
    pos + 1
  }

  @tailrec
  private def writeString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeString(s, from, to, growBuffer(2, pos), buf.length - 1, escapedChars)
    else {
      val ch = s.charAt(from)
      if (ch < 128 && escapedChars(ch) == 0) {
        buf(pos) = ch.toByte
        writeString(s, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(s, from, to, pos, posLim - 12, escapedChars)
      else writeEncodedString(s, from, to, pos, posLim - 6, escapedChars)
    }

  @tailrec
  private def writeEncodedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(s, from, to, growBuffer(7, pos), buf.length - 6, escapedChars)
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
        val cp = (ch1 << 10) + ch2 + 0xFCA02400 // 0xFCA02400 == 0x010000 - (0xD800 << 10) - 0xDC00
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
    else if (pos >= posLim) writeEscapedString(s, from, to, growBuffer(13, pos), buf.length - 12, escapedChars)
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
        if (ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        writeEscapedString(s, from + 2, to, writeEscapedUnicode(ch2, writeEscapedUnicode(ch1, pos)), posLim, escapedChars)
      }
    }

  @tailrec
  private def writeString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeString(bs, from, to, growBuffer(2, pos), buf.length - 1, escapedChars)
    else {
      val b = bs(from)
      if (b >= 0 && escapedChars(b) == 0) {
        buf(pos) = b
        writeString(bs, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(bs, from, to, pos, posLim - 12, escapedChars)
      else writeEncodedString(bs, from, to, pos, posLim - 6, escapedChars)
    }

  @tailrec
  private def writeEncodedString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(bs, from, to, growBuffer(7, pos), buf.length - 6, escapedChars)
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
    else if (pos >= posLim) writeEscapedString(bs, from, to, growBuffer(7, pos), buf.length - 6, escapedChars)
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
    (((9 - nibble) >> 31) & 39) + (nibble + 48) // branchless conversion of nibble to hex digit
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

  private def writeByte(x: Byte): Unit = count = {
    var pos = ensureBufferCapacity(4) // Byte.MinValue.toString.length
    val buf = this.buf
    val q0: Int =
      if (x >= 0) x
      else {
        buf(pos) = '-'
        pos += 1
        -x
      }
    if (q0 < 10) {
      buf(pos) = (q0 + '0').toByte
      pos + 1
    } else if (q0 < 100) {
      val d = digits(q0)
      buf(pos) = (d >> 8).toByte
      buf(pos + 1) = d.toByte
      pos + 2
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide int by 100
      val r1 = q0 - 100 * q1
      val d = digits(r1)
      buf(pos) = (q1 + '0').toByte
      buf(pos + 1) = (d >> 8).toByte
      buf(pos + 2) = d.toByte
      pos + 3
    }
  }

  private def writeInstant(x: Instant): Unit = count = {
    val dt = LocalDateTime.ofEpochSecond(x.getEpochSecond, x.getNano, ZoneOffset.UTC)
    var pos = ensureBufferCapacity(39) // 39 == Instant.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(dt.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeLocalTime(dt.toLocalTime, pos + 1, buf, ds, full = true)
    buf(pos) = 'Z'
    buf(pos + 1) = '"'
    pos + 2
  }

  private def writeLocalDate(x: LocalDate): Unit = count = {
    var pos = ensureBufferCapacity(18) // 18 == LocalDate.MAX.toString.length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeLocalDate(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private def writeLocalDateTime(x: LocalDateTime): Unit = count = {
    var pos = ensureBufferCapacity(37) // 37 == LocalDateTime.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeLocalTime(x.toLocalTime, pos + 1, buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private def writeLocalTime(x: LocalTime): Unit = count = {
    var pos = ensureBufferCapacity(20) // 20 == LocalTime.MAX.toString.length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeLocalTime(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private def writeMonthDay(x: MonthDay): Unit = count = {
    var pos = ensureBufferCapacity(9) // 9 == "--01-01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    buf(pos + 1) = '-'
    buf(pos + 2) = '-'
    pos = write2Digits(x.getMonthValue, pos + 3, buf, ds)
    buf(pos) = '-'
    pos = write2Digits(x.getDayOfMonth, pos + 1, buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private def writeOffsetDateTime(x: OffsetDateTime): Unit = count = {
    var pos = ensureBufferCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private def writeOffsetTime(x: OffsetTime): Unit = count = {
    var pos = ensureBufferCapacity(29) // 29 == "00:00:07.999999998+00:00:08".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private def writeYearMonth(x: YearMonth): Unit = count = {
    var pos = ensureBufferCapacity(15) // 15 == "+999999999-12".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeYearMonth(x.getYear, x.getMonthValue, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private def writeZonedDateTime(x: ZonedDateTime): Unit = count = {
    var pos = ensureBufferCapacity(maxZonedDateTimeLength) // ~80 for current time zones
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    val zone = x.getZone
    if (!zone.isInstanceOf[ZoneOffset]) {
      buf(pos) = '['
      pos += 1
      val zoneId = zone.getId
      val len = zoneId.length
      zoneId.getBytes(0, len, buf, pos)
      pos += len
      buf(pos) = ']'
      pos += 1
    }
    buf(pos) = '"'
    pos + 1
  }

  private def writeZoneOffset(x: ZoneOffset): Unit = count = {
    var pos = ensureBufferCapacity(12) // 12 == "+10:10:10".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeOffset(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private def writeLocalDate(x: LocalDate, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYearMonth(x.getYear, x.getMonthValue, p, buf, ds)
    buf(pos) = '-'
    write2Digits(x.getDayOfMonth, pos + 1, buf, ds)
  }

  private def writeYearMonth(year: Int, month: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = p
    val posYear =
      if (year >= 0) {
        if (year >= 10000) {
          buf(pos) = '+'
          pos += 1
        }
        year
      } else {
        buf(pos) = '-'
        pos += 1
        -year
      }
    if (posYear >= 10000) {
      val off = offset(posYear)
      pos = writeIntFirst(posYear, pos + off, buf, ds) + off
    } else pos = write4Digits(posYear, pos, buf, ds)
    buf(pos) = '-'
    write2Digits(month, pos + 1, buf, ds)
  }

  private def writeLocalTime(x: LocalTime, p: Int, buf: Array[Byte], ds: Array[Short], full: Boolean = false): Int = {
    var pos = write2Digits(x.getHour, p, buf, ds)
    buf(pos) = ':'
    pos = write2Digits(x.getMinute, pos + 1, buf, ds)
    val second = x.getSecond
    val nano = x.getNano
    if (full || second != 0 || nano != 0) {
      buf(pos) = ':'
      pos = write2Digits(x.getSecond, pos + 1, buf, ds)
      if (nano > 0) {
        buf(pos) = '.'
        val q1 = nano / 1000000 // TODO use 64-bit mul with shift instead of /
        pos = write3Digits(q1, pos + 1, buf, ds)
        val r1 = nano - q1 * 1000000
        if (r1 > 0) {
          val q2 = r1 / 1000 // TODO use 64-bit mul with shift instead of /
          pos = write3Digits(q2, pos, buf, ds)
          val r2 = r1 - q2 * 1000
          if (r2 > 0) pos = write3Digits(r2, pos, buf, ds)
        }
      }
    }
    pos
  }

  private def writeOffset(x: ZoneOffset, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val ots = x.getTotalSeconds
    if (ots == 0) {
      buf(p) = 'Z'
      p + 1
    } else {
      val pots =
        if (ots >= 0) {
          buf(p) = '+'
          ots
        } else {
          buf(p) = '-'
          -ots
        }
      val q1 = pots / 3600 // TODO use 64-bit mul with shift instead of /
      val r1 = pots - q1 * 3600
      val q2 = r1 / 60 // TODO use 64-bit mul with shift instead of /
      val r2 = r1 - q2 * 60
      var pos = write2Digits(q1, p + 1, buf, ds)
      buf(pos) = ':'
      pos = write2Digits(q2, pos + 1, buf, ds)
      if (r2 != 0) {
        buf(pos) = ':'
        pos = write2Digits(r2, pos + 1, buf, ds)
      }
      pos
    }
  }

  private def write2Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val d = ds(x)
    buf(pos) = (d >> 8).toByte
    buf(pos + 1) = d.toByte
    pos + 2
  }

  private def write3Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (x * 1374389535L >> 37).toInt // divide int by 100
    val r1 = x - 100 * q1
    buf(pos) = (q1 + '0').toByte
    write2Digits(r1, pos + 1, buf, ds)
  }

  private def write4Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (x * 1374389535L >> 37).toInt // divide int by 100
    val r1 = x - 100 * q1
    write2Digits(r1, write2Digits(q1, pos, buf, ds), buf, ds)
  }

  private def writeShort(x: Short): Unit = count = {
    var pos = ensureBufferCapacity(6) // Short.MinValue.toString.length
    val buf = this.buf
    val q0: Int =
      if (x >= 0) x
      else {
        buf(pos) = '-'
        pos += 1
        -x
      }
    val off = offset(q0)
    writeIntFirst(q0, pos + off, buf, digits) + off
  }

  private def writeInt(x: Int): Unit = count = {
    var pos = ensureBufferCapacity(11) // minIntBytes.length
    if (x == -2147483648) writeByteArray(minIntBytes, pos)
    else {
      val buf = this.buf
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      val off = offset(q0)
      writeIntFirst(q0, pos + off, buf, digits) + off
    }
  }

  private def writeLong(x: Long): Unit = count = {
    var pos = ensureBufferCapacity(20) // minLongBytes.length
    if (x == -9223372036854775808L) writeByteArray(minLongBytes, pos)
    else {
      val buf = this.buf
      val ds = digits
      val q0 =
        if (x >= 0) x
        else {
          buf(pos) = '-'
          pos += 1
          -x
        }
      if (q0 >= 100000000) {
        val q1 = q0 / 100000000
        val r1 = (q0 - 100000000 * q1).toInt
        if (q1 >= 100000000) {
          val q2 = q1 / 100000000
          val r2 = (q1 - 100000000 * q2).toInt
          val off = offset(q2.toInt) + 16
          writeIntFirst(q2.toInt, writeIntRem(r2, writeIntRem(r1, pos + off, buf, ds, 3), buf, ds, 3), buf, ds) + off
        } else {
          val off = offset(q1.toInt) + 8
          writeIntFirst(q1.toInt, writeIntRem(r1, pos + off, buf, ds, 3), buf, ds) + off
        }
      } else {
        val off = offset(q0.toInt)
        writeIntFirst(q0.toInt, pos + off, buf, ds) + off
      }
    }
  }

  @tailrec
  private def writeIntFirst(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (q0 < 100) {
      if (q0 < 10) {
        buf(pos) = (q0 + '0').toByte
        pos + 1
      } else {
        val d = ds(q0)
        buf(pos) = d.toByte
        buf(pos - 1) = (d >> 8).toByte
        pos
      }
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide int by 100
      val r1 = q0 - 100 * q1
      val d = ds(r1)
      buf(pos) = d.toByte
      buf(pos - 1) = (d >> 8).toByte
      writeIntFirst(q1, pos - 2, buf, ds)
    }

  @tailrec
  private def writeIntRem(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short], i: Int): Int =
    if (i == 0) {
      val d = ds(q0)
      buf(pos) = d.toByte
      buf(pos - 1) = (d >> 8).toByte
      pos - 2
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide int by 100
      val r1 = q0 - 100 * q1
      val d = ds(r1)
      buf(pos) = d.toByte
      buf(pos - 1) = (d >> 8).toByte
      writeIntRem(q1, pos - 2, buf, ds, i - 1)
    }

  private def offset(q0: Int): Int =
    if (q0 < 100) (9 - q0) >>> 31
    else if (q0 < 10000) ((999 - q0) >>> 31) + 2
    else if (q0 < 1000000) ((99999 - q0) >>> 31) + 4
    else if (q0 < 100000000) ((9999999 - q0) >>> 31) + 6
    else ((999999999 - q0) >>> 31) + 8

  private def writeByteArray(bs: Array[Byte], pos: Int): Int = {
    System.arraycopy(bs, 0, buf, pos, bs.length)
    pos + bs.length
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
  private final val pool: ThreadLocal[JsonWriter] = new ThreadLocal[JsonWriter] {
    override def initialValue(): JsonWriter = new JsonWriter
  }
  private final val defaultConfig = new WriterConfig
  private final val escapedChars: Array[Byte] = (0 to 127).map { b =>
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
  private final val digits: Array[Short] =
    (0 to 99).map(i => (((i / 10 + '0') << 8) + (i % 10 + '0')).toShort)(breakOut)
  private final val minIntBytes: Array[Byte] = "-2147483648".getBytes
  private final val minLongBytes: Array[Byte] = "-9223372036854775808".getBytes
  private final val maxZonedDateTimeLength: Int = {
    val mostLongZoneId = ZoneId.of(ZoneId.getAvailableZoneIds.asScala.maxBy(_.length))
    ZonedDateTime.ofLocal(LocalDateTime.MAX, mostLongZoneId, ZoneOffset.UTC).toString.length + 5
  }

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