package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.InputStream
import java.math.{BigInteger, MathContext}
import java.nio.ByteBuffer
import java.time._
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader.{pow10d, _}

import scala.annotation.{switch, tailrec}
import scala.{specialized => sp}

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonReader]] that contains flags for tuning of
  * parsing exceptions and preferred sizes for internal buffers that created on the reader instantiation and reused in
  * runtime for parsing of messages.
  * <br/>
  * All configuration params already initialized by recommended default values, but in some cases they should be altered
  * for performance reasons:
  * <ul>
  * <li>turn off stack traces for parsing exceptions to greatly reduce impact on performance for cases when exceptions
  * can be not exceptional (e.g. under DoS attacks over open to the world systems), see more details here:
  * [[https://shipilev.net/blog/2014/exceptional-performance/]]</li>
  * <li>turn off appending of hex dump to minimize length of exception message</li>
  * <li>increase preferred size of an internal char buffer to reduce allocation rate of grown and then reduced
  * buffers when large (>1Kb) string instances need to be parsed</li>
  * <li>increase preferred size of an internal byte buffer for parsing from [[java.io.InputStream]] or
  * [[java.nio.DirectByteBuffer]] to reduce allocation rate of grown and then reduced buffers during parsing of large
  * (>16Kb) [[scala.math.BigDecimal]], [[scala.math.BigInt]] or ADT instances with the discriminator field doesn't
  * appear in the beginning of the JSON object</li>
  * </ul>
  * @param throwParseExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                          development
  * @param appendHexDumpToParseException a flag that allows to turn off hex dumping of affected by error part of
  *                                      an internal byte buffer
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when parsing from
  *                         [[java.io.InputStream]]
  * @param preferredCharBufSize a preferred size (in chars) of an internal char buffer for parsing of string values
  */
case class ReaderConfig(
    throwParseExceptionWithStackTrace: Boolean = false,
    appendHexDumpToParseException: Boolean = true,
    preferredBufSize: Int = 16384,
    preferredCharBufSize: Int = 1024) {
  if (preferredBufSize < 12) throw new IllegalArgumentException("'preferredBufSize' should be not less than 12")
  if (preferredCharBufSize < 0) throw new IllegalArgumentException("'preferredCharBufSize' should be not less than 0")
}

class JsonParseException private[jsoniter_scala](msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause, true, withStackTrace)

final class JsonReader private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](16384),
    private[this] var head: Int = 0,
    private[this] var tail: Int = 0,
    private[this] var mark: Int = 2147483647,
    private[this] var charBuf: Array[Char] = new Array[Char](1024),
    private[this] var bbuf: ByteBuffer = null,
    private[this] var in: InputStream = null,
    private[this] var totalRead: Long = 0,
    private[this] var config: ReaderConfig = null) {
  def requiredFieldError(reqField: String): Nothing = {
    var i = appendString("missing required field \"", 0)
    i = appendString(reqField, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def duplicatedKeyError(len: Int): Nothing = {
    var i = prependString("duplicated field \"", len)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def unexpectedKeyError(len: Int): Nothing = {
    var i = prependString("unexpected field \"", len)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def discriminatorError(): Nothing = decodeError("illegal discriminator")

  def discriminatorValueError(discriminatorFieldName: String): Nothing = {
    var i = appendString("illegal value of discriminator field \"", 0)
    i = appendString(discriminatorFieldName, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def enumValueError(value: String): Nothing = {
    var i = appendString("illegal enum value \"", 0)
    i = appendString(value, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def enumValueError(len: Int): Nothing = {
    var i = prependString("illegal enum value \"", len)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def setMark(): Unit = mark = head

  @tailrec
  def skipToKey(key: String): Boolean = isCharBufEqualsTo(readKeyAsCharBuf(), key) || {
    skip()
    isNextToken(',', head) && skipToKey(key)
  }

  def rollbackToMark(): Unit = {
    if (mark == 2147483647) illegalMarkOperation()
    head = mark
    mark = 2147483647
  }

  def readKeyAsCharBuf(): Int = {
    nextTokenOrError('"', head)
    val len = parseString()
    nextTokenOrError(':', head)
    len
  }

  def readKeyAsString(): String = {
    nextTokenOrError('"', head)
    val len = parseString()
    nextTokenOrError(':', head)
    new String(charBuf, 0, len)
  }

  def readKeyAsDuration(): Duration = {
    nextTokenOrError('"', head)
    val x = parseDuration()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsInstant(): Instant = {
    nextTokenOrError('"', head)
    val x = parseInstant()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsLocalDate(): LocalDate = {
    nextTokenOrError('"', head)
    val x = parseLocalDate()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsLocalDateTime(): LocalDateTime = {
    nextTokenOrError('"', head)
    val x = parseLocalDateTime()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsLocalTime(): LocalTime = {
    nextTokenOrError('"', head)
    val x = parseLocalTime()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsMonthDay(): MonthDay = {
    nextTokenOrError('"', head)
    val x = parseMonthDay(head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsOffsetDateTime(): OffsetDateTime = {
    nextTokenOrError('"', head)
    val x = parseOffsetDateTime()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsOffsetTime(): OffsetTime = {
    nextTokenOrError('"', head)
    val x = parseOffsetTime()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsPeriod(): Period = {
    nextTokenOrError('"', head)
    val x = parsePeriod()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsYear(): Year = {
    nextTokenOrError('"', head)
    val x = toYear(parseYearWithByte('"', 9, head))
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsYearMonth(): YearMonth = {
    nextTokenOrError('"', head)
    val x = parseYearMonth()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsZonedDateTime(): ZonedDateTime = {
    nextTokenOrError('"', head)
    val x = parseZonedDateTime()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsZoneId(): ZoneId = {
    nextTokenOrError('"', head)
    val x = toZoneId(parseZoneIdUntilToken('"'))
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsZoneOffset(): ZoneOffset = {
    nextTokenOrError('"', head)
    val x = parseZoneOffset()
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsBoolean(): Boolean = {
    nextTokenOrError('"', head)
    val x = parseBoolean(isToken = false, head)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsByte(): Byte = {
    nextTokenOrError('"', head)
    val x = parseByte(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsChar(): Char = {
    nextTokenOrError('"', head)
    val x = parseChar(head)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsShort(): Short = {
    nextTokenOrError('"', head)
    val x = parseShort(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsInt(): Int = {
    nextTokenOrError('"', head)
    val x = parseInt(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsLong(): Long = {
    nextTokenOrError('"', head)
    val x = parseLong(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsFloat(): Float = {
    nextTokenOrError('"', head)
    val x = parseFloat(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsDouble(): Double = {
    nextTokenOrError('"', head)
    val x = parseDouble(isToken = false)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsBigInt(digitsLimit: Int = bigIntDigitsLimit): BigInt = {
    nextTokenOrError('"', head)
    val x = parseBigInt(isToken = false, null, digitsLimit)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsBigDecimal(mc: MathContext = bigDecimalMathContext, scaleLimit: Int = bigDecimalScaleLimit,
                          digitsLimit: Int = bigDecimalDigitsLimit): BigDecimal = {
    nextTokenOrError('"', head)
    val x = parseBigDecimal(isToken = false, null, mc, scaleLimit, digitsLimit)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsUUID(): UUID = {
    nextTokenOrError('"', head)
    val x = parseUUID(head)
    nextTokenOrError(':', head)
    x
  }

  def readByte(): Byte = parseByte(isToken = true)

  def readChar(): Char = {
    nextTokenOrError('"', head)
    val x = parseChar(head)
    nextByteOrError('"', head)
    x
  }

  def readShort(): Short = parseShort(isToken = true)

  def readInt(): Int = parseInt(isToken = true)

  def readLong(): Long = parseLong(isToken = true)

  def readDouble(): Double = parseDouble(isToken = true)

  def readFloat(): Float = parseFloat(isToken = true)

  def readBigInt(default: BigInt, digitsLimit: Int = bigIntDigitsLimit): BigInt =
    parseBigInt(isToken = true, default, digitsLimit)

  def readBigDecimal(default: BigDecimal, mc: MathContext = bigDecimalMathContext,
                     scaleLimit: Int = bigDecimalScaleLimit, digitsLimit: Int = bigDecimalDigitsLimit): BigDecimal =
    parseBigDecimal(isToken = true, default, mc, scaleLimit, digitsLimit)

  def readString(default: String): String =
    if (isNextToken('"', head)) {
      val len = parseString()
      new String(charBuf, 0, len)
    } else readNullOrTokenError(default, '"')

  def readDuration(default: Duration): Duration =
    if (isNextToken('"', head)) parseDuration()
    else readNullOrTokenError(default, '"')

  def readInstant(default: Instant): Instant =
    if (isNextToken('"', head)) parseInstant()
    else readNullOrTokenError(default, '"')

  def readLocalDate(default: LocalDate): LocalDate =
    if (isNextToken('"', head)) parseLocalDate()
    else readNullOrTokenError(default, '"')

  def readLocalDateTime(default: LocalDateTime): LocalDateTime =
    if (isNextToken('"', head)) parseLocalDateTime()
    else readNullOrTokenError(default, '"')

  def readLocalTime(default: LocalTime): LocalTime =
    if (isNextToken('"', head)) parseLocalTime()
    else readNullOrTokenError(default, '"')

  def readMonthDay(default: MonthDay): MonthDay =
    if (isNextToken('"', head)) parseMonthDay(head)
    else readNullOrTokenError(default, '"')

  def readOffsetDateTime(default: OffsetDateTime): OffsetDateTime =
    if (isNextToken('"', head)) parseOffsetDateTime()
    else readNullOrTokenError(default, '"')

  def readOffsetTime(default: OffsetTime): OffsetTime =
    if (isNextToken('"', head)) parseOffsetTime()
    else readNullOrTokenError(default, '"')

  def readPeriod(default: Period): Period =
    if (isNextToken('"', head)) parsePeriod()
    else readNullOrTokenError(default, '"')

  def readYear(default: Year): Year =
    if (isNextToken('"', head)) toYear(parseYearWithByte('"', 9, head))
    else readNullOrTokenError(default, '"')

  def readYearMonth(default: YearMonth): YearMonth =
    if (isNextToken('"', head)) parseYearMonth()
    else readNullOrTokenError(default, '"')

  def readZonedDateTime(default: ZonedDateTime): ZonedDateTime =
    if (isNextToken('"', head)) parseZonedDateTime()
    else readNullOrTokenError(default, '"')

  def readZoneId(default: ZoneId): ZoneId =
    if (isNextToken('"', head)) toZoneId(parseZoneIdUntilToken('"'))
    else readNullOrTokenError(default, '"')

  def readZoneOffset(default: ZoneOffset): ZoneOffset =
    if (isNextToken('"', head)) parseZoneOffset()
    else readNullOrTokenError(default, '"')

  def readUUID(default: UUID): UUID =
    if (isNextToken('"', head)) parseUUID(head)
    else readNullOrTokenError(default, '"')

  def readBoolean(): Boolean = parseBoolean(isToken = true, head)

  def readStringAsCharBuf(): Int = {
    nextTokenOrError('"', head)
    parseString()
  }

  def readStringAsByte(): Byte = {
    nextTokenOrError('"', head)
    val x = parseByte(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsShort(): Short = {
    nextTokenOrError('"', head)
    val x = parseShort(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsInt(): Int = {
    nextTokenOrError('"', head)
    val x = parseInt(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsLong(): Long = {
    nextTokenOrError('"', head)
    val x = parseLong(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsDouble(): Double = {
    nextTokenOrError('"', head)
    val x = parseDouble(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsFloat(): Float = {
    nextTokenOrError('"', head)
    val x = parseFloat(isToken = false)
    nextByteOrError('"', head)
    x
  }

  def readStringAsBigInt(default: BigInt, digitsLimit: Int = bigIntDigitsLimit): BigInt =
    if (isNextToken('"', head)) {
      val x = parseBigInt(isToken = false, default, digitsLimit)
      nextByteOrError('"', head)
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBigDecimal(default: BigDecimal, mc: MathContext = bigDecimalMathContext,
                             scaleLimit: Int = bigDecimalScaleLimit,
                             digitsLimit: Int = bigDecimalDigitsLimit): BigDecimal =
    if (isNextToken('"', head)) {
      val x = parseBigDecimal(isToken = false, default, mc, scaleLimit, digitsLimit)
      nextByteOrError('"', head)
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBoolean(): Boolean = {
    nextTokenOrError('"', head)
    val x = parseBoolean(isToken = false, head)
    nextByteOrError('"', head)
    x
  }

  def readNullOrError[@sp A](default: A, msg: String): A =
    if (default == null) decodeError(msg)
    else if (isCurrentToken('n', head)) parseNullOrError(default, msg, head)
    else decodeError(msg)

  def readNullOrTokenError[@sp A](default: A, b: Byte): A =
    if (default == null) tokenError(b)
    else if (isCurrentToken('n', head)) parseNullOrTokenError(default, b, head)
    else tokenOrNullError(b)

  def nextToken(): Byte = nextToken(head)

  def isNextToken(b: Byte): Boolean = isNextToken(b, head)

  def isCurrentToken(b: Byte): Boolean = isCurrentToken(b, head)

  def rollbackToken(): Unit = {
    val pos = head
    if (pos == 0) illegalTokenOperation()
    head = pos - 1
  }

  def charBufToHashCode(len: Int): Int = toHashCode(charBuf, len)

  def isCharBufEqualsTo(len: Int, s: String): Boolean = len == s.length && isCharBufEqualsTo(len, s, 0)

  def skip(): Unit = head = {
    val b = nextToken(head)
    if (b == '"') skipString(evenBackSlashes = true, head)
    else if ((b >= '0' && b <= '9') || b == '-') skipNumber(head)
    else if (b == 'n' || b == 't') skipFixedBytes(3, head)
    else if (b == 'f') skipFixedBytes(4, head)
    else if (b == '[') skipArray(0, head)
    else if (b == '{') skipObject(0, head)
    else decodeError("expected value")
  }

  def commaError(): Nothing = tokenError(',')

  def arrayStartOrNullError(): Nothing = tokenOrNullError('[')

  def arrayEndError(): Nothing = tokenError(']')

  def arrayEndOrCommaError(): Nothing = decodeError("expected ']' or ','")

  def objectStartOrNullError(): Nothing = tokenOrNullError('{')

  def objectEndOrCommaError(): Nothing = decodeError("expected '}' or ','")

  def decodeError(msg: String): Nothing = decodeError(msg, head - 1)

  private[jsoniter_scala] def read[@sp A](codec: JsonValueCodec[A], buf: Array[Byte], from: Int, to: Int, config: ReaderConfig): A = {
    val currBuf = this.buf
    try {
      this.buf = buf
      this.config = config
      head = from
      tail = to
      totalRead = 0
      mark = 2147483647
      codec.decodeValue(this, codec.nullValue)
    } finally {
      this.buf = currBuf
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }
  }

  private[jsoniter_scala] def read[@sp A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig): A =
    try {
      this.config = config
      this.in = in
      head = 0
      tail = 0
      totalRead = 0
      mark = 2147483647
      if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
      codec.decodeValue(this, codec.nullValue)
    } finally {
      this.in = null
      if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }

  private[jsoniter_scala] def read[@sp A](codec: JsonValueCodec[A], bbuf: ByteBuffer, config: ReaderConfig): A =
    if (bbuf.hasArray) {
      val offset = bbuf.arrayOffset
      val currBuf = this.buf
      try {
        this.buf = bbuf.array
        this.config = config
        head = offset + bbuf.position()
        tail = offset + bbuf.limit()
        totalRead = 0
        mark = 2147483647
        codec.decodeValue(this, codec.nullValue)
      } finally {
        this.buf = currBuf
        if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
        bbuf.position(head - offset)
      }
    } else {
      val position = bbuf.position()
      try {
        this.config = config
        this.bbuf = bbuf
        head = 0
        tail = 0
        totalRead = 0
        mark = 2147483647
        if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
        codec.decodeValue(this, codec.nullValue)
      } finally {
        this.bbuf = null
        if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
        if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
        bbuf.position(totalRead.toInt - tail + head + position)
      }
    }

  private[jsoniter_scala] def scanValueStream[@sp A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig)
                                                    (f: A => Boolean): Unit =
    try {
      this.config = config
      this.in = in
      head = 0
      tail = 0
      totalRead = 0
      mark = 2147483647
      if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
      while (f(codec.decodeValue(this, codec.nullValue)) && skipWhitespaces()) ()
    } finally {
      this.in = null
      if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }

  private[jsoniter_scala] def scanArray[@sp A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig)
                                              (f: A => Boolean): Unit =
    try {
      this.config = config
      this.in = in
      head = 0
      tail = 0
      totalRead = 0
      mark = 2147483647
      if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
      if (isNextToken('[')) {
        if (!isNextToken(']')) {
          rollbackToken()
          var continue = true
          do {
            continue = f(codec.decodeValue(this, codec.nullValue))
          } while (continue && isNextToken(','))
          if (continue && !isCurrentToken(']')) arrayEndOrCommaError()
        }
      } else readNullOrTokenError((), '[')
    } finally {
      this.in = null
      if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }

  private[this] def skipWhitespaces(): Boolean = {
    var pos = head
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      val b = buf(pos)
      b == ' ' || b == '\n' || b == '\t' || b == '\r'
    }) pos += 1
    head = pos
    pos != tail
  }

  private[this] def tokenOrDigitError(b: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendString("' or digit", i)
    decodeError(i, pos, null)
  }

  private[this] def tokensError(b1: Byte, b2: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b1.toChar, i)
    i = appendString("' or '", i)
    i = appendChar(b2.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private[this] def tokenOrNullError(b: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendString("' or null", i)
    decodeError(i, pos, null)
  }

  private[this] def tokenError(b: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private[this] def decodeError(msg: String, pos: Int, cause: Throwable = null): Nothing =
    decodeError(appendString(msg, 0), pos, cause)

  private[this] def decodeError(from: Int, pos: Int, cause: Throwable) = {
    var i = appendString(", offset: 0x", from)
    val off =
      if ((bbuf eq null) && (in eq null)) 0
      else totalRead - tail
    i = appendHexOffset(off + pos, i, hexDigits)
    if (config.appendHexDumpToParseException) {
      i = appendString(", buf:", i)
      i = appendHexDump(Math.max((pos - 32) & 0xFFFFFFF0, 0), Math.min((pos + 48) & 0xFFFFFFF0, tail), off.toInt, i)
    }
    throw new JsonParseException(new String(charBuf, 0, i), cause, config.throwParseExceptionWithStackTrace)
  }

  @tailrec
  private[this] def nextByte(pos: Int): Byte =
    if (pos < tail) {
      head = pos + 1
      buf(pos)
    } else nextByte(loadMoreOrError(pos))

  @tailrec
  private def nextByteOrError(b: Byte, pos: Int): Unit =
    if (pos < tail) {
      if (buf(pos) != b) tokenError(b, pos)
      head = pos + 1
    } else nextByteOrError(b, loadMoreOrError(pos))

  @tailrec
  private[this] def nextToken(pos: Int): Byte =
    if (pos < tail) {
      val b = buf(pos)
      if (b == ' ' || b == '\n' || b == '\t' || b == '\r') nextToken(pos + 1)
      else {
        head = pos + 1
        b
      }
    } else nextToken(loadMoreOrError(pos))

  @tailrec
  private[this] def nextTokenOrError(t: Byte, pos: Int): Unit =
    if (pos < tail) {
      val b = buf(pos)
      if (b == t) head = pos + 1
      else if (b == ' ' || b == '\n' || b == '\t' || b == '\r') nextTokenOrError(t, pos + 1)
      else tokenError(t, pos)
    } else nextTokenOrError(t, loadMoreOrError(pos))

  @tailrec
  private[this] def isNextToken(t: Byte, pos: Int): Boolean =
    if (pos < tail) {
      val b = buf(pos)
      if (b == t) {
        head = pos + 1
        true
      } else if (b == ' ' || b == '\n' || b == '\t' || b == '\r') isNextToken(t, pos + 1)
      else {
        head = pos + 1
        false
      }
    } else isNextToken(t, loadMoreOrError(pos))

  private[this] def isCurrentToken(b: Byte, pos: Int): Boolean = {
    if (pos == 0) illegalTokenOperation()
    buf(pos - 1) == b
  }

  @tailrec
  private[this] def scanUntilToken(t: Byte, pos: Int): Int =
    if (pos + 3 < tail) {
      if (buf(pos) == t) pos + 1
      else if (buf(pos + 1) == t) pos + 2
      else if (buf(pos + 2) == t) pos + 3
      else if (buf(pos + 3) == t) pos + 4
      else scanUntilToken(t, pos + 4)
    } else if (pos < tail) {
      if (buf(pos) == t) pos + 1
      else scanUntilToken(t, pos + 1)
    } else scanUntilToken(t, loadMoreOrError(pos))

  private[this] def illegalTokenOperation(): Nothing =
    throw new ArrayIndexOutOfBoundsException("expected preceding call of 'nextToken()' or 'isNextToken()'")

  private[this] def illegalMarkOperation(): Nothing =
    throw new ArrayIndexOutOfBoundsException("expected preceding call of 'setMark()'")

  @tailrec
  private[this] def next2Digits(pos: Int): Int =
    if (pos + 1 < tail) {
      val d1 = buf(pos) - '0'
      val d2 = buf(pos + 1) - '0'
      if (d1 < 0 || d1 > 9) digitError(pos)
      if (d2 < 0 || d2 > 9) digitError(pos + 1)
      head = pos + 2
      d1 * 10 + d2
    } else next2Digits(loadMoreOrError(pos))

  @tailrec
  private[this] def next2DigitsWithByte(t: Byte, pos: Int): Int =
    if (pos + 2 < tail) {
      val d1 = buf(pos) - '0'
      val d2 = buf(pos + 1) - '0'
      if (d1 < 0 || d1 > 9) digitError(pos)
      if (d2 < 0 || d2 > 9) digitError(pos + 1)
      if (buf(pos + 2) != t) tokenError(t, pos + 2)
      head = pos + 3
      d1 * 10 + d2
    } else next2DigitsWithByte(t, loadMoreOrError(pos))

  @tailrec
  private[this] def parseYearWithByte(t: Byte, maxDigits: Int, pos: Int): Int =
    if (pos + 4 < tail) {
      val b = buf(pos)
      if (b >= '0' && b <= '9') {
        val d2 = buf(pos + 1) - '0'
        val d3 = buf(pos + 2) - '0'
        val d4 = buf(pos + 3) - '0'
        if (d2 < 0 || d2 > 9) digitError(pos + 1)
        if (d3 < 0 || d3 > 9) digitError(pos + 2)
        if (d4 < 0 || d4 > 9) digitError(pos + 3)
        if (buf(pos + 4) != t) tokenError(t, pos + 4)
        head = pos + 5
        (b - '0') * 1000 + d2 * 100 + d3 * 10 + d4
      } else parseNon4DigitYearWithByte(t, maxDigits, b, pos)
    } else parseYearWithByte(t, maxDigits, loadMoreOrError(pos))

  private[this] def parseNon4DigitYearWithByte(t: Byte, maxDigits: Int, b: Byte, p: Int): Int = {
    var pos = p
    val yearNeg = b == '-' || (b != '+' && decodeError("expected '-' or '+' or digit", pos))
    val d2 = buf(pos + 1) - '0'
    val d3 = buf(pos + 2) - '0'
    val d4 = buf(pos + 3) - '0'
    val d5 = buf(pos + 4) - '0'
    if (d2 < 0 || d2 > 9) digitError(pos + 1)
    if (d3 < 0 || d3 > 9) digitError(pos + 2)
    if (d4 < 0 || d4 > 9) digitError(pos + 3)
    if (d5 < 0 || d5 > 9) digitError(pos + 4)
    var year = d2 * 1000 + d3 * 100 + d4 * 10 + d5
    var yearDigits = 4
    var d = 0
    pos += 5
    while ({
      if (pos >= tail) pos = loadMoreOrError(pos)
      d = buf(pos) - '0'
      d >= 0 && d <= 9 && yearDigits < maxDigits
    }) {
      year =
        if (year > 100000000) 2147483647
        else year * 10 + d
      yearDigits += 1
      pos += 1
    }
    if (d + '0' != t) {
      if (!yearNeg && yearDigits == 4) digitError(pos)
      if (yearDigits == maxDigits) tokenError(t, pos)
      tokenOrDigitError(t, pos)
    }
    head = pos + 1
    if (yearNeg) {
      if (year == 0) 2147483647
      else -year
    } else year
  }

  private[this] def parseNanoWithByte(t: Byte): Int = {
    var nano, d = 0
    var nanoDigitWeight = 100000000
    var pos = head
    while ({
      if (pos >= tail) pos = loadMoreOrError(pos)
      d = buf(pos) - '0'
      d >= 0 && d <= 9 && nanoDigitWeight != 0
    }) {
      nano += d * nanoDigitWeight
      nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
      pos += 1
    }
    if (d + '0' != t) nanoError(nanoDigitWeight, t, pos)
    head = pos + 1
    nano
  }

  private[this] def parseZoneIdUntilToken(t: Byte): String = {
    val mark = this.mark
    this.mark = Math.min(mark, head)
    try {
      head = scanUntilToken(t, head)
      new String(buf, 0, this.mark, head - this.mark - 1)
    } finally this.mark = mark
  }

  @tailrec
  private[this] def parseNullOrError[@sp A](default: A, error: String, pos: Int): A =
    if (pos + 2 < tail) {
      if (buf(pos) != 'u') decodeError(error, pos)
      if (buf(pos + 1) != 'l') decodeError(error, pos + 1)
      if (buf(pos + 2) != 'l') decodeError(error, pos + 2)
      head = pos + 3
      default
    } else parseNullOrError(default, error, loadMoreOrError(pos))

  @tailrec
  private[this] def parseNullOrTokenError[@sp A](default: A, b: Byte, pos: Int): A =
    if (pos + 2 < tail) {
      if (buf(pos) != 'u') tokenOrNullError(b, pos)
      if (buf(pos + 1) != 'l') tokenOrNullError(b, pos + 1)
      if (buf(pos + 2) != 'l') tokenOrNullError(b, pos + 2)
      head = pos + 3
      default
    } else parseNullOrTokenError(default, b, loadMoreOrError(pos))

  @tailrec
  private[this] def isCharBufEqualsTo(len: Int, s: String, i: Int): Boolean =
    i == len || (charBuf(i) == s.charAt(i) && isCharBufEqualsTo(len, s, i + 1))

  private[this] def appendChar(ch: Char, i: Int): Int = {
    if (i >= charBuf.length) growCharBuf(i + 1)
    charBuf(i) = ch
    i + 1
  }

  private[this] def appendChars(cs: Array[Char], i: Int): Int = {
    val len = cs.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    System.arraycopy(cs, 0, charBuf, i, len)
    required
  }

  private[this] def appendString(s: String, i: Int): Int = {
    val len = s.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    s.getChars(0, len, charBuf, i)
    required
  }

  private[this] def prependString(s: String, i: Int): Int = {
    val len = s.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    var i1 = required - 1
    var i2 = i1 - len
    while (i2 >= 0) {
      charBuf(i1) = charBuf(i2)
      i1 -= 1
      i2 -= 1
    }
    s.getChars(0, len, charBuf, 0)
    required
  }

  @tailrec
  private[this] def parseBoolean(isToken: Boolean, pos: Int): Boolean =
    if (pos + 3 < tail) {
      val b = buf(pos)
      if (b == 't') {
        if (buf(pos + 1) != 'r') booleanError(pos + 1)
        if (buf(pos + 2) != 'u') booleanError(pos + 2)
        if (buf(pos + 3) != 'e') booleanError(pos + 3)
        head = pos + 4
        true
      } else if (b == 'f') {
        if (buf(pos + 1) != 'a') booleanError(pos + 1)
        if (buf(pos + 2) != 'l') booleanError(pos + 2)
        if (buf(pos + 3) != 's') booleanError(pos + 3)
        if (pos + 4 < tail) {
          if (buf(pos + 4) != 'e') booleanError(pos + 4)
          head = pos + 5
          false
        } else parseBoolean(isToken, loadMoreOrError(pos))
      } else if (isToken && (b == ' ' || b == '\n' || b == '\t' || b == '\r')) parseBoolean(isToken, pos + 1)
      else booleanError(pos)
    } else parseBoolean(isToken, loadMoreOrError(pos))

  private[this] def booleanError(pos: Int): Nothing = decodeError("illegal boolean", pos)

  private[this] def parseByte(isToken: Boolean): Byte = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    var pos = head
    if (b < '0' || b > '9') numberError(pos - 1)
    var x = '0' - b
    val isZeroFirst = isToken && x == 0
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      b = buf(pos)
      b >= '0' && b <= '9'
    }) {
      if (isZeroFirst) leadingZeroError(pos - 1)
      x = x * 10 + ('0' - b)
      if (x < -128) byteOverflowError(pos)
      pos += 1
    }
    head = pos
    if (b == '.' || (b | 0x20) == 'e') numberError(pos)
    if (isNeg) x.toByte
    else {
      if (x == -128) byteOverflowError(pos - 1)
      (-x).toByte
    }
  }

  private[this] def parseShort(isToken: Boolean): Short = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    var pos = head
    if (b < '0' || b > '9') numberError(pos - 1)
    var x = '0' - b
    val isZeroFirst = isToken && x == 0
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      b = buf(pos)
      b >= '0' && b <= '9'
    }) {
      if (isZeroFirst) leadingZeroError(pos - 1)
      x = x * 10 + ('0' - b)
      if (x < -32768) shortOverflowError(pos)
      pos += 1
    }
    head = pos
    if (b == '.' || (b | 0x20) == 'e') numberError(pos)
    if (isNeg) x.toShort
    else {
      if (x == -32768) shortOverflowError(pos - 1)
      (-x).toShort
    }
  }

  private[this] def parseInt(isToken: Boolean): Int = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    var pos = head
    if (b < '0' || b > '9') numberError(pos - 1)
    var x = '0' - b
    val isZeroFirst = isToken && x == 0
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      b = buf(pos)
      b >= '0' && b <= '9'
    }) {
      if (isZeroFirst) leadingZeroError(pos - 1)
      if (x < -214748364) intOverflowError(pos)
      x = x * 10 + ('0' - b)
      if (x > 0) intOverflowError(pos)
      pos += 1
    }
    head = pos
    if (b == '.' || (b | 0x20) == 'e') numberError(pos)
    if (isNeg) x
    else {
      if (x == -2147483648) intOverflowError(pos - 1)
      -x
    }
  }

  private[this] def parseLong(isToken: Boolean): Long = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    var pos = head
    if (b < '0' || b > '9') numberError(pos - 1)
    var x: Long = '0' - b
    val isZeroFirst = isToken && x == 0
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      b = buf(pos)
      b >= '0' && b <= '9'
    }) {
      if (isZeroFirst) leadingZeroError(pos - 1)
      if (x < -922337203685477580L) longOverflowError(pos)
      x = x * 10 + ('0' - b)
      if (x > 0) longOverflowError(pos)
      pos += 1
    }
    head = pos
    if (b == '.' || (b | 0x20) == 'e') numberError(pos)
    if (isNeg) x
    else {
      if (x == -9223372036854775808L) longOverflowError(pos - 1)
      -x
    }
  }

  private[this] def parseDouble(isToken: Boolean): Double = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val mark = this.mark
    this.mark = Math.min(mark, head - 1)
    try {
      val isNeg = b == '-'
      if (isNeg) b = nextByte(head)
      var pos = head
      if (b < '0' || b > '9') numberError(pos - 1)
      var posMan: Long = b - '0'
      val isZeroFirst = isToken && posMan == 0
      var manExp, posExp, digits = 0
      var isExpNeg = false
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (isZeroFirst) leadingZeroError(pos - 1)
        if (posMan < 9007199254740992L) {
          posMan = posMan * 10 + (b - '0')
          digits += 1
        } else manExp += 1
        pos += 1
      }
      if (b == '.') {
        b = nextByte(pos + 1)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        if (posMan < 9007199254740992L) {
          posMan = posMan * 10 + (b - '0')
          digits += 1
          manExp -= 1
        }
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMan < 9007199254740992L) {
            posMan = posMan * 10 + (b - '0')
            digits += 1
            manExp -= 1
          }
          pos += 1
        }
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        isExpNeg = b == '-'
        if (isExpNeg || b == '+') b = nextByte(head)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        posExp = b - '0'
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posExp < 100) posExp = posExp * 10 + (b - '0')
          pos += 1
        }
      }
      head = pos
      if (posMan < 9007199254740992L) { // 9007199254740991L == (1L << 53) - 1, max mantissa that can be converted w/o rounding error by double mul or div
        val exp =
          if (isExpNeg) manExp - posExp
          else manExp + posExp
        if (exp == 0) toSignedDouble(isNeg, posMan)
        else if (exp < 0) {
          if (exp >= -22) toSignedDouble(isNeg, posMan / pow10d(-exp))
          else toDouble(pos)
        } else if (exp <= 22) toSignedDouble(isNeg, posMan * pow10d(exp))
        else {
          val slop = 15 - digits
          if (exp - slop <= 22) {
            val pow10 = JsonReader.pow10d
            toSignedDouble(isNeg, (posMan * pow10(slop)) * pow10(exp - slop))
          } else toDouble(pos)
        }
      } else toDouble(pos)
    } finally this.mark = mark
  }

  private[this] def toSignedDouble(isNeg: Boolean, posX: Double): Double =
    if (isNeg) -posX
    else posX

  private[this] def toDouble(pos: Int): Double = java.lang.Double.parseDouble(new String(buf, 0, mark, pos - mark))

  private[this] def parseFloat(isToken: Boolean): Float = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val mark = this.mark
    this.mark = Math.min(mark, head - 1)
    try {
      val isNeg = b == '-'
      if (isNeg) b = nextByte(head)
      var pos = head
      if (b < '0' || b > '9') numberError(pos - 1)
      var posMan = b - '0'
      val isZeroFirst = isToken && posMan == 0
      var manExp, posExp, digits = 0
      var isExpNeg = false
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (isZeroFirst) leadingZeroError(pos - 1)
        if (posMan < 16777216) {
          posMan = posMan * 10 + (b - '0')
          digits += 1
        } else manExp += 1
        pos += 1
      }
      if (b == '.') {
        b = nextByte(pos + 1)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        if (posMan < 16777216) {
          posMan = posMan * 10 + (b - '0')
          digits += 1
          manExp -= 1
        }
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMan < 16777216) {
            posMan = posMan * 10 + (b - '0')
            digits += 1
            manExp -= 1
          }
          pos += 1
        }
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        isExpNeg = b == '-'
        if (isExpNeg || b == '+') b = nextByte(head)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        posExp = b - '0'
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posExp < 100) posExp = posExp * 10 + (b - '0')
          pos += 1
        }
      }
      head = pos
      if (posMan < 16777216) { // 16777215 == (1L << 24) - 1, max mantissa that can be converted w/o rounding error by float mul or div
        val exp =
          if (isExpNeg) manExp - posExp
          else manExp + posExp
        if (exp == 0) toSignedFloat(isNeg, posMan)
        else if (exp < 0) {
          if (exp >= -10) toSignedFloat(isNeg, posMan / pow10f(-exp))
          else toFloat(pos)
        } else if (exp <= 10) toSignedFloat(isNeg, posMan * pow10f(exp))
        else {
          val slop = 6 - digits
          if (exp - slop <= 10) {
            val pow10 = JsonReader.pow10f
            toSignedFloat(isNeg, (posMan * pow10(slop)) * pow10(exp - slop))
          } else toFloat(pos)
        }
      } else toFloat(pos)
    } finally this.mark = mark
  }

  private[this] def toSignedFloat(isNeg: Boolean, posX: Float): Float =
    if (isNeg) -posX
    else posX

  private[this] def toFloat(pos: Int): Float = java.lang.Float.parseFloat(new String(buf, 0, mark, pos - mark))

  private[this] def parseBigInt(isToken: Boolean, default: BigInt, digitsLimit: Int): BigInt = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    if (isToken && b == 'n') readNullOrNumberError(default, head)
    else {
      val mark = this.mark
      this.mark = Math.min(mark, head - 1)
      try {
        val isNeg = b == '-'
        if (isNeg) b = nextByte(head)
        var pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        val isZeroFirst = isToken && b == '0'
        var significandDigits =
          if (b == '0') 0
          else 1
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (isZeroFirst) leadingZeroError(pos - 1)
          significandDigits += 1
          if (significandDigits >= digitsLimit) digitsLimitError(pos)
          pos += 1
        }
        head = pos
        if (b == '.' || (b | 0x20) == 'e') numberError(pos)
        toBigInt(isNeg, pos)
      } finally this.mark = mark
    }
  }

  private[this] def toBigInt(isNeg: Boolean, pos: Int): BigInt = {
    val startPos = this.mark
    var numPos =
      if (isNeg) startPos + 1
      else startPos
    if (pos < 19 + numPos) { // 19 == Long.MaxValue.toString.length
      var x = 0L
      while (numPos < pos) {
        x = x * 10 + (buf(numPos) - '0')
        numPos += 1
      }
      new BigInt(BigInteger.valueOf {
        if (isNeg) -x
        else x
      })
    } else new BigInt(new java.math.BigInteger(new String(buf, 0, startPos, pos - startPos), 10))
  }

  private[this] def parseBigDecimal(isToken: Boolean, default: BigDecimal, mc: MathContext, scaleLimit: Int,
                                    digitsLimit: Int): BigDecimal = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    if (isToken && b == 'n') readNullOrNumberError(default, head)
    else {
      val mark = this.mark
      this.mark = Math.min(mark, head - 1)
      try {
        val isNeg = b == '-'
        if (isNeg) b = nextByte(head)
        var pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        val isZeroFirst = isToken && b == '0'
        var significandDigits =
          if (b == '0') 0
          else 1
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (isZeroFirst) leadingZeroError(pos - 1)
          significandDigits += 1
          if (significandDigits >= digitsLimit) digitsLimitError(pos)
          pos += 1
        }
        if (b == '.') {
          b = nextByte(pos + 1)
          pos = head
          if (b < '0' || b > '9') numberError(pos - 1)
          while ((pos < tail || {
            pos = loadMore(pos)
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) {
            if (significandDigits > 0 || b != '0') {
              significandDigits += 1
              if (significandDigits >= digitsLimit) digitsLimitError(pos)
            }
            pos += 1
          }
        }
        if ((b | 0x20) == 'e') {
          b = nextByte(pos + 1)
          if (b == '-' || b == '+') b = nextByte(head)
          pos = head
          if (b < '0' || b > '9') numberError(pos - 1)
          while ((pos < tail || {
            pos = loadMore(pos)
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) pos += 1
        }
        head = pos
        val x = toBigDecimal(pos, mc)
        if (Math.abs(x.scale) >= scaleLimit) scaleLimitError(pos - 1)
        x
      } finally this.mark = mark
    }
  }

  private[this] def toBigDecimal(pos: Int, mc: MathContext): BigDecimal = {
    val startPos = this.mark
    new BigDecimal(new java.math.BigDecimal(new String(buf, 0, startPos, pos - startPos)))
  }

  private[this] def readNullOrNumberError[@sp A](default: A, pos: Int): A =
    if (default == null) numberError(pos - 1)
    else parseNullOrError(default, "expected number or null", pos)

  private[this] def numberError(pos: Int): Nothing = decodeError("illegal number", pos)

  private[this] def digitsLimitError(pos: Int): Nothing =
    decodeError("value exceeds limit for number of significant digits", pos)

  private[this] def scaleLimitError(pos: Int): Nothing = decodeError("value exceeds limit for scale", pos)

  private[this] def leadingZeroError(pos: Int): Nothing = decodeError("illegal number with leading zero", pos)

  private[this] def byteOverflowError(pos: Int): Nothing = decodeError("value is too large for byte", pos)

  private[this] def shortOverflowError(pos: Int): Nothing = decodeError("value is too large for short", pos)

  private[this] def intOverflowError(pos: Int): Nothing = decodeError("value is too large for int", pos)

  private[this] def longOverflowError(pos: Int): Nothing = decodeError("value is too large for long", pos)

  private[this] def parseDuration(): Duration = {
    var seconds = 0L
    var nanos, state = 0
    var b = nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b != 'P') {
      if (isNeg) tokenError('P')
      else tokensError('P', '-')
    }
    b = nextByte(head)
    do {
      if (state == 0) {
        if (b == 'T') {
          b = nextByte(head)
          state = 1
        }
      } else if (state == 1) {
        if (b == 'T') b = nextByte(head)
        else tokensError('T', '"')
      } else if (state == 4) tokenError('"')
      val isNegX = b == '-'
      if (isNegX) b = nextByte(head)
      if (b < '0' || b > '9') {
        if (isNegX) digitError()
        if (state < 2) tokenOrDigitError('-')
        decodeError("expected '\"' or '-' or digit")
      }
      var pos = head
      var x: Long = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (x < -922337203685477580L) durationError(pos)
        x = x * 10 + ('0' - b)
        if (x > 0) durationError(pos)
        pos += 1
      }
      if (!(isNeg ^ isNegX)) {
        if (x == -9223372036854775808L) durationError(pos)
        x = -x
      }
      if (state < 1 && b == 'D') {
        if (x < -106751991167300L || x > 106751991167300L) durationError(pos) // -106751991167300L == Long.MinValue / 86400
        seconds = x * 86400
        state = 1
      } else if (state < 2 && b == 'H') {
        if (x < -2562047788015215L || x > 2562047788015215L) durationError(pos) // -2562047788015215L == Long.MinValue / 3600
        seconds = sumSeconds(x * 3600, seconds, pos)
        state = 2
      } else if (state < 3 && b == 'M') {
        if (x < -153722867280912930L || x > 153722867280912930L) durationError(pos) // -153722867280912930L == Long.MinValue / 60
        seconds = sumSeconds(x * 60, seconds, pos)
        state = 3
      } else if (b == '.') {
        pos += 1
        seconds = sumSeconds(x, seconds, pos)
        var nanoDigitWeight = 100000000
        while ({
          if (pos >= tail) pos = loadMoreOrError(pos)
          b = buf(pos)
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nanos += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
          pos += 1
        }
        if (b != 'S') nanoError(nanoDigitWeight, 'S', pos)
        if (isNeg ^ isNegX) nanos = -nanos
        state = 4
      } else if (b == 'S') {
        seconds = sumSeconds(x, seconds, pos)
        state = 4
      } else durationError(state, pos)
      b = nextByte(pos + 1)
    } while (b != '"')
    Duration.ofSeconds(seconds, nanos)
  }

  private[this] def sumSeconds(s1: Long, s2: Long, pos: Int): Long = {
    val s = s1 + s2
    if (((s1 ^ s) & (s2 ^ s)) < 0) durationError(pos)
    s
  }

  private[this] def parseInstant(): Instant = {
    val year = parseYearWithByte('-', 10, head)
    val month = next2DigitsWithByte('-', head)
    val day = next2DigitsWithByte('T', head)
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithByte('Z')
      else if (b != 'Z') tokensError('.', 'Z')
    } else if (b != 'Z') tokensError(':', 'Z')
    nextByteOrError('"', head)
    Instant.ofEpochSecond(epochSecond(year, month, day, hour, minute, second), nano)
  }

  private[this] def parseLocalDate(): LocalDate =
    toLocalDate(parseYearWithByte('-', 9, head), next2DigitsWithByte('-', head), next2DigitsWithByte('"', head))

  private[this] def parseLocalDateTime(): LocalDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = next2DigitsWithByte('-', head)
    val day = next2DigitsWithByte('T', head)
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithByte('"')
      else if (b != '"') tokensError('.', '"')
    } else if (b != '"') tokensError(':', '"')
    LocalDateTime.of(toLocalDate(year, month, day), toLocalTime(hour, minute, second, nano))
  }

  private[this] def parseLocalTime(): LocalTime = {
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithByte('"')
      else if (b != '"') tokensError('.', '"')
    } else if (b != '"') tokensError(':', '"')
    toLocalTime(hour, minute, second, nano)
  }

  @tailrec
  private[this] def parseMonthDay(pos: Int): MonthDay =
    if (pos + 7 < tail) {
      val buf = this.buf
      val md1 = buf(pos + 2) - '0'
      val md2 = buf(pos + 3) - '0'
      val dd1 = buf(pos + 5) - '0'
      val dd2 = buf(pos + 6) - '0'
      if (buf(pos) != '-') tokenError('-', pos)
      if (buf(pos + 1) != '-') tokenError('-', pos + 1)
      if (md1 < 0 || md1 > 9) digitError(pos + 2)
      if (md2 < 0 || md2 > 9) digitError(pos + 3)
      if (buf(pos + 4) != '-') tokenError('-', pos + 4)
      if (dd1 < 0 || dd1 > 9) digitError(pos + 5)
      if (dd2 < 0 || dd2 > 9) digitError(pos + 6)
      if (buf(pos + 7) != '"') tokenError('"', pos + 7)
      head = pos + 8
      toMonthDay(md1 * 10 + md2, dd1 * 10 + dd2)
    } else parseMonthDay(loadMoreOrError(pos))

  private[this] def parseOffsetDateTime(): OffsetDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = next2DigitsWithByte('-', head)
    val day = next2DigitsWithByte('T', head)
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano = 0
    var nanoDigitWeight = 100000000
    var hasSecond, hasNano = false
    var b = nextByte(head)
    if (b == ':') {
      hasSecond = true
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') {
        hasNano = true
        var pos = head
        while ({
          if (pos >= tail) pos = loadMoreOrError(pos)
          b = buf(pos)
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
          pos += 1
        }
        head = pos + 1
      }
    }
    val zoneOffset =
      if (b == 'Z') {
        nextByteOrError('"', head)
        ZoneOffset.UTC
      } else {
        val offsetNeg = b == '-' || (b != '+' && timeError(hasSecond, hasNano, nanoDigitWeight))
        val offsetHour = next2Digits(head)
        var offsetMinute, offsetSecond = 0
        b = nextByte(head)
        if (b == ':') {
          offsetMinute = next2Digits(head)
          b = nextByte(head)
          if (b == ':') offsetSecond = next2DigitsWithByte('"', head)
          else if (b != '"') tokensError(':', '"')
        } else if (b != '"') tokensError(':', '"')
        toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      }
    OffsetDateTime.of(toLocalDate(year, month, day), toLocalTime(hour, minute, second, nano), zoneOffset)
  }

  private[this] def parseOffsetTime(): OffsetTime = {
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano = 0
    var nanoDigitWeight = 100000000
    var hasSecond, hasNano = false
    var b = nextByte(head)
    if (b == ':') {
      hasSecond = true
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') {
        hasNano = true
        var pos = head
        while ({
          if (pos >= tail) pos = loadMoreOrError(pos)
          b = buf(pos)
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
          pos += 1
        }
        head = pos + 1
      }
    }
    val zoneOffset =
      if (b == 'Z') {
        nextByteOrError('"', head)
        ZoneOffset.UTC
      } else {
        val offsetNeg = b == '-' || (b != '+' && timeError(hasSecond, hasNano, nanoDigitWeight))
        val offsetHour = next2Digits(head)
        var offsetMinute, offsetSecond = 0
        b = nextByte(head)
        if (b == ':') {
          offsetMinute = next2Digits(head)
          b = nextByte(head)
          if (b == ':') offsetSecond = next2DigitsWithByte('"', head)
          else if (b != '"') tokensError(':', '"')
        } else if (b != '"') tokensError(':', '"')
        toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      }
    OffsetTime.of(toLocalTime(hour, minute, second, nano), zoneOffset)
  }

  private[this] def parsePeriod(): Period = {
    var years, months, days, state = 0
    var b = nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b != 'P') {
      if (isNeg) tokenError('P')
      else tokensError('P', '-')
    }
    b = nextByte(head)
    do {
      if (state == 4) tokenError('"')
      val isNegX = b == '-'
      if (isNegX) b = nextByte(head)
      if (b < '0' || b > '9') {
        if (isNegX) digitError()
        if (state < 1) tokenOrDigitError('-')
        decodeError("expected '\"' or '-' or digit")
      }
      var pos = head
      var x = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (x < -214748364) periodError(pos)
        x = x * 10 + ('0' - b)
        if (x > 0) periodError(pos)
        pos += 1
      }
      if (!(isNeg ^ isNegX)) {
        if (x == -2147483648) periodError(pos)
        x = -x
      }
      if (state < 1 && b == 'Y') {
        years = x
        state = 1
      } else if (state < 2 && b == 'M') {
        months = x
        state = 2
      } else if (state < 3 && b == 'W') {
        val ds = x * 7L
        if (ds != ds.toInt) periodError(pos)
        days = ds.toInt
        state = 3
      } else if (b == 'D') {
        val ds = x + days.toLong
        if (ds != ds.toInt) periodError(pos)
        days = ds.toInt
        state = 4
      } else periodError(state, pos)
      b = nextByte(pos + 1)
    } while (b != '"')
    Period.of(years, months, days)
  }

  private[this] def parseYearMonth(): YearMonth =
    toYearMonth(parseYearWithByte('-', 9, head), next2DigitsWithByte('"', head))

  private[this] def parseZonedDateTime(): ZonedDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = next2DigitsWithByte('-', head)
    val day = next2DigitsWithByte('T', head)
    val hour = next2DigitsWithByte(':', head)
    val minute = next2Digits(head)
    var second, nano, offsetHour, offsetMinute, offsetSecond = 0
    var nanoDigitWeight = 100000000
    var zone: String = null
    var hasSecond, hasNano, offsetNeg, hasOffsetHour, hasOffsetSecond = false
    var b = nextByte(head)
    if (b == ':') {
      hasSecond = true
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') {
        hasNano = true
        var pos = head
        while ({
          if (pos >= tail) pos = loadMoreOrError(pos)
          b = buf(pos)
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
          pos += 1
        }
        head = pos + 1
      }
    }
    if (b != 'Z') {
      hasOffsetHour = true
      offsetNeg = b == '-' || (b != '+' && timeError(hasSecond, hasNano, nanoDigitWeight))
      offsetHour = next2Digits(head)
      b = nextByte(head)
      if (b == ':') {
        offsetMinute = next2Digits(head)
        b = nextByte(head)
        if (b == ':') {
          hasOffsetSecond = true
          offsetSecond = next2Digits(head)
          b = nextByte(head)
        }
      }
    } else b = nextByte(head)
    if (b == '[') {
      zone = parseZoneIdUntilToken(']')
      b = nextByte(head)
    }
    if (b != '"') {
      if (zone ne null) tokenError('"')
      if (hasOffsetSecond || !hasOffsetHour) tokensError('[', '"')
      decodeError("expected ':' or '[' or '\"'")
    }
    val localDateTime = LocalDateTime.of(toLocalDate(year, month, day), toLocalTime(hour, minute, second, nano))
    val zoneOffset =
      if (hasOffsetHour) toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      else ZoneOffset.UTC
    if (zone eq null) ZonedDateTime.of(localDateTime, zoneOffset)
    else ZonedDateTime.ofInstant(localDateTime, zoneOffset, toZoneId(zone))
  }

  private[this] def parseZoneOffset(): ZoneOffset = {
    var b = nextByte(head)
    if (b == 'Z') {
      nextByteOrError('"', head)
      ZoneOffset.UTC
    } else {
      val offsetNeg = b == '-' || (b != '+' && decodeError("expected '+' or '-' or 'Z'"))
      val offsetHour = next2Digits(head)
      var offsetMinute, offsetSecond = 0
      b = nextByte(head)
      if (b == ':') {
        offsetMinute = next2Digits(head)
        b = nextByte(head)
        if (b == ':') offsetSecond = next2DigitsWithByte('"', head)
        else if (b != '"') tokensError(':', '"')
      } else if (b != '"') tokensError(':', '"')
      toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
    }
  }

  private[this] def epochSecond(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = {
    if (year < -1000000000 || year > 1000000000) yearError()
    if (month < 1 || month > 12) monthError()
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) dayError()
    if (hour > 23) hourError()
    if (minute > 59) minuteError()
    if (second > 59) secondError()
    (epochDayForYear(year) + (dayOfYearForYearMonth(year, month) + day - 719529)) * 86400 + // 719528 == days 0000 to 1970
      (hour * 3600 + minute * 60 + second)
  }

  private[this] def toLocalDate(year: Int, month: Int, day: Int): LocalDate = {
    if (year < -999999999 || year > 999999999) yearError()
    if (month < 1 || month > 12) monthError()
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) dayError()
    LocalDate.of(year, month, day)
  }

  private[this] def toYear(year: Int): Year = {
    if (year < -999999999 || year > 999999999) yearError()
    Year.of(year)
  }

  private[this] def toYearMonth(year: Int, month: Int): YearMonth = {
    if (year < -999999999 || year > 999999999) yearError()
    if (month < 1 || month > 12) monthError()
    YearMonth.of(year, month)
  }

  private[this] def toMonthDay(month: Int, day: Int): MonthDay = {
    if (month < 1 || month > 12) monthError()
    if (day < 1 || (day > 28 && day > maxDayForMonth(month))) dayError()
    MonthDay.of(month, day)
  }

  private[this] def toLocalTime(hour: Int, minute: Int, second: Int, nano: Int): LocalTime = {
    if (hour > 23) hourError()
    if (minute > 59) minuteError()
    if (second > 59) secondError()
    LocalTime.of(hour, minute, second, nano)
  }

  private[this] def toZoneOffset(isNeg: Boolean, offsetHour: Int, offsetMinute: Int, offsetSecond: Int): ZoneOffset = {
    val offsetTotal = offsetHour * 3600 + offsetMinute * 60 + offsetSecond
    if (offsetHour > 18) timezoneOffsetHourError()
    if (offsetMinute > 59) timezoneOffsetMinuteError()
    if (offsetSecond > 59) timezoneOffsetSecondError()
    if (offsetTotal > 64800) timezoneOffsetError() // 64800 == 18 * 60 * 60
    val q1 = (offsetTotal * 2443359173L >> 41).toInt // div positive int by 900
    if (q1 * 900 == offsetTotal) zoneOffsets {
      if (isNeg) 72 - q1
      else 72 + q1
    } else ZoneOffset.ofTotalSeconds {
      if (isNeg) -offsetTotal
      else offsetTotal
    }
  }

  private[this] def toZoneId(zone: String): ZoneId = {
    val x = zoneIds.get(zone)
    if (x ne null) x
    else try ZoneId.of(zone) catch {
      case ex: DateTimeException => timezoneError(ex)
    }
  }

  private[this] def epochDayForYear(year: Int): Long =
    365L * year + (((year + 3) >> 2) - (if (year < 0) {
      val century = year * 1374389535L >> 37 // divide int by 100 (a sign correction is not required)
      century - (century >> 2)
    } else ((year + 99) * 1374389535L >> 37) - ((year + 399) * 1374389535L >> 39))) // divide int by 100 and by 400 accordingly (a sign correction is not required)

  private[this] def dayOfYearForYearMonth(year: Int, month: Int): Int =
    ((month * 1050835331877L - 1036518774222L) >> 35).toInt - // == (367 * month - 362) / 12
      (if (month <= 2) 0
      else if (isLeap(year)) 1
      else 2)

  private[this] def maxDayForYearMonth(year: Int, month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 0x1)) + 30
    else if (isLeap(year)) 29
    else 28

  private[this] def maxDayForMonth(month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 0x1)) + 30
    else 29

  private[this] def isLeap(year: Int): Boolean = (year & 0x3) == 0 && {
    val century = (year * 1374389535L >> 37).toInt - (year >> 31) // divide int by 100
    century * 100 != year || (century & 0x3) == 0
  }

  private[this] def digitError(pos: Int = head - 1): Nothing = decodeError("expected digit", pos)

  private[this] def periodError(pos: Int): Nothing = decodeError("illegal period", pos)

  private[this] def periodError(state: Int, pos: Int): Nothing = decodeError((state: @switch) match {
    case 0 => "expected 'Y' or 'M' or 'W' or 'D' or digit"
    case 1 => "expected 'M' or 'W' or 'D' or digit"
    case 2 => "expected 'W' or 'D' or digit"
    case 3 => "expected 'D' or digit"
  }, pos)

  private[this] def durationError(pos: Int): Nothing = decodeError("illegal duration", pos)

  private[this] def durationError(state: Int, pos: Int): Nothing = decodeError((state: @switch) match {
    case 0 => "expected 'D' or digit"
    case 1 => "expected 'H' or 'M' or 'S or '.' or digit"
    case 2 => "expected 'M' or 'S or '.' or digit"
    case 3 => "expected 'S or '.' or digit"
  }, pos)

  private[this] def yearError(): NotImplementedError = decodeError("illegal year")

  private[this] def monthError(): NotImplementedError = decodeError("illegal month")

  private[this] def dayError(): NotImplementedError = decodeError("illegal day")

  private[this] def hourError(): NotImplementedError = decodeError("illegal hour")

  private[this] def minuteError(): NotImplementedError = decodeError("illegal minute")

  private[this] def secondError(): NotImplementedError = decodeError("illegal second")

  private[this] def nanoError(nanoDigitWeight: Int, t: Byte, pos: Int): Nothing =
    if (nanoDigitWeight == 0) tokenError(t, pos)
    else tokenOrDigitError(t, pos)

  private[this] def timeError(hasSecond: Boolean, hasNano: Boolean, nanoDigitWeight: Int): Nothing = decodeError {
    if (hasSecond) {
      if (hasNano) {
        if (nanoDigitWeight == 0) "expected '+' or '-' or 'Z'"
        else "expected '+' or '-' or 'Z' or digit"
      } else "expected '.' or '+' or '-' or 'Z'"
    } else "expected ':' or '+' or '-' or 'Z'"
  }

  private[this] def timezoneError(ex: DateTimeException): Nothing = decodeError("illegal timezone", head - 1, ex)

  private[this] def timezoneOffsetError(): Nothing = decodeError("illegal timezone offset")

  private[this] def timezoneOffsetHourError(): Nothing = decodeError("illegal timezone offset hour")

  private[this] def timezoneOffsetMinuteError(): Nothing = decodeError("illegal timezone offset minute")

  private[this] def timezoneOffsetSecondError(): Nothing = decodeError("illegal timezone offset second")

  @tailrec
  private[this] def parseUUID(pos: Int): UUID =
    if (pos + 36 < tail) {
      val ns = nibbles
      val buf = this.buf
      val mostSigBits1: Long =
        (ns(buf(pos) & 0xFF).toLong << 28) |
        (ns(buf(pos + 1) & 0xFF) << 24) |
        (ns(buf(pos + 2) & 0xFF) << 20) |
        (ns(buf(pos + 3) & 0xFF) << 16) |
        (ns(buf(pos + 4) & 0xFF) << 12) |
        (ns(buf(pos + 5) & 0xFF) << 8) |
        (ns(buf(pos + 6) & 0xFF) << 4) |
        ns(buf(pos + 7) & 0xFF)
      val mostSigBits2: Int =
        (ns(buf(pos + 9) & 0xFF) << 12) |
        (ns(buf(pos + 10) & 0xFF) << 8) |
        (ns(buf(pos + 11) & 0xFF) << 4) |
        ns(buf(pos + 12) & 0xFF)
      val mostSigBits3: Int =
        (ns(buf(pos + 14) & 0xFF) << 12) |
        (ns(buf(pos + 15) & 0xFF) << 8) |
        (ns(buf(pos + 16) & 0xFF) << 4) |
        ns(buf(pos + 17) & 0xFF)
      val leastSigBits1: Int =
        (ns(buf(pos + 19) & 0xFF) << 12) |
        (ns(buf(pos + 20) & 0xFF) << 8) |
        (ns(buf(pos + 21) & 0xFF) << 4) |
        ns(buf(pos + 22) & 0xFF)
      val leastSigBits2: Long =
        (ns(buf(pos + 24) & 0xFF).toLong << 44) |
        (ns(buf(pos + 25) & 0xFF).toLong << 40) |
        (ns(buf(pos + 26) & 0xFF).toLong << 36) |
        (ns(buf(pos + 27) & 0xFF).toLong << 32) |
        (ns(buf(pos + 28) & 0xFF).toLong << 28) |
        (ns(buf(pos + 29) & 0xFF) << 24) |
        (ns(buf(pos + 30) & 0xFF) << 20) |
        (ns(buf(pos + 31) & 0xFF) << 16) |
        (ns(buf(pos + 32) & 0xFF) << 12) |
        (ns(buf(pos + 33) & 0xFF) << 8) |
        (ns(buf(pos + 34) & 0xFF) << 4) |
        ns(buf(pos + 35) & 0xFF)
      if (mostSigBits1 < 0) hexDigitError(pos)
      if (buf(pos + 8) != '-') tokenError('-', pos + 8)
      if (mostSigBits2 < 0) hexDigitError(pos + 9)
      if (buf(pos + 13) != '-') tokenError('-', pos + 13)
      if (mostSigBits3 < 0) hexDigitError(pos + 14)
      if (buf(pos + 18) != '-') tokenError('-', pos + 18)
      if (leastSigBits1 < 0) hexDigitError(pos + 19)
      if (buf(pos + 23) != '-') tokenError('-', pos + 23)
      if (leastSigBits2 < 0) hexDigitError(pos + 24)
      if (buf(pos + 36) != '"') tokenError('"', pos + 36)
      head = pos + 37
      new UUID((mostSigBits1 << 32) | (mostSigBits2.toLong << 16) | mostSigBits3,
        (leastSigBits1.toLong << 48) | leastSigBits2)
    } else parseUUID(loadMoreOrError(pos))

  private[this] def parseString(): Int = parseString(0, Math.min(charBuf.length, tail - head), charBuf, head)

  @tailrec
  private[this] def parseString(i: Int, minLim: Int, charBuf: Array[Char], pos: Int): Int =
    if (i < minLim) {
      val b = buf(pos)
      charBuf(i) = b.toChar
      if (b == '"') {
        head = pos + 1
        i
      } else if (((b - 32) ^ 60) > 0) parseString(i + 1, minLim, charBuf, pos + 1) // == else if (b >= ' ' && b != '\\') ...
      else parseEncodedString(i, charBuf.length - 1, charBuf, pos)
    } else if (pos >= tail) {
      val newPos = loadMoreOrError(pos)
      parseString(i, Math.min(charBuf.length, i + tail - newPos), charBuf, newPos)
    } else parseString(i, Math.min(growCharBuf(i + 1), i + tail - pos), this.charBuf, pos)

  @tailrec
  private[this] def parseEncodedString(i: Int, lim: Int, charBuf: Array[Char], pos: Int): Int = {
    val remaining = tail - pos
    if (i < lim) {
      if (remaining > 0) {
        val b1 = buf(pos)
        if (b1 >= 0) { // 0aaaaaaa (UTF-8 byte) -> 000000000aaaaaaa (UTF-16 char)
          if (b1 == '"') {
            head = pos + 1
            i
          } else if (b1 != '\\') {
            if (b1 < ' ') unescapedControlCharacterError(pos)
            charBuf(i) = b1.toChar
            parseEncodedString(i + 1, lim, charBuf, pos + 1)
          } else if (remaining > 1) {
            val b2 = buf(pos + 1)
            if (b2 == 'u') {
              if (remaining > 5) {
                val ch1 = readEscapedUnicode(pos + 2, buf)
                charBuf(i) = ch1
                if (ch1 < 0xD800 || ch1 > 0xDFFF) parseEncodedString(i + 1, lim, charBuf, pos + 6)
                else if (remaining > 11) {
                  if (buf(pos + 6) != '\\') illegalEscapeSequenceError(pos + 6)
                  if (buf(pos + 7) != 'u') illegalEscapeSequenceError(pos + 7)
                  val ch2 = readEscapedUnicode(pos + 8, buf)
                  if (ch1 >= 0xDC00 || ch2 < 0xDC00 || ch2 > 0xDFFF) decodeError("illegal surrogate character pair", pos + 11)
                  charBuf(i + 1) = ch2
                  parseEncodedString(i + 2, lim, charBuf, pos + 12)
                } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
              } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
            } else {
              charBuf(i) = (b2: @switch) match {
                case '"' => '"'
                case 'n' => '\n'
                case 'r' => '\r'
                case 't' => '\t'
                case 'b' => '\b'
                case 'f' => '\f'
                case '\\' => '\\'
                case '/' => '/'
                case _ => illegalEscapeSequenceError(pos + 1)
              }
              parseEncodedString(i + 1, lim, charBuf, pos + 2)
            }
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else if ((b1 >> 5) == -2) { // 110bbbbb 10aaaaaa (UTF-8 bytes) -> 00000bbbbbaaaaaa (UTF-16 char)
          if (remaining > 1) {
            val b2 = buf(pos + 1)
            if ((b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80) malformedBytesError(b1, b2, pos)
            charBuf(i) = ((b1 << 6) ^ (b2 ^ 0xF80)).toChar // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
            parseEncodedString(i + 1, lim, charBuf, pos + 2)
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else if ((b1 >> 4) == -2) { // 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes) -> ccccbbbbbbaaaaaa (UTF-16 char)
          if (remaining > 2) {
            val b2 = buf(pos + 1)
            val b3 = buf(pos + 2)
            val ch = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
            if ((b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 ||
              (ch >= 0xD800 && ch <= 0xDFFF)) malformedBytesError(b1, b2, b3, pos)
            charBuf(i) = ch
            parseEncodedString(i + 1, lim, charBuf, pos + 3)
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else if ((b1 >> 3) == -2) { // 11110ddd 10ddcccc 10bbbbbb 10aaaaaa (UTF-8 bytes) -> 110110uuuuccccbb 110111bbbbaaaaaa (UTF-16 chars), where uuuu = ddddd - 1
          if (remaining > 3) {
            val b2 = buf(pos + 1)
            val b3 = buf(pos + 2)
            val b4 = buf(pos + 3)
            val cp = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) // 0x381F80 == ((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
            if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80 ||
              cp < 0x010000 || cp > 0x10FFFF) malformedBytesError(b1, b2, b3, b4, pos)
            charBuf(i) = ((cp >>> 10) + 0xD7C0).toChar // 0xD7C0 == 0xD800 - (0x010000 >>> 10)
            charBuf(i + 1) = ((cp & 0x3FF) + 0xDC00).toChar
            parseEncodedString(i + 2, lim, charBuf, pos + 4)
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else malformedBytesError(b1, pos)
      } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
    } else parseEncodedString(i, growCharBuf(i + 2) - 1, this.charBuf, pos) // 2 is length of surrogate pair
  }

  @tailrec
  private[this] def parseChar(pos: Int): Char = {
    val remaining = tail - pos
    if (remaining > 0) {
      val b1 = buf(pos)
      if (b1 >= 0) { // 0aaaaaaa (UTF-8 byte) -> 000000000aaaaaaa (UTF-16 char)
        if (b1 == '"') decodeError("illegal value for char", pos)
        else if (b1 != '\\') {
          if (b1 < ' ') unescapedControlCharacterError(pos)
          head = pos + 1
          b1.toChar
        } else if (remaining > 1) {
          val b2 = buf(pos + 1)
          if (b2 == 'u') {
            if (remaining > 5) {
              val ch = readEscapedUnicode(pos + 2, buf)
              if (ch >= 0xD800 && ch <= 0xDFFF) decodeError("illegal surrogate character", pos + 5)
              head = pos + 6
              ch
            } else parseChar(loadMoreOrError(pos))
          } else {
            head = pos + 2
            (b2: @switch) match {
              case 'b' => '\b'
              case 'f' => '\f'
              case 'n' => '\n'
              case 'r' => '\r'
              case 't' => '\t'
              case '"' => '"'
              case '/' => '/'
              case '\\' => '\\'
              case _ => illegalEscapeSequenceError(pos + 1)
            }
          }
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 5) == -2) { // 110bbbbb 10aaaaaa (UTF-8 bytes) -> 00000bbbbbaaaaaa (UTF-16 char)
        if (remaining > 1) {
          val b2 = buf(pos + 1)
          if ((b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80) malformedBytesError(b1, b2, pos)
          head = pos + 2
          ((b1 << 6) ^ (b2 ^ 0xF80)).toChar // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 4) == -2) { // 1110cccc 10bbbbbb 10aaaaaa (UTF-8 bytes) -> ccccbbbbbbaaaaaa (UTF-16 char)
        if (remaining > 2) {
          val b2 = buf(pos + 1)
          val b3 = buf(pos + 2)
          val ch = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
          if ((b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 ||
            (ch >= 0xD800 && ch <= 0xDFFF)) malformedBytesError(b1, b2, b3, pos)
          head = pos + 3
          ch
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 3) == -2) decodeError("illegal surrogate character", pos + 3)
      else malformedBytesError(b1, pos)
    } else parseChar(loadMoreOrError(pos))
  }

  private[this] def readEscapedUnicode(pos: Int, buf: Array[Byte]): Char = {
    val ns = nibbles
    val x =
      (ns(buf(pos) & 0xFF) << 12) |
      (ns(buf(pos + 1) & 0xFF) << 8) |
      (ns(buf(pos + 2) & 0xFF) << 4) |
      ns(buf(pos + 3) & 0xFF)
    if (x < 0) hexDigitError(pos)
    x.toChar
  }

  @tailrec
  private[this] def hexDigitError(pos: Int): Nothing = {
    if (nibbles(buf(pos) & 0xFF) < 0) decodeError("expected hex digit", pos)
    hexDigitError(pos + 1)
  }

  private[this] def illegalEscapeSequenceError(pos: Int): Nothing = decodeError("illegal escape sequence", pos)

  private[this] def unescapedControlCharacterError(pos: Int): Nothing = decodeError("unescaped control character", pos)

  private[this] def malformedBytesError(b1: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHexByte(b1, i, hexDigits)
    decodeError(i, pos, null)
  }

  private[this] def malformedBytesError(b1: Byte, b2: Byte, pos: Int): Nothing = {
    val ds = hexDigits
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHexByte(b1, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b2, i, ds)
    decodeError(i, pos + 1, null)
  }

  private[this] def malformedBytesError(b1: Byte, b2: Byte, b3: Byte, pos: Int): Nothing = {
    val ds = hexDigits
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHexByte(b1, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b2, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b3, i, ds)
    decodeError(i, pos + 2, null)
  }

  private[this] def malformedBytesError(b1: Byte, b2: Byte, b3: Byte, b4: Byte, pos: Int): Nothing = {
    val ds = hexDigits
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHexByte(b1, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b2, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b3, i, ds)
    i = appendString(", 0x", i)
    i = appendHexByte(b4, i, ds)
    decodeError(i, pos + 3, null)
  }

  private[this] def appendHexDump(start: Int, end: Int, offset: Int, from: Int): Int = {
    val alignedAbsFrom = (start + offset) & 0xFFFFFFF0
    val alignedAbsTo = (end + offset + 15) & 0xFFFFFFF0
    val len = alignedAbsTo - alignedAbsFrom
    val bufOffset = alignedAbsFrom - offset
    var i = appendChars(dumpHeader, from)
    i = appendChars(dumpBorder, i)
    val ds = hexDigits
    var charBuf = this.charBuf
    var lim = charBuf.length
    var j = 0
    while (j < len) {
      val linePos = j & 0xF
      if (linePos == 0) {
        if (i + 81 >= lim) { // 81 == dumpBorder.length
          lim = growCharBuf(i + 81)
          charBuf = this.charBuf
        }
        charBuf(i) = '\n'
        charBuf(i + 1) = '|'
        charBuf(i + 2) = ' '
        putHexInt(alignedAbsFrom + j, i + 3, charBuf, ds)
        charBuf(i + 11) = ' '
        charBuf(i + 12) = '|'
        charBuf(i + 13) = ' '
        i += 14
      }
      val pos = bufOffset + j
      charBuf(i + 50 - (linePos << 1)) =
        if (pos >= start && pos < end) {
          val b = buf(pos)
          charBuf(i) = ds((b >>> 4) & 0xF)
          charBuf(i + 1) = ds(b & 0xF)
          charBuf(i + 2) = ' '
          if (b <= 31 || b >= 127) '.'
          else b.toChar
        } else {
          charBuf(i) = ' '
          charBuf(i + 1) = ' '
          charBuf(i + 2) = ' '
          ' '
        }
      i += 3
      if (linePos == 15) {
        charBuf(i) = '|'
        charBuf(i + 1) = ' '
        charBuf(i + 18) = ' '
        charBuf(i + 19) = '|'
        i += 20
      }
      j += 1
    }
    appendChars(dumpBorder, i)
  }

  private[this] def appendHexOffset(d: Long, i: Int, ds: Array[Char]): Int = {
    if (i + 16 >= charBuf.length) growCharBuf(i + 16)
    val ds = hexDigits
    var j = i
    val hd = (d >>> 32).toInt
    if (hd != 0) {
      var shift = 4
      while (shift < 32 && (hd >>> shift) != 0) shift += 4
      while (shift > 0) {
        shift -= 4
        charBuf(j) = ds((hd >>> shift) & 0xF)
        j += 1
      }
    }
    putHexInt(d.toInt, j, charBuf, ds)
    j + 8
  }

  private[this] def appendHexByte(b: Byte, i: Int, ds: Array[Char]): Int = {
    if (i + 2 >= charBuf.length) growCharBuf(i + 2)
    charBuf(i) = ds((b >>> 4) & 0xF)
    charBuf(i + 1) = ds(b & 0xF)
    i + 2
  }

  private[this] def putHexInt(d: Int, i: Int, charBuf: Array[Char], ds: Array[Char]): Unit = {
    charBuf(i) = ds(d >>> 28)
    charBuf(i + 1) = ds((d >>> 24) & 0xF)
    charBuf(i + 2) = ds((d >>> 20) & 0xF)
    charBuf(i + 3) = ds((d >>> 16) & 0xF)
    charBuf(i + 4) = ds((d >>> 12) & 0xF)
    charBuf(i + 5) = ds((d >>> 8) & 0xF)
    charBuf(i + 6) = ds((d >>> 4) & 0xF)
    charBuf(i + 7) = ds(d & 0xF)
  }

  private[this] def growCharBuf(required: Int): Int = {
    val newLim = Integer.highestOneBit(charBuf.length | required) << 1
    charBuf = java.util.Arrays.copyOf(charBuf, newLim)
    newLim
  }

  @tailrec
  private[this] def skipString(evenBackSlashes: Boolean, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"' && evenBackSlashes) pos + 1
      else skipString(b != '\\' || !evenBackSlashes, pos + 1)
    } else skipString(evenBackSlashes, loadMoreOrError(pos))

  private[this] def skipNumber(p: Int): Int = {
    var pos = p
    while ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      val b = buf(pos)
      (b >= '0' && b <= '9') || b == '.' || (b | 0x20) == 'e' || b == '-' || b == '+'
    }) pos += 1
    pos
  }

  @tailrec
  private[this] def skipObject(level: Int, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') skipObject(level, skipString(evenBackSlashes = true, pos + 1))
      else if (b == '}') {
        if (level == 0) pos + 1
        else skipObject(level - 1, pos + 1)
      } else if (b == '{') skipObject(level + 1, pos + 1)
      else skipObject(level, pos + 1)
    } else skipObject(level, loadMoreOrError(pos))

  @tailrec
  private[this] def skipArray(level: Int, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') skipArray(level, skipString(evenBackSlashes = true, pos + 1))
      else if (b == ']') {
        if (level == 0) pos + 1
        else skipArray(level - 1, pos + 1)
      } else if (b == '[') skipArray(level + 1, pos + 1)
      else skipArray(level, pos + 1)
    } else skipArray(level, loadMoreOrError(pos))

  @tailrec
  private[this] def skipFixedBytes(n: Int, pos: Int): Int = {
    val newPos = pos + n
    val diff = newPos - tail
    if (diff <= 0) newPos
    else skipFixedBytes(diff, loadMoreOrError(pos))
  }

  private[this] def loadMoreOrError(pos: Int): Int =
    if (bbuf ne null) {
      val newPos = ensureBufCapacity(pos)
      val n = Math.min(bbuf.remaining, buf.length - tail)
      if (n > 0) {
        bbuf.get(buf, tail, n)
        tail += n
        totalRead += n
        newPos
      } else endOfInputError()
    } else if (in ne null) {
      val newPos = ensureBufCapacity(pos)
      val n = in.read(buf, tail, buf.length - tail)
      if (n > 0) {
        tail += n
        totalRead += n
        newPos
      } else endOfInputError()
    } else endOfInputError()

  private[this] def loadMore(pos: Int): Int =
    if (bbuf ne null) {
      val newPos = ensureBufCapacity(pos)
      val n = Math.min(bbuf.remaining, buf.length - tail)
      if (n > 0) {
        bbuf.get(buf, tail, n)
        this.tail = tail + n
        totalRead += n
      }
      newPos
    } else if (in ne null) {
      val newPos = ensureBufCapacity(pos)
      val n = in.read(buf, tail, buf.length - tail)
      if (n > 0) {
        tail += n
        totalRead += n
      }
      newPos
    } else pos

  private[this] def ensureBufCapacity(pos: Int): Int = {
    val minPos = Math.min(mark, pos)
    if (minPos > 0) {
      val newPos = pos - minPos
      val remaining = tail - minPos
      var i = 0
      while (i < remaining) {
        buf(i) = buf(i + minPos)
        i += 1
      }
      if (mark != 2147483647) mark = 0
      tail = remaining
      head = newPos
      newPos
    } else {
      if (tail > 0) buf = java.util.Arrays.copyOf(buf, buf.length << 1)
      pos
    }
  }

  private[this] def endOfInputError(): Nothing = decodeError("unexpected end of input", tail)

  private[this] def reallocateBufToPreferredSize(): Unit = buf = new Array[Byte](config.preferredBufSize)

  private[this] def reallocateCharBufToPreferredSize(): Unit = charBuf = new Array[Char](config.preferredCharBufSize)
}

object JsonReader {
  private final val pow10f: Array[Float] =
    Array(1f, 1e+1f, 1e+2f, 1e+3f, 1e+4f, 1e+5f, 1e+6f, 1e+7f, 1e+8f, 1e+9f, 1e+10f)
  private final val pow10d: Array[Double] =
    Array(1, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11,
      1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22)
  private final val nibbles: Array[Byte] = {
    val ns = new Array[Byte](256)
    java.util.Arrays.fill(ns, -1: Byte)
    ns('0') = 0
    ns('1') = 1
    ns('2') = 2
    ns('3') = 3
    ns('4') = 4
    ns('5') = 5
    ns('6') = 6
    ns('7') = 7
    ns('8') = 8
    ns('9') = 9
    ns('A') = 10
    ns('B') = 11
    ns('C') = 12
    ns('D') = 13
    ns('E') = 14
    ns('F') = 15
    ns('a') = 10
    ns('b') = 11
    ns('c') = 12
    ns('d') = 13
    ns('e') = 14
    ns('f') = 15
    ns
  }
  private final val zoneOffsets: Array[ZoneOffset] = {
    val zos = new Array[ZoneOffset](145)
    var i = 0
    while (i < 145) {
      zos(i) = ZoneOffset.ofTotalSeconds((i - 72) * 900)
      i += 1
    }
    zos
  }
  private final val zoneIds: java.util.HashMap[String, ZoneId] = {
    val zs = new java.util.HashMap[String, ZoneId](1024)
    val azs = ZoneId.getAvailableZoneIds.iterator()
    while (azs.hasNext) {
      val id = azs.next()
      zs.put(id, ZoneId.of(id))
    }
    val zos = zoneOffsets
    val l = zos.length
    var i = 0
    while (i < l) {
      val z1 = zos(i)
      zs.put(z1.getId, z1)
      val z2 = ZoneId.ofOffset("UT", z1)
      zs.put(z2.getId, z2)
      val z3 = ZoneId.ofOffset("UTC", z1)
      zs.put(z3.getId, z3)
      val z4 = ZoneId.ofOffset("GMT", z1)
      zs.put(z4.getId, z4)
      i += 1
    }
    zs
  }
  private final val hexDigits: Array[Char] =
    Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  private final val dumpHeader: Array[Char] = {
    "\n           +-------------------------------------------------+" +
    "\n           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |"
  }.toCharArray
  private final val dumpBorder: Array[Char] =
    "\n+----------+-------------------------------------------------+------------------+".toCharArray
  final val bigDecimalMathContext: MathContext = MathContext.DECIMAL128
  final val bigDecimalDigitsLimit: Int = 308
  final val bigDecimalScaleLimit: Int = 6178
  final val bigIntDigitsLimit: Int = 308

  /**
    * Calculates hash code value string represented by sequence of characters from begining of the provided char array
    * up to limit position.
    *
    * @param cs a char array
    * @param len an exclusive limit
    * @return a hash code value
    * @throws NullPointerException if the `cs` is null
    * @throws ArrayIndexOutOfBoundsException if the length of `cs` is less than the provided `len`
    */
  final def toHashCode(cs: Array[Char], len: Int): Int = {
    var h, i = 0
    while (i < len) {
      h = (h << 5) + (cs(i) - h)
      i += 1
    }
    h
  }
}