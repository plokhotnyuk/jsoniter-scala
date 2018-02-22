package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.InputStream
import java.math.BigInteger
import java.time._
import java.time.zone.ZoneRulesException
import java.util.UUID

import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader._

import scala.annotation.{switch, tailrec}
import scala.util.control.NonFatal

class JsonParseException(msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause, true, withStackTrace)

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonReader]] that contains flags for tuning of
  * parsing exceptions and preferred sizes for internal buffers that created on the reader instantiation and reused in
  * runtime for parsing of messages.
  * <br/>
  * All configuration params already initialized by recommended default values, but in some cases they should be altered
  * for performance reasons:
  * <ul>
  * <li>turn off stack traces for parsing exceptions to greatly reduce impact on performance for cases when exceptions
  * can be not exceptional, see more details here: [[https://shipilev.net/blog/2014/exceptional-performance/]]</li>
  * <li>turn off appending of hex dump to minimize length of exception message</li>
  * <li>increase preferred size of an internal char buffer to reduce allocation rate of grown and then reduced
  * buffers when lot of large strings with length greater than 2K need to be parsed</li>
  * <li>increase preferred size of an internal byte buffer for parsing from [[java.io.InputStream]] to reduce allocation
  * rate of grown and then reduced buffers when during parsing of large ADT instances (>16Kb) the discriminator field does
  * not appear in the beginning of the JSON object</li>
  * </ul>
  * @param throwParseExceptionWithStackTrace a flag that allows to turn off a stack trace for parsing exceptions
  * @param appendHexDumpToParseException a flag that allows to turn off hex dumping of affected by error part of
  *                                      an internal byte buffer
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when parsing from
  *                         [[java.io.InputStream]]
  * @param preferredCharBufSize a preferred size (in chars) of an internal char buffer for parsing of string values
  */
case class ReaderConfig(
    throwParseExceptionWithStackTrace: Boolean = true,
    appendHexDumpToParseException: Boolean = true,
    preferredBufSize: Int = 16384,
    preferredCharBufSize: Int = 2048) {
  if (preferredBufSize < 12) throw new IllegalArgumentException("`preferredBufSize` should be not less than 12")
}

final class JsonReader private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](1024),
    private[this] var head: Int = 0,
    private[this] var tail: Int = 0,
    private[this] var mark: Int = 2147483647,
    private[this] var charBuf: Array[Char] = new Array[Char](128),
    private[this] var in: InputStream = null,
    private[this] var totalRead: Long = 0,
    private[this] var config: ReaderConfig = null) {
  def requiredKeyError(reqFields: Array[String], reqBits: Array[Int]): Nothing = {
    val len = Math.min(reqFields.length, reqBits.length << 5)
    var i = 0
    var j = 0
    var reqBitBlock = 0
    while (j < len) {
      if ((j & 31) == 0) reqBitBlock = reqBits(j >> 5)
      if ((reqBitBlock & (1 << j)) != 0) {
        i = appendString(if (i == 0) "missing required field(s) \"" else "\", \"", i)
        i = appendString(reqFields(j), i)
      }
      j += 1
    }
    if (i == 0) throw new IllegalArgumentException("missing required field(s) cannot be reported for arguments: " +
      s"reqFields = ${reqFields.mkString("Array(", ", ", ")")}, reqBits = ${reqBits.mkString("Array(", ", ", ")")}")
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def unexpectedKeyError(len: Int): Nothing = {
    var i = prependString("unexpected field: \"", len)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def discriminatorValueError(discriminatorFieldName: String): Nothing = {
    var i = appendString("illegal value of discriminator field \"", 0)
    i = appendString(discriminatorFieldName, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def enumValueError(value: String): Nothing = {
    var i = appendString("illegal enum value: \"", 0)
    i = appendString(value, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def enumValueError(len: Int): Nothing = {
    var i = prependString("illegal enum value: \"", len)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  def setMark(): Unit = mark = head

  @tailrec
  def scanToKey(s: String): Unit = if (!isCharBufEqualsTo(readKeyAsCharBuf(), s)) {
    skip()
    if (isNextToken(',', head)) scanToKey(s)
    else reqFieldError(s)
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
    val x = parseMonthDay()
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
    val x = parseYear(false)
    readParenthesesByteWithColonToken()
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
    val x = parseZoneId()
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
    val x = parseDouble(isToken = false).toFloat
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsDouble(): Double = {
    readParenthesesToken()
    val x = parseDouble(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsBigInt(): BigInt = {
    readParenthesesToken()
    val x = parseBigInt(isToken = false)
    readParenthesesByteWithColonToken()
    x
  }

  def readKeyAsBigDecimal(): BigDecimal = {
    readParenthesesToken()
    val x = parseBigDecimal(isToken = false, null)
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

  def readFloat(): Float = parseDouble(isToken = true).toFloat

  def readBigInt(default: BigInt): BigInt = parseBigInt(isToken = true, default)

  def readBigDecimal(default: BigDecimal): BigDecimal = parseBigDecimal(isToken = true, default)

  def readString(default: String = null): String =
    if (isNextToken('"', head)) {
      val len = parseString()
      new String(charBuf, 0, len)
    } else readNullOrTokenError(default, '"')

  def readDuration(default: Duration = null): Duration =
    if (isNextToken('"', head)) parseDuration()
    else readNullOrTokenError(default, '"')

  def readInstant(default: Instant = null): Instant =
    if (isNextToken('"', head)) parseInstant()
    else readNullOrTokenError(default, '"')

  def readLocalDate(default: LocalDate = null): LocalDate =
    if (isNextToken('"', head)) parseLocalDate()
    else readNullOrTokenError(default, '"')

  def readLocalDateTime(default: LocalDateTime = null): LocalDateTime =
    if (isNextToken('"', head)) parseLocalDateTime()
    else readNullOrTokenError(default, '"')

  def readLocalTime(default: LocalTime = null): LocalTime =
    if (isNextToken('"', head)) parseLocalTime()
    else readNullOrTokenError(default, '"')

  def readMonthDay(default: MonthDay = null): MonthDay =
    if (isNextToken('"', head)) parseMonthDay()
    else readNullOrTokenError(default, '"')

  def readOffsetDateTime(default: OffsetDateTime = null): OffsetDateTime =
    if (isNextToken('"', head)) parseOffsetDateTime()
    else readNullOrTokenError(default, '"')

  def readOffsetTime(default: OffsetTime = null): OffsetTime =
    if (isNextToken('"', head)) parseOffsetTime()
    else readNullOrTokenError(default, '"')

  def readPeriod(default: Period = null): Period =
    if (isNextToken('"', head)) parsePeriod()
    else readNullOrTokenError(default, '"')

  def readYear(default: Year = null): Year =
    if (isNextToken('n', head)) parseNullOrError(default, "expected number or null", head)
    else {
      rollbackToken()
      parseYear(true)
    }

  def readYearMonth(default: YearMonth = null): YearMonth =
    if (isNextToken('"', head)) parseYearMonth()
    else readNullOrTokenError(default, '"')

  def readZonedDateTime(default: ZonedDateTime = null): ZonedDateTime =
    if (isNextToken('"', head)) parseZonedDateTime()
    else readNullOrTokenError(default, '"')

  def readZoneId(default: ZoneId = null): ZoneId =
    if (isNextToken('"', head)) parseZoneId()
    else readNullOrTokenError(default, '"')

  def readZoneOffset(default: ZoneOffset = null): ZoneOffset =
    if (isNextToken('"', head)) parseZoneOffset()
    else readNullOrTokenError(default, '"')

  def readUUID(default: UUID = null): UUID =
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
    val x = parseDouble(isToken = false).toFloat
    readParenthesesByte()
    x
  }

  def readStringAsBigInt(default: BigInt): BigInt =
    if (isNextToken('"', head)) {
      val x = parseBigInt(isToken = false, default)
      readParenthesesByte()
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBigDecimal(default: BigDecimal): BigDecimal =
    if (isNextToken('"', head)) {
      val x = parseBigDecimal(isToken = false, default)
      readParenthesesByte()
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsYear(default: Year): Year =
    if (isNextToken('"', head)) {
      val x = parseYear(false)
      readParenthesesByte()
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBoolean(): Boolean = {
    readParenthesesToken()
    val x = parseBoolean(isToken = false)
    readParenthesesByte()
    x
  }

  def readNullOrError[A](default: A, error: String): A =
    if (isNextToken('n', head)) parseNullOrError(default, error, head)
    else decodeError(error)

  def readNullOrTokenError[A](default: A, b: Byte): A =
    if (isCurrentToken('n', head)) parseNullOrTokenError(default, b, head)
    else tokenOrNullError(b)

  def nextToken(): Byte = nextToken(head)

  def isNextToken(b: Byte): Boolean = isNextToken(b, head)

  def isCurrentToken(b: Byte): Boolean = isCurrentToken(b, head)

  def rollbackToken(): Unit = {
    val pos = head
    if (pos == 0) throw new ArrayIndexOutOfBoundsException("expected preceding call of 'nextToken()' or 'isNextToken()'")
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
    else if (b == '{') skipNested('{', '}', 0, head)
    else if (b == '[') skipNested('[', ']', 0, head)
    else decodeError("expected value", head - 1)
  }

  def commaError(): Nothing = tokenError(',')

  def arrayStartOrNullError(): Nothing = tokenOrNullError('[')

  def arrayEndError(): Nothing = tokenError(']')

  def arrayEndOrCommaError(): Nothing = decodeError("expected ']' or ','")

  def objectStartOrNullError(): Nothing = tokenOrNullError('{')

  def objectEndOrCommaError(): Nothing = decodeError("expected '}' or ','")

  def decodeError(msg: String): Nothing = decodeError(msg, head - 1)

  private[jsoniter_scala] def read[A](codec: JsonValueCodec[A], buf: Array[Byte], from: Int, to: Int, config: ReaderConfig): A = {
    val currBuf = this.buf
    this.config = config
    this.buf = buf
    head = from
    tail = to
    mark = 2147483647
    totalRead = 0
    try codec.decode(this, codec.nullValue) // also checks that `codec` is not null before any parsing
    finally {
      this.buf = currBuf
      freeTooLongCharBuf()
    }
  }

  private[jsoniter_scala] def read[A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig): A = {
    this.config = config
    this.in = in
    head = 0
    tail = 0
    mark = 2147483647
    totalRead = 0
    try codec.decode(this, codec.nullValue)
    finally {
      this.in = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
      freeTooLongBuf()
      freeTooLongCharBuf()
    }
  }

  private[jsoniter_scala] def scanValueStream[A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig)
                                                (f: A => Boolean): Unit = {
    this.config = config
    this.in = in
    head = 0
    tail = 0
    mark = 2147483647
    totalRead = 0
    try {
      while (f(codec.decode(this, codec.nullValue)) && skipWhitespaces()) ()
    } finally {
      this.in = null  // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
      freeTooLongBuf()
      freeTooLongCharBuf()
    }
  }

  private[jsoniter_scala] def scanArray[A](codec: JsonValueCodec[A], in: InputStream, config: ReaderConfig)
                                          (f: A => Boolean): Unit = {
    this.config = config
    this.in = in
    head = 0
    tail = 0
    mark = 2147483647
    totalRead = 0
    try {
      if (isNextToken('[')) {
        if (!isNextToken(']')) {
          rollbackToken()
          var continue = true
          do {
            continue = f(codec.decode(this, codec.nullValue))
          } while (continue && isNextToken(','))
          if (continue && !isCurrentToken(']')) arrayEndOrCommaError()
        }
      } else readNullOrTokenError((), '[')
    } finally {
      this.in = null  // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
      freeTooLongBuf()
      freeTooLongCharBuf()
    }
  }

  private def skipWhitespaces(): Boolean = {
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

  private def tokenOrDigitError(b: Byte, pos: Int): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendString("' or digit", i)
    decodeError(i, pos, null)
  }

  private def tokensError(b1: Byte, b2: Byte, pos: Int): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b1.toChar, i)
    i = appendString("' or '", i)
    i = appendChar(b2.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private def tokenOrNullError(b: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendString("' or null", i)
    decodeError(i, pos, null)
  }

  private def tokenError(b: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(b.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private def reqFieldError(s: String): Nothing = {
    var i = appendString("missing required field \"", 0)
    i = appendString(s, i)
    i = appendChar('"', i)
    decodeError(i, head - 1, null)
  }

  private def decodeError(msg: String, pos: Int, cause: Throwable = null): Nothing =
    decodeError(appendString(msg, 0), pos, cause)

  private def decodeError(from: Int, pos: Int, cause: Throwable) = {
    var i = appendString(", offset: 0x", from)
    val offset = totalRead - (if (in eq null) 0 else tail)
    i = appendHex(offset + pos, i)
    if (config.appendHexDumpToParseException) {
      i = appendString(", buf:", i)
      i = appendHexDump(Math.max((pos - 32) & -16, 0), Math.min((pos + 48) & -16, tail), offset.toInt, i)
    }
    throw new JsonParseException(new String(charBuf, 0, i), cause, config.throwParseExceptionWithStackTrace)
  }

  @tailrec
  private def nextByte(pos: Int): Byte =
    if (pos < tail) {
      head = pos + 1
      buf(pos)
    } else nextByte(loadMoreOrError(pos))

  @tailrec
  private def nextToken(pos: Int): Byte =
    if (pos < tail) {
      val b = buf(pos)
      if (b == ' ' || b == '\n' || b == '\t' || b == '\r') nextToken(pos + 1)
      else {
        head = pos + 1
        b
      }
    } else nextToken(loadMoreOrError(pos))

  @tailrec
  private def isNextToken(t: Byte, pos: Int): Boolean =
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

  private def isCurrentToken(b: Byte, pos: Int): Boolean =
    if (pos == 0) throw new ArrayIndexOutOfBoundsException("expected preceding call of 'nextToken()' or 'isNextToken()'")
    else buf(pos - 1) == b

  @tailrec
  private def parseNullOrError[A](default: A, error: String, pos: Int): A =
    if (pos + 2 < tail) {
      if (buf(pos) != 'u') decodeError(error, pos)
      if (buf(pos + 1) != 'l') decodeError(error, pos + 1)
      if (buf(pos + 2) != 'l') decodeError(error, pos + 2)
      head = pos + 3
      default
    } else parseNullOrError(default, error, loadMoreOrError(pos))

  @tailrec
  private def parseNullOrTokenError[A](default: A, b: Byte, pos: Int): A =
    if (pos + 2 < tail) {
      if (buf(pos) != 'u') tokenOrNullError(b, pos)
      if (buf(pos + 1) != 'l') tokenOrNullError(b, pos + 1)
      if (buf(pos + 2) != 'l') tokenOrNullError(b, pos + 2)
      head = pos + 3
      default
    } else parseNullOrTokenError(default, b, loadMoreOrError(pos))

  @tailrec
  private def isCharBufEqualsTo(len: Int, s: String, i: Int): Boolean =
    if (i == len) true
    else if (charBuf(i) != s.charAt(i)) false
    else isCharBufEqualsTo(len, s, i + 1)

  private def appendChar(ch: Char, i: Int): Int = {
    if (i >= charBuf.length) growCharBuf(i + 1)
    charBuf(i) = ch
    i + 1
  }

  private def appendChars(cs: Array[Char], i: Int): Int = {
    val len = cs.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    System.arraycopy(cs, 0, charBuf, i, len)
    required
  }

  private def appendString(s: String, i: Int): Int = {
    val len = s.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    s.getChars(0, len, charBuf, i)
    required
  }

  private def prependString(s: String, i: Int): Int = {
    val len = s.length
    val required = i + len
    if (required > charBuf.length) growCharBuf(required)
    var j = required - 1
    while (j >= len) {
      charBuf(j) = charBuf(j - len)
      j -= 1
    }
    j = 0
    s.getChars(0, len, charBuf, 0)
    required
  }

  private def readParenthesesToken(): Unit = if (!isNextToken('"', head)) tokenError('"')

  private def readParenthesesByteWithColonToken(): Unit = {
    readParenthesesByte()
    readColonToken()
  }

  private def readParenthesesByte(): Unit = if (nextByte(head) != '"') tokenError('"')

  private def readColonToken(): Unit = if (!isNextToken(':', head)) tokenError(':')

  private def parseBoolean(isToken: Boolean): Boolean =
    (if (isToken) nextToken(head) else nextByte(head): @switch) match {
      case 't' => parseTrue(head)
      case 'f' => parseFalse(head)
      case _ => booleanError(head - 1)
    }

  @tailrec
  private def parseTrue(pos: Int): Boolean =
    if (pos + 2 < tail) {
      if (buf(pos) != 'r') booleanError(pos)
      if (buf(pos + 1) != 'u') booleanError(pos + 1)
      if (buf(pos + 2) != 'e') booleanError(pos + 2)
      head = pos + 3
      true
    } else parseTrue(loadMoreOrError(pos))

  @tailrec
  private def parseFalse(pos: Int): Boolean =
    if (pos + 3 < tail) {
      if (buf(pos) != 'a') booleanError(pos)
      if (buf(pos + 1) != 'l') booleanError(pos + 1)
      if (buf(pos + 2) != 's') booleanError(pos + 2)
      if (buf(pos + 3) != 'e') booleanError(pos + 3)
      head = pos + 4
      false
    } else parseFalse(loadMoreOrError(pos))

  private def booleanError(pos: Int): Nothing = decodeError("illegal boolean", pos)

  private def parseByte(isToken: Boolean): Byte = {
    var b = if (isToken) nextToken(head) else nextByte(head)
    val negative = b == '-'
    if (negative) b = nextByte(head)
    var pos = head
    if (b >= '0' && b <= '9') {
      var x = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (isToken && x == 0) leadingZeroError(pos - 1)
        x = x * 10 + ('0' - b)
        if (x < -128) byteOverflowError(pos)
        pos + 1
      }
      head = pos
      if (b == '.' || (b | 0x20) == 'e') numberError(pos)
      else if (negative) x.toByte
      else if (x == -128) byteOverflowError(pos - 1)
      else (-x).toByte
    } else numberError(pos - 1)
  }

  private def parseShort(isToken: Boolean): Short = {
    var b = if (isToken) nextToken(head) else nextByte(head)
    val negative = b == '-'
    if (negative) b = nextByte(head)
    var pos = head
    if (b >= '0' && b <= '9') {
      var x = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (isToken && x == 0) leadingZeroError(pos - 1)
        x = x * 10 + ('0' - b)
        if (x < -32768) shortOverflowError(pos)
        pos + 1
      }
      head = pos
      if (b == '.' || (b | 0x20) == 'e') numberError(pos)
      else if (negative) x.toShort
      else if (x == -32768) shortOverflowError(pos - 1)
      else (-x).toShort
    } else numberError(pos - 1)
  }

  private def parseInt(isToken: Boolean): Int = {
    var b = if (isToken) nextToken(head) else nextByte(head)
    val negative = b == '-'
    if (negative) b = nextByte(head)
    var pos = head
    if (b >= '0' && b <= '9') {
      var x = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (isToken && x == 0) leadingZeroError(pos - 1)
        if (x < -214748364) intOverflowError(pos)
        x = x * 10 + ('0' - b)
        if (x > 0) intOverflowError(pos)
        pos + 1
      }
      head = pos
      if (b == '.' || (b | 0x20) == 'e') numberError(pos)
      else if (negative) x
      else if (x == -2147483648) intOverflowError(pos - 1)
      else -x
    } else numberError(pos - 1)
  }

  private def parseLong(isToken: Boolean): Long = {
    var b = if (isToken) nextToken(head) else nextByte(head)
    val negative = b == '-'
    if (negative) b = nextByte(head)
    var pos = head
    if (b >= '0' && b <= '9') {
      var x: Long = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (isToken && x == 0) leadingZeroError(pos - 1)
        if (x < -922337203685477580L) longOverflowError(pos)
        x = x * 10 + ('0' - b)
        if (x > 0) longOverflowError(pos)
        pos + 1
      }
      head = pos
      if (b == '.' || (b | 0x20) == 'e') numberError(pos)
      else if (negative) x
      else if (x == -9223372036854775808L) longOverflowError(pos - 1)
      else -x
    } else numberError(pos - 1)
  }

  private def parseDouble(isToken: Boolean): Double = {
    var posMan = 0L
    var manExp = 0
    var posExp = 0
    var isNeg = false
    var isExpNeg = false
    var isZeroFirst = false
    var state = 0
    var pos = head
    val mark = this.mark
    this.mark = Math.min(mark, pos)
    try {
      while (pos < tail || {
        pos = loadMore(pos)
        pos < tail
      }) {
        val b = buf(pos)
        (state: @switch) match {
          case 0 => // start
            if (b >= '0' && b <= '9') {
              posMan = b - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else if (b == '-') {
              isNeg = true
              state = 2
            } else if (b == ' ' || b == '\n' || b == '\t' || b == '\r') {
              if (!isToken) numberError(pos)
              state = 1
            } else numberError(pos)
          case 1 => // whitespaces
            if (b == ' ' || b == '\n' || b == '\t' || b == '\r') () // for fast skipping of whitespaces
            else if (b >= '0' && b <= '9') {
              posMan = b - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else if (b == '-') {
              isNeg = true
              state = 2
            } else numberError(pos)
          case 2 => // signum
            if (b >= '0' && b <= '9') {
              posMan = b - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else numberError(pos)
          case 3 => // first int digit
            if (b >= '0' && b <= '9') {
              posMan = posMan * 10 + (b - '0')
              if (isToken && isZeroFirst) leadingZeroError(pos - 1)
              state = 4
            } else if (b == '.') state = 5
            else if ((b | 0x20) == 'e') state = 7
            else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp)
            }
          case 4 => // int digit
            if (b >= '0' && b <= '9') {
              if (posMan < 4503599627370496L) posMan = posMan * 10 + (b - '0')
              else manExp += 1
            } else if (b == '.') state = 5
            else if ((b | 0x20) == 'e') state = 7
            else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp)
            }
          case 5 => // dot
            if (b >= '0' && b <= '9') {
              if (posMan < 4503599627370496L) {
                posMan = posMan * 10 + (b - '0')
                manExp -= 1
              }
              state = 6
            } else numberError(pos)
          case 6 => // frac digit
            if (b >= '0' && b <= '9') {
              if (posMan < 4503599627370496L) {
                posMan = posMan * 10 + (b - '0')
                manExp -= 1
              }
            } else if ((b | 0x20) == 'e') state = 7
            else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp)
            }
          case 7 => // e char
            if (b >= '0' && b <= '9') {
              posExp = b - '0'
              state = 9
            } else if (b == '-') {
              isExpNeg = true
              state = 8
            } else if (b == '+') state = 8
            else numberError(pos)
          case 8 => // exp. sign
            if (b >= '0' && b <= '9') {
              posExp = b - '0'
              state = 9
            } else numberError(pos)
          case 9 => // exp. digit
            if (b >= '0' && b <= '9') {
              posExp = posExp * 10 + (b - '0')
              if (Math.abs(toExp(manExp, isExpNeg, posExp)) > 350) state = 10
            } else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp)
            }
          case 10 => // exp. digit overflow
            if (b >= '0' && b <= '9') ()
            else {
              head = pos
              return toExpOverflowDouble(isNeg, posMan, manExp, isExpNeg, posExp)
            }
        }
        pos += 1
      }
      head = pos
      if (state == 3 || state == 4 || state == 6 || state == 9) toDouble(isNeg, posMan, manExp, isExpNeg, posExp)
      else if (state == 10) toExpOverflowDouble(isNeg, posMan, manExp, isExpNeg, posExp)
      else numberError(pos)
    } finally this.mark = mark
  }

  private def toDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int): Double =
    if (posMan < 4503599627370496L) { // 4503599627370496L == 1L < 52, max mantissa that can be converted w/o rounding error by double mul or div
      val man = if (isNeg) -posMan else posMan
      val exp = toExp(manExp, isExpNeg, posExp)
      if (exp == 0) man
      else if (exp < 0 && exp > -22) man / pow10(-exp) // 22 == pow10.length
      else if (exp > 0 && exp < 22) man * pow10(exp)
      else toDouble
    } else toDouble

  private def toDouble: Double = java.lang.Double.parseDouble(new String(buf, 0, mark, head - mark))

  private def toExpOverflowDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int): Double =
    if (toExp(manExp, isExpNeg, posExp) > 0 && posMan != 0) {
      if (isNeg) Double.NegativeInfinity else Double.PositiveInfinity
    } else {
      if (isNeg) -0.0 else 0.0
    }

  private def toExp(manExp: Int, isExpNeg: Boolean, exp: Int): Int = manExp + (if (isExpNeg) -exp else exp)

  private def parseBigInt(isToken: Boolean, default: BigInt = null): BigInt = {
    var b = if (isToken) nextToken(head) else nextByte(head)
    if (b == 'n') {
      if (isToken) parseNullOrError(default, "expected number value or null", head)
      else numberError(head)
    } else {
      val mark = this.mark
      this.mark = Math.min(mark, head - 1)
      try {
        val negative = b == '-'
        if (negative) b = nextByte(head)
        var pos = head
        if (b >= '0' && b <= '9') {
          val isZeroFirst = b == '0'
          while ((pos < tail || {
            pos = loadMore(pos)
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) pos = {
            if (isToken && isZeroFirst) leadingZeroError(pos - 1)
            pos + 1
          }
          head = pos
          if (b == '.' || (b | 0x20) == 'e') numberError(pos)
          toBigInt(negative, pos)
        } else numberError(pos - 1)
      } finally this.mark = mark
    }
  }

  private def toBigInt(negative: Boolean, pos: Int): BigInt = {
    var signPos = this.mark
    var numPos = signPos + (if (negative) 1 else 0)
    if (pos < 19 + numPos) { // 19 == Long.MaxValue.toString.length
      var x = 0L
      while (numPos < pos) {
        x = x * 10 + (buf(numPos) - '0')
        numPos += 1
      }
      new BigInt(BigInteger.valueOf(if (negative) -x else x))
    } else {
      val required = pos - signPos
      var charBuf = this.charBuf
      if (required > charBuf.length) {
        growCharBuf(required)
        charBuf = this.charBuf
      }
      var i = 0
      while (signPos < pos) signPos = {
        charBuf(i) = buf(signPos).toChar
        i += 1
        signPos + 1
      }
      new BigInt(new java.math.BigDecimal(charBuf, 0, i).toBigIntegerExact)
    }
  }

  private def parseBigDecimal(isToken: Boolean, default: BigDecimal): BigDecimal = {
    var isZeroFirst = false
    var charBuf = this.charBuf
    var lim = charBuf.length
    var i = 0
    var state = 0
    var pos = head
    while (pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) {
      if (i >= lim) {
        lim = growCharBuf(i + 1)
        charBuf = this.charBuf
      }
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // start
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            isZeroFirst = b == '0'
            state = 3
          } else if (b == '-') {
            charBuf(i) = b.toChar
            i += 1
            state = 2
          } else if (b == 'n' && isToken) state = 10
          else if (b == ' ' || b == '\n' || b == '\t' || b == '\r') {
            if (!isToken) numberError(pos)
            state = 1
          } else numberError(pos)
        case 1 => // whitespaces
          if (b == ' ' || b == '\n' || b == '\t' || b == '\r') () // for fast skipping of whitespaces
          else if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            isZeroFirst = b == '0'
            state = 3
          } else if (b == '-') {
            charBuf(i) = b.toChar
            i += 1
            state = 2
          } else if (b == 'n' && isToken) state = 10
          else numberError(pos)
        case 2 => // signum
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            isZeroFirst = b == '0'
            state = 3
          } else numberError(pos)
        case 3 => // first int digit
          if (b >= '0' && b <= '9') {
            if (isToken && isZeroFirst) leadingZeroError(pos - 1)
            charBuf(i) = b.toChar
            i += 1
            state = 4
          } else if (b == '.') {
            charBuf(i) = b.toChar
            i += 1
            state = 5
          } else if ((b | 0x20) == 'e') {
            charBuf(i) = b.toChar
            i += 1
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 4 => // int digit
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
          } else if (b == '.') {
            charBuf(i) = b.toChar
            i += 1
            state = 5
          } else if ((b | 0x20) == 'e') {
            charBuf(i) = b.toChar
            i += 1
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 5 => // dot
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            state = 6
          } else numberError(pos)
        case 6 => // frac digit
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
          } else if ((b | 0x20) == 'e') {
            charBuf(i) = b.toChar
            i += 1
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 7 => // e char
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            state = 9
          } else if (b == '-' || b == '+') {
            charBuf(i) = b.toChar
            i += 1
            state = 8
          } else numberError(pos)
        case 8 => // exp. sign
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
            state = 9
          } else numberError(pos)
        case 9 => // exp. digit
          if (b >= '0' && b <= '9') {
            charBuf(i) = b.toChar
            i += 1
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 10 => // n'u'll
          if (b == 'u') state = 11
          else numberError(pos)
        case 11 => // nu'l'l
          if (b == 'l') state = 12
          else numberError(pos)
        case 12 => // nul'l'
          if (b == 'l') state = 13
          else numberError(pos)
        case 13 => // null
          head = pos
          return default
      }
      pos += 1
    }
    head = pos
    if (state == 3 || state == 4 || state == 6 || state == 9) toBigDecimal(i)
    else if (state == 13) default
    else numberError(pos)
  }

  private def toBigDecimal(len: Int): BigDecimal =
    try new BigDecimal(new java.math.BigDecimal(charBuf, 0, len)) catch {
      case ex: NumberFormatException => decodeError("illegal number", head - 1, ex)
    }

  private def numberError(pos: Int): Nothing = decodeError("illegal number", pos)

  private def leadingZeroError(pos: Int): Nothing = decodeError("illegal number with leading zero", pos)

  private def byteOverflowError(pos: Int): Nothing = decodeError("value is too large for byte", pos)

  private def shortOverflowError(pos: Int): Nothing = decodeError("value is too large for short", pos)

  private def intOverflowError(pos: Int): Nothing = decodeError("value is too large for int", pos)

  private def longOverflowError(pos: Int): Nothing = decodeError("value is too large for long", pos)

  private def parseDuration(): Duration = {
    var neg = false
    var daysAsSecs = 0L
    var hoursAsSecs = 0L
    var minutesAsSecs = 0L
    var seconds = 0L
    var nanos = 0
    var nanoDigits = 0
    var x = 0L
    var xNeg = false
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or 'P'
          if (b == 'P') state = 2
          else if (b == '-') {
            neg = true
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
            xNeg = true
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
            if (x > 0) durationError(pos)
          } else if (b == 'D') {
            if (x < -106751991167300L) durationError(pos) // -106751991167300L == Long.MinValue / 86400
            daysAsSecs = (if (neg ^ xNeg) x else -x) * 86400
            x = 0
            xNeg = false
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
            xNeg = true
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
            hoursAsSecs = (if (neg ^ xNeg) x else -x) * 3600
            x = 0
            xNeg = false
            state = 9
          } else if (b == 'M') {
            if (x < -153722867280912930L) durationError(pos) // -153722867280912930L == Long.MinValue / 60
            minutesAsSecs = (if (neg ^ xNeg) x else -x) * 60
            x = 0
            xNeg = false
            state = 12
          } else if (b == 'S' || b == '.') {
            seconds = if (neg ^ xNeg) x else if (x == -9223372036854775808L) durationError(pos) else -x
            state = if (b == '.') 15 else 17
          } else decodeError("expected 'H' or 'M' or 'S or '.' or digit", pos)
        case 9 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 11
          } else if (b == '-') {
            xNeg = true
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
            minutesAsSecs = (if (neg ^ xNeg) x else -x) * 60
            x = 0
            xNeg = false
            state = 12
          } else if (b == 'S' || b == '.') {
            seconds = if (neg ^ xNeg) x else if (x == -9223372036854775808L) durationError(pos) else -x
            state = if (b == '.') 15 else 17
          } else decodeError("expected 'M' or 'S or '.' or digit", pos)
        case 12 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 14
          } else if (b == '-') {
            xNeg = true
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
            seconds = if (neg ^ xNeg) x else if (x == -9223372036854775808L) durationError(pos) else -x
            state = if (b == '.') 15 else 17
          } else decodeError("expected 'S or '.' or digit", pos)
        case 15 => // 'S' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nanos += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) {
              if (xNeg) nanos = -nanos
              state = 16
            }
          } else if (b == 'S') {
            if (xNeg) nanos = -nanos
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
    val minutesWithSecs = minutesAsSecs + seconds
    if (((minutesAsSecs ^ minutesWithSecs) & (seconds ^ minutesWithSecs)) < 0) durationError(pos)
    val hoursWithMinsWithSecs = hoursAsSecs + minutesWithSecs
    if (((hoursAsSecs ^ hoursWithMinsWithSecs) & (minutesWithSecs ^ hoursWithMinsWithSecs)) < 0) durationError(pos)
    val totalSeconds = daysAsSecs + hoursWithMinsWithSecs
    if (((daysAsSecs ^ totalSeconds) & (hoursWithMinsWithSecs ^ totalSeconds)) < 0) durationError(pos)
    Duration.ofSeconds(totalSeconds, nanos)
  }

  private def parseInstant(): Instant = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digit
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 10) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '-'
          if (b == '-') state = 7
          else tokenError('-', pos)
        case 7 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // 'T'
          if (b == 'T') state = 10
          else tokenError('T', pos)
        case 10 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 11
          } else digitError(pos)
        case 11 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 12
          } else digitError(pos)
        case 12 => // ':'
          if (b == ':') state = 13
          else tokenError(':', pos)
        case 13 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 14
          } else digitError(pos)
        case 14 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 15
          } else digitError(pos)
        case 15 => // ':'
          if (b == ':') state = 16
          else tokenError(':', pos)
        case 16 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 17
          } else digitError(pos)
        case 17 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 18
          } else digitError(pos)
        case 18 => // 'Z' or '.'
          if (b == '.') state = 19
          else if (b == 'Z') state = 21
          else tokensError('Z', '.', pos)
        case 19 => // 'Z' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 20
          } else if (b == 'Z') state = 21
          else tokenOrDigitError('Z', pos)
        case 20 => // 'Z'
          if (b == 'Z') state = 21
          else tokenError('Z', pos)
        case 21 => // '"'
          if (b == '"') state = 22
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 22)
    head = pos
    Instant.ofEpochSecond(epochSecond(yearNeg, year, month, day, hour, minute, second), nano)
  }

  private def parseLocalDate(): LocalDate = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var day = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digit
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 9) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '-'
          if (b == '-') state = 7
          else tokenError('-', pos)
        case 7 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // '"'
          if (b == '"') state = 10
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 10)
    head = pos
    localDate(yearNeg, year, month, day)
  }

  private def parseLocalDateTime(): LocalDateTime = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digits
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 9) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '-'
          if (b == '-') state = 7
          else tokenError('-', pos)
        case 7 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // 'T'
          if (b == 'T') state = 10
          else tokenError('T', pos)
        case 10 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 11
          } else digitError(pos)
        case 11 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 12
          } else digitError(pos)
        case 12 => // ':'
          if (b == ':') state = 13
          else tokenError(':', pos)
        case 13 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 14
          } else digitError(pos)
        case 14 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 15
          } else digitError(pos)
        case 15 => // ':' or '"'
          if (b == ':') state = 16
          else if (b == '"') state = 21
          else tokensError(':', '"', pos)
        case 16 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 17
          } else digitError(pos)
        case 17 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 18
          } else digitError(pos)
        case 18 => // '"' or '.'
          if (b == '.') state = 19
          else if (b == '"') state = 21
          else tokensError('.', '"', pos)
        case 19 => // '"' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 20
          } else if (b == '"') state = 21
          else tokenOrDigitError('"', pos)
        case 20 => // '"'
          if (b == '"') state = 21
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 21)
    head = pos
    LocalDateTime.of(localDate(yearNeg, year, month, day), localTime(hour, minute, second, nano))
  }

  private def parseLocalTime(): LocalTime = {
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 1
          } else digitError(pos)
        case 1 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 2
          } else digitError(pos)
        case 2 => // ':'
          if (b == ':') state = 3
          else tokenError(':', pos)
        case 3 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 4
          } else digitError(pos)
        case 4 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 5
          } else digitError(pos)
        case 5 => // ':' or '"'
          if (b == ':') state = 6
          else if (b == '"') state = 11
          else tokensError(':', '"', pos)
        case 6 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 7
          } else digitError(pos)
        case 7 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 8
          } else digitError(pos)
        case 8 => // '"' or '.'
          if (b == '.') state = 9
          else if (b == '"') state = 11
          else tokensError('.', '"', pos)
        case 9 => // '"' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 10
          } else if (b == '"') state = 11
          else tokenOrDigitError('"', pos)
        case 10 => // '"'
          if (b == '"') state = 11
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 11)
    head = pos
    localTime(hour, minute, second, nano)
  }

  private def parseMonthDay(): MonthDay = {
    var month = 0
    var day = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-'
          if (b == '-') state = 1
          else tokenError('-', pos)
        case 1 => // '-'
          if (b == '-') state = 2
          else tokenError('-', pos)
        case 2 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 3
          } else digitError(pos)
        case 3 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 4
          } else digitError(pos)
        case 4 => // '-'
          if (b == '-') state = 5
          else tokenError('-', pos)
        case 5 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 6
          } else digitError(pos)
        case 6 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 7
          } else digitError(pos)
        case 7 => // '"'
          if (b == '"') state = 8
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 8)
    head = pos
    monthDay(month, day)
  }

  private def parseOffsetDateTime(): OffsetDateTime = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digit
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 9) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '-'
          if (b == '-') state = 7
          else tokenError('-', pos)
        case 7 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // 'T'
          if (b == 'T') state = 10
          else tokenError('T', pos)
        case 10 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 11
          } else digitError(pos)
        case 11 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 12
          } else digitError(pos)
        case 12 => // ':'
          if (b == ':') state = 13
          else tokenError(':', pos)
        case 13 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 14
          } else digitError(pos)
        case 14 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 15
          } else digitError(pos)
        case 15 => // ':' or '+' or '-' or 'Z'
          if (b == ':') state = 16
          else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected ':' or '+' or '-' or 'Z'", pos)
        case 16 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 17
          } else digitError(pos)
        case 17 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 18
          } else digitError(pos)
        case 18 => // 'Z' or '.' or '-' or '+'
          if (b == '.') state = 19
          else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '.' or '+' or '-' or 'Z'", pos)
        case 19 => // 'Z' or '-' or '+' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 20
          } else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '+' or '-' or 'Z' or digit", pos)
        case 20 => // 'Z' or '-' or '+'
          if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '+' or '-' or 'Z'", pos)
        case 21 => // offset hour (1st digit)
          if (b >= '0' && b <= '9') {
            offsetHour = b - '0'
            state = 22
          } else digitError(pos)
        case 22 => // offset hour (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetHour = offsetHour * 10 + (b - '0')
            state = 23
          } else digitError(pos)
        case 23 => // ':'
          if (b == ':') state = 24
          else if (b == '"') state = 30
          else tokensError(':', '"', pos)
        case 24 => // offset minute (1st digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = b - '0'
            state = 25
          } else digitError(pos)
        case 25 => // offset minute (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = offsetMinute * 10 + (b - '0')
            state = 26
          } else digitError(pos)
        case 26 => // ':' or '"'
          if (b == ':') state = 27
          else if (b == '"') state = 30
          else tokensError(':', '"', pos)
        case 27 => // offset second (1st digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = b - '0'
            state = 28
          } else digitError(pos)
        case 28 => // offset second (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = offsetSecond * 10 + (b - '0')
            state = 29
          } else digitError(pos)
        case 29 => // '"'
          if (b == '"') state = 30
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 30)
    head = pos
    OffsetDateTime.of(localDate(yearNeg, year, month, day), localTime(hour, minute, second, nano),
      zoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond))
  }

  private def parseOffsetTime(): OffsetTime = {
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 1
          } else digitError(pos)
        case 1 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 2
          } else digitError(pos)
        case 2 => // ':'
          if (b == ':') state = 3
          else tokenError(':', pos)
        case 3 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 4
          } else digitError(pos)
        case 4 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 5
          } else digitError(pos)
        case 5 => // ':' or '+' or '-' or 'Z'
          if (b == ':') state = 6
          else if (b == '+') state = 11
          else if (b == '-') {
            offsetNeg = true
            state = 11
          } else if (b == 'Z') state = 19
          else decodeError("expected ':' or '+' or '-' or 'Z'", pos)
        case 6 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 7
          } else digitError(pos)
        case 7 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 8
          } else digitError(pos)
        case 8 => // 'Z' or '.' or '-' or '+'
          if (b == '.') state = 9
          else if (b == '+') state = 11
          else if (b == '-') {
            offsetNeg = true
            state = 11
          } else if (b == 'Z') state = 19
          else decodeError("expected '.' or '+' or '-' or 'Z'", pos)
        case 9 => // 'Z' or '-' or '+' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 10
          } else if (b == '+') state = 11
          else if (b == '-') {
            offsetNeg = true
            state = 11
          } else if (b == 'Z') state = 19
          else decodeError("expected '+' or '-' or 'Z' or digit", pos)
        case 10 => // 'Z' or '-' or '+'
          if (b == '+') state = 11
          else if (b == '-') {
            offsetNeg = true
            state = 11
          } else if (b == 'Z') state = 19
          else decodeError("expected '+' or '-' or 'Z'", pos)
        case 11 => // offset hour (1st digit)
          if (b >= '0' && b <= '9') {
            offsetHour = b - '0'
            state = 12
          } else digitError(pos)
        case 12 => // offset hour (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetHour = offsetHour * 10 + (b - '0')
            state = 13
          } else digitError(pos)
        case 13 => // ':'
          if (b == ':') state = 14
          else if (b == '"') state = 20
          else tokensError(':', '"', pos)
        case 14 => // offset minute (1st digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = b - '0'
            state = 15
          } else digitError(pos)
        case 15 => // offset minute (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = offsetMinute * 10 + (b - '0')
            state = 16
          } else digitError(pos)
        case 16 => // ':' or '"'
          if (b == ':') state = 17
          else if (b == '"') state = 20
          else tokensError(':', '"', pos)
        case 17 => // offset second (1st digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = b - '0'
            state = 18
          } else digitError(pos)
        case 18 => // offset second (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = offsetSecond * 10 + (b - '0')
            state = 19
          } else digitError(pos)
        case 19 => // '"'
          if (b == '"') state = 20
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 20)
    head = pos
    OffsetTime.of(localTime(hour, minute, second, nano),
      zoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond))
  }

  private def parsePeriod(): Period = {
    var neg = false
    var years = 0
    var months = 0
    var days = 0
    var x = 0
    var xNeg = false
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or 'P'
          if (b == 'P') state = 2
          else if (b == '-') {
            neg = true
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
            xNeg = true
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
            years = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            x = 0
            xNeg = false
            state = 5
          } else if (b == 'M') {
            months = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            x = 0
            xNeg = false
            state = 8
          } else if (b == 'W') {
            val r = 7L * (if (neg ^ xNeg) x else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            xNeg = false
            state = 11
          } else if (b == 'D') {
            days = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            state = 14
          } else decodeError("expected 'Y' or 'M' or 'W' or 'D' or digit", pos)
        case 5 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 7
          } else if (b == '-') {
            xNeg = true
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
            months = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            x = 0
            xNeg = false
            state = 8
          } else if (b == 'W') {
            val r = 7L * (if (neg ^ xNeg) x else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            xNeg = false
            state = 11
          } else if (b == 'D') {
            days = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            state = 14
          } else decodeError("expected 'M' or 'W' or 'D' or digit", pos)
        case 8 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 10
          } else if (b == '-') {
            xNeg = true
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
            val r = 7L * (if (neg ^ xNeg) x else -x)
            days = r.toInt
            if (r != days) periodError(pos)
            x = 0
            xNeg = false
            state = 11
          } else if (b == 'D') {
            x = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            val r = days + x
            if (((x ^ r) & (days ^ r)) < 0) periodError(pos)
            days = r
            state = 14
          } else decodeError("expected 'W' or 'D' or digit", pos)
        case 11 => // '-' or '"' or digit
          if (b >= '0' && b <= '9') {
            x = '0' - b
            state = 13
          } else if (b == '-') {
            xNeg = true
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
            x = if (neg ^ xNeg) x else if (x == -2147483648) periodError(pos) else -x
            val r = days + x
            if (((x ^ r) & (days ^ r)) < 0) periodError(pos)
            days = r
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

  private def parseYear(isToken: Boolean) = {
    val year = parseInt(isToken)
    if (year < -999999999 || year > 999999999) decodeError("illegal year")
    try Year.of(year) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def parseYearMonth(): YearMonth = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digit
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 9) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '"'
          if (b == '"') state = 7
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 7)
    head = pos
    yearMonth(yearNeg, year, month)
  }

  private def parseZonedDateTime(): ZonedDateTime = {
    var year = 0
    var yearNeg = false
    var yearDigits = 0
    var yearMinDigits = 4
    var month = 0
    var day = 0
    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var nanoDigits = 0
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
    var zone: String = null
    var state = 0
    var pos = head
    var i = 0
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // '-' or '+' or year digit
          if (b >= '0' && b <= '9') {
            year = b - '0'
            yearDigits = 1
            state = 1
          } else if (b == '-') {
            yearNeg = true
            state = 1
          } else if (b == '+') {
            yearMinDigits = 5
            state = 1
          } else decodeError("expected '-' or '+' or digit", pos)
        case 1 => // year digit
          if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == yearMinDigits) state = 2
          } else digitError(pos)
        case 2 => // '-' or year digit
          if (b == '-') state = 4
          else if (b >= '0' && b <= '9') {
            year = year * 10 + (b - '0')
            yearDigits += 1
            if (yearDigits == 9) state = 3
          } else tokenOrDigitError('-', pos)
        case 3 => // '-'
          if (b == '-') state = 4
          else tokenError('-', pos)
        case 4 => // month (1st digit)
          if (b >= '0' && b <= '9') {
            month = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // month (2nd digit)
          if (b >= '0' && b <= '9') {
            month = month * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // '-'
          if (b == '-') state = 7
          else tokenError('-', pos)
        case 7 => // day (1st digit)
          if (b >= '0' && b <= '9') {
            day = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // day (2nd digit)
          if (b >= '0' && b <= '9') {
            day = day * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // 'T'
          if (b == 'T') state = 10
          else tokenError('T', pos)
        case 10 => // hour (1st digit)
          if (b >= '0' && b <= '9') {
            hour = b - '0'
            state = 11
          } else digitError(pos)
        case 11 => // hour (2nd digit)
          if (b >= '0' && b <= '9') {
            hour = hour * 10 + (b - '0')
            state = 12
          } else digitError(pos)
        case 12 => // ':'
          if (b == ':') state = 13
          else tokenError(':', pos)
        case 13 => // minute (1st digit)
          if (b >= '0' && b <= '9') {
            minute = b - '0'
            state = 14
          } else digitError(pos)
        case 14 => // minute (2nd digit)
          if (b >= '0' && b <= '9') {
            minute = minute * 10 + (b - '0')
            state = 15
          } else digitError(pos)
        case 15 => // ':' or '+' or '-' or 'Z'
          if (b == ':') state = 16
          else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected ':' or '+' or '-' or 'Z'", pos)
        case 16 => // second (1st digit)
          if (b >= '0' && b <= '9') {
            second = b - '0'
            state = 17
          } else digitError(pos)
        case 17 => // second (2nd digit)
          if (b >= '0' && b <= '9') {
            second = second * 10 + (b - '0')
            state = 18
          } else digitError(pos)
        case 18 => // 'Z' or '.' or '-' or '+'
          if (b == '.') state = 19
          else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '.' or '+' or '-' or 'Z'", pos)
        case 19 => // 'Z' or '-' or '+' or nano digit
          if (b >= '0' && b <= '9') {
            nanoDigits += 1
            nano += nanoMultiplier(nanoDigits) * (b - '0')
            if (nanoDigits == 9) state = 20
          } else if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '+' or '-' or 'Z' or digit", pos)
        case 20 => // 'Z' or '-' or '+'
          if (b == '+') state = 21
          else if (b == '-') {
            offsetNeg = true
            state = 21
          } else if (b == 'Z') state = 29
          else decodeError("expected '+' or '-' or 'Z'", pos)
        case 21 => // offset hour (1st digit)
          if (b >= '0' && b <= '9') {
            offsetHour = b - '0'
            state = 22
          } else digitError(pos)
        case 22 => // offset hour (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetHour = offsetHour * 10 + (b - '0')
            state = 23
          } else digitError(pos)
        case 23 => // ':' or '[' or '"'
          if (b == ':') state = 24
          else if (b == '[') state = 30
          else if (b == '"') state = 32
          else decodeError("expected ':' or '[' or '\"'", pos)
        case 24 => // offset minute (1st digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = b - '0'
            state = 25
          } else digitError(pos)
        case 25 => // offset minute (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = offsetMinute * 10 + (b - '0')
            state = 26
          } else digitError(pos)
        case 26 => // ':' or '['
          if (b == ':') state = 27
          else if (b == '[') state = 30
          else if (b == '"') state = 32
          else decodeError("expected ':' or '[' or '\"'", pos)
        case 27 => // offset second (1st digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = b - '0'
            state = 28
          } else digitError(pos)
        case 28 => // offset second (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = offsetSecond * 10 + (b - '0')
            state = 29
          } else digitError(pos)
        case 29 => // '[' or '"'
          if (b == '[') state = 30
          else if (b == '"') state = 32
          else tokenError('[', pos)
        case 30 => // zone id
          if (b == ']') {
            zone = new String(charBuf, 0, i)
            state = 31
          } else i = appendChar(b.toChar, i)
        case 31 => // '"'
          if (b == '"') state = 32
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 32)
    head = pos
    val ld = localDate(yearNeg, year, month, day)
    val lt = localTime(hour, minute, second, nano)
    val zo = zoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
    if (zone == null) ZonedDateTime.of(ld, lt, zo)
    else ZonedDateTime.ofLocal(LocalDateTime.of(ld, lt), zoneId(zone), zo)
  }

  private def parseZoneId(): ZoneId = {
    val len = parseString()
    zoneId(new String(charBuf, 0, len))
  }

  private def parseZoneOffset(): ZoneOffset = {
    var offsetNeg = false
    var offsetHour = 0
    var offsetMinute = 0
    var offsetSecond = 0
    var state = 0
    var pos = head
    do {
      if (pos >= tail) pos = loadMoreOrError(pos)
      val b = buf(pos)
      (state: @switch) match {
        case 0 => // 'Z' or '-' or '+'
          if (b == '+') state = 1
          else if (b == '-') {
            offsetNeg = true
            state = 1
          } else if (b == 'Z') state = 9
          else decodeError("expected '+' or '-' or 'Z'", pos)
        case 1 => // offset hour (1st digit)
          if (b >= '0' && b <= '9') {
            offsetHour = b - '0'
            state = 2
          } else digitError(pos)
        case 2 => // offset hour (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetHour = offsetHour * 10 + (b - '0')
            state = 3
          } else digitError(pos)
        case 3 => // ':'
          if (b == ':') state = 4
          else if (b == '"') state = 10
          else tokensError(':', '"', pos)
        case 4 => // offset minute (1st digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = b - '0'
            state = 5
          } else digitError(pos)
        case 5 => // offset minute (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetMinute = offsetMinute * 10 + (b - '0')
            state = 6
          } else digitError(pos)
        case 6 => // ':' or '"'
          if (b == ':') state = 7
          else if (b == '"') state = 10
          else tokensError(':', '"', pos)
        case 7 => // offset second (1st digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = b - '0'
            state = 8
          } else digitError(pos)
        case 8 => // offset second (2nd digit)
          if (b >= '0' && b <= '9') {
            offsetSecond = offsetSecond * 10 + (b - '0')
            state = 9
          } else digitError(pos)
        case 9 => // '"'
          if (b == '"') state = 10
          else tokenError('"', pos)
      }
      pos += 1
    } while (state != 10)
    head = pos
    zoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
  }

  private def epochSecond(yearNeg: Boolean, year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = {
    if (yearNeg && year == 0 || year > 1000000000) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) decodeError("illegal day")
    if (hour > 23) decodeError("illegal hour")
    if (minute > 59) decodeError("illegal minute")
    if (second > 59) decodeError("illegal second")
    val y = if (yearNeg) -year else year
    (365L * y + ({
      if (y > 0) (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400
      else y / 4 - y / 100 + y / 400
    } + ((367 * month - 362) / 12) - {
      if (month > 2) {
        if (isLeap(y)) 1 else 2
      } else 0
    } + day - 719529)) * 86400 + (hour * 3600 + minute * 60 + second) // 719528 == days 0000 to 1970
  }

  private def localDate(yearNeg: Boolean, year: Int, month: Int, day: Int): LocalDate = {
    if (yearNeg && year == 0 || year > 999999999) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(year, month))) decodeError("illegal day")
    try LocalDate.of(if (yearNeg) -year else year, month, day) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def yearMonth(yearNeg: Boolean, year: Int, month: Int): YearMonth = {
    if (yearNeg && year == 0 || year > 999999999) decodeError("illegal year")
    if (month < 1 || month > 12) decodeError("illegal month")
    try YearMonth.of(if (yearNeg) -year else year, month) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def monthDay(month: Int, day: Int): MonthDay = {
    if (month < 1 || month > 12) decodeError("illegal month")
    if (day < 1 || (day > 28 && day > maxDayForYearMonth(2004, month))) decodeError("illegal day")
    try MonthDay.of(month, day) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def localTime(hour: Int, minute: Int, second: Int, nano: Int): LocalTime = {
    if (hour > 23) decodeError("illegal hour")
    if (minute > 59) decodeError("illegal minute")
    if (second > 59) decodeError("illegal second")
    try LocalTime.of(hour, minute, second, nano) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def zoneOffset(offsetNeg: Boolean, offsetHour: Int, offsetMinute: Int, offsetSecond: Int): ZoneOffset = {
    if (offsetHour > 18) decodeError("illegal zone offset hour")
    if (offsetMinute > 59) decodeError("illegal zone offset minute")
    if (offsetSecond > 59) decodeError("illegal zone offset second")
    val offsetTotal = offsetHour * 3600 + offsetMinute * 60 + offsetSecond
    if (offsetTotal > 64800) decodeError("illegal zone offset") // 64800 == 18 * 60 * 60
    try ZoneOffset.ofTotalSeconds(if (offsetNeg) -offsetTotal else offsetTotal)  catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
    }
  }

  private def zoneId(zone: String): ZoneId =
    try ZoneId.of(zone) catch {
      case ex: DateTimeException => dateTimeZoneError(ex)
      case ex: ZoneRulesException => dateTimeZoneError(ex)
    }

  private def maxDayForYearMonth(year: Int, month: Int): Int =
    (month: @switch) match {
      case 1 => 31
      case 2 => if (isLeap(year)) 29 else 28
      case 3 => 31
      case 4 => 30
      case 5 => 31
      case 6 => 30
      case 7 => 31
      case 8 => 31
      case 9 => 30
      case 10 => 31
      case 11 => 30
      case 12 => 31
    }

  private def isLeap(year: Int): Boolean =
    (year & 3) == 0 && {
      val century = year / 100
      century * 100 != year || (century & 3) == 0
    }

  private def digitError(pos: Int): Nothing = decodeError("expected digit", pos)

  private def periodError(pos: Int): Nothing = decodeError("illegal period", pos)

  private def durationError(pos: Int): Nothing = decodeError("illegal duration", pos)

  private def dateTimeZoneError(ex: DateTimeException): Nothing = decodeError("illegal date/time/zone", head - 1, ex)

  private def parseUUID(pos: Int): UUID =
    if (pos + 36 < tail) {
      val mostSigBits1: Int =
        (fromHexDigit(pos) << 28) |
          (fromHexDigit(pos + 1) << 24) |
          (fromHexDigit(pos + 2) << 20) |
          (fromHexDigit(pos + 3) << 16) |
          (fromHexDigit(pos + 4) << 12) |
          (fromHexDigit(pos + 5) << 8) |
          (fromHexDigit(pos + 6) << 4) |
          fromHexDigit(pos + 7)
      checkByte('-', pos + 8)
      checkByte('-', pos + 13)
      val mostSigBits2: Int =
        (fromHexDigit(pos + 9) << 28) |
          (fromHexDigit(pos + 10) << 24) |
          (fromHexDigit(pos + 11) << 20) |
          (fromHexDigit(pos + 12) << 16) |
          (fromHexDigit(pos + 14) << 12) |
          (fromHexDigit(pos + 15) << 8) |
          (fromHexDigit(pos + 16) << 4) |
          fromHexDigit(pos + 17)
      checkByte('-', pos + 18)
      val leastSigBits1: Long =
        (fromHexDigit(pos + 19) << 28) |
          (fromHexDigit(pos + 20) << 24) |
          (fromHexDigit(pos + 21) << 20) |
          (fromHexDigit(pos + 22) << 16) |
          (fromHexDigit(pos + 24) << 12) |
          (fromHexDigit(pos + 25) << 8) |
          (fromHexDigit(pos + 26) << 4) |
          fromHexDigit(pos + 27)
      checkByte('-', pos + 23)
      val leastSigBits2: Long =
        (fromHexDigit(pos + 28) << 28) |
          (fromHexDigit(pos + 29) << 24) |
          (fromHexDigit(pos + 30) << 20) |
          (fromHexDigit(pos + 31) << 16) |
          (fromHexDigit(pos + 32) << 12) |
          (fromHexDigit(pos + 33) << 8) |
          (fromHexDigit(pos + 34) << 4) |
          fromHexDigit(pos + 35)
      checkByte('"', pos + 36)
      head = pos + 37
      new UUID((mostSigBits1.toLong << 32) | (mostSigBits2 & 0xffffffffL),
        (leastSigBits1.toLong << 32) | (leastSigBits2 & 0xffffffffL))
    } else parseUUID(loadMoreOrError(pos))

  private def checkByte(b: Byte, pos: Int): Unit = if (buf(pos) != b) tokenError(b, pos)

  private def parseString(): Int = parseString(0, Math.min(charBuf.length, tail - head), charBuf, head)

  @tailrec
  private def parseString(i: Int, minLim: Int, charBuf: Array[Char], pos: Int): Int =
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
  private def parseEncodedString(i: Int, lim: Int, charBuf: Array[Char], pos: Int): Int =
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
                val ch1 = readEscapedUnicode(pos + 2)
                if (ch1 < 0xD800 || ch1 > 0xDFFF) {
                  charBuf(i) = ch1
                  parseEncodedString(i + 1, lim, charBuf, pos + 6)
                } else if (remaining > 11) {
                  if (buf(pos + 6) == '\\') {
                    if (buf(pos + 7) == 'u') {
                      val ch2 = readEscapedUnicode(pos + 8)
                      if (ch1 >= 0xDC00 || ch2 < 0xDC00 || ch2 > 0xDFFF) decodeError("illegal surrogate character pair", pos + 11)
                      charBuf(i) = ch1
                      charBuf(i + 1) = ch2
                      parseEncodedString(i + 2, lim, charBuf, pos + 12)
                    } else illegalEscapeSequenceError(pos + 7)
                  } else illegalEscapeSequenceError(pos + 6)
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
  private def parseChar(pos: Int): Char = {
    val remaining = tail - pos
    if (remaining > 0) {
      val b1 = buf(pos)
      if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
        if (b1 == '"') decodeError("illegal value for char")
        else if (b1 != '\\') {
          if (b1 < ' ') unescapedControlCharacterError(pos)
          head = pos + 1
          b1.toChar
        } else if (remaining > 1) {
          val b2 = buf(pos + 1)
          if (b2 == 'u') {
            if (remaining > 5) {
              val ch = readEscapedUnicode(pos + 2)
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

  private def readEscapedUnicode(pos: Int): Char =
    ((fromHexDigit(pos) << 12) + (fromHexDigit(pos + 1) << 8) + (fromHexDigit(pos + 2) << 4) + fromHexDigit(pos + 3)).toChar

  private def fromHexDigit(pos: Int): Int = {
    val b = buf(pos)
    if (b >= '0' && b <= '9') b - '0'
    else {
      val b1 = b | 0x20 // 0x20 == 'A' ^ 'a' or 'B' ^ 'b', etc.
      if (b1 >= 'a' && b1 <= 'f') b1 - 0x57 // 0x57 == 'a' - 10
      else decodeError("expected hex digit", pos)
    }
  }

  private def illegalEscapeSequenceError(pos: Int): Nothing = decodeError("illegal escape sequence", pos)

  private def unescapedControlCharacterError(pos: Int): Nothing = decodeError("unescaped control character", pos)

  private def malformedBytesError(b1: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    decodeError(i, pos, null)
  }

  private def malformedBytesError(b1: Byte, b2: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    i = appendString(", 0x", i)
    i = appendHex(b2, i)
    decodeError(i, pos + 1, null)
  }

  private def malformedBytesError(b1: Byte, b2: Byte, b3: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    i = appendString(", 0x", i)
    i = appendHex(b2, i)
    i = appendString(", 0x", i)
    i = appendHex(b3, i)
    decodeError(i, pos + 2, null)
  }

  private def malformedBytesError(b1: Byte, b2: Byte, b3: Byte, b4: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    i = appendString(", 0x", i)
    i = appendHex(b2, i)
    i = appendString(", 0x", i)
    i = appendHex(b3, i)
    i = appendString(", 0x", i)
    i = appendHex(b4, i)
    decodeError(i, pos + 3, null)
  }

  private def appendHexDump(start: Int, end: Int, offset: Int, from: Int): Int = {
    val alignedAbsFrom = (start + offset) & -16
    val alignedAbsTo = (end + offset + 15) & -16
    val len = alignedAbsTo - alignedAbsFrom
    val bufOffset = alignedAbsFrom - offset
    var i = appendChars(dumpHeader, from)
    i = appendChars(dumpBorder, i)
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
        putHex(alignedAbsFrom + j, i + 3, charBuf)
        charBuf(i + 11) = ' '
        charBuf(i + 12) = '|'
        charBuf(i + 13) = ' '
        i += 14
      }
      val pos = bufOffset + j
      charBuf(i + 50 - (linePos << 1)) =
        if (pos >= start && pos < end) {
          val b = buf(pos)
          putHex(b, i, charBuf)
          charBuf(i + 2) = ' '
          if (b <= 31 || b >= 127) '.' else b.toChar
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

  private def appendHex(d: Long, i: Int): Int = {
    if (i + 16 >= charBuf.length) growCharBuf(i + 16)
    var j = i
    val hd = (d >>> 32).toInt
    if (hd != 0) {
      var shift = 4
      while (shift < 32 && (hd >>> shift) != 0) shift += 4
      while (shift > 0) {
        shift -= 4
        charBuf(j) = toHexDigit(hd >>> shift)
        j += 1
      }
    }
    putHex(d.toInt, j, charBuf)
    j + 8
  }

  private def putHex(d: Int, i: Int, charBuf: Array[Char]): Unit = {
    charBuf(i) = toHexDigit(d >>> 28)
    charBuf(i + 1) = toHexDigit(d >>> 24)
    charBuf(i + 2) = toHexDigit(d >>> 20)
    charBuf(i + 3) = toHexDigit(d >>> 16)
    charBuf(i + 4) = toHexDigit(d >>> 12)
    charBuf(i + 5) = toHexDigit(d >>> 8)
    charBuf(i + 6) = toHexDigit(d >>> 4)
    charBuf(i + 7) = toHexDigit(d)
  }

  private def appendHex(b: Byte, i: Int): Int = {
    if (i + 2 >= charBuf.length) growCharBuf(i + 2)
    putHex(b, i, charBuf)
    i + 2
  }

  private def putHex(b: Byte, i: Int, charBuf: Array[Char]): Unit = {
    charBuf(i) = toHexDigit(b >>> 4)
    charBuf(i + 1) = toHexDigit(b)
  }

  private def toHexDigit(n: Int): Char = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + (nibble + 48) // branchless conversion of nibble to hex digit
  }.toChar

  private def growCharBuf(required: Int): Int = {
    val lim = charBuf.length
    val newLim = Math.max(lim << 1, required)
    val cs = new Array[Char](newLim)
    System.arraycopy(charBuf, 0, cs, 0, lim)
    charBuf = cs
    newLim
  }

  @tailrec
  private def skipString(evenBackSlashes: Boolean, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"' && evenBackSlashes) pos + 1
      else skipString(b != '\\' || !evenBackSlashes, pos + 1)
    } else skipString(evenBackSlashes, loadMoreOrError(pos))

  @tailrec
  private def skipNumber(pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if ((b >= '0' && b <= '9') || b == '.' || (b | 0x20) == 'e' || b == '-' || b == '+') skipNumber(pos + 1)
      else pos
    } else skipNumber(loadMoreOrError(pos))

  @tailrec
  private def skipNested(opening: Byte, closing: Byte, level: Int, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') skipNested(opening, closing, level, skipString(evenBackSlashes = true, pos + 1))
      else if (b == closing) {
        if (level == 0) pos + 1
        else skipNested(opening, closing, level - 1, pos + 1)
      } else if (b == opening) skipNested(opening, closing, level + 1, pos + 1)
      else skipNested(opening, closing, level, pos + 1)
    } else skipNested(opening, closing, level, loadMoreOrError(pos))

  @tailrec
  private def skipFixedBytes(n: Int, pos: Int): Int = {
    val newPos = pos + n
    val diff = newPos - tail
    if (diff <= 0) newPos
    else skipFixedBytes(diff, loadMoreOrError(pos))
  }

  private def loadMoreOrError(pos: Int): Int =
    if (in eq null) endOfInputError()
    else {
      val minPos = ensureBufCapacity(pos)
      val n = externalRead(tail, buf.length - tail)
      if (n > 0) {
        tail += n
        totalRead += n
        pos - minPos
      } else endOfInputError()
    }

  private def loadMore(pos: Int): Int =
    if (in eq null) pos
    else {
      val minPos = ensureBufCapacity(pos)
      val n = externalRead(tail, buf.length - tail)
      if (n > 0) {
        tail += n
        totalRead += n
      }
      pos - minPos
    }

  private def ensureBufCapacity(pos: Int): Int = {
    val minPos = Math.min(mark, pos)
    if (minPos > 0) {
      val remaining = tail - minPos
      if (remaining > 0) {
        System.arraycopy(buf, minPos, buf, 0, remaining)
        if (mark != 2147483647) mark -= minPos
      }
      tail = remaining
    } else if (tail > 0) {
      val bs = new Array[Byte](buf.length << 1)
      System.arraycopy(buf, 0, bs, 0, buf.length)
      buf = bs
    }
    minPos
  }

  private def externalRead(from: Int, len: Int): Int = try in.read(buf, from, len) catch {
    case NonFatal(ex) => endOfInputError(ex)
  }

  private def endOfInputError(cause: Throwable = null): Nothing = decodeError("unexpected end of input", tail, cause)

  private def freeTooLongBuf(): Unit =
    if (buf.length > config.preferredBufSize) buf = new Array[Byte](config.preferredBufSize)

  private def freeTooLongCharBuf(): Unit =
    if (charBuf.length > config.preferredCharBufSize) charBuf = new Array[Char](config.preferredCharBufSize)
}

object JsonReader {
  private final val pow10: Array[Double] = // all powers of 10 that can be represented exactly in double
    Array(1, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11,
      1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22)
  private final val nanoMultiplier: Array[Int] =
    Array(1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1)
  private final val dumpHeader: Array[Char] = {
    "\n           +-------------------------------------------------+" +
    "\n           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |"
  }.toCharArray
  private final val dumpBorder: Array[Char] =
    "\n+----------+-------------------------------------------------+------------------+".toCharArray

  final def toHashCode(cs: Array[Char], len: Int): Int = {
    var h = 0
    var i = 0
    while (i < len) i = {
      h = (h << 5) + (cs(i) - h)
      i + 1
    }
    h
  }
}