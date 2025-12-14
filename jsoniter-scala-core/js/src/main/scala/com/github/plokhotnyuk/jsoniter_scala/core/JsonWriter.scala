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
 * A writer for iterative serialization of JSON keys and values.
 *
 * @param buf an internal buffer for writing JSON data
 * @param count the current position in the internal buffer
 * @param limit the last position in the internal buffer
 * @param indention the current indention level
 * @param comma a flag indicating if the next element should be preceded by comma
 * @param disableBufGrowing a flag indicating if growing of the internal buffer is disabled
 * @param bbuf a byte buffer for writing JSON data
 * @param out the output stream for writing JSON data
 * @param config a writer configuration
 */
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
  /**
   * Writes a `Boolean` value as a JSON key.
   *
   * @param x the `Boolean` value to write
   */
  def writeKey(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBoolean(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Byte` value as a JSON key.
   *
   * @param x the `Byte` value to write
   */
  def writeKey(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeByte(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Char` value as a JSON key.
   *
   * @param x the `Char` value to write
   * @throws JsonWriterException in case of `Char` value is a part of surrogate pair
   */
  def writeKey(x: Char): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeChar(x.toInt)
    writeColon()
  }

  /**
   * Writes a `Short` value as a JSON key.
   *
   * @param x the `Short` value to write
   */
  def writeKey(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeShort(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Int` value as a JSON key.
   *
   * @param x the `Int` value to write
   */
  def writeKey(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeInt(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Long` value as a JSON key.
   *
   * @param x the `Long` value to write
   */
  def writeKey(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeLong(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Float` value as a JSON key.
   *
   * @param x the `Float` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeKey(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeFloat(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `Double` value as a JSON key.
   *
   * @param x the `Double` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeKey(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeDouble(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a timestamp value as a JSON key.
   *
   * @param epochSecond the epoch second of the timestamp to write
   * @param nano the nanoseconds of the timestamp to write
   * @throws JsonWriterException if the nanoseconds value is less than 0 or greater than 999999999
   */
  def writeTimestampKey(epochSecond: Long, nano: Int): Unit = {
    if (nano < 0 || nano > 999999999) encodeError("illegal nanoseconds value: " + nano)
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    var pos = ensureBufCapacity(30)
    val buf = this.buf
    var es = epochSecond
    var ns = nano
    if (es < 0 & ns > 0) {
      es += 1
      ns = 1000000000 - ns
      if (es == 0) {
        buf(pos) = '-'
        pos += 1
      }
    }
    pos = writeLong(es, pos, buf)
    if (ns != 0) {
      val dotPos = pos
      pos = writeSignificantFractionDigits(ns, pos + 9, pos, buf, digits)
      buf(dotPos) = '.'
    }
    this.count = pos
    writeParenthesesWithColon()
  }

  /**
   * Writes a `BigInt` value as a JSON key.
   *
   * @param x the `BigInt` value to write
   */
  def writeKey(x: BigInt): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    if (x.isValidLong) {
      val l = x.longValue
      val i = l.toInt
      if (i == l) writeInt(i)
      else writeLong(l)
    } else writeBigInteger(x.bigInteger, null)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `BigDecimal` value as a JSON key.
   *
   * @param x the `BigDecimal` value to write
   */
  def writeKey(x: BigDecimal): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeParenthesesWithColon()
  }

  /**
   * Writes a [[java.util.UUID]] value as a JSON key.
   *
   * @param x the [[java.util.UUID]] value to write
   */
  def writeKey(x: UUID): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
    writeColon()
  }

  /**
   * Writes a `String` value as a JSON key.
   *
   * @param x the `String` value to write
   * @throws JsonWriterException if the provided string has an illegal surrogate pair
   */
  def writeKey(x: String): Unit = {
    if (x eq null) throw new NullPointerException
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
    pos = writeString(x, 0, pos, Math.min(x.length, limit - pos - 1) + pos, escapedChars)
    if (pos + 3 >= limit) pos = flushAndGrowBuf(3, pos)
    buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = ':'
    pos += 2
    if (indention > 0) {
      buf(pos) = ' '
      pos += 1
    }
    count = pos
  }

  /**
   * Writes a `String` value that doesn't require encoding or escaping as a JSON key.
   *
   * @note Use [[JsonWriter.isNonEscapedAscii]] for validation if the string is eligable for writing by this method.
   *
   * @param x the `String` value to write
   */
  def writeNonEscapedAsciiKey(x: String): Unit = {
    if (x eq null) throw new NullPointerException
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
      if (indention > 0) {
        buf(pos) = ' '
        pos += 1
      }
      count = pos
    } else writeLongNonEscapedAsciiKey(x)
  }

  /**
   * Writes a [[java.time.Duration]] value as a JSON key.
   *
   * @param x the [[java.time.Duration]] value to write
   */
  def writeKey(x: Duration): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeDuration(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.Duration]] value as a JSON key.
   *
   * @param x the [[java.time.Duration]] value to write
   */
  def writeKey(x: Instant): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeInstant(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.LocalDate]] value as a JSON key.
   *
   * @param x the [[java.time.LocalDate]] value to write
   */
  def writeKey(x: LocalDate): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDate(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.LocalDateTime]] value as a JSON key.
   *
   * @param x the [[java.time.LocalDateTime]] value to write
   */
  def writeKey(x: LocalDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalDateTime(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.LocalTime]] value as a JSON key.
   *
   * @param x the [[java.time.LocalTime]] value to write
   */
  def writeKey(x: LocalTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeLocalTime(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.MonthDay]] value as a JSON key.
   *
   * @param x the [[java.time.MonthDay]] value to write
   */
  def writeKey(x: MonthDay): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeMonthDay(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.OffsetDateTime]] value as a JSON key.
   *
   * @param x the [[java.time.OffsetDateTime]] value to write
   */
  def writeKey(x: OffsetDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetDateTime(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.OffsetTime]] value as a JSON key.
   *
   * @param x the [[java.time.OffsetTime]] value to write
   */
  def writeKey(x: OffsetTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeOffsetTime(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.Period]] value as a JSON key.
   *
   * @param x the [[java.time.Period]] value to write
   */
  def writeKey(x: Period): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writePeriod(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.Year]] value as a JSON key.
   *
   * @param x the [[java.time.Year]] value to write
   */
  def writeKey(x: Year): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeYear(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.YearMonth]] value as a JSON key.
   *
   * @param x the [[java.time.YearMonth]] value to write
   */
  def writeKey(x: YearMonth): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeYearMonth(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.ZonedDateTime]] value as a JSON key.
   *
   * @param x the [[java.time.ZonedDateTime]] value to write
   */
  def writeKey(x: ZonedDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZonedDateTime(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.ZoneId]] value as a JSON key.
   *
   * @param x the [[java.time.ZoneId]] value to write
   */
  def writeKey(x: ZoneId): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZoneId(x)
    writeColon()
  }

  /**
   * Writes a [[java.time.ZoneOffset]] value as a JSON key.
   *
   * @param x the [[java.time.ZoneOffset]] value to write
   */
  def writeKey(x: ZoneOffset): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeKey()
    writeZoneOffset(x)
    writeColon()
  }

  /**
   * Throws a [[JsonWriterException]] with the given error message.
   *
   * @param msg the error message
   * @throws JsonWriterException always
   */
  def encodeError(msg: String): Nothing =
    throw new JsonWriterException(msg, null, config.throwWriterExceptionWithStackTrace)

  /**
   * Writes a `BigDecimal` value as a JSON value.
   *
   * @param x the `BigDecimal` value to write
   */
  def writeVal(x: BigDecimal): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigDecimal(x.bigDecimal)
  }

  /**
   * Writes a `BigInt` value as a JSON value.
   *
   * @param x the `BigInt` value to write
   */
  def writeVal(x: BigInt): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    if (x.isValidLong) {
      val l = x.longValue
      val i = l.toInt
      if (i == l) writeInt(i)
      else writeLong(l)
    } else writeBigInteger(x.bigInteger, null)
  }

  /**
   * Writes a [[java.util.UUID]] value as a JSON value.
   *
   * @param x the [[java.util.UUID]] value to write
   */
  def writeVal(x: UUID): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeUUID(x.getMostSignificantBits, x.getLeastSignificantBits)
  }

  /**
   * Writes a `String` value as a JSON value.
   *
   * @param x the `String` value to write
   * @throws JsonWriterException if the provided string has an illegal surrogate pair
   */
  def writeVal(x: String): Unit = {
    if (x eq null) throw new NullPointerException
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 4)
    val buf = this.buf
    if (comma) {
      buf(pos) = ','
      pos += 1
      if (indention != 0) pos = writeIndention(buf, pos, indention)
    } else comma = true
    buf(pos) = '"'
    pos += 1
    pos = writeString(x, 0, pos, Math.min(x.length, limit - pos - 1) + pos, escapedChars)
    this.buf(pos) = '"'
    count = pos + 1
  }

  /**
   * Writes a `String` value that doesn't require encoding or escaping as a JSON value.
   *
   * @note Use [[JsonWriter.isNonEscapedAscii]] for validation if the string is eligable for writing by this method.
   *
   * @param x the `String` value to write
   */
  def writeNonEscapedAsciiVal(x: String): Unit = {
    if (x eq null) throw new NullPointerException
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

  /**
   * Writes a [[java.time.Duration]] value as a JSON value.
   *
   * @param x the [[java.time.Duration]] value to write
   */
  def writeVal(x: Duration): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeDuration(x)
  }

  /**
   * Writes a [[java.time.Instant]] value as a JSON value.
   *
   * @param x the [[java.time.Instant]] value to write
   */
  def writeVal(x: Instant): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeInstant(x)
  }

  /**
   * Writes a [[java.time.LocalDate]] value as a JSON value.
   *
   * @param x the [[java.time.LocalDate]] value to write
   */
  def writeVal(x: LocalDate): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDate(x)
  }

  /**
   * Writes a [[java.time.LocalDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.LocalDateTime]] value to write
   */
  def writeVal(x: LocalDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDateTime(x)
  }

  /**
   * Writes a [[java.time.LocalTime]] value as a JSON value.
   *
   * @param x the [[java.time.LocalTime]] value to write
   */
  def writeVal(x: LocalTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalTime(x)
  }

  /**
   * Writes a [[java.time.MonthDay]] value as a JSON value.
   *
   * @param x the [[java.time.MonthDay]] value to write
   */
  def writeVal(x: MonthDay): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeMonthDay(x)
  }

  /**
   * Writes a [[java.time.OffsetDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.OffsetDateTime]] value to write
   */
  def writeVal(x: OffsetDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetDateTime(x)
  }

  /**
   * Writes a [[java.time.OffsetTime]] value as a JSON value.
   *
   * @param x the [[java.time.OffsetTime]] value to write
   */
  def writeVal(x: OffsetTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetTime(x)
  }

  /**
   * Writes a [[java.time.Period]] value as a JSON value.
   *
   * @param x the [[java.time.Period]] value to write
   */
  def writeVal(x: Period): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writePeriod(x)
  }

  /**
   * Writes a [[java.time.Year]] value as a JSON value.
   *
   * @param x the [[java.time.Year]] value to write
   */
  def writeVal(x: Year): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeYear(x)
  }

  /**
   * Writes a [[java.time.YearMonth]] value as a JSON value.
   *
   * @param x the [[java.time.YearMonth]] value to write
   */
  def writeVal(x: YearMonth): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeYearMonth(x)
  }

  /**
   * Writes a [[java.time.ZonedDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.ZonedDateTime]] value to write
   */
  def writeVal(x: ZonedDateTime): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeZonedDateTime(x)
  }

  /**
   * Writes a [[java.time.ZoneId]] value as a JSON value.
   *
   * @param x the [[java.time.ZoneId]] value to write
   */
  def writeVal(x: ZoneId): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeZoneId(x)
  }

  /**
   * Writes a [[java.time.ZoneOffset]] value as a JSON value.
   *
   * @param x the [[java.time.ZoneOffset]] value to write
   */
  def writeVal(x: ZoneOffset): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeZoneOffset(x)
  }

  /**
   * Writes a `Boolean` value as a JSON value.
   *
   * @param x the `Boolean` value to write
   */
  def writeVal(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBoolean(x)
  }

  /**
   * Writes a `Byte` value as a JSON value.
   *
   * @param x the `Byte` value to write
   */
  def writeVal(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeByte(x)
  }

  /**
   * Writes a `Short` value as a JSON value.
   *
   * @param x the `Short` value to write
   */
  def writeVal(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeShort(x)
  }

  /**
   * Writes a `Char` value as a JSON key.
   *
   * @param x the `Char` value to write
   * @throws JsonWriterException in case of `Char` value is a part of surrogate pair
   */
  def writeVal(x: Char): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeChar(x.toInt)
  }

  /**
   * Writes a `Int` value as a JSON value.
   *
   * @param x the `Int` value to write
   */
  def writeVal(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeInt(x)
  }

  /**
   * Writes a `Long` value as a JSON value.
   *
   * @param x the `Long` value to write
   */
  def writeVal(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLong(x)
  }

  /**
   * Writes a `Float` value as a JSON value.
   *
   * @param x the `Float` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeVal(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeFloat(x)
  }

  /**
   * Writes a `Double` value as a JSON value.
   *
   * @param x the `Double` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeVal(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeDouble(x)
  }

  /**
   * Writes a timestamp value as a JSON value.
   *
   * @param epochSecond the epoch second of the timestamp to write
   * @param nano the nanoseconds of the timestamp to write
   * @throws JsonWriterException if the nanoseconds value is less than 0 or greater than 999999999
   */
  def writeTimestampVal(epochSecond: Long, nano: Int): Unit = {
    if (nano < 0 || nano > 999999999) encodeError("illegal nanoseconds value: " + nano)
    writeOptionalCommaAndIndentionBeforeValue()
    var pos = ensureBufCapacity(30)
    val buf = this.buf
    var es = epochSecond
    var ns = nano
    if (es < 0 & ns > 0) {
      es += 1
      ns = 1000000000 - ns
      if (es == 0) {
        buf(pos) = '-'
        pos += 1
      }
    }
    pos = writeLong(es, pos, buf)
    if (ns != 0) {
      val dotPos = pos
      pos = writeSignificantFractionDigits(ns, pos + 9, pos, buf, digits)
      buf(dotPos) = '.'
    }
    this.count = pos
  }

  /**
   * Writes a `BigDecimal` value as a JSON string value.
   *
   * @param x the `BigDecimal` value to write
   */
  def writeValAsString(x: BigDecimal): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBigDecimal(x.bigDecimal)
    writeBytes('"')
  }

  /**
   * Writes a `BigInt` value as a JSON string value.
   *
   * @param x the `BigInt` value to write
   */
  def writeValAsString(x: BigInt): Unit = {
    if (x eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    if (x.isValidLong) {
      val l = x.longValue
      val i = l.toInt
      if (i == l) writeInt(i)
      else writeLong(l)
    } else writeBigInteger(x.bigInteger, null)
    writeBytes('"')
  }

  /**
   * Writes a `Boolean` value as a JSON string value.
   *
   * @param x the `Boolean` value to write
   */
  def writeValAsString(x: Boolean): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeBoolean(x)
    writeBytes('"')
  }

  /**
   * Writes a `Byte` value as a JSON string value.
   *
   * @param x the `Byte` value to write
   */
  def writeValAsString(x: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeByte(x)
    writeBytes('"')
  }

  /**
   * Writes a `Short` value as a JSON string value.
   *
   * @param x the `Short` value to write
   */
  def writeValAsString(x: Short): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeShort(x)
    writeBytes('"')
  }

  /**
   * Writes a `Int` value as a JSON string value.
   *
   * @param x the `Int` value to write
   */
  def writeValAsString(x: Int): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeInt(x)
    writeBytes('"')
  }

  /**
   * Writes a `Long` value as a JSON string value.
   *
   * @param x the `Long` value to write
   */
  def writeValAsString(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeLong(x)
    writeBytes('"')
  }

  /**
   * Writes a `Float` value as a JSON string value.
   *
   * @param x the `Float` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeValAsString(x: Float): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeFloat(x)
    writeBytes('"')
  }

  /**
   * Writes a `Double` value as a JSON string value.
   *
   * @param x the `Double` value to write
   * @throws JsonWriterException if the value is non-finite
   */
  def writeValAsString(x: Double): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeDouble(x)
    writeBytes('"')
  }

  /**
   * Writes a timestamp value as a JSON string value.
   *
   * @param epochSecond the epoch second of the timestamp to write
   * @param nano the nanoseconds of the timestamp to write
   * @throws JsonWriterException if the nanoseconds value is less than 0 or greater than 999999999
   */
  def writeTimestampValAsString(epochSecond: Long, nano: Int): Unit = {
    if (nano < 0 || nano > 999999999) encodeError("illegal nanoseconds value: " + nano)
    writeOptionalCommaAndIndentionBeforeValue()
    var pos = ensureBufCapacity(32)
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    var es = epochSecond
    var ns = nano
    if (es < 0 & ns > 0) {
      es += 1
      ns = 1000000000 - ns
      if (es == 0) {
        buf(pos) = '-'
        pos += 1
      }
    }
    pos = writeLong(es, pos, buf)
    if (ns != 0) {
      val dotPos = pos
      pos = writeSignificantFractionDigits(ns, pos + 9, pos, buf, digits)
      buf(dotPos) = '.'
    }
    buf(pos) = '"'
    pos += 1
    this.count = pos
  }

  /**
   * Writes a byte array as a JSON hexadecimal string value.
   *
   * @param bs the byte array to write
   * @param lowerCase if `true`, outputs lowercase hexadecimal digits
   */
  def writeBase16Val(bs: Array[Byte], lowerCase: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    val ds =
      if (lowerCase) lowerCaseHexDigits
      else upperCaseHexDigits
    writeBase16Bytes(bs, ds)
  }

  /**
   * Writes a byte array as a JSON string value encoded in a base-64 format.
   *
   * @param bs the byte array to write
   * @param doPadding if `true`, outputs padding characters (`=`) as needed
   */
  def writeBase64Val(bs: Array[Byte], doPadding: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64Digits, doPadding)
  }

  /**
   * Writes a byte array as a JSON string value encoded in a base-64 format for URLs.
   *
   * @param bs the byte array to write
   * @param doPadding if `true`, outputs padding characters (`=`) as needed
   */
  def writeBase64UrlVal(bs: Array[Byte], doPadding: Boolean): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64UrlDigits, doPadding)
  }

  /**
   * Writes a byte array as a JSON raw binary value.
   *
   * @param bs the byte array to write
   */
  def writeRawVal(bs: Array[Byte]): Unit = {
    if (bs eq null) throw new NullPointerException
    writeOptionalCommaAndIndentionBeforeValue()
    writeRawBytes(bs)
  }

  /**
   * Writes a JSON `null` value.
   */
  def writeNull(): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    val pos = ensureBufCapacity(4)
    val buf = this.buf
    buf(pos) = 'n'
    buf(pos + 1) = 'u'
    buf(pos + 2) = 'l'
    buf(pos + 3) = 'l'
    count = pos + 4
  }

  /**
   * Writes a JSON array start marker (`[`).
   */
  def writeArrayStart(): Unit = writeNestedStart('[')

  /**
   * Writes a JSON array end marker (`]`).
   */
  def writeArrayEnd(): Unit = writeNestedEnd(']')

  /**
   * Writes a JSON array start marker (`{`).
   */
  def writeObjectStart(): Unit = writeNestedStart('{')

  /**
   * Writes a JSON array end marker (`}`).
   */
  def writeObjectEnd(): Unit = writeNestedEnd('}')

  /**
   * Writes JSON-encoded value of type `A` to an output stream.
   *
   * @param codec a JSON value codec for type `A`
   * @param x the value to encode
   * @param out the output stream to write to
   * @param config the writer configuration
   */
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

  /**
   * Encodes a value of type `A` to a byte array.
   *
   * @param codec a JSON value codec for type `A`
   * @param x the value to encode
   * @param config the writer configuration
   * @return the encoded JSON as a byte array
   */
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

  /**
   * Encodes a value of type `A` to a string.
   *
   * @param codec a JSON value codec for type `A`
   * @param x the value to encode
   * @param config the writer configuration
   * @return the encoded JSON as a string
   */
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

  /**
   * Encodes a value of type `A` to a string without buffer reallocation.
   *
   * @note Use only once with a newly allocated writer, so buffer reallocation is not required.
   *
   * @param codec a JSON value codec for type `A`
   * @param x the value to encode
   * @param config the writer configuration
   * @return the encoded JSON as a string
   */
  private[jsoniter_scala] def writeToStringWithoutBufReallocation[@sp A](codec: JsonValueCodec[A], x: A, config: WriterConfig): String = {
    this.config = config
    count = 0
    indention = 0
    comma = false
    disableBufGrowing = false
    codec.encodeValue(x, this)
    new String(buf, 0, count, StandardCharsets.UTF_8)
  }

  /**
   * Encodes a value of type `A` into a pre-allocated byte array slice.
   *
   * @param codec a JSON value codec for type `A`
   * @param x the value to encode
   * @param buf the target byte array
   * @param from the start index of the target slice (inclusive)
   * @param to the end index of the target slice (exclusive)
   * @param config the writer configuration
   * @return the number of bytes written to the target slice
   */
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
    } finally {
      setBuf(currBuf)
    }
    count
  }

  /**
   * Encodes a value of type `A` into a byte buffer.
   *
   * @param codec JSON value codec for type `A`
   * @param x the value to encode
   * @param bbuf the target byte buffer
   * @param config the writer configuration
   */
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

  @inline
  private[this] def writeNestedStart(b: Byte): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes(b)
    val indentionStep = config.indentionStep
    if (indentionStep != 0) {
      indention += indentionStep
      writeIndention()
    }
  }

  @inline
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

  private[this] def writeIndention(): Unit = {
    val n = indention
    val pos = ensureBufCapacity(n + 1)
    count = writeIndention(buf, pos, n)
  }

  private[this] def writeIndention(buf: Array[Byte], p: Int, n: Int): Int = {
    var pos = p
    buf(pos) = '\n'
    pos += 1
    val posLim = pos + n
    while (pos < posLim) {
      buf(pos) = ' '
      pos += 1
    }
    pos
  }

  private[this] def writeParenthesesWithColon(): Unit = {
    var pos = ensureBufCapacity(3)
    val buf = this.buf
    buf(pos) = '"'
    buf(pos + 1) = ':'
    pos += 2
    if (indention > 0) {
      buf(pos) = ' '
      pos += 1
    }
    count = pos
  }

  private[this] def writeColon(): Unit = {
    var pos = ensureBufCapacity(2)
    val buf = this.buf
    buf(pos) = ':'
    pos += 1
    if (indention > 0) {
      buf(pos) = ' '
      pos += 1
    }
    count = pos
  }

  private[this] def writeBytes(b: Byte): Unit = {
    var pos = count
    if (pos >= limit) pos = flushAndGrowBuf(1, pos)
    buf(pos) = b
    count = pos + 1
  }

  private[this] def writeBase16Bytes(bs: Array[Byte], ds: Array[Short]): Unit = {
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
      val offsetLim = Math.min((posLim - pos + 1 >> 1) + offset, lenM1)
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
      pos += 2
    }
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeBase64Bytes(bs: Array[Byte], ds: Array[Byte], doPadding: Boolean): Unit = {
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
      val offsetLim = Math.min((posLim - pos + 3 >> 2) * 3 + offset, lenM2)
      while (offset < offsetLim) {
        val p = (bs(offset) & 0xFF) << 16 | (bs(offset + 1) & 0xFF) << 8 | (bs(offset + 2) & 0xFF)
        buf(pos) = ds(p >> 18)
        buf(pos + 1) = ds(p >> 12 & 0x3F)
        buf(pos + 2) = ds(p >> 6 & 0x3F)
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
      buf(pos + 1) = ds(p >> 6 & 0x3F)
      buf(pos + 2) = ds(p & 0x3F)
      pos += 3
      if (doPadding) {
        buf(pos) = '='
        pos += 1
      }
    } else if (offset == lenM2 + 1) {
      val p = bs(offset) & 0xFF
      buf(pos) = ds(p >> 2)
      buf(pos + 1) = ds(p << 4 & 0x3F)
      pos += 2
      if (doPadding) {
        buf(pos) = '='
        buf(pos + 1) = '='
        pos += 2
      }
    }
    buf(pos) = '"'
    count = pos + 1
  }

  @inline
  private[this] def writeRawBytes(bs: Array[Byte]): Unit = {
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
    count = pos
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
      while (offset < newOffset) {
        buf(pos) = x.charAt(offset).toByte
        pos += 1
        offset += 1
      }
      remaining -= step
    }
    count = pos
    writeParenthesesWithColon()
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
      while (offset < newOffset) {
        buf(pos) = x.charAt(offset).toByte
        pos += 1
        offset += 1
      }
      remaining -= step
    }
    count = pos
    writeBytes('"')
  }

  private[this] def writeZoneId(x: ZoneId): Unit = {
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
    count = pos + 1
  }

  private[this] def writeUUID(mostSigBits: Long, leastSigBits: Long): Unit = {
    val pos = ensureBufCapacity(38) // 38 == (new java.util.UUID(0, 0)).toString.length + 2
    val buf = this.buf
    val ds = lowerCaseHexDigits
    val mostSigBits1 = (mostSigBits >>> 32).toInt
    buf(pos) = '"'
    val d1 = ds(mostSigBits1 >>> 24)
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d1 >> 8).toByte
    val d2 = ds(mostSigBits1 >> 16 & 0xFF)
    buf(pos + 3) = d2.toByte
    buf(pos + 4) = (d2 >> 8).toByte
    val d3 = ds(mostSigBits1 >> 8 & 0xFF)
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
    val d6 = ds(mostSigBits2 >> 16 & 0xFF)
    buf(pos + 12) = d6.toByte
    buf(pos + 13) = (d6 >> 8).toByte
    buf(pos + 14) = '-'
    val d7 = ds(mostSigBits2 >> 8 & 0xFF)
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
    val d10 = ds(leastSigBits1 >> 16 & 0xFF)
    buf(pos + 22) = d10.toByte
    buf(pos + 23) = (d10 >> 8).toByte
    buf(pos + 24) = '-'
    val d11 = ds(leastSigBits1 >> 8 & 0xFF)
    buf(pos + 25) = d11.toByte
    buf(pos + 26) = (d11 >> 8).toByte
    val d12 = ds(leastSigBits1 & 0xFF)
    buf(pos + 27) = d12.toByte
    buf(pos + 28) = (d12 >> 8).toByte
    val leastSigBits2 = leastSigBits.toInt
    val d13 = ds(leastSigBits2 >>> 24)
    buf(pos + 29) = d13.toByte
    buf(pos + 30) = (d13 >> 8).toByte
    val d14 = ds(leastSigBits2 >> 16 & 0xFF)
    buf(pos + 31) = d14.toByte
    buf(pos + 32) = (d14 >> 8).toByte
    val d15 = ds(leastSigBits2 >> 8 & 0xFF)
    buf(pos + 33) = d15.toByte
    buf(pos + 34) = (d15 >> 8).toByte
    val d16 = ds(leastSigBits2 & 0xFF)
    buf(pos + 35) = d16.toByte
    buf(pos + 36) = (d16 >> 8).toByte
    buf(pos + 37) = '"'
    count = pos + 38
  }

  @tailrec
  private[this] def writeString(s: String, from: Int, pos: Int, minLim: Int, escapedChars: Array[Byte]): Int =
    if (pos < minLim) {
      val ch = s.charAt(from)
      buf(pos) = ch.toByte
      if (ch >= 0x80 || escapedChars(ch.toInt) != 0) writeEscapedOrEncodedString(s, from, pos, escapedChars)
      else writeString(s, from + 1, pos + 1, minLim, escapedChars)
    } else {
      val remaining = s.length - from
      if (remaining > 0) {
        val newPos = flushAndGrowBuf(2, pos)
        writeString(s, from, newPos, Math.min(remaining, limit - newPos - 1) + newPos, escapedChars)
      } else pos
    }

  private[this] def writeEscapedOrEncodedString(s: String, from: Int, pos: Int, escapedChars: Array[Byte]): Int =
    if (config.escapeUnicode) writeEscapedString(s, from, s.length, pos, limit - 13, escapedChars, lowerCaseHexDigits)
    else writeEncodedString(s, from, s.length, pos, limit - 7, escapedChars)

  @tailrec
  private[this] def writeEncodedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEncodedString(s, from, to, flushAndGrowBuf(7, pos), limit - 6, escapedChars)
    else {
      val ch1 = s.charAt(from).toInt
      if (ch1 < 0x80) {
        val esc = escapedChars(ch1)
        if (esc == 0) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
          buf(pos) = ch1.toByte
          writeEncodedString(s, from + 1, to, pos + 1, posLim, escapedChars)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
        } else writeEncodedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos, buf, lowerCaseHexDigits), posLim, escapedChars)
      } else if (ch1 < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (ch1 >> 6 | 0xC0).toByte
        buf(pos + 1) = (ch1 & 0x3F | 0x80).toByte
        writeEncodedString(s, from + 1, to, pos + 2, posLim, escapedChars)
      } else if ((ch1 & 0xF800) != 0xD800) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
        buf(pos) = (ch1 >> 12 | 0xE0).toByte
        buf(pos + 1) = (ch1 >> 6 & 0x3F | 0x80).toByte
        buf(pos + 2) = (ch1 & 0x3F | 0x80).toByte
        writeEncodedString(s, from + 1, to, pos + 3, posLim, escapedChars)
      } else { // 110110uuuuccccbb 110111bbbbaaaaaa (UTF-16 chars) -> 11110ddd 10ddcccc 10bbbbbb 10aaaaaa (UTF-8 bytes), where ddddd = uuuu + 1
        var ch2 = 0
        if (ch1 >= 0xDC00 || from + 1 >= to || {
          ch2 = s.charAt(from + 1).toInt
          (ch2 & 0xFC00) != 0xDC00
        }) illegalSurrogateError()
        val cp = (ch1 << 10) + (ch2 - 56613888) // -56613888 == 0x10000 - (0xD800 << 10) - 0xDC00
        buf(pos) = (cp >> 18 | 0xF0).toByte
        buf(pos + 1) = (cp >> 12 & 0x3F | 0x80).toByte
        buf(pos + 2) = (cp >> 6 & 0x3F | 0x80).toByte
        buf(pos + 3) = (cp & 0x3F | 0x80).toByte
        writeEncodedString(s, from + 2, to, pos + 4, posLim, escapedChars)
      }
    }

  @tailrec
  private[this] def writeEscapedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte], ds: Array[Short]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEscapedString(s, from, to, flushAndGrowBuf(13, pos), limit - 12, escapedChars, ds)
    else {
      val ch1 = s.charAt(from).toInt
      if (ch1 < 0x80) {
        val esc = escapedChars(ch1)
        if (esc == 0) {
          buf(pos) = ch1.toByte
          writeEscapedString(s, from + 1, to, pos + 1, posLim, escapedChars, ds)
        } else if (esc > 0) {
          buf(pos) = '\\'
          buf(pos + 1) = esc
          writeEscapedString(s, from + 1, to, pos + 2, posLim, escapedChars, ds)
        } else writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1.toByte, pos, buf, ds), posLim, escapedChars, ds)
      } else if ((ch1 & 0xF800) != 0xD800) {
        writeEscapedString(s, from + 1, to, writeEscapedUnicode(ch1, pos, buf, ds), posLim, escapedChars, ds)
      } else {
        var ch2 = 0
        if (ch1 >= 0xDC00 || from + 1 >= to || {
          ch2 = s.charAt(from + 1).toInt
          (ch2 & 0xFC00) != 0xDC00
        }) illegalSurrogateError()
        writeEscapedString(s, from + 2, to, writeEscapedUnicode(ch2, writeEscapedUnicode(ch1, pos, buf, ds), buf, ds), posLim, escapedChars, ds)
      }
    }

  private[this] def writeChar(ch: Int): Unit = {
    var pos = ensureBufCapacity(8) // 6 bytes per char for escaped unicode + make room for the quotes
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    if (ch < 0x80) {
      val esc = escapedChars(ch)
      if (esc == 0) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
        buf(pos) = ch.toByte
        pos += 1
      } else if (esc > 0) {
        buf(pos) = '\\'
        buf(pos + 1) = esc
        pos += 2
      } else pos = writeEscapedUnicode(ch.toByte, pos, buf, lowerCaseHexDigits)
    } else if (config.escapeUnicode) {
      if ((ch & 0xF800) == 0xD800) illegalSurrogateError()
      pos = writeEscapedUnicode(ch, pos, buf, lowerCaseHexDigits)
    } else if (ch < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
      buf(pos) = (ch >> 6 | 0xC0).toByte
      buf(pos + 1) = (ch & 0x3F | 0x80).toByte
      pos += 2
    } else if ((ch & 0xF800) != 0xD800) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
      buf(pos) = (ch >> 12 | 0xE0).toByte
      buf(pos + 1) = (ch >> 6 & 0x3F | 0x80).toByte
      buf(pos + 2) = (ch & 0x3F | 0x80).toByte
      pos += 3
    } else illegalSurrogateError()
    buf(pos) = '"'
    count = pos + 1
  }

  @inline
  private[this] def writeEscapedUnicode(ch: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    val d1 = ds(ch >> 8)
    buf(pos + 2) = d1.toByte
    buf(pos + 3) = (d1 >> 8).toByte
    val d2 = ds(ch & 0xFF)
    buf(pos + 4) = d2.toByte
    buf(pos + 5) = (d2 >> 8).toByte
    pos + 6
  }

  @inline
  private[this] def writeEscapedUnicode(b: Byte, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    buf(pos) = '\\'
    buf(pos + 1) = 'u'
    buf(pos + 2) = '0'
    buf(pos + 3) = '0'
    val d = ds(b & 0xFF)
    buf(pos + 4) = d.toByte
    buf(pos + 5) = (d >> 8).toByte
    pos + 6
  }

  private[this] def illegalSurrogateError(): Nothing = encodeError("illegal char sequence of surrogate pair")

  private[this] def writeBigInteger(x: BigInteger, ss: Array[BigInteger]): Unit = {
    val bitLen = x.bitLength
    if (bitLen < 64) writeLong(x.longValue)
    else {
      val n = calculateTenPow18SquareNumber(bitLen)
      val ss1 =
        if (ss eq null) getTenPow18Squares(n)
        else ss
      val qr = x.divideAndRemainder(ss1(n))
      writeBigInteger(qr(0), ss1)
      writeBigIntegerRemainder(qr(1), n - 1, ss1)
    }
  }

  private[this] def writeBigIntegerRemainder(x: BigInteger, n: Int, ss: Array[BigInteger]): Unit =
    if (n < 0) count = write18Digits(Math.abs(x.longValue), ensureBufCapacity(18), buf, digits)
    else {
      val qr = x.divideAndRemainder(ss(n))
      writeBigIntegerRemainder(qr(0), n - 1, ss)
      writeBigIntegerRemainder(qr(1), n - 1, ss)
    }

  private[this] def writeBigDecimal(x: java.math.BigDecimal): Unit = {
    var exp = writeBigDecimal(x.unscaledValue, x.scale, 0, null)
    if (exp != 0) {
      var pos = ensureBufCapacity(12)
      val buf = this.buf
      val ds = digits
      buf(pos) = 'E'
      var sb: Byte = '+'
      if (exp < 0) {
        sb = '-'
        exp = -exp
      }
      buf(pos + 1) = sb
      pos += 2
      var q = exp.toInt
      if (exp == q) {
        pos += digitCount(q)
        count = pos
      } else {
        q = (exp * 1e-8).toInt // divide a small positive long by 100000000
        pos += ((9 - q) >>> 31) + 1
        count = write8Digits((exp - q * 100000000L).toInt, pos, buf, ds)
      }
      writePositiveIntDigits(q, pos, buf, ds)
    }
  }

  private[this] def writeBigDecimal(x: BigInteger, scale: Int, blockScale: Int, ss: Array[BigInteger]): Long = {
    val bitLen = x.bitLength
    if (bitLen < 64) {
      val v = x.longValue
      val pos = ensureBufCapacity(28) // Long.MinValue.toString.length + 8 (for a leading zero, dot, and padding zeroes)
      val buf = this.buf
      var lastPos = writeLong(v, pos, buf)
      var digits = lastPos - pos
      if (v < 0) digits -= 1
      val dotOff = scale.toLong - blockScale
      val exp = (digits - 1) - dotOff
      if (scale >= 0 && exp >= -6) {
        if (exp < 0) lastPos = insertDotWithZeroes(digits, -1 - exp.toInt, lastPos, buf)
        else if (dotOff > 0) lastPos = insertDot(lastPos - dotOff.toInt, lastPos, buf)
        count = lastPos
        0
      } else {
        if (digits > 1 || blockScale > 0) lastPos = insertDot(lastPos - digits + 1, lastPos, buf)
        count = lastPos
        exp
      }
    } else {
      val n = calculateTenPow18SquareNumber(bitLen)
      val ss1 =
        if (ss eq null) getTenPow18Squares(n)
        else ss
      val qr = x.divideAndRemainder(ss1(n))
      val exp = writeBigDecimal(qr(0), scale, (18 << n) + blockScale, ss1)
      writeBigDecimalRemainder(qr(1), scale, blockScale, n - 1, ss1)
      exp
    }
  }

  private[this] def writeBigDecimalRemainder(x: BigInteger, scale: Int, blockScale: Int, n: Int,
                                             ss: Array[BigInteger]): Unit =
    if (n < 0) {
      val pos = ensureBufCapacity(19) // 18 digits and a place for optional dot
      val buf = this.buf
      var lastPos = write18Digits(Math.abs(x.longValue), pos, buf, digits)
      val dotOff = scale - blockScale
      if (dotOff > 0 && dotOff <= 18) lastPos = insertDot(lastPos - dotOff, lastPos, buf)
      count = lastPos
    } else {
      val qr = x.divideAndRemainder(ss(n))
      writeBigDecimalRemainder(qr(0), scale, (18 << n) + blockScale, n - 1, ss)
      writeBigDecimalRemainder(qr(1), scale, blockScale, n - 1, ss)
    }

  @inline
  private[this] def calculateTenPow18SquareNumber(bitLen: Int): Int = {
    val m = Math.max((bitLen * 0.016723888647998956).toInt - 1, 1) // Math.max((x.bitLength * Math.log(2) / Math.log(1e18)).toInt - 1, 1)
    31 - java.lang.Integer.numberOfLeadingZeros(m)
  }

  private[this] def insertDotWithZeroes(digits: Int, pad: Int, lastPos: Int, buf: Array[Byte]): Int = {
    var pos = lastPos + pad + 1
    val numPos = pos - digits
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
    lastPos + off
  }

  private[this] def insertDot(dotPos: Int, lastPos: Int, buf: Array[Byte]): Int = {
    var pos = lastPos
    while (pos > dotPos) {
      buf(pos) = buf(pos - 1)
      pos -= 1
    }
    buf(dotPos) = '.'
    lastPos + 1
  }

  private[this] def writeBoolean(x: Boolean): Unit = {
    var pos = ensureBufCapacity(5) // false.toString.length
    val buf = this.buf
    if (x) {
      buf(pos) = 't'
      buf(pos + 1) = 'r'
      buf(pos + 2) = 'u'
      buf(pos + 3) = 'e'
      pos += 4
    } else {
      buf(pos) = 'f'
      buf(pos + 1) = 'a'
      buf(pos + 2) = 'l'
      buf(pos + 3) = 's'
      buf(pos + 4) = 'e'
      pos += 5
    }
    count = pos
  }

  private[this] def writeByte(x: Byte): Unit = {
    var pos = ensureBufCapacity(4) // Byte.MinValue.toString.length
    val buf = this.buf
    val ds = digits
    var q0 = x.toInt
    if (q0 < 0) {
      q0 = -q0
      buf(pos) = '-'
      pos += 1
    }
    if (q0 < 10) {
      buf(pos) = (q0 | '0').toByte
      pos += 1
    } else {
      if (q0 >= 100) {
        q0 -= 100
        buf(pos) = '1'
        pos += 1
      }
      val d = ds(q0)
      buf(pos) = d.toByte
      buf(pos + 1) = (d >> 8).toByte
      pos += 2
    }
    count = pos
  }

  private[this] def writeDuration(x: Duration): Unit = {
    var pos = ensureBufCapacity(40) // 40 == "PT-1111111111111111H-11M-11.111111111S".length + 2
    val buf = this.buf
    var totalSecs = x.getSeconds
    var nano = x.getNano
    buf(pos) = '"'
    buf(pos + 1) = 'P'
    buf(pos + 2) = 'T'
    pos += 3
    if (totalSecs == 0 && nano == 0) {
      buf(pos) = '0'
      buf(pos + 1) = 'S'
      pos += 2
    } else {
      val isNeg = totalSecs < 0
      if (isNeg) totalSecs = (-nano >> 31) - totalSecs
      var hours = 0L
      var secsOfHour = totalSecs.toInt
      if (totalSecs >= 3600) {
        hours =
          if (totalSecs >= 4503599627370496L) totalSecs / 3600
          else (totalSecs * 2.777777777777778E-4).toLong
        secsOfHour = (totalSecs - (hours << 12) + (hours << 9) - (hours << 4)).toInt // (totalSecs - hours * 3600).toInt
      }
      val minutes = secsOfHour * 17477 >> 20 // divide a small positive int by 60
      val seconds = secsOfHour - minutes * 60
      val ds = digits
      if (hours != 0) {
        if (isNeg) {
          buf(pos) = '-'
          pos += 1
        }
        var q = hours.toInt
        var lastPos = pos
        if (hours == q) {
          lastPos += digitCount(q)
          pos = lastPos
        } else {
          q = (hours * 1e-8).toInt // divide a medium positive long by 100000000
          lastPos += digitCount(q)
          pos = write8Digits((hours - q * 100000000L).toInt, lastPos, buf, ds)
        }
        writePositiveIntDigits(q, lastPos, buf, ds)
        buf(pos) = 'H'
        pos += 1
      }
      if (minutes != 0) {
        if (isNeg) {
          buf(pos) = '-'
          pos += 1
        }
        if (minutes < 10) {
          buf(pos) = (minutes | '0').toByte
          pos += 1
        } else {
          val d = ds(minutes)
          buf(pos) = d.toByte
          buf(pos + 1) = (d >> 8).toByte
          pos += 2
        }
        buf(pos) = 'M'
        pos += 1
      }
      if ((seconds | nano) != 0) {
        if (isNeg) {
          buf(pos) = '-'
          pos += 1
        }
        if (seconds < 10) {
          buf(pos) = (seconds | '0').toByte
          pos += 1
        } else {
          val d = ds(seconds)
          buf(pos) = d.toByte
          buf(pos + 1) = (d >> 8).toByte
          pos += 2
        }
        if (nano != 0) {
          if (isNeg) nano = 1000000000 - nano
          val dotPos = pos
          pos = writeSignificantFractionDigits(nano, pos + 9, pos, buf, ds)
          buf(dotPos) = '.'
        }
        buf(pos) = 'S'
        pos += 1
      }
    }
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeInstant(x: Instant): Unit = {
    val epochSecond = x.getEpochSecond
    var year, adjust400YearCycles, marchDayOfYear, secsOfDay = 0
    if (epochSecond > -316224000000L && epochSecond < 316224000000L) { // the fast path from -10000 to 10000 years
      val epochDay =
        ((if (epochSecond >= 0) epochSecond
        else epochSecond - 86399) * 1.1574074074074073E-5).toInt
      var marchZeroDay = epochDay + 719468  // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
      if (marchZeroDay < 0) {
        adjust400YearCycles = ((marchZeroDay + 1) * 6.844767517471269E-6).toInt - 1
        marchZeroDay -= adjust400YearCycles * 146097
      }
      year = ((marchZeroDay * 400 + 591) * 6.844767517471269E-6).toInt
      var days = year * 365
      var century = year / 100
      marchDayOfYear = marchZeroDay - days - (year >> 2) + century - (century >> 2)
      if (marchDayOfYear < 0) {
        days -= 365
        year -= 1
        century = year / 100
        marchDayOfYear = marchZeroDay - days - (year >> 2) + century - (century >> 2)
      }
      secsOfDay = (epochSecond - epochDay * 86400).toInt
    } else {
      val epochDay =
        (if (epochSecond >= 0) epochSecond
        else epochSecond - 86399L) / 86400L
      var marchZeroDay = epochDay + 719468L  // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
      if (marchZeroDay < 0) {
        adjust400YearCycles = ((marchZeroDay + 1L) / 146097L).toInt - 1
        marchZeroDay -= adjust400YearCycles * 146097L
      }
      year = ((marchZeroDay * 400L + 591L) / 146097L).toInt
      var days = year * 365L
      var century = year / 100
      marchDayOfYear = (marchZeroDay - days).toInt - (year >> 2) + century - (century >> 2)
      if (marchDayOfYear < 0) {
        days -= 365L
        year -= 1
        century = year / 100
        marchDayOfYear = (marchZeroDay - days).toInt - (year >> 2) + century - (century >> 2)
      }
      secsOfDay = (epochSecond - epochDay * 86400L).toInt
    }
    val marchMonth = marchDayOfYear * 17135 + 6854 >> 19 // (marchDayOfYear * 5 + 2) / 153
    val day = marchDayOfYear - (marchMonth * 1002762 - 16383 >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
    val m = 9 - marchMonth >> 4
    val month = (m & -9 | 3) + marchMonth
    year += adjust400YearCycles * 400 - m
    writeInstant(year, month, day, secsOfDay, x.getNano)
  }

  @inline
  private[this] def writeInstant(year: Int, month: Int, day: Int, secsOfDay: Int, nano: Int): Unit = {
    var pos = ensureBufCapacity(39) // 39 == Instant.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeYear(year, pos + 1, buf, ds)
    buf(pos) = '-'
    val d1 = ds(month)
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d1 >> 8).toByte
    buf(pos + 3) = '-'
    val d2 = ds(day)
    buf(pos + 4) = d2.toByte
    buf(pos + 5) = (d2 >> 8).toByte
    buf(pos + 6) = 'T'
    var y = secsOfDay * 37283 // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val d3 = ds(y >>> 27)
    buf(pos + 7) = d3.toByte
    buf(pos + 8) = (d3 >> 8).toByte
    buf(pos + 9) = ':'
    y &= 0x7FFFFFF
    y *= 15
    val d4 = ds(y >> 25)
    buf(pos + 10) = d4.toByte
    buf(pos + 11) = (d4 >> 8).toByte
    buf(pos + 12) = ':'
    val d5 = ds((y & 0x1FFFFFF) * 15 >> 23)
    buf(pos + 13) = d5.toByte
    buf(pos + 14) = (d5 >> 8).toByte
    pos += 15
    if (nano != 0) pos = writeNanos(nano, pos, buf, ds)
    buf(pos) = 'Z'
    buf(pos + 1) = '"'
    count = pos + 2
  }

  private[this] def writeLocalDate(x: LocalDate): Unit = {
    var pos = ensureBufCapacity(18) // 18 == LocalDate.MAX.toString.length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeLocalDate(x, pos + 1, buf, digits)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeLocalDateTime(x: LocalDateTime): Unit = {
    var pos = ensureBufCapacity(37) // 37 == LocalDateTime.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeLocalTime(x.toLocalTime, pos + 1, buf, ds)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeLocalTime(x: LocalTime): Unit = {
    var pos = ensureBufCapacity(20) // 20 == LocalTime.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalTime(x, pos + 1, buf, ds)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeMonthDay(x: MonthDay): Unit = {
    val pos = ensureBufCapacity(9) // 9 == "--01-01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    buf(pos + 1) = '-'
    buf(pos + 2) = '-'
    val d1 = ds(x.getMonthValue)
    buf(pos + 3) = d1.toByte
    buf(pos + 4) = (d1 >> 8).toByte
    buf(pos + 5) = '-'
    val d2 = ds(x.getDayOfMonth)
    buf(pos + 6) = d2.toByte
    buf(pos + 7) = (d2 >> 8).toByte
    buf(pos + 8) = '"'
    count = pos + 9
  }

  private[this] def writeOffsetDateTime(x: OffsetDateTime): Unit = {
    var pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalDate(x.toLocalDate, pos + 1, buf, ds)
    buf(pos) = 'T'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeOffsetTime(x: OffsetTime): Unit = {
    var pos = ensureBufCapacity(29) // 29 == "00:00:07.999999998+00:00:08".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writePeriod(x: Period): Unit = {
    var pos = ensureBufCapacity(39) // 39 == "P-2147483648Y-2147483648M-2147483648D".length + 2
    val buf = this.buf
    val years = x.getYears
    val months = x.getMonths
    val days = x.getDays
    buf(pos) = '"'
    buf(pos + 1) = 'P'
    pos += 2
    if ((years | months | days) == 0) {
      buf(pos) = '0'
      buf(pos + 1) = 'D'
      buf(pos + 2) = '"'
      count = pos + 3
    } else {
      val ds = digits
      var q0 = years
      var b: Byte = 'Y'
      while (true) {
        if (q0 != 0) {
          if (q0 < 0) {
            q0 = -q0
            buf(pos) = '-'
            pos += 1
            if (q0 == -2147483648) {
              q0 = 147483648
              buf(pos) = '2'
              pos += 1
            }
          }
          pos += digitCount(q0)
          writePositiveIntDigits(q0, pos, buf, ds)
          buf(pos) = b
          pos += 1
        }
        if (b == 'Y') {
          q0 = months
          b = 'M'
        } else if (b == 'M') {
          q0 = days
          b = 'D'
        } else {
          buf(pos) = '"'
          count = pos + 1
          return
        }
      }
    }
  }

  private[this] def writeYear(x: Year): Unit = {
    var pos = ensureBufCapacity(12) // 12 == "+999999999".length + 2
    val buf = this.buf
    buf(pos) = '"'
    pos = writeYear(x.getValue, pos + 1, buf, digits)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeYearMonth(x: YearMonth): Unit = {
    var pos = ensureBufCapacity(15) // 15 == "+999999999-12".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeYear(x.getYear, pos + 1, buf, ds)
    buf(pos) = '-'
    val d = ds(x.getMonthValue)
    buf(pos + 1) = d.toByte
    buf(pos + 2) = (d >> 8).toByte
    buf(pos + 3) = '"'
    count = pos + 4
  }

  private[this] def writeZonedDateTime(x: ZonedDateTime): Unit = {
    var pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    var buf = this.buf
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
      val required = len + 3
      if (pos + required > limit) {
        pos = flushAndGrowBuf(required, pos)
        buf = this.buf
      }
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
    count = pos + 1
  }

  private[this] def writeZoneOffset(x: ZoneOffset): Unit = {
    var pos = ensureBufCapacity(12) // 12 == "+10:10:10".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeOffset(x, pos + 1, buf, ds)
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeLocalDate(x: LocalDate, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYear(x.getYear, p, buf, ds)
    buf(pos) = '-'
    val d1 = ds(x.getMonthValue)
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d1 >> 8).toByte
    buf(pos + 3) = '-'
    val d2 = ds(x.getDayOfMonth)
    buf(pos + 4) = d2.toByte
    buf(pos + 5) = (d2 >> 8).toByte
    pos + 6
  }

  @inline
  private[this] def writeYear(year: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int =
    if (year >= 0 && year < 10000) write4Digits(year, pos, buf, ds)
    else writeYearWithSign(year, pos, buf, ds)

  private[this] def writeYearWithSign(year: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = year
    var pos = p
    var b: Byte = '+'
    if (q0 < 0) {
      q0 = -q0
      b = '-'
    }
    buf(pos) = b
    pos += 1
    if (q0 < 10000) write4Digits(q0, pos, buf, ds)
    else {
      pos += digitCount(q0)
      writePositiveIntDigits(q0, pos, buf, ds)
      pos
    }
  }

  private[this] def writeLocalTime(x: LocalTime, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = p
    val d1 = ds(x.getHour)
    buf(pos) = d1.toByte
    buf(pos + 1) = (d1 >> 8).toByte
    buf(pos + 2) = ':'
    val d2 = ds(x.getMinute)
    buf(pos + 3) = d2.toByte
    buf(pos + 4) = (d2 >> 8).toByte
    pos += 5
    val second = x.getSecond
    val nano = x.getNano
    if ((second | nano) != 0) {
      buf(pos) = ':'
      val d3 = ds(second)
      buf(pos + 1) = d3.toByte
      buf(pos + 2) = (d3 >> 8).toByte
      pos += 3
      if (nano != 0) pos = writeNanos(nano, pos, buf, ds)
    }
    pos
  }

  private[this] def writeNanos(x: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var pos = p
    buf(pos) = '.'
    val q1 = x / 10000000
    val r1 = x - q1 * 10000000
    val d1 = ds(q1)
    buf(pos + 1) = d1.toByte
    buf(pos + 2) = (d1 >> 8).toByte
    val q2 = r1 / 100000
    val r2 = r1 - q2 * 100000
    val d2 = ds(q2)
    buf(pos +3) = d2.toByte
    pos += 4
    if (r2 != 0 || d2 > 0x3039) { // check if q0 is divisible by 1000000
      buf(pos) = (d2 >> 8).toByte
      val q3 = r2 / 1000
      val r3 = r2 - q3 * 1000
      val d3 = ds(q3)
      buf(pos + 1) = d3.toByte
      buf(pos + 2) = (d3 >> 8).toByte
      pos += 3
      if (r3 != 0) pos = write3Digits(r3, pos, buf, ds) // check if q0 is divisible by 1000
    }
    pos
  }

  private[this] def writeOffset(x: ZoneOffset, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var y = x.getTotalSeconds
    if (y == 0) {
      buf(pos) = 'Z'
      pos + 1
    } else {
      if (y > 0) buf(pos) = '+'
      else {
        y = -y
        buf(pos) = '-'
      }
      y *= 37283 // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      val d = ds(y >>> 27)
      buf(pos + 1) = d.toByte
      buf(pos + 2) = (d >> 8).toByte
      buf(pos + 3) = ':'
      if ((y & 0x7FF8000) == 0) { // check if totalSeconds is divisible by 3600
        buf(pos + 4) = '0'
        buf(pos + 5) = '0'
        pos + 6
      } else {
        y &= 0x7FFFFFF
        y *= 15
        val d = ds(y >> 25)
        buf(pos + 4) = d.toByte
        buf(pos + 5) = (d >> 8).toByte
        if ((y & 0x1F80000) == 0) pos + 6 // check if totalSeconds is divisible by 60
        else {
          buf(pos + 6) = ':'
          val d = ds((y & 0x1FFFFFF) * 15 >> 23)
          buf(pos + 7) = d.toByte
          buf(pos + 8) = (d >> 8).toByte
          pos + 9
        }
      }
    }
  }

  @inline
  private[this] def write3Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = x * 1311 >> 17 // divide a small positive int by 100
    buf(pos) = (q1 | '0').toByte
    val d = ds(x - q1 * 100)
    buf(pos + 1) = d.toByte
    buf(pos + 2) = (d >> 8).toByte
    pos + 3
  }

  @inline
  private[this] def write4Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = x * 5243 >> 19 // divide a small positive int by 100
    val d1 = ds(q1)
    buf(pos) = d1.toByte
    buf(pos + 1) = (d1 >> 8).toByte
    val d2 = ds(x - q1 * 100)
    buf(pos + 2) = d2.toByte
    buf(pos + 3) = (d2 >> 8).toByte
    pos + 4
  }

  @inline
  private[this] def write8Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = x / 10000
    val q2 = q1 * 5243 >> 19 // divide a small positive int by 100
    val d1 = ds(q2)
    buf(pos) = d1.toByte
    buf(pos + 1) = (d1 >> 8).toByte
    val d2 = ds(q1 - q2 * 100)
    buf(pos + 2) = d2.toByte
    buf(pos + 3) = (d2 >> 8).toByte
    val r1 = x - q1 * 10000
    val q3 = r1 * 5243 >> 19 // divide a small positive int by 100
    val d3 = ds(q3)
    buf(pos + 4) = d3.toByte
    buf(pos + 5) = (d3 >> 8).toByte
    val d4 = ds(r1 - q3 * 100)
    buf(pos + 6) = d4.toByte
    buf(pos + 7) = (d4 >> 8).toByte
    pos + 8
  }

  @inline
  private[this] def write18Digits(x: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = ((x >>> 8) * 2.56e-6).toLong  // divide a medium positive long by 100000000
    val q2 = (q1 >>> 8) * 1441151881L >>> 49 // divide a small positive long by 100000000
    val d = ds(q2.toInt)
    buf(pos) = d.toByte
    buf(pos + 1) = (d >> 8).toByte
    write8Digits((x - q1 * 100000000L).toInt, write8Digits((q1 - q2 * 100000000L).toInt, pos + 2, buf, ds), buf, ds)
  }

  private[this] def writeShort(x: Short): Unit = {
    var pos = ensureBufCapacity(6) // Short.MinValue.toString.length
    val buf = this.buf
    val ds = digits
    var q0 = x.toInt
    if (q0 < 0) {
      q0 = -q0
      buf(pos) = '-'
      pos += 1
    }
    if (q0 < 100) {
      if (q0 < 10) {
        buf(pos) = (q0 | '0').toByte
        pos += 1
      } else {
        val d = ds(q0)
        buf(pos) = d.toByte
        buf(pos + 1) = (d >> 8).toByte
        pos += 2
      }
    } else if (q0 < 10000) {
      if (q0 < 1000) pos = write3Digits(q0, pos, buf, ds)
      else pos = write4Digits(q0, pos, buf, ds)
    } else {
      val q1 = q0 * 53688 >> 29 // divide a small positive int by 10000
      buf(pos) = (q1 | '0').toByte
      pos = write4Digits(q0 - 10000 * q1, pos + 1, buf, ds)
    }
    count = pos
  }

  private[this] def writeInt(x: Int): Unit = {
    var pos = ensureBufCapacity(11) // Int.MinValue.toString.length
    val buf = this.buf
    val ds = digits
    var q0 = x
    if (x < 0) {
      q0 = -q0
      buf(pos) = '-'
      pos += 1
      if (q0 == x) {
        q0 = 147483648
        buf(pos) = '2'
        pos += 1
      }
    }
    pos += digitCount(q0)
    writePositiveIntDigits(q0, pos, buf, ds)
    count = pos
  }

  @inline
  private[this] def writeLong(x: Long): Unit =
    count = writeLong(x, ensureBufCapacity(20), buf) // Long.MinValue.toString.length

  private[this] def writeLong(x: Long, p: Int, buf: Array[Byte]): Int = {
    var pos = p
    val ds = digits
    var q0 = x
    if (x < 0) {
      q0 = -q0
      buf(pos) = '-'
      pos += 1
      if (q0 == x) {
        q0 = 3372036854775808L
        buf(pos) = '9'
        buf(pos + 1) = '2'
        buf(pos + 2) = '2'
        pos += 3
      }
    }
    var q = q0.toInt
    var lastPos = pos
    if (q0 == q) {
      lastPos += digitCount(q)
      pos = lastPos
    } else {
      var posCorr = 0
      if (q0 >= 1000000000000000000L) {
        var z = q0
        q0 = (q0 >>> 1) + (q0 >>> 2) // Based upon the divu10() code from Hacker's Delight 2nd Edition by Henry Warren
        q0 += q0 >>> 4
        q0 += q0 >>> 8
        q0 += q0 >>> 16
        q0 += q0 >>> 32
        z -= q0 & 0xFFFFFFFFFFFFFFF8L
        q0 >>>= 3
        var r = (z - (q0 << 1)).toInt
        if (r >= 10) {
          q0 += 1L
          r -= 10
        }
        buf(pos + 18) = (r | '0').toByte
        posCorr = 1
      }
      val q1 = ((q0 >>> 8) * 2.56e-6).toLong // divide a medium positive long by 100000000
      q = q1.toInt
      if (q1 == q) {
        lastPos += digitCount(q)
        pos = lastPos
      } else {
        q = ((q1 >>> 8) * 1441151881L >>> 49).toInt // divide a small positive long by 100000000
        lastPos += ((9 - q) >>> 31) + 1
        pos = write8Digits((q1 - q * 100000000L).toInt, lastPos, buf, ds)
      }
      pos = write8Digits((q0 - q1 * 100000000L).toInt, pos, buf, ds) + posCorr
    }
    writePositiveIntDigits(q, lastPos, buf, ds)
    pos
  }

  // Based on the ingenious work of Xiang JunBo and Wang TieJun
  // "xjb: Fast Float to String Algorithm": https://github.com/xjb714/xjb/blob/4852e533287bd0e8d554c2a9f4cc6eaa93ca799f/fast_f2s.pdf
  // Sources with the license are here: https://github.com/xjb714/xjb
  private[this] def writeFloat(x: Float): Unit = {
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
      pos += 3
    } else {
      val e2IEEE = bits >> 23 & 0xFF
      val m2IEEE = bits & 0x7FFFFF
      var e2 = e2IEEE - 150
      var m2 = m2IEEE | 0x800000
      var m10, e10 = 0
      if (e2 == 0) m10 = m2
      else if ((e2 >= -23 && e2 < 0) && m2 << e2 == 0) m10 = m2 >> -e2
      else {
        if (e2IEEE == 0) {
          m2 = m2IEEE
          e2 = -149
        } else if (e2 == 105) illegalNumberError(x)
        e10 =
          if (m2IEEE == 0) (e2 * 315653 - 131237) >> 20
          else (e2 * 315653) >> 20
        val h = (((e10 + 1) * -217707) >> 16) + e2
        val pow10 = floatPow10s(31 - e10)
        val hi64 = unsignedMultiplyHigh1(pow10, m2.toLong << (h + 37)) // TODO: when dropping JDK 17 support replace by Math.unsignedMultiplyHigh(pow10, m2.toLong << (h + 37))
        m10 = (hi64 >>> 36).toInt * 10
        val dotOne = hi64 & 0xFFFFFFFFFL
        val halfUlpPlusEven = (pow10 >>> (28 - h)) + ((m2IEEE + 1) & 1)
        var m10Corr =
          if ({
            if (m2IEEE == 0) halfUlpPlusEven >>> 1
            else halfUlpPlusEven
          } > dotOne) 0
          else if (halfUlpPlusEven > 0xFFFFFFFFFL - dotOne) 10
          else (((dotOne << 4) + (dotOne << 2) + 0xFFFFFFFF9L + ((hi64 >>> 32) & 0xF)) >>> 37).toInt
        if (m2IEEE == 0 && ((e2 == -119) | (e2 == 64) | (e2 == 67))) m10Corr += 1
        m10 += m10Corr
      }
      val len = digitCount(m10)
      e10 += len - 1
      val ds = digits
      if (e10 < -3 || e10 >= 7) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        if (lastPos - 3 < pos) {
          buf(lastPos) = '0'
          pos = lastPos + 1
        } else pos = lastPos
        buf(pos) = 'E'
        pos += 1
        if (e10 < 0) {
          e10 = -e10
          buf(pos) = '-'
          pos += 1
        }
        if (e10 < 10) {
          buf(pos) = (e10 | '0').toByte
          pos += 1
        } else {
          val d = ds(e10)
          buf(pos) = d.toByte
          buf(pos + 1) = (d >> 8).toByte
          pos += 2
        }
      } else if (e10 < 0) {
        val dotPos = pos + 1
        buf(pos) = '0'
        buf(pos + 2) = '0'
        buf(pos + 3) = '0'
        pos -= e10
        pos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
      } else if (e10 < len - 1) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        val beforeDotPos = pos + e10
        while (pos <= beforeDotPos) {
          buf(pos) = buf(pos + 1)
          pos += 1
        }
        buf(pos) = '.'
        pos = lastPos
      } else {
        pos += len
        writePositiveIntDigits(m10, pos, buf, ds)
        buf(pos) = '.'
        buf(pos + 1) = '0'
        pos += 2
      }
    }
    count = pos
  }

  @inline
  private[this] def unsignedMultiplyHigh1(x: Long, y: Long): Long =
    Math.multiplyHigh(x, y) + y // Use implementation that works only when x is negative and y is positive

  // Based on the ingenious work of Xiang JunBo and Wang TieJun
  // "xjb: Fast Float to String Algorithm": https://github.com/xjb714/xjb/blob/4852e533287bd0e8d554c2a9f4cc6eaa93ca799f/fast_f2s.pdf
  // Sources with the license are here: https://github.com/xjb714/xjb
  private[this] def writeDouble(x: Double): Unit = {
    val bits = java.lang.Double.doubleToRawLongBits(x)
    var pos = ensureBufCapacity(25) // -1.2898455142673966E-135.toString.length + 1
    val buf = this.buf
    if (bits < 0L) {
      buf(pos) = '-'
      pos += 1
    }
    if (x == 0.0) {
      buf(pos) = '0'
      buf(pos + 1) = '.'
      buf(pos + 2) = '0'
      pos += 3
    } else {
      val e2IEEE = (bits >> 52).toInt & 0x7FF
      val m2IEEE = bits & 0xFFFFFFFFFFFFFL
      var e2 = e2IEEE - 1075
      var m2 = m2IEEE | 0x10000000000000L
      var m10 = 0L
      var e10 = 0
      if (e2 == 0) m10 = m2
      else if ((e2 >= -52 && e2 < 0) && m2 << e2 == 0) m10 = m2 >> -e2
      else {
        if (e2IEEE == 0) {
          m2 = m2IEEE
          e2 = -1074
        } else if (e2 == 972) illegalNumberError(x)
        e10 =
          if (m2IEEE == 0) (e2 * 315653 - 131237) >> 20
          else (e2 * 315653) >> 20
        val h = (((e10 + 1) * -217707) >> 16) + e2
        val pow10s = doublePow10s
        val i = 292 - e10 << 1
        val pow10_1 = pow10s(i)
        val pow10_2 = pow10s(i + 1)
        val cb = m2 << (h + 7)
        val lo64_1 = unsignedMultiplyHigh2(pow10_2, cb) // TODO: when dropping JDK 17 support replace by Math.unsignedMultiplyHigh(pow10_2, cb)
        val lo64_2 = pow10_1 * cb
        var hi64 = unsignedMultiplyHigh2(pow10_1, cb) // TODO: when dropping JDK 17 support replace by Math.unsignedMultiplyHigh(pow10_1, cb)
        val lo64 = lo64_1 + lo64_2
        hi64 += java.lang.Long.compareUnsigned(lo64, lo64_1) >>> 31
        val dotOne = (hi64 << 58) | (lo64 >>> 6)
        val halfUlp = pow10_1 >>> -h
        val even = (m2 + 1L) & 1L
        m10 = hi64 >>> 6
        m10 = (m10 << 3) + (m10 << 1)
        if (java.lang.Long.compareUnsigned(halfUlp + even, -1 - dotOne) > 0) m10 += 10L
        else if (m2IEEE != 0) {
          if (java.lang.Long.compareUnsigned(halfUlp + even, dotOne) <= 0) {
            m10 = (unsignedMultiplyHigh2(lo64, 10L) + (hi64 << 3) + (hi64 << 1) + { // TODO: when dropping JDK 17 support replace by Math.unsignedMultiplyHigh(lo64, 10L)
              if (dotOne == 0x4000000000000000L) 0x1FL
              else 0x20L
            }) >>> 6
          }
        } else {
          var tmp1 = dotOne >>> 4
          tmp1 = (tmp1 << 3) + (tmp1 << 1)
          var tmp2 = halfUlp >>> 4
          tmp2 += tmp2 << 2
          if (java.lang.Long.compareUnsigned((tmp1 << 4) >>> 4, tmp2) > 0) m10 += (tmp1 >>> 60).toInt + 1
          else if (java.lang.Long.compareUnsigned(halfUlp >>> 1, dotOne) <= 0) {
            m10 = (unsignedMultiplyHigh2(lo64, 10L) + (hi64 << 3) + (hi64 << 1) + { // TODO: when dropping JDK 17 support replace by Math.unsignedMultiplyHigh(lo64, 10L)
              if (dotOne == 0x4000000000000000L) 0x1FL
              else 0x20L
            }) >>> 6
          }
        }
      }
      val len = digitCount(m10)
      e10 += len - 1
      val ds = digits
      if (e10 < -3 || e10 >= 7) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(pos) = buf(pos + 1)
        buf(pos + 1) = '.'
        if (lastPos - 3 < pos) {
          buf(lastPos) = '0'
          pos = lastPos + 1
        } else pos = lastPos
        buf(pos) = 'E'
        pos += 1
        if (e10 < 0) {
          e10 = -e10
          buf(pos) = '-'
          pos += 1
        }
        if (e10 < 10) {
          buf(pos) = (e10 | '0').toByte
          pos += 1
        } else if (e10 < 100) {
          val d = ds(e10)
          buf(pos) = d.toByte
          buf(pos + 1) = (d >> 8).toByte
          pos += 2
        } else pos = write3Digits(e10, pos, buf, ds)
      } else if (e10 < 0) {
        val dotPos = pos + 1
        buf(pos) = '0'
        buf(pos + 2) = '0'
        buf(pos + 3) = '0'
        pos -= e10
        pos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
      } else if (e10 < len - 1) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        val beforeDotPos = pos + e10
        while (pos <= beforeDotPos) {
          buf(pos) = buf(pos + 1)
          pos += 1
        }
        buf(pos) = '.'
        pos = lastPos
      } else {
        pos += len
        writePositiveIntDigits(m10.toInt, pos, buf, ds)
        buf(pos) = '.'
        buf(pos + 1) = '0'
        pos += 2
      }
    }
    count = pos
  }

  @inline
  private[this] def unsignedMultiplyHigh2(x: Long, y: Long): Long =
    Math.multiplyHigh(x, y) + (y & (x >> 63)) // Use implementation that works only when y is positive

  @inline
  private[this] def digitCount(x: Long): Int =
    if (x >= 1000000000000000L) {
      if (x >= 10000000000000000L) 17
      else 16
    } else if (x >= 10000000000000L) {
      if (x >= 100000000000000L) 15
      else 14
    } else if (x >= 100000000000L) {
      if (x >= 1000000000000L) 13
      else 12
    } else if (x >= 1000000000L) {
      if (x >= 10000000000L) 11
      else 10
    } else digitCount(x.toInt)

  @inline
  private[this] def digitCount(x: Int): Int =
    if (x < 100) {
      if (x < 10) 1
      else 2
    } else if (x < 10000) {
      if (x < 1000) 3
      else 4
    } else if (x < 1000000) {
      if (x < 100000) 5
      else 6
    } else if (x < 100000000) {
      if (x < 10000000) 7
      else 8
    } else {
      if (x < 1000000000) 9
      else 10
    }

  @inline
  private[this] def writeSignificantFractionDigits(x: Long, p: Int, pl: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = x.toInt
    var pos = p
    var posLim = pl
    if (q0 != x) {
      val q1 = ((x >>> 8) * 2.56e-6).toInt  // divide a medium positive long by 100000000
      val r1 = (x - q1 * 100000000L).toInt
      val posm8 = pos - 8
      if (r1 == 0) {
        q0 = q1
        pos = posm8
      } else {
        writeFractionDigits(q1, posm8, posLim, buf, ds)
        q0 = r1
        posLim = posm8
      }
    }
    writeSignificantFractionDigits(q0, pos, posLim, buf, ds)
  }

  @inline
  private[this] def writeSignificantFractionDigits(x: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = x
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
    if (d > 0x3039) {
      buf(pos) = (d >> 8).toByte
      lastPos += 1
    }
    writeFractionDigits(q1, pos - 2, posLim, buf, ds)
    lastPos
  }

  @inline
  private[this] def writeFractionDigits(x: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = x
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

  @inline
  private[this] def writePositiveIntDigits(x: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = x
    var pos = p
    while ({
      pos -= 2
      q0 >= 100
    }) {
      val q1 = q0 / 100
      val d = ds(q0 - q1 * 100)
      buf(pos) = d.toByte
      buf(pos + 1) = (d >> 8).toByte
      q0 = q1
    }
    if (q0 < 10) buf(pos + 1) = (q0 | '0').toByte
    else {
      val d = ds(q0)
      buf(pos) = d.toByte
      buf(pos + 1) = (d >> 8).toByte
    }
  }

  private[this] def illegalNumberError(x: Float): Nothing = encodeError("illegal number: " + x)

  private[this] def illegalNumberError(x: Double): Nothing = encodeError("illegal number: " + x)

  @inline
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
  private final val base64Digits: Array[Byte] = Array(
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
    81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102,
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
    119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47
  )
  private final val base64UrlDigits: Array[Byte] = Array(
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
    81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102,
    103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
    119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 45, 95
  )
  private final val floatPow10s: Array[Long] = Array(
    0xCFB11EAD453994BBL, // -32
    0x81CEB32C4B43FCF5L, // -31
    0xA2425FF75E14FC32L, // -30
    0xCAD2F7F5359A3B3FL, // -29
    0xFD87B5F28300CA0EL, // -28
    0x9E74D1B791E07E49L, // -27
    0xC612062576589DDBL, // -26
    0xF79687AED3EEC552L, // -25
    0x9ABE14CD44753B53L, // -24
    0xC16D9A0095928A28L, // -23
    0xF1C90080BAF72CB2L, // -22
    0x971DA05074DA7BEFL, // -21
    0xBCE5086492111AEBL, // -20
    0xEC1E4A7DB69561A6L, // -19
    0x9392EE8E921D5D08L, // -18
    0xB877AA3236A4B44AL, // -17
    0xE69594BEC44DE15CL, // -16
    0x901D7CF73AB0ACDAL, // -15
    0xB424DC35095CD810L, // -14
    0xE12E13424BB40E14L, // -13
    0x8CBCCC096F5088CCL, // -12
    0xAFEBFF0BCB24AAFFL, // -11
    0xDBE6FECEBDEDD5BFL, // -10
    0x89705F4136B4A598L, // -9
    0xABCC77118461CEFDL, // -8
    0xD6BF94D5E57A42BDL, // -7
    0x8637BD05AF6C69B6L, // -6
    0xA7C5AC471B478424L, // -5
    0xD1B71758E219652CL, // -4
    0x83126E978D4FDF3CL, // -3
    0xA3D70A3D70A3D70BL, // -2
    0xCCCCCCCCCCCCCCCDL, // -1
    0x8000000000000000L, // 0
    0xA000000000000000L, // 1
    0xC800000000000000L, // 2
    0xFA00000000000000L, // 3
    0x9C40000000000000L, // 4
    0xC350000000000000L, // 5
    0xF424000000000000L, // 6
    0x9896800000000000L, // 7
    0xBEBC200000000000L, // 8
    0xEE6B280000000000L, // 9
    0x9502F90000000000L, // 10
    0xBA43B74000000000L, // 11
    0xE8D4A51000000000L, // 12
    0x9184E72A00000000L, // 13
    0xB5E620F480000000L, // 14
    0xE35FA931A0000000L, // 15
    0x8E1BC9BF04000000L, // 16
    0xB1A2BC2EC5000000L, // 17
    0xDE0B6B3A76400000L, // 18
    0x8AC7230489E80000L, // 19
    0xAD78EBC5AC620000L, // 20
    0xD8D726B7177A8000L, // 21
    0x878678326EAC9000L, // 22
    0xA968163F0A57B400L, // 23
    0xD3C21BCECCEDA100L, // 24
    0x84595161401484A0L, // 25
    0xA56FA5B99019A5C8L, // 26
    0xCECB8F27F4200F3AL, // 27
    0x813F3978F8940985L, // 28
    0xA18F07D736B90BE6L, // 29
    0xC9F2C9CD04674EDFL, // 30
    0xFC6F7C4045812297L, // 31
    0x9DC5ADA82B70B59EL, // 32
    0xC5371912364CE306L, // 33
    0xF684DF56C3E01BC7L, // 34
    0x9A130B963A6C115DL, // 35
    0xC097CE7BC90715B4L, // 36
    0xF0BDC21ABB48DB21L, // 37
    0x96769950B50D88F5L, // 38
    0xBC143FA4E250EB32L, // 39
    0xEB194F8E1AE525FEL, // 40
    0x92EFD1B8D0CF37BFL, // 41
    0xB7ABC627050305AEL, // 42
    0xE596B7B0C643C71AL, // 43
    0x8F7E32CE7BEA5C70L // 44
  )
  private final val doublePow10s: Array[Long] = Array(
    0xCC5FC196FEFD7D0CL, 0x1E53ED49A96272C9L, // -293
    0xFF77B1FCBEBCDC4FL, 0x25E8E89C13BB0F7BL, // -292
    0x9FAACF3DF73609B1L, 0x77B191618C54E9ADL, // -291
    0xC795830D75038C1DL, 0xD59DF5B9EF6A2418L, // -290
    0xF97AE3D0D2446F25L, 0x4B0573286B44AD1EL, // -289
    0x9BECCE62836AC577L, 0x4EE367F9430AEC33L, // -288
    0xC2E801FB244576D5L, 0x229C41F793CDA740L, // -287
    0xF3A20279ED56D48AL, 0x6B43527578C11110L, // -286
    0x9845418C345644D6L, 0x830A13896B78AAAAL, // -285
    0xBE5691EF416BD60CL, 0x23CC986BC656D554L, // -284
    0xEDEC366B11C6CB8FL, 0x2CBFBE86B7EC8AA9L, // -283
    0x94B3A202EB1C3F39L, 0x7BF7D71432F3D6AAL, // -282
    0xB9E08A83A5E34F07L, 0xDAF5CCD93FB0CC54L, // -281
    0xE858AD248F5C22C9L, 0xD1B3400F8F9CFF69L, // -280
    0x91376C36D99995BEL, 0x23100809B9C21FA2L, // -279
    0xB58547448FFFFB2DL, 0xABD40A0C2832A78BL, // -278
    0xE2E69915B3FFF9F9L, 0x16C90C8F323F516DL, // -277
    0x8DD01FAD907FFC3BL, 0xAE3DA7D97F6792E4L, // -276
    0xB1442798F49FFB4AL, 0x99CD11CFDF41779DL, // -275
    0xDD95317F31C7FA1DL, 0x40405643D711D584L, // -274
    0x8A7D3EEF7F1CFC52L, 0x482835EA666B2573L, // -273
    0xAD1C8EAB5EE43B66L, 0xDA3243650005EED0L, // -272
    0xD863B256369D4A40L, 0x90BED43E40076A83L, // -271
    0x873E4F75E2224E68L, 0x5A7744A6E804A292L, // -270
    0xA90DE3535AAAE202L, 0x711515D0A205CB37L, // -269
    0xD3515C2831559A83L, 0x0D5A5B44CA873E04L, // -268
    0x8412D9991ED58091L, 0xE858790AFE9486C3L, // -267
    0xA5178FFF668AE0B6L, 0x626E974DBE39A873L, // -266
    0xCE5D73FF402D98E3L, 0xFB0A3D212DC81290L, // -265
    0x80FA687F881C7F8EL, 0x7CE66634BC9D0B9AL, // -264
    0xA139029F6A239F72L, 0x1C1FFFC1EBC44E81L, // -263
    0xC987434744AC874EL, 0xA327FFB266B56221L, // -262
    0xFBE9141915D7A922L, 0x4BF1FF9F0062BAA9L, // -261
    0x9D71AC8FADA6C9B5L, 0x6F773FC3603DB4AAL, // -260
    0xC4CE17B399107C22L, 0xCB550FB4384D21D4L, // -259
    0xF6019DA07F549B2BL, 0x7E2A53A146606A49L, // -258
    0x99C102844F94E0FBL, 0x2EDA7444CBFC426EL, // -257
    0xC0314325637A1939L, 0xFA911155FEFB5309L, // -256
    0xF03D93EEBC589F88L, 0x793555AB7EBA27CBL, // -255
    0x96267C7535B763B5L, 0x4BC1558B2F3458DFL, // -254
    0xBBB01B9283253CA2L, 0x9EB1AAEDFB016F17L, // -253
    0xEA9C227723EE8BCBL, 0x465E15A979C1CADDL, // -252
    0x92A1958A7675175FL, 0x0BFACD89EC191ECAL, // -251
    0xB749FAED14125D36L, 0xCEF980EC671F667CL, // -250
    0xE51C79A85916F484L, 0x82B7E12780E7401BL, // -249
    0x8F31CC0937AE58D2L, 0xD1B2ECB8B0908811L, // -248
    0xB2FE3F0B8599EF07L, 0x861FA7E6DCB4AA16L, // -247
    0xDFBDCECE67006AC9L, 0x67A791E093E1D49BL, // -246
    0x8BD6A141006042BDL, 0xE0C8BB2C5C6D24E1L, // -245
    0xAECC49914078536DL, 0x58FAE9F773886E19L, // -244
    0xDA7F5BF590966848L, 0xAF39A475506A899FL, // -243
    0x888F99797A5E012DL, 0x6D8406C952429604L, // -242
    0xAAB37FD7D8F58178L, 0xC8E5087BA6D33B84L, // -241
    0xD5605FCDCF32E1D6L, 0xFB1E4A9A90880A65L, // -240
    0x855C3BE0A17FCD26L, 0x5CF2EEA09A550680L, // -239
    0xA6B34AD8C9DFC06FL, 0xF42FAA48C0EA481FL, // -238
    0xD0601D8EFC57B08BL, 0xF13B94DAF124DA27L, // -237
    0x823C12795DB6CE57L, 0x76C53D08D6B70859L, // -236
    0xA2CB1717B52481EDL, 0x54768C4B0C64CA6FL, // -235
    0xCB7DDCDDA26DA268L, 0xA9942F5DCF7DFD0AL, // -234
    0xFE5D54150B090B02L, 0xD3F93B35435D7C4DL, // -233
    0x9EFA548D26E5A6E1L, 0xC47BC5014A1A6DB0L, // -232
    0xC6B8E9B0709F109AL, 0x359AB6419CA1091CL, // -231
    0xF867241C8CC6D4C0L, 0xC30163D203C94B63L, // -230
    0x9B407691D7FC44F8L, 0x79E0DE63425DCF1EL, // -229
    0xC21094364DFB5636L, 0x985915FC12F542E5L, // -228
    0xF294B943E17A2BC4L, 0x3E6F5B7B17B2939EL, // -227
    0x979CF3CA6CEC5B5AL, 0xA705992CEECF9C43L, // -226
    0xBD8430BD08277231L, 0x50C6FF782A838354L, // -225
    0xECE53CEC4A314EBDL, 0xA4F8BF5635246429L, // -224
    0x940F4613AE5ED136L, 0x871B7795E136BE9AL, // -223
    0xB913179899F68584L, 0x28E2557B59846E40L, // -222
    0xE757DD7EC07426E5L, 0x331AEADA2FE589D0L, // -221
    0x9096EA6F3848984FL, 0x3FF0D2C85DEF7622L, // -220
    0xB4BCA50B065ABE63L, 0x0FED077A756B53AAL, // -219
    0xE1EBCE4DC7F16DFBL, 0xD3E8495912C62895L, // -218
    0x8D3360F09CF6E4BDL, 0x64712DD7ABBBD95DL, // -217
    0xB080392CC4349DECL, 0xBD8D794D96AACFB4L, // -216
    0xDCA04777F541C567L, 0xECF0D7A0FC5583A1L, // -215
    0x89E42CAAF9491B60L, 0xF41686C49DB57245L, // -214
    0xAC5D37D5B79B6239L, 0x311C2875C522CED6L, // -213
    0xD77485CB25823AC7L, 0x7D633293366B828CL, // -212
    0x86A8D39EF77164BCL, 0xAE5DFF9C02033198L, // -211
    0xA8530886B54DBDEBL, 0xD9F57F830283FDFDL, // -210
    0xD267CAA862A12D66L, 0xD072DF63C324FD7CL, // -209
    0x8380DEA93DA4BC60L, 0x4247CB9E59F71E6EL, // -208
    0xA46116538D0DEB78L, 0x52D9BE85F074E609L, // -207
    0xCD795BE870516656L, 0x67902E276C921F8CL, // -206
    0x806BD9714632DFF6L, 0x00BA1CD8A3DB53B7L, // -205
    0xA086CFCD97BF97F3L, 0x80E8A40ECCD228A5L, // -204
    0xC8A883C0FDAF7DF0L, 0x6122CD128006B2CEL, // -203
    0xFAD2A4B13D1B5D6CL, 0x796B805720085F82L, // -202
    0x9CC3A6EEC6311A63L, 0xCBE3303674053BB1L, // -201
    0xC3F490AA77BD60FCL, 0xBEDBFC4411068A9DL, // -200
    0xF4F1B4D515ACB93BL, 0xEE92FB5515482D45L, // -199
    0x991711052D8BF3C5L, 0x751BDD152D4D1C4BL, // -198
    0xBF5CD54678EEF0B6L, 0xD262D45A78A0635EL, // -197
    0xEF340A98172AACE4L, 0x86FB897116C87C35L, // -196
    0x9580869F0E7AAC0EL, 0xD45D35E6AE3D4DA1L, // -195
    0xBAE0A846D2195712L, 0x8974836059CCA10AL, // -194
    0xE998D258869FACD7L, 0x2BD1A438703FC94CL, // -193
    0x91FF83775423CC06L, 0x7B6306A34627DDD0L, // -192
    0xB67F6455292CBF08L, 0x1A3BC84C17B1D543L, // -191
    0xE41F3D6A7377EECAL, 0x20CABA5F1D9E4A94L, // -190
    0x8E938662882AF53EL, 0x547EB47B7282EE9DL, // -189
    0xB23867FB2A35B28DL, 0xE99E619A4F23AA44L, // -188
    0xDEC681F9F4C31F31L, 0x6405FA00E2EC94D5L, // -187
    0x8B3C113C38F9F37EL, 0xDE83BC408DD3DD05L, // -186
    0xAE0B158B4738705EL, 0x9624AB50B148D446L, // -185
    0xD98DDAEE19068C76L, 0x3BADD624DD9B0958L, // -184
    0x87F8A8D4CFA417C9L, 0xE54CA5D70A80E5D7L, // -183
    0xA9F6D30A038D1DBCL, 0x5E9FCF4CCD211F4DL, // -182
    0xD47487CC8470652BL, 0x7647C32000696720L, // -181
    0x84C8D4DFD2C63F3BL, 0x29ECD9F40041E074L, // -180
    0xA5FB0A17C777CF09L, 0xF468107100525891L, // -179
    0xCF79CC9DB955C2CCL, 0x7182148D4066EEB5L, // -178
    0x81AC1FE293D599BFL, 0xC6F14CD848405531L, // -177
    0xA21727DB38CB002FL, 0xB8ADA00E5A506A7DL, // -176
    0xCA9CF1D206FDC03BL, 0xA6D90811F0E4851DL, // -175
    0xFD442E4688BD304AL, 0x908F4A166D1DA664L, // -174
    0x9E4A9CEC15763E2EL, 0x9A598E4E043287FFL, // -173
    0xC5DD44271AD3CDBAL, 0x40EFF1E1853F29FEL, // -172
    0xF7549530E188C128L, 0xD12BEE59E68EF47DL, // -171
    0x9A94DD3E8CF578B9L, 0x82BB74F8301958CFL, // -170
    0xC13A148E3032D6E7L, 0xE36A52363C1FAF02L, // -169
    0xF18899B1BC3F8CA1L, 0xDC44E6C3CB279AC2L, // -168
    0x96F5600F15A7B7E5L, 0x29AB103A5EF8C0BAL, // -167
    0xBCB2B812DB11A5DEL, 0x7415D448F6B6F0E8L, // -166
    0xEBDF661791D60F56L, 0x111B495B3464AD22L, // -165
    0x936B9FCEBB25C995L, 0xCAB10DD900BEEC35L, // -164
    0xB84687C269EF3BFBL, 0x3D5D514F40EEA743L, // -163
    0xE65829B3046B0AFAL, 0x0CB4A5A3112A5113L, // -162
    0x8FF71A0FE2C2E6DCL, 0x47F0E785EABA72ACL, // -161
    0xB3F4E093DB73A093L, 0x59ED216765690F57L, // -160
    0xE0F218B8D25088B8L, 0x306869C13EC3532DL, // -159
    0x8C974F7383725573L, 0x1E414218C73A13FCL, // -158
    0xAFBD2350644EEACFL, 0xE5D1929EF90898FBL, // -157
    0xDBAC6C247D62A583L, 0xDF45F746B74ABF3AL, // -156
    0x894BC396CE5DA772L, 0x6B8BBA8C328EB784L, // -155
    0xAB9EB47C81F5114FL, 0x066EA92F3F326565L, // -154
    0xD686619BA27255A2L, 0xC80A537B0EFEFEBEL, // -153
    0x8613FD0145877585L, 0xBD06742CE95F5F37L, // -152
    0xA798FC4196E952E7L, 0x2C48113823B73705L, // -151
    0xD17F3B51FCA3A7A0L, 0xF75A15862CA504C6L, // -150
    0x82EF85133DE648C4L, 0x9A984D73DBE722FCL, // -149
    0xA3AB66580D5FDAF5L, 0xC13E60D0D2E0EBBBL, // -148
    0xCC963FEE10B7D1B3L, 0x318DF905079926A9L, // -147
    0xFFBBCFE994E5C61FL, 0xFDF17746497F7053L, // -146
    0x9FD561F1FD0F9BD3L, 0xFEB6EA8BEDEFA634L, // -145
    0xC7CABA6E7C5382C8L, 0xFE64A52EE96B8FC1L, // -144
    0xF9BD690A1B68637BL, 0x3DFDCE7AA3C673B1L, // -143
    0x9C1661A651213E2DL, 0x06BEA10CA65C084FL, // -142
    0xC31BFA0FE5698DB8L, 0x486E494FCFF30A63L, // -141
    0xF3E2F893DEC3F126L, 0x5A89DBA3C3EFCCFBL, // -140
    0x986DDB5C6B3A76B7L, 0xF89629465A75E01DL, // -139
    0xBE89523386091465L, 0xF6BBB397F1135824L, // -138
    0xEE2BA6C0678B597FL, 0x746AA07DED582E2DL, // -137
    0x94DB483840B717EFL, 0xA8C2A44EB4571CDDL, // -136
    0xBA121A4650E4DDEBL, 0x92F34D62616CE414L, // -135
    0xE896A0D7E51E1566L, 0x77B020BAF9C81D18L, // -134
    0x915E2486EF32CD60L, 0x0ACE1474DC1D122FL, // -133
    0xB5B5ADA8AAFF80B8L, 0x0D819992132456BBL, // -132
    0xE3231912D5BF60E6L, 0x10E1FFF697ED6C6AL, // -131
    0x8DF5EFABC5979C8FL, 0xCA8D3FFA1EF463C2L, // -130
    0xB1736B96B6FD83B3L, 0xBD308FF8A6B17CB3L, // -129
    0xDDD0467C64BCE4A0L, 0xAC7CB3F6D05DDBDFL, // -128
    0x8AA22C0DBEF60EE4L, 0x6BCDF07A423AA96CL, // -127
    0xAD4AB7112EB3929DL, 0x86C16C98D2C953C7L, // -126
    0xD89D64D57A607744L, 0xE871C7BF077BA8B8L, // -125
    0x87625F056C7C4A8BL, 0x11471CD764AD4973L, // -124
    0xA93AF6C6C79B5D2DL, 0xD598E40D3DD89BD0L, // -123
    0xD389B47879823479L, 0x4AFF1D108D4EC2C4L, // -122
    0x843610CB4BF160CBL, 0xCEDF722A585139BBL, // -121
    0xA54394FE1EEDB8FEL, 0xC2974EB4EE658829L, // -120
    0xCE947A3DA6A9273EL, 0x733D226229FEEA33L, // -119
    0x811CCC668829B887L, 0x0806357D5A3F5260L, // -118
    0xA163FF802A3426A8L, 0xCA07C2DCB0CF26F8L, // -117
    0xC9BCFF6034C13052L, 0xFC89B393DD02F0B6L, // -116
    0xFC2C3F3841F17C67L, 0xBBAC2078D443ACE3L, // -115
    0x9D9BA7832936EDC0L, 0xD54B944B84AA4C0EL, // -114
    0xC5029163F384A931L, 0x0A9E795E65D4DF12L, // -113
    0xF64335BCF065D37DL, 0x4D4617B5FF4A16D6L, // -112
    0x99EA0196163FA42EL, 0x504BCED1BF8E4E46L, // -111
    0xC06481FB9BCF8D39L, 0xE45EC2862F71E1D7L, // -110
    0xF07DA27A82C37088L, 0x5D767327BB4E5A4DL, // -109
    0x964E858C91BA2655L, 0x3A6A07F8D510F870L, // -108
    0xBBE226EFB628AFEAL, 0x890489F70A55368CL, // -107
    0xEADAB0ABA3B2DBE5L, 0x2B45AC74CCEA842FL, // -106
    0x92C8AE6B464FC96FL, 0x3B0B8BC90012929EL, // -105
    0xB77ADA0617E3BBCBL, 0x09CE6EBB40173745L, // -104
    0xE55990879DDCAABDL, 0xCC420A6A101D0516L, // -103
    0x8F57FA54C2A9EAB6L, 0x9FA946824A12232EL, // -102
    0xB32DF8E9F3546564L, 0x47939822DC96ABFAL, // -101
    0xDFF9772470297EBDL, 0x59787E2B93BC56F8L, // -100
    0x8BFBEA76C619EF36L, 0x57EB4EDB3C55B65BL, // -99
    0xAEFAE51477A06B03L, 0xEDE622920B6B23F2L, // -98
    0xDAB99E59958885C4L, 0xE95FAB368E45ECEEL, // -97
    0x88B402F7FD75539BL, 0x11DBCB0218EBB415L, // -96
    0xAAE103B5FCD2A881L, 0xD652BDC29F26A11AL, // -95
    0xD59944A37C0752A2L, 0x4BE76D3346F04960L, // -94
    0x857FCAE62D8493A5L, 0x6F70A4400C562DDCL, // -93
    0xA6DFBD9FB8E5B88EL, 0xCB4CCD500F6BB953L, // -92
    0xD097AD07A71F26B2L, 0x7E2000A41346A7A8L, // -91
    0x825ECC24C873782FL, 0x8ED400668C0C28C9L, // -90
    0xA2F67F2DFA90563BL, 0x728900802F0F32FBL, // -89
    0xCBB41EF979346BCAL, 0x4F2B40A03AD2FFBAL, // -88
    0xFEA126B7D78186BCL, 0xE2F610C84987BFA9L, // -87
    0x9F24B832E6B0F436L, 0x0DD9CA7D2DF4D7CAL, // -86
    0xC6EDE63FA05D3143L, 0x91503D1C79720DBCL, // -85
    0xF8A95FCF88747D94L, 0x75A44C6397CE912BL, // -84
    0x9B69DBE1B548CE7CL, 0xC986AFBE3EE11ABBL, // -83
    0xC24452DA229B021BL, 0xFBE85BADCE996169L, // -82
    0xF2D56790AB41C2A2L, 0xFAE27299423FB9C4L, // -81
    0x97C560BA6B0919A5L, 0xDCCD879FC967D41BL, // -80
    0xBDB6B8E905CB600FL, 0x5400E987BBC1C921L, // -79
    0xED246723473E3813L, 0x290123E9AAB23B69L, // -78
    0x9436C0760C86E30BL, 0xF9A0B6720AAF6522L, // -77
    0xB94470938FA89BCEL, 0xF808E40E8D5B3E6AL, // -76
    0xE7958CB87392C2C2L, 0xB60B1D1230B20E05L, // -75
    0x90BD77F3483BB9B9L, 0xB1C6F22B5E6F48C3L, // -74
    0xB4ECD5F01A4AA828L, 0x1E38AEB6360B1AF4L, // -73
    0xE2280B6C20DD5232L, 0x25C6DA63C38DE1B1L, // -72
    0x8D590723948A535FL, 0x579C487E5A38AD0FL, // -71
    0xB0AF48EC79ACE837L, 0x2D835A9DF0C6D852L, // -70
    0xDCDB1B2798182244L, 0xF8E431456CF88E66L, // -69
    0x8A08F0F8BF0F156BL, 0x1B8E9ECB641B5900L, // -68
    0xAC8B2D36EED2DAC5L, 0xE272467E3D222F40L, // -67
    0xD7ADF884AA879177L, 0x5B0ED81DCC6ABB10L, // -66
    0x86CCBB52EA94BAEAL, 0x98E947129FC2B4EAL, // -65
    0xA87FEA27A539E9A5L, 0x3F2398D747B36225L, // -64
    0xD29FE4B18E88640EL, 0x8EEC7F0D19A03AAEL, // -63
    0x83A3EEEEF9153E89L, 0x1953CF68300424ADL, // -62
    0xA48CEAAAB75A8E2BL, 0x5FA8C3423C052DD8L, // -61
    0xCDB02555653131B6L, 0x3792F412CB06794EL, // -60
    0x808E17555F3EBF11L, 0xE2BBD88BBEE40BD1L, // -59
    0xA0B19D2AB70E6ED6L, 0x5B6ACEAEAE9D0EC5L, // -58
    0xC8DE047564D20A8BL, 0xF245825A5A445276L, // -57
    0xFB158592BE068D2EL, 0xEED6E2F0F0D56713L, // -56
    0x9CED737BB6C4183DL, 0x55464DD69685606CL, // -55
    0xC428D05AA4751E4CL, 0xAA97E14C3C26B887L, // -54
    0xF53304714D9265DFL, 0xD53DD99F4B3066A9L, // -53
    0x993FE2C6D07B7FABL, 0xE546A8038EFE402AL, // -52
    0xBF8FDB78849A5F96L, 0xDE98520472BDD034L, // -51
    0xEF73D256A5C0F77CL, 0x963E66858F6D4441L, // -50
    0x95A8637627989AADL, 0xDDE7001379A44AA9L, // -49
    0xBB127C53B17EC159L, 0x5560C018580D5D53L, // -48
    0xE9D71B689DDE71AFL, 0xAAB8F01E6E10B4A7L, // -47
    0x9226712162AB070DL, 0xCAB3961304CA70E9L, // -46
    0xB6B00D69BB55C8D1L, 0x3D607B97C5FD0D23L, // -45
    0xE45C10C42A2B3B05L, 0x8CB89A7DB77C506BL, // -44
    0x8EB98A7A9A5B04E3L, 0x77F3608E92ADB243L, // -43
    0xB267ED1940F1C61CL, 0x55F038B237591ED4L, // -42
    0xDF01E85F912E37A3L, 0x6B6C46DEC52F6689L, // -41
    0x8B61313BBABCE2C6L, 0x2323AC4B3B3DA016L, // -40
    0xAE397D8AA96C1B77L, 0xABEC975E0A0D081BL, // -39
    0xD9C7DCED53C72255L, 0x96E7BD358C904A22L, // -38
    0x881CEA14545C7575L, 0x7E50D64177DA2E55L, // -37
    0xAA242499697392D2L, 0xDDE50BD1D5D0B9EAL, // -36
    0xD4AD2DBFC3D07787L, 0x955E4EC64B44E865L, // -35
    0x84EC3C97DA624AB4L, 0xBD5AF13BEF0B113FL, // -34
    0xA6274BBDD0FADD61L, 0xECB1AD8AEACDD58FL, // -33
    0xCFB11EAD453994BAL, 0x67DE18EDA5814AF3L, // -32
    0x81CEB32C4B43FCF4L, 0x80EACF948770CED8L, // -31
    0xA2425FF75E14FC31L, 0xA1258379A94D028EL, // -30
    0xCAD2F7F5359A3B3EL, 0x096EE45813A04331L, // -29
    0xFD87B5F28300CA0DL, 0x8BCA9D6E188853FDL, // -28
    0x9E74D1B791E07E48L, 0x775EA264CF55347EL, // -27
    0xC612062576589DDAL, 0x95364AFE032A819EL, // -26
    0xF79687AED3EEC551L, 0x3A83DDBD83F52205L, // -25
    0x9ABE14CD44753B52L, 0xC4926A9672793543L, // -24
    0xC16D9A0095928A27L, 0x75B7053C0F178294L, // -23
    0xF1C90080BAF72CB1L, 0x5324C68B12DD6339L, // -22
    0x971DA05074DA7BEEL, 0xD3F6FC16EBCA5E04L, // -21
    0xBCE5086492111AEAL, 0x88F4BB1CA6BCF585L, // -20
    0xEC1E4A7DB69561A5L, 0x2B31E9E3D06C32E6L, // -19
    0x9392EE8E921D5D07L, 0x3AFF322E62439FD0L, // -18
    0xB877AA3236A4B449L, 0x09BEFEB9FAD487C3L, // -17
    0xE69594BEC44DE15BL, 0x4C2EBE687989A9B4L, // -16
    0x901D7CF73AB0ACD9L, 0x0F9D37014BF60A11L, // -15
    0xB424DC35095CD80FL, 0x538484C19EF38C95L, // -14
    0xE12E13424BB40E13L, 0x2865A5F206B06FBAL, // -13
    0x8CBCCC096F5088CBL, 0xF93F87B7442E45D4L, // -12
    0xAFEBFF0BCB24AAFEL, 0xF78F69A51539D749L, // -11
    0xDBE6FECEBDEDD5BEL, 0xB573440E5A884D1CL, // -10
    0x89705F4136B4A597L, 0x31680A88F8953031L, // -9
    0xABCC77118461CEFCL, 0xFDC20D2B36BA7C3EL, // -8
    0xD6BF94D5E57A42BCL, 0x3D32907604691B4DL, // -7
    0x8637BD05AF6C69B5L, 0xA63F9A49C2C1B110L, // -6
    0xA7C5AC471B478423L, 0x0FCF80DC33721D54L, // -5
    0xD1B71758E219652BL, 0xD3C36113404EA4A9L, // -4
    0x83126E978D4FDF3BL, 0x645A1CAC083126EAL, // -3
    0xA3D70A3D70A3D70AL, 0x3D70A3D70A3D70A4L, // -2
    0xCCCCCCCCCCCCCCCCL, 0xCCCCCCCCCCCCCCCDL, // -1
    0x8000000000000000L, 0x0,                // 0
    0xA000000000000000L, 0x0,                // 1
    0xC800000000000000L, 0x0,                // 2
    0xFA00000000000000L, 0x0,                // 3
    0x9C40000000000000L, 0x0,                // 4
    0xC350000000000000L, 0x0,                // 5
    0xF424000000000000L, 0x0,                // 6
    0x9896800000000000L, 0x0,                // 7
    0xBEBC200000000000L, 0x0,                // 8
    0xEE6B280000000000L, 0x0,                // 9
    0x9502F90000000000L, 0x0,                // 10
    0xBA43B74000000000L, 0x0,                // 11
    0xE8D4A51000000000L, 0x0,                // 12
    0x9184E72A00000000L, 0x0,                // 13
    0xB5E620F480000000L, 0x0,                // 14
    0xE35FA931A0000000L, 0x0,                // 15
    0x8E1BC9BF04000000L, 0x0,                // 16
    0xB1A2BC2EC5000000L, 0x0,                // 17
    0xDE0B6B3A76400000L, 0x0,                // 18
    0x8AC7230489E80000L, 0x0,                // 19
    0xAD78EBC5AC620000L, 0x0,                // 20
    0xD8D726B7177A8000L, 0x0,                // 21
    0x878678326EAC9000L, 0x0,                // 22
    0xA968163F0A57B400L, 0x0,                // 23
    0xD3C21BCECCEDA100L, 0x0,                // 24
    0x84595161401484A0L, 0x0,                // 25
    0xA56FA5B99019A5C8L, 0x0,                // 26
    0xCECB8F27F4200F3AL, 0x0,                // 27
    0x813F3978F8940984L, 0x4000000000000000L, // 28
    0xA18F07D736B90BE5L, 0x5000000000000000L, // 29
    0xC9F2C9CD04674EDEL, 0xA400000000000000L, // 30
    0xFC6F7C4045812296L, 0x4D00000000000000L, // 31
    0x9DC5ADA82B70B59DL, 0xF020000000000000L, // 32
    0xC5371912364CE305L, 0x6C28000000000000L, // 33
    0xF684DF56C3E01BC6L, 0xC732000000000000L, // 34
    0x9A130B963A6C115CL, 0x3C7F400000000000L, // 35
    0xC097CE7BC90715B3L, 0x4B9F100000000000L, // 36
    0xF0BDC21ABB48DB20L, 0x1E86D40000000000L, // 37
    0x96769950B50D88F4L, 0x1314448000000000L, // 38
    0xBC143FA4E250EB31L, 0x17D955A000000000L, // 39
    0xEB194F8E1AE525FDL, 0x5DCFAB0800000000L, // 40
    0x92EFD1B8D0CF37BEL, 0x5AA1CAE500000000L, // 41
    0xB7ABC627050305ADL, 0xF14A3D9E40000000L, // 42
    0xE596B7B0C643C719L, 0x6D9CCD05D0000000L, // 43
    0x8F7E32CE7BEA5C6FL, 0xE4820023A2000000L, // 44
    0xB35DBF821AE4F38BL, 0xDDA2802C8A800000L, // 45
    0xE0352F62A19E306EL, 0xD50B2037AD200000L, // 46
    0x8C213D9DA502DE45L, 0x4526F422CC340000L, // 47
    0xAF298D050E4395D6L, 0x9670B12B7F410000L, // 48
    0xDAF3F04651D47B4CL, 0x3C0CDD765F114000L, // 49
    0x88D8762BF324CD0FL, 0xA5880A69FB6AC800L, // 50
    0xAB0E93B6EFEE0053L, 0x8EEA0D047A457A00L, // 51
    0xD5D238A4ABE98068L, 0x72A4904598D6D880L, // 52
    0x85A36366EB71F041L, 0x47A6DA2B7F864750L, // 53
    0xA70C3C40A64E6C51L, 0x999090B65F67D924L, // 54
    0xD0CF4B50CFE20765L, 0xFFF4B4E3F741CF6DL, // 55
    0x82818F1281ED449FL, 0xBFF8F10E7A8921A5L, // 56
    0xA321F2D7226895C7L, 0xAFF72D52192B6A0EL, // 57
    0xCBEA6F8CEB02BB39L, 0x9BF4F8A69F764491L, // 58
    0xFEE50B7025C36A08L, 0x02F236D04753D5B5L, // 59
    0x9F4F2726179A2245L, 0x01D762422C946591L, // 60
    0xC722F0EF9D80AAD6L, 0x424D3AD2B7B97EF6L, // 61
    0xF8EBAD2B84E0D58BL, 0xD2E0898765A7DEB3L, // 62
    0x9B934C3B330C8577L, 0x63CC55F49F88EB30L, // 63
    0xC2781F49FFCFA6D5L, 0x3CBF6B71C76B25FCL, // 64
    0xF316271C7FC3908AL, 0x8BEF464E3945EF7BL, // 65
    0x97EDD871CFDA3A56L, 0x97758BF0E3CBB5ADL, // 66
    0xBDE94E8E43D0C8ECL, 0x3D52EEED1CBEA318L, // 67
    0xED63A231D4C4FB27L, 0x4CA7AAA863EE4BDEL, // 68
    0x945E455F24FB1CF8L, 0x8FE8CAA93E74EF6BL, // 69
    0xB975D6B6EE39E436L, 0xB3E2FD538E122B45L, // 70
    0xE7D34C64A9C85D44L, 0x60DBBCA87196B617L, // 71
    0x90E40FBEEA1D3A4AL, 0xBC8955E946FE31CEL, // 72
    0xB51D13AEA4A488DDL, 0x6BABAB6398BDBE42L, // 73
    0xE264589A4DCDAB14L, 0xC696963C7EED2DD2L, // 74
    0x8D7EB76070A08AECL, 0xFC1E1DE5CF543CA3L, // 75
    0xB0DE65388CC8ADA8L, 0x3B25A55F43294BCCL, // 76
    0xDD15FE86AFFAD912L, 0x49EF0EB713F39EBFL, // 77
    0x8A2DBF142DFCC7ABL, 0x6E3569326C784338L, // 78
    0xACB92ED9397BF996L, 0x49C2C37F07965405L, // 79
    0xD7E77A8F87DAF7FBL, 0xDC33745EC97BE907L, // 80
    0x86F0AC99B4E8DAFDL, 0x69A028BB3DED71A4L, // 81
    0xA8ACD7C0222311BCL, 0xC40832EA0D68CE0DL, // 82
    0xD2D80DB02AABD62BL, 0xF50A3FA490C30191L, // 83
    0x83C7088E1AAB65DBL, 0x792667C6DA79E0FBL, // 84
    0xA4B8CAB1A1563F52L, 0x577001B891185939L, // 85
    0xCDE6FD5E09ABCF26L, 0xED4C0226B55E6F87L, // 86
    0x80B05E5AC60B6178L, 0x544F8158315B05B5L, // 87
    0xA0DC75F1778E39D6L, 0x696361AE3DB1C722L, // 88
    0xC913936DD571C84CL, 0x03BC3A19CD1E38EAL, // 89
    0xFB5878494ACE3A5FL, 0x04AB48A04065C724L, // 90
    0x9D174B2DCEC0E47BL, 0x62EB0D64283F9C77L, // 91
    0xC45D1DF942711D9AL, 0x3BA5D0BD324F8395L, // 92
    0xF5746577930D6500L, 0xCA8F44EC7EE3647AL, // 93
    0x9968BF6ABBE85F20L, 0x7E998B13CF4E1ECCL, // 94
    0xBFC2EF456AE276E8L, 0x9E3FEDD8C321A67FL, // 95
    0xEFB3AB16C59B14A2L, 0xC5CFE94EF3EA101FL, // 96
    0x95D04AEE3B80ECE5L, 0xBBA1F1D158724A13L, // 97
    0xBB445DA9CA61281FL, 0x2A8A6E45AE8EDC98L, // 98
    0xEA1575143CF97226L, 0xF52D09D71A3293BEL, // 99
    0x924D692CA61BE758L, 0x593C2626705F9C57L, // 100
    0xB6E0C377CFA2E12EL, 0x6F8B2FB00C77836DL, // 101
    0xE498F455C38B997AL, 0x0B6DFB9C0F956448L, // 102
    0x8EDF98B59A373FECL, 0x4724BD4189BD5EADL, // 103
    0xB2977EE300C50FE7L, 0x58EDEC91EC2CB658L, // 104
    0xDF3D5E9BC0F653E1L, 0x2F2967B66737E3EEL, // 105
    0x8B865B215899F46CL, 0xBD79E0D20082EE75L, // 106
    0xAE67F1E9AEC07187L, 0xECD8590680A3AA12L, // 107
    0xDA01EE641A708DE9L, 0xE80E6F4820CC9496L, // 108
    0x884134FE908658B2L, 0x3109058D147FDCDEL, // 109
    0xAA51823E34A7EEDEL, 0xBD4B46F0599FD416L, // 110
    0xD4E5E2CDC1D1EA96L, 0x6C9E18AC7007C91BL, // 111
    0x850FADC09923329EL, 0x03E2CF6BC604DDB1L, // 112
    0xA6539930BF6BFF45L, 0x84DB8346B786151DL, // 113
    0xCFE87F7CEF46FF16L, 0xE612641865679A64L, // 114
    0x81F14FAE158C5F6EL, 0x4FCB7E8F3F60C07FL, // 115
    0xA26DA3999AEF7749L, 0xE3BE5E330F38F09EL, // 116
    0xCB090C8001AB551CL, 0x5CADF5BFD3072CC6L, // 117
    0xFDCB4FA002162A63L, 0x73D9732FC7C8F7F7L, // 118
    0x9E9F11C4014DDA7EL, 0x2867E7FDDCDD9AFBL, // 119
    0xC646D63501A1511DL, 0xB281E1FD541501B9L, // 120
    0xF7D88BC24209A565L, 0x1F225A7CA91A4227L, // 121
    0x9AE757596946075FL, 0x3375788DE9B06959L, // 122
    0xC1A12D2FC3978937L, 0x0052D6B1641C83AFL, // 123
    0xF209787BB47D6B84L, 0xC0678C5DBD23A49BL, // 124
    0x9745EB4D50CE6332L, 0xF840B7BA963646E1L, // 125
    0xBD176620A501FBFFL, 0xB650E5A93BC3D899L, // 126
    0xEC5D3FA8CE427AFFL, 0xA3E51F138AB4CEBFL, // 127
    0x93BA47C980E98CDFL, 0xC66F336C36B10138L, // 128
    0xB8A8D9BBE123F017L, 0xB80B0047445D4185L, // 129
    0xE6D3102AD96CEC1DL, 0xA60DC059157491E6L, // 130
    0x9043EA1AC7E41392L, 0x87C89837AD68DB30L, // 131
    0xB454E4A179DD1877L, 0x29BABE4598C311FCL, // 132
    0xE16A1DC9D8545E94L, 0xF4296DD6FEF3D67BL, // 133
    0x8CE2529E2734BB1DL, 0x1899E4A65F58660DL, // 134
    0xB01AE745B101E9E4L, 0x5EC05DCFF72E7F90L, // 135
    0xDC21A1171D42645DL, 0x76707543F4FA1F74L, // 136
    0x899504AE72497EBAL, 0x6A06494A791C53A9L, // 137
    0xABFA45DA0EDBDE69L, 0x0487DB9D17636893L, // 138
    0xD6F8D7509292D603L, 0x45A9D2845D3C42B7L, // 139
    0x865B86925B9BC5C2L, 0x0B8A2392BA45A9B3L, // 140
    0xA7F26836F282B732L, 0x8E6CAC7768D7141FL, // 141
    0xD1EF0244AF2364FFL, 0x3207D795430CD927L, // 142
    0x8335616AED761F1FL, 0x7F44E6BD49E807B9L, // 143
    0xA402B9C5A8D3A6E7L, 0x5F16206C9C6209A7L, // 144
    0xCD036837130890A1L, 0x36DBA887C37A8C10L, // 145
    0x802221226BE55A64L, 0xC2494954DA2C978AL, // 146
    0xA02AA96B06DEB0FDL, 0xF2DB9BAA10B7BD6DL, // 147
    0xC83553C5C8965D3DL, 0x6F92829494E5ACC8L, // 148
    0xFA42A8B73ABBF48CL, 0xCB772339BA1F17FAL, // 149
    0x9C69A97284B578D7L, 0xFF2A760414536EFCL, // 150
    0xC38413CF25E2D70DL, 0xFEF5138519684ABBL, // 151
    0xF46518C2EF5B8CD1L, 0x7EB258665FC25D6AL, // 152
    0x98BF2F79D5993802L, 0xEF2F773FFBD97A62L, // 153
    0xBEEEFB584AFF8603L, 0xAAFB550FFACFD8FBL, // 154
    0xEEAABA2E5DBF6784L, 0x95BA2A53F983CF39L, // 155
    0x952AB45CFA97A0B2L, 0xDD945A747BF26184L, // 156
    0xBA756174393D88DFL, 0x94F971119AEEF9E5L, // 157
    0xE912B9D1478CEB17L, 0x7A37CD5601AAB85EL, // 158
    0x91ABB422CCB812EEL, 0xAC62E055C10AB33BL, // 159
    0xB616A12B7FE617AAL, 0x577B986B314D600AL, // 160
    0xE39C49765FDF9D94L, 0xED5A7E85FDA0B80CL, // 161
    0x8E41ADE9FBEBC27DL, 0x14588F13BE847308L, // 162
    0xB1D219647AE6B31CL, 0x596EB2D8AE258FC9L, // 163
    0xDE469FBD99A05FE3L, 0x6FCA5F8ED9AEF3BCL, // 164
    0x8AEC23D680043BEEL, 0x25DE7BB9480D5855L, // 165
    0xADA72CCC20054AE9L, 0xAF561AA79A10AE6BL, // 166
    0xD910F7FF28069DA4L, 0x1B2BA1518094DA05L, // 167
    0x87AA9AFF79042286L, 0x90FB44D2F05D0843L, // 168
    0xA99541BF57452B28L, 0x353A1607AC744A54L, // 169
    0xD3FA922F2D1675F2L, 0x42889B8997915CE9L, // 170
    0x847C9B5D7C2E09B7L, 0x69956135FEBADA12L, // 171
    0xA59BC234DB398C25L, 0x43FAB9837E699096L, // 172
    0xCF02B2C21207EF2EL, 0x94F967E45E03F4BCL, // 173
    0x8161AFB94B44F57DL, 0x1D1BE0EEBAC278F6L, // 174
    0xA1BA1BA79E1632DCL, 0x6462D92A69731733L, // 175
    0xCA28A291859BBF93L, 0x7D7B8F7503CFDCFFL, // 176
    0xFCB2CB35E702AF78L, 0x5CDA735244C3D43FL, // 177
    0x9DEFBF01B061ADABL, 0x3A0888136AFA64A8L, // 178
    0xC56BAEC21C7A1916L, 0x088AAA1845B8FDD1L, // 179
    0xF6C69A72A3989F5BL, 0x8AAD549E57273D46L, // 180
    0x9A3C2087A63F6399L, 0x36AC54E2F678864CL, // 181
    0xC0CB28A98FCF3C7FL, 0x84576A1BB416A7DEL, // 182
    0xF0FDF2D3F3C30B9FL, 0x656D44A2A11C51D6L, // 183
    0x969EB7C47859E743L, 0x9F644AE5A4B1B326L, // 184
    0xBC4665B596706114L, 0x873D5D9F0DDE1FEFL, // 185
    0xEB57FF22FC0C7959L, 0xA90CB506D155A7EBL, // 186
    0x9316FF75DD87CBD8L, 0x09A7F12442D588F3L, // 187
    0xB7DCBF5354E9BECEL, 0x0C11ED6D538AEB30L, // 188
    0xE5D3EF282A242E81L, 0x8F1668C8A86DA5FBL, // 189
    0x8FA475791A569D10L, 0xF96E017D694487BDL, // 190
    0xB38D92D760EC4455L, 0x37C981DCC395A9ADL, // 191
    0xE070F78D3927556AL, 0x85BBE253F47B1418L, // 192
    0x8C469AB843B89562L, 0x93956D7478CCEC8FL, // 193
    0xAF58416654A6BABBL, 0x387AC8D1970027B3L, // 194
    0xDB2E51BFE9D0696AL, 0x06997B05FCC0319FL, // 195
    0x88FCF317F22241E2L, 0x441FECE3BDF81F04L, // 196
    0xAB3C2FDDEEAAD25AL, 0xD527E81CAD7626C4L, // 197
    0xD60B3BD56A5586F1L, 0x8A71E223D8D3B075L, // 198
    0x85C7056562757456L, 0xF6872D5667844E4AL, // 199
    0xA738C6BEBB12D16CL, 0xB428F8AC016561DCL, // 200
    0xD106F86E69D785C7L, 0xE13336D701BEBA53L, // 201
    0x82A45B450226B39CL, 0xECC0024661173474L, // 202
    0xA34D721642B06084L, 0x27F002D7F95D0191L, // 203
    0xCC20CE9BD35C78A5L, 0x31EC038DF7B441F5L, // 204
    0xFF290242C83396CEL, 0x7E67047175A15272L, // 205
    0x9F79A169BD203E41L, 0x0F0062C6E984D387L, // 206
    0xC75809C42C684DD1L, 0x52C07B78A3E60869L, // 207
    0xF92E0C3537826145L, 0xA7709A56CCDF8A83L, // 208
    0x9BBCC7A142B17CCBL, 0x88A66076400BB692L, // 209
    0xC2ABF989935DDBFEL, 0x6ACFF893D00EA436L, // 210
    0xF356F7EBF83552FEL, 0x0583F6B8C4124D44L, // 211
    0x98165AF37B2153DEL, 0xC3727A337A8B704BL, // 212
    0xBE1BF1B059E9A8D6L, 0x744F18C0592E4C5DL, // 213
    0xEDA2EE1C7064130CL, 0x1162DEF06F79DF74L, // 214
    0x9485D4D1C63E8BE7L, 0x8ADDCB5645AC2BA9L, // 215
    0xB9A74A0637CE2EE1L, 0x6D953E2BD7173693L, // 216
    0xE8111C87C5C1BA99L, 0xC8FA8DB6CCDD0438L, // 217
    0x910AB1D4DB9914A0L, 0x1D9C9892400A22A3L, // 218
    0xB54D5E4A127F59C8L, 0x2503BEB6D00CAB4CL, // 219
    0xE2A0B5DC971F303AL, 0x2E44AE64840FD61EL, // 220
    0x8DA471A9DE737E24L, 0x5CEAECFED289E5D3L, // 221
    0xB10D8E1456105DADL, 0x7425A83E872C5F48L, // 222
    0xDD50F1996B947518L, 0xD12F124E28F7771AL, // 223
    0x8A5296FFE33CC92FL, 0x82BD6B70D99AAA70L, // 224
    0xACE73CBFDC0BFB7BL, 0x636CC64D1001550CL, // 225
    0xD8210BEFD30EFA5AL, 0x3C47F7E05401AA4FL, // 226
    0x8714A775E3E95C78L, 0x65ACFAEC34810A72L, // 227
    0xA8D9D1535CE3B396L, 0x7F1839A741A14D0EL, // 228
    0xD31045A8341CA07CL, 0x1EDE48111209A051L, // 229
    0x83EA2B892091E44DL, 0x934AED0AAB460433L, // 230
    0xA4E4B66B68B65D60L, 0xF81DA84D56178540L, // 231
    0xCE1DE40642E3F4B9L, 0x36251260AB9D668FL, // 232
    0x80D2AE83E9CE78F3L, 0xC1D72B7C6B42601AL, // 233
    0xA1075A24E4421730L, 0xB24CF65B8612F820L, // 234
    0xC94930AE1D529CFCL, 0xDEE033F26797B628L, // 235
    0xFB9B7CD9A4A7443CL, 0x169840EF017DA3B2L, // 236
    0x9D412E0806E88AA5L, 0x8E1F289560EE864FL, // 237
    0xC491798A08A2AD4EL, 0xF1A6F2BAB92A27E3L, // 238
    0xF5B5D7EC8ACB58A2L, 0xAE10AF696774B1DCL, // 239
    0x9991A6F3D6BF1765L, 0xACCA6DA1E0A8EF2AL, // 240
    0xBFF610B0CC6EDD3FL, 0x17FD090A58D32AF4L, // 241
    0xEFF394DCFF8A948EL, 0xDDFC4B4CEF07F5B1L, // 242
    0x95F83D0A1FB69CD9L, 0x4ABDAF101564F98FL, // 243
    0xBB764C4CA7A4440FL, 0x9D6D1AD41ABE37F2L, // 244
    0xEA53DF5FD18D5513L, 0x84C86189216DC5EEL, // 245
    0x92746B9BE2F8552CL, 0x32FD3CF5B4E49BB5L, // 246
    0xB7118682DBB66A77L, 0x3FBC8C33221DC2A2L, // 247
    0xE4D5E82392A40515L, 0x0FABAF3FEAA5334BL, // 248
    0x8F05B1163BA6832DL, 0x29CB4D87F2A7400FL, // 249
    0xB2C71D5BCA9023F8L, 0x743E20E9EF511013L, // 250
    0xDF78E4B2BD342CF6L, 0x914DA9246B255417L, // 251
    0x8BAB8EEFB6409C1AL, 0x1AD089B6C2F7548FL, // 252
    0xAE9672ABA3D0C320L, 0xA184AC2473B529B2L, // 253
    0xDA3C0F568CC4F3E8L, 0xC9E5D72D90A2741FL, // 254
    0x8865899617FB1871L, 0x7E2FA67C7A658893L, // 255
    0xAA7EEBFB9DF9DE8DL, 0xDDBB901B98FEEAB8L, // 256
    0xD51EA6FA85785631L, 0x552A74227F3EA566L, // 257
    0x8533285C936B35DEL, 0xD53A88958F872760L, // 258
    0xA67FF273B8460356L, 0x8A892ABAF368F138L, // 259
    0xD01FEF10A657842CL, 0x2D2B7569B0432D86L, // 260
    0x8213F56A67F6B29BL, 0x9C3B29620E29FC74L, // 261
    0xA298F2C501F45F42L, 0x8349F3BA91B47B90L, // 262
    0xCB3F2F7642717713L, 0x241C70A936219A74L, // 263
    0xFE0EFB53D30DD4D7L, 0xED238CD383AA0111L, // 264
    0x9EC95D1463E8A506L, 0xF4363804324A40ABL, // 265
    0xC67BB4597CE2CE48L, 0xB143C6053EDCD0D6L, // 266
    0xF81AA16FDC1B81DAL, 0xDD94B7868E94050BL, // 267
    0x9B10A4E5E9913128L, 0xCA7CF2B4191C8327L, // 268
    0xC1D4CE1F63F57D72L, 0xFD1C2F611F63A3F1L, // 269
    0xF24A01A73CF2DCCFL, 0xBC633B39673C8CEDL, // 270
    0x976E41088617CA01L, 0xD5BE0503E085D814L, // 271
    0xBD49D14AA79DBC82L, 0x4B2D8644D8A74E19L, // 272
    0xEC9C459D51852BA2L, 0xDDF8E7D60ED1219FL, // 273
    0x93E1AB8252F33B45L, 0xCABB90E5C942B504L, // 274
    0xB8DA1662E7B00A17L, 0x3D6A751F3B936244L, // 275
    0xE7109BFBA19C0C9DL, 0x0CC512670A783AD5L, // 276
    0x906A617D450187E2L, 0x27FB2B80668B24C6L, // 277
    0xB484F9DC9641E9DAL, 0xB1F9F660802DEDF7L, // 278
    0xE1A63853BBD26451L, 0x5E7873F8A0396974L, // 279
    0x8D07E33455637EB2L, 0xDB0B487B6423E1E9L, // 280
    0xB049DC016ABC5E5FL, 0x91CE1A9A3D2CDA63L, // 281
    0xDC5C5301C56B75F7L, 0x7641A140CC7810FCL, // 282
    0x89B9B3E11B6329BAL, 0xA9E904C87FCB0A9EL, // 283
    0xAC2820D9623BF429L, 0x546345FA9FBDCD45L, // 284
    0xD732290FBACAF133L, 0xA97C177947AD4096L, // 285
    0x867F59A9D4BED6C0L, 0x49ED8EABCCCC485EL, // 286
    0xA81F301449EE8C70L, 0x5C68F256BFFF5A75L, // 287
    0xD226FC195C6A2F8CL, 0x73832EEC6FFF3112L, // 288
    0x83585D8FD9C25DB7L, 0xC831FD53C5FF7EACL, // 289
    0xA42E74F3D032F525L, 0xBA3E7CA8B77F5E56L, // 290
    0xCD3A1230C43FB26FL, 0x28CE1BD2E55F35ECL, // 291
    0x80444B5E7AA7CF85L, 0x7980D163CF5B81B4L, // 292
    0xA0555E361951C366L, 0xD7E105BCC3326220L, // 293
    0xC86AB5C39FA63440L, 0x8DD9472BF3FEFAA8L, // 294
    0xFA856334878FC150L, 0xB14F98F6F0FEB952L, // 295
    0x9C935E00D4B9D8D2L, 0x6ED1BF9A569F33D4L, // 296
    0xC3B8358109E84F07L, 0x0A862F80EC4700C9L, // 297
    0xF4A642E14C6262C8L, 0xCD27BB612758C0FBL, // 298
    0x98E7E9CCCFBD7DBDL, 0x8038D51CB897789DL, // 299
    0xBF21E44003ACDD2CL, 0xE0470A63E6BD56C4L, // 300
    0xEEEA5D5004981478L, 0x1858CCFCE06CAC75L, // 301
    0x95527A5202DF0CCBL, 0x0F37801E0C43EBC9L, // 302
    0xBAA718E68396CFFDL, 0xD30560258F54E6BBL, // 303
    0xE950DF20247C83FDL, 0x47C6B82EF32A206AL, // 304
    0x91D28B7416CDD27EL, 0x4CDC331D57FA5442L, // 305
    0xB6472E511C81471DL, 0xE0133FE4ADF8E953L, // 306
    0xE3D8F9E563A198E5L, 0x58180FDDD97723A7L, // 307
    0x8E679C2F5E44FF8FL, 0x570F09EAA7EA7649L, // 308
    0xB201833B35D63F73L, 0x2CD2CC6551E513DBL, // 309
    0xDE81E40A034BCF4FL, 0xF8077F7EA65E58D2L, // 310
    0x8B112E86420F6191L, 0xFB04AFAF27FAF783L, // 311
    0xADD57A27D29339F6L, 0x79C5DB9AF1F9B564L, // 312
    0xD94AD8B1C7380874L, 0x18375281AE7822BDL, // 313
    0x87CEC76F1C830548L, 0x8F2293910D0B15B6L, // 314
    0xA9C2794AE3A3C69AL, 0xB2EB3875504DDB23L, // 315
    0xD433179D9C8CB841L, 0x5FA60692A46151ECL, // 316
    0x849FEEC281D7F328L, 0xDBC7C41BA6BCD334L, // 317
    0xA5C7EA73224DEFF3L, 0x12B9B522906C0801L, // 318
    0xCF39E50FEAE16BEFL, 0xD768226B34870A01L, // 319
    0x81842F29F2CCE375L, 0xE6A1158300D46641L, // 320
    0xA1E53AF46F801C53L, 0x60495AE3C1097FD1L, // 321
    0xCA5E89B18B602368L, 0x385BB19CB14BDFC5L, // 322
    0xFCF62C1DEE382C42L, 0x46729E03DD9ED7B6L, // 323
    0x9E19DB92B4E31BA9L, 0x6C07A2C26A8346D2L  // 324
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

  /**
   * Checks if a character does not require JSON escaping or encoding.
   *
   * @param ch the character to check
   * @return `true` if the character is a basic ASCII character (code point less than `0x80`) that does not need JSON escaping
   */
  final def isNonEscapedAscii(ch: Char): Boolean = ch < 0x80 && escapedChars(ch.toInt) == 0
}