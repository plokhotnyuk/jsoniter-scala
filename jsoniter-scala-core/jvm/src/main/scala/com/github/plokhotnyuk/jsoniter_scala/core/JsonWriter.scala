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

final class JsonWriter private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](32768),
    private[this] var count: Int = 0,
    private[this] var limit: Int = 32768,
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
    writeBigInteger(x.bigInteger, null)
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

  def writeKey(x: String): Unit = count = {
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 3)
    var buf = this.buf
    if (comma) {
      comma = false
      buf(pos) = ','
      pos += 1
      if (indention != 0) pos = writeIndention(buf, pos, indention)
    }
    buf(pos) = '"'
    pos += 1
    pos =
      if (JsonWriter.isGraalVM) writeString(x, 0, x.length, pos, limit - 1, escapedChars)
      else writeStringUnrolled(x, 0, pos, Math.min(x.length, limit - pos - 1) + pos, escapedChars)
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

  def writeNonEscapedAsciiKey(x: String): Unit = {
    val len = x.length
    val indention = this.indention
    val required = indention + len + 6
    if (required <= config.preferredBufSize) {
      var pos = ensureBufCapacity(required)
      val buf = this.buf
      if (comma) {
        comma = false
        buf(pos) = ','
        pos += 1
        if (indention != 0) pos = writeIndention(buf, pos, indention)
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
      count = pos
    } else writeLongNonEscapedAsciiKey(x)
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
    writeZoneId(x)
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
    writeBigInteger(x.bigInteger, null)
  }

  def writeVal(x: UUID): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
  }

  def writeVal(x: String): Unit = count = {
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 4)
    if (comma) {
      buf(pos) = ','
      pos += 1
      if (indention != 0) pos = writeIndention(buf, pos, indention)
    } else comma = true
    buf(pos) = '"'
    pos += 1
    pos =
      if (JsonWriter.isGraalVM) writeString(x, 0, x.length, pos, limit - 1, escapedChars)
      else writeStringUnrolled(x, 0, pos, Math.min(x.length, limit - pos - 1) + pos, escapedChars)
    buf(pos) = '"'
    pos + 1
  }

  def writeNonEscapedAsciiVal(x: String): Unit = {
    val len = x.length
    val indention = this.indention
    val required = indention + len + 4
    if (required <= config.preferredBufSize) {
      var pos = ensureBufCapacity(required)
      val buf = this.buf
      if (comma) {
        buf(pos) = ','
        pos += 1
        if (indention != 0) pos = writeIndention(buf, pos, indention)
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
      count = pos + 1
    } else writeLongNonEscapedAsciiVal(x)
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
    writeZoneId(x)
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
    writeOptionalCommaAndIndentionBeforeValue()
    val ds =
      if (lowerCase) lowerCaseHexDigits
      else upperCaseHexDigits
    writeBase16Bytes(bs, ds)
  }

  def writeBase64Val(bs: Array[Byte], doPadding: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64Digits, doPadding)
  }

  def writeBase64UrlVal(bs: Array[Byte], doPadding: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64UrlDigits, doPadding)
  }

  def writeRawVal(bs: Array[Byte]): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeRawBytes(bs)
  }

  def writeNull(): Unit = count = {
    writeOptionalCommaAndIndentionBeforeValue()
    val pos = ensureBufCapacity(4)
    ByteArrayAccess.setInt(buf, pos, 0x6C6C756E)
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

  private[jsoniter_scala] def writeToString[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): String =
    try {
      this.config = config
      count = 0
      indention = 0
      comma = false
      disableBufGrowing = false
      codec.encodeValue(x, this)
      new String(buf, 0, count, StandardCharsets.UTF_8)
    } finally {
      if (limit > config.preferredBufSize) reallocateBufToPreferredSize()
    }

  private[jsoniter_scala] def writeToStringWithoutBufReallocation[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): String = {
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
    val n = indention
    val pos = ensureBufCapacity(n + 1)
    writeIndention(buf, pos, n)
  }

  private[this] def writeIndention(buf: Array[Byte], pos: Int, n: Int): Int = {
    var p = pos
    buf(p) = '\n'
    p += 1
    val posLim = p + n
    while (p < posLim) {
      buf(p) = ' '
      p += 1
    }
    p
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
    var pos = count
    if (pos >= limit) pos = flushAndGrowBuf(1, pos)
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
        ByteArrayAccess.setInt(buf, pos, ds(bs(offset) & 0xFF) | (ds(bs(offset + 1) & 0xFF) << 16))
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
      ByteArrayAccess.setShort(buf, pos, ds(bs(offset) & 0xFF))
      pos += 2
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

  private[this] def writeLongNonEscapedAsciiKey(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    var pos = count
    var step = Math.max(config.preferredBufSize, limit - pos)
    var remaining = x.length
    var offset = 0
    while (remaining > 0) {
      step = Math.min(step, remaining)
      if (pos + step > limit) pos = flushAndGrowBuf(step, pos)
      val newOffset = offset + step
      x.getBytes(offset, newOffset, buf, pos)
      offset = newOffset
      pos += step
      remaining -= step
    }
    count = pos
    writeBytes('"')
    writeColon()
  }

  private[this] def writeLongNonEscapedAsciiVal(x: String): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    var pos = count
    var step = Math.max(config.preferredBufSize, limit - pos)
    var remaining = x.length
    var offset = 0
    while (remaining > 0) {
      step = Math.min(step, remaining)
      if (pos + step > limit) pos = flushAndGrowBuf(step, pos)
      val newOffset = offset + step
      x.getBytes(offset, newOffset, buf, pos)
      offset = newOffset
      pos += step
      remaining -= step
    }
    count = pos
    writeBytes('"')
  }

  private[this] def writeZoneId(x: ZoneId): Unit = count = {
    val s = x.getId
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
    val pos = ensureBufCapacity(40) // 38 == (new java.util.UUID(0, 0)).toString.length + 2
    val buf = this.buf
    val ds = lowerCaseHexDigits
    val mostSigBits1 = (mostSigBits >> 32).toInt
    val d1 = ds(mostSigBits1 >>> 24) << 8
    val d2 = ds((mostSigBits1 >> 16) & 0xFF).toLong << 24
    val d3 = ds((mostSigBits1 >> 8) & 0xFF).toLong << 40
    val d4 = ds(mostSigBits1 & 0xFF)
    ByteArrayAccess.setLong(buf, pos, '"' | d1 | d2 | d3 | d4.toLong << 56)
    val mostSigBits2 = mostSigBits.toInt
    val d5 = ds(mostSigBits2 >>> 24) << 16
    val d6 = ds((mostSigBits2 >> 16) & 0xFF).toLong << 32
    val d7 = ds((mostSigBits2 >> 8) & 0xFF)
    ByteArrayAccess.setLong(buf, pos + 8, d4 >> 8 | d5 | d6 | d7.toLong << 56 | 0x2D000000002D00L) // 0x2D000000002D00L == '-'.toLong << 48 | '-' << 8
    val d8 = ds(mostSigBits2 & 0xFF) << 8
    val leastSigBits1 = (leastSigBits >> 32).toInt
    val d9 = ds(leastSigBits1 >>> 24).toLong << 32
    val d10 = ds((leastSigBits1 >> 16) & 0xFF).toLong << 48
    ByteArrayAccess.setLong(buf, pos + 16, d7 >> 8 | d8 | d9 | d10 | 0x2D000000) // 0x2D000000 == '-' << 24
    val d11 = ds((leastSigBits1 >> 8) & 0xFF) << 8
    val d12 = ds(leastSigBits1 & 0xFF).toLong << 24
    val leastSigBits2 = leastSigBits.toInt
    val d13 = ds(leastSigBits2 >>> 24).toLong << 40
    val d14 = ds((leastSigBits2 >> 16) & 0xFF)
    ByteArrayAccess.setLong(buf, pos + 24, '-' | d11 | d12| d13 | d14.toLong << 56)
    val d15 = ds((leastSigBits2 >> 8) & 0xFF) << 8
    val d16 = ds(leastSigBits2 & 0xFF).toLong << 24
    ByteArrayAccess.setLong(buf, pos + 32, d14 >> 8 | d15 | d16 | 0x220000000000L) // 0x220000000000L == '"'.toLong << 40
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
      else writeEscapedOrEncodedString(s, from, pos, escapedChars)
    }

  @tailrec
  private[this] def writeStringUnrolled(s: String, from: Int, pos: Int, minLim: Int, escapedChars: Array[Byte]): Int =
    if (pos + 3 < minLim) {
      val ch1 = s.charAt(from)
      val ch2 = s.charAt(from + 1)
      val ch3 = s.charAt(from + 2)
      val ch4 = s.charAt(from + 3)
      buf(pos) = ch1.toByte
      buf(pos + 1) = ch2.toByte
      buf(pos + 2) = ch3.toByte
      buf(pos + 3) = ch4.toByte
      if ((ch1 | ch2 | ch3 | ch4) < 0x80 &&
        (escapedChars(ch1) | escapedChars(ch2) | escapedChars(ch3) | escapedChars(ch4)) == 0) {
        writeStringUnrolled(s, from + 4, pos + 4, minLim, escapedChars)
      } else writeEscapedOrEncodedString(s, from, pos, escapedChars)
    } else if (pos < minLim) {
      val ch = s.charAt(from)
      buf(pos) = ch.toByte
      if (ch < 0x80 && escapedChars(ch) == 0) writeStringUnrolled(s, from + 1, pos + 1, minLim, escapedChars)
      else writeEscapedOrEncodedString(s, from, pos, escapedChars)
    } else {
      val remaining = s.length - from
      if (remaining > 0) {
        val newPos = flushAndGrowBuf(2, pos)
        writeStringUnrolled(s, from, newPos, Math.min(remaining, limit - newPos - 1) + newPos, escapedChars)
      } else pos
    }

  private[this] def writeEscapedOrEncodedString(s: String, from: Int, pos: Int, escapedChars: Array[Byte]): Int =
    if (config.escapeUnicode) writeEscapedString(s, from, s.length, pos, limit - 13, escapedChars)
    else writeEncodedString(s, from, s.length, pos, limit - 7, escapedChars)

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
          ByteArrayAccess.setShort(buf, pos, (esc << 8 | 0x5C).toShort)
          writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEncodedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos, buf), posLim, escapedChars)
      } else if (ch1 < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
        ByteArrayAccess.setShort(buf, pos, (ch1 >> 6 | (ch1 << 8 & 0x3F00) | 0x80C0).toShort)
        writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
      } else if (ch1 < 0xD800 || ch1 > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
        ByteArrayAccess.setInt(buf, pos, ch1 >> 12 | (ch1 << 2 & 0x3F00) | (ch1 << 16 & 0x3F0000) | 0x8080E0)
        writeEncodedString(s, from + 1, to, pos + 3, posLim, escapedChars)
      } else { // 110110uuuuccccbb 110111bbbbaaaaaa (UTF-16 chars) -> 11110ddd 10ddcccc 10bbbbbb 10aaaaaa (UTF-8 bytes), where ddddd = uuuu + 1
        if (ch1 >= 0xDC00 || from + 1 >= to) illegalSurrogateError()
        val ch2 = s.charAt(from + 1)
        if (ch2 < 0xDC00 || ch2 > 0xDFFF) illegalSurrogateError()
        val cp = (ch1 << 10) + (ch2 - 56613888) // -56613888 == 0x010000 - (0xD800 << 10) - 0xDC00
        ByteArrayAccess.setInt(buf, pos, cp >> 18 | (cp >> 4 & 0x3F00) | (cp << 10 & 0x3F0000) | (cp << 24 & 0x3F000000) | 0x808080F0)
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
          ByteArrayAccess.setShort(buf, pos, (esc << 8 | 0x5C).toShort)
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
        ByteArrayAccess.setShort(buf, pos, (esc << 8 | 0x5C).toShort)
        pos += 2
      } else pos = writeEscapedUnicode(ch.toByte, pos, buf)
    } else if (config.escapeUnicode) {
      if (ch >= 0xD800 && ch <= 0xDFFF) illegalSurrogateError()
      pos = writeEscapedUnicode(ch, pos, buf)
    } else if (ch < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
      ByteArrayAccess.setShort(buf, pos, (ch >> 6 | (ch << 8 & 0x3F00) | 0x80C0).toShort)
      pos += 2
    } else if (ch < 0xD800 || ch > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
      ByteArrayAccess.setInt(buf, pos, ch >> 12 | (ch << 2 & 0x3F00) | (ch << 16 & 0x3F0000) | 0x8080E0)
      pos += 3
    } else illegalSurrogateError()
    buf(pos) = '"'
    pos + 1
  }

  private[this] def writeEscapedUnicode(ch: Char, pos: Int, buf: Array[Byte]): Int = {
    val ds = lowerCaseHexDigits
    ByteArrayAccess.setShort(buf, pos, 0x755C)
    ByteArrayAccess.setInt(buf, pos + 2, ds(ch >> 8) | ds(ch & 0xFF) << 16)
    pos + 6
  }

  private[this] def writeEscapedUnicode(b: Byte, pos: Int, buf: Array[Byte]): Int = {
    val d = lowerCaseHexDigits(b & 0xFF)
    ByteArrayAccess.setInt(buf, pos, 0x3030755C)
    ByteArrayAccess.setShort(buf, pos + 4, d)
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
    val m = Math.max((x.bitLength * 71828554L >> 32).toInt - 1, 1) // Math.max((x.bitLength * Math.log(1e18) / Math.log(2)).toInt - 1, 1)
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
    val pos = ensureBufCapacity(8) // bytes in Long
    if (x) {
      ByteArrayAccess.setInt(buf, pos, 0x65757274)
      pos + 4
    } else {
      ByteArrayAccess.setLong(buf, pos, 0x65736c6166L)
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
      ByteArrayAccess.setShort(buf, pos, digits(q0))
      pos + 2
    } else {
      buf(pos) = '1'
      ByteArrayAccess.setShort(buf, pos + 1, digits(q0 - 100))
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
      val effectiveTotalSecs =
        if (totalSecs < 0) (-nano >> 31) - totalSecs
        else totalSecs
      val hours = Math.multiplyHigh(effectiveTotalSecs >> 4, 655884233731895169L) >> 3 // divide a positive long by 3600
      val secsOfHour = (effectiveTotalSecs - hours * 3600).toInt
      val minutes = secsOfHour * 17477 >> 20 // divide a small positive int by 60
      val seconds = secsOfHour - minutes * 60
      val ds = digits
      if (hours != 0) {
        if (totalSecs < 0) {
          buf(pos) = '-'
          pos += 1
        }
        pos =
          if (hours.toInt == hours) writePositiveInt(hours.toInt, pos, buf, ds)
          else {
            val q1 = Math.multiplyHigh(hours, 193428131138340668L) >>> 20 // divide a positive long by 100000000
            write8Digits((hours - q1 * 100000000).toInt, writePositiveInt(q1.toInt, pos, buf, ds), buf, ds)
          }
        buf(pos) = 'H'
        pos += 1
      }
      if (minutes != 0) {
        if (totalSecs < 0) {
          buf(pos) = '-'
          pos += 1
        }
        if (minutes < 10) {
          buf(pos) = (minutes + '0').toByte
          pos += 1
        } else pos = write2Digits(minutes, pos, buf, ds)
        buf(pos) = 'M'
        pos += 1
      }
      if ((seconds | nano) != 0) {
        if (totalSecs < 0) {
          buf(pos) = '-'
          pos += 1
        }
        if (seconds < 10) {
          buf(pos) = (seconds + '0').toByte
          pos += 1
        } else pos = write2Digits(seconds, pos, buf, ds)
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
    val epochDay = (Math.multiplyHigh({
      if (epochSecond >= 0) epochSecond
      else epochSecond - 86399
    }, 1749024623285053783L) >> 13) - (epochSecond >> 63) // (if (epochSecond >= 0) epochSecond else epochSecond - 86399) / 86400
    val secsOfDay = (epochSecond - epochDay * 86400).toInt
    var marchZeroDay = epochDay + 719468 // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    var adjustYear = 0
    if (marchZeroDay < 0) { // adjust negative years to positive for calculation
      val marchZeroDayP1 = marchZeroDay + 1
      val adjust400YearCycles = (((marchZeroDayP1 * 7525902) >> 40) + (~marchZeroDayP1 >> 63)).toInt // ((marchZeroDay + 1) / 146097).toInt - 1 (146097 == number of days in a 400 year cycle)
      adjustYear = adjust400YearCycles * 400
      marchZeroDay -= adjust400YearCycles * 146097L // 146097 == number of days in a 400 year cycle
    }
    var year = { // ((marchZeroDay * 400 + 591) / 146097).toInt
      val pa = marchZeroDay * 400 + 591
      ((Math.multiplyHigh(pa, 4137408090565272301L) >> 15) + (pa >> 63)).toInt
    }
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

  private[this] def toMarchDayOfYear(marchZeroDay: Long, year: Int): Int = {
    val century = (year * 1374389535L >> 37).toInt // divide an int by 100 (the sign correction is not needed)
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
    val pos = ensureBufCapacity(9) // 9 == "--01-01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    ByteArrayAccess.setLong(buf, pos + 1, ds(x.getDayOfMonth).toLong << 40 | ds(x.getMonthValue) << 16 | 0x2200002D00002D2DL)
    pos + 9
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
      val ds = digits
      val years = x.getYears
      val months = x.getMonths
      val days = x.getDays
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
    val ds = digits
    buf(pos) = '"'
    pos = writeYear(x.getYear, pos + 1, buf, ds)
    ByteArrayAccess.setInt(buf, pos, ds(x.getMonthValue) << 8 | 0x2200002D)
    pos + 4
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
    val pos = writeYear(x.getYear, p, buf, ds)
    ByteArrayAccess.setInt(buf, pos, ds(x.getMonthValue) << 8 | 0x2D00002D)
    ByteArrayAccess.setShort(buf, pos + 4, ds(x.getDayOfMonth))
    pos + 6
  }

  private[this] def writeLocalDate(year: Int, month: Int, day: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYear(year, p, buf, ds)
    ByteArrayAccess.setInt(buf, pos, ds(month) << 8 | 0x2D00002D)
    ByteArrayAccess.setShort(buf, pos + 4, ds(day))
    pos + 6
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

  private[this] def writeLocalTime(x: LocalTime, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val d = ds(x.getHour) | ds(x.getMinute).toLong << 24
    val second = x.getSecond
    val nano = x.getNano
    if ((second | nano) == 0) {
      ByteArrayAccess.setLong(buf, pos, d | 0x3A0000)
      pos + 5
    } else {
      ByteArrayAccess.setLong(buf, pos, d | ds(second).toLong << 48 | 0x3A00003A0000L)
      if (nano == 0) pos + 8
      else {
        buf(pos + 8) = '.'
        writeNanos(nano, pos + 9, buf, ds)
      }
    }
  }

  private[this] def writeLocalTime(hour: Int, minute: Int, second: Int, nano: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    ByteArrayAccess.setLong(buf, pos, ds(hour) | ds(minute).toLong << 24 | ds(second).toLong << 48 | 0x3A00003A0000L)
    if (nano == 0) pos + 8
    else {
      buf(pos + 8) = '.'
      writeNanos(nano, pos + 9, buf, ds)
    }
  }

  private[this] def writeNanos(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = (q0 * 1801439851L >> 54).toInt // divide a positive int by 10000000
    val r1 = q0 - q1 * 10000000
    val p2 = r1 * 175921861L
    val q2 = (p2 >> 44).toInt // divide a positive int by 100000
    val d = ds(q2)
    ByteArrayAccess.setInt(buf, pos, ds(q1) | d << 16)
    if ((p2 & 0xFFFF8000000L) == 0 && d <= 0x3039) pos + 3 // check if nanos are divisible by 1000000
    else {
      val r2 = r1 - q2 * 100000
      val p3 = r2 * 2199023256L
      val q3 = (p3 >> 41).toInt // divide a positive int by 1000
      ByteArrayAccess.setShort(buf, pos + 4, ds(q3))
      if ((p3 & 0x1FF80000000L) == 0) pos + 6 // check if r2 divisible by 1000
      else write3Digits(r2 - q3 * 1000, pos + 6, buf, ds)
    }
  }

  private[this] def writeOffset(x: ZoneOffset, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = x.getTotalSeconds
    if (q0 == 0) {
      buf(pos) = 'Z'
      pos + 1
    } else {
      var m = 0x3A00002B
      if (q0 < 0) {
        q0 = -q0
        m = 0x3A00002D
      }
      val p1 = q0 * 37283
      val q1 = p1 >>> 27 // divide a small positive int by 3600
      ByteArrayAccess.setInt(buf, pos, ds(q1) << 8 | m)
      if ((p1 & 0x7FF8000) == 0) { // check if q0 is divisible by 3600
        ByteArrayAccess.setShort(buf, pos + 4, 0x3030)
        pos + 6
      } else {
        val r1 = q0 - q1 * 3600
        val p2 = r1 * 17477
        val q2 = p2 >> 20 // divide a small positive int by 60
        ByteArrayAccess.setShort(buf, pos + 4, ds(q2))
        if ((p2 & 0xFC000) == 0) pos + 6 // check if r1 is divisible by 60
        else {
          buf(pos + 6) = ':'
          ByteArrayAccess.setShort(buf, pos + 7, ds(r1 - q2 * 60))
          pos + 9
        }
      }
    }
  }

  private[this] def write2Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    ByteArrayAccess.setShort(buf, pos, ds(q0))
    pos + 2
  }

  private[this] def write3Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 * 1311 >> 17 // divide a small positive int by 100
    buf(pos) = (q1 + '0').toByte
    ByteArrayAccess.setShort(buf, pos + 1, ds(q0 - q1 * 100))
    pos + 3
  }

  private[this] def write4Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = q0 * 5243 >> 19 // divide a small positive int by 100
    ByteArrayAccess.setInt(buf, pos, ds(q1) | ds(q0 - q1 * 100) << 16)
    pos + 4
  }

  private[this] def write5Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =  {
    val y0 = q0 * 429497L // James Anhalt's algorithm for 5 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    buf(pos) = ((y0 >>> 32).toInt + '0').toByte
    val y1 = (y0 & 0xFFFFFFFFL) * 100
    val y2 = (y1 & 0xFFFFFFFFL) * 100
    ByteArrayAccess.setInt(buf, pos + 1, ds((y1 >>> 32).toInt) | ds((y2 >>> 32).toInt) << 16)
    pos + 5
  }

  private[this] def write8Digits(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val y1 = q0 * 140737489L // James Anhalt's algorithm for 8 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0x7FFFFFFFFFFFL) * 100
    val y3 = (y2 & 0x7FFFFFFFFFFFL) * 100
    val y4 = (y3 & 0x7FFFFFFFFFFFL) * 100
    ByteArrayAccess.setLong(buf, pos,
      ds((y1 >>> 47).toInt) | ds((y2 >>> 47).toInt) << 16 | ds((y3 >>> 47).toInt).toLong << 32 | ds((y4 >>> 47).toInt).toLong << 48)
    pos + 8
  }

  private[this] def write18Digits(q0: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = Math.multiplyHigh(q0, 193428131138340668L) >>> 20 // divide a positive long by 100000000
    write8Digits((q0 - q1 * 100000000).toInt, {
      val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
      write8Digits((q1 - q2 * 100000000).toInt, write2Digits(q2.toInt, pos, buf, ds), buf, ds)
    }, buf, ds)
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
    } else write5Digits(q0, pos, buf, digits)
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
      val q1 = Math.multiplyHigh(q0, 193428131138340668L) >>> 20 // divide a positive long by 100000000
      write8Digits((q0 - q1 * 100000000).toInt, {
        if (q1.toInt == q1) writePositiveInt(q1.toInt, pos, buf, ds)
        else {
          val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
          write8Digits((q1 - q2 * 100000000).toInt, writePositiveInt(q2.toInt, pos, buf, ds), buf, ds)
        }
      }, buf, ds)
    }
  }

  private[this] def writePositiveInt(q0: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val lastPos = digitCount(q0) + pos
    writePositiveIntDigits(q0, lastPos - 1, buf, ds)
    lastPos
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java
  private[this] def writeFloat(x: Float): Unit = count = {
    val bits = java.lang.Float.floatToRawIntBits(x)
    var pos = ensureBufCapacity(15)
    val buf = this.buf
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
          dv = (s * 3435973837L >> 35).toInt // divide a positive int by 10
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
      val ds = digits
      val len = digitCount(dv)
      exp += len - 1
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
    val x1 = Math.multiplyHigh(g, cp.toLong << 32)
    (x1 >>> 31).toInt | -x1.toInt >>> 31
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/DoubleToDecimal.java
  private[this] def writeDouble(x: Double): Unit = count = {
    val bits = java.lang.Double.doubleToRawLongBits(x)
    var pos = ensureBufCapacity(24)
    val buf = this.buf
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
          dv = Math.multiplyHigh(s, 1844674407370955168L) // divide a positive long by 10
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
      val ds = digits
      val len = digitCount(dv)
      exp += len - 1
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
    val z = (g1 * cp >>> 1) + Math.multiplyHigh(g0, cp)
    Math.multiplyHigh(g1, cp) + (z >>> 63) | -(z & 0x7FFFFFFFFFFFFFFFL) >>> 63
  }

  // Adoption of a nice trick from Daniel Lemire's blog that works for numbers up to 10^18:
  // https://lemire.me/blog/2021/06/03/computing-the-number-of-digits-of-an-integer-even-faster/
  private[this] def digitCount(q0: Long): Int = (offsets(java.lang.Long.numberOfLeadingZeros(q0)) + q0 >> 58).toInt

  private[this] def writeSignificantFractionDigits(q0: Long, pos: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (q0.toInt == q0) writeSignificantFractionDigits(q0.toInt, pos, posLim, buf, ds)
    else {
      val q1 = Math.multiplyHigh(q0, 193428131138340668L) >>> 20 // divide a positive long by 100000000
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
    var q1 = 0
    var pos = p
    while ({
      val qp = q0 * 1374389535L
      q1 = (qp >> 37).toInt // divide a positive int by 100
      (qp & 0x1FC0000000L) == 0 // check if q is divisible by 100
    }) {
      q0 = q1
      pos -= 2
    }
    val d = ds(q0 - q1 * 100)
    ByteArrayAccess.setShort(buf, pos - 1, d)
    var lastPos = pos
    if (d > 0x3039) lastPos += 1
    writeFractionDigits(q1, pos - 2, posLim, buf, ds)
    lastPos
  }

  private[this] def writeFractionDigits(q: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = q
    var pos = p
    while (pos > posLim) {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide a positive int by 100
      ByteArrayAccess.setShort(buf, pos - 1, ds(q0 - q1 * 100))
      q0 = q1
      pos -= 2
    }
  }

  private[this] def writePositiveIntDigits(q: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = q
    var pos = p
    while (q0 >= 100) {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide a positive int by 100
      ByteArrayAccess.setShort(buf, pos - 1, ds(q0 - q1 * 100))
      q0 = q1
      pos -= 2
    }
    if (q0 < 10) buf(pos) = (q0 + '0').toByte
    else ByteArrayAccess.setShort(buf, pos - 1, ds(q0))
  }

  private[this] def illegalNumberError(x: Double): Nothing = encodeError("illegal number: " + x)

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
  private final val isGraalVM: Boolean =
    Option(System.getProperty("java.vendor.version")).getOrElse(System.getProperty("java.vm.name")).contains("GraalVM") ||
      java.lang.management.ManagementFactory.getRuntimeMXBean.getInputArguments.contains("-XX:+UseJVMCICompiler")
  /* Use the following code to generate `escapedChars` in Scala REPL:
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
    es.grouped(16).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val escapedChars: Array[Byte] = Array(
    -1, -1, -1, -1, -1, -1, -1, -1, 98, 116, 110, -1, 102, 114, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    0, 0, 34, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 92, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1
  )
  private final val offsets = Array(
    5088146770730811392L, 5088146770730811392L, 5088146770730811392L, 5088146770730811392L,
    5088146770730811392L, 5088146770730811392L, 5088146770730811392L, 5088146770730811392L,
    4889916394579099648L, 4889916394579099648L, 4889916394579099648L, 4610686018427387904L,
    4610686018427387904L, 4610686018427387904L, 4610686018427387904L, 4323355642275676160L,
    4323355642275676160L, 4323355642275676160L, 4035215266123964416L, 4035215266123964416L,
    4035215266123964416L, 3746993889972252672L, 3746993889972252672L, 3746993889972252672L,
    3746993889972252672L, 3458764413820540928L, 3458764413820540928L, 3458764413820540928L,
    3170534127668829184L, 3170534127668829184L, 3170534127668829184L, 2882303760517117440L,
    2882303760517117440L, 2882303760517117440L, 2882303760517117440L, 2594073385265405696L,
    2594073385265405696L, 2594073385265405696L, 2305843009203693952L, 2305843009203693952L,
    2305843009203693952L, 2017612633060982208L, 2017612633060982208L, 2017612633060982208L,
    2017612633060982208L, 1729382256910170464L, 1729382256910170464L, 1729382256910170464L,
    1441151880758548720L, 1441151880758548720L, 1441151880758548720L, 1152921504606845976L,
    1152921504606845976L, 1152921504606845976L, 1152921504606845976L, 864691128455135132L,
    864691128455135132L, 864691128455135132L, 576460752303423478L, 576460752303423478L,
    576460752303423478L, 576460752303423478L, 576460752303423478L, 576460752303423478L,
    576460752303423478L)
  /* Use the following code to generate `digits` in Scala REPL:
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
    ds.grouped(10).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val digits: Array[Short] = Array(
    12336, 12592, 12848, 13104, 13360, 13616, 13872, 14128, 14384, 14640,
    12337, 12593, 12849, 13105, 13361, 13617, 13873, 14129, 14385, 14641,
    12338, 12594, 12850, 13106, 13362, 13618, 13874, 14130, 14386, 14642,
    12339, 12595, 12851, 13107, 13363, 13619, 13875, 14131, 14387, 14643,
    12340, 12596, 12852, 13108, 13364, 13620, 13876, 14132, 14388, 14644,
    12341, 12597, 12853, 13109, 13365, 13621, 13877, 14133, 14389, 14645,
    12342, 12598, 12854, 13110, 13366, 13622, 13878, 14134, 14390, 14646,
    12343, 12599, 12855, 13111, 13367, 13623, 13879, 14135, 14391, 14647,
    12344, 12600, 12856, 13112, 13368, 13624, 13880, 14136, 14392, 14648,
    12345, 12601, 12857, 13113, 13369, 13625, 13881, 14137, 14393, 14649
  )
  /* Use the following code to generate `lowerCaseHexDigits` in Scala REPL:
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
    ds.grouped(16).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val lowerCaseHexDigits: Array[Short] = Array(
    12336, 12592, 12848, 13104, 13360, 13616, 13872, 14128, 14384, 14640, 24880, 25136, 25392, 25648, 25904, 26160,
    12337, 12593, 12849, 13105, 13361, 13617, 13873, 14129, 14385, 14641, 24881, 25137, 25393, 25649, 25905, 26161,
    12338, 12594, 12850, 13106, 13362, 13618, 13874, 14130, 14386, 14642, 24882, 25138, 25394, 25650, 25906, 26162,
    12339, 12595, 12851, 13107, 13363, 13619, 13875, 14131, 14387, 14643, 24883, 25139, 25395, 25651, 25907, 26163,
    12340, 12596, 12852, 13108, 13364, 13620, 13876, 14132, 14388, 14644, 24884, 25140, 25396, 25652, 25908, 26164,
    12341, 12597, 12853, 13109, 13365, 13621, 13877, 14133, 14389, 14645, 24885, 25141, 25397, 25653, 25909, 26165,
    12342, 12598, 12854, 13110, 13366, 13622, 13878, 14134, 14390, 14646, 24886, 25142, 25398, 25654, 25910, 26166,
    12343, 12599, 12855, 13111, 13367, 13623, 13879, 14135, 14391, 14647, 24887, 25143, 25399, 25655, 25911, 26167,
    12344, 12600, 12856, 13112, 13368, 13624, 13880, 14136, 14392, 14648, 24888, 25144, 25400, 25656, 25912, 26168,
    12345, 12601, 12857, 13113, 13369, 13625, 13881, 14137, 14393, 14649, 24889, 25145, 25401, 25657, 25913, 26169,
    12385, 12641, 12897, 13153, 13409, 13665, 13921, 14177, 14433, 14689, 24929, 25185, 25441, 25697, 25953, 26209,
    12386, 12642, 12898, 13154, 13410, 13666, 13922, 14178, 14434, 14690, 24930, 25186, 25442, 25698, 25954, 26210,
    12387, 12643, 12899, 13155, 13411, 13667, 13923, 14179, 14435, 14691, 24931, 25187, 25443, 25699, 25955, 26211,
    12388, 12644, 12900, 13156, 13412, 13668, 13924, 14180, 14436, 14692, 24932, 25188, 25444, 25700, 25956, 26212,
    12389, 12645, 12901, 13157, 13413, 13669, 13925, 14181, 14437, 14693, 24933, 25189, 25445, 25701, 25957, 26213,
    12390, 12646, 12902, 13158, 13414, 13670, 13926, 14182, 14438, 14694, 24934, 25190, 25446, 25702, 25958, 26214
  )
  /* Use the following code to generate `upperCaseHexDigits` in Scala REPL:
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
    ds.grouped(16).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val upperCaseHexDigits: Array[Short] = Array(
    12336, 12592, 12848, 13104, 13360, 13616, 13872, 14128, 14384, 14640, 16688, 16944, 17200, 17456, 17712, 17968,
    12337, 12593, 12849, 13105, 13361, 13617, 13873, 14129, 14385, 14641, 16689, 16945, 17201, 17457, 17713, 17969,
    12338, 12594, 12850, 13106, 13362, 13618, 13874, 14130, 14386, 14642, 16690, 16946, 17202, 17458, 17714, 17970,
    12339, 12595, 12851, 13107, 13363, 13619, 13875, 14131, 14387, 14643, 16691, 16947, 17203, 17459, 17715, 17971,
    12340, 12596, 12852, 13108, 13364, 13620, 13876, 14132, 14388, 14644, 16692, 16948, 17204, 17460, 17716, 17972,
    12341, 12597, 12853, 13109, 13365, 13621, 13877, 14133, 14389, 14645, 16693, 16949, 17205, 17461, 17717, 17973,
    12342, 12598, 12854, 13110, 13366, 13622, 13878, 14134, 14390, 14646, 16694, 16950, 17206, 17462, 17718, 17974,
    12343, 12599, 12855, 13111, 13367, 13623, 13879, 14135, 14391, 14647, 16695, 16951, 17207, 17463, 17719, 17975,
    12344, 12600, 12856, 13112, 13368, 13624, 13880, 14136, 14392, 14648, 16696, 16952, 17208, 17464, 17720, 17976,
    12345, 12601, 12857, 13113, 13369, 13625, 13881, 14137, 14393, 14649, 16697, 16953, 17209, 17465, 17721, 17977,
    12353, 12609, 12865, 13121, 13377, 13633, 13889, 14145, 14401, 14657, 16705, 16961, 17217, 17473, 17729, 17985,
    12354, 12610, 12866, 13122, 13378, 13634, 13890, 14146, 14402, 14658, 16706, 16962, 17218, 17474, 17730, 17986,
    12355, 12611, 12867, 13123, 13379, 13635, 13891, 14147, 14403, 14659, 16707, 16963, 17219, 17475, 17731, 17987,
    12356, 12612, 12868, 13124, 13380, 13636, 13892, 14148, 14404, 14660, 16708, 16964, 17220, 17476, 17732, 17988,
    12357, 12613, 12869, 13125, 13381, 13637, 13893, 14149, 14405, 14661, 16709, 16965, 17221, 17477, 17733, 17989,
    12358, 12614, 12870, 13126, 13382, 13638, 13894, 14150, 14406, 14662, 16710, 16966, 17222, 17478, 17734, 17990
  )
  /* Use the following code to generate `base64Digits` in Scala REPL:
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes
      .grouped(16).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val base64Digits: Array[Byte] = Array(
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
    81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102,
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
    119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47
  )
  /* Use the following code to generate `base64UrlDigits` in Scala REPL:
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".getBytes
      .grouped(16).map(_.mkString(", ")).mkString("Array(\n", ",\n", "\n)")
   */
  private final val base64UrlDigits: Array[Byte] = Array(
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
    81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102,
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
    119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 45, 95
  )
  /* Use the following code to generate `gs` in Scala REPL:
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
    gs.grouped(4).map(_.mkString("L, ")).mkString("Array(\n", "L,\n", "L\n)")
   */
  private final val gs: Array[Long] = Array(
    5696189077778435540L, 6557778377634271669L, 9113902524445496865L, 1269073367360058862L,
    7291122019556397492L, 1015258693888047090L, 5832897615645117993L, 6346230177223303157L,
    4666318092516094394L, 8766332956520552849L, 7466108948025751031L, 8492109508320019073L,
    5972887158420600825L, 4949013199285060097L, 4778309726736480660L, 3959210559428048077L,
    7645295562778369056L, 6334736895084876923L, 6116236450222695245L, 3223115108696946377L,
    4892989160178156196L, 2578492086957557102L, 7828782656285049914L, 436238524390181040L,
    6263026125028039931L, 2193665226883099993L, 5010420900022431944L, 9133629810990300641L,
    8016673440035891111L, 9079784475471615541L, 6413338752028712889L, 5419153173006337271L,
    5130671001622970311L, 6179996945776024979L, 8209073602596752498L, 6198646298499729642L,
    6567258882077401998L, 8648265853541694037L, 5253807105661921599L, 1384589460720489745L,
    8406091369059074558L, 5904691951894693915L, 6724873095247259646L, 8413102376257665455L,
    5379898476197807717L, 4885807493635177203L, 8607837561916492348L, 438594360332462878L,
    6886270049533193878L, 4040224303007880625L, 5509016039626555102L, 6921528257148214824L,
    8814425663402488164L, 3695747581953323071L, 7051540530721990531L, 4801272472933613619L,
    5641232424577592425L, 1996343570975935733L, 9025971879324147880L, 3194149713561497173L,
    7220777503459318304L, 2555319770849197738L, 5776622002767454643L, 3888930224050313352L,
    4621297602213963714L, 6800492993982161005L, 7394076163542341943L, 5346765568258592123L,
    5915260930833873554L, 7966761269348784022L, 4732208744667098843L, 8218083422849982379L,
    7571533991467358150L, 2080887032334240837L, 6057227193173886520L, 1664709625867392670L,
    4845781754539109216L, 1331767700693914136L, 7753250807262574745L, 7664851543223128102L,
    6202600645810059796L, 6131881234578502482L, 4962080516648047837L, 3060830580291846824L,
    7939328826636876539L, 6742003335837910079L, 6351463061309501231L, 7238277076041283225L,
    5081170449047600985L, 3945947253462071419L, 8129872718476161576L, 6313515605539314269L,
    6503898174780929261L, 3206138077060496254L, 5203118539824743409L, 720236054277441842L,
    8324989663719589454L, 4841726501585817270L, 6659991730975671563L, 5718055608639608977L,
    5327993384780537250L, 8263793301653597505L, 8524789415648859601L, 3998697245790980200L,
    6819831532519087681L, 1354283389261828999L, 5455865226015270144L, 8462124340893283845L,
    8729384361624432231L, 8005375723316388668L, 6983507489299545785L, 4559626171282155773L,
    5586805991439636628L, 3647700937025724618L, 8938889586303418605L, 3991647091870204227L,
    7151111669042734884L, 3193317673496163382L, 5720889335234187907L, 4399328546167885867L,
    9153422936374700651L, 8883600081239572549L, 7322738349099760521L, 5262205657620702877L,
    5858190679279808417L, 2365090118725607140L, 4686552543423846733L, 7426095317093351197L,
    7498484069478154774L, 813706063123630946L, 5998787255582523819L, 2495639257869859918L,
    4799029804466019055L, 3841185813666843096L, 7678447687145630488L, 6145897301866948954L,
    6142758149716504390L, 8606066656235469486L, 4914206519773203512L, 6884853324988375589L,
    7862730431637125620L, 3637067690497580296L, 6290184345309700496L, 2909654152398064237L,
    5032147476247760397L, 483048914547496228L, 8051435961996416635L, 2617552670646949126L,
    6441148769597133308L, 2094042136517559301L, 5152919015677706646L, 5364582523955957764L,
    8244670425084330634L, 4893983223587622099L, 6595736340067464507L, 5759860986241052841L,
    5276589072053971606L, 918539974250931950L, 8442542515286354569L, 7003687180914356604L,
    6754034012229083655L, 7447624152102440445L, 5403227209783266924L, 5958099321681952356L,
    8645163535653227079L, 3998935692578258285L, 6916130828522581663L, 5043822961433561789L,
    5532904662818065330L, 7724407183888759755L, 8852647460508904529L, 3135679457367239799L,
    7082117968407123623L, 4353217973264747001L, 5665694374725698898L, 7171923193353707924L,
    9065110999561118238L, 407030665140201709L, 7252088799648894590L, 4014973346854071690L,
    5801671039719115672L, 3211978677483257352L, 4641336831775292537L, 8103606164099471367L,
    7426138930840468060L, 5587072233075333540L, 5940911144672374448L, 4469657786460266832L,
    4752728915737899558L, 7265075043910123789L, 7604366265180639294L, 556073626030467093L,
    6083493012144511435L, 2289533308195328836L, 4866794409715609148L, 1831626646556263069L,
    7786871055544974637L, 1085928227119065748L, 6229496844435979709L, 6402765803808118083L,
    4983597475548783767L, 6966887050417449628L, 7973755960878054028L, 3768321651184098759L,
    6379004768702443222L, 6704006135689189330L, 5103203814961954578L, 1673856093809441141L,
    8165126103939127325L, 833495342724150664L, 6532100883151301860L, 666796274179320531L,
    5225680706521041488L, 533437019343456425L, 8361089130433666380L, 8232196860433350926L,
    6688871304346933104L, 6585757488346680741L, 5351097043477546483L, 7113280398048299755L,
    8561755269564074374L, 313202192651548637L, 6849404215651259499L, 2095236161492194072L,
    5479523372521007599L, 3520863336564710419L, 8767237396033612159L, 99358116390671185L,
    7013789916826889727L, 1924160900483492110L, 5611031933461511781L, 7073351942499659173L,
    8977651093538418850L, 7628014293257544353L, 7182120874830735080L, 6102411434606035483L,
    5745696699864588064L, 4881929147684828386L, 9193114719783340903L, 2277063414182859933L,
    7354491775826672722L, 5510999546088198270L, 5883593420661338178L, 719450822128648293L,
    4706874736529070542L, 4264909472444828957L, 7530999578446512867L, 8668529563282681493L,
    6024799662757210294L, 3245474835884234871L, 4819839730205768235L, 4441054276078343059L,
    7711743568329229176L, 7105686841725348894L, 6169394854663383341L, 3839875066009323953L,
    4935515883730706673L, 1227225645436504001L, 7896825413969130677L, 118886625327451240L,
    6317460331175304541L, 5629132522374826477L, 5053968264940243633L, 2658631610528906020L,
    8086349223904389813L, 2409136169475294470L, 6469079379123511850L, 5616657750322145900L,
    5175263503298809480L, 4493326200257716720L, 8280421605278095168L, 7189321920412346751L,
    6624337284222476135L, 217434314217011916L, 5299469827377980908L, 173947451373609533L,
    8479151723804769452L, 7657013551681595899L, 6783321379043815562L, 2436262026603366396L,
    5426657103235052449L, 7483032843395558602L, 8682651365176083919L, 6438829327320028278L,
    6946121092140867135L, 6995737869226977784L, 5556896873712693708L, 5596590295381582227L,
    8891034997940309933L, 7109870065239576402L, 7112827998352247947L, 153872830078795637L,
    5690262398681798357L, 5657121486175901994L, 9104419837890877372L, 1672696748397622544L,
    7283535870312701897L, 6872180620830963520L, 5826828696250161518L, 1808395681922860493L,
    4661462957000129214L, 5136065360280198718L, 7458340731200206743L, 2683681354335452463L,
    5966672584960165394L, 5836293898210272294L, 4773338067968132315L, 6513709525939172997L,
    7637340908749011705L, 1198563204647900987L, 6109872726999209364L, 958850563718320789L,
    4887898181599367491L, 2611754858345611793L, 7820637090558987986L, 489458958611068546L,
    6256509672447190388L, 7770264796372675483L, 5005207737957752311L, 682188614985274902L,
    8008332380732403697L, 6625525006089305327L, 6406665904585922958L, 1611071190129533939L,
    5125332723668738366L, 4978205766845537474L, 8200532357869981386L, 4275780412210949635L,
    6560425886295985109L, 1575949922397804547L, 5248340709036788087L, 3105434345289198799L,
    8397345134458860939L, 6813369359833673240L, 6717876107567088751L, 7295369895237893754L,
    5374300886053671001L, 3991621508819359841L, 8598881417685873602L, 2697245599369065423L,
    6879105134148698881L, 7691819701608117823L, 5503284107318959105L, 4308781353915539097L,
    8805254571710334568L, 6894050166264862555L, 7044203657368267654L, 9204588947753800367L,
    5635362925894614123L, 9208345565573995455L, 9016580681431382598L, 3665306460692661759L,
    7213264545145106078L, 6621593983296039730L, 5770611636116084862L, 8986624001378742108L,
    4616489308892867890L, 3499950386361083363L, 7386382894228588624L, 5599920618177733380L,
    5909106315382870899L, 6324610901913141866L, 4727285052306296719L, 6904363128901468655L,
    7563656083690074751L, 5512957784129484362L, 6050924866952059801L, 2565691819932632328L,
    4840739893561647841L, 207879048575150701L, 7745183829698636545L, 5866629699833106606L,
    6196147063758909236L, 4693303759866485285L, 4956917651007127389L, 1909968600522233067L,
    7931068241611403822L, 6745298575577483229L, 6344854593289123058L, 1706890045720076260L,
    5075883674631298446L, 5054860851317971332L, 8121413879410077514L, 4398428547366843807L,
    6497131103528062011L, 5363417245264430207L, 5197704882822449609L, 2446059388840589004L,
    8316327812515919374L, 7603043836886852730L, 6653062250012735499L, 7927109476880437346L,
    5322449800010188399L, 8186361988875305038L, 8515919680016301439L, 7564155960087622576L,
    6812735744013041151L, 7895999175441053223L, 5450188595210432921L, 4472124932981887417L,
    8720301752336692674L, 3466051078029109543L, 6976241401869354139L, 4617515269794242796L,
    5580993121495483311L, 5538686623206349399L, 8929588994392773298L, 5172549782388248714L,
    7143671195514218638L, 7827388640652509295L, 5714936956411374911L, 727887690409141951L,
    9143899130258199857L, 6698643526767492606L, 7315119304206559886L, 1669566006672083762L,
    5852095443365247908L, 8714350434821487656L, 4681676354692198327L, 1437457125744324640L,
    7490682167507517323L, 4144605808561874585L, 5992545734006013858L, 7005033461591409992L,
    4794036587204811087L, 70003547160262509L, 7670458539527697739L, 1956680082827375175L,
    6136366831622158191L, 3410018473632855302L, 4909093465297726553L, 883340371535329080L,
    7854549544476362484L, 8792042223940347174L, 6283639635581089987L, 8878308186523232901L,
    5026911708464871990L, 3413297734476675998L, 8043058733543795184L, 5461276375162681596L,
    6434446986835036147L, 6213695507501100438L, 5147557589468028918L, 1281607591258970028L,
    8236092143148846269L, 205897738643396882L, 6588873714519077015L, 2009392598285672668L,
    5271098971615261612L, 1607514078628538134L, 8433758354584418579L, 4416696933176616176L,
    6747006683667534863L, 5378031953912248102L, 5397605346934027890L, 7991774377871708805L,
    8636168555094444625L, 3563466967739958280L, 6908934844075555700L, 2850773574191966624L,
    5527147875260444560L, 2280618859353573299L, 8843436600416711296L, 3648990174965717279L,
    7074749280333369037L, 1074517732601618662L, 5659799424266695229L, 6393637408194160414L,
    9055679078826712367L, 4695796630997791177L, 7244543263061369894L, 67288490056322619L,
    5795634610449095915L, 1898505199416013257L, 4636507688359276732L, 1518804159532810606L,
    7418412301374842771L, 4274761062623452130L, 5934729841099874217L, 1575134442727806543L,
    4747783872879899373L, 6794130776295110719L, 7596454196607838997L, 9025934834701221989L,
    6077163357286271198L, 3531399053019067268L, 4861730685829016958L, 6514468057157164137L,
    7778769097326427133L, 8578474484080507458L, 6223015277861141707L, 1328756365151540482L,
    4978412222288913365L, 6597028314234097870L, 7965459555662261385L, 1331873265919780784L,
    6372367644529809108L, 1065498612735824627L, 5097894115623847286L, 4541747704930570025L,
    8156630584998155658L, 3577447513147001717L, 6525304467998524526L, 6551306825259511697L,
    5220243574398819621L, 3396371052836654196L, 8352389719038111394L, 1744844869796736390L,
    6681911775230489115L, 3240550303208344274L, 5345529420184391292L, 2592440242566675419L,
    8552847072295026067L, 5992578795477635832L, 6842277657836020854L, 1104714221640198342L,
    5473822126268816683L, 2728445784683113836L, 8758115402030106693L, 2520838848122026975L,
    7006492321624085354L, 5706019893239531903L, 5605193857299268283L, 6409490321962580684L,
    8968310171678829253L, 8410510107769173933L, 7174648137343063403L, 1194384864102473662L,
    5739718509874450722L, 4644856706023889253L, 9183549615799121156L, 53073100154402158L,
    7346839692639296924L, 7421156109607342373L, 5877471754111437539L, 7781599295056829060L,
    4701977403289150031L, 8069953843416418410L, 7523163845262640050L, 9222577334724359132L,
    6018531076210112040L, 7378061867779487306L, 4814824860968089632L, 5902449494223589845L,
    7703719777548943412L, 2065221561273923105L, 6162975822039154729L, 7186200471132003969L,
    4930380657631323783L, 7593634784276558337L, 7888609052210118054L, 1081769210616762369L,
    6310887241768094443L, 2710089775864365057L, 5048709793414475554L, 5857420635433402369L,
    8077935669463160887L, 3837849794580578305L, 6462348535570528709L, 8604303057777328129L,
    5169878828456422967L, 8728116853592817665L, 8271806125530276748L, 6586289336264687617L,
    6617444900424221398L, 8958380283753660417L, 5293955920339377119L, 1632681004890062849L,
    8470329472543003390L, 6301638422566010881L, 6776263578034402712L, 5041310738052808705L,
    5421010862427522170L, 343699775700336641L, 8673617379884035472L, 549919641120538625L,
    6938893903907228377L, 5973958935009296385L, 5551115123125782702L, 1089818333265526785L,
    8881784197001252323L, 3588383740595798017L, 7105427357601001858L, 6560055807218548737L,
    5684341886080801486L, 8937393460516749313L, 9094947017729282379L, 1387108685230112769L,
    7275957614183425903L, 2954361355555045377L, 5820766091346740722L, 6052837899185946625L,
    4656612873077392578L, 1152921504606846977L, 7450580596923828125L, 1L,
    5960464477539062500L, 1L, 4768371582031250000L, 1L,
    7629394531250000000L, 1L, 6103515625000000000L, 1L,
    4882812500000000000L, 1L, 7812500000000000000L, 1L,
    6250000000000000000L, 1L, 5000000000000000000L, 1L,
    8000000000000000000L, 1L, 6400000000000000000L, 1L,
    5120000000000000000L, 1L, 8192000000000000000L, 1L,
    6553600000000000000L, 1L, 5242880000000000000L, 1L,
    8388608000000000000L, 1L, 6710886400000000000L, 1L,
    5368709120000000000L, 1L, 8589934592000000000L, 1L,
    6871947673600000000L, 1L, 5497558138880000000L, 1L,
    8796093022208000000L, 1L, 7036874417766400000L, 1L,
    5629499534213120000L, 1L, 9007199254740992000L, 1L,
    7205759403792793600L, 1L, 5764607523034234880L, 1L,
    4611686018427387904L, 1L, 7378697629483820646L, 3689348814741910324L,
    5902958103587056517L, 1106804644422573097L, 4722366482869645213L, 6419466937650923963L,
    7555786372591432341L, 8426472692870523179L, 6044629098073145873L, 4896503746925463381L,
    4835703278458516698L, 7606551812282281028L, 7737125245533626718L, 1102436455425918676L,
    6189700196426901374L, 4571297979082645264L, 4951760157141521099L, 5501712790637071373L,
    7922816251426433759L, 3268717242906448711L, 6338253001141147007L, 4459648201696114131L,
    5070602400912917605L, 9101741783469756789L, 8112963841460668169L, 5339414816696835055L,
    6490371073168534535L, 6116206260728423206L, 5192296858534827628L, 4892965008582738565L,
    8307674973655724205L, 5984069606361426541L, 6646139978924579364L, 4787255685089141233L,
    5316911983139663491L, 5674478955442268148L, 8507059173023461586L, 5389817513965718714L,
    6805647338418769269L, 2467179603801619810L, 5444517870735015415L, 3818418090412251009L,
    8711228593176024664L, 6109468944659601615L, 6968982874540819731L, 6732249563098636453L,
    5575186299632655785L, 3541125243107954001L, 8920298079412249256L, 5665800388972726402L,
    7136238463529799405L, 2687965903807225960L, 5708990770823839524L, 2150372723045780768L,
    9134385233318143238L, 7129945171615159552L, 7307508186654514591L, 169932915179262157L,
    5846006549323611672L, 7514643961627230372L, 4676805239458889338L, 2322366354559873974L,
    7482888383134222941L, 1871111759924843197L, 5986310706507378352L, 8875587037423695204L,
    4789048565205902682L, 3411120815197045840L, 7662477704329444291L, 7302467711686228506L,
    6129982163463555433L, 3997299761978027643L, 4903985730770844346L, 6887188624324332438L,
    7846377169233350954L, 7330152984177021577L, 6277101735386680763L, 7708796794712572423L,
    5021681388309344611L, 633014213657192454L, 8034690221294951377L, 6546845963964373411L,
    6427752177035961102L, 1548127956429588405L, 5142201741628768881L, 6772525587256536209L,
    8227522786606030210L, 7146692124868547611L, 6582018229284824168L, 5717353699894838089L,
    5265614583427859334L, 8263231774657780795L, 8424983333484574935L, 7687147617339583786L,
    6739986666787659948L, 6149718093871667029L, 5391989333430127958L, 8609123289839243947L,
    8627182933488204734L, 2706550819517059345L, 6901746346790563787L, 4009915062984602637L,
    5521397077432451029L, 8741955272500547595L, 8834235323891921647L, 8453105213888010667L,
    7067388259113537318L, 3073135356368498210L, 5653910607290829854L, 6147857099836708891L,
    9046256971665327767L, 4302548137625868741L, 7237005577332262213L, 8976061732213560478L,
    5789604461865809771L, 1646826163657982898L, 4631683569492647816L, 8696158560410206965L,
    7410693711188236507L, 1001132845059645012L, 5928554968950589205L, 6334929498160581494L,
    4742843975160471364L, 5067943598528465196L, 7588550360256754183L, 2574686535532678828L,
    6070840288205403346L, 5749098043168053386L, 4856672230564322677L, 2754604027163487547L,
    7770675568902916283L, 6252040850832535236L, 6216540455122333026L, 8690981495407938512L,
    4973232364097866421L, 5108110788955395648L, 7957171782556586274L, 4483628447586722714L,
    6365737426045269019L, 5431577165440333333L, 5092589940836215215L, 6189936139723221828L,
    8148143905337944345L, 680525786702379117L, 6518515124270355476L, 544420629361903293L,
    5214812099416284380L, 7814234132973343281L, 8343699359066055009L, 3279402575902573442L,
    6674959487252844007L, 4468196468093013915L, 5339967589802275205L, 9108580396587276617L,
    8543948143683640329L, 5350356597684866779L, 6835158514946912263L, 6124959685518848585L,
    5468126811957529810L, 8589316563156989191L, 8749002899132047697L, 4519534464196406897L,
    6999202319305638157L, 9149650793469991003L, 5599361855444510526L, 3630371820034082479L,
    8958978968711216842L, 2119246097312621643L, 7167183174968973473L, 7229420099962962799L,
    5733746539975178779L, 249512857857504755L, 9173994463960286046L, 4088569387313917931L,
    7339195571168228837L, 1426181102480179183L, 5871356456934583069L, 6674968104097008831L,
    4697085165547666455L, 7184648890648562227L, 7515336264876266329L, 2272066188182923754L,
    6012269011901013063L, 3662327357917294165L, 4809815209520810450L, 6619210701075745655L,
    7695704335233296721L, 1367365084866417240L, 6156563468186637376L, 8472589697376954439L,
    4925250774549309901L, 4933397350530608390L, 7880401239278895842L, 4204086946107063100L,
    6304320991423116673L, 8897292778998515965L, 5043456793138493339L, 1583811001085947287L,
    8069530869021589342L, 6223446416479425982L, 6455624695217271474L, 1289408318441630463L,
    5164499756173817179L, 2876201062124259532L, 8263199609878107486L, 8291270514140725574L,
    6610559687902485989L, 4788342003941625298L, 5288447750321988791L, 5675348010524255400L,
    8461516400515182066L, 5391208002096898316L, 6769213120412145653L, 2468291994306563491L,
    5415370496329716522L, 5663982410187161116L, 8664592794127546436L, 1683674226815637140L,
    6931674235302037148L, 8725637010936330358L, 5545339388241629719L, 1446486386636198802L,
    8872543021186607550L, 6003727033359828406L, 7098034416949286040L, 4802981626687862725L,
    5678427533559428832L, 3842385301350290180L, 9085484053695086131L, 7992490889531419449L,
    7268387242956068905L, 4549318304254180398L, 5814709794364855124L, 3639454643403344318L,
    4651767835491884099L, 4756238122093630616L, 7442828536787014559L, 2075957773236943501L,
    5954262829429611647L, 3505440625960509963L, 4763410263543689317L, 8338375722881273455L,
    7621456421669902908L, 5962703527126216881L, 6097165137335922326L, 8459511636442883828L,
    4877732109868737861L, 4922934901783351901L, 7804371375789980578L, 4187347028111452718L,
    6243497100631984462L, 7039226437231072498L, 4994797680505587570L, 1942032335042947675L,
    7991676288808940112L, 3107251736068716280L, 6393341031047152089L, 8019824610967838509L,
    5114672824837721671L, 8260534096145225969L, 8183476519740354675L, 304133702235675419L,
    6546781215792283740L, 243306961788540335L, 5237424972633826992L, 194645569430832268L,
    8379879956214123187L, 2156107318460286790L, 6703903964971298549L, 7258909076881094917L,
    5363123171977038839L, 7651801668875831096L, 8580997075163262143L, 6708859448088464268L,
    6864797660130609714L, 9056436373212681737L, 5491838128104487771L, 9089823505941100552L,
    8786941004967180435L, 1630996757909074751L, 7029552803973744348L, 1304797406327259801L,
    5623642243178995478L, 4733186739803718164L, 8997827589086392765L, 5728424376314993901L,
    7198262071269114212L, 4582739501051995121L, 5758609657015291369L, 9200214822954461581L,
    9213775451224466191L, 9186320494614273045L, 7371020360979572953L, 5504381988320463275L,
    5896816288783658362L, 8092854405398280943L, 4717453031026926690L, 2784934709576714431L,
    7547924849643082704L, 4455895535322743090L, 6038339879714466163L, 5409390835629149634L,
    4830671903771572930L, 8016861483245230030L, 7729075046034516689L, 3603606336337592240L,
    6183260036827613351L, 4727559476441028954L, 4946608029462090681L, 1937373173781868001L,
    7914572847139345089L, 8633820300163854287L, 6331658277711476071L, 8751730647502038591L,
    5065326622169180857L, 5156710110630675711L, 8104522595470689372L, 872038547525260492L,
    6483618076376551497L, 6231654060133073878L, 5186894461101241198L, 1295974433364548779L,
    8299031137761985917L, 228884686012322885L, 6639224910209588733L, 5717130970922723793L,
    5311379928167670986L, 8263053591480089358L, 8498207885068273579L, 308164894771456841L,
    6798566308054618863L, 2091206323188120634L, 5438853046443695090L, 5362313873292406831L,
    8702164874309912144L, 8579702197267850929L, 6961731899447929715L, 8708436165185235905L,
    5569385519558343772L, 6966748932148188724L, 8911016831293350036L, 3768100661953281312L,
    7128813465034680029L, 1169806122191669888L, 5703050772027744023L, 2780519305124291072L,
    9124881235244390437L, 2604156480827910553L, 7299904988195512349L, 7617348406775193928L,
    5839923990556409879L, 7938553132791110304L, 4671939192445127903L, 8195516913603843405L,
    7475102707912204646L, 2044780617540418478L, 5980082166329763716L, 9014522123516155429L,
    4784065733063810973L, 5366943291441969181L, 7654505172902097557L, 6742434858936195528L,
    6123604138321678046L, 1704599072407046100L, 4898883310657342436L, 8742376887409457526L,
    7838213297051747899L, 1075082168258445910L, 6270570637641398319L, 2704740141977711890L,
    5016456510113118655L, 4008466520953124674L, 8026330416180989848L, 6413546433524999478L,
    6421064332944791878L, 8820185961561909905L, 5136851466355833503L, 1522125547136662440L,
    8218962346169333605L, 590726468047704741L, 6575169876935466884L, 472581174438163793L,
    5260135901548373507L, 2222739346921486196L, 8416217442477397611L, 5401057362445333075L,
    6732973953981918089L, 2476171482585311299L, 5386379163185534471L, 3825611593439204201L,
    8618206661096855154L, 2431629734760816398L, 6894565328877484123L, 3789978195179608280L,
    5515652263101987298L, 6721331370885596947L, 8825043620963179677L, 8909455786045999954L,
    7060034896770543742L, 3438215814094889640L, 5648027917416434993L, 8284595873388777197L,
    9036844667866295990L, 2187306953196312545L, 7229475734293036792L, 1749845562557050036L,
    5783580587434429433L, 6933899672158505514L, 4626864469947543547L, 13096515613938926L,
    7402983151916069675L, 1865628832353257443L, 5922386521532855740L, 1492503065882605955L,
    4737909217226284592L, 1194002452706084764L, 7580654747562055347L, 3755078331700690783L,
    6064523798049644277L, 8538085887473418112L, 4851619038439715422L, 3141119895236824166L,
    7762590461503544675L, 6870466239749873827L, 6210072369202835740L, 5496372991799899062L,
    4968057895362268592L, 4397098393439919250L, 7948892632579629747L, 8880031836874825961L,
    6359114106063703798L, 3414676654757950445L, 5087291284850963038L, 6421090138548270680L,
    8139666055761540861L, 8429069814306277926L, 6511732844609232689L, 4898581444074067179L,
    5209386275687386151L, 5763539562630208905L, 8335018041099817842L, 5532314485466423924L,
    6668014432879854274L, 736502773631228816L, 5334411546303883419L, 2433876626275938215L,
    8535058474086213470L, 7583551416783411467L, 6828046779268970776L, 6066841133426729173L,
    5462437423415176621L, 3008798499370428177L, 8739899877464282594L, 1124728784250774760L,
    6991919901971426075L, 2744457434771574970L, 5593535921577140860L, 2195565947817259976L,
    8949657474523425376L, 3512905516507615961L, 7159725979618740301L, 965650005835137607L,
    5727780783694992240L, 8151217634151930732L, 9164449253911987585L, 3818576177788313364L,
    7331559403129590068L, 3054860942230650691L, 5865247522503672054L, 6133237568526430876L,
    4692198018002937643L, 6751264462192099863L, 7507516828804700229L, 8957348732136404618L,
    6006013463043760183L, 9010553393080078856L, 4804810770435008147L, 1674419492351197600L,
    7687697232696013035L, 4523745595132871322L, 6150157786156810428L, 3618996476106297057L,
    4920126228925448342L, 6584545995626947969L, 7872201966280717348L, 3156575963519296104L,
    6297761573024573878L, 6214609585557347207L, 5038209258419659102L, 8661036483187788089L,
    8061134813471454564L, 6478960743616640295L, 6448907850777163651L, 7027843002264267398L,
    5159126280621730921L, 3777599994440458757L, 8254602048994769474L, 2354811176362823687L,
    6603681639195815579L, 3728523348461214111L, 5282945311356652463L, 4827493086139926451L,
    8452712498170643941L, 5879314530452927160L, 6762169998536515153L, 2858777216991386566L,
    5409735998829212122L, 5976370588335019576L, 8655577598126739396L, 2183495311852210675L,
    6924462078501391516L, 9125493878965589187L, 5539569662801113213L, 5455720695801516188L,
    8863311460481781141L, 6884478705911470739L, 7090649168385424913L, 3662908557358221429L,
    5672519334708339930L, 6619675660628487467L, 9076030935533343889L, 1368109020150804139L,
    7260824748426675111L, 2939161623491598473L, 5808659798741340089L, 506654891422323617L,
    4646927838993072071L, 2249998320508814055L, 7435084542388915313L, 9134020534926967972L,
    5948067633911132251L, 1773193205828708893L, 4758454107128905800L, 8797252194146787761L,
    7613526571406249281L, 4852231473780084609L, 6090821257124999425L, 2037110771653112526L,
    4872657005699999540L, 1629688617322490021L, 7796251209119999264L, 2607501787715984033L,
    6237000967295999411L, 3930675837543742388L, 4989600773836799529L, 1299866262664038749L,
    7983361238138879246L, 5769134835004372321L, 6386688990511103397L, 2770633460632542696L,
    5109351192408882717L, 7750529990618899641L, 8174961907854212348L, 5022150355506418780L,
    6539969526283369878L, 7707069099147045347L, 5231975621026695903L, 631632057204770793L,
    8371160993642713444L, 8389308921011453915L, 6696928794914170755L, 8556121544180118293L,
    5357543035931336604L, 6844897235344094635L, 8572068857490138567L, 5417812354437685931L,
    6857655085992110854L, 644901068808238421L, 5486124068793688683L, 2360595262417545899L,
    8777798510069901893L, 1932278012497118276L, 7022238808055921514L, 5235171224739604944L,
    5617791046444737211L, 6032811387162639117L, 8988465674311579538L, 5963149404718312264L,
    7190772539449263630L, 8459868338516560134L, 5752618031559410904L, 6767894670813248108L,
    9204188850495057447L, 5294608251188331487L
  )
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