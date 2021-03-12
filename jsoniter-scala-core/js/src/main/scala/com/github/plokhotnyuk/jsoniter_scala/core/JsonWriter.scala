package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.{BufferOverflowException, ByteBuffer}
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.expression_evaluator.eval
import com.github.plokhotnyuk.jsoniter_scala.core.JsonWriter._

import scala.annotation.tailrec
import scala.{specialized => sp}

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
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBigInteger(x.bigInteger, null)
    writeParenthesesWithColon()
  }

  def writeKey(x: BigDecimal): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeParenthesesWithColon()
  }

  def writeKey(x: UUID): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
    writeColon()
  }

  def writeKey(x: String): Unit = count = {
    if (x eq null) throw new NullPointerException
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 3)
    var buf = this.buf
    if (comma) {
      comma = false
      buf(pos) = ','
      pos += 1
      if (indention != 0) {
        buf(pos) = '\n'
        pos = writeNBytes(indention, ' ', pos + 1, buf)
      }
    }
    buf(pos) = '"'
    pos = writeString(x, 0, x.length, pos + 1, limit - 1, escapedChars)
    if (pos + 3 >= limit) pos = flushAndGrowBuf(3, pos)
    buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = ':'
    pos += 2
    if (config.indentionStep > 0) {
      buf(pos) = ' '
      pos += 1
    }
    pos
  }

  def writeNonEscapedAsciiKey(x: String): Unit = count = {
    if (x eq null) throw new NullPointerException
    val len = x.length
    val indention = this.indention
    var pos = ensureBufCapacity(indention + len + 6)
    val buf = this.buf
    if (comma) {
      comma = false
      buf(pos) = ','
      pos += 1
      if (indention != 0) {
        buf(pos) = '\n'
        pos = writeNBytes(indention, ' ', pos + 1, buf)
      }
    }
    buf(pos) = '"'
    pos += 1
    var i = 0
    while (i < len) {
      buf(pos) = x.charAt(i).toByte
      pos += 1
      i += 1
    }
    buf(pos) = '"'
    buf(pos + 1) = ':'
    pos += 2
    if (config.indentionStep > 0) {
      buf(pos) = ' '
      pos += 1
    }
    pos
  }

  def writeKey(x: Duration): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeDuration(x)
    writeColon()
  }

  def writeKey(x: Instant): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeInstant(x)
    writeColon()
  }

  def writeKey(x: LocalDate): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDate(x)
    writeColon()
  }

  def writeKey(x: LocalDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDateTime(x)
    writeColon()
  }

  def writeKey(x: LocalTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalTime(x)
    writeColon()
  }

  def writeKey(x: MonthDay): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeMonthDay(x)
    writeColon()
  }

  def writeKey(x: OffsetDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetDateTime(x)
    writeColon()
  }

  def writeKey(x: OffsetTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetTime(x)
    writeColon()
  }

  def writeKey(x: Period): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writePeriod(x)
    writeColon()
  }

  def writeKey(x: Year): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeYear(x)
    writeColon()
  }

  def writeKey(x: YearMonth): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeYearMonth(x)
    writeColon()
  }

  def writeKey(x: ZonedDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZonedDateTime(x)
    writeColon()
  }

  def writeKey(x: ZoneId): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZoneId(x)
    writeColon()
  }

  def writeKey(x: ZoneOffset): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZoneOffset(x)
    writeColon()
  }

  def encodeError(msg: String): Nothing =
    throw new JsonWriterException(msg, null, config.throwWriterExceptionWithStackTrace)

  def writeVal(x: BigDecimal): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigDecimal(x.bigDecimal)
  }

  def writeVal(x: BigInt): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigInteger(x.bigInteger, null)
  }

  def writeVal(x: UUID): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
  }

  def writeVal(x: String): Unit = count = {
    if (x eq null) throw new NullPointerException
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 4)
    if (comma) {
      buf(pos) = ','
      pos += 1
      if (indention != 0) {
        buf(pos) = '\n'
        pos = writeNBytes(indention, ' ', pos + 1, buf)
      }
    } else comma = true
    buf(pos) = '"'
    pos = writeString(x, 0, x.length, pos + 1, limit - 1, escapedChars)
    buf(pos) = '"'
    pos + 1
  }

  def writeNonEscapedAsciiVal(x: String): Unit =  count = {
    if (x eq null) throw new NullPointerException
    val len = x.length
    val indention = this.indention
    var pos = ensureBufCapacity(indention + len + 4)
    if (comma) {
      buf(pos) = ','
      pos += 1
      if (indention != 0) {
        buf(pos) = '\n'
        pos = writeNBytes(indention, ' ', pos + 1, buf)
      }
    } else comma = true
    buf(pos) = '"'
    pos += 1
    var i = 0
    while (i < len) {
      buf(pos) = x.charAt(i).toByte
      pos += 1
      i += 1
    }
    buf(pos) = '"'
    pos + 1
  }

  def writeVal(x: Duration): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeDuration(x)
  }

  def writeVal(x: Instant): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeInstant(x)
  }

  def writeVal(x: LocalDate): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDate(x)
  }

  def writeVal(x: LocalDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDateTime(x)
  }

  def writeVal(x: LocalTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalTime(x)
  }

  def writeVal(x: MonthDay): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeMonthDay(x)
  }

  def writeVal(x: OffsetDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetDateTime(x)
  }

  def writeVal(x: OffsetTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetTime(x)
  }

  def writeVal(x: Period): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writePeriod(x)
  }

  def writeVal(x: Year): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeYear(x)
  }

  def writeVal(x: YearMonth): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeYearMonth(x)
  }

  def writeVal(x: ZonedDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeZonedDateTime(x)
  }

  def writeVal(x: ZoneId): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeZoneId(x)
  }

  def writeVal(x: ZoneOffset): Unit = {
    if (x eq null) throw new NullPointerException
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
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeBytes('"')
  }

  def writeValAsString(x: BigInt): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBigInteger(x.bigInteger, null)
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

  def writeBase16Val(bs: Array[Byte], lowerCase: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    val ds =
      if (lowerCase) lowerCaseHexDigits
      else upperCaseHexDigits
    writeBase16Bytes(bs, ds)
  }

  def writeBase64Val(bs: Array[Byte], doPadding: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64Digits, doPadding)
  }

  def writeBase64UrlVal(bs: Array[Byte], doPadding: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64UrlDigits, doPadding)
  }

  def writeRawVal(bs: Array[Byte]): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeRawBytes(bs)
  }

  def writeNull(): Unit = count = {
    writeOptionalCommaAndIndentionBeforeValue()
    val pos = ensureBufCapacity(4)
    val buf = this.buf
    buf(pos) = 'n'
    buf(pos + 1) = 'u'
    buf(pos + 2) = 'l'
    buf(pos + 3) = 'l'
    pos + 4
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
      this.out = null // don't close output stream
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
    var pos = ensureBufCapacity(3)
    val buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = ':'
    pos += 2
    if (config.indentionStep > 0) {
      buf(pos) = ' '
      pos += 1
    }
    pos
  }

  private[this] def writeColon(): Unit = count = {
    var pos = ensureBufCapacity(2)
    val buf = this.buf
    buf(pos) = ':'
    pos += 1
    if (config.indentionStep > 0) {
      buf(pos) = ' '
      pos += 1
    }
    pos
  }

  private[this] def writeBytes(b: Byte): Unit = count = {
    val pos = ensureBufCapacity(1)
    buf(pos) = b
    pos + 1
  }

  private[this] def writeBase16Bytes(bs: Array[Byte], ds: Array[Short]): Unit = count = {
    val lenM1 = bs.length - 1
    var posLim = limit - 6
    var pos = count
    if (pos >= posLim) {
      pos = flushAndGrowBuf(6, pos)
      posLim = limit - 5
    }
    var buf = this.buf
    buf(pos) = '"'
    pos += 1
    var offset = 0
    while (offset < lenM1) {
      val offsetLim = Math.min(((posLim - pos + 1) >> 1) + offset, lenM1)
      while (offset < offsetLim) {
        val d1 = ds(bs(offset) & 0xFF)
        val d2 = ds(bs(offset + 1) & 0xFF)
        buf(pos) = d1.toByte
        buf(pos + 1) = (d1 >> 8).toByte
        buf(pos + 2) = d2.toByte
        buf(pos + 3) = (d2 >> 8).toByte
        pos += 4
        offset += 2
      }
      if (pos >= posLim) {
        pos = flushAndGrowBuf(5, pos)
        buf = this.buf
        posLim = limit - 5
      }
    }
    if (offset == lenM1) {
      val d1 = ds(bs(offset) & 0xFF)
      buf(pos) = d1.toByte
      buf(pos + 1) = (d1 >> 8).toByte
      pos +=2
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeBase64Bytes(bs: Array[Byte], ds: Array[Byte], doPadding: Boolean): Unit = count = {
    val lenM2 = bs.length - 2
    var posLim = limit - 6
    var pos = count
    if (pos >= posLim) {
      pos = flushAndGrowBuf(6, pos)
      posLim = limit - 5
    }
    var buf = this.buf
    buf(pos) = '"'
    pos += 1
    var offset = 0
    while (offset < lenM2) {
      val offsetLim = Math.min(((posLim - pos + 3) >> 2) * 3 + offset, lenM2)
      while (offset < offsetLim) {
        val p = (bs(offset) & 0xFF) << 16 | (bs(offset + 1) & 0xFF) << 8 | (bs(offset + 2) & 0xFF)
        buf(pos) = ds(p >> 18)
        buf(pos + 1) = ds((p >> 12) & 0x3F)
        buf(pos + 2) = ds((p >> 6) & 0x3F)
        buf(pos + 3) = ds(p & 0x3F)
        pos += 4
        offset += 3
      }
      if (pos >= posLim) {
        pos = flushAndGrowBuf(5, pos)
        buf = this.buf
        posLim = limit - 5
      }
    }
    if (offset == lenM2) {
      val p = (bs(offset) & 0xFF) << 10 | (bs(offset + 1) & 0xFF) << 2
      buf(pos) = ds(p >> 12)
      buf(pos + 1) = ds((p >> 6) & 0x3F)
      buf(pos + 2) = ds(p & 0x3F)
      pos += 3
      if (doPadding) {
        buf(pos) = '='
        pos += 1
      }
    } else if (offset == lenM2 + 1) {
      val p = bs(offset) & 0xFF
      buf(pos) = ds(p >> 2)
      buf(pos + 1) = ds((p << 4) & 0x3F)
      pos += 2
      if (doPadding) {
        buf(pos) = '='
        buf(pos + 1) = '='
        pos += 2
      }
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeRawBytes(bs: Array[Byte]): Unit = count = {
    var pos = count
    var step = Math.max(config.preferredBufSize, limit - pos)
    var remaining = bs.length
    var offset = 0
    while (remaining > 0) {
      step = Math.min(step, remaining)
      if (pos + step > limit) pos = flushAndGrowBuf(step, pos)
      System.arraycopy(bs, offset, buf, pos, step)
      offset += step
      pos += step
      remaining -= step
    }
    pos
  }

  private[this] def writeZoneId(x: ZoneId): Unit = count = {
    val s = x.getId
    val len = s.length
    var pos = ensureBufCapacity(len + 2)
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    var i = 0
    while (i < len) {
      buf(pos) = s.charAt(i).toByte
      pos += 1
      i += 1
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeUUID(mostSigBits: Long, leastSigBits: Long): Unit = count = {
    val pos = ensureBufCapacity(38) // 38 == (new java.util.UUID(0, 0)).toString.length + 2
    val buf = this.buf
    val ds = lowerCaseHexDigits
    val mostSigBits1 = (mostSigBits >>> 32).toInt
    buf(pos) = '"'
    val d1 = ds(mostSigBits1 >>> 24)
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d1 >> 8).toByte
    val d2 = ds((mostSigBits1 >>> 16) & 0xFF)
    buf(pos + 3) = d2.toByte
    buf(pos + 4) = (d2 >> 8).toByte
    val d3 = ds((mostSigBits1 >>> 8) & 0xFF)
    buf(pos + 5) = d3.toByte
    buf(pos + 6) = (d3 >> 8).toByte
    val d4 = ds(mostSigBits1 & 0xFF)
    buf(pos + 7) = d4.toByte
    buf(pos + 8) = (d4 >> 8).toByte
    val mostSigBits2 = mostSigBits.toInt
    buf(pos + 9) = '-'
    val d5 = ds(mostSigBits2 >>> 24)
    buf(pos + 10) = d5.toByte
    buf(pos + 11) = (d5 >> 8).toByte
    val d6 = ds((mostSigBits2 >>> 16) & 0xFF)
    buf(pos + 12) = d6.toByte
    buf(pos + 13) = (d6 >> 8).toByte
    buf(pos + 14) = '-'
    val d7 = ds((mostSigBits2 >>> 8) & 0xFF)
    buf(pos + 15) = d7.toByte
    buf(pos + 16) = (d7 >> 8).toByte
    val d8 = ds(mostSigBits2 & 0xFF)
    buf(pos + 17) = d8.toByte
    buf(pos + 18) = (d8 >> 8).toByte
    val leastSigBits1 = (leastSigBits >>> 32).toInt
    buf(pos + 19) = '-'
    val d9 = ds(leastSigBits1 >>> 24)
    buf(pos + 20) = d9.toByte
    buf(pos + 21) = (d9 >> 8).toByte
    val d10 = ds((leastSigBits1 >>> 16) & 0xFF)
    buf(pos + 22) = d10.toByte
    buf(pos + 23) = (d10 >> 8).toByte
    buf(pos + 24) = '-'
    val d11 = ds((leastSigBits1 >>> 8) & 0xFF)
    buf(pos + 25) = d11.toByte
    buf(pos + 26) = (d11 >> 8).toByte
    val d12 = ds(leastSigBits1 & 0xFF)
    buf(pos + 27) = d12.toByte
    buf(pos + 28) = (d12 >> 8).toByte
    val leastSigBits2 = leastSigBits.toInt
    val d13 = ds(leastSigBits2 >>> 24)
    buf(pos + 29) = d13.toByte
    buf(pos + 30) = (d13 >> 8).toByte
    val d14 = ds((leastSigBits2 >>> 16) & 0xFF)
    buf(pos + 31) = d14.toByte
    buf(pos + 32) = (d14 >> 8).toByte
    val d15 = ds((leastSigBits2 >>> 8) & 0xFF)
    buf(pos + 33) = d15.toByte
    buf(pos + 34) = (d15 >> 8).toByte
    val d16 = ds(leastSigBits2 & 0xFF)
    buf(pos + 35) = d16.toByte
    buf(pos + 36) = (d16 >> 8).toByte
    buf(pos + 37) = '"'
    pos + 38
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
    if (ch < 0x80) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
      val esc = escapedChars(ch)
      if (esc == 0) {
        buf(pos) = ch.toByte
        pos += 1
      } else if (esc > 0) {
        buf(pos) = '\\'
        buf(pos + 1) = esc
        pos += 2
      } else pos = writeEscapedUnicode(ch.toByte, pos, buf)
    } else if (config.escapeUnicode) {
      if (ch >= 0xD800 && ch <= 0xDFFF) illegalSurrogateError()
      pos = writeEscapedUnicode(ch, pos, buf)
    } else if (ch < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
      buf(pos) = (0xC0 | (ch >> 6)).toByte
      buf(pos + 1) = (0x80 | (ch & 0x3F)).toByte
      pos += 2
    } else if (ch < 0xD800 || ch > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
      buf(pos) = (0xE0 | (ch >> 12)).toByte
      buf(pos + 1) = (0x80 | ((ch >> 6) & 0x3F)).toByte
      buf(pos + 2) = (0x80 | (ch & 0x3F)).toByte
      pos += 3
    } else illegalSurrogateError()
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeEscapedUnicode(ch: Char, pos: Int, buf: Array[Byte]): Int = {
    val ds = lowerCaseHexDigits
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    val d1 = ds(ch >>> 8)
    buf(pos + 2) = d1.toByte
    buf(pos + 3) = (d1 >> 8).toByte
    val d2 = ds(ch & 0xFF)
    buf(pos + 4) = d2.toByte
    buf(pos + 5) = (d2 >> 8).toByte
    pos + 6
  }

  private[this] def writeEscapedUnicode(b: Byte, pos: Int, buf: Array[Byte]): Int = {
    val d = lowerCaseHexDigits(b & 0xFF)
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = '0'
    buf(pos + 3) = '0'
    buf(pos + 4) = d.toByte
    buf(pos + 5) = (d >> 8).toByte
    pos + 6
  }

  private[this] def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private[this] def writeBigInteger(x: BigInteger, ss: Array[BigInteger]): Unit =
    if (x.bitLength < 64) writeLong(x.longValue)
    else {
      val n = calculateTenPow18SquareNumber(x)
      val ss1 =
        if (ss eq null) getTenPow18Squares(n)
        else ss
      val qr = x.divideAndRemainder(ss1(n))
      writeBigInteger(qr(0), ss1)
      writeBigIntegerRemainder(qr(1), n - 1, ss1)
    }

  private[this] def writeBigIntegerRemainder(x: BigInteger, n: Int, ss: Array[BigInteger]): Unit =
    if (n < 0) count = write18Digits(Math.abs(x.longValue), ensureBufCapacity(18), buf, digits)
    else {
      val qr = x.divideAndRemainder(ss(n))
      writeBigIntegerRemainder(qr(0), n - 1, ss)
      writeBigIntegerRemainder(qr(1), n - 1, ss)
    }

  private[this] def writeBigDecimal(x: java.math.BigDecimal): Unit = {
    val exp = writeBigDecimal(x.unscaledValue, x.scale, 0, null)
    if (exp != 0) {
      var pos = ensureBufCapacity(12)
      val buf = this.buf
      val ds = digits
      buf(pos) = 'E'
      pos += 1
      val q0 =
        if (exp >= 0) {
          buf(pos) = '+'
          pos += 1
          exp
        } else {
          buf(pos) = '-'
          pos += 1
          -exp
        }
      count =
        if (q0.toInt == q0) writePositiveInt(q0.toInt, pos, buf, ds)
        else {
          val q1 = (q0 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
          write8Digits((q0 - q1 * 100000000).toInt, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
        }
    }
  }

  private[this] def writeBigDecimal(x: BigInteger, scale: Int, blockScale: Int, ss: Array[BigInteger]): Long =
    if (x.bitLength < 64) {
      val v = x.longValue
      val pos = ensureBufCapacity(28) // Long.MinValue.toString.length + 8 (for a leading zero, dot, and padding zeroes)
      count = pos
      writeLong(v)
      val blockLen = (v >> 63).toInt + count - pos
      val dotOff = scale.toLong - blockScale
      val exp = (blockLen - 1) - dotOff
      if (scale >= 0 && exp >= -6) {
        if (exp < 0) insertDotWithZeroes(blockLen, -1 - exp.toInt)
        else if (dotOff > 0) insertDot(count - dotOff.toInt)
        0
      } else {
        if (blockLen > 1 || blockScale > 0) insertDot(count - blockLen + 1)
        exp
      }
    } else {
      val n = calculateTenPow18SquareNumber(x)
      val ss1 =
        if (ss eq null) getTenPow18Squares(n)
        else ss
      val qr = x.divideAndRemainder(ss1(n))
      val exp = writeBigDecimal(qr(0), scale, blockScale + (18 << n), ss1)
      writeBigDecimalRemainder(qr(1), scale, blockScale, n - 1, ss1)
      exp
    }

  private[this] def writeBigDecimalRemainder(x: BigInteger, scale: Int, blockScale: Int, n: Int,
                                            ss: Array[BigInteger]): Unit =
    if (n < 0) {
      count = write18Digits(Math.abs(x.longValue), ensureBufCapacity(19), buf, digits) // 18 digits and a place for optional dot
      val dotOff = scale - blockScale
      if (dotOff > 0 && dotOff <= 18) insertDot(count - dotOff)
    } else {
      val qr = x.divideAndRemainder(ss(n))
      writeBigDecimalRemainder(qr(0), scale, blockScale + (18 << n), n - 1, ss)
      writeBigDecimalRemainder(qr(1), scale, blockScale, n - 1, ss)
    }

  private[this] def calculateTenPow18SquareNumber(x: BigInteger): Int = {
    val m = Math.max((x.bitLength * 71828554L >>> 32).toInt - 1, 1) // Math.max((x.bitLength * Math.log(1e18) / Math.log(2)).toInt - 1, 1)
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
    var pos = count
    val buf = this.buf
    while (pos > dotPos) {
      buf(pos) = buf(pos - 1)
      pos -= 1
    }
    buf(dotPos) = '.'
    count + 1
  }

  private[this] def writeBoolean(x: Boolean): Unit = count = {
    val pos = ensureBufCapacity(5) // false.toString.length
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
      buf(pos) = d.toByte
      buf(pos + 1) = (d >> 8).toByte
      pos + 2
    } else {
      val d = digits(q0 - 100)
      buf(pos) = '1'
      buf(pos + 1) = d.toByte
      buf(pos + 2) = (d >> 8).toByte
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
    val totalSecs = x.getSeconds
    var nano = x.getNano
    if ((totalSecs | nano) == 0) {
      buf(pos) = '0'
      buf(pos + 1) = 'S'
      pos += 2
    } else {
      var effectiveTotalSecs = totalSecs
      if (totalSecs < 0 && nano > 0) effectiveTotalSecs += 1
      var hours = effectiveTotalSecs / 3600 // 3600 == seconds in a hour
      val secsOfHour = (effectiveTotalSecs - hours * 3600).toInt
      var minutes = secsOfHour / 60
      var seconds = secsOfHour - minutes * 60
      val ds = digits
      if (hours != 0) {
        if (hours < 0) {
          buf(pos) = '-'
          pos += 1
          hours = -hours
        }
        pos =
          if (hours.toInt == hours) writePositiveInt(hours.toInt, pos, buf, ds)
          else {
            val q1 = hours / 100000000
            val r1 = (hours - q1 * 100000000).toInt
            write8Digits(r1, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
          }
        buf(pos) = 'H'
        pos += 1
      }
      if (minutes != 0) {
        if (minutes < 0) {
          buf(pos) = '-'
          pos += 1
          minutes = -minutes
        }
        if (minutes < 10) {
          buf(pos) = (minutes + '0').toByte
          pos += 1
        } else pos = write2Digits(minutes, pos, buf, ds)
        buf(pos) = 'M'
        pos += 1
      }
      if ((seconds | nano) != 0) {
        if (totalSecs < 0 && seconds == 0) {
          buf(pos) = '-'
          buf(pos + 1) = '0'
          pos += 2
        } else {
          if (seconds < 0) {
            buf(pos) = '-'
            pos += 1
            seconds = -seconds
          }
          if (seconds < 10) {
            buf(pos) = (seconds + '0').toByte
            pos += 1
          } else pos = write2Digits(seconds, pos, buf, ds)
        }
        if (nano != 0) {
          if (totalSecs < 0) nano = 1000000000 - nano
          val dotPos = pos
          pos = writeSignificantFractionDigits(nano, pos + 9, pos, buf, ds)
          buf(dotPos) = '.'
        }
        buf(pos) = 'S'
        pos += 1
      }
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeInstant(x: Instant): Unit = count = {
    val epochSecond = x.getEpochSecond
    val epochDay =
      (if (epochSecond >= 0) epochSecond
      else epochSecond - 86399) / 86400 // 86400 == seconds per day
    val secsOfDay = (epochSecond - epochDay * 86400).toInt
    var marchZeroDay = epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    var adjustYear = 0
    if (marchZeroDay < 0) { // adjust negative years to positive for calculation
      val adjust400YearCycles = to400YearCycle(marchZeroDay + 1) - 1
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L
    }
    var year = to400YearCycle(marchZeroDay * 400 + 591)
    var marchDayOfYear = toMarchDayOfYear(marchZeroDay, year)
    if (marchDayOfYear < 0) { // fix year estimate
      year -= 1
      marchDayOfYear = toMarchDayOfYear(marchZeroDay, year)
    }
    val marchMonth = (marchDayOfYear * 17135 + 6854) >> 19 // (marchDayOfYear * 5 + 2) / 153
    year += (marchMonth * 3277 >> 15) + adjustYear // year += marchMonth / 10 + adjustYear (reset any negative year and convert march-based values back to january-based)
    val month = marchMonth +
      (if (marchMonth < 10) 3
      else -9)
    val day = marchDayOfYear - ((marchMonth * 1002762 - 16383) >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
    val hour = secsOfDay * 37283 >>> 27 // divide a small positive int by 3600
    val secsOfHour = secsOfDay - hour * 3600
    val minute = secsOfHour * 17477 >> 20 // divide a small positive int by 60
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

  private[this] def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century = year / 100
    (marchZeroDay - year * 365L).toInt - (year >> 2) + century - (century >> 2)
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
      pos += 2
    } else {
      val years = x.getYears
      val months = x.getMonths
      val days = x.getDays
      val ds = digits
      if (years != 0) pos = writePeriod(years, pos, buf, ds, 'Y')
      if (months != 0) pos = writePeriod(months, pos, buf, ds, 'M')
      if (days != 0) pos = writePeriod(days, pos, buf, ds, 'D')
    }
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writePeriod(x: Int, p: Int, buf: Array[Byte], ds: Array[Short], b: Byte): Int = {
    var pos = p
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
    pos = writePositiveInt(q0, pos, buf, ds)
    buf(pos) = b
    pos + 1
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
      var i = 0
      while (i < len) {
        buf(pos) = zoneId.charAt(i).toByte
        pos += 1
        i += 1
      }
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
    if ((second | nano) != 0) {
      buf(pos) = ':'
      pos = write2Digits(second, pos + 1, buf, ds)
      if (nano != 0) {
        buf(pos) = '.'
        val q1 = nano / 10000000
        val r1 = nano - q1 * 10000000
          pos = write2Digits(q1, pos + 1, buf, ds)
        val q2 = r1 / 100000
        val r2 = r1 - q2 * 100000
        val d = ds(q2)
        buf(pos) = d.toByte
        pos += 1
        val b = (d >> 8).toByte
        if ((r2 | b - '0') != 0) {
          buf(pos) = b
          val q3 = r2 / 1000
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
      val q1 = nano / 10000000
      val r1 = nano - q1 * 10000000
        pos = write2Digits(q1, pos + 1, buf, ds)
      val q2 = r1 / 100000
      val r2 = r1 - q2 * 100000
      val d = ds(q2)
      buf(pos) = d.toByte
      pos += 1
      val b = (d >> 8).toByte
      if ((r2 | b - '0') != 0) {
        buf(pos) = b
        val q3 = r2 / 1000
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
      val q1 = q0 * 37283 >>> 27 // divide a small positive int by 3600
      val r1 = q0 - q1 * 3600
      var pos = write2Digits(q1, p + 1, buf, ds)
      buf(pos) = ':'
      val q2 = r1 * 17477 >> 20 // divide a small positive int by 60
      val r2 = r1 - q2 * 60
      pos = write2Digits(q2, pos + 1, buf, ds)
      if (r2 == 0) pos
      else {
        buf(pos) = ':'
        write2Digits(r2, pos + 1, buf, ds)
      }
    }
  }

  private[this] def write2Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val d = ds(q0)
    buf(pos) = d.toByte
    buf(pos + 1) = (d >> 8).toByte
    pos + 2
  }

  private[this] def write3Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 * 1311 >> 17 // divide a small positive int by 100
    val d = ds(q0 - q1 * 100)
    buf(pos) = (q1 + '0').toByte
    buf(pos + 1) = d.toByte
    buf(pos + 2) = (d >> 8).toByte
    pos + 3
  }

  private[this] def write4Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 * 5243 >> 19 // divide a small positive int by 100
    val d1 = ds(q1)
    buf(pos) = d1.toByte
    buf(pos + 1) = (d1 >> 8).toByte
    val d2 = ds(q0 - q1 * 100)
    buf(pos + 2) = d2.toByte
    buf(pos + 3) = (d2 >> 8).toByte
    pos + 4
  }

  private[this] def write8Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 / 10000
    val q2 = q1 * 5243 >> 19 // divide a small positive int by 100
    val d1 = ds(q2)
    buf(pos) = d1.toByte
    buf(pos + 1) = (d1 >> 8).toByte
    val d2 = ds(q1 - q2 * 100)
    buf(pos + 2) = d2.toByte
    buf(pos + 3) = (d2 >> 8).toByte
    val r1 = q0 - q1 * 10000
    val q3 = r1 * 5243 >> 19 // divide a small positive int by 100
    val d3 = ds(q3)
    buf(pos + 4) = d3.toByte
    buf(pos + 5) = (d3 >> 8).toByte
    val d4 = ds(r1 - q3 * 100)
    buf(pos + 6) = d4.toByte
    buf(pos + 7) = (d4 >> 8).toByte
    pos + 8
  }

  private[this] def write18Digits(q0: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 / 100000000
    val r1 = (q0 - q1 * 100000000).toInt
    val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
    val r2 = (q1 - q2 * 100000000).toInt
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
      val q1 = q0 * 53688 >> 29 // divide a small positive int by 10000
      buf(pos) = (q1 + '0').toByte
      write4Digits(q0 - 10000 * q1, pos + 1, buf, digits)
    }
  }

  private[this] def writeInt(x: Int): Unit = count = {
    var pos = ensureBufCapacity(11) // Int.MinValue.toString.length
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
    var pos = ensureBufCapacity(20) // Long.MinValue.toString.length
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
      val r1 = (q0 - q1 * 100000000).toInt
      if (q1.toInt == q1) write8Digits(r1, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
      else {
        val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
        val r2 = (q1 - q2 * 100000000).toInt
        write8Digits(r1, write8Digits(r2, writePositiveInt(q2.toInt, pos, buf, ds), buf, ds), buf, ds)
      }
    }
  }

  private[this] def writePositiveInt(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val lastPos = offset(q0) + pos
    writePositiveIntDigits(q0, lastPos, buf, ds)
    lastPos + 1
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java
  private[this] def writeFloat(x: Float): Unit = count = {
    val bits = java.lang.Float.floatToIntBits(x)
    var pos = ensureBufCapacity(15)
    val buf = this.buf
    val ds = digits
    if (bits < 0) {
      buf(pos) = '-'
      pos += 1
    }
    if (x == 0.0f) {
      buf(pos) = '0'
      buf(pos + 1) = '.'
      buf(pos + 2) = '0'
      pos + 3
    } else {
      val ieeeExponent = (bits >> 23) & 0xFF
      val ieeeMantissa = bits & 0x7FFFFF
      var e = ieeeExponent - 150
      var m = ieeeMantissa | 0x800000
      var dv, exp = 0
      if (e == 0) dv = m
      else if (e >= -23 && e < 0 && m << e == 0) dv = m >> -e
      else {
        var expShift, expCorr = 0
        var cblShift = 2
        if (ieeeExponent == 0) {
          e = -149
          m = ieeeMantissa
          if (ieeeMantissa < 8) {
            m *= 10
            expShift = 1
          }
        } else if (ieeeExponent == 255) illegalNumberError(x)
        if (ieeeMantissa == 0 && ieeeExponent > 1) {
          expCorr = 131007
          cblShift = 1
        }
        exp = e * 315653 - expCorr >> 20
        val g1 = gs(exp + 324 << 1) + 1
        val h = (-exp * 108853 >> 15) + e + 1
        val cb = m << 2
        val outm1 = (m & 0x1) - 1
        val vb = rop(g1, cb << h)
        val vbls = rop(g1, cb - cblShift << h) + outm1
        val vbrd = outm1 - rop(g1, cb + 2 << h)
        val s = vb >> 2
        if (s < 100 || {
          dv = s / 10
          val sp40 = dv * 40
          val upin = vbls - sp40
          ((sp40 + vbrd + 40) ^ upin) >= 0 || {
            dv += ~upin >>> 31
            exp += 1
            false
          }
        }) {
          val s4 = s << 2
          val uin = vbls - s4
          dv = (~{
            if (((s4 + vbrd + 4) ^ uin) < 0) uin
            else (vb & 0x3) + (s & 0x1) - 3
          } >>> 31) + s
          exp -= expShift
        }
      }
      var len = offset(dv)
      exp += len
      len += 1
      if (exp < -3 || exp >= 7) {
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos =
          if (lastPos < pos + 3) {
            buf(lastPos) = '0'
            lastPos + 1
          } else lastPos
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
      } else if (exp < 0) {
        val dotPos = pos + 1
        buf(pos) = '0'
        buf(pos + 2) = '0'
        buf(pos + 3) = '0'
        pos -= exp
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
        lastPos
      } else if (exp < len - 1) {
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        val beforeDotPos = pos + exp
        while (pos <= beforeDotPos) {
          buf(pos) = buf(pos + 1)
          pos += 1
        }
        buf(pos) = '.'
        lastPos
      } else {
        pos += len
        writePositiveIntDigits(dv, pos - 1, buf, ds)
        buf(pos) = '.'
        buf(pos + 1) = '0'
        pos + 2
      }
    }
  }

  private[this] def rop(g: Long, cp: Int): Int = {
    val x1 = ((g & 0xFFFFFFFFL) * cp >>> 32) + (g >>> 32) * cp
    (x1 >>> 31).toInt | -x1.toInt >>> 31
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/DoubleToDecimal.java
  private[this] def writeDouble(x: Double): Unit = count = {
    val bits = java.lang.Double.doubleToLongBits(x)
    var pos = ensureBufCapacity(24)
    val buf = this.buf
    val ds = digits
    if (bits < 0) {
      buf(pos) = '-'
      pos += 1
    }
    if (x == 0.0) {
      buf(pos) = '0'
      buf(pos + 1) = '.'
      buf(pos + 2) = '0'
      pos + 3
    } else {
      val ieeeExponent = (bits >> 52).toInt & 0x7FF
      val ieeeMantissa = bits & 0xFFFFFFFFFFFFFL
      var e = ieeeExponent - 1075
      var m = ieeeMantissa | 0x10000000000000L
      var dv = 0L
      var exp = 0
      if (e == 0) dv = m
      else if (e >= -52 && e < 0 && m << e == 0) dv = m >> -e
      else {
        var expShift, expCorr = 0
        var cblShift = 2
        if (ieeeExponent == 0) {
          e = -1074
          m = ieeeMantissa
          if (ieeeMantissa < 3) {
            m *= 10
            expShift = 1
          }
        } else if (ieeeExponent == 2047) illegalNumberError(x)
        if (ieeeMantissa == 0 && ieeeExponent > 1) {
          expCorr = 131007
          cblShift = 1
        }
        exp = e * 315653 - expCorr >> 20
        val i = exp + 324 << 1
        val g1 = gs(i)
        val g0 = gs(i + 1)
        val h = (-exp * 108853 >> 15) + e + 2
        val cb = m << 2
        val outm1 = (m.toInt & 0x1) - 1
        val vb = rop(g1, g0, cb << h)
        val vbls = rop(g1, g0, cb - cblShift << h) + outm1
        val vbrd = outm1 - rop(g1, g0, cb + 2 << h)
        val s = vb >> 2
        if (s < 100 || {
          dv = s / 10
          val sp40 = dv * 40
          val upin = (vbls - sp40).toInt
          (((sp40 + vbrd).toInt + 40) ^ upin) >= 0 || {
            dv += ~upin >>> 31
            exp += 1
            false
          }
        }) {
          val s4 = s << 2
          val uin = (vbls - s4).toInt
          dv = (~{
            if ((((s4 + vbrd).toInt + 4) ^ uin) < 0) uin
            else (vb.toInt & 0x3) + (s.toInt & 0x1) - 3
          } >>> 31) + s
          exp -= expShift
        }
      }
      var len = offset(dv)
      exp += len
      len += 1
      if (exp < -3 || exp >= 7) {
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        pos =
          if (lastPos < pos + 3) {
            buf(lastPos) = '0'
            lastPos + 1
          } else lastPos
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
      } else if (exp < 0) {
        val dotPos = pos + 1
        buf(pos) = '0'
        buf(pos + 2) = '0'
        buf(pos + 3) = '0'
        pos -= exp
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
        lastPos
      } else if (exp < len - 1) {
        val lastPos = writeSignificantFractionDigits(dv, pos + len, pos, buf, ds)
        val beforeDotPos = pos + exp
        while (pos <= beforeDotPos) {
          buf(pos) = buf(pos + 1)
          pos += 1
        }
        buf(pos) = '.'
        lastPos
      } else {
        pos += len
        writePositiveIntDigits(dv.toInt, pos - 1, buf, ds)
        buf(pos) = '.'
        buf(pos + 1) = '0'
        pos + 2
      }
    }
  }

  private[this] def rop(g1: Long, g0: Long, cp: Long): Long = {
    val x1 = multiplyHigh(g0, cp)
    val z = (g1 * cp >>> 1) + x1
    val y1 = multiplyHigh(g1, cp)
    (z >>> 63) + y1 | -(z & 0x7FFFFFFFFFFFFFFFL) >>> 63
  }

  private[this] def multiplyHigh(x: Long, y: Long): Long = {
    val x2 = x & 0xFFFFFFFFL
    val y2 = y & 0xFFFFFFFFL
    val b = x2 * y2
    val x1 = x >>> 32
    val y1 = y >>> 32
    val a = x1 * y1
    (((b >>> 32) + (x1 + x2) * (y1 + y2) - b - a) >>> 32) + a
  }

  private[this] def offset(q0: Long): Int =
    if (q0.toInt == q0) offset(q0.toInt)
    else if (q0 < 10000000000L) ((999999999 - q0) >>> 63).toInt + 8
    else if (q0 < 1000000000000L) ((99999999999L - q0) >>> 63).toInt + 10
    else if (q0 < 100000000000000L) ((9999999999999L - q0) >>> 63).toInt + 12
    else if (q0 < 10000000000000000L) ((999999999999999L - q0) >>> 63).toInt + 14
    else ((99999999999999999L - q0) >>> 63).toInt + 16

  private[this] def offset(q0: Int): Int =
    if (q0 < 100) (9 - q0) >>> 31
    else if (q0 < 10000) ((999 - q0) >>> 31) + 2
    else if (q0 < 1000000) ((99999 - q0) >>> 31) + 4
    else if (q0 < 100000000) ((9999999 - q0) >>> 31) + 6
    else ((999999999 - q0) >>> 31) + 8

  private[this] def writeSignificantFractionDigits(q0: Long, pos: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (q0.toInt == q0) writeSignificantFractionDigits(q0.toInt, pos, posLim, buf, ds)
    else {
      val q1 = q0 / 100000000
      val r1 = (q0 - q1 * 100000000).toInt
      if (r1 == 0) writeSignificantFractionDigits(q1.toInt, pos - 8, posLim, buf, ds)
      else {
        val lastPos = writeSignificantFractionDigits(r1, pos, pos - 8, buf, ds)
        writeFractionDigits(q1.toInt, pos - 8, posLim, buf, ds)
        lastPos
      }
    }

  private[this] def writeSignificantFractionDigits(q: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = q
    var q1, r1 = 0
    var pos = p
    while ({
      q1 = q0 / 100
      r1 = q0 - q1 * 100
      r1 == 0
    }) {
      q0 = q1
      pos -= 2
    }
    val d = ds(r1)
    buf(pos - 1) = d.toByte
    var lastPos = pos
    if (d > 12345) { // 12345 == ('0' << 8) | '9'
      buf(pos) = (d >> 8).toByte
      lastPos += 1
    }
    writeFractionDigits(q1, pos - 2, posLim, buf, ds)
    lastPos
  }

  private[this] def writeFractionDigits(q: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = q
    var pos = p
    while (pos > posLim) {
      val q1 = q0 / 100
      val d = ds(q0 - q1 * 100)
      buf(pos - 1) = d.toByte
      buf(pos) = (d >> 8).toByte
      q0 = q1
      pos -= 2
    }
  }

  private[this] def writePositiveIntDigits(q: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = q
    var pos = p
    while (q0 >= 100) {
      val q1 = q0 / 100
      val d = ds(q0 - q1 * 100)
      buf(pos - 1) = d.toByte
      buf(pos) = (d >> 8).toByte
      q0 = q1
      pos -= 2
    }
    if (q0 < 10) buf(pos) = (q0 + '0').toByte
    else {
      val d = ds(q0)
      buf(pos - 1) = d.toByte
      buf(pos) = (d >> 8).toByte
    }
  }

  private[this] def illegalNumberError(x: Double): Nothing = encodeError("illegal number: " + x)

  private[this] def writeNBytes(n: Int, b: Byte, pos: Int, buf: Array[Byte]): Int = {
    var i = 0
    while (i < n) {
      buf(pos + i) = b
      i += 1
    }
    pos + n
  }

  private[this] def ensureBufCapacity(required: Int): Int = {
    val pos = count
    if (pos + required <= limit) pos
    else flushAndGrowBuf(required, pos)
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
    setBuf(java.util.Arrays.copyOf(buf, (-1 >>> Integer.numberOfLeadingZeros(limit | required)) + 1))

  private[this] def reallocateBufToPreferredSize(): Unit = setBuf(new Array[Byte](config.preferredBufSize))

  private[this] def setBuf(buf: Array[Byte]): Unit = {
    this.buf = buf
    limit = buf.length
  }
}

object JsonWriter {
  private final val escapedChars: Array[Byte] = eval {
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
  private final val digits: Array[Short] = eval {
    val ds = new Array[Short](100)
    var i, j = 0
    while (j < 10) {
      var k = 0
      while (k < 10) {
        ds(i) = (((k + '0') << 8) + (j + '0')).toShort
        i += 1
        k += 1
      }
      j += 1
    }
    ds
  }
  private final val lowerCaseHexDigits: Array[Short] = eval {
    val ds = new Array[Short](256)
    var i, j = 0
    while (j < 16) {
      val d1 =
        if (j <= 9) j + '0'
        else j + 'a' - 10
      var k = 0
      while (k < 16) {
        val d2 =
          if (k <= 9) k + '0'
          else k + 'a' - 10
        ds(i) = ((d2 << 8) + d1).toShort
        i += 1
        k += 1
      }
      j += 1
    }
    ds
  }
  private final val upperCaseHexDigits: Array[Short] = eval {
    val ds = new Array[Short](256)
    var i, j = 0
    while (j < 16) {
      val d1 =
        if (j <= 9) j + '0'
        else j + 'A' - 10
      var k = 0
      while (k < 16) {
        val d2 =
          if (k <= 9) k + '0'
          else k + 'A' - 10
        ds(i) = ((d2 << 8) + d1).toShort
        i += 1
        k += 1
      }
      j += 1
    }
    ds
  }
  private final val base64Digits: Array[Byte] =
    eval("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes)
  private final val base64UrlDigits: Array[Byte] =
    eval("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".getBytes)
  private final val gs: Array[Long] = eval {
    val gs = new Array[Long](1234)
    var i = 0
    var pow5 = BigInt(1)
    while (i < 650) {
      val av = (pow5 >> (pow5.bitLength - 126)) + 1
      gs(648 - i) = (av >> 63).longValue & 0x7FFFFFFFFFFFFFFFL
      gs(649 - i) = av.longValue & 0x7FFFFFFFFFFFFFFFL
      pow5 *= 5
      i += 2
    }
    pow5 = BigInt(5)
    while (i < 1234) {
      val inv = ((BigInt(1) << (pow5.bitLength + 125)) / pow5) + 1
      gs(i) = (inv >> 63).longValue & 0x7FFFFFFFFFFFFFFFL
      gs(i + 1) = inv.longValue & 0x7FFFFFFFFFFFFFFFL
      pow5 *= 5
      i += 2
    }
    gs
  }
  @volatile private[this] var tenPow18Squares: Array[BigInteger] = Array(BigInteger.valueOf(1000000000000000000L))

  final private def getTenPow18Squares(n: Int): Array[BigInteger] = {
    var ss = tenPow18Squares
    var i = ss.length
    if (n >= i) {
      var s = ss(i - 1)
      ss = java.util.Arrays.copyOf(ss, n + 1)
      while (i <= n) {
        s = s.multiply(s)
        ss(i) = s
        i += 1
      }
      tenPow18Squares = ss
    }
    ss
  }

  final def isNonEscapedAscii(ch: Char): Boolean = ch < 0x80 && escapedChars(ch) == 0
}