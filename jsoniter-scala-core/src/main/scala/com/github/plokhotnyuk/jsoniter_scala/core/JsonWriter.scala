package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{IOException, OutputStream}
import java.math.BigInteger
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter.{escapedChars, _}

import scala.annotation.tailrec
import scala.{specialized => sp}

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
    preferredBufSize: Int = 16384) {
  if (indentionStep < 0) throw new IllegalArgumentException("'indentionStep' should be not less than 0")
  if (preferredBufSize < 0) throw new IllegalArgumentException("'preferredBufSize' should be not less than 0")
}

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

  def writeKey(x: BigInt): Unit = {
    writeCommaWithParentheses()
    writeNonEscapedAsciiStringWithoutParentheses(new java.math.BigDecimal(x.bigInteger).toPlainString)
    writeParenthesesWithColon()
  }

  def writeKey(x: BigDecimal): Unit = {
    writeCommaWithParentheses()
    writeNonEscapedAsciiStringWithoutParentheses(x.toString)
    writeParenthesesWithColon()
  }

  def writeKey(x: UUID): Unit = {
    writeComma()
    writeUUID(x)
    writeColon()
  }

  def writeKey(x: String): Unit = {
    writeComma()
    writeString(x)
    writeColon()
  }

  def writeNonEscapedAsciiKey(x: String): Unit = {
    writeComma()
    writeNonEscapedAsciiString(x)
    writeColon()
  }

  def writeKey(x: Duration): Unit = {
    writeComma()
    writeDuration(x)
    writeColon()
  }

  def writeKey(x: Instant): Unit = {
    writeComma()
    writeInstant(x)
    writeColon()
  }

  def writeKey(x: LocalDate): Unit = {
    writeComma()
    writeLocalDate(x)
    writeColon()
  }

  def writeKey(x: LocalDateTime): Unit = {
    writeComma()
    writeLocalDateTime(x)
    writeColon()
  }

  def writeKey(x: LocalTime): Unit = {
    writeComma()
    writeLocalTime(x)
    writeColon()
  }

  def writeKey(x: MonthDay): Unit = {
    writeComma()
    writeMonthDay(x)
    writeColon()
  }

  def writeKey(x: OffsetDateTime): Unit = {
    writeComma()
    writeOffsetDateTime(x)
    writeColon()
  }

  def writeKey(x: OffsetTime): Unit = {
    writeComma()
    writeOffsetTime(x)
    writeColon()
  }

  def writeKey(x: Period): Unit = {
    writeComma()
    writePeriod(x)
    writeColon()
  }

  def writeKey(x: Year): Unit = {
    writeComma()
    writeYear(x)
    writeColon()
  }

  def writeKey(x: YearMonth): Unit = {
    writeComma()
    writeYearMonth(x)
    writeColon()
  }

  def writeKey(x: ZonedDateTime): Unit = {
    writeComma()
    writeZonedDateTime(x)
    writeColon()
  }

  def writeKey(x: ZoneId): Unit = {
    writeComma()
    writeNonEscapedAsciiString(x.getId)
    writeColon()
  }

  def writeKey(x: ZoneOffset): Unit = {
    writeComma()
    writeZoneOffset(x)
    writeColon()
  }

  def encodeError(msg: String): Nothing = throw new IOException(msg)

  def writeVal(x: BigDecimal): Unit = writeNonEscapedAsciiStringWithoutParentheses(x.toString)

  def writeVal(x: BigInt): Unit =
    writeNonEscapedAsciiStringWithoutParentheses(new java.math.BigDecimal(x.bigInteger).toPlainString)

  def writeVal(x: UUID): Unit = writeUUID(x)

  def writeVal(x: String): Unit = writeString(x)

  def writeNonEscapedAsciiVal(x: String): Unit = writeNonEscapedAsciiString(x)

  def writeVal(x: Duration): Unit = writeDuration(x)

  def writeVal(x: Instant): Unit = writeInstant(x)

  def writeVal(x: LocalDate): Unit = writeLocalDate(x)

  def writeVal(x: LocalDateTime): Unit = writeLocalDateTime(x)

  def writeVal(x: LocalTime): Unit = writeLocalTime(x)

  def writeVal(x: MonthDay): Unit = writeMonthDay(x)

  def writeVal(x: OffsetDateTime): Unit = writeOffsetDateTime(x)

  def writeVal(x: OffsetTime): Unit = writeOffsetTime(x)

  def writeVal(x: Period): Unit = writePeriod(x)

  def writeVal(x: Year): Unit = writeYear(x)

  def writeVal(x: YearMonth): Unit = writeYearMonth(x)

  def writeVal(x: ZonedDateTime): Unit = writeZonedDateTime(x)

  def writeVal(x: ZoneId): Unit = writeNonEscapedAsciiString(x.getId)

  def writeVal(x: ZoneOffset): Unit = writeZoneOffset(x)

  def writeVal(x: Boolean): Unit = if (x) writeBytes('t', 'r', 'u', 'e') else writeBytes('f', 'a', 'l', 's', 'e')

  def writeVal(x: Byte): Unit = writeByte(x)

  def writeVal(x: Short): Unit = writeShort(x)

  def writeVal(x: Char): Unit = writeChar(x)

  def writeVal(x: Int): Unit = writeInt(x)

  def writeVal(x: Long): Unit = writeLong(x)

  def writeVal(x: Float): Unit = writeFloat(x)

  def writeVal(x: Double): Unit = writeDouble(x)

  def writeValAsString(x: BigDecimal): Unit = writeNonEscapedAsciiString(x.toString)

  def writeValAsString(x: BigInt): Unit =
    writeNonEscapedAsciiString(new java.math.BigDecimal(x.bigInteger).toPlainString)

  def writeValAsString(x: Boolean): Unit =
    if (x) writeBytes('"', 't', 'r', 'u', 'e', '"') else writeBytes('"', 'f', 'a', 'l', 's', 'e', '"')

  def writeValAsString(x: Byte): Unit = {
    writeBytes('"')
    writeByte(x)
    writeBytes('"')
  }

  def writeValAsString(x: Short): Unit = {
    writeBytes('"')
    writeShort(x)
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

  def writeNull(): Unit = writeBytes('n', 'u', 'l', 'l')

  def writeArrayStart(): Unit = writeNestedStart('[')

  def writeArrayEnd(): Unit = writeNestedEnd(']')

  def writeObjectStart(): Unit = writeNestedStart('{')

  def writeObjectEnd(): Unit = writeNestedEnd('}')

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, out: OutputStream, config: WriterConfig): Unit =
    try {
      this.out = out
      this.config = config
      count = 0
      indention = 0
      isBufGrowingAllowed = true
      codec.encodeValue(x, this)
      flushBuf() // do not flush buffer in case of exception during encoding to avoid hiding it by possible new one
    } finally {
      this.out = null // do not close output stream, just help GC instead
      freeTooLongBuf()
    }

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): Array[Byte] =
    try {
      this.config = config
      this.count = 0
      this.indention = 0
      isBufGrowingAllowed = true
      codec.encodeValue(x, this)
      java.util.Arrays.copyOf(buf, count)
    } finally freeTooLongBuf()

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, buf: Array[Byte], from: Int, config: WriterConfig): Int = {
    val currBuf = this.buf
    try {
      this.buf = buf
      this.config = config
      count = from
      indention = 0
      isBufGrowingAllowed = false
      codec.encodeValue(x, this)
      count
    } finally this.buf = currBuf
  }

  private[this] def writeNestedStart(b: Byte): Unit = {
    indention += config.indentionStep
    comma = false
    writeBytes(b)
  }

  private[this] def writeNestedEnd(b: Byte): Unit = {
    val indentionStep = config.indentionStep
    writeIndention(indentionStep)
    indention -= indentionStep
    comma = true
    writeBytes(b)
  }

  private[this] def writeBytes(b: Byte): Unit = count = {
    val pos = ensureBufCapacity(1)
    buf(pos) = b
    pos + 1
  }

  private[this] def writeBytes(b1: Byte, b2: Byte): Unit = count = {
    val pos = ensureBufCapacity(2)
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte): Unit = count = {
    val pos = ensureBufCapacity(3)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = count = {
    val pos = ensureBufCapacity(4)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte): Unit = count = {
    val pos = ensureBufCapacity(5)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    pos + 5
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte): Unit = count = {
    val pos = ensureBufCapacity(6)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    buf(pos + 5) = b6
    pos + 6
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, b5: Byte, b6: Byte, b7: Byte): Unit = count = {
    val pos = ensureBufCapacity(7)
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    buf(pos + 4) = b5
    buf(pos + 5) = b6
    buf(pos + 6) = b7
    pos + 7
  }

  private[this] def writeNonEscapedAsciiStringWithoutParentheses(s: String): Unit = count = {
    val len = s.length
    val pos = ensureBufCapacity(len)
    s.getBytes(0, len, buf, pos)
    pos + len
  }

  private[this] def writeNonEscapedAsciiString(s: String): Unit = count = {
    val len = s.length
    val pos = ensureBufCapacity(len + 2)
    buf(pos) = '"'
    s.getBytes(0, len, buf, pos + 1)
    buf(pos + len + 1) = '"'
    pos + len + 2
  }

  private[this] def writeUUID(x: UUID): Unit = count = {
    val pos = ensureBufCapacity(38)
    val mostSigBits1 = (x.getMostSignificantBits >>> 32).toInt
    val mostSigBits2 = x.getMostSignificantBits.toInt
    val leastSigBits1 = (x.getLeastSignificantBits >>> 32).toInt
    val leastSigBits2 = x.getLeastSignificantBits.toInt
    val toHexDigit = hexDigits
    val m = 15
    buf(pos) = '"'
    buf(pos + 1) = toHexDigit(mostSigBits1 >>> 28)
    buf(pos + 2) = toHexDigit((mostSigBits1 >>> 24) & m)
    buf(pos + 3) = toHexDigit((mostSigBits1 >>> 20) & m)
    buf(pos + 4) = toHexDigit((mostSigBits1 >>> 16) & m)
    buf(pos + 5) = toHexDigit((mostSigBits1 >>> 12) & m)
    buf(pos + 6) = toHexDigit((mostSigBits1 >>> 8) & m)
    buf(pos + 7) = toHexDigit((mostSigBits1 >>> 4) & m)
    buf(pos + 8) = toHexDigit(mostSigBits1 & m)
    buf(pos + 9) = '-'
    buf(pos + 10) = toHexDigit(mostSigBits2 >>> 28)
    buf(pos + 11) = toHexDigit((mostSigBits2 >>> 24) & m)
    buf(pos + 12) = toHexDigit((mostSigBits2 >>> 20) & m)
    buf(pos + 13) = toHexDigit((mostSigBits2 >>> 16) & m)
    buf(pos + 14) = '-'
    buf(pos + 15) = toHexDigit((mostSigBits2 >>> 12) & m)
    buf(pos + 16) = toHexDigit((mostSigBits2 >>> 8) & m)
    buf(pos + 17) = toHexDigit((mostSigBits2 >>> 4) & m)
    buf(pos + 18) = toHexDigit(mostSigBits2 & m)
    buf(pos + 19) = '-'
    buf(pos + 20) = toHexDigit(leastSigBits1 >>> 28)
    buf(pos + 21) = toHexDigit((leastSigBits1 >>> 24) & m)
    buf(pos + 22) = toHexDigit((leastSigBits1 >>> 20) & m)
    buf(pos + 23) = toHexDigit((leastSigBits1 >>> 16) & m)
    buf(pos + 24) = '-'
    buf(pos + 25) = toHexDigit((leastSigBits1 >>> 12) & m)
    buf(pos + 26) = toHexDigit((leastSigBits1 >>> 8) & m)
    buf(pos + 27) = toHexDigit((leastSigBits1 >>> 4) & m)
    buf(pos + 28) = toHexDigit(leastSigBits1 & m)
    buf(pos + 29) = toHexDigit(leastSigBits2 >>> 28)
    buf(pos + 30) = toHexDigit((leastSigBits2 >>> 24) & m)
    buf(pos + 31) = toHexDigit((leastSigBits2 >>> 20) & m)
    buf(pos + 32) = toHexDigit((leastSigBits2 >>> 16) & m)
    buf(pos + 33) = toHexDigit((leastSigBits2 >>> 12) & m)
    buf(pos + 34) = toHexDigit((leastSigBits2 >>> 8) & m)
    buf(pos + 35) = toHexDigit((leastSigBits2 >>> 4) & m)
    buf(pos + 36) = toHexDigit(leastSigBits2 & m)
    buf(pos + 37) = '"'
    pos + 38
  }

  private[this] def writeString(s: String): Unit = count = {
    var pos = ensureBufCapacity(2)
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
  private[this] def writeString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeString(s, from, to, flushAndGrowBuf(2, pos), buf.length - 1, escapedChars)
    else {
      val ch = s.charAt(from)
      if (ch < 128 && escapedChars(ch) == 0) {
        buf(pos) = ch.toByte
        writeString(s, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(s, from, to, pos, posLim - 12, escapedChars)
      else writeEncodedString(s, from, to, pos, posLim - 6, escapedChars)
    }

  @tailrec
  private[this] def writeEncodedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(s, from, to, flushAndGrowBuf(7, pos), buf.length - 6, escapedChars)
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
  private[this] def writeEscapedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEscapedString(s, from, to, flushAndGrowBuf(13, pos), buf.length - 12, escapedChars)
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
  private[this] def writeString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeString(bs, from, to, flushAndGrowBuf(2, pos), buf.length - 1, escapedChars)
    else {
      val b = bs(from)
      if (b >= 0 && escapedChars(b) == 0) {
        buf(pos) = b
        writeString(bs, from + 1, to, pos + 1, posLim, escapedChars)
      } else if (config.escapeUnicode) writeEscapedString(bs, from, to, pos, posLim - 12, escapedChars)
      else writeEncodedString(bs, from, to, pos, posLim - 6, escapedChars)
    }

  @tailrec
  private[this] def writeEncodedString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(bs, from, to, flushAndGrowBuf(7, pos), buf.length - 6, escapedChars)
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
  private[this] def writeEscapedString(bs: Array[Byte], from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEscapedString(bs, from, to, flushAndGrowBuf(7, pos), buf.length - 6, escapedChars)
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

  private[this] def writeChar(ch: Char): Unit = count = {
    var pos = ensureBufCapacity(8) // 6 bytes per char for escaped unicode + make room for the quotes
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

  private[this] def writeEscapedUnicode(ch: Char, pos: Int): Int = {
    val toHexDigit = hexDigits
    val m = 15
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = toHexDigit(ch >>> 12)
    buf(pos + 3) = toHexDigit((ch >>> 8) & m)
    buf(pos + 4) = toHexDigit((ch >>> 4) & m)
    buf(pos + 5) = toHexDigit(ch & m)
    pos + 6
  }

  private[this] def writeEscapedUnicode(b: Byte, pos: Int): Int = {
    val toHexDigit = hexDigits
    val m = 15
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = '0'
    buf(pos + 3) = '0'
    buf(pos + 4) = toHexDigit((b >>> 4) & m)
    buf(pos + 5) = toHexDigit(b & m)
    pos + 6
  }

  private[this] def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private[this] def writeCommaWithParentheses(): Unit = {
    if (comma) writeBytes(',')
    else comma = true
    writeIndention(0)
    writeBytes('"')
  }

  private[this] def writeParenthesesWithColon(): Unit =
    if (config.indentionStep > 0) writeBytes('"', ':', ' ')
    else writeBytes('"', ':')

  private[this] def writeColon(): Unit =
    if (config.indentionStep > 0) writeBytes(':', ' ')
    else writeBytes(':')

  private[this] def writeByte(x: Byte): Unit = count = {
    var pos = ensureBufCapacity(4) // Byte.MinValue.toString.length
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
      buf(pos) = '1'
      val d = digits(q0 - 100)
      buf(pos + 1) = (d >> 8).toByte
      buf(pos + 2) = d.toByte
      pos + 3
    }
  }

  private[this] def writeDuration(x: Duration): Unit =
    if (x.isZero) writeBytes('"', 'P', 'T', '0', 'S', '"')
    else {
      writeBytes('"', 'P', 'T')
      val totalSecs = x.getSeconds
      val nanos = x.getNano
      var effectiveTotalSecs = totalSecs
      if (effectiveTotalSecs < 0 && nanos > 0) effectiveTotalSecs += 1
      val hours = effectiveTotalSecs / 3600
      if (hours != 0) {
        writeLong(hours)
        writeBytes('H')
      }
      val secsOfHour = (effectiveTotalSecs - hours * 3600).toInt
      val minutes = secsOfHour / 60
      if (minutes != 0) {
        writeInt(minutes)
        writeBytes('M')
      }
      val seconds = secsOfHour - minutes * 60
      if (seconds == 0 && nanos == 0) writeBytes('"')
      else {
        if (totalSecs < 0 && seconds == 0 && nanos != 0) writeBytes('-', '0')
        else writeInt(seconds)
        if (nanos == 0) writeBytes('S', '"')
        else {
          var pos = ensureBufCapacity(12)
          val buf = this.buf
          buf(pos) = '.'
          pos = writeNanos(if (totalSecs < 0) 1000000000 - nanos else nanos, pos, buf)
          buf(pos) = 'S'
          buf(pos + 1) = '"'
          count = pos + 2
        }
      }
    }

  private[this] def writeNanos(x: Int, pos: Int, buf: Array[Byte]): Int =
    writeSignificantFractionDigits(x, pos + 9, pos, 0, buf)

  @tailrec
  private[this] def writeSignificantFractionDigits(x: Int, pos: Int, posLim: Int, posMax: Int, buf: Array[Byte]): Int =
    if (pos == posLim) posMax
    else {
      val q0 = (x * 3435973837L >> 35).toInt // divide positive int by 10
      val r0 = x - 10 * q0
      if (posMax == 0 && r0 == 0) writeSignificantFractionDigits(q0, pos - 1, posLim, 0, buf)
      else {
        buf(pos) = ('0' + r0).toByte
        writeSignificantFractionDigits(q0, pos - 1, posLim, if (posMax == 0) pos + 1 else posMax, buf)
      }
    }

  private[this] def writeInstant(x: Instant): Unit = count = {
    val epochSecond = x.getEpochSecond
    val epochDay = (if (epochSecond < 0) epochSecond - 86399 else epochSecond) / 86400 // 86400 == seconds per day
    var marchZeroDay = epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    var adjustYear = 0
    if (marchZeroDay < 0) { // adjust negative years to positive for calculation
      val adjust400YearCycles = ((marchZeroDay + 1) / 146097).toInt - 1 // 146097 == number of days in a 400 year cycle
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L
    }
    var yearEst = ((400 * marchZeroDay + 591) / 146097).toInt
    var marchDayOfYear = toMarchDayOfYear(marchZeroDay, yearEst)
    if (marchDayOfYear < 0) { // fix estimate
      yearEst -= 1
      marchDayOfYear = toMarchDayOfYear(marchZeroDay, yearEst)
    }
    yearEst += adjustYear // reset any negative year
    val marchMonth = ((marchDayOfYear * 17965876275L + 7186350510L) >> 39).toInt // == (marchDayOfYear * 5 + 2) / 153
    val year = yearEst + (marchMonth * 3435973837L >> 35).toInt // == yearEst + marchMonth / 10 (convert march-based values back to january-based)
    val month = marchMonth + (if (marchMonth < 10) 3 else -9)
    val day = marchDayOfYear - ((marchMonth * 1051407994122L - 17179869183L) >> 35).toInt // == marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
    val secsOfDay = (epochSecond - epochDay * 86400).toInt
    val hour = (secsOfDay * 2443359173L >> 43).toInt // divide positive int by 3600
    val secsOfHour = secsOfDay - hour * 3600
    val minute = (secsOfHour * 2290649225L >> 37).toInt // divide positive int by 60
    val second = secsOfHour - minute * 60
    var pos = ensureBufCapacity(39) // 39 == Instant.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(year, month, day, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeLocalTime(hour, minute, second, x.getNano, pos + 1, buf, ds, full = true)
    buf(pos) = 'Z'
    buf(pos + 1) = '"'
    pos + 2
  }

  private[this] def toMarchDayOfYear(marchZeroDay: Long, yearEst: Int): Int = {
    val centuryEst = yearEst / 100
    (marchZeroDay - 365L * yearEst).toInt - (yearEst >> 2) + centuryEst - (centuryEst >> 2)
  }

  private[this] def writeLocalDate(x: LocalDate): Unit = count = {
    var pos = ensureBufCapacity(18) // 18 == LocalDate.MAX.toString.length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeLocalDate(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeLocalDateTime(x: LocalDateTime): Unit = count = {
    var pos = ensureBufCapacity(37) // 37 == LocalDateTime.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeLocalTime(x.toLocalTime, pos + 1, buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeLocalTime(x: LocalTime): Unit = count = {
    var pos = ensureBufCapacity(20) // 20 == LocalTime.MAX.toString.length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeLocalTime(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeMonthDay(x: MonthDay): Unit = count = {
    var pos = ensureBufCapacity(9) // 9 == "--01-01".length + 2
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

  private[this] def writeOffsetDateTime(x: OffsetDateTime): Unit = count = {
    var pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeOffsetTime(x: OffsetTime): Unit = count = {
    var pos = ensureBufCapacity(29) // 29 == "00:00:07.999999998+00:00:08".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writePeriod(x: Period): Unit =
    if (x.isZero) writeBytes('"', 'P', '0', 'D', '"')
    else {
      writeBytes('"', 'P')
      val years = x.getYears
      if (years != 0) {
        writeInt(years)
        writeBytes('Y')
      }
      val months = x.getMonths
      if (months != 0) {
        writeInt(months)
        writeBytes('M')
      }
      val days = x.getDays
      if (days != 0) {
        writeInt(days)
        writeBytes('D', '"')
      } else writeBytes('"')
    }

  private[this] def writeYear(x: Year): Unit = count = {
    var pos = ensureBufCapacity(12) // 12 == "+999999999".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeYear(x.getValue, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeYearMonth(x: YearMonth): Unit = count = {
    var pos = ensureBufCapacity(15) // 15 == "+999999999-12".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeYearMonth(x.getYear, x.getMonthValue, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeZonedDateTime(x: ZonedDateTime): Unit = count = {
    var pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    var buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    val zone = x.getZone
    if (!zone.isInstanceOf[ZoneOffset]) {
      val zoneId = zone.getId
      val len = zoneId.length
      val required = len + 3
      if (buf.length < pos + required) {
        pos = flushAndGrowBuf(required, pos)
        buf = this.buf
      }
      buf(pos) = '['
      pos += 1
      zoneId.getBytes(0, len, buf, pos)
      pos += len
      buf(pos) = ']'
      pos += 1
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeZoneOffset(x: ZoneOffset): Unit = count = {
    var pos = ensureBufCapacity(12) // 12 == "+10:10:10".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeOffset(x, pos + 1, buf, digits)
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeLocalDate(x: LocalDate, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    writeLocalDate(x.getYear, x.getMonthValue, x.getDayOfMonth, pos, buf, ds)

  private[this] def writeLocalDate(year: Int, month: Int, day: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYearMonth(year, month, p, buf, ds)
    buf(pos) = '-'
    write2Digits(day, pos + 1, buf, ds)
  }

  private[this] def writeYearMonth(year: Int, month: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYear(year, p, buf, ds)
    buf(pos) = '-'
    write2Digits(month, pos + 1, buf, ds)
  }

  private[this] def writeYear(year: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
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
    if (posYear >= 10000) writeInt(posYear, pos, buf, ds)
    else write4Digits(posYear, pos, buf, ds)
  }

  private[this] def writeLocalTime(x: LocalTime, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    writeLocalTime(x.getHour, x.getMinute, x.getSecond, x.getNano, pos, buf, ds, full = false)

  private[this] def writeLocalTime(hour: Int, minute: Int, second: Int, nano: Int, p: Int, buf: Array[Byte],
                                   ds: Array[Short], full: Boolean): Int = {
    var pos = write2Digits(hour, p, buf, ds)
    buf(pos) = ':'
    pos = write2Digits(minute, pos + 1, buf, ds)
    if (full || second != 0 || nano != 0) {
      buf(pos) = ':'
      pos = write2Digits(second, pos + 1, buf, ds)
      if (nano > 0) {
        buf(pos) = '.'
        val q1 = (nano * 2251799814L >> 51).toInt // divide positive int by 1000000
        pos = write3Digits(q1, pos + 1, buf, ds)
        val r1 = nano - q1 * 1000000
        if (r1 > 0) {
          val q2 = (r1 * 2199023256L >> 41).toInt // divide positive int by 1000
          pos = write3Digits(q2, pos, buf, ds)
          val r2 = r1 - q2 * 1000
          if (r2 > 0) pos = write3Digits(r2, pos, buf, ds)
        }
      }
    }
    pos
  }

  private[this] def writeOffset(x: ZoneOffset, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val offsetTotalSeconds = x.getTotalSeconds
    if (offsetTotalSeconds == 0) {
      buf(p) = 'Z'
      p + 1
    } else {
      val posOffsetTotalSeconds =
        if (offsetTotalSeconds >= 0) {
          buf(p) = '+'
          offsetTotalSeconds
        } else {
          buf(p) = '-'
          -offsetTotalSeconds
        }
      val q1 = (posOffsetTotalSeconds * 2443359173L >> 43).toInt // divide positive int by 3600
      val r1 = posOffsetTotalSeconds - q1 * 3600
      var pos = write2Digits(q1, p + 1, buf, ds)
      buf(pos) = ':'
      val q2 = (r1 * 2290649225L >> 37).toInt // divide positive int by 60
      val r2 = r1 - q2 * 60
      pos = write2Digits(q2, pos + 1, buf, ds)
      if (r2 != 0) {
        buf(pos) = ':'
        pos = write2Digits(r2, pos + 1, buf, ds)
      }
      pos
    }
  }

  private[this] def write2Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val d = ds(x)
    buf(pos) = (d >> 8).toByte
    buf(pos + 1) = d.toByte
    pos + 2
  }

  private[this] def write3Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (x * 1374389535L >> 37).toInt // divide positive int by 100
    val r1 = x - 100 * q1
    buf(pos) = (q1 + '0').toByte
    val d = ds(r1)
    buf(pos + 1) = (d >> 8).toByte
    buf(pos + 2) = d.toByte
    pos + 3
  }

  private[this] def write4Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (x * 1374389535L >> 37).toInt // divide positive int by 100
    val r1 = x - 100 * q1
    val d1 = ds(q1)
    buf(pos) = (d1 >> 8).toByte
    buf(pos + 1) = d1.toByte
    val d2 = ds(r1)
    buf(pos + 2) = (d2 >> 8).toByte
    buf(pos + 3) = d2.toByte
    pos + 4
  }

  private[this] def write8Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (x * 3518437209L >> 45).toInt // divide positive int by 10000
    val r1 = x - 10000 * q1
    val q2 = (q1 * 1374389535L >> 37).toInt // divide positive int by 100
    val r2 = q1 - 100 * q2
    val d1 = ds(q2)
    buf(pos) = (d1 >> 8).toByte
    buf(pos + 1) = d1.toByte
    val d2 = ds(r2)
    buf(pos + 2) = (d2 >> 8).toByte
    buf(pos + 3) = d2.toByte
    val q3 = (r1 * 1374389535L >> 37).toInt // divide positive int by 100
    val r3 = r1 - 100 * q3
    val d3 = ds(q3)
    buf(pos + 4) = (d3 >> 8).toByte
    buf(pos + 5) = d3.toByte
    val d4 = ds(r3)
    buf(pos + 6) = (d4 >> 8).toByte
    buf(pos + 7) = d4.toByte
    pos + 8
  }

  private[this] def writeShort(x: Short): Unit = count = {
    var pos = ensureBufCapacity(6) // Short.MinValue.toString.length
    val buf = this.buf
    val q0: Int =
      if (x >= 0) x
      else {
        buf(pos) = '-'
        pos += 1
        -x
      }
    if (q0 < 100) {
      if (q0 < 10) {
        buf(pos) = (q0 + '0').toByte
        pos + 1
      } else write2Digits(q0, pos, buf, digits)
    } else if (q0 < 10000) {
      if (q0 < 1000) write3Digits(q0, pos, buf, digits)
      else write4Digits(q0, pos, buf, digits)
    } else {
      val q1 = (q0 * 3518437209L >> 45).toInt // divide positive int by 10000
      val r1 = q0 - 10000 * q1
      buf(pos) = (q1 + '0').toByte
      write4Digits(r1, pos + 1, buf, digits)
    }
  }

  private[this] def writeInt(x: Int): Unit = count = {
    var pos = ensureBufCapacity(11) // minIntBytes.length
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
      writeInt(q0, pos, buf, digits)
    }
  }

  private[this] def writeLong(x: Long): Unit = count = {
    var pos = ensureBufCapacity(20) // minLongBytes.length
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
      val q0i = q0.toInt
      if (q0 == q0i) writeInt(q0i, pos, buf, ds)
      else {
        val q1 = q0 / 100000000
        val r1 = (q0 - 100000000 * q1).toInt
        val q1i = q1.toInt
        if (q1 == q1i) write8Digits(r1, writeInt(q1i, pos, buf, ds), buf, ds)
        else {
          val q2 = q1 / 100000000
          val r2 = (q1 - 100000000 * q2).toInt
          write8Digits(r1, write8Digits(r2, writeInt(q2.toInt, pos, buf, ds), buf, ds), buf, ds)
        }
      }
    }
  }

  private[this] def writeInt(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val off = offset(q0)
    writeIntFirst(q0, pos + off, buf, ds) + off
  }

  @tailrec
  private[this] def writeIntFirst(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (q0 < 100) {
      if (q0 < 10) {
        buf(pos) = (q0 + '0').toByte
        pos + 1
      } else {
        val d = ds(q0)
        buf(pos - 1) = (d >> 8).toByte
        buf(pos) = d.toByte
        pos
      }
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
      val r1 = q0 - 100 * q1
      val d = ds(r1)
      buf(pos - 1) = (d >> 8).toByte
      buf(pos) = d.toByte
      writeIntFirst(q1, pos - 2, buf, ds)
    }

  private[this] def writeByteArray(bs: Array[Byte], pos: Int): Int = {
    val len = bs.length
    System.arraycopy(bs, 0, buf, pos, len)
    pos + len
  }

  // Based on a great work of Ulf Adams:
  // http://delivery.acm.org/10.1145/3200000/3192369/pldi18main-p10-p.pdf
  // https://github.com/ulfjack/ryu/blob/62925340e4abc76e3c63b6de8dea1486d6970260/src/main/java/info/adams/ryu/RyuFloat.java
  // Also, see his presentation "Ryū - Fast Float-to-String Conversion": https://www.youtube.com/watch?v=kw-U6smcLzk
  private[this] def writeFloat(x: Float): Unit = {
    if (!java.lang.Float.isFinite(x)) encodeError("illegal number: " + x)
    val bits = java.lang.Float.floatToRawIntBits(x)
    if (bits == 0) writeBytes('0', '.', '0')
    else if (bits == 0x80000000) writeBytes('-', '0', '.', '0')
    else count = {
      val ieeeExponent = (bits >> 23) & 255
      val ieeeMantissa = bits & 8388607
      val e2 = if (ieeeExponent == 0) -151 else ieeeExponent - 152
      val m2 = if (ieeeExponent == 0) ieeeMantissa else ieeeMantissa | 8388608
      val mv = m2 << 2
      val mp = mv + 2
      val mm = mv - (if (m2 != 8388608 || ieeeExponent <= 1) 2 else 1)
      var dp, dv, dm, exp = 0
      var dpIsTrailingZeros, dmIsTrailingZeros = false
      var lastRemovedDigit = 0
      if (e2 >= 0) {
        val ss = f32Pow5InvSplit
        val q = (e2 * 5422872582025449L >> 54).toInt // == (e2 * Math.log10(2)).toInt
        val k = 58 + pow5bits(q)
        val i = -e2 + q + k
        dv = mulPow5DivPow2(mv, q, i, ss)
        dp = mulPow5DivPow2(mp, q, i, ss)
        dm = mulPow5DivPow2(mm, q, i, ss)
        if (q != 0 && (dp - 1 <= dm)) {
          val l = 58 + pow5bits(q - 1)
          val ds = mulPow5DivPow2(mv, q - 1, -e2 + q - 1 + l, ss)
          lastRemovedDigit = ds - (ds * 3435973837L >> 35).toInt // divide positive int by 10
        }
        exp = q
        dpIsTrailingZeros = pow5Factor(mp, 0) >= q
        dmIsTrailingZeros = pow5Factor(mm, 0) >= q
      } else {
        val ss = f32Pow5Split
        val q = (-e2 * 3147881031633675L >> 52).toInt // == (-e2 * Math.log10(5)).toInt
        val i = -e2 - q
        val k = pow5bits(i) - 61
        var j = q - k
        dv = mulPow5DivPow2(mv, i, j, ss)
        dp = mulPow5DivPow2(mp, i, j, ss)
        dm = mulPow5DivPow2(mm, i, j, ss)
        if (q != 0 && (dp - 1 <= dm)) {
          j = q + 60 - pow5bits(i + 1)
          val ds = mulPow5DivPow2(mv, i + 1, j, ss)
          lastRemovedDigit = ds - (ds * 3435973837L >> 35).toInt // divide positive int by 10
        }
        exp = q + e2
        dpIsTrailingZeros = 1 >= q
        dmIsTrailingZeros = (~mm & 1) >= q
      }
      var olength = offset(dp)
      exp += olength
      olength += 1
      val decimalNotation = exp >= -3 && exp < 7
      val even = (m2 & 1) == 0
      if (dpIsTrailingZeros && !even) dp -= 1
      var newDp = (dp * 3435973837L >> 35).toInt // divide positive int by 10
      var newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
      while (newDp > newDm && (dp >= 100 || decimalNotation)) {
        dmIsTrailingZeros &= dm == 10 * newDm
        dp = newDp
        val newDv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
        lastRemovedDigit = dv - 10 * newDv
        dv = newDv
        dm = newDm
        newDp = (dp * 3435973837L >> 35).toInt // divide positive int by 10
        newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
        olength -= 1
      }
      if (dmIsTrailingZeros && even) {
        newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
        while (dm == 10 * newDm && (dp >= 100 || decimalNotation)) {
          dp = (dp * 3435973837L >> 35).toInt
          val newDv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
          lastRemovedDigit = dv - 10 * newDv
          dv = newDv
          dm = newDm
          newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
          olength -= 1
        }
      }
      var output = dv + (if (lastRemovedDigit >= 5 || dv == dm && !(dmIsTrailingZeros && even)) 1 else 0)
      var pos = ensureBufCapacity(15)
      if (bits < 0) {
        buf(pos) = '-'
        pos += 1
      }
      val ds = digits
      if (decimalNotation) {
        if (exp < 0) {
          buf(pos) = '0'
          buf(pos + 1) = '.'
          writeNDigits(output, olength, writeNZeros(-1 - exp, pos + 2, buf) + olength - 1, buf, ds) + olength + 1
        } else if (exp + 1 >= olength) {
          pos = writeNZeros(exp - olength + 1, writeNDigits(output, olength, pos + olength - 1, buf, ds) + olength + 1, buf)
          buf(pos) = '.'
          buf(pos + 1) = '0'
          pos + 2
        } else {
          pos += olength
          val ei = olength - 1 - exp
          var i = 0
          while (i < ei) {
            val newOutput = (output * 3435973837L >> 35).toInt // divide positive int by 10
            buf(pos) = ('0' + output - 10 * newOutput).toByte
            output = newOutput
            pos -= 1
            i += 1
          }
          buf(pos) = '.'
          writeNDigits(output, olength - i, pos - 1, buf, ds) + olength + 2
        }
      } else {
        pos = writeNDigits(output, olength, pos + olength, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos += olength + 1
        if (olength == 1) {
          buf(pos) = '0'
          pos += 1
        }
        buf(pos) = 'E'
        pos += 1
        if (exp < 0) {
          buf(pos) = '-'
          pos += 1
          exp = -exp
        }
        if (exp >= 10) write2Digits(exp, pos, buf, ds)
        else {
          buf(pos) = ('0' + exp).toByte
          pos + 1
        }
      }
    }
  }

  private[this] def mulPow5DivPow2(m: Long, i: Int, j: Int, ss: Array[Int]): Int = {
    val idx = i << 1
    ((m * ss(idx + 1) + (m * ss(idx) >> 31)) >> (j - 31)).toInt
  }

  private[this] def offset(q0: Int): Int =
    if (q0 < 100) (9 - q0) >>> 31
    else if (q0 < 10000) ((999 - q0) >>> 31) + 2
    else if (q0 < 1000000) ((99999 - q0) >>> 31) + 4
    else if (q0 < 100000000) ((9999999 - q0) >>> 31) + 6
    else ((999999999 - q0) >>> 31) + 8

  @tailrec
  private[this] def writeNDigits(q0: Int, n: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (n == 0) pos
    else if (n == 1) {
      buf(pos) =
        if (q0 < 10) (q0 + '0').toByte
        else if (q0 < 100) ds(q0).toByte
        else (q0 % 10 + '0').toByte
      pos - 1
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
      val r1 = q0 - 100 * q1
      val d = ds(r1)
      buf(pos - 1) = (d >> 8).toByte
      buf(pos) = d.toByte
      writeNDigits(q1, n - 2, pos - 2, buf, ds)
    }

  // Based on a great work of Ulf Adams:
  // http://delivery.acm.org/10.1145/3200000/3192369/pldi18main-p10-p.pdf
  // https://github.com/ulfjack/ryu/blob/62925340e4abc76e3c63b6de8dea1486d6970260/src/main/java/info/adams/ryu/RyuDouble.java
  // Also, see his presentation "Ryū - Fast Float-to-String Conversion": https://www.youtube.com/watch?v=kw-U6smcLzk
  private[this] def writeDouble(x: Double): Unit = {
    if (!java.lang.Double.isFinite(x)) encodeError("illegal number: " + x)
    val bits = java.lang.Double.doubleToRawLongBits(x)
    if (bits == 0) writeBytes('0', '.', '0')
    else if (bits == 0x8000000000000000L) writeBytes('-', '0', '.', '0')
    else count = {
      val ieeeExponent = ((bits >> 52) & 2047).toInt
      val ieeeMantissa = bits & 4503599627370495L
      val e2 = if (ieeeExponent == 0) -1076 else ieeeExponent - 1077
      val m2 = if (ieeeExponent == 0) ieeeMantissa else ieeeMantissa | 4503599627370496L
      val mv = m2 << 2L
      val mp = mv + 2
      val mm = mv - (if (m2 != 4503599627370496L || ieeeExponent <= 1) 2 else 1)
      var dp, dv, dm = 0L
      var exp = 0
      var dpIsTrailingZeros, dmIsTrailingZeros = false
      if (e2 >= 0) {
        val ss = f64Pow5InvSplit
        val q = Math.max(0, (e2 * 5422872582025449L >> 54).toInt - 1) // == Math.max(0, (e2 * Math.log10(2)).toInt - 1)
        val k = 121 + pow5bits(q)
        val i = -e2 + q + k
        dv = fullMulPow5DivPow2(mv, q, i, ss)
        dp = fullMulPow5DivPow2(mp, q, i, ss)
        dm = fullMulPow5DivPow2(mm, q, i, ss)
        exp = q
        dpIsTrailingZeros = pow5Factor(mp, 0) >= q
        dmIsTrailingZeros = pow5Factor(mm, 0) >= q
      } else {
        val ss = f64Pow5Split
        val q = Math.max(0, (-e2 * 3147881031633675L >> 52).toInt - 1) // == Math.max(0, (-e2 * Math.log10(5)).toInt - 1)
        val i = -e2 - q
        val k = pow5bits(i) - 121
        val j = q - k
        dv = fullMulPow5DivPow2(mv, i, j, ss)
        dp = fullMulPow5DivPow2(mp, i, j, ss)
        dm = fullMulPow5DivPow2(mm, i, j, ss)
        exp = q + e2
        dpIsTrailingZeros = 1 >= q
        dmIsTrailingZeros = (~mm & 1) >= q
      }
      var olength = offset(dp)
      exp += olength
      olength += 1
      val decimalNotation = exp >= -3 && exp < 7
      val even = (m2 & 1) == 0
      if (dpIsTrailingZeros && !even) dp -= 1
      var lastRemovedDigit = 0
      var newDp = dp / 10
      var newDm = dm / 10
      while (newDp > newDm && (dp >= 100 || decimalNotation)) {
        dmIsTrailingZeros &= dm == 10 * newDm
        dp = newDp
        val newDv = dv / 10
        lastRemovedDigit = (dv - 10 * newDv).toInt
        dv = newDv
        dm = newDm
        newDp = dp / 10
        newDm = dm / 10
        olength -= 1
      }
      if (dmIsTrailingZeros && even) {
        newDm = dm / 10
        while (dm == 10 * newDm && (dp >= 100 || decimalNotation)) {
          dp /= 10
          val newDv = dv / 10
          lastRemovedDigit = (dv - 10 * newDv).toInt
          dv = newDv
          dm = newDm
          newDm = dm / 10
          olength -= 1
        }
      }
      var output = dv + (if (lastRemovedDigit >= 5 || dv == dm && !(dmIsTrailingZeros && even)) 1 else 0)
      var pos = ensureBufCapacity(24)
      if (bits < 0) {
        buf(pos) = '-'
        pos += 1
      }
      val ds = digits
      if (decimalNotation) {
        if (exp < 0) {
          buf(pos) = '0'
          buf(pos + 1) = '.'
          writeNDigits(output, olength, writeNZeros(-1 - exp, pos + 2, buf) + olength - 1, buf, ds) + olength + 1
        } else if (exp + 1 >= olength) {
          pos = writeNZeros(exp - olength + 1, writeNDigits(output, olength, pos + olength - 1, buf, ds) + olength + 1, buf)
          buf(pos) = '.'
          buf(pos + 1) = '0'
          pos + 2
        } else {
          pos += olength
          val ei = olength - 1 - exp
          var i = 0
          while (i < ei) {
            val newOutput = output / 10
            buf(pos) = ('0' + output - 10 * newOutput).toByte
            output = newOutput
            pos -= 1
            i += 1
          }
          buf(pos) = '.'
          writeNDigits(output, olength - i, pos - 1, buf, ds) + olength + 2
        }
      } else {
        pos = writeNDigits(output, olength, pos + olength, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos += olength + 1
        if (olength == 1) {
          buf(pos) = '0'
          pos += 1
        }
        buf(pos) = 'E'
        pos += 1
        if (exp < 0) {
          buf(pos) = '-'
          pos += 1
          exp = -exp
        }
        if (exp >= 100) write3Digits(exp, pos, buf, ds)
        else if (exp >= 10) write2Digits(exp, pos, buf, ds)
        else {
          buf(pos) = ('0' + exp).toByte
          pos + 1
        }
      }
    }
  }

  @tailrec
  private[this] def pow5Factor(value: Long, count: Int): Int =  {
    val newValue = value / 5
    if ((newValue << 2) + newValue != value) count
    else if (newValue == newValue.toInt) pow5Factor(newValue.toInt, count + 1)
    else pow5Factor(newValue, count + 1)
  }

  private def fullMulPow5DivPow2(m: Long, i: Int, j: Int, ss: Array[Int]): Long = {
    val mHigh = m >>> 31
    val mLow = m & 0x7fffffff
    val idx = i << 2
    val s3 = ss(idx + 3)
    val s2 = ss(idx + 2)
    val s1 = ss(idx + 1)
    val s0 = ss(idx)
    ((((((((mLow * s3 >>> 31) + mLow * s2 + mHigh * s3) >>> 31) + mLow * s1 +
      mHigh * s2) >>> 31) + mLow * s0 + mHigh * s1) >>> 21) + (mHigh * s0 << 10)) >>> (j - 114)
  }

  private[this] def offset(q0: Long) = {
    if (q0 < 100) (9 - q0) >>> 63
    else if (q0 < 10000) ((999 - q0) >>> 63) + 2
    else if (q0 < 1000000) ((99999 - q0) >>> 63) + 4
    else if (q0 < 100000000) ((9999999 - q0) >>> 63) + 6
    else if (q0 < 10000000000L) ((999999999 - q0) >>> 63) + 8
    else if (q0 < 1000000000000L) ((99999999999L - q0) >>> 63) + 10
    else if (q0 < 100000000000000L) ((9999999999999L - q0) >>> 63) + 12
    else if (q0 < 10000000000000000L) ((999999999999999L - q0) >>> 63) + 14
    else if (q0 < 1000000000000000000L) ((99999999999999999L - q0) >>> 63) + 16
    else 18
  }.toInt

  @tailrec
  private[this] def writeNDigits(q0: Long, n: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (n == 0) pos
    else if (n == 1) {
      buf(pos) =
        if (q0 < 10) (q0 + '0').toByte
        else if (q0 < 100) ds(q0.toInt).toByte
        else (q0 % 10 + '0').toByte
      pos - 1
    } else if (q0 == q0.toInt) writeNDigits(q0.toInt, n, pos, buf, ds)
    else {
      val q1 = q0 / 100
      val r1 = (q0 - 100 * q1).toInt
      val d = ds(r1)
      buf(pos - 1) = (d >> 8).toByte
      buf(pos) = d.toByte
      writeNDigits(q1, n - 2, pos - 2, buf, ds)
    }

  @tailrec
  private[this] def pow5Factor(value: Int, count: Int): Int =  {
    val newValue = (value * 3435973837L >> 34).toInt // divide positive int by 5
    if ((newValue << 2) + newValue != value) count
    else pow5Factor(newValue, count + 1)
  }

  private[this] def pow5bits(e: Int): Int =
    if (e == 0) 1
    else ((e * 23219280L + 9999999) * 1801439851L >> 54).toInt // == Math.ceil(e * Math.log(5) / Math.log(2))

  @tailrec
  private[this] def writeNZeros(n: Int, pos: Int, buf: Array[Byte]): Int =
    if (n <= 0) pos
    else if (n == 1) {
      buf(pos) = '0'
      pos + 1
    } else {
      buf(pos) = '0'
      buf(pos + 1) = '0'
      writeNZeros(n - 2, pos + 2, buf)
    }

  private[this] def writeIndention(delta: Int): Unit = if (indention != 0) writeNewLineAndSpaces(delta)

  private[this] def writeNewLineAndSpaces(delta: Int): Unit = count = {
    val toWrite = indention - delta
    var pos = ensureBufCapacity(toWrite + 1)
    buf(pos) = '\n'
    pos += 1
    val to = pos + toWrite
    while (pos < to) pos = {
      buf(pos) = ' '
      pos + 1
    }
    pos
  }

  private[this] def ensureBufCapacity(required: Int): Int = {
    val pos = count
    if (pos + required > buf.length) flushAndGrowBuf(required, pos)
    else pos
  }

  private[this] def flushAndGrowBuf(required: Int, pos: Int): Int =
    if (out eq null) {
      growBuf(pos + required)
      pos
    } else {
      out.write(buf, 0, pos)
      if (required > buf.length) growBuf(required)
      0
    }

  private[jsoniter_scala] def flushBuf(): Unit =
    if (out ne null) {
      out.write(buf, 0, count)
      count = 0
    }

  private[this] def growBuf(required: Int): Unit =
    if (isBufGrowingAllowed) buf = java.util.Arrays.copyOf(buf, Integer.highestOneBit(buf.length | required) << 1)
    else throw new ArrayIndexOutOfBoundsException("`buf` length exceeded")

  private[this] def freeTooLongBuf(): Unit =
    if (buf.length > config.preferredBufSize) buf = new Array[Byte](config.preferredBufSize)
}

object JsonWriter {
  private final val escapedChars: Array[Byte] = {
    val es = new Array[Byte](128)
    java.util.Arrays.fill(es, 0, 32, -1: Byte)
    es('\n') = 'n'
    es('\r') = 'r'
    es('\t') = 't'
    es('\b') = 'b'
    es('\f') = 'f'
    es('\\') = '\\'
    es('\"') = '"'
    es(127) = -1
    es
  }
  private final val digits: Array[Short] = {
    val ds = new Array[Short](100)
    var i = 0
    var j = 0
    do {
      var k = 0
      do {
        ds(i) = (((j + '0') << 8) + (k + '0')).toShort
        i += 1
        k += 1
      } while (k < 10)
      j += 1
    } while (j < 10)
    ds
  }
  private final val hexDigits: Array[Byte] =
    Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  private final val minIntBytes: Array[Byte] = Array('-', '2', '1', '4', '7', '4', '8', '3', '6', '4', '8')
  private final val minLongBytes: Array[Byte] =
    Array('-', '9', '2', '2', '3', '3', '7', '2', '0', '3', '6', '8', '5', '4', '7', '7', '5', '8', '0', '8')
  private final val f32Pow5Split = new Array[Int](94)
  private final val f32Pow5InvSplit = new Array[Int](62)
  private final val f64Pow5Split = new Array[Int](1304)
  private final val f64Pow5InvSplit = new Array[Int](1164)

  {
    var i = 0
    while (i < (Math.max(f32Pow5Split.length, f32Pow5InvSplit.length) >> 1)) {
      val pow5 = BigInteger.valueOf(5).pow(i)
      val pow5len = pow5.bitLength
      if (i < (f32Pow5Split.length >> 1)) {
        val s = pow5.shiftRight(pow5len - 61).longValue
        f32Pow5Split(i * 2) = (s & 2147483647).toInt
        f32Pow5Split(i * 2 + 1) = (s >> 31).toInt
      }
      if (i < (f32Pow5InvSplit.length >> 1)) {
        val s = BigInteger.ONE.shiftLeft(pow5len + 58).divide(pow5).longValue + 1
        f32Pow5InvSplit(i * 2) = (s & 2147483647).toInt
        f32Pow5InvSplit(i * 2 + 1) = (s >> 31).toInt
      }
      i += 1
    }
    val mask = BigInteger.ONE.shiftLeft(31).subtract(BigInteger.ONE)
    i = 0
    while (i < (Math.max(f64Pow5Split.length, f64Pow5InvSplit.length) >> 2)) {
      val pow5 = BigInteger.valueOf(5).pow(i)
      val pow5len = pow5.bitLength
      if (i < (f64Pow5Split.length >> 2)) {
        var j = 0
        while (j < 4) {
          f64Pow5Split(i * 4 + j) = pow5.shiftRight(pow5len - 121 + (3 - j) * 31).and(mask).intValue
          j += 1
        }
      }
      if (i < (f64Pow5InvSplit.length >> 2)) {
        val inv = BigInteger.ONE.shiftLeft(pow5len + 121).divide(pow5).add(BigInteger.ONE)
        var j = 0
        while (j < 4) {
          f64Pow5InvSplit(i * 4 + j) =
            if (j == 0) inv.shiftRight((3 - j) * 31).intValue
            else inv.shiftRight((3 - j) * 31).and(mask).intValue
          j += 1
        }
      }
      i += 1
    }
  }

  final def isNonEscapedAscii(ch: Char): Boolean = ch < 128 && escapedChars(ch) == 0
}