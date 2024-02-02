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
   * Writes an underlying Decimal64 representation as a JSON key.
   *
   * @param x the underlying Decimal64 representation
   * @throws JsonWriterException if the value is non-finite
   */
  def writeDecimal64Key(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    writeDecimal64(x)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `BigInt` value as a JSON key.
   *
   * @param x the `BigInt` value to write
   */
  def writeKey(x: BigInt): Unit = {
    writeOptionalCommaAndIndentionBeforeKey()
    writeBytes('"')
    if (x.isValidLong) writeLong(x.longValue)
    else writeBigInteger(x.bigInteger, null)
    writeParenthesesWithColon()
  }

  /**
   * Writes a `BigDecimal` value as a JSON key.
   *
   * @param x the `BigDecimal` value to write
   */
  def writeKey(x: BigDecimal): Unit = {
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
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 10)
    val buf = this.buf
    if (comma) {
      comma = false
      buf(pos) = ','
      pos += 1
      if (indention != 0) pos = writeIndention(buf, pos, indention)
    }
    buf(pos) = '"'
    pos += 1
    pos = writeString(x, 0, pos, buf, Math.min(x.length, limit - pos - 1) + pos)
    if (pos + 4 >= limit) pos = flushAndGrowBuf(4, pos)
    ByteArrayAccess.setInt(this.buf, pos, 0x203A22)
    if (indention > 0) pos += 1
    count = pos + 2
  }

  /**
   * Writes a `String` value that doesn't require encoding or escaping as a JSON key.
   *
   * @note Use [[JsonWriter.isNonEscapedAscii]] for validation if the string is eligable for writing by this method.
   *
   * @param x the `String` value to write
   */
  def writeNonEscapedAsciiKey(x: String): Unit = {
    val len = x.length
    val indention = this.indention
    val required = indention + len + 10
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
      ByteArrayAccess.setInt(buf, pos, 0x203A22)
      if (indention > 0) pos += 1
      count = pos + 2
    } else writeLongNonEscapedAsciiKey(x)
  }

  /**
   * Writes a [[java.time.Duration]] value as a JSON key.
   *
   * @param x the [[java.time.Duration]] value to write
   */
  def writeKey(x: Duration): Unit = {
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
    writeOptionalCommaAndIndentionBeforeValue()
    writeBigDecimal(x.bigDecimal)
  }

  /**
   * Writes a `BigInt` value as a JSON value.
   *
   * @param x the `BigInt` value to write
   */
  def writeVal(x: BigInt): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    if (x.isValidLong) writeLong(x.longValue)
    else writeBigInteger(x.bigInteger, null)
  }

  /**
   * Writes a [[java.util.UUID]] value as a JSON value.
   *
   * @param x the [[java.util.UUID]] value to write
   */
  def writeVal(x: UUID): Unit = {
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
    val indention = this.indention
    var pos = ensureBufCapacity(indention + 10)
    val buf = this.buf
    if (comma) {
      buf(pos) = ','
      pos += 1
      if (indention != 0) pos = writeIndention(buf, pos, indention)
    } else comma = true
    buf(pos) = '"'
    pos += 1
    pos = writeString(x, 0, pos, buf, Math.min(x.length, limit - pos - 1) + pos)
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
    val len = x.length
    val indention = this.indention
    val required = indention + len + 10
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
    writeOptionalCommaAndIndentionBeforeValue()
    writeDuration(x)
  }

  /**
   * Writes a [[java.time.Instant]] value as a JSON value.
   *
   * @param x the [[java.time.Instant]] value to write
   */
  def writeVal(x: Instant): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeInstant(x)
  }

  /**
   * Writes a [[java.time.LocalDate]] value as a JSON value.
   *
   * @param x the [[java.time.LocalDate]] value to write
   */
  def writeVal(x: LocalDate): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDate(x)
  }

  /**
   * Writes a [[java.time.LocalDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.LocalDateTime]] value to write
   */
  def writeVal(x: LocalDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalDateTime(x)
  }

  /**
   * Writes a [[java.time.LocalTime]] value as a JSON value.
   *
   * @param x the [[java.time.LocalTime]] value to write
   */
  def writeVal(x: LocalTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeLocalTime(x)
  }

  /**
   * Writes a [[java.time.MonthDay]] value as a JSON value.
   *
   * @param x the [[java.time.MonthDay]] value to write
   */
  def writeVal(x: MonthDay): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeMonthDay(x)
  }

  /**
   * Writes a [[java.time.OffsetDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.OffsetDateTime]] value to write
   */
  def writeVal(x: OffsetDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetDateTime(x)
  }

  /**
   * Writes a [[java.time.OffsetTime]] value as a JSON value.
   *
   * @param x the [[java.time.OffsetTime]] value to write
   */
  def writeVal(x: OffsetTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeOffsetTime(x)
  }

  /**
   * Writes a [[java.time.Period]] value as a JSON value.
   *
   * @param x the [[java.time.Period]] value to write
   */
  def writeVal(x: Period): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writePeriod(x)
  }

  /**
   * Writes a [[java.time.Year]] value as a JSON value.
   *
   * @param x the [[java.time.Year]] value to write
   */
  def writeVal(x: Year): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeYear(x)
  }

  /**
   * Writes a [[java.time.YearMonth]] value as a JSON value.
   *
   * @param x the [[java.time.YearMonth]] value to write
   */
  def writeVal(x: YearMonth): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeYearMonth(x)
  }

  /**
   * Writes a [[java.time.ZonedDateTime]] value as a JSON value.
   *
   * @param x the [[java.time.ZonedDateTime]] value to write
   */
  def writeVal(x: ZonedDateTime): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeZonedDateTime(x)
  }

  /**
   * Writes a [[java.time.ZoneId]] value as a JSON value.
   *
   * @param x the [[java.time.ZoneId]] value to write
   */
  def writeVal(x: ZoneId): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeZoneId(x)
  }

  /**
   * Writes a [[java.time.ZoneOffset]] value as a JSON value.
   *
   * @param x the [[java.time.ZoneOffset]] value to write
   */
  def writeVal(x: ZoneOffset): Unit = {
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
   * Writes an underlying Decimal64 representation as a JSON number.
   *
   * @param x the underlying Decimal64 representation
   * @throws JsonWriterException if the value is non-finite
   */
  def writeDecimal64Val(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeDecimal64(x)
  }

  /**
   * Writes a `BigDecimal` value as a JSON string value.
   *
   * @param x the `BigDecimal` value to write
   */
  def writeValAsString(x: BigDecimal): Unit = {
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
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    if (x.isValidLong) writeLong(x.longValue)
    else writeBigInteger(x.bigInteger, null)
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
   * Writes an underlying Decimal64 representation as a JSON number.
   *
   * @param x the underlying Decimal64 representation
   * @throws JsonWriterException if the value is non-finite
   */
  def writeDecimal64ValAsString(x: Long): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeBytes('"')
    writeDecimal64(x)
    writeBytes('"')
  }

  /**
   * Writes a byte array as a JSON hexadecimal string value.
   *
   * @param bs the byte array to write
   * @param lowerCase if `true`, outputs lowercase hexadecimal digits
   */
  def writeBase16Val(bs: Array[Byte], lowerCase: Boolean): Unit = {
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
    writeOptionalCommaAndIndentionBeforeValue()
    writeBase64Bytes(bs, base64UrlDigits, doPadding)
  }

  /**
   * Writes a byte array as a JSON raw binary value.
   *
   * @param bs the byte array to write
   */
  def writeRawVal(bs: Array[Byte]): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    writeRawBytes(bs)
  }

  /**
   * Writes a JSON `null` value.
   */
  def writeNull(): Unit = {
    writeOptionalCommaAndIndentionBeforeValue()
    val pos = ensureBufCapacity(4)
    ByteArrayAccess.setInt(buf, pos, 0x6C6C756E)
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

  private[this] def writeIndention(): Unit = {
    val n = indention
    val pos = ensureBufCapacity(n + 8)
    count = writeIndention(buf, pos, n)
  }

  private[this] def writeIndention(buf: Array[Byte], p: Int, n: Int): Int = {
    var pos = p
    buf(pos) = '\n'
    pos += 1
    val posLim = pos + n
    while (pos < posLim) {
      ByteArrayAccess.setLong(buf, pos, 0x2020202020202020L)
      pos += 8
    }
    posLim
  }

  private[this] def writeParenthesesWithColon(): Unit = {
    var pos = ensureBufCapacity(4) // 4 == size of Int in bytes
    ByteArrayAccess.setInt(buf, pos, 0x203A22)
    if (indention > 0) pos += 1
    count = pos + 2
  }

  private[this] def writeColon(): Unit = {
    var pos = ensureBufCapacity(2)
    ByteArrayAccess.setShort(buf, pos, 0x203A)
    if (indention > 0) pos += 1
    count = pos + 1
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
        val d2 = ds(bs(offset + 1) & 0xFF) << 16
        ByteArrayAccess.setInt(buf, pos, d1 | d2)
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
    count = pos + 1
  }

  private[this] def writeBase64Bytes(bs: Array[Byte], ds: Array[Byte], doPadding: Boolean): Unit = {
    val lenM3 = bs.length - 3
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
    while (offset < lenM3) {
      val offsetLim = Math.min((posLim - pos + 3 >> 2) * 3 + offset, lenM3)
      while (offset < offsetLim) {
        val p = ByteArrayAccess.getIntReversed(bs, offset)
        ByteArrayAccess.setInt(buf, pos,
          ds(p >> 8 & 0x3F) << 24 | ds(p >> 14 & 0x3F) << 16 | ds(p >> 20 & 0x3F) << 8 | ds(p >>> 26))
        pos += 4
        offset += 3
      }
      if (pos >= posLim) {
        pos = flushAndGrowBuf(5, pos)
        buf = this.buf
        posLim = limit - 5
      }
    }
    if (offset == lenM3) {
      val p = (bs(offset) & 0xFF) << 16 | (bs(offset + 1) & 0xFF) << 8 | (bs(offset + 2) & 0xFF)
      ByteArrayAccess.setInt(buf, pos,
        ds(p & 0x3F) << 24 | ds(p >> 6 & 0x3F) << 16 | ds(p >> 12 & 0x3F) << 8 | ds(p >> 18))
      pos += 4
    } else if (offset == lenM3 + 1) {
      val p = (bs(offset) & 0xFF) << 10 | (bs(offset + 1) & 0xFF) << 2
      ByteArrayAccess.setInt(buf, pos, ds(p & 0x3F) << 16 | ds(p >> 6 & 0x3F) << 8 | ds(p >> 12) | 0x3D000000)
      pos += 3
      if (doPadding) pos += 1
    } else if (offset == lenM3 + 2) {
      val p = bs(offset)
      ByteArrayAccess.setInt(buf, pos, ds(p << 4 & 0x3F) << 8 | ds(p >> 2 & 0x3F) | 0x3D3D0000)
      pos += 2
      if (doPadding) pos += 2
    }
    buf(pos) = '"'
    count = pos + 1
  }

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

  private[this] def writeZoneId(x: ZoneId): Unit = {
    val s = x.getId
    val len = s.length
    var pos = ensureBufCapacity(len + 2)
    val buf = this.buf
    buf(pos) = '"'
    pos += 1
    s.getBytes(0, len, buf, pos)
    pos += len
    buf(pos) = '"'
    count = pos + 1
  }

  private[this] def writeUUID(mostSigBits: Long, leastSigBits: Long): Unit = {
    val pos = ensureBufCapacity(40) // 40 == 5 * size of Long in bytes
    val buf = this.buf
    val ds = lowerCaseHexDigits
    val mostSigBits1 = (mostSigBits >> 32).toInt
    val d1 = ds(mostSigBits1 >>> 24) << 8
    val d2 = ds(mostSigBits1 >> 16 & 0xFF).toLong << 24
    val d3 = ds(mostSigBits1 >> 8 & 0xFF).toLong << 40
    val d4 = ds(mostSigBits1 & 0xFF)
    ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3 | d4.toLong << 56 | 0x22)
    val mostSigBits2 = mostSigBits.toInt
    val d5 = ds(mostSigBits2 >>> 24) << 16
    val d6 = ds(mostSigBits2 >> 16 & 0xFF).toLong << 32
    val d7 = ds(mostSigBits2 >> 8 & 0xFF)
    ByteArrayAccess.setLong(buf, pos + 8, d4 >> 8 | d5 | d6 | d7.toLong << 56 | 0x2D000000002D00L)
    val d8 = ds(mostSigBits2 & 0xFF) << 8
    val leastSigBits1 = (leastSigBits >> 32).toInt
    val d9 = ds(leastSigBits1 >>> 24).toLong << 32
    val d10 = ds(leastSigBits1 >> 16 & 0xFF).toLong << 48
    ByteArrayAccess.setLong(buf, pos + 16, d7 >> 8 | d8 | d9 | d10 | 0x2D000000)
    val d11 = ds(leastSigBits1 >> 8 & 0xFF) << 8
    val d12 = ds(leastSigBits1 & 0xFF).toLong << 24
    val leastSigBits2 = leastSigBits.toInt
    val d13 = ds(leastSigBits2 >>> 24).toLong << 40
    val d14 = ds(leastSigBits2 >> 16 & 0xFF)
    ByteArrayAccess.setLong(buf, pos + 24, d11 | d12| d13 | d14.toLong << 56 | 0x2D)
    val d15 = ds(leastSigBits2 >> 8 & 0xFF) << 8
    val d16 = ds(leastSigBits2 & 0xFF).toLong << 24
    ByteArrayAccess.setLong(buf, pos + 32, d14 >> 8 | d15 | d16 | 0x220000000000L)
    count = pos + 38
  }

  @tailrec
  private[this] def writeString(s: String, from: Int, pos: Int, buf: Array[Byte], minLim: Int): Int =
    if (pos < minLim) {
      val ch = s.charAt(from).toInt
      buf(pos) = ch.toByte
      if (ch >= 0x20 && ch < 0x7F && ch != 0x22 && ch != 0x5C) writeString(s, from + 1, pos + 1, buf, minLim)
      else writeEscapedOrEncodedString(s, from, pos)
    } else if (s.length == from) pos
    else {
      val newPos = flushAndGrowBuf(2, pos)
      writeString(s, from, newPos, this.buf, Math.min(s.length - from, limit - newPos - 1) + newPos)
    }

  private[this] def writeEscapedOrEncodedString(s: String, from: Int, pos: Int): Int =
    if (config.escapeUnicode) writeEscapedString(s, from, s.length, pos, limit - 13, escapedChars)
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
        if ((ch2 & 0xFC00) != 0xDC00) illegalSurrogateError()
        val cp = (ch1 << 10) + (ch2 - 56613888) // -56613888 == 0x10000 - (0xD800 << 10) - 0xDC00
        ByteArrayAccess.setInt(buf, pos, cp >> 18 | (cp >> 4 & 0x3F00) | (cp << 10 & 0x3F0000) | (cp << 24 & 0x3F000000) | 0x808080F0)
        writeEncodedString(s, from + 2, to, pos + 4, posLim, escapedChars)
      }
    }

  @tailrec
  private[this] def writeEscapedString(s: String, from: Int, to: Int, pos: Int, posLim: Int, escapedChars: Array[Byte]): Int =
    if (from >= to) pos
    else if (pos >= posLim) writeEscapedString(s, from, to, flushAndGrowBuf(13, pos), limit - 12, escapedChars)
    else {
      val ch1 = s.charAt(from).toInt
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
        val ch2 = s.charAt(from + 1).toInt
        if ((ch2 & 0xFC00) != 0xDC00) illegalSurrogateError()
        writeEscapedString(s, from + 2, to, writeEscapedUnicode(ch2, writeEscapedUnicode(ch1, pos, buf), buf), posLim, escapedChars)
      }
    }

  private[this] def writeChar(ch: Int): Unit = {
    var pos = ensureBufCapacity(8) // 8 = size of Long in bytes
    if (ch < 0x80) {
      val esc = escapedChars(ch)
      if (esc == 0) { // 000000000aaaaaaa (UTF-16 char) -> 0aaaaaaa (UTF-8 byte)
        ByteArrayAccess.setInt(buf, pos, ch << 8 | 0x220022)
        pos += 3
      } else if (esc > 0) {
        ByteArrayAccess.setInt(buf, pos, esc << 16 | 0x22005C22)
        pos += 4
      } else {
        val ds = lowerCaseHexDigits
        ByteArrayAccess.setLong(buf, pos, ds(ch).toLong << 40 | 0x2200003030755C22L)
        pos += 8
      }
    } else if (config.escapeUnicode) {
      if (ch >= 0xD800 && ch <= 0xDFFF) illegalSurrogateError()
      val ds = lowerCaseHexDigits
      val d1 = ds(ch >> 8).toLong << 24
      val d2 = ds(ch & 0xFF).toLong << 40
      ByteArrayAccess.setLong(buf, pos, d1 | d2 | 0x2200000000755C22L)
      pos += 8
    } else if (ch < 0x800) { // 00000bbbbbaaaaaa (UTF-16 char) -> 110bbbbb 10aaaaaa (UTF-8 bytes)
      ByteArrayAccess.setInt(buf, pos, (ch & 0x3F) << 16 | (ch & 0xFC0) << 2 | 0x2280C022)
      pos += 4
    } else if (ch < 0xD800 || ch > 0xDFFF) { // ccccbbbbbbaaaaaa (UTF-16 char) -> 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes)
      ByteArrayAccess.setLong(buf, pos, ((ch & 0x3F) << 24 | (ch & 0xFC0) << 10 | (ch & 0xF000) >> 4) | 0x228080E022L)
      pos += 5
    } else illegalSurrogateError()
    count = pos
  }

  private[this] def writeEscapedUnicode(ch: Int, pos: Int, buf: Array[Byte]): Int = {
    val ds = lowerCaseHexDigits
    ByteArrayAccess.setShort(buf, pos, 0x755C)
    val d1 = ds(ch >> 8)
    val d2 = ds(ch & 0xFF) << 16
    ByteArrayAccess.setInt(buf, pos + 2, d1 | d2)
    pos + 6
  }

  private[this] def writeEscapedUnicode(b: Byte, pos: Int, buf: Array[Byte]): Int = {
    val ds = lowerCaseHexDigits
    ByteArrayAccess.setInt(buf, pos, 0x3030755C)
    ByteArrayAccess.setShort(buf, pos + 4, ds(b & 0xFF))
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
      var m: Short = 0x2B45
      if (exp < 0) {
        m = 0x2D45
        exp = -exp
      }
      ByteArrayAccess.setShort(buf, pos, m)
      pos += 2
      var q = 0
      if (exp < 100000000) {
        q = exp.toInt
        pos += digitCount(exp)
        count = pos
      } else {
        val q1 = (exp >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
        q = q1.toInt
        pos += digitCount(q1)
        count = write8Digits(exp - q1 * 100000000, pos, buf, ds)
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
      val digits = (v >> 63).toInt + lastPos - pos
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

  private[this] def calculateTenPow18SquareNumber(bitLen: Int): Int = {
    val m = Math.max((bitLen * 71828554L >> 32).toInt - 1, 1) // Math.max((x.bitLength * Math.log(2) / Math.log(1e18)).toInt - 1, 1)
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
    ByteArrayAccess.setShort(buf, dotPos - 1, 0x2E30)
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
    var pos = ensureBufCapacity(8) // bytes in Long
    if (x) {
      ByteArrayAccess.setInt(buf, pos, 0x65757274)
      pos += 4
    } else {
      ByteArrayAccess.setLong(buf, pos, 0x65736c6166L)
      pos += 5
    }
    count = pos
  }

  private[this] def writeByte(x: Byte): Unit = {
    var pos = ensureBufCapacity(5) // size of Int in bytes + one byte for the sign
    val buf = this.buf
    val ds = digits
    var q0 = x.toInt
    if (q0 < 0) {
      q0 = -q0
      buf(pos) = '-'
      pos += 1
    }
    if (q0 < 10) {
      buf(pos) = (q0 + '0').toByte
      pos += 1
    } else if (q0 < 100) {
      ByteArrayAccess.setShort(buf, pos, ds(q0))
      pos += 2
    } else {
      ByteArrayAccess.setInt(buf, pos, ds(q0 - 100) << 8 | 0x31)
      pos += 3
    }
    count = pos
  }

  private[this] def writeDuration(x: Duration): Unit = {
    var pos = ensureBufCapacity(40) // 40 == "PT-1111111111111111H-11M-11.111111111S".length + 2
    val buf = this.buf
    val totalSecs = x.getSeconds
    var nano = x.getNano
    ByteArrayAccess.setLong(buf, pos, 0x225330545022L)
    if ((totalSecs | nano) == 0) pos += 6
    else {
      pos += 3
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
        var q = 0
        var lastPos = pos
        if (hours < 100000000) {
          q = hours.toInt
          lastPos += digitCount(hours)
          pos = lastPos
        } else {
          val q1 = Math.multiplyHigh(hours, 6189700196426901375L) >>> 25 // divide a positive long by 100000000
          q = q1.toInt
          lastPos += digitCount(q1)
          pos = write8Digits(hours - q1 * 100000000, lastPos, buf, ds)
        }
        writePositiveIntDigits(q, lastPos, buf, ds)
        ByteArrayAccess.setShort(buf, pos, 0x2248)
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
        ByteArrayAccess.setShort(buf, pos, 0x224D)
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
        ByteArrayAccess.setShort(buf, pos, 0x2253)
        pos += 1
      }
      pos += 1
    }
    count = pos
  }

  private[this] def writeInstant(x: Instant): Unit = {
    val epochSecond = x.getEpochSecond
    if (epochSecond < 0) writeBeforeEpochInstant(epochSecond, x.getNano)
    else {
      val epochDay = Math.multiplyHigh(epochSecond, 1749024623285053783L) >> 13 // epochSecond / 86400
      val marchZeroDay = epochDay + 719468  // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
      var year = (Math.multiplyHigh(marchZeroDay * 400 + 591, 4137408090565272301L) >> 15).toInt // ((marchZeroDay * 400 + 591) / 146097).toInt
      var year365 = year * 365L
      var year1374389535 = year * 1374389535L
      var century = (year1374389535 >> 37).toInt
      var marchDayOfYear = (marchZeroDay - year365).toInt - (year >> 2) + century - (century >> 2)
      if (marchDayOfYear < 0) {
        year365 -= 365
        year1374389535 -= 1374389535
        year -= 1
        century = (year1374389535 >> 37).toInt
        marchDayOfYear = (marchZeroDay - year365).toInt - (year >> 2) + century - (century >> 2)
      }
      val marchMonth = marchDayOfYear * 17135 + 6854 >> 19 // (marchDayOfYear * 5 + 2) / 153
      val day = marchDayOfYear - (marchMonth * 1002762 - 16383 >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
      val m = 9 - marchMonth >> 4
      val month = (m & -9 | 3) + marchMonth
      year -= m
      writeInstant(year, month, day, (epochSecond - epochDay * 86400).toInt, x.getNano)
    }
  }

  private[this] def writeBeforeEpochInstant(epochSecond: Long, nano: Int): Unit = {
    val epochDay = (Math.multiplyHigh(epochSecond - 86399, 1749024623285053783L) >> 13) + 1 // (epochSecond - 86399) / 86400
    var marchZeroDay = epochDay + 719468  // 719468 == 719528 - 60 == days 0000 to 1970 - days 1st Jan to 1st Mar
    val adjust400YearCycles = ((marchZeroDay + 1) * 7525902 >> 40).toInt // ((marchZeroDay + 1) / 146097).toInt - 1
    marchZeroDay -= adjust400YearCycles * 146097L
    var year = { // ((marchZeroDay * 400 + 591) / 146097).toInt
      val pa = marchZeroDay * 400 + 591
      ((Math.multiplyHigh(pa, 4137408090565272301L) >> 15) + (pa >> 63)).toInt
    }
    var year365 = year * 365L
    var year1374389535 = year * 1374389535L
    var century = (year1374389535 >> 37).toInt
    var marchDayOfYear = (marchZeroDay - year365).toInt - (year >> 2) + century - (century >> 2)
    if (marchDayOfYear < 0) {
      year365 -= 365
      year1374389535 -= 1374389535
      year -= 1
      century = (year1374389535 >> 37).toInt
      marchDayOfYear = (marchZeroDay - year365).toInt - (year >> 2) + century - (century >> 2)
    }
    val marchMonth = marchDayOfYear * 17135 + 6854 >> 19 // (marchDayOfYear * 5 + 2) / 153
    val day = marchDayOfYear - (marchMonth * 1002762 - 16383 >> 15) // marchDayOfYear - (marchMonth * 306 + 5) / 10 + 1
    val m = 9 - marchMonth >> 4
    val month = (m & -9 | 3) + marchMonth
    year += adjust400YearCycles * 400 - m
    writeInstant(year, month, day, (epochSecond - epochDay * 86400).toInt, nano)
  }

  private[this] def writeInstant(year: Int, month: Int, day: Int, secsOfDay: Int, nano: Int): Unit = {
    var pos = ensureBufCapacity(39) // 39 == Instant.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeYear(year, pos + 1, buf, ds)
    ByteArrayAccess.setLong(buf, pos, ds(month) << 8 | ds(day).toLong << 32 | 0x5400002D00002DL)
    pos += 7
    val y1 = secsOfDay * 37283 // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0x7FFFFFF) * 15
    val y3 = (y2 & 0x1FFFFFF) * 15
    ByteArrayAccess.setLong(buf, pos, ds(y1 >>> 27) | ds(y2 >> 25).toLong << 24 | ds(y3 >> 23).toLong << 48 | 0x3A00003A0000L)
    pos += 8
    if (nano != 0) pos = writeNanos(nano, pos, buf, ds)
    ByteArrayAccess.setShort(buf, pos, 0x225A)
    count = pos + 2
  }

  private[this] def writeLocalDate(x: LocalDate): Unit = {
    var pos = ensureBufCapacity(19) // 19 == java.time.Year.MAX_VALUE.toString.length + 9
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeYear(x.getYear, pos + 1, buf, ds)
    val d1 = ds(x.getMonthValue) << 8
    val d2 = ds(x.getDayOfMonth).toLong << 32
    ByteArrayAccess.setLong(buf, pos, d1 | d2 | 0x2200002D00002DL)
    count = pos + 7
  }

  private[this] def writeLocalDateTime(x: LocalDateTime): Unit = {
    var pos = ensureBufCapacity(37) // 37 == LocalDateTime.MAX.toString.length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeLocalTime(x.toLocalTime, writeLocalDateWithT(x.toLocalDate, pos + 1, buf, ds), buf, ds)
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
    val d1 = ds(x.getMonthValue) << 16
    val d2 = ds(x.getDayOfMonth).toLong << 40
    ByteArrayAccess.setLong(buf, pos + 1, d1 | d2 | 0x2200002D00002D2DL)
    count = pos + 9
  }

  private[this] def writeOffsetDateTime(x: OffsetDateTime): Unit = {
    val pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    count = writeOffset(x.getOffset,
      writeLocalTime(x.toLocalTime, writeLocalDateWithT(x.toLocalDate, pos + 1, buf, ds), buf, ds), buf, ds)
  }

  private[this] def writeOffsetTime(x: OffsetTime): Unit = {
    val pos = ensureBufCapacity(29) // 29 == "00:00:07.999999998+00:00:08".length + 2
    val buf = this.buf
    val ds = digits
    buf(pos) = '"'
    count = writeOffset(x.getOffset, writeLocalTime(x.toLocalTime, pos + 1, buf, ds), buf, ds)
  }

  private[this] def writePeriod(x: Period): Unit = {
    var pos = ensureBufCapacity(39) // 39 == "P-2147483648Y-2147483648M-2147483648D".length + 2
    val buf = this.buf
    val years = x.getYears
    val months = x.getMonths
    val days = x.getDays
    ByteArrayAccess.setLong(buf, pos, 0x2244305022L)
    if ((years | months | days) == 0) pos += 5
    else {
      pos += 2
      val ds = digits
      if (years != 0) pos = writePeriod(years, pos, buf, ds, 0x2259)
      if (months != 0) pos = writePeriod(months, pos, buf, ds, 0x224D)
      if (days != 0) pos = writePeriod(days, pos, buf, ds, 0x2244)
      pos += 1
    }
    count = pos
  }

  private[this] def writePeriod(x: Int, p: Int, buf: Array[Byte], ds: Array[Short], bs: Short): Int = {
    var pos = p
    var q0 = x
    if (x < 0) {
      q0 = -q0
      if (q0 != x) {
        buf(pos) = '-'
        pos += 1
      } else {
        q0 = 147483648
        ByteArrayAccess.setShort(buf, pos, 0x322D)
        pos += 2
      }
    }
    pos += digitCount(q0.toLong)
    writePositiveIntDigits(q0, pos, buf, ds)
    ByteArrayAccess.setShort(buf, pos, bs)
    pos + 1
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
    ByteArrayAccess.setInt(buf, pos, ds(x.getMonthValue) << 8 | 0x2200002D)
    count = pos + 4
  }

  private[this] def writeZonedDateTime(x: ZonedDateTime): Unit = {
    var pos = ensureBufCapacity(46) // 46 == "+999999999-12-31T23:59:59.999999999+00:00:01".length + 2
    var buf = this.buf
    val ds = digits
    buf(pos) = '"'
    pos = writeOffset(x.getOffset,
      writeLocalTime(x.toLocalTime, writeLocalDateWithT(x.toLocalDate, pos + 1, buf, ds), buf, ds), buf, ds)
    val zone = x.getZone
    if (!zone.isInstanceOf[ZoneOffset]) {
      val zoneId = zone.getId
      val len = zoneId.length
      val required = len + 3
      if (pos + required > limit) {
        pos = flushAndGrowBuf(required, pos)
        buf = this.buf
      }
      buf(pos - 1) = '['
      zoneId.getBytes(0, len, buf, pos)
      pos += len
      ByteArrayAccess.setShort(buf, pos, 0x225D)
      pos += 2
    }
    count = pos
  }

  private[this] def writeZoneOffset(x: ZoneOffset): Unit = {
    var pos = ensureBufCapacity(12) // 12 == number of bytes in Long and Int
    val buf = this.buf
    var y = x.getTotalSeconds
    if (y == 0) {
      ByteArrayAccess.setInt(buf, pos, 0x225A22)
      pos += 3
    } else {
      val ds = digits
      var m = 0x2230303A00002B22L
      if (y < 0) {
        y = -y
        m = 0x2230303A00002D22L
      }
      y *= 37283 // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      m |= ds(y >>> 27) << 16
      if ((y & 0x7FF8000) == 0) { // check if totalSeconds is divisible by 3600
        ByteArrayAccess.setLong(buf, pos, m)
        pos += 8
      } else {
        y &= 0x7FFFFFF
        y *= 15
        ByteArrayAccess.setLong(buf, pos, ds(y >> 25).toLong << 40 | m)
        if ((y & 0x1F80000) == 0) pos += 8 // check if totalSeconds is divisible by 60
        else {
          ByteArrayAccess.setInt(buf, pos + 7, ds((y & 0x1FFFFFF) * 15 >> 23) << 8 | 0x2200003A)
          pos += 11
        }
      }
    }
    count = pos
  }

  private[this] def writeLocalDateWithT(x: LocalDate, p: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val pos = writeYear(x.getYear, p, buf, ds)
    val d1 = ds(x.getMonthValue) << 8
    val d2 = ds(x.getDayOfMonth).toLong << 32
    ByteArrayAccess.setLong(buf, pos, d1 | d2 | 0x5400002D00002DL)
    pos + 7
  }

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
      pos += digitCount(q0.toLong)
      writePositiveIntDigits(q0, pos, buf, ds)
      pos
    }
  }

  private[this] def writeLocalTime(x: LocalTime, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val second = x.getSecond
    val nano = x.getNano
    val d1 = ds(x.getHour) | 0x3A00003A0000L
    val d2 = ds(x.getMinute).toLong << 24
    if ((second | nano) == 0) {
      ByteArrayAccess.setLong(buf, pos, d1 | d2)
      pos + 5
    } else {
      val d3 = ds(second).toLong << 48
      ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3)
      if (nano == 0) pos + 8
      else writeNanos(nano, pos + 8, buf, ds)
    }
  }

  private[this] def writeNanos(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val y1 = x * 1441151881L // Based on James Anhalt's algorithm for 9 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0x1FFFFFFFFFFFFFFL) * 100
    var m = y1 >>> 57 << 8 | ds((y2 >>> 57).toInt) << 16 | 0x302E
    if ((y2 & 0x1FFFFF800000000L) == 0) { // check if q0 is divisible by 1000000
      ByteArrayAccess.setInt(buf, pos, m.toInt)
      pos + 4
    } else {
      val y3 = (y2 & 0x1FFFFFFFFFFFFFFL) * 100
      val y4 = (y3 & 0x1FFFFFFFFFFFFFFL) * 100
      m |= ds((y3 >>> 57).toInt).toLong << 32
      val d = ds((y4 >>> 57).toInt)
      ByteArrayAccess.setLong(buf, pos, m | d.toLong << 48)
      if ((y4 & 0x1FF000000000000L) == 0 && d <= 0x3039) pos + 7 // check if x is divisible by 1000
      else {
        ByteArrayAccess.setShort(buf, pos + 8, ds(((y4 & 0x1FFFFFFFFFFFFFFL) * 100 >>> 57).toInt))
        pos + 10
      }
    }
  }

  private[this] def writeOffset(x: ZoneOffset, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var y = x.getTotalSeconds
    if (y == 0) {
      ByteArrayAccess.setShort(buf, pos, 0x225A)
      pos + 2
    } else {
      var m = 0x2230303A00002BL
      if (y < 0) {
        y = -y
        m = 0x2230303A00002DL
      }
      y *= 37283 // Based on James Anhalt's algorithm: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      m |= ds(y >>> 27) << 8
      if ((y & 0x7FF8000) == 0) { // check if totalSeconds is divisible by 3600
        ByteArrayAccess.setLong(buf, pos, m)
        pos + 7
      } else {
        y &= 0x7FFFFFF
        y *= 15
        ByteArrayAccess.setLong(buf, pos, ds(y >> 25).toLong << 32 | m)
        if ((y & 0x1F80000) == 0) pos + 7 // check if totalSeconds is divisible by 60
        else {
          ByteArrayAccess.setInt(buf, pos + 6, ds((y & 0x1FFFFFF) * 15 >> 23) << 8 | 0x2200003A)
          pos + 10
        }
      }
    }
  }

  private[this] def write2Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    ByteArrayAccess.setShort(buf, pos, ds(x))
    pos + 2
  }

  private[this] def write3Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = x * 1311 >> 17 // divide a small positive int by 100
    ByteArrayAccess.setInt(buf, pos, ds(x - q1 * 100) << 8 | q1 + '0')
    pos + 3
  }

  private[this] def write4Digits(x: Int, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = x * 5243 >> 19 // divide a small positive int by 100
    val d1 = ds(x - q1 * 100) << 16
    val d2 = ds(q1)
    ByteArrayAccess.setInt(buf, pos, d1 | d2)
    pos + 4
  }

  private[this] def write8Digits(x: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val y1 = x * 140737489 // Based on James Anhalt's algorithm for 8 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
    val y2 = (y1 & 0x7FFFFFFFFFFFL) * 100
    val y3 = (y2 & 0x7FFFFFFFFFFFL) * 100
    val y4 = (y3 & 0x7FFFFFFFFFFFL) * 100
    val d1 = ds((y1 >> 47).toInt)
    val d2 = ds((y2 >> 47).toInt) << 16
    val d3 = ds((y3 >> 47).toInt).toLong << 32
    val d4 = ds((y4 >> 47).toInt).toLong << 48
    ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3 | d4)
    pos + 8
  }

  private[this] def write18Digits(x: Long, pos: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    val q1 = Math.multiplyHigh(x, 6189700196426901375L) >>> 25 // divide a positive long by 100000000
    write8Digits(x - q1 * 100000000, {
      val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
      write8Digits(q1 - q2 * 100000000, write2Digits(q2.toInt, pos, buf, ds), buf, ds)
    }, buf, ds)
  }

  private[this] def writeShort(x: Short): Unit = {
    var pos = ensureBufCapacity(9) // 8 bytes in long + a byte for the sign
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
        buf(pos) = (q0 + '0').toByte
        pos += 1
      } else {
        ByteArrayAccess.setShort(buf, pos, ds(q0))
        pos += 2
      }
    } else if (q0 < 10000) {
      val q1 = q0 * 5243 >> 19 // divide a small positive int by 100
      val d2 = ds(q0 - q1 * 100)
      if (q0 < 1000) {
        ByteArrayAccess.setInt(buf, pos, q1 + '0' | d2 << 8)
        pos += 3
      } else {
        ByteArrayAccess.setInt(buf, pos, ds(q1) | d2 << 16)
        pos += 4
      }
    } else {
      val y1 = q0 * 429497L // Based on James Anhalt's algorithm for 5 digits: https://jk-jeon.github.io/posts/2022/02/jeaiii-algorithm/
      val y2 = (y1 & 0xFFFFFFFFL) * 100
      val y3 = (y2 & 0xFFFFFFFFL) * 100
      val d1 = (y1 >> 32).toInt + '0'
      val d2 = ds((y2 >> 32).toInt) << 8
      val d3 = ds((y3 >> 32).toInt).toLong << 24
      ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3)
      pos += 5
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
      if (q0 != x) {
        buf(pos) = '-'
        pos += 1
      } else {
        q0 = 147483648
        ByteArrayAccess.setShort(buf, pos, 0x322D)
        pos += 2
      }
    }
    pos += digitCount(q0.toLong)
    writePositiveIntDigits(q0, pos, buf, ds)
    count = pos
  }

  private[this] def writeLong(x: Long): Unit =
    count = writeLong(x, ensureBufCapacity(20), buf) // Long.MinValue.toString.length

  private[this] def writeLong(x: Long, p: Int, buf: Array[Byte]): Int = {
    var pos = p
    val ds = digits
    var q0 = x
    if (x < 0) {
      q0 = -q0
      if (q0 != x) {
        buf(pos) = '-'
        pos += 1
      } else {
        q0 = 3372036854775808L
        ByteArrayAccess.setInt(buf, pos, 0x3232392D)
        pos += 4
      }
    }
    var q = 0
    var lastPos = pos
    if (q0 < 100000000) {
      q = q0.toInt
      lastPos += digitCount(q0)
      pos = lastPos
    } else {
      val q1 = Math.multiplyHigh(q0, 6189700196426901375L) >>> 25 // divide a positive long by 100000000
      if (q1 < 100000000) {
        q = q1.toInt
        lastPos += digitCount(q1)
        pos = lastPos
      } else {
        val q2 = (q1 >> 8) * 1441151881 >> 49 // divide a small positive long by 100000000
        q = q2.toInt
        lastPos += digitCount(q2)
        pos = write8Digits(q1 - q2 * 100000000, lastPos, buf, ds)
      }
      pos = write8Digits(q0 - q1 * 100000000, pos, buf, ds)
    }
    writePositiveIntDigits(q, lastPos, buf, ds)
    pos
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/FloatToDecimal.java
  private[this] def writeFloat(x: Float): Unit = {
    var pos = ensureBufCapacity(15)
    val buf = this.buf
    if (x < 0.0f) {
      buf(pos) = '-'
      pos += 1
    }
    if (x == 0.0f) {
      ByteArrayAccess.setInt(buf, pos, 0x302E30)
      pos += 3
    } else {
      val bits = java.lang.Float.floatToRawIntBits(x)
      var e2 = (bits >> 23 & 0xFF) - 150
      var m2 = bits & 0x7FFFFF | 0x800000
      var m10, e10 = 0
      if (e2 == 0) m10 = m2
      else if ((e2 >= -23 && e2 < 0) && m2 << e2 == 0) m10 = m2 >> -e2
      else {
        var e10Corr, e2Corr = 0
        var cblCorr = 2
        if (e2 == -150) {
          m2 &= 0x7FFFFF
          e2 = -149
          if (m2 < 8) {
            m2 *= 10
            e10Corr = 1
          }
        } else if (e2 == 105) illegalNumberError(x)
        else if (m2 == 0x800000 && e2 > -149) {
          e2Corr = 131007
          cblCorr = 1
        }
        e10 = e2 * 315653 - e2Corr >> 20
        val g = gs(e10 + 324 << 1) + 1
        val h = (e10 * -108853 >> 15) + e2 + 1
        val cb = m2 << 2
        val vbCorr = (m2 & 0x1) - 1
        val vb = rop(g, cb << h)
        val vbl = rop(g, cb - cblCorr << h) + vbCorr
        val vbr = rop(g, cb + 2 << h) - vbCorr
        if (vb < 400 || {
          m10 = (vb * 107374183L >> 32).toInt // divide a positive int by 40
          val vb40 = m10 * 40
          val diff = vbl - vb40
          (vb40 - vbr + 40 ^ diff) >= 0 || {
            m10 += ~diff >>> 31
            e10 += 1
            false
          }
        }) {
          m10 = vb >> 2
          val vb4 = m10 << 2
          var diff = vbl - vb4
          if ((vb4 - vbr + 4 ^ diff) >= 0) diff = (vb & 0x3) + (m10 & 0x1) - 3
          m10 += ~diff >>> 31
          e10 -= e10Corr
        }
      }
      val ds = digits
      val len = digitCount(m10.toLong)
      e10 += len - 1
      if (e10 < -3 || e10 >= 7) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        ByteArrayAccess.setShort(buf, pos, (buf(pos + 1) | 0x2E00).toShort)
        if (lastPos - 3 < pos) {
          buf(lastPos) = '0'
          pos = lastPos + 1
        } else pos = lastPos
        ByteArrayAccess.setShort(buf, pos, 0x2D45)
        pos += 1
        if (e10 < 0) {
          e10 = -e10
          pos += 1
        }
        if (e10 < 10) {
          buf(pos) = (e10 + '0').toByte
          pos += 1
        } else pos = write2Digits(e10, pos, buf, ds)
      } else if (e10 < 0) {
        val dotPos = pos + 1
        ByteArrayAccess.setInt(buf, pos, 0x30303030)
        pos -= e10
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
        pos = lastPos
      } else if (e10 < len - 1) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        val bs = ByteArrayAccess.getLong(buf, pos)
        val s = e10 << 3
        val m = 0xFFFFFFFFFFFF0000L << s
        val d1 = (~m & bs) >> 8
        val d2 = 0x2E00L << s
        val d3 = m & bs
        ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3)
        pos = lastPos
      } else {
        pos += len
        writePositiveIntDigits(m10, pos, buf, ds)
        ByteArrayAccess.setShort(buf, pos, 0x302E)
        pos += 2
      }
    }
    count = pos
  }

  private[this] def rop(g: Long, cp: Int): Int = {
    val x = Math.multiplyHigh(g, cp.toLong << 32)
    (x >>> 31).toInt | -x.toInt >>> 31
  }

  // Based on the amazing work of Raffaello Giulietti
  // "The Schubfach way to render doubles": https://drive.google.com/file/d/1luHhyQF9zKlM8yJ1nebU0OgVYhfC6CBN/view
  // Sources with the license are here: https://github.com/c4f7fcce9cb06515/Schubfach/blob/3c92d3c9b1fead540616c918cdfef432bca53dfa/todec/src/math/DoubleToDecimal.java
  private[this] def writeDouble(x: Double): Unit = {
    var pos = ensureBufCapacity(24)
    val buf = this.buf
    if (x < 0.0) {
      buf(pos) = '-'
      pos += 1
    }
    if (x == 0.0) {
      ByteArrayAccess.setInt(buf, pos, 0x302E30)
      pos += 3
    } else {
      val bits = java.lang.Double.doubleToRawLongBits(x)
      var e2 = ((bits >> 52).toInt & 0x7FF) - 1075
      var m2 = bits & 0xFFFFFFFFFFFFFL | 0x10000000000000L
      var m10 = 0L
      var e10 = 0
      if (e2 == 0) m10 = m2
      else if ((e2 >= -52 && e2 < 0) && m2 << e2 == 0) m10 = m2 >> -e2
      else {
        var e10Corr, e2Corr = 0
        var cblCorr = 2
        if (e2 == -1075) {
          m2 &= 0xFFFFFFFFFFFFFL
          e2 = -1074
          if (m2 < 3) {
            m2 *= 10
            e10Corr = 1
          }
        } else if (e2 == 972) illegalNumberError(x)
        else if (m2 == 0x10000000000000L && e2 > -1074) {
          e2Corr = 131007
          cblCorr = 1
        }
        e10 = e2 * 315653 - e2Corr >> 20
        val i = e10 + 324 << 1
        val g1 = gs(i)
        val g0 = gs(i + 1)
        val h = (e10 * -108853 >> 15) + e2 + 2
        val cb = m2 << 2
        val vbCorr = (m2.toInt & 0x1) - 1
        val vb = rop(g1, g0, cb << h)
        val vbl = rop(g1, g0, cb - cblCorr << h) + vbCorr
        val vbr = rop(g1, g0, cb + 2 << h) - vbCorr
        if (vb < 400 || {
          m10 = Math.multiplyHigh(vb, 461168601842738792L) // divide a positive long by 40
          val vb40 = m10 * 40
          val diff = (vbl - vb40).toInt
          ((vb40 - vbr).toInt + 40 ^ diff) >= 0 || {
            m10 += ~diff >>> 31
            e10 += 1
            false
          }
        }) {
          m10 = vb >> 2
          val vb4 = m10 << 2
          var diff = (vbl - vb4).toInt
          if (((vb4 - vbr).toInt + 4 ^ diff) >= 0) diff = (vb.toInt & 0x3) + (m10.toInt & 0x1) - 3
          m10 += ~diff >>> 31
          e10 -= e10Corr
        }
      }
      val ds = digits
      val len = digitCount(m10)
      e10 += len - 1
      if (e10 < -3 || e10 >= 7) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        ByteArrayAccess.setShort(buf, pos, (buf(pos + 1) | 0x2E00).toShort)
        if (lastPos - 3 < pos) {
          buf(lastPos) = '0'
          pos = lastPos + 1
        } else pos = lastPos
        ByteArrayAccess.setShort(buf, pos, 0x2D45)
        pos += 1
        if (e10 < 0) {
          e10 = -e10
          pos += 1
        }
        if (e10 < 10) {
          buf(pos) = (e10 + '0').toByte
          pos += 1
        } else if (e10 < 100) pos = write2Digits(e10, pos, buf, ds)
        else pos = write3Digits(e10, pos, buf, ds)
      } else if (e10 < 0) {
        val dotPos = pos + 1
        ByteArrayAccess.setInt(buf, pos, 0x30303030)
        pos -= e10
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        buf(dotPos) = '.'
        pos = lastPos
      } else if (e10 < len - 1) {
        val lastPos = writeSignificantFractionDigits(m10, pos + len, pos, buf, ds)
        val bs = ByteArrayAccess.getLong(buf, pos)
        val s = e10 << 3
        val m = 0xFFFFFFFFFFFF0000L << s
        val d1 = (~m & bs) >> 8
        val d2 = 0x2E00L << s
        val d3 = m & bs
        ByteArrayAccess.setLong(buf, pos, d1 | d2 | d3)
        pos = lastPos
      } else {
        pos += len
        writePositiveIntDigits(m10.toInt, pos, buf, ds)
        ByteArrayAccess.setShort(buf, pos, 0x302E)
        pos += 2
      }
    }
    count = pos
  }

  private[this] def writeDecimal64(x: Long): Unit = {
    var pos = ensureBufCapacity(22)
    val buf = this.buf
    var m10 = x & 0x001FFFFFFFFFFFFFL
    var e10 = (x >> 53).toInt
    if ((x & 0x6000000000000000L) == 0x6000000000000000L) {
      if ((x & 0x7800000000000000L) == 0x7800000000000000L) illegalDecimal64NumberError(x)
      m10 = (x & 0x0007FFFFFFFFFFFFL) | 0x0020000000000000L
      if (m10 > 9999999999999999L) m10 = 0
      e10 = (x >> 51).toInt
    }
    e10 = (e10 & 0x3FF) - 398
    if (x < 0) {
      buf(pos) = '-'
      pos += 1
    }
    pos = writeLong(m10, pos, buf)
    if (e10 != 0) {
      ByteArrayAccess.setShort(buf, pos, 0x2D65)
      pos += 1
      if (e10 < 0) {
        e10 = -e10
        pos += 1
      }
      if (e10 < 10) {
        buf(pos) = (e10 + '0').toByte
        pos += 1
      } else if (e10 < 100) pos = write2Digits(e10, pos, buf, digits)
      else pos = write3Digits(e10, pos, buf, digits)
    }
    count = pos
  }

  private[this] def rop(g1: Long, g0: Long, cp: Long): Long = {
    val x = Math.multiplyHigh(g0, cp) + (g1 * cp >>> 1)
    Math.multiplyHigh(g1, cp) + (x >>> 63) | (-x ^ x) >>> 63
  }

  // Adoption of a nice trick from Daniel Lemire's blog that works for numbers up to 10^18:
  // https://lemire.me/blog/2021/06/03/computing-the-number-of-digits-of-an-integer-even-faster/
  private[this] def digitCount(x: Long): Int = (offsets(java.lang.Long.numberOfLeadingZeros(x)) + x >> 58).toInt

  private[this] def writeSignificantFractionDigits(x: Long, p: Int, pl: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = x.toInt
    var pos = p
    var posLim = pl
    if (q0 != x) {
      val q1 = (Math.multiplyHigh(x, 6189700196426901375L) >>> 25).toInt // divide a positive long by 100000000
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

  private[this] def writeSignificantFractionDigits(x: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Int = {
    var q0 = x
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
    writeFractionDigits(q1, pos - 2, posLim, buf, ds)
    pos + ((0x3039 - d) >>> 31)
  }

  private[this] def writeFractionDigits(x: Int, p: Int, posLim: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = x
    var pos = p
    while (pos > posLim) {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide a positive int by 100
      ByteArrayAccess.setShort(buf, pos - 1, ds(q0 - q1 * 100))
      q0 = q1
      pos -= 2
    }
  }

  private[this] def writePositiveIntDigits(x: Int, p: Int, buf: Array[Byte], ds: Array[Short]): Unit = {
    var q0 = x
    var pos = p
    while ({
      pos -= 2
      q0 >= 100
    }) {
      val q1 = (q0 * 1374389535L >> 37).toInt // divide a positive int by 100
      ByteArrayAccess.setShort(buf, pos, ds(q0 - q1 * 100))
      q0 = q1
    }
    if (q0 < 10) buf(pos + 1) = (q0 + '0').toByte
    else ByteArrayAccess.setShort(buf, pos, ds(q0))
  }

  private[this] def illegalNumberError(x: Float): Nothing = encodeError("illegal number: " + x)

  private[this] def illegalNumberError(x: Double): Nothing = encodeError("illegal number: " + x)

  private[this] def illegalDecimal64NumberError(x: Long): Nothing = encodeError("illegal Decimal64 number: " + x)

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

  /**
   * Checks if a character does not require JSON escaping or encoding.
   *
   * @param ch the character to check
   * @return `true` if the character is a basic ASCII character (code point less than `0x80`) that does not need JSON escaping
   */
  final def isNonEscapedAscii(ch: Char): Boolean = ch < 0x80 && escapedChars(ch.toInt) == 0
}