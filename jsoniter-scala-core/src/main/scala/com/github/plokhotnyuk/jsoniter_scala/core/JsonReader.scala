package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.InputStream
import java.math.{BigInteger, MathContext}
import java.nio.ByteBuffer
import java.time._
import java.time.zone.ZoneRulesException
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader._

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
    if (mark == 2147483647) throw new ArrayIndexOutOfBoundsException("expected preceding call of 'setMark()'")
    head = mark
    mark = 2147483647
  }

  def readKeyAsCharBuf(): Int = {
    readParenthesesToken()
    val len = parseString()
    readColonToken()
    len
  }

  def readKeyAsString(): String = {
    readParenthesesToken()
    val len = parseString()
    readColonToken()
    new String(charBuf, 0, len)
  }

  def readKeyAsDuration(): Duration = {
    readParenthesesToken()
    val x = parseDuration()
    readColonToken()
    x
  }

  def readKeyAsInstant(): Instant = {
    readParenthesesToken()
    val x = parseInstant()
    readColonToken()
    x
  }

  def readKeyAsLocalDate(): LocalDate = {
    readParenthesesToken()
    val x = parseLocalDate()
    readColonToken()
    x
  }

  def readKeyAsLocalDateTime(): LocalDateTime = {
    readParenthesesToken()
    val x = parseLocalDateTime()
    readColonToken()
    x
  }

  def readKeyAsLocalTime(): LocalTime = {
    readParenthesesToken()
    val x = parseLocalTime()
    readColonToken()
    x
  }

  def readKeyAsMonthDay(): MonthDay = {
    readParenthesesToken()
    val x = parseMonthDay(head)
    readColonToken()
    x
  }

  def readKeyAsOffsetDateTime(): OffsetDateTime = {
    readParenthesesToken()
    val x = parseOffsetDateTime()
    readColonToken()
    x
  }

  def readKeyAsOffsetTime(): OffsetTime = {
    readParenthesesToken()
    val x = parseOffsetTime()
    readColonToken()
    x
  }

  def readKeyAsPeriod(): Period = {
    readParenthesesToken()
    val x = parsePeriod()
    readColonToken()
    x
  }

  def readKeyAsYear(): Year = {
    readParenthesesToken()
    val x = toYear(parseYearWithToken('"', 9))
    readColonToken()
    x
  }

  def readKeyAsYearMonth(): YearMonth = {
    readParenthesesToken()
    val x = parseYearMonth()
    readColonToken()
    x
  }

  def readKeyAsZonedDateTime(): ZonedDateTime = {
    readParenthesesToken()
    val x = parseZonedDateTime()
    readColonToken()
    x
  }

  def readKeyAsZoneId(): ZoneId = {
    readParenthesesToken()
    val x = toZoneId(parseZoneIdUntilToken('"'))
    readColonToken()
    x
  }

  def readKeyAsZoneOffset(): ZoneOffset = {
    readParenthesesToken()
    val x = parseZoneOffset()
    readColonToken()
    x
  }

  def readKeyAsBoolean(): Boolean = {
    readParenthesesToken()
    val x = parseBoolean(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsByte(): Byte = {
    readParenthesesToken()
    val x = parseByte(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsChar(): Char = {
    readParenthesesToken()
    val x = parseChar(head)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsShort(): Short = {
    readParenthesesToken()
    val x = parseShort(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsInt(): Int = {
    readParenthesesToken()
    val x = parseInt(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsLong(): Long = {
    readParenthesesToken()
    val x = parseLong(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsFloat(): Float = {
    readParenthesesToken()
    val x = parseFloat(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsDouble(): Double = {
    readParenthesesToken()
    val x = parseDouble(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsBigInt(digitsLimit: Int = bigIntDigitsLimit): BigInt = {
    readParenthesesToken()
    val x = parseBigInt(isToken = false, null, digitsLimit)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsBigDecimal(mc: MathContext = bigDecimalMathContext, scaleLimit: Int = bigDecimalScaleLimit,
                          digitsLimit: Int = bigDecimalDigitsLimit): BigDecimal = {
    readParenthesesToken()
    val x = parseBigDecimal(isToken = false, null, mc, scaleLimit, digitsLimit)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsUUID(): UUID = {
    readParenthesesToken()
    val x = parseUUID(head)
    readColonToken()
    x
  }

  def readByte(): Byte = parseByte(isToken = true)

  def readChar(): Char = {
    readParenthesesToken()
    val x = parseChar(head)
    readParenthesesByte()
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
    if (isNextToken('"', head)) toYear(parseYearWithToken('"', 9))
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

  def readBoolean(): Boolean = parseBoolean(isToken = true)

  def readStringAsCharBuf(): Int =
    if (isNextToken('"', head)) parseString()
    else tokenError('"')

  def readStringAsByte(): Byte = {
    readParenthesesToken()
    val x = parseByte(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsShort(): Short = {
    readParenthesesToken()
    val x = parseShort(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsInt(): Int = {
    readParenthesesToken()
    val x = parseInt(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsLong(): Long = {
    readParenthesesToken()
    val x = parseLong(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsDouble(): Double = {
    readParenthesesToken()
    val x = parseDouble(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsFloat(): Float = {
    readParenthesesToken()
    val x = parseFloat(isToken = false)
    readParenthesesByte()
    x
  }

  def readStringAsBigInt(default: BigInt, digitsLimit: Int = bigIntDigitsLimit): BigInt =
    if (isNextToken('"', head)) {
      val x = parseBigInt(isToken = false, default, digitsLimit)
      readParenthesesByte()
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBigDecimal(default: BigDecimal, mc: MathContext = bigDecimalMathContext,
                             scaleLimit: Int = bigDecimalScaleLimit,
                             digitsLimit: Int = bigDecimalDigitsLimit): BigDecimal =
    if (isNextToken('"', head)) {
      val x = parseBigDecimal(isToken = false, default, mc, scaleLimit, digitsLimit)
      readParenthesesByte()
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBoolean(): Boolean = {
    readParenthesesToken()
    val x = parseBoolean(isToken = false)
    readParenthesesByte()
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
    else decodeError("expected value", head - 1)
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
      this.in = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
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
        this.bbuf = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
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
      this.in = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
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
      this.in = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
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

  private[this] def tokenOrDigitError(b: Byte, pos: Int): Nothing = {
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
      i = appendHexDump(Math.max((pos - 32) & -16, 0), Math.min((pos + 48) & -16, tail), off.toInt, i)
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
  private[this] def scanUntilToken(t: Byte, pos: Int): Unit =
    if (pos + 3 < tail) {
      if (buf(pos) == t) head = pos + 1
      else if (buf(pos + 1) == t) head = pos + 2
      else if (buf(pos + 2) == t) head = pos + 3
      else if (buf(pos + 3) == t) head = pos + 4
      else scanUntilToken(t, pos + 4)
    } else if (pos < tail) {
      if (buf(pos) == t) head = pos + 1
      else scanUntilToken(t, pos + 1)
    } else scanUntilToken(t, loadMoreOrError(pos))

  private[this] def illegalTokenOperation(): Nothing =
    throw new ArrayIndexOutOfBoundsException("expected preceding call of 'nextToken()' or 'isNextToken()'")

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

  private[this] def parseYearWithToken(t: Byte, maxDigits: Int): Int = {
    var pos = head
    while (pos + 4 >= tail) {
      pos = loadMoreOrError(pos)
    }
    var d1 = buf(pos) - '0'
    val d2 = buf(pos + 1) - '0'
    val d3 = buf(pos + 2) - '0'
    val d4 = buf(pos + 3) - '0'
    val d5 = buf(pos + 4) - '0'
    if (d1 >= 0 && d1 <= 9) {
      if (d2 < 0 || d2 > 9) digitError(pos + 1)
      if (d3 < 0 || d3 > 9) digitError(pos + 2)
      if (d4 < 0 || d4 > 9) digitError(pos + 3)
      if (d5 != t - '0') tokenError(t, pos + 4)
      head = pos + 5
      d1 * 1000 + d2 * 100 + d3 * 10 + d4
    } else {
      var yearNeg = false
      if (d1 == -3) yearNeg = true // -3 == '-' - '0'
      else if (d1 != -5) decodeError("expected '-' or '+' or digit", pos) // -5 == '-' - '0'
      if (d2 < 0 || d2 > 9) digitError(pos + 1)
      if (d3 < 0 || d3 > 9) digitError(pos + 2)
      if (d4 < 0 || d4 > 9) digitError(pos + 3)
      if (d5 < 0 || d5 > 9) digitError(pos + 4)
      var year = d2 * 1000 + d3 * 100 + d4 * 10 + d5
      pos += 5
      var yearDigits = 4
      while ({
        if (pos >= tail) pos = loadMoreOrError(pos)
        d1 = buf(pos) - '0'
        d1 >= 0 && d1 <= 9 && yearDigits < maxDigits
      }) {
        year =
          if (year > 100000000) 2147483647
          else year * 10 + d1
        yearDigits += 1
        pos += 1
      }
      if (d1 != t - '0') {
        if (!yearNeg && yearDigits == 4) digitError(pos)
        else if (yearDigits == maxDigits) tokenError(t, pos)
        else tokenOrDigitError(t, pos)
      }
      head = pos + 1
      if (yearNeg) {
        if (year == 0) 2147483647
        else -year
      } else year
    }
  }

  private[this] def parseNanoWithToken(t: Byte): Int = {
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
    if (d != t - '0') {
      if (nanoDigitWeight == 0) tokenError(t, pos)
      else tokenOrDigitError(t, pos)
    }
    head = pos + 1
    nano
  }

  private[this] def parseZoneIdUntilToken(t: Byte): String = {
    val mark = this.mark
    this.mark = Math.min(mark, head)
    try {
      scanUntilToken(t, head)
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

  private[this] def readParenthesesToken(): Unit = if (!isNextToken('"', head)) tokenError('"')

  private[this] def readParenthesesByteWithColonToken(): Unit = {
    readParenthesesByte()
    readColonToken()
  }

  private[this] def readParenthesesByte(): Unit = if (nextByte(head) != '"') tokenError('"')

  private[this] def readColonToken(): Unit = if (!isNextToken(':', head)) tokenError(':')

  private[this] def parseBoolean(isToken: Boolean): Boolean =
    (if (isToken) nextToken(head)
    else nextByte(head): @switch) match {
      case 't' => parseTrue(head)
      case 'f' => parseFalse(head)
      case _ => booleanError(head - 1)
    }

  @tailrec
  private[this] def parseTrue(pos: Int): Boolean =
    if (pos + 2 < tail) {
      if (buf(pos) != 'r') booleanError(pos)
      if (buf(pos + 1) != 'u') booleanError(pos + 1)
      if (buf(pos + 2) != 'e') booleanError(pos + 2)
      head = pos + 3
      true
    } else parseTrue(loadMoreOrError(pos))

  @tailrec
  private[this] def parseFalse(pos: Int): Boolean =
    if (pos + 3 < tail) {
      if (buf(pos) != 'a') booleanError(pos)
      if (buf(pos + 1) != 'l') booleanError(pos + 1)
      if (buf(pos + 2) != 's') booleanError(pos + 2)
      if (buf(pos + 3) != 'e') booleanError(pos + 3)
      head = pos + 4
      false
    } else parseFalse(loadMoreOrError(pos))

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
      var manExp = 0
      var posExp = 0
      var isExpNeg = false
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (isZeroFirst) leadingZeroError(pos - 1)
        if (posMan < 4503599627370496L) posMan = posMan * 10 + (b - '0')
        else manExp += 1
        pos += 1
      }
      if (b == '.') {
        b = nextByte(pos + 1)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        if (posMan < 4503599627370496L) {
          posMan = posMan * 10 + (b - '0')
          manExp -= 1
        }
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMan < 4503599627370496L) {
            posMan = posMan * 10 + (b - '0')
            manExp -= 1
          }
          pos += 1
        }
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        if (b == '-' || b == '+') {
          isExpNeg = b == '-'
          b = nextByte(head)
        }
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
      val exp = manExp + toSignedInt(isExpNeg, posExp)
      if (posMan < 4503599627370496L) { // 4503599627370496L == 1L < 52, max mantissa that can be converted w/o rounding error by double mul or div
        if (exp == 0) toSignedDouble(isNeg, posMan)
        else if (exp < 0 && exp > -23) toSignedDouble(isNeg, posMan / pow10(-exp))
        else if (exp > 0 && exp < 23) toSignedDouble(isNeg, posMan * pow10(exp))
        else toDouble(pos)
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
      var posMan: Long = b - '0'
      val isZeroFirst = isToken && posMan == 0
      var manExp = 0
      var posExp = 0
      var isExpNeg = false
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (isZeroFirst) leadingZeroError(pos - 1)
        if (posMan < 4503599627370496L) posMan = posMan * 10 + (b - '0')
        else manExp += 1
        pos += 1
      }
      if (b == '.') {
        b = nextByte(pos + 1)
        pos = head
        if (b < '0' || b > '9') numberError(pos - 1)
        if (posMan < 4503599627370496L) {
          posMan = posMan * 10 + (b - '0')
          manExp -= 1
        }
        while ((pos < tail || {
          pos = loadMore(pos)
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMan < 4503599627370496L) {
            posMan = posMan * 10 + (b - '0')
            manExp -= 1
          }
          pos += 1
        }
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        if (b == '-' || b == '+') {
          isExpNeg = b == '-'
          b = nextByte(head)
        }
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
      val exp = manExp + toSignedInt(isExpNeg, posExp)
      if (posMan < 4503599627370496L) { // 4503599627370496L == 1L < 52, max mantissa that can be converted w/o rounding error by double mul or div
        if (exp == 0) toSignedFloat(isNeg, posMan)
        else if (exp < 0 && exp > -pow10.length) toSignedFloat(isNeg, (posMan / pow10(-exp)).toFloat)
        else if (exp > 0 && exp < pow10.length) toSignedFloat(isNeg, (posMan * pow10(exp)).toFloat)
        else toFloat(pos)
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
      new BigInt(BigInteger.valueOf(toSignedLong(isNeg, x)))
    } else {
      try new BigInt(new java.math.BigInteger(new String(buf, 0, startPos, pos - startPos), 10)) catch {
        case ex: NumberFormatException => decodeError("illegal number", pos - 1, ex)
      }
    }
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

  private[this] def toBigDecimal(pos: Int, mc: MathContext): BigDecimal =
    try new BigDecimal(new java.math.BigDecimal(new String(buf, 0, mark, pos - mark))) catch {
      case ex: NumberFormatException => decodeError("illegal number", pos - 1, ex)
    }

  private[this] def readNullOrNumberError[@sp A](default: A, pos: Int): A =
    if (default == null) numberError(pos - 1)
    else parseNullOrError(default, "expected number or null", pos)

  private[this] def numberError(pos: Int): Nothing = decodeError("illegal number", pos)

  private[this] def digitsLimitError(pos: Int): Nothing =
    decodeError("value exceeds limit for number of significant digits", pos)

  private[this] def scaleLimitError(pos: Int): Nothing =
    decodeError("value exceeds limit for scale", pos)

  private[this] def leadingZeroError(pos: Int): Nothing = decodeError("illegal number with leading zero", pos)

  private[this] def byteOverflowError(pos: Int): Nothing = decodeError("value is too large for byte", pos)

  private[this] def shortOverflowError(pos: Int): Nothing = decodeError("value is too large for short", pos)

  private[this] def intOverflowError(pos: Int): Nothing = decodeError("value is too large for int", pos)

  private[this] def longOverflowError(pos: Int): Nothing = decodeError("value is too large for long", pos)

  private[this] def parseDuration(): Duration = {
    var isNeg = false
    var daysAsSecs = 0L
    var hoursAsSecs = 0L
    var minutesAsSecs = 0L
    var seconds = 0L
    var nanos = 0
    var nanoDigitWeight = 100000000
    var x = 0L
    var isNegX = false
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or 'P'
          if (b == 'P') state = 2
          else if (b == '-') {
            isNeg = true
            state = 1
          } else tokensError('P', '-', pos)
        case 1 => // 'P'
          if (b == 'P') state = 2
          else tokenError('P', pos)
        case 2 => // 'T' or '-' or digit
          if (b == 'T') state = 6
          else if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 4
          } else if (b == '-') {
            isNegX = true
            state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 4
          } else digitError(pos)
        case 4 => // 'D' or digit
          if (b >= '0' && b <= '9') {
            if (x < -10675199116730L) durationError(pos)
            x = x * 10 + ('0' - b)
          } else if (b == 'D') {
            if (x < -106751991167300L) durationError(pos) // -106751991167300L == Long.MinValue / 86400
            daysAsSecs =
              (if (isNeg ^ isNegX) x
              else -x) * 86400
            x = 0
            isNegX = false
            state = 5
          } else decodeError("expected 'D' or digit", pos)
        case 5 => // 'T' or '"'
          if (b == 'T') state = 6
          else if (b == '"') state = 18
          else tokensError('T', '"', pos)
        case 6 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 8
          } else if (b == '-') {
            isNegX = true
            state = 7
          } else decodeError("expected '-' or digit", pos)
        case 7 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 8
          } else digitError(pos)
        case 8 => // 'H' or 'M' or '.' or 'S' or digit
          if (b >= '0' && b <= '9') {
            if (x < -922337203685477580L) durationError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) durationError(pos)
          } else if (b == 'H') {
            if (x < -2562047788015215L) durationError(pos) // -2562047788015215L == Long.MinValue / 3600
            hoursAsSecs =
              (if (isNeg ^ isNegX) x
              else -x) * 3600
            x = 0
            isNegX = false
            state = 9
          } else if (b == 'M') {
            if (x < -153722867280912930L) durationError(pos) // -153722867280912930L == Long.MinValue / 60
            minutesAsSecs =
              (if (isNeg ^ isNegX) x
              else -x) * 60
            x = 0
            isNegX = false
            state = 12
          } else if (b == 'S' || b == '.') {
            seconds =
              if (isNeg ^ isNegX) x
              else if (x == -9223372036854775808L) durationError(pos)
              else -x
            state =
              if (b == '.') 15
              else 17
          } else decodeError("expected 'H' or 'M' or 'S or '.' or digit", pos)
        case 9 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 11
          } else if (b == '-') {
            isNegX = true
            state = 10
          } else if (b == '"') state = 18
          else decodeError("expected '\"' or '-' or digit", pos)
        case 10 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 11
          } else digitError(pos)
        case 11 => // 'M' or '.' or 'S' or digit
          if (b >= '0' && b <= '9') {
            if (x < -922337203685477580L) durationError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) durationError(pos)
          } else if (b == 'M') {
            if (x < -153722867280912930L) durationError(pos) // -153722867280912930L == Long.MinValue / 60
            minutesAsSecs =
              (if (isNeg ^ isNegX) x
              else -x) * 60
            x = 0
            isNegX = false
            state = 12
          } else if (b == 'S' || b == '.') {
            seconds =
              if (isNeg ^ isNegX) x
              else if (x == -9223372036854775808L) durationError(pos)
              else -x
            state =
              if (b == '.') 15
              else 17
          } else decodeError("expected 'M' or 'S or '.' or digit", pos)
        case 12 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 14
          } else if (b == '-') {
            isNegX = true
            state = 13
          } else if (b == '"') state = 18
          else decodeError("expected '\"' or '-' or digit", pos)
        case 13 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 14
          } else digitError(pos)
        case 14 => // 'S' or '.' or digit
          if (b >= '0' && b <= '9') {
            if (x < -922337203685477580L) durationError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) durationError(pos)
          } else if (b == 'S' || b == '.') {
            seconds =
              if (isNeg ^ isNegX) x
              else if (x == -9223372036854775808L) durationError(pos)
              else -x
            state =
              if (b == '.') 15
              else 17
          } else decodeError("expected 'S or '.' or digit", pos)
        case 15 => // 'S' or nano digit
          if (b >= '0' && b <= '9') {
            nanos += (b - '0') * nanoDigitWeight
            nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide positive int by 10
            if (nanoDigitWeight == 0) {
              if (isNeg ^ isNegX) nanos = -nanos
              state = 16
            }
          } else if (b == 'S') {
            if (isNeg ^ isNegX) nanos = -nanos
            state = 17
          } else tokenOrDigitError('S', pos)
        case 16 => // 'S'
          if (b == 'S') state = 17
          else tokenError('S', pos)
        case 17 => // '"'
          if (b == '"') state = 18
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 18)
    head = pos
    Duration.ofSeconds(sumSeconds(sumSeconds(sumSeconds(minutesAsSecs, seconds), hoursAsSecs), daysAsSecs), nanos)
  }

  private[this] def sumSeconds(s1: Long, s2: Long): Long = {
    val s = s1 + s2
    if (((s1 ^ s) & (s2 ^ s)) < 0) durationError(head - 1)
    s
  }

  private[this] def parseInstant(): Instant = {
    val year = parseYearWithToken('-', 10)
    val month = next2Digits(head)
    nextByteOrError('-', head)
    val day = next2Digits(head)
    nextByteOrError('T', head)
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var second = 0
    var nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithToken('Z')
      else if (b != 'Z') tokensError('.', 'Z')
    } else if (b != 'Z') tokensError(':', 'Z')
    nextByteOrError('"', head)
    Instant.ofEpochSecond(epochSecond(year, month, day, hour, minute, second), nano)
  }

  private[this] def parseLocalDate(): LocalDate = {
    val year = parseYearWithToken('-', 9)
    val month = next2Digits(head)
    nextByteOrError('-', head)
    val day = next2Digits(head)
    nextByteOrError('"', head)
    toLocalDate(year, month, day)
  }

  private[this] def parseLocalDateTime(): LocalDateTime = {
    val year = parseYearWithToken('-', 9)
    val month = next2Digits(head)
    nextByteOrError('-', head)
    val day = next2Digits(head)
    nextByteOrError('T', head)
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var second = 0
    var nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithToken('"')
      else if (b != '"') tokensError('.', '"')
    } else if (b != '"') tokensError(':', '"')
    LocalDateTime.of(toLocalDate(year, month, day), toLocalTime(hour, minute, second, nano))
  }

  private[this] def parseLocalTime(): LocalTime = {
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var second = 0
    var nano = 0
    var b = nextByte(head)
    if (b == ':') {
      second = next2Digits(head)
      b = nextByte(head)
      if (b == '.') nano = parseNanoWithToken('"')
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
    val year = parseYearWithToken('-', 9)
    val month = next2Digits(head)
    nextByteOrError('-', head)
    val day = next2Digits(head)
    nextByteOrError('T', head)
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var hasSecond = false
    var second = 0
    var hasNano = false
    var nano = 0
    var nanoDigitWeight = 100000000
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
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
      if (b == '-') offsetNeg = true
      else if (b != '+') timeError(hasSecond, hasNano, nanoDigitWeight)
      offsetHour = next2Digits(head)
      b = nextByte(head)
      if (b == ':') {
        offsetMinute = next2Digits(head)
        b = nextByte(head)
        if (b == ':') {
          offsetSecond = next2Digits(head)
          nextByteOrError('"', head)
        } else if (b != '"') tokensError(':', '"')
      } else if (b != '"') tokensError(':', '"')
    } else nextByteOrError('"', head)
    OffsetDateTime.of(toLocalDate(year, month, day), toLocalTime(hour, minute, second, nano),
      toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond))
  }

  private[this] def parseOffsetTime(): OffsetTime = {
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var hasSecond = false
    var second = 0
    var hasNano = false
    var nano = 0
    var nanoDigitWeight = 100000000
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
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
      if (b == '-') offsetNeg = true
      else if (b != '+') timeError(hasSecond, hasNano, nanoDigitWeight)
      offsetHour = next2Digits(head)
      b = nextByte(head)
      if (b == ':') {
        offsetMinute = next2Digits(head)
        b = nextByte(head)
        if (b == ':') {
          offsetSecond = next2Digits(head)
          nextByteOrError('"', head)
        } else if (b != '"') tokensError(':', '"')
      } else if (b != '"') tokensError(':', '"')
    } else nextByteOrError('"', head)
    OffsetTime.of(toLocalTime(hour, minute, second, nano),
      toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond))
  }

  private[this] def parsePeriod(): Period = {
    var isNeg = false
    var years = 0
    var months = 0
    var days = 0
    var x = 0
    var isNegX = false
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or 'P'
          if (b == 'P') state = 2
          else if (b == '-') {
            isNeg = true
            state = 1
          } else tokensError('P', '-', pos)
        case 1 => // 'P'
          if (b == 'P') state = 2
          else tokenError('P', pos)
        case 2 => // '-' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 4
          } else if (b == '-') {
            isNegX = true
            state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 4
          } else digitError(pos)
        case 4 => // 'Y' or 'M' or 'W' or 'D' or digit
          if (b >= '0' && b <= '9') {
            if (x < -214748364) periodError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) periodError(pos)
          } else if (b == 'Y') {
            years =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            x = 0
            isNegX = false
            state = 5
          } else if (b == 'M') {
            months =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            x = 0
            isNegX = false
            state = 8
          } else if (b == 'W') {
            val r = 7L *
              (if (isNeg ^ isNegX) x
              else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            isNegX = false
            state = 11
          } else if (b == 'D') {
            days =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            state = 14
          } else decodeError("expected 'Y' or 'M' or 'W' or 'D' or digit", pos)
        case 5 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 7
          } else if (b == '-') {
            isNegX = true
            state = 6
          } else if (b == '"') state = 15
          else decodeError("expected '\"' or '-' or digit", pos)
        case 6 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 7
          } else digitError(pos)
        case 7 => // 'M' or 'W' or 'D' or digit
          if (b >= '0' && b <= '9') {
            if (x < -214748364) periodError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) periodError(pos)
          } else if (b == 'M') {
            months =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            x = 0
            isNegX = false
            state = 8
          } else if (b == 'W') {
            val r = 7L *
              (if (isNeg ^ isNegX) x
              else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            isNegX = false
            state = 11
          } else if (b == 'D') {
            days =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            state = 14
          } else decodeError("expected 'M' or 'W' or 'D' or digit", pos)
        case 8 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 10
          } else if (b == '-') {
            isNegX = true
            state = 9
          } else if (b == '"') state = 15
          else decodeError("expected '\"' or '-' or digit", pos)
        case 9 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 10
          } else digitError(pos)
        case 10 => // 'W' or 'D' or digit
          if (b >= '0' && b <= '9') {
            if (x < -214748364) periodError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) periodError(pos)
          } else if (b == 'W') {
            val r = 7L *
              (if (isNeg ^ isNegX) x
              else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            isNegX = false
            state = 11
          } else if (b == 'D') {
            x =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            val r = days + x.toLong
            days = r.toInt
            if (r != days) periodError(pos)
            state = 14
          } else decodeError("expected 'W' or 'D' or digit", pos)
        case 11 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 13
          } else if (b == '-') {
            isNegX = true
            state = 12
          } else if (b == '"') state = 15
          else decodeError("expected '\"' or '-' or digit", pos)
        case 12 => // digit (after '-')
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 13
          } else digitError(pos)
        case 13 => // 'D' or digit
          if (b >= '0' && b <= '9') {
            if (x < -214748364) periodError(pos)
            x = x * 10 + ('0' - b)
            if (x > 0) periodError(pos)
            state = 13
          } else if (b == 'D') {
            x =
              if (isNeg ^ isNegX) x
              else if (x == -2147483648) periodError(pos)
              else -x
            val r = days + x.toLong
            days = r.toInt
            if (r != days) periodError(pos)
            state = 14
          } else tokenOrDigitError('D', pos)
        case 14 => // '"'
          if (b == '"') state = 15
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 15)
    head = pos
    Period.of(years, months, days)
  }

  private[this] def parseYearMonth(): YearMonth = {
    val year = parseYearWithToken('-', 9)
    val month = next2Digits(head)
    nextByteOrError('"', head)
    toYearMonth(year, month)
  }

  private[this] def parseZonedDateTime(): ZonedDateTime = {
    val year = parseYearWithToken('-', 9)
    val month = next2Digits(head)
    nextByteOrError('-', head)
    val day = next2Digits(head)
    nextByteOrError('T', head)
    val hour = next2Digits(head)
    nextByteOrError(':', head)
    val minute = next2Digits(head)
    var hasSecond = false
    var second = 0
    var hasNano = false
    var nano = 0
    var nanoDigitWeight = 100000000
    var offsetNeg = false
    var hasOffsetHour = false
    var offsetHour = 0
    var offsetMinute = 0
    var hasOffsetSecond = false
    var offsetSecond = 0
    var zone: String = null
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
      if (b == '-') offsetNeg = true
      else if (b != '+') timeError(hasSecond, hasNano, nanoDigitWeight)
      hasOffsetHour = true
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
      else if (hasOffsetSecond || !hasOffsetHour) tokensError('[', '"')
      else decodeError("expected ':' or '[' or '\"'")
    }
    val ld = toLocalDate(year, month, day)
    val lt = toLocalTime(hour, minute, second, nano)
    val zo = toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
    if (zone eq null) ZonedDateTime.of(ld, lt, zo)
    else ZonedDateTime.ofLocal(LocalDateTime.of(ld, lt), toZoneId(zone), zo)
  }

  private[this] def parseZoneOffset(): ZoneOffset = {
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
    var b = nextByte(head)
    if (b != 'Z') {
      if (b == '-') offsetNeg = true
      else if (b != '+') decodeError("expected '+' or '-' or 'Z'")
      offsetHour = next2Digits(head)
      b = nextByte(head)
      if (b == ':') {
        offsetMinute = next2Digits(head)
        b = nextByte(head)
        if (b == ':') {
          offsetSecond = next2Digits(head)
          nextByteOrError('"', head)
        } else if (b != '"') tokensError(':', '"')
      } else if (b != '"') tokensError(':', '"')
    } else nextByteOrError('"', head)
    toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
  }

  private[this] def epochSecond(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = {
    if (year < -1000000000 || year > 1000000000) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) decodeError("illegal day")
    if (hour > 23) decodeError("illegal hour")
    if (minute > 59) decodeError("illegal minute")
    if (second > 59) decodeError("illegal second")
    (epochDayForYear(year) + (dayOfYearForYearMonth(year, month) + day - 719529)) * 86400 + // 719528 == days 0000 to 1970
      secondOfDay(hour, minute, second)
  }

  private[this] def toLocalDate(year: Int, month: Int, day: Int): LocalDate = {
    if (year < -999999999 || year > 999999999) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) decodeError("illegal day")
    try LocalDate.of(year, month, day) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toYear(year: Int): Year = {
    if (year < -999999999 || year > 999999999) decodeError("illegal year")
    try Year.of(year) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toYearMonth(year: Int, month: Int): YearMonth = {
    if (year < -999999999 || year > 999999999) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    try YearMonth.of(year, month) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toMonthDay(month: Int, day: Int): MonthDay = {
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(2004, month))) decodeError("illegal day")
    try MonthDay.of(month, day) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toLocalTime(hour: Int, minute: Int, second: Int, nano: Int): LocalTime = {
    if (hour > 23) decodeError("illegal hour")
    if (minute > 59) decodeError("illegal minute")
    if (second > 59) decodeError("illegal second")
    try LocalTime.of(hour, minute, second, nano) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toZoneOffset(isNeg: Boolean, offsetHour: Int, offsetMinute: Int, offsetSecond: Int): ZoneOffset = {
    if (offsetHour > 18) decodeError("illegal zone offset hour")
    if (offsetMinute > 59) decodeError("illegal zone offset minute")
    if (offsetSecond > 59) decodeError("illegal zone offset second")
    val offsetTotal = secondOfDay(offsetHour, offsetMinute, offsetSecond)
    if (offsetTotal > 64800) decodeError("illegal zone offset") // 64800 == 18 * 60 * 60
    try ZoneOffset.ofTotalSeconds(toSignedInt(isNeg, offsetTotal))  catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private[this] def toSignedInt(isNeg: Boolean, posX: Int): Int =
    if (isNeg) -posX
    else posX

  private[this] def toSignedLong(isNeg: Boolean, posX: Long): Long =
    if (isNeg) -posX
    else posX

  private[this] def toZoneId(zone: String): ZoneId =
    try ZoneId.of(zone) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
      case ex: ZoneRulesException => dateTimeZoneError(ex)
    }

  private[this] def epochDayForYear(year: Int): Long =
    if (year < 0) {
      val posYear = -year
      val century = posYear * 1374389535L >> 37 // divide positive int by 100
      -365L * posYear - (posYear >> 2) + century - (century >> 2)
    } else {
      365L * year + ((year + 3) >> 2) - ((year + 99) * 1374389535L >> 37) + // divide positive int by 100
        ((year + 399) * 1374389535L >> 39) // divide positive int by 400
    }

  private[this] def dayOfYearForYearMonth(posYear: Int, month: Int): Int =
    ((month * 1050835331877L - 1036518774222L) >> 35).toInt - // == (367 * month - 362) / 12
      (if (month <= 2) 0
      else if (isLeap(posYear)) 1
      else 2)

  private[this] def maxDayForYearMonth(year: Int, month: Int): Int =
    if (month != 2) ((month >> 3) ^ (month & 1)) + 30
    else if (isLeap(year)) 29
    else 28

  private[this] def isLeap(year: Int): Boolean = {
    val posYear =
      if (year < 0) -year
      else year
    (posYear & 3) == 0 && {
      val century = (posYear * 1374389535L >> 37).toInt // divide positive int by 100
      century * 100 != posYear || (century & 3) == 0
    }
  }

  private[this] def secondOfDay(hour: Int, month: Int, day: Int): Int = hour * 3600 + month * 60 + day

  private[this] def digitError(pos: Int): Nothing = decodeError("expected digit", pos)

  private[this] def periodError(pos: Int): Nothing = decodeError("illegal period", pos)

  private[this] def durationError(pos: Int): Nothing = decodeError("illegal duration", pos)

  private[this] def timeError(hasSecond: Boolean, hasNano: Boolean, nanoDigitWeight: Int): Nothing =
    decodeError {
      if (hasSecond) {
        if (hasNano) {
          if (nanoDigitWeight == 0) "expected '+' or '-' or 'Z'"
          else "expected '+' or '-' or 'Z' or digit"
        } else "expected '.' or '+' or '-' or 'Z'"
      } else "expected ':' or '+' or '-' or 'Z'"
    }

  private[this] def dateTimeZoneError(ex: DateTimeException): Nothing =
    decodeError("illegal date/time/zone", head - 1, ex)

  @tailrec
  private[this] def parseUUID(pos: Int): UUID =
    if (pos + 36 < tail) {
      val ns = nibbles
      val buf = this.buf
      val mostSigBits1: Long =
        (ns(buf(pos) & 255).toLong << 28) |
        (ns(buf(pos + 1) & 255) << 24) |
        (ns(buf(pos + 2) & 255) << 20) |
        (ns(buf(pos + 3) & 255) << 16) |
        (ns(buf(pos + 4) & 255) << 12) |
        (ns(buf(pos + 5) & 255) << 8) |
        (ns(buf(pos + 6) & 255) << 4) |
        ns(buf(pos + 7) & 255)
      val mostSigBits2: Int =
        (ns(buf(pos + 9) & 255) << 12) |
        (ns(buf(pos + 10) & 255) << 8) |
        (ns(buf(pos + 11) & 255) << 4) |
        ns(buf(pos + 12) & 255)
      val mostSigBits3: Int =
        (ns(buf(pos + 14) & 255) << 12) |
        (ns(buf(pos + 15) & 255) << 8) |
        (ns(buf(pos + 16) & 255) << 4) |
        ns(buf(pos + 17) & 255)
      val leastSigBits1: Int =
        (ns(buf(pos + 19) & 255) << 12) |
        (ns(buf(pos + 20) & 255) << 8) |
        (ns(buf(pos + 21) & 255) << 4) |
        ns(buf(pos + 22) & 255)
      val leastSigBits2: Long =
        (ns(buf(pos + 24) & 255).toLong << 44) |
        (ns(buf(pos + 25) & 255).toLong << 40) |
        (ns(buf(pos + 26) & 255).toLong << 36) |
        (ns(buf(pos + 27) & 255).toLong << 32) |
        (ns(buf(pos + 28) & 255).toLong << 28) |
        (ns(buf(pos + 29) & 255) << 24) |
        (ns(buf(pos + 30) & 255) << 20) |
        (ns(buf(pos + 31) & 255) << 16) |
        (ns(buf(pos + 32) & 255) << 12) |
        (ns(buf(pos + 33) & 255) << 8) |
        (ns(buf(pos + 34) & 255) << 4) |
        ns(buf(pos + 35) & 255)
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
      if (b == '"') {
        head = pos + 1
        i
      } else if (b >= ' ' && b != '\\') {
        charBuf(i) = b.toChar
        parseString(i + 1, minLim, charBuf, pos + 1)
      } else parseEncodedString(i, charBuf.length - 1, charBuf, pos)
    } else if (pos >= tail) {
      val newPos = loadMoreOrError(pos)
      parseString(i, Math.min(charBuf.length, i + tail - newPos), charBuf, newPos)
    } else parseString(i, Math.min(growCharBuf(i + 1), i + tail - pos), this.charBuf, pos)

  @tailrec
  private[this] def parseEncodedString(i: Int, lim: Int, charBuf: Array[Char], pos: Int): Int =
    if (i >= lim) parseEncodedString(i, growCharBuf(i + 2) - 1, this.charBuf, pos) // 2 is length of surrogate pair
    else {
      val remaining = tail - pos
      if (remaining > 0) {
        val b1 = buf(pos)
        if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
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
                if (ch1 < 0xD800 || ch1 > 0xDFFF) {
                  charBuf(i) = ch1
                  parseEncodedString(i + 1, lim, charBuf, pos + 6)
                } else if (remaining > 11) {
                  if (buf(pos + 6) != '\\') illegalEscapeSequenceError(pos + 6)
                  if (buf(pos + 7) != 'u') illegalEscapeSequenceError(pos + 7)
                  val ch2 = readEscapedUnicode(pos + 8, buf)
                  if (ch1 >= 0xDC00 || ch2 < 0xDC00 || ch2 > 0xDFFF) decodeError("illegal surrogate character pair", pos + 11)
                  charBuf(i) = ch1
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
        } else if ((b1 >> 5) == -2) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
          if (remaining > 1) {
            val b2 = buf(pos + 1)
            if ((b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80) malformedBytesError(b1, b2, pos)
            charBuf(i) = ((b1 << 6) ^ (b2 ^ 0xF80)).toChar // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
            parseEncodedString(i + 1, lim, charBuf, pos + 2)
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else if ((b1 >> 4) == -2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
          if (remaining > 2) {
            val b2 = buf(pos + 1)
            val b3 = buf(pos + 2)
            val ch = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
            if ((b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 ||
              (ch >= 0xD800 && ch <= 0xDFFF)) malformedBytesError(b1, b2, b3, pos)
            charBuf(i) = ch
            parseEncodedString(i + 1, lim, charBuf, pos + 3)
          } else parseEncodedString(i, lim, charBuf, loadMoreOrError(pos))
        } else if ((b1 >> 3) == -2) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
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
    }

  @tailrec
  private[this] def parseChar(pos: Int): Char = {
    val remaining = tail - pos
    if (remaining > 0) {
      val b1 = buf(pos)
      if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
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
      } else if ((b1 >> 5) == -2) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        if (remaining > 1) {
          val b2 = buf(pos + 1)
          if ((b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80) malformedBytesError(b1, b2, pos)
          head = pos + 2
          ((b1 << 6) ^ (b2 ^ 0xF80)).toChar // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 4) == -2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
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
      (ns(buf(pos) & 255) << 12) |
      (ns(buf(pos + 1) & 255) << 8) |
      (ns(buf(pos + 2) & 255) << 4) |
      ns(buf(pos + 3) & 255)
    if (x < 0) hexDigitError(pos)
    x.toChar
  }

  @tailrec
  private[this] def hexDigitError(pos: Int): Nothing = {
    if (nibbles(buf(pos) & 255) < 0) decodeError("expected hex digit", pos)
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
    val alignedAbsFrom = (start + offset) & -16
    val alignedAbsTo = (end + offset + 15) & -16
    val len = alignedAbsTo - alignedAbsFrom
    val bufOffset = alignedAbsFrom - offset
    var i = appendChars(dumpHeader, from)
    i = appendChars(dumpBorder, i)
    val ds = hexDigits
    var charBuf = this.charBuf
    var lim = charBuf.length
    var j = 0
    while (j < len) {
      val linePos = j & 15
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
          charBuf(i) = ds((b >>> 4) & 15)
          charBuf(i + 1) = ds(b & 15)
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
        charBuf(j) = ds((hd >>> shift) & 15)
        j += 1
      }
    }
    putHexInt(d.toInt, j, charBuf, ds)
    j + 8
  }

  private[this] def appendHexByte(b: Byte, i: Int, ds: Array[Char]): Int = {
    if (i + 2 >= charBuf.length) growCharBuf(i + 2)
    charBuf(i) = ds((b >>> 4) & 15)
    charBuf(i + 1) = ds(b & 15)
    i + 2
  }

  private[this] def putHexInt(d: Int, i: Int, charBuf: Array[Char], ds: Array[Char]): Unit = {
    charBuf(i) = ds(d >>> 28)
    charBuf(i + 1) = ds((d >>> 24) & 15)
    charBuf(i + 2) = ds((d >>> 20) & 15)
    charBuf(i + 3) = ds((d >>> 16) & 15)
    charBuf(i + 4) = ds((d >>> 12) & 15)
    charBuf(i + 5) = ds((d >>> 8) & 15)
    charBuf(i + 6) = ds((d >>> 4) & 15)
    charBuf(i + 7) = ds(d & 15)
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
  private final val pow10: Array[Double] =
    Array(1, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11,
      1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22,
      1e+23, 1e+24, 1e+25, 1e+26, 1e+27, 1e+28, 1e+29, 1e+30, 1e+31, 1e+32, 1e+33,
      1e+34, 1e+35, 1e+36, 1e+37, 1e+38, 1e+39, 1e+40, 1e+41, 1e+42, 1e+43, 1e+44,
      1e+45, 1e+46, 1e+47, 1e+48, 1e+49)
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
    var h = 0
    var i = 0
    while (i < len) {
      h = (h << 5) + (cs(i) - h)
      i += 1
    }
    h
  }
}