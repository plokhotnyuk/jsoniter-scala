package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.{BufferOverflowException, ByteBuffer}
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter._

import scala.annotation.tailrec
import scala.{specialized => sp}

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter]] that contains params for formatting of
  * output JSON and for tuning of preferred size for internal byte buffer that is created on the writer instantiation
  * and reused in runtime for serialization of messages using [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]].
  * <br/>
  * All configuration params already initialized to default values, but in some cases they should be altered:
  * <ul>
  * <li>turn on pretty printing by specifying of indention step that is greater than 0</li>
  * <li>turn on escaping of Unicode characters to serialize with only ASCII characters</li>
  * <li>increase preferred size of an internal byte buffer to reduce allocation rate of grown and then reduced buffers
  * when writing to [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]] lot of large (>16Kb)
  * [[scala.math.BigDecimal]], [[scala.math.BigInt]] or other non escaped ASCII strings written using
  * `JsonWriter.writeNonEscapedAsciiKey` or `JsonWriter.writeNonEscapedAsciiVal` </li>
  * </ul>
  * @param throwWriterExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                           development
  * @param indentionStep a size of indention for pretty-printed formatting or 0 for compact output
  * @param escapeUnicode a flag to turn on hexadecimal escaping of all non-ASCII chars
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when writing to
  *                         [[java.io.OutputStream]] or [[java.nio.DirectByteBuffer]]
  */
case class WriterConfig(
    throwWriterExceptionWithStackTrace: Boolean = false,
    indentionStep: Int = 0,
    escapeUnicode: Boolean = false,
    preferredBufSize: Int = 16384) {
  if (indentionStep < 0) throw new IllegalArgumentException("'indentionStep' should be not less than 0")
  if (preferredBufSize < 0) throw new IllegalArgumentException("'preferredBufSize' should be not less than 0")
}

class JsonWriterException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause, true, withStackTrace)

final class JsonWriter private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](16384),
    private[this] var count: Int = 0,
    private[this] var limit: Int = 16384,
    private[this] var indention: Int = 0,
    private[this] var comma: Boolean = false,
    private[this] var disableBufGrowing: Boolean = false,
    private[this] var bbuf: ByteBuffer = null,
    private[this] var out: OutputStream = null,
    private[this] var config: WriterConfig = null) {
  def writeKey(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBoolean(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeByte(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Char): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeChar(x)
    writeColon()
  }

  def writeKey(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeShort(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeInt(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeLong(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeFloat(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeDouble(x)
    writeParenthesesWithColon()
  }

  def writeKey(x: BigInt): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBigInteger(x.bigInteger)
    writeParenthesesWithColon()
  }

  def writeKey(x: BigDecimal): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeParenthesesWithColon()
  }

  def writeKey(x: UUID): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
    writeColon()
  }

  def writeKey(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeString(x)
    writeColon()
  }

  def writeNonEscapedAsciiKey(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeNonEscapedAsciiString(x)
    writeColon()
  }

  def writeKey(x: Duration): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeDuration(x)
    writeColon()
  }

  def writeKey(x: Instant): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeInstant(x)
    writeColon()
  }

  def writeKey(x: LocalDate): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDate(x)
    writeColon()
  }

  def writeKey(x: LocalDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDateTime(x)
    writeColon()
  }

  def writeKey(x: LocalTime): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalTime(x)
    writeColon()
  }

  def writeKey(x: MonthDay): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeMonthDay(x)
    writeColon()
  }

  def writeKey(x: OffsetDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetDateTime(x)
    writeColon()
  }

  def writeKey(x: OffsetTime): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetTime(x)
    writeColon()
  }

  def writeKey(x: Period): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writePeriod(x)
    writeColon()
  }

  def writeKey(x: Year): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeYear(x)
    writeColon()
  }

  def writeKey(x: YearMonth): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeYearMonth(x)
    writeColon()
  }

  def writeKey(x: ZonedDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeZonedDateTime(x)
    writeColon()
  }

  def writeKey(x: ZoneId): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeNonEscapedAsciiString(x.getId)
    writeColon()
  }

  def writeKey(x: ZoneOffset): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeZoneOffset(x)
    writeColon()
  }

  def encodeError(msg: String): Nothing =
    throw new JsonWriterException(msg, null, config.throwWriterExceptionWithStackTrace)

  def writeVal(x: BigDecimal): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigDecimal(x.bigDecimal)
  }

  def writeVal(x: BigInt): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigInteger(x.bigInteger)
  }

  def writeVal(x: UUID): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
  }

  def writeVal(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeString(x)
  }

  def writeNonEscapedAsciiVal(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeNonEscapedAsciiString(x)
  }

  def writeVal(x: Duration): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeDuration(x)
  }

  def writeVal(x: Instant): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeInstant(x)
  }

  def writeVal(x: LocalDate): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDate(x)
  }

  def writeVal(x: LocalDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDateTime(x)
  }

  def writeVal(x: LocalTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalTime(x)
  }

  def writeVal(x: MonthDay): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeMonthDay(x)
  }

  def writeVal(x: OffsetDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetDateTime(x)
  }

  def writeVal(x: OffsetTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetTime(x)
  }

  def writeVal(x: Period): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writePeriod(x)
  }

  def writeVal(x: Year): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeYear(x)
  }

  def writeVal(x: YearMonth): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeYearMonth(x)
  }

  def writeVal(x: ZonedDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeZonedDateTime(x)
  }

  def writeVal(x: ZoneId): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeNonEscapedAsciiString(x.getId)
  }

  def writeVal(x: ZoneOffset): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeZoneOffset(x)
  }

  def writeVal(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBoolean(x)
  }

  def writeVal(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeByte(x)
  }

  def writeVal(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeShort(x)
  }

  def writeVal(x: Char): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeChar(x)
  }

  def writeVal(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeInt(x)
  }

  def writeVal(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLong(x)
  }

  def writeVal(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeFloat(x)
  }

  def writeVal(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeDouble(x)
  }

  def writeValAsString(x: BigDecimal): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeBytes('"')
  }

  def writeValAsString(x: BigInt): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBigInteger(x.bigInteger)
    writeBytes('"')
  }

  def writeValAsString(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBoolean(x)
    writeBytes('"')
  }

  def writeValAsString(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeByte(x)
    writeBytes('"')
  }

  def writeValAsString(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeShort(x)
    writeBytes('"')
  }

  def writeValAsString(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeInt(x)
    writeBytes('"')
  }

  def writeValAsString(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeLong(x)
    writeBytes('"')
  }

  def writeValAsString(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeFloat(x)
    writeBytes('"')
  }

  def writeValAsString(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeDouble(x)
    writeBytes('"')
  }

  def writeRawVal(bs: Array[Byte]): Unit = count = {
    val len = bs.length
    val pos = ensureBufCapacity(len)
    System.arraycopy(bs, 0, this.buf, pos, len)
    pos + len
  }

  def writeNull(): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('n', 'u', 'l', 'l')
  }

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
      comma = false
      disableBufGrowing = false
      if (limit < config.preferredBufSize) reallocateBufToPreferredSize()
      codec.encodeValue(x, this)
      out.write(buf, 0, count)
    } finally {
      this.out = null // do not close output stream
      if (limit > config.preferredBufSize) reallocateBufToPreferredSize()
    }

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): Array[Byte] =
    try {
      this.config = config
      count = 0
      indention = 0
      comma = false
      disableBufGrowing = false
      codec.encodeValue(x, this)
      java.util.Arrays.copyOf(buf, count)
    } finally {
      if (limit > config.preferredBufSize) reallocateBufToPreferredSize()
    }

  private[jsoniter_scala] def writeStringWithoutBufReallocation[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): String = {
    this.config = config
    count = 0
    indention = 0
    comma = false
    disableBufGrowing = false
    codec.encodeValue(x, this)
    new String(buf, 0, count, StandardCharsets.UTF_8)
  } // used only once with a new allocated writer, so reallocation of `buf` is not required

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, buf: Array[Byte], from: Int, to: Int, config: WriterConfig): Int = {
    val currBuf = this.buf
    try {
      this.buf = buf
      this.config = config
      count = from
      limit = to
      indention = 0
      comma = false
      disableBufGrowing = true
      codec.encodeValue(x, this)
      count
    } finally {
      setBuf(currBuf)
    }
  }

  private[jsoniter_scala] def write[@sp A](codec: JsonValueCodec[A], x: A, bbuf: ByteBuffer, config: WriterConfig): Unit =
    if (bbuf.hasArray) {
      val offset = bbuf.arrayOffset
      val currBuf = this.buf
      try {
        this.buf = bbuf.array
        this.config = config
        count = bbuf.position() + offset
        limit = bbuf.limit() + offset
        indention = 0
        comma = false
        disableBufGrowing = true
        codec.encodeValue(x, this)
      } catch {
        case _: ArrayIndexOutOfBoundsException => throw new BufferOverflowException
      } finally {
        setBuf(currBuf)
        bbuf.position(count - offset)
      }
    } else {
      try {
        this.bbuf = bbuf
        this.config = config
        count = 0
        indention = 0
        comma = false
        disableBufGrowing = false
        if (limit < config.preferredBufSize) reallocateBufToPreferredSize()
        codec.encodeValue(x, this)
        bbuf.put(buf, 0, count)
      } finally {
        this.bbuf = null
        if (limit > config.preferredBufSize) reallocateBufToPreferredSize()
      }
    }

  private[this] def writeNestedStart(b: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes(b)
    val indentionStep = config.indentionStep
    if (indentionStep != 0) {
      indention += indentionStep
      writeIndention()
    }
  }

  private[this] def writeNestedEnd(b: Byte): Unit = {
    comma = true
    if (indention != 0) {
      indention -= config.indentionStep
      writeIndention()
    }
    writeBytes(b)
  }

  private[this] def writeOptionalCommaAndIndentionBeforeValue(): Unit =
    if (comma) {
      writeBytes(',')
      if (indention != 0) writeIndention()
    } else comma = true

  private[this] def writeOptionalCommaAndIndentionBeforeKey(): Unit =
    if (comma) {
      comma = false
      writeBytes(',')
      if (indention != 0) writeIndention()
    }

  private[this] def writeIndention(): Unit = count = {
    val pos = ensureBufCapacity(indention + 1)
    val buf = this.buf
    buf(pos) = '\n'
    writeNBytes(indention, ' ', pos + 1, buf)
  }

  private[this] def writeParenthesesWithColon(): Unit = count = {
    val pos = ensureBufCapacity(3)
    val buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = ':'
    if (config.indentionStep > 0) {
      buf(pos + 2) = ' '
      pos + 3
    } else pos + 2
  }

  private[this] def writeColon(): Unit = count = {
    val pos = ensureBufCapacity(2)
    val buf = this.buf
    buf(pos) = ':'
    if (config.indentionStep > 0) {
      buf(pos + 1) = ' '
      pos + 2
    } else pos + 1
  }

  private[this] def writeBytes(b: Byte): Unit = count = {
    val pos = ensureBufCapacity(1)
    buf(pos) = b
    pos + 1
  }

  private[this] def writeBytes(b1: Byte, b2: Byte): Unit = count = {
    val pos = ensureBufCapacity(2)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    pos + 2
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte): Unit = count = {
    val pos = ensureBufCapacity(3)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    pos + 3
  }

  private[this] def writeBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte): Unit = count = {
    val pos = ensureBufCapacity(4)
    val buf = this.buf
    buf(pos) = b1
    buf(pos + 1) = b2
    buf(pos + 2) = b3
    buf(pos + 3) = b4
    pos + 4
  }

  private[this] def writeNonEscapedAsciiString(s: String): Unit = count = {
    val len = s.length
    var pos = ensureBufCapacity(len + 2)
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    s.getBytes(0, len, buf, pos)
    pos += len
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeUUID(mostSigBits: Long, leastSigBits: Long): Unit = count = {
    val pos = ensureBufCapacity(38)
    val buf = this.buf
    val ds = hexDigits
    val mostSigBits1 = (mostSigBits >>> 32).toInt
    buf(pos) = '"'
    val d1 = ds(mostSigBits1 >>> 24)
    buf(pos + 1) = (d1 >> 8).toByte
    buf(pos + 2) = d1.toByte
    val d2 = ds((mostSigBits1 >>> 16) & 0xFF)
    buf(pos + 3) = (d2 >> 8).toByte
    buf(pos + 4) = d2.toByte
    val d3 = ds((mostSigBits1 >>> 8) & 0xFF)
    buf(pos + 5) = (d3 >> 8).toByte
    buf(pos + 6) = d3.toByte
    val d4 = ds(mostSigBits1 & 0xFF)
    buf(pos + 7) = (d4 >> 8).toByte
    buf(pos + 8) = d4.toByte
    val mostSigBits2 = mostSigBits.toInt
    buf(pos + 9) = '-'
    val d5 = ds(mostSigBits2 >>> 24)
    buf(pos + 10) = (d5 >> 8).toByte
    buf(pos + 11) = d5.toByte
    val d6 = ds((mostSigBits2 >>> 16) & 0xFF)
    buf(pos + 12) = (d6 >> 8).toByte
    buf(pos + 13) = d6.toByte
    buf(pos + 14) = '-'
    val d7 = ds((mostSigBits2 >>> 8) & 0xFF)
    buf(pos + 15) = (d7 >> 8).toByte
    buf(pos + 16) = d7.toByte
    val d8 = ds(mostSigBits2 & 0xFF)
    buf(pos + 17) = (d8 >> 8).toByte
    buf(pos + 18) = d8.toByte
    val leastSigBits1 = (leastSigBits >>> 32).toInt
    buf(pos + 19) = '-'
    val d9 = ds(leastSigBits1 >>> 24)
    buf(pos + 20) = (d9 >> 8).toByte
    buf(pos + 21) = d9.toByte
    val d10 = ds((leastSigBits1 >>> 16) & 0xFF)
    buf(pos + 22) = (d10 >> 8).toByte
    buf(pos + 23) = d10.toByte
    buf(pos + 24) = '-'
    val d11 = ds((leastSigBits1 >>> 8) & 0xFF)
    buf(pos + 25) = (d11 >> 8).toByte
    buf(pos + 26) = d11.toByte
    val d12 = ds(leastSigBits1 & 0xFF)
    buf(pos + 27) = (d12 >> 8).toByte
    buf(pos + 28) = d12.toByte
    val leastSigBits2 = leastSigBits.toInt
    val d13 = ds(leastSigBits2 >>> 24)
    buf(pos + 29) = (d13 >> 8).toByte
    buf(pos + 30) = d13.toByte
    val d14 = ds((leastSigBits2 >>> 16) & 0xFF)
    buf(pos + 31) = (d14 >> 8).toByte
    buf(pos + 32) = d14.toByte
    val d15 = ds((leastSigBits2 >>> 8) & 0xFF)
    buf(pos + 33) = (d15 >> 8).toByte
    buf(pos + 34) = d15.toByte
    val d16 = ds(leastSigBits2 & 0xFF)
    buf(pos + 35) = (d16 >> 8).toByte
    buf(pos + 36) = d16.toByte
    buf(pos + 37) = '"'
    pos + 38
  }

  private[this] def writeString(s: String): Unit = count = {
    var pos = ensureBufCapacity(2)
    buf(pos) = '"'
    pos = writeString(s, 0, s.length, pos + 1, limit - 1, escapedChars)
    buf(pos) = '"'
    pos + 1
  }

  @tailrec
  private[this] def writeString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeString(s, from, to, flushAndGrowBuf(2, pos), limit - 1, escapedChars)
    else {
      val ch = s.charAt(from)
      buf(pos) = ch.toByte
      if (ch < 0x80 && escapedChars(ch) == 0) writeString(s, from + 1, to, pos + 1, posLim, escapedChars)
      else if (config.escapeUnicode) writeEscapedString(s, from, to, pos, posLim - 12, escapedChars)
      else writeEncodedString(s, from, to, pos, posLim - 6, escapedChars)
    }

  @tailrec
  private[this] def writeEncodedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(s, from, to, flushAndGrowBuf(7, pos), limit - 6, escapedChars)
    else {
      val ch1 = s.charAt(from)
      if (ch1 < 0x80) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
        val esc = escapedChars(ch1)
        if (esc == 0) {
          buf(pos) = ch1.toByte
          writeEncodedString(s, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEncodedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos, buf), posLim, escapedChars)
      } else if (ch1 < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (0xC0 | (ch1 >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch1 & 0x3F)).toByte
        writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
      } else if (ch1 < 0xD800 || ch1 > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (0xE0 | (ch1 >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch1 >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch1 & 0x3F)).toByte
        writeEncodedString(s, from + 1, to, pos + 3, posLim, escapedChars)
      } else { // 110110uuuuccccbb 110111bbbbaaaaaa (UTF-16 chars) -> 11110ddd 10ddcccc 10bbbbbb 10aaaaaa (UTF-8 bytes), where ddddd = uuuu + 1
        if (ch1 >= 0xDC00 || from + 1 >= to) illegalSurrogateError()
        val ch2 = s.charAt(from + 1)
        if (ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        val cp = (ch1 << 10) + (ch2 - 56613888) // -56613888 == 0x010000 - (0xD800 << 10) - 0xDC00
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
    else if (pos >= posLim) writeEscapedString(s, from, to, flushAndGrowBuf(13, pos), limit - 12, escapedChars)
    else {
      val ch1 = s.charAt(from)
      if (ch1 < 0x80) {
        val esc = escapedChars(ch1)
        if (esc == 0) {
          buf(pos) = ch1.toByte
          writeEscapedString(s, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEscapedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos, buf), posLim, escapedChars)
      } else if (ch1 < 0xD800 || ch1 > 0xDFFF) {
        writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1, pos, buf), posLim, escapedChars)
      } else {
        if (ch1 >= 0xDC00 || from + 1 >= to) illegalSurrogateError()
        val ch2 = s.charAt(from + 1)
        if (ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        writeEscapedString(s, from + 2, to, writeEscapedUnicode(ch2, writeEscapedUnicode(ch1, pos, buf), buf), posLim, escapedChars)
      }
    }

  private[this] def writeChar(ch: Char): Unit = count = {
    var pos = ensureBufCapacity(8) // 6 bytes per char for escaped unicode + make room for the quotes
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    pos = {
      if (ch < 0x80) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
        val esc = escapedChars(ch)
        if (esc == 0) {
          buf(pos) = ch.toByte
          pos + 1
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          pos + 2
        } else writeEscapedUnicode(ch.toByte, pos, buf)
      } else if (config.escapeUnicode) {
        if (ch >= 0xD800 && ch <= 0xDFFF) illegalSurrogateError()
        writeEscapedUnicode(ch, pos, buf)
      } else if (ch < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (0xC0 | (ch >> 6)).toByte
        buf(pos + 1) = (0x80 | (ch & 0x3F)).toByte
        pos + 2
      } else if (ch < 0xD800 || ch > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (0xE0 | (ch >> 12)).toByte
        buf(pos + 1) = (0x80 | ((ch >> 6) & 0x3F)).toByte
        buf(pos + 2) = (0x80 | (ch & 0x3F)).toByte
        pos + 3
      } else illegalSurrogateError()
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeEscapedUnicode(ch: Char, pos: Int, buf: Array[Byte]): Int = {
    val ds = hexDigits
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    val d1 = ds(ch >>> 8)
    buf(pos + 2) = (d1 >> 8).toByte
    buf(pos + 3) = d1.toByte
    val d2 = ds(ch & 0xFF)
    buf(pos + 4) = (d2 >> 8).toByte
    buf(pos + 5) = d2.toByte
    pos + 6
  }

  private[this] def writeEscapedUnicode(b: Byte, pos: Int, buf: Array[Byte]): Int = {
    val d = hexDigits(b & 0xFF)
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = '0'
    buf(pos + 3) = '0'
    buf(pos + 4) = (d >> 8).toByte
    buf(pos + 5) = d.toByte
    pos + 6
  }

  private[this] def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private[this] def writeBigInteger(x: BigInteger): Unit =
    if (x.bitLength < 64) writeLong(x.longValue)
    else {
      val n = calculateTenPow18SquareNumber(x)
      val qr = x.divideAndRemainder(tenPow18Squares(n))
      writeBigInteger(qr(0))
      writeBigIntegerReminder(qr(1), n - 1)
    }

  private[this] def writeBigIntegerReminder(x: BigInteger, n: Int): Unit =
    if (n < 0) count = write18Digits(Math.abs(x.longValue), ensureBufCapacity(18), buf, digits)
    else {
      val qr = x.divideAndRemainder(tenPow18Squares(n))
      writeBigIntegerReminder(qr(0), n - 1)
      writeBigIntegerReminder(qr(1), n - 1)
    }

  private[this] def writeBigDecimal(x: java.math.BigDecimal): Unit = {
    val exp = writeBigDecimal(x.unscaledValue, x.scale, 0)
    if (exp != 0) {
      if (exp > 0) writeBytes('E', '+')
      else writeBytes('E')
      writeInt(exp)
    }
  }

  private[this] def writeBigDecimal(x: BigInteger, scale: Int, blockScale: Int): Int =
    if (x.bitLength < 64) {
      val v = x.longValue
      val pos = ensureBufCapacity(28) // == Long.MinValue.toString.length + 8 (for a leading zero, dot, and padding zeroes)
      writeLong(v)
      val blockLen = count - pos + (v >> 63).toInt
      val dotOff = scale - blockScale
      val exp = blockLen - dotOff - 1
      if (scale >= 0 && exp >= -6) {
        if (exp < 0) insertDotWithZeroes(blockLen, -1 - exp)
        else if (dotOff > 0) insertDot(count - dotOff)
        0
      } else {
        if (blockLen > 1 || blockScale > 0) insertDot(count - blockLen + 1)
        exp
      }
    } else {
      val n = calculateTenPow18SquareNumber(x)
      val qr = x.divideAndRemainder(tenPow18Squares(n))
      val exp = writeBigDecimal(qr(0), scale, blockScale + (18 << n))
      writeBigDecimalReminder(qr(1), scale, blockScale, n - 1)
      exp
    }

  private[this] def writeBigDecimalReminder(x: BigInteger, scale: Int, blockScale: Int, n: Int): Unit =
    if (n < 0) {
      count = write18Digits(Math.abs(x.longValue), ensureBufCapacity(19), buf, digits) // 19 == 18 digits and a place for optional dot
      val dotOff = scale - blockScale
      if (dotOff > 0 && dotOff <= 18) insertDot(count - dotOff)
    } else {
      val qr = x.divideAndRemainder(tenPow18Squares(n))
      writeBigDecimalReminder(qr(0), scale, blockScale + (18 << n), n - 1)
      writeBigDecimalReminder(qr(1), scale, blockScale, n - 1)
    }

  private[this] def calculateTenPow18SquareNumber(x: BigInteger): Int = {
    val m = Math.max((x.bitLength * 71828554L >>> 32).toInt - 1, 1) // == Math.max((x.bitLength * Math.log(1e18) / Math.log(2)).toInt - 1, 1)
    31 - java.lang.Integer.numberOfLeadingZeros(m)
  }

  private[this] def insertDotWithZeroes(len: Int, pad: Int): Unit = count = {
    var pos = count + pad + 1
    val buf = this.buf
    val numPos = pos - len
    val off = pad + 2
    while (pos > numPos) {
      buf(pos) = buf(pos - off)
      pos -= 1
    }
    val dotPos = pos - pad
    while (pos > dotPos) {
      buf(pos) = '0'
      pos -= 1
    }
    buf(dotPos) = '.'
    buf(dotPos - 1) = '0'
    count + off
  }

  private[this] def insertDot(dotPos: Int): Unit = count = {
    val buf = this.buf
    var pos = count
    while (pos > dotPos) {
      buf(pos) = buf(pos - 1)
      pos -= 1
    }
    buf(dotPos) = '.'
    count + 1
  }

  private[this] def writeBoolean(x: Boolean): Unit = count = {
    val pos = ensureBufCapacity(5)
    val buf = this.buf
    if (x) {
      buf(pos) = 't'
      buf(pos + 1) = 'r'
      buf(pos + 2) = 'u'
      buf(pos + 3) = 'e'
      pos + 4
    } else {
      buf(pos) = 'f'
      buf(pos + 1) = 'a'
      buf(pos + 2) = 'l'
      buf(pos + 3) = 's'
      buf(pos + 4) = 'e'
      pos + 5
    }
  }

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
      val d = digits(q0 - 100)
      buf(pos) = '1'
      buf(pos + 1) = (d >> 8).toByte
      buf(pos + 2) = d.toByte
      pos + 3
    }
  }

  private[this] def writeDuration(x: Duration): Unit = count = {
    var pos = ensureBufCapacity(40) // 40 == "PT-1111111111111111H-11M-11.111111111S".length + 2
    val buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = 'P'
    buf(pos + 2) = 'T'
    pos += 3
    if (x.isZero) {
      buf(pos) = '0'
      buf(pos + 1) = 'S'
      buf(pos + 2) = '"'
      pos + 3
    } else {
      val ds = digits
      val totalSecs = x.getSeconds
      val nanos = x.getNano
      val effectiveTotalSecs =
        if (totalSecs < 0 && nanos > 0) totalSecs + 1
        else totalSecs
      val hours = effectiveTotalSecs / 3600 // 3600 == seconds in a hour
      if (hours != 0) {
        val q0 =
          if (hours > 0) hours
          else {
            buf(pos) = '-'
            pos += 1
            -hours
          }
        pos =
          if (q0.toInt == q0) writePositiveInt(q0.toInt, pos, buf, ds)
          else {
            val q1 = q0 / 100000000
            val r1 = (q0 - 100000000 * q1).toInt
            write8Digits(r1, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
          }
        buf(pos) = 'H'
        pos += 1
      }
      val secsOfHour = (effectiveTotalSecs - hours * 3600).toInt
      val minutes = secsOfHour / 60
      if (minutes != 0) {
        val q0 =
          if (minutes > 0) minutes
          else {
            buf(pos) = '-'
            pos += 1
            -minutes
          }
        if (q0 < 10) {
          buf(pos) = (q0 + '0').toByte
          pos += 1
        } else {
          val d = ds(q0)
          buf(pos) = (d >> 8).toByte
          buf(pos + 1) = d.toByte
          pos += 2
        }
        buf(pos) = 'M'
        pos += 1
      }
      val seconds = secsOfHour - minutes * 60
      if (seconds == 0 && nanos == 0) {
        buf(pos) = '"'
        pos + 1
      } else {
        if (totalSecs < 0 && seconds == 0 && nanos != 0) {
          buf(pos) = '-'
          buf(pos + 1) = '0'
          pos += 2
        } else {
          val q0: Int =
            if (seconds >= 0) seconds
            else {
              buf(pos) = '-'
              pos += 1
              -seconds
            }
          if (q0 < 10) {
            buf(pos) = (q0 + '0').toByte
            pos += 1
          } else {
            val d = ds(q0)
            buf(pos) = (d >> 8).toByte
            buf(pos + 1) = d.toByte
            pos += 2
          }
        }
        if (nanos == 0) {
          buf(pos) = 'S'
          buf(pos + 1) = '"'
          pos + 2
        } else {
          val q0 =
            if (totalSecs < 0) 1000000000 - nanos
            else nanos
          val dotPos = pos
          pos = writeSignificantFractionDigits(q0, pos + 9, pos, 0, buf, ds)
          buf(dotPos) = '.'
          buf(pos) = 'S'
          buf(pos + 1) = '"'
          pos + 2
        }
      }
    }
  }

  @tailrec
  private[this] def writeSignificantFractionDigits(q0: Int, pos: Int, posLim: Int, lastPos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (pos > posLim) {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
      val r1 = q0 - q1 * 100
      if (lastPos != 0 || r1 != 0) {
        val d = ds(r1)
        buf(pos - 1) = (d >> 8).toByte
        buf(pos) = d.toByte
        if (lastPos == 0) {
          val newLastPos =
            if (d.toByte == '0') pos
            else pos + 1
          writeSignificantFractionDigits(q1, pos - 2, posLim, newLastPos, buf, ds)
        } else writeSignificantFractionDigits(q1, pos - 2, posLim, lastPos, buf, ds)
      } else writeSignificantFractionDigits(q1, pos - 2, posLim, lastPos, buf, ds)
    } else lastPos

  private[this] def writeInstant(x: Instant): Unit = count = {
    val epochSecond = x.getEpochSecond
    val epochDay =
      (if (epochSecond >= 0) epochSecond
      else epochSecond - 86399) / 86400 // 86400 == seconds per day
    var marchZeroDay = epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    var adjustYear = 0
    if (marchZeroDay < 0) { // adjust negative years to positive for calculation
      val adjust400YearCycles = to400YearCycle(marchZeroDay + 1) - 1
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L
    }
    var yearEst = to400YearCycle(400 * marchZeroDay + 591)
    var marchDayOfYear = toMarchDayOfYear(marchZeroDay, yearEst)
    if (marchDayOfYear < 0) { // fix estimate
      yearEst -= 1
      marchDayOfYear = toMarchDayOfYear(marchZeroDay, yearEst)
    }
    yearEst += adjustYear // reset any negative year
    val marchMonth = ((marchDayOfYear * 17965876275L + 7186350510L) >> 39).toInt // == (marchDayOfYear * 5 + 2) / 153
    val year = yearEst + (marchMonth * 3435973837L >> 35).toInt // == yearEst + marchMonth / 10 (convert march-based values back to january-based)
    val month = marchMonth +
      (if (marchMonth < 10) 3
      else -9)
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
    pos = writeLocalTime(hour, minute, second, x.getNano, pos + 1, buf, ds)
    buf(pos) = 'Z'
    buf(pos + 1) = '"'
    pos + 2
  }

  private[this] def to400YearCycle(day: Long): Int = (day / 146097).toInt // 146097 == number of days in a 400 year cycle

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

  private[this] def writePeriod(x: Period): Unit = count = {
    var pos = ensureBufCapacity(39) // 39 == "P-2147483648Y-2147483648M-2147483648D".length + 2
    val buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = 'P'
    pos += 2
    if (x.isZero) {
      buf(pos) = '0'
      buf(pos + 1) = 'D'
      buf(pos + 2) = '"'
      pos + 3
    } else {
      val ds = digits
      val years = x.getYears
      if (years != 0) {
        val q0 =
          if (years >= 0) years
          else if (years != -2147483648) {
            buf(pos) = '-'
            pos += 1
            -years
          } else {
            buf(pos) = '-'
            buf(pos + 1) = '2'
            pos += 2
            147483648
          }
        pos = writePositiveInt(q0, pos, buf, ds)
        buf(pos) = 'Y'
        pos += 1
      }
      val months = x.getMonths
      if (months != 0) {
        val q0 =
          if (months >= 0) months
          else if (months != -2147483648) {
            buf(pos) = '-'
            pos += 1
            -months
          } else {
            buf(pos) = '-'
            buf(pos + 1) = '2'
            pos += 2
            147483648
          }
        pos = writePositiveInt(q0, pos, buf, ds)
        buf(pos) = 'M'
        pos += 1
      }
      val days = x.getDays
      if (days != 0) {
        val q0 =
          if (days >= 0) days
          else if (days != -2147483648) {
            buf(pos) = '-'
            pos += 1
            -days
          } else {
            buf(pos) = '-'
            buf(pos + 1) = '2'
            pos += 2
            147483648
          }
        pos = writePositiveInt(q0, pos, buf, ds)
        buf(pos) = 'D'
        pos += 1
      }
      buf(pos) = '"'
      pos + 1
    }
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
      if (pos + required > limit) {
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

  private[this] def writeLocalDate(x: LocalDate, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = writeYear(x.getYear, p, buf, ds)
    buf(pos) = '-'
    pos = write2Digits(x.getMonthValue, pos + 1, buf, ds)
    buf(pos) = '-'
    write2Digits(x.getDayOfMonth, pos + 1, buf, ds)
  }

  private[this] def writeLocalDate(year: Int, month: Int, day: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = writeYear(year, p, buf, ds)
    buf(pos) = '-'
    pos = write2Digits(month, pos + 1, buf, ds)
    buf(pos) = '-'
    write2Digits(day, pos + 1, buf, ds)
  }

  private[this] def writeYearMonth(year: Int, month: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYear(year, p, buf, ds)
    buf(pos) = '-'
    write2Digits(month, pos + 1, buf, ds)
  }

  private[this] def writeYear(year: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (year >= 0) {
      if (year < 10000) write4Digits(year, pos, buf, ds)
      else {
        buf(pos) = '+'
        writePositiveInt(year, pos + 1, buf, ds)
      }
    } else {
      buf(pos) = '-'
      if (year > -10000) write4Digits(-year, pos + 1, buf, ds)
      else writePositiveInt(-year, pos + 1, buf, ds)
    }

  private[this] def writeLocalTime(x: LocalTime, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = write2Digits(x.getHour, p, buf, ds)
    buf(pos) = ':'
    pos = write2Digits(x.getMinute, pos + 1, buf, ds)
    val second = x.getSecond
    val nano = x.getNano
    if (second != 0 || nano != 0) {
      buf(pos) = ':'
      pos = write2Digits(second, pos + 1, buf, ds)
      if (nano != 0) {
        buf(pos) = '.'
        val q1 = (nano * 1801439851L >> 54).toInt // divide positive int by 10000000
        val r1 = nano - 10000000 * q1
        pos = write2Digits(q1, pos + 1, buf, ds)
        val q2 = (r1 * 175921861L >> 44).toInt // divide positive int by 100000
        val r2 = r1 - 100000 * q2
        val d = ds(q2)
        buf(pos) = (d >> 8).toByte
        pos += 1
        if (r2 != 0 || d.toByte != '0') {
          buf(pos) = d.toByte
          val q3 = (r2 * 2199023256L >> 41).toInt // divide positive int by 1000
          val r3 = r2 - q3 * 1000
          pos = write2Digits(q3, pos + 1, buf, ds)
          if (r3 != 0) pos = write3Digits(r3, pos, buf, ds)
        }
      }
    }
    pos
  }

  private[this] def writeLocalTime(hour: Int, minute: Int, second: Int, nano: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = write2Digits(hour, p, buf, ds)
    buf(pos) = ':'
    pos = write2Digits(minute, pos + 1, buf, ds)
    buf(pos) = ':'
    pos = write2Digits(second, pos + 1, buf, ds)
    if (nano != 0) {
      buf(pos) = '.'
      val q1 = (nano * 1801439851L >> 54).toInt // divide positive int by 10000000
      val r1 = nano - 10000000 * q1
      pos = write2Digits(q1, pos + 1, buf, ds)
      val q2 = (r1 * 175921861L >> 44).toInt // divide positive int by 100000
      val r2 = r1 - 100000 * q2
      val d = ds(q2)
      buf(pos) = (d >> 8).toByte
      pos += 1
      if (r2 != 0 || d.toByte != '0') {
        buf(pos) = d.toByte
        val q3 = (r2 * 2199023256L >> 41).toInt // divide positive int by 1000
        val r3 = r2 - q3 * 1000
        pos = write2Digits(q3, pos + 1, buf, ds)
        if (r3 != 0) pos = write3Digits(r3, pos, buf, ds)
      }
    }
    pos
  }

  private[this] def writeOffset(x: ZoneOffset, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val totalSeconds = x.getTotalSeconds
    if (totalSeconds == 0) {
      buf(p) = 'Z'
      p + 1
    } else {
      val q0 =
        if (totalSeconds > 0) {
          buf(p) = '+'
          totalSeconds
        } else {
          buf(p) = '-'
          -totalSeconds
        }
      val q1 = (q0 * 2443359173L >> 43).toInt // divide positive int by 3600
      val r1 = q0 - q1 * 3600
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

  private[this] def write2Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val d = ds(q0)
    buf(pos) = (d >> 8).toByte
    buf(pos + 1) = d.toByte
    pos + 2
  }

  private[this] def write3Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
    val d = ds(q0 - 100 * q1)
    buf(pos) = (q1 + '0').toByte
    buf(pos + 1) = (d >> 8).toByte
    buf(pos + 2) = d.toByte
    pos + 3
  }

  private[this] def write4Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
    val d1 = ds(q1)
    val d2 = ds(q0 - 100 * q1)
    buf(pos) = (d1 >> 8).toByte
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d2 >> 8).toByte
    buf(pos + 3) = d2.toByte
    pos + 4
  }

  private[this] def write8Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (q0 * 3518437209L >> 45).toInt // divide positive int by 10000
    val q2 = (q1 * 1374389535L >> 37).toInt // divide positive int by 100
    val d1 = ds(q2)
    val d2 = ds(q1 - 100 * q2)
    buf(pos) = (d1 >> 8).toByte
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d2 >> 8).toByte
    buf(pos + 3) = d2.toByte
    val r1 = q0 - 10000 * q1
    val q3 = (r1 * 1374389535L >> 37).toInt // divide positive int by 100
    val d3 = ds(q3)
    val d4 = ds(r1 - 100 * q3)
    buf(pos + 4) = (d3 >> 8).toByte
    buf(pos + 5) = d3.toByte
    buf(pos + 6) = (d4 >> 8).toByte
    buf(pos + 7) = d4.toByte
    pos + 8
  }

  private[this] def write18Digits(q0: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 / 100000000
    val r1 = (q0 - 100000000 * q1).toInt
    val q2 = (q1 >> 8) * 1441151881 >> 49 // divide small positive long by 100000000
    val r2 = (q1 - 100000000 * q2).toInt
    write8Digits(r1, write8Digits(r2, write2Digits(q2.toInt, pos, buf, ds), buf, ds), buf, ds)
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
      buf(pos) = (q1 + '0').toByte
      write4Digits(q0 - 10000 * q1, pos + 1, buf, digits)
    }
  }

  private[this] def writeInt(x: Int): Unit = count = {
    var pos = ensureBufCapacity(11) // minIntBytes.length
    val buf = this.buf
    val q0 =
      if (x >= 0) x
      else if (x != -2147483648) {
        buf(pos) = '-'
        pos += 1
        -x
      } else {
        buf(pos) = '-'
        buf(pos + 1) = '2'
        pos += 2
        147483648
      }
    writePositiveInt(q0, pos, buf, digits)
  }

  private[this] def writeLong(x: Long): Unit = count = {
    var pos = ensureBufCapacity(20) // minLongBytes.length
    val buf = this.buf
    val ds = digits
    val q0 =
      if (x >= 0) x
      else if (x != -9223372036854775808L) {
        buf(pos) = '-'
        pos += 1
        -x
      } else {
        buf(pos) = '-'
        buf(pos + 1) = '9'
        pos += 2
        223372036854775808L
      }
    if (q0.toInt == q0) writePositiveInt(q0.toInt, pos, buf, ds)
    else {
      val q1 = q0 / 100000000
      val r1 = (q0 - 100000000 * q1).toInt
      if (q1.toInt == q1) write8Digits(r1, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
      else {
        val q2 = (q1 >> 8) * 1441151881 >> 49 // divide small positive long by 100000000
        val r2 = (q1 - 100000000 * q2).toInt
        write8Digits(r1, write8Digits(r2, writePositiveInt(q2.toInt, pos, buf, ds), buf, ds), buf, ds)
      }
    }
  }

  private[this] def writePositiveInt(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val lastPos = offset(q0) + pos
    writePositiveIntStartingFromLastPosition(q0, lastPos, buf, ds)
    lastPos + 1
  }

  // Based on a great work of Ulf Adams:
  // http://delivery.acm.org/10.1145/3200000/3192369/pldi18main-p10-p.pdf
  // https://github.com/ulfjack/ryu/blob/62925340e4abc76e3c63b6de8dea1486d6970260/src/main/java/info/adams/ryu/RyuFloat.java
  // Also, see his presentation "Ryū - Fast Float-to-String Conversion": https://www.youtube.com/watch?v=kw-U6smcLzk
  private[this] def writeFloat(x: Float): Unit = {
    val bits = java.lang.Float.floatToRawIntBits(x)
    if (bits == 0) writeBytes('0', '.', '0')
    else if (bits == 0x80000000) writeBytes('-', '0', '.', '0')
    else count = {
      val ieeeExponent = (bits >> 23) & 0xFF
      if (ieeeExponent == 255) encodeError("illegal number: " + x)
      val ieeeMantissa = bits & 0x7FFFFF
      val e =
        if (ieeeExponent == 0) -149
        else ieeeExponent - 150
      val m =
        if (ieeeExponent == 0) ieeeMantissa
        else ieeeMantissa | 0x800000
      var decimalNotation = false
      var dv, exp, len = 0
      if (e >= -23 && e <= 0 && multiplePowOf2(m, -e)) {
        dv = m >> -e
        var newDv = 0
        while ((dv >= 100) && {
          newDv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
          newDv * 10 == dv
        }) {
          dv = newDv
          exp += 1
        }
        len = offset(dv)
        exp += len
        len += 1
        decimalNotation = exp < 7
      } else {
        val even = (m & 0x1) == 0
        val mv = m << 2
        val mp = mv + 2
        val mmShift =
          if (ieeeMantissa != 0 || ieeeExponent <= 1) 1
          else 0
        val mm = mv - 1 - mmShift
        val e2 = e - 2
        var dp, dm = 0
        var dvIsTrailingZeros, dmIsTrailingZeros = false
        if (e2 >= 0) {
          val ss = f32Pow5InvSplit
          val q = (e2 * 1292913986L >> 32).toInt // == (e2 * Math.log10(2)).toInt
          val i = -e2 + q + Math.max(0, (q * 9972605231L >> 32).toInt) + 59 // == -e2 + q + Math.max(0, (q * Math.log(5) / Math.log(2)).toInt) + 59
          exp = q
          dv = mulPow5DivPow2(mv, q, i, ss)
          dp = mulPow5DivPow2(mp, q, i, ss)
          dm = mulPow5DivPow2(mm, q, i, ss)
          if (q <= 9) {
            val mv5 = (mv * 3435973837L >> 34).toInt // divide positive int by 5
            if ((mv5 << 2) + mv5 == mv) dvIsTrailingZeros = multiplePowOf5(mv5, q - 1)
            else if (even) dmIsTrailingZeros = multiplePowOf5(mm, q)
            else if (multiplePowOf5(mp, q)) dp -= 1
          }
        } else {
          val ss = f32Pow5Split
          val q = (-e2 * 3002053309L >> 32).toInt // == (-e2 * Math.log10(5)).toInt
          val i = -e2 - q
          val j = q - Math.max(0, (i * 9972605231L >> 32).toInt) + 60 // == q - Math.max(0, (i * Math.log(5) / Math.log(2)).toInt) + 60
          exp = -i
          dv = mulPow5DivPow2(mv, i, j, ss)
          dp = mulPow5DivPow2(mp, i, j, ss)
          dm = mulPow5DivPow2(mm, i, j, ss)
          if (q <= 1) {
            dvIsTrailingZeros = true
            if (even) dmIsTrailingZeros = mmShift == 1
            else dp -= 1
          } else if (q < 31) dvIsTrailingZeros = multiplePowOf2(mv, q)
        }
        len = offset(dp)
        exp += len
        len += 1
        decimalNotation = exp >= -3 && exp < 7
        if (dmIsTrailingZeros || dvIsTrailingZeros) {
          var newDp, newDm, lastRemovedDigit = 0
          while ((decimalNotation || dp >= 100) && {
            newDp = (dp * 3435973837L >> 35).toInt // divide positive int by 10
            newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
            newDp > newDm
          }) {
            dp = newDp
            dmIsTrailingZeros &= newDm * 10 == dm
            dm = newDm
            dvIsTrailingZeros &= lastRemovedDigit == 0
            val newDv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
            lastRemovedDigit = dv - newDv * 10
            dv = newDv
            len -= 1
          }
          if (dmIsTrailingZeros && even) {
            while ((decimalNotation || dp >= 100) && {
              newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
              newDm * 10 == dm
            }) {
              dp = (dp * 3435973837L >> 35).toInt
              dm = newDm
              dvIsTrailingZeros &= lastRemovedDigit == 0
              val newDv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
              lastRemovedDigit = dv - newDv * 10
              dv = newDv
              len -= 1
            }
          }
          if (!(dvIsTrailingZeros && lastRemovedDigit == 5 && (dv & 0x1) == 0 ||
            (lastRemovedDigit < 5 && (dv != dm || dmIsTrailingZeros && even)))) dv += 1
        } else {
          var newDp, newDm = 0
          var oldDv = dv
          while ((decimalNotation || dp >= 1000) && {
            newDp = (dp * 1374389535L >> 37).toInt // divide positive int by 100
            newDm = (dm * 1374389535L >> 37).toInt // divide positive int by 100
            newDp > newDm
          }) {
            oldDv = dv
            dv = (dv * 1374389535L >> 37).toInt // divide positive int by 100
            dm = newDm
            dp = newDp
            len -= 2
          }
          val roundUp =
            if ((decimalNotation || dp >= 100) && {
              newDm = (dm * 3435973837L >> 35).toInt // divide positive int by 10
              (dp * 3435973837L >> 35).toInt > newDm // divide positive int by 10
            }) {
              oldDv = dv
              dv = (dv * 3435973837L >> 35).toInt // divide positive int by 10
              dm = newDm
              len -= 1
              oldDv - dv * 10 >= 5
            } else oldDv - dv * 100 >= 50
          if (roundUp || dv == dm) dv += 1
        }
      }
      var pos = ensureBufCapacity(15)
      val buf = this.buf
      if (bits < 0) {
        buf(pos) = '-'
        pos += 1
      }
      val ds = digits
      if (decimalNotation) {
        if (exp < 0) {
          buf(pos) = '0'
          buf(pos + 1) = '.'
          pos = writeNBytes(-1 - exp, '0', pos + 2, buf)
          writePositiveIntStartingFromLastPosition(dv, pos + len - 1, buf, ds)
          pos + len
        } else if (exp + 1 >= len) {
          writePositiveIntStartingFromLastPosition(dv, pos + len - 1, buf, ds)
          pos = writeNBytes(exp - len + 1, '0', pos + len, buf)
          buf(pos) = '.'
          buf(pos + 1) = '0'
          pos + 2
        } else {
          val lastPos = pos + len
          val dotPos = pos + exp + 1
          writePositiveIntStartingFromLastPosition(dv, lastPos, buf, ds)
          while (pos < dotPos) {
            buf(pos) = buf(pos + 1)
            pos += 1
          }
          buf(pos) = '.'
          lastPos + 1
        }
      } else {
        writePositiveIntStartingFromLastPosition(dv, pos + len, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos += len + 1
        buf(pos) = 'E'
        pos += 1
        if (exp < 0) {
          buf(pos) = '-'
          pos += 1
          exp = -exp
        }
        if (exp < 10) {
          buf(pos) = (exp + '0').toByte
          pos + 1
        } else write2Digits(exp, pos, buf, ds)
      }
    }
  }

  private[this] def mulPow5DivPow2(m: Long, i: Int, j: Int, ss: Array[Int]): Int = {
    val idx = i << 1
    ((m * ss(idx + 1) + (m * ss(idx) >> 31)) >> (j - 31)).toInt
  }

  // Based on a great work of Ulf Adams:
  // http://delivery.acm.org/10.1145/3200000/3192369/pldi18main-p10-p.pdf
  // https://github.com/ulfjack/ryu/blob/62925340e4abc76e3c63b6de8dea1486d6970260/src/main/java/info/adams/ryu/RyuDouble.java
  // Also, see his presentation "Ryū - Fast Float-to-String Conversion": https://www.youtube.com/watch?v=kw-U6smcLzk
  private[this] def writeDouble(x: Double): Unit = {
    val bits = java.lang.Double.doubleToRawLongBits(x)
    if (bits == 0) writeBytes('0', '.', '0')
    else if (bits == 0x8000000000000000L) writeBytes('-', '0', '.', '0')
    else count = {
      val ieeeExponent = ((bits >> 52) & 0x7FF).toInt
      if (ieeeExponent == 2047) encodeError("illegal number: " + x)
      val ieeeMantissa = bits & 0xFFFFFFFFFFFFFL
      val e =
        if (ieeeExponent == 0) -1074
        else ieeeExponent - 1075
      val m =
        if (ieeeExponent == 0) ieeeMantissa
        else ieeeMantissa | 0x10000000000000L
      var decimalNotation = false
      var dv = 0L
      var exp, len = 0
      if (e >= -52 && e <= 0 && multiplePowOf2(m, -e)) {
        dv = m >> -e
        var newDv = 0L
        while ((dv >= 100) && {
          newDv = dv / 10
          newDv * 10 == dv
        }) {
          dv = newDv
          exp += 1
        }
        len = offset(dv)
        exp += len
        len += 1
        decimalNotation = exp < 7
      } else {
        val even = (m & 0x1) == 0
        val mv = m << 2
        val mp = mv + 2
        val mmShift =
          if (ieeeMantissa != 0 || ieeeExponent <= 1) 1
          else 0
        val mm = mv - 1 - mmShift
        val e2 = e - 2
        var dp, dm = 0L
        var dvIsTrailingZeros, dmIsTrailingZeros = false
        if (e2 >= 0) {
          val ss = f64Pow5InvSplit
          val q = Math.max(0, (e2 * 1292913986L >> 32).toInt - 1) // == Math.max(0, (e2 * Math.log10(2)).toInt - 1)
          val i = -e2 + q + Math.max(0, (q * 9972605231L >> 32).toInt) + 122 // == -e2 + q + Math.max(0, (q * Math.log(5) / Math.log(2)).toInt) + 122
          exp = q
          dv = fullMulPow5DivPow2(mv, q, i, ss)
          dp = fullMulPow5DivPow2(mp, q, i, ss)
          dm = fullMulPow5DivPow2(mm, q, i, ss)
          if (q <= 21) {
            val mv5 = mv / 5
            if ((mv5 << 2) + mv5 == mv) dvIsTrailingZeros = multiplePowOf5(mv5, q - 1)
            else if (even) dmIsTrailingZeros = multiplePowOf5(mm, q)
            else if (multiplePowOf5(mp, q)) dp -= 1
          }
        } else {
          val ss = f64Pow5Split
          val q = Math.max(0, (-e2 * 3002053309L >> 32).toInt - 1) // == Math.max(0, (-e2 * Math.log10(5)).toInt - 1)
          val i = -e2 - q
          val j = q - Math.max(0, (i * 9972605231L >> 32).toInt) + 120 // == q - Math.max(0, (i * Math.log(5) / Math.log(2)).toInt) + 120
          exp = -i
          dv = fullMulPow5DivPow2(mv, i, j, ss)
          dp = fullMulPow5DivPow2(mp, i, j, ss)
          dm = fullMulPow5DivPow2(mm, i, j, ss)
          if (q <= 1) {
            dvIsTrailingZeros = true
            if (even) dmIsTrailingZeros = mmShift == 1
            else dp -= 1
          } else if (q < 63) dvIsTrailingZeros = multiplePowOf2(mv, q)
        }
        len = offset(dp)
        exp += len
        len += 1
        decimalNotation = exp >= -3 && exp < 7
        if (dmIsTrailingZeros || dvIsTrailingZeros) {
          var newDp, newDm, lastRemovedDigit = 0L
          while ((decimalNotation || dp >= 100) && {
            newDp = dp / 10
            newDm = dm / 10
            newDp > newDm
          }) {
            dp = newDp
            dmIsTrailingZeros &= newDm * 10 == dm
            dm = newDm
            dvIsTrailingZeros &= lastRemovedDigit == 0
            val newDv = dv / 10
            lastRemovedDigit = dv - newDv * 10
            dv = newDv
            len -= 1
          }
          if (dmIsTrailingZeros && even) {
            while ((decimalNotation || dp >= 100) && {
              newDm = dm / 10
              newDm * 10 == dm
            }) {
              dp /= 10
              dm = newDm
              dvIsTrailingZeros &= lastRemovedDigit == 0
              val newDv = dv / 10
              lastRemovedDigit = dv - newDv * 10
              dv = newDv
              len -= 1
            }
          }
          if (!(dvIsTrailingZeros && lastRemovedDigit == 5 && (dv & 0x1) == 0 ||
            (lastRemovedDigit < 5 && (dv != dm || dmIsTrailingZeros && even)))) dv += 1
        } else {
          var newDp, newDm = 0L
          var oldDv = dv
          while ((decimalNotation || dp >= 1000) && {
            newDp = dp / 100
            newDm = dm / 100
            newDp > newDm
          }) {
            oldDv = dv
            dv /= 100
            dp = newDp
            dm = newDm
            len -= 2
          }
          val roundUp =
            if ((decimalNotation || dp >= 100) && {
              newDm = dm / 10
              dp / 10 > newDm
            }) {
              oldDv = dv
              dv /= 10
              dm = newDm
              len -= 1
              oldDv - dv * 10 >= 5
            } else oldDv - dv * 100 >= 50
          if (roundUp || dv == dm) dv += 1
        }
      }
      var pos = ensureBufCapacity(24)
      val buf = this.buf
      if (bits < 0) {
        buf(pos) = '-'
        pos += 1
      }
      val ds = digits
      if (decimalNotation) {
        if (exp < 0) {
          buf(pos) = '0'
          buf(pos + 1) = '.'
          pos = writeNBytes(-1 - exp, '0', pos + 2, buf)
          writeSmallPositiveLongStartingFromLastPosition(dv, pos + len - 1, buf, ds)
          pos + len
        } else if (exp + 1 >= len) {
          writeSmallPositiveLongStartingFromLastPosition(dv, pos + len - 1, buf, ds)
          pos = writeNBytes(exp - len + 1, '0', pos + len, buf)
          buf(pos) = '.'
          buf(pos + 1) = '0'
          pos + 2
        } else {
          val lastPos = pos + len
          val dotPos = pos + exp + 1
          writeSmallPositiveLongStartingFromLastPosition(dv, lastPos, buf, ds)
          while (pos < dotPos) {
            buf(pos) = buf(pos + 1)
            pos += 1
          }
          buf(pos) = '.'
          lastPos + 1
        }
      } else {
        writeSmallPositiveLongStartingFromLastPosition(dv, pos + len, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos += len + 1
        buf(pos) = 'E'
        pos += 1
        if (exp < 0) {
          buf(pos) = '-'
          pos += 1
          exp = -exp
        }
        if (exp < 10) {
          buf(pos) = (exp + '0').toByte
          pos + 1
        } else if (exp < 100) write2Digits(exp, pos, buf, ds)
        else write3Digits(exp, pos, buf, ds)
      }
    }
  }

  private[this] def multiplePowOf2(q0: Long, q: Int): Boolean = (q0 & ((1L << q) - 1)) == 0

  @tailrec
  private[this] def multiplePowOf5(q0: Long, q: Int): Boolean = q <= 0 || {
    val q1 = q0 / 5
    (q1 << 2) + q1 == q0 && multiplePowOf5(q1, q - 1)
  }

  private[this] def fullMulPow5DivPow2(m: Long, i: Int, j: Int, ss: Array[Int]): Long = {
    val ml = m & 0x7FFFFFFF
    val mh = m >>> 31
    val idx = i << 2
    val s3 = ss(idx + 3)
    val s2 = ss(idx + 2)
    val s1 = ss(idx + 1)
    val s0 = ss(idx)
    ((((((((ml * s3 >>> 31) + ml * s2 + mh * s3) >>> 31) + ml * s1 +
      mh * s2) >>> 31) + ml * s0 + mh * s1) >>> 21) + (mh * s0 << 10)) >>> (j - 114)
  }

  private[this] def offset(q0: Long): Int = {
    if (q0.toInt == q0) offset(q0.toInt)
    else if (q0 < 10000000000L) ((999999999 - q0) >>> 63) + 8
    else if (q0 < 1000000000000L) ((99999999999L - q0) >>> 63) + 10
    else if (q0 < 100000000000000L) ((9999999999999L - q0) >>> 63) + 12
    else if (q0 < 10000000000000000L) ((999999999999999L - q0) >>> 63) + 14
    else if (q0 < 1000000000000000000L) ((99999999999999999L - q0) >>> 63) + 16
    else 18
  }.toInt

  private[this] def offset(q0: Int): Int =
    if (q0 < 100) (9 - q0) >>> 31
    else if (q0 < 10000) ((999 - q0) >>> 31) + 2
    else if (q0 < 1000000) ((99999 - q0) >>> 31) + 4
    else if (q0 < 100000000) ((9999999 - q0) >>> 31) + 6
    else ((999999999 - q0) >>> 31) + 8

  private[this] def writeSmallPositiveLongStartingFromLastPosition(q0: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Unit =
    if (q0.toInt == q0) writePositiveIntStartingFromLastPosition(q0.toInt, pos, buf, ds)
    else {
      val q1 = q0 / 100000000
      writePositiveIntStartingFromLastPosition(q1.toInt, pos - 8, buf, ds)
      write8Digits((q0 - 100000000 * q1).toInt, pos - 7, buf, ds)
    }

  @tailrec
  private[this] def writePositiveIntStartingFromLastPosition(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Unit =
    if (q0 < 100) {
      if (q0 < 10) buf(pos) = (q0 + '0').toByte
      else {
        val d = ds(q0)
        buf(pos - 1) = (d >> 8).toByte
        buf(pos) = d.toByte
      }
    } else {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide positive int by 100
      val d = ds(q0 - 100 * q1)
      buf(pos - 1) = (d >> 8).toByte
      buf(pos) = d.toByte
      writePositiveIntStartingFromLastPosition(q1, pos - 2, buf, ds)
    }

  private[this] def multiplePowOf2(q0: Int, q: Int): Boolean = (q0 & ((1 << q) - 1)) == 0

  @tailrec
  private[this] def multiplePowOf5(q0: Int, q: Int): Boolean = q <= 0 || {
    val q1 = (q0 * 3435973837L >> 34).toInt // divide positive int by 5
    (q1 << 2) + q1 == q0 && multiplePowOf5(q1, q - 1)
  }

  @tailrec
  private[this] def writeNBytes(n: Int, b: Byte, pos: Int, buf: Array[Byte]): Int =
    if (n == 0) pos
    else {
      buf(pos) = b
      writeNBytes(n - 1, b, pos + 1, buf)
    }

  private[this] def ensureBufCapacity(required: Int): Int = {
    val pos = count
    if (pos + required > limit) flushAndGrowBuf(required, pos)
    else pos
  }

  private[this] def flushAndGrowBuf(required: Int, pos: Int): Int =
    if (bbuf ne null) {
      bbuf.put(buf, 0, pos)
      if (required > limit) growBuf(required)
      0
    } else if (out ne null) {
      out.write(buf, 0, pos)
      if (required > limit) growBuf(required)
      0
    } else if (disableBufGrowing) throw new ArrayIndexOutOfBoundsException("`buf` length exceeded")
    else {
      growBuf(pos + required)
      pos
    }

  private[this] def growBuf(required: Int): Unit =
    setBuf(java.util.Arrays.copyOf(buf, Integer.highestOneBit(limit | required) << 1))

  private[this] def reallocateBufToPreferredSize(): Unit = setBuf(new Array[Byte](config.preferredBufSize))

  private[this] def setBuf(buf: Array[Byte]): Unit = {
    this.buf = buf
    limit = buf.length
  }
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
    var i, j = 0
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
  private final val hexDigits: Array[Short] = {
    val ds = new Array[Short](256)
    var i, j = 0
    do {
      val d1 =
        if (j <= 9) j + '0'
        else j + 'a' - 10
      var k = 0
      do {
        val d2 =
          if (k <= 9) k + '0'
          else k + 'a' - 10
        ds(i) = ((d1 << 8) + d2).toShort
        i += 1
        k += 1
      } while (k < 16)
      j += 1
    } while (j < 16)
    ds
  }
  private final val f32Pow5InvSplit = new Array[Int](62)
  private final val f32Pow5Split = new Array[Int](94)
  private final val f64Pow5InvSplit = new Array[Int](1164)
  private final val f64Pow5Split = new Array[Int](1304)
  private final val tenPow18Squares: Stream[BigInteger] =
    BigInteger.valueOf(1000000000000000000L) #:: tenPow18Squares.map(p => p.multiply(p))

  {
    var pow5 = BigInteger.ONE
    var i = 0
    while (i < 326) {
      val pow5len = pow5.bitLength
      if (i < 31) {
        val s = BigInteger.ONE.shiftLeft(pow5len + 58).divide(pow5).longValue + 1
        f32Pow5InvSplit(i * 2) = (s & 0x7FFFFFFF).toInt
        f32Pow5InvSplit(i * 2 + 1) = (s >> 31).toInt
      }
      if (i < 47) {
        val s = pow5.shiftRight(pow5len - 61).longValue
        f32Pow5Split(i * 2) = (s & 0x7FFFFFFF).toInt
        f32Pow5Split(i * 2 + 1) = (s >> 31).toInt
      }
      if (i < 291) {
        val inv = BigInteger.ONE.shiftLeft(pow5len + 121).divide(pow5).add(BigInteger.ONE)
        var j = 0
        while (j < 4) {
          f64Pow5InvSplit(i * 4 + j) = inv.shiftRight((3 - j) * 31).intValue & 0x7FFFFFFF
          j += 1
        }
      }
      var j = 0
      while (j < 4) {
        f64Pow5Split(i * 4 + j) = pow5.shiftRight(pow5len - 121 + (3 - j) * 31).intValue & 0x7FFFFFFF
        j += 1
      }
      pow5 = pow5.shiftLeft(2).add(pow5)
      i += 1
    }
  }

  final def isNonEscapedAscii(ch: Char): Boolean = ch < 0x80 && escapedChars(ch) == 0
}