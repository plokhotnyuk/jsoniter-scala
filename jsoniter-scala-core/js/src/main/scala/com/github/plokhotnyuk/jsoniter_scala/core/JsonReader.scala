package com.github.plokhotnyuk.jsoniter_scala.core

import java.io.InputStream
import java.math.MathContext
import java.nio.ByteBuffer
import java.time._
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import com.github.plokhotnyuk.expression_evaluator.eval
import com.github.plokhotnyuk.jsoniter_scala.core.JsonReader._

import scala.annotation.{switch, tailrec}
import scala.{specialized => sp}

/**
  * Configuration for [[com.github.plokhotnyuk.jsoniter_scala.core.JsonReader]] that contains flags for tuning of
  * parsing exceptions and preferred sizes for internal buffers that are created on the reader instantiation and reused
  * in runtime for parsing of messages.
  * <br/>
  * All configuration params already initialized by recommended default values, but in some cases they should be altered
  * for performance reasons:
  * <ul>
  * <li>turn off stack traces for parsing exceptions to greatly reduce impact on performance for cases when exceptions
  * can be not exceptional (e.g. under DoS attacks over open to the world systems), see more details here:
  * [[https://shipilev.net/blog/2014/exceptional-performance/]]</li>
  * <li>turn off appending of hex dump to minimize length of exception message</li>
  * <li>increase preferred size of an internal byte buffer for parsing from [[java.io.InputStream]] or
  * [[java.nio.DirectByteBuffer]] to reduce allocation rate of grown and then reduced buffers during parsing of large
  * (>16Kb) numbers (including stringified), raw values, or ADT instances with the discriminator field doesn't
  * appear in the beginning of the JSON object</li>
  * <li>increase preferred size of an internal char buffer to reduce allocation rate of grown and then reduced
  * buffers when large (>4Kb) string instances need to be parsed including those one which use Base16 or Base64
  * encodings</li>
  * </ul>
  * @param throwReaderExceptionWithStackTrace a flag that allows to turn on a stack traces for debugging purposes in
  *                                           development
  * @param appendHexDumpToParseException a flag that allows to turn off hex dumping of affected by error part of
  *                                      an internal byte buffer
  * @param preferredBufSize a preferred size (in bytes) of an internal byte buffer when parsing from
  *                         [[java.io.InputStream]]
  * @param preferredCharBufSize a preferred size (in chars) of an internal char buffer for parsing of string values
  * @param checkForEndOfInput a flag to check and raise an error if some non whitespace bytes will be detected after
  *                           successful parsing of the value
  */
class ReaderConfig private (
    val throwReaderExceptionWithStackTrace: Boolean,
    val appendHexDumpToParseException: Boolean,
    val preferredBufSize: Int,
    val preferredCharBufSize: Int,
    val checkForEndOfInput: Boolean) {
  def withThrowReaderExceptionWithStackTrace(throwReaderExceptionWithStackTrace: Boolean): ReaderConfig =
    copy(throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace)

  def withAppendHexDumpToParseException(appendHexDumpToParseException: Boolean): ReaderConfig =
    copy(appendHexDumpToParseException = appendHexDumpToParseException)

  def withPreferredBufSize(preferredBufSize: Int): ReaderConfig = {
    if (preferredBufSize < 12) throw new IllegalArgumentException("'preferredBufSize' should be not less than 12")
    copy(preferredBufSize = preferredBufSize)
  }

  def withPreferredCharBufSize(preferredCharBufSize: Int): ReaderConfig = {
    if (preferredCharBufSize < 0) throw new IllegalArgumentException("'preferredCharBufSize' should be not less than 0")
    copy(preferredCharBufSize = preferredCharBufSize)
  }

  def withCheckForEndOfInput(checkForEndOfInput: Boolean): ReaderConfig =
    copy(checkForEndOfInput = checkForEndOfInput)

  private[this] def copy(throwReaderExceptionWithStackTrace: Boolean = throwReaderExceptionWithStackTrace,
                         appendHexDumpToParseException: Boolean = appendHexDumpToParseException,
                         preferredBufSize: Int = preferredBufSize,
                         preferredCharBufSize: Int = preferredCharBufSize,
                         checkForEndOfInput: Boolean = checkForEndOfInput): ReaderConfig =
    new ReaderConfig(
      throwReaderExceptionWithStackTrace = throwReaderExceptionWithStackTrace,
      appendHexDumpToParseException = appendHexDumpToParseException,
      preferredBufSize = preferredBufSize,
      preferredCharBufSize = preferredCharBufSize,
      checkForEndOfInput = checkForEndOfInput)
}

object ReaderConfig extends ReaderConfig(
    throwReaderExceptionWithStackTrace = false,
    appendHexDumpToParseException = true,
    preferredBufSize = 16384,
    preferredCharBufSize = 4096,
    checkForEndOfInput = true)

class JsonReaderException private[jsoniter_scala](msg: String, cause: Throwable)
  extends RuntimeException(msg, cause) // FIXME: No constructor with the withStackTrace parameter

final class JsonReader private[jsoniter_scala](
    private[this] var buf: Array[Byte] = new Array[Byte](16384),
    private[this] var head: Int = 0,
    private[this] var tail: Int = 0,
    private[this] var mark: Int = -1,
    private[this] var charBuf: Array[Char] = new Array[Char](4096),
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
    if (mark < 0) missingSetMarkOperation()
    head = mark
    mark = -1
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
    val x = Year.of(parseYearWithByte('"', 9, head))
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
    val x = parseZoneIdWithByte('"')
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

  def readKeyAsBigInt(): BigInt = readKeyAsBigInt(bigIntDigitsLimit)

  def readKeyAsBigInt(digitsLimit: Int): BigInt = {
    nextTokenOrError('"', head)
    val x = parseBigInt(isToken = false, null, digitsLimit)
    nextByteOrError('"', head)
    nextTokenOrError(':', head)
    x
  }

  def readKeyAsBigDecimal(): BigDecimal =
    readKeyAsBigDecimal(bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit)

  def readKeyAsBigDecimal(mc: MathContext, scaleLimit: Int, digitsLimit: Int): BigDecimal = {
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

  def readBigInt(default: BigInt): BigInt = parseBigInt(isToken = true, default, bigIntDigitsLimit)

  def readBigInt(default: BigInt, digitsLimit: Int): BigInt = parseBigInt(isToken = true, default, digitsLimit)

  def readBigDecimal(default: BigDecimal): BigDecimal =
    parseBigDecimal(isToken = true, default, bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit)

  def readBigDecimal(default: BigDecimal, mc: MathContext, scaleLimit: Int, digitsLimit: Int): BigDecimal =
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
    if (isNextToken('"', head)) Year.of(parseYearWithByte('"', 9, head))
    else readNullOrTokenError(default, '"')

  def readYearMonth(default: YearMonth): YearMonth =
    if (isNextToken('"', head)) parseYearMonth()
    else readNullOrTokenError(default, '"')

  def readZonedDateTime(default: ZonedDateTime): ZonedDateTime =
    if (isNextToken('"', head)) parseZonedDateTime()
    else readNullOrTokenError(default, '"')

  def readZoneId(default: ZoneId): ZoneId =
    if (isNextToken('"', head)) parseZoneIdWithByte('"')
    else readNullOrTokenError(default, '"')

  def readZoneOffset(default: ZoneOffset): ZoneOffset =
    if (isNextToken('"', head)) parseZoneOffset()
    else readNullOrTokenError(default, '"')

  def readUUID(default: UUID): UUID =
    if (isNextToken('"', head)) parseUUID(head)
    else readNullOrTokenError(default, '"')

  def readBase16AsBytes(default: Array[Byte]): Array[Byte] =
    if (isNextToken('"', head)) parseBase16(nibbles)
    else readNullOrTokenError(default, '"')

  def readBase64AsBytes(default: Array[Byte]): Array[Byte] =
    if (isNextToken('"', head)) parseBase64(base64Bytes)
    else readNullOrTokenError(default, '"')

  def readBase64UrlAsBytes(default: Array[Byte]): Array[Byte] =
    if (isNextToken('"', head)) parseBase64(base64UrlBytes)
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

  def readStringAsBigInt(default: BigInt): BigInt = readStringAsBigInt(default, bigIntDigitsLimit)

  def readStringAsBigInt(default: BigInt, digitsLimit: Int): BigInt =
    if (isNextToken('"', head)) {
      val x = parseBigInt(isToken = false, default, digitsLimit)
      nextByteOrError('"', head)
      x
    } else readNullOrTokenError(default, '"')

  def readStringAsBigDecimal(default: BigDecimal): BigDecimal =
    readStringAsBigDecimal(default, bigDecimalMathContext, bigDecimalScaleLimit, bigDecimalDigitsLimit)

  def readStringAsBigDecimal(default: BigDecimal, mc: MathContext, scaleLimit: Int, digitsLimit: Int): BigDecimal =
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

  def readRawValAsBytes(): Array[Byte] = {
    var from = head
    val oldMark = mark
    val newMark =
      if (oldMark < 0) from
      else oldMark
    mark = newMark
    try {
      skip()
      if (mark == 0) from -= newMark
      val len = head - from
      val x = new Array[Byte](len)
      System.arraycopy(buf, from, x, 0, len)
      x
    } finally if (mark != 0 || oldMark < 0) mark = oldMark
  }

  def readNullOrError[@sp A](default: A, msg: String): A =
    if (default != null && isCurrentToken('n', head)) parseNullOrError(default, msg, head)
    else decodeError(msg)

  def readNullOrTokenError[@sp A](default: A, t: Byte): A =
    if (default != null && isCurrentToken('n', head)) parseNullOrTokenError(default, t, head)
    else if (default != null) tokenOrNullError(t)
    else tokenError(t)

  def nextByte(): Byte = nextByte(head)

  def nextToken(): Byte = nextToken(head)

  def isNextToken(t: Byte): Boolean = isNextToken(t, head)

  def isCurrentToken(t: Byte): Boolean = isCurrentToken(t, head)

  def rollbackToken(): Unit = {
    val pos = head
    if (pos == 0) illegalTokenOperation()
    head = pos - 1
  }

  def charBufToHashCode(len: Int): Int = toHashCode(charBuf, len)

  def isCharBufEqualsTo(len: Int, s: String): Boolean = {
    if (s eq null) throw new NullPointerException
    s.length == len && {
      val charBuf = this.charBuf
      var i = 0
      while (i < len) {
        if (s.charAt(i) != charBuf(i)) return false
        i += 1
      }
      true
    }
  }

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

  def commaError(): Nothing = decodeError("expected ','")

  def arrayStartOrNullError(): Nothing = decodeError("expected '[' or null")

  def arrayEndError(): Nothing = decodeError("expected ']'")

  def arrayEndOrCommaError(): Nothing = decodeError("expected ']' or ','")

  def objectStartOrNullError(): Nothing = decodeError("expected '{' or null")

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
      mark = -1
      val x = codec.decodeValue(this, codec.nullValue)
      if (head != to && config.checkForEndOfInput) endOfInputOrError()
      x
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
      mark = -1
      if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
      val x = codec.decodeValue(this, codec.nullValue)
      if (config.checkForEndOfInput) endOfInputOrError()
      x
    } finally {
      this.in = null
      if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }

  private[jsoniter_scala] def read[@sp A](codec: JsonValueCodec[A], bbuf: ByteBuffer, config: ReaderConfig): A =
    if (bbuf.hasArray) {
      val offset = bbuf.arrayOffset
      val to = offset + bbuf.limit()
      val currBuf = this.buf
      try {
        this.buf = bbuf.array
        this.config = config
        head = offset + bbuf.position()
        tail = to
        totalRead = 0
        mark = -1
        val x = codec.decodeValue(this, codec.nullValue)
        if (head != to && config.checkForEndOfInput) endOfInputOrError()
        x
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
        mark = -1
        if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
        val x = codec.decodeValue(this, codec.nullValue)
        if (config.checkForEndOfInput) endOfInputOrError()
        x
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
      mark = -1
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
      mark = -1
      if (buf.length < config.preferredBufSize) reallocateBufToPreferredSize()
      var continue = true
      if (isNextToken('[', head)) {
        if (!isNextToken(']', head)) {
          head -= 1
          while ({
            continue = f(codec.decodeValue(this, codec.nullValue))
            continue && isNextToken(',', head)
          }) ()
          if (continue && !isCurrentToken(']', head)) arrayEndOrCommaError()
        }
      } else readNullOrTokenError((), '[')
      if (continue && config.checkForEndOfInput) endOfInputOrError()
    } finally {
      this.in = null
      if (buf.length > config.preferredBufSize) reallocateBufToPreferredSize()
      if (charBuf.length > config.preferredCharBufSize) reallocateCharBufToPreferredSize()
    }

  private[jsoniter_scala] def endOfInputOrError(): Boolean =
    !skipWhitespaces() || decodeError("expected end of input", head)

  private[this] def skipWhitespaces(): Boolean = {
    var pos = head
    var buf = this.buf
    while ((pos < tail || {
      pos = loadMore(pos)
      buf = this.buf
      pos < tail
    }) && {
      val b = buf(pos)
      b == ' ' || b == '\n' || (b | 0x4) == '\r'
    }) pos += 1
    head = pos
    pos != tail
  }

  private[this] def tokenOrDigitError(t: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(t.toChar, i)
    i = appendString("' or digit", i)
    decodeError(i, pos, null)
  }

  private[this] def tokensError(t1: Byte, t2: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(t1.toChar, i)
    i = appendString("' or '", i)
    i = appendChar(t2.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private[this] def tokenOrNullError(t: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(t.toChar, i)
    i = appendString("' or null", i)
    decodeError(i, pos, null)
  }

  private[this] def tokenError(t: Byte, pos: Int = head - 1): Nothing = {
    var i = appendString("expected '", 0)
    i = appendChar(t.toChar, i)
    i = appendChar('\'', i)
    decodeError(i, pos, null)
  }

  private[this] def decodeError(msg: String, pos: Int, cause: Throwable = null): Nothing =
    decodeError(appendString(msg, 0), pos, cause)

  private[this] def decodeError(from: Int, pos: Int, cause: Throwable): Nothing = {
    var i = appendString(", offset: 0x", from)
    val offset =
      if ((bbuf eq null) && (in eq null)) 0
      else totalRead - tail
    i = appendHexOffset(offset + pos, i)
    if (config.appendHexDumpToParseException) {
      i = appendString(", buf:", i)
      i = appendHexDump(pos, offset.toInt, i)
    }
    throw new JsonReaderException(new String(charBuf, 0, i), cause)
  }

  @tailrec
  private[this] def nextByte(pos: Int): Byte =
    if (pos < tail) {
      head = pos + 1
      buf(pos)
    } else nextByte(loadMoreOrError(pos))

  @tailrec
  private[this] def nextByteOrError(t: Byte, pos: Int): Unit =
    if (pos < tail) {
      if (buf(pos) != t) tokenError(t, pos)
      head = pos + 1
    } else nextByteOrError(t, loadMoreOrError(pos))

  @tailrec
  private[this] def nextToken(pos: Int): Byte =
    if (pos < tail) {
      val b = buf(pos)
      if (b == ' ' || b == '\n' || (b | 0x4) == '\r') nextToken(pos + 1)
      else {
        head = pos + 1
        b
      }
    } else nextToken(loadMoreOrError(pos))

  @tailrec
  private[this] def nextTokenOrError(t: Byte, pos: Int): Unit =
    if (pos < tail) {
      val b = buf(pos)
      head = pos + 1
      if (b != t && ((b != ' ' && b != '\n' && (b | 0x4) != '\r') || nextToken(pos + 1) != t)) tokenError(t, head - 1)
    } else nextTokenOrError(t, loadMoreOrError(pos))

  @tailrec
  private[this] def isNextToken(t: Byte, pos: Int): Boolean =
    if (pos < tail) {
      val b = buf(pos)
      head = pos + 1
      b == t || ((b == ' ' || b == '\n' || (b | 0x4) == '\r') && nextToken(pos + 1) == t)
    } else isNextToken(t, loadMoreOrError(pos))

  private[this] def isCurrentToken(t: Byte, pos: Int): Boolean = {
    if (pos == 0) illegalTokenOperation()
    buf(pos - 1) == t
  }

  private[this] def illegalTokenOperation(): Nothing =
    throw new IllegalStateException("expected preceding call of 'nextToken()' or 'isNextToken()'")

  private[this] def missingSetMarkOperation(): Nothing =
    throw new IllegalStateException("expected preceding call of 'setMark()'")

  @tailrec
  private[this] def parseYearWithByte(t: Byte, maxDigits: Int, pos: Int): Int =
    if (pos + 4 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      if (b1 >= '0' && b1 <= '9') {
        val b2 = buf(pos + 1)
        val b3 = buf(pos + 2)
        val b4 = buf(pos + 3)
        val b5 = buf(pos + 4)
        head = pos + 5
        if (b2 < '0' || b2 > '9') digitError(pos + 1)
        if (b3 < '0' || b3 > '9') digitError(pos + 2)
        if (b4 < '0' || b4 > '9') digitError(pos + 3)
        if (b5 != t) tokenError(t, pos + 4)
        b1 * 1000 + b2 * 100 + b3 * 10 + b4 - 53328 // 53328 == '0' * 1111
      } else parseNon4DigitYearWithByte(t, maxDigits, b1, pos)
    } else parseYearWithByte(t, maxDigits, loadMoreOrError(pos))

  private[this] def parseNon4DigitYearWithByte(t: Byte, maxDigits: Int, b1: Byte, p: Int): Int = {
    var pos = p
    var buf = this.buf
    val b2 = buf(pos + 1)
    val b3 = buf(pos + 2)
    val b4 = buf(pos + 3)
    val b5 = buf(pos + 4)
    val yearNeg = b1 == '-' || (b1 != '+' && decodeError("expected '-' or '+' or digit", pos))
    if (b2 < '0' || b2 > '9') digitError(pos + 1)
    if (b3 < '0' || b3 > '9') digitError(pos + 2)
    if (b4 < '0' || b4 > '9') digitError(pos + 3)
    if (b5 < '0' || b5 > '9') digitError(pos + 4)
    var year = b2 * 1000 + b3 * 100 + b4 * 10 + b5 - 53328 // 53328 == '0' * 1111
    var yearDigits = 4
    var b: Byte = 0
    pos += 5
    while ({
      if (pos >= tail) {
        pos = loadMoreOrError(pos)
        buf = this.buf
      }
      b = buf(pos)
      b >= '0' && b <= '9' && yearDigits < maxDigits
    }) {
      year =
        if (year > 100000000) 2147483647
        else year * 10 + (b - '0')
      yearDigits += 1
      pos += 1
    }
    head = pos + 1
    if (yearNeg && year == 0 || yearDigits == 10 && year > 1000000000) yearError(pos - 1)
    if (b != t) yearError(t, maxDigits, pos, yearNeg, yearDigits)
    if (yearNeg) year = -year
    year
  }

  @tailrec
  private[this] def parseMonthWithByte(t: Byte, pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      val month = b1 * 10 + b2 - 528 // 528 == '0' * 11
      head = pos + 3
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (month < 1 || month > 12) monthError(pos + 1)
      if (b3 != t) tokenError(t, pos + 2)
      month
    } else parseMonthWithByte(t, loadMoreOrError(pos))

  @tailrec
  private[this] def parseDayWithByte(year: Int, month: Int, t: Byte, pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      val day = b1 * 10 + b2 - 528 // 528 == '0' * 11
      head = pos + 3
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (day == 0 || (day > 28 && day > maxDayForYearMonth(year, month))) dayError(pos + 1)
      if (b3 != t) tokenError(t, pos + 2)
      day
    } else parseDayWithByte(year, month, t, loadMoreOrError(pos))

  @tailrec
  private[this] def parseHourWithByte(t: Byte, pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      val hour = b1 * 10 + b2 - 528 // 528 == '0' * 11
      head = pos + 3
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (hour > 23) hourError(pos + 1)
      if (b3 != t) tokenError(t, pos + 2)
      hour
    } else parseHourWithByte(t, loadMoreOrError(pos))

  @tailrec
  private[this] def parseMinute(pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      head = pos + 2
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (b1 > '5') minuteError(pos + 1)
      b1 * 10 + b2 - 528 // 528 == '0' * 11
    } else parseMinute(loadMoreOrError(pos))

  @tailrec
  private[this] def parseSecond(pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      head = pos + 2
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (b1 > '5') secondError(pos + 1)
      b1 * 10 + b2 - 528 // 528 == '0' * 11
    } else parseSecond(loadMoreOrError(pos))

  private[this] def parseOptionalNanoWithByte(t: Byte): Int = {
    var nano = 0
    var b = nextByte(head)
    if (b == '.') {
      var nanoDigitWeight = 100000000
      var pos = head
      var buf = this.buf
      while ({
        if (pos >= tail) {
          pos = loadMoreOrError(pos)
          buf = this.buf
        }
        b = buf(pos)
        pos += 1
        b >= '0' && b <= '9' && nanoDigitWeight != 0
      }) {
        nano += (b - '0') * nanoDigitWeight
        nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
      }
      head = pos
      if (b != t) nanoError(nanoDigitWeight, t)
    } else if (b != t) tokensError('.', t)
    nano
  }

  @tailrec
  private[this] def parseOffsetHour(pos: Int): Int =
    if (pos + 1 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val offsetHour = b1 * 10 + b2 - 528 // 528 == '0' * 11
      head = pos + 2
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (offsetHour > 18) timezoneOffsetHourError(pos + 1)
      offsetHour
    } else parseOffsetHour(loadMoreOrError(pos))

  @tailrec
  private[this] def parseOffsetMinute(pos: Int): Int =
    if (pos + 1 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      head = pos + 2
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (b1 > '5') timezoneOffsetMinuteError(pos + 1)
      b1 * 10 + b2 - 528 // 528 == '0' * 11
    } else parseOffsetMinute(loadMoreOrError(pos))

  @tailrec
  private[this] def parseOffsetSecond(pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      head = pos + 2
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (b1 > '5') timezoneOffsetSecondError(pos + 1)
      b1 * 10 + b2 - 528 // 528 == '0' * 11
    } else parseOffsetSecond(loadMoreOrError(pos))

  @tailrec
  private[this] def parseOffsetSecondWithByte(t: Byte, pos: Int): Int =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      head = pos + 3
      if (b1 < '0' || b1 > '9') digitError(pos)
      if (b2 < '0' || b2 > '9') digitError(pos + 1)
      if (b1 > '5') timezoneOffsetSecondError(pos + 1)
      if (b3 != t) tokenError(t, pos + 2)
      b1 * 10 + b2 - 528 // 528 == '0' * 11
    } else parseOffsetSecondWithByte(t, loadMoreOrError(pos))

  private[this] def parseZoneIdWithByte(t: Byte): ZoneId = {
    var from = head
    val oldMark = mark
    val newMark =
      if (oldMark < 0) from
      else oldMark
    mark = newMark
    try {
      var pos = from
      var buf = this.buf
      var hash, b = 0
      while ({
        if (pos >= tail) {
          pos = loadMoreOrError(pos)
          buf = this.buf
        }
        b = buf(pos)
        b != t
      }) {
        hash = (hash << 5) - hash + b
        pos += 1
      }
      head = pos + 1
      if (mark == 0) from -= newMark
      val k = new Key(hash, buf, from, pos)
      var zoneId = zoneIds.get(k)
      if ((zoneId eq null) && {
        zoneId = ZoneId.of(k.toString)
        !zoneId.isInstanceOf[ZoneOffset] || zoneId.asInstanceOf[ZoneOffset].getTotalSeconds % 900 == 0
      }) zoneIds.put(k.copy, zoneId)
      zoneId
    } catch {
      case ex: DateTimeException => timezoneError(ex)
    } finally if (mark != 0 || oldMark < 0) mark = oldMark
  }

  @tailrec
  private[this] def parseNullOrError[@sp A](default: A, error: String, pos: Int): A =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      head = pos + 3
      if (b1 != 'u') decodeError(error, pos)
      if (b2 != 'l') decodeError(error, pos + 1)
      if (b3 != 'l') decodeError(error, pos + 2)
      default
    } else parseNullOrError(default, error, loadMoreOrError(pos))

  @tailrec
  private[this] def parseNullOrTokenError[@sp A](default: A, t: Byte, pos: Int): A =
    if (pos + 2 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      head = pos + 3
      if (b1 != 'u') tokenOrNullError(t, pos)
      if (b2 != 'l') tokenOrNullError(t, pos + 1)
      if (b3 != 'l') tokenOrNullError(t, pos + 2)
      default
    } else parseNullOrTokenError(default, t, loadMoreOrError(pos))

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
      val buf = this.buf
      val b1 = buf(pos)
      if (b1 == 't') {
        val b2 = buf(pos + 1)
        val b3 = buf(pos + 2)
        val b4 = buf(pos + 3)
        head = pos + 4
        if (b2 != 'r') booleanError(pos + 1)
        if (b3 != 'u') booleanError(pos + 2)
        if (b4 != 'e') booleanError(pos + 3)
        true
      } else if (b1 == 'f') {
        if (pos + 4 < tail) {
          val b2 = buf(pos + 1)
          val b3 = buf(pos + 2)
          val b4 = buf(pos + 3)
          val b5 = buf(pos + 4)
          head = pos + 5
          if (b2 != 'a') booleanError(pos + 1)
          if (b3 != 'l') booleanError(pos + 2)
          if (b4 != 's') booleanError(pos + 3)
          if (b5 != 'e') booleanError(pos + 4)
          false
        } else parseBoolean(isToken, loadMoreOrError(pos))
      } else if (isToken && (b1 == ' ' || b1 == '\n' || (b1 | 0x4) == '\r')) parseBoolean(isToken, pos + 1)
      else booleanError(pos)
    } else parseBoolean(isToken, loadMoreOrError(pos))

  private[this] def booleanError(pos: Int): Nothing = decodeError("illegal boolean", pos)

  private[this] def parseByte(isToken: Boolean): Byte = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var x = b - '0'
    if (isToken && x == 0) ensureNotLeadingZero()
    else {
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        x = x * 10 + (b - '0')
        if (x > 128) byteOverflowError(pos)
        pos += 1
      }
      head = pos
      if ((b | 0x20) == 'e' || b == '.') numberError(pos)
      if (isNeg) x = -x
      else if (x == 128) byteOverflowError(pos - 1)
    }
    x.toByte
  }

  private[this] def parseShort(isToken: Boolean): Short = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var x = b - '0'
    if (isToken && x == 0) ensureNotLeadingZero()
    else {
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        x = x * 10 + (b - '0')
        if (x > 32768) shortOverflowError(pos)
        pos += 1
      }
      head = pos
      if ((b | 0x20) == 'e' || b == '.') numberError(pos)
      if (isNeg) x = -x
      else if (x == 32768) shortOverflowError(pos - 1)
    }
    x.toShort
  }

  private[this] def parseInt(isToken: Boolean): Int = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var x: Long = b - '0'
    if (isToken && x == 0) ensureNotLeadingZero()
    else {
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        x = x * 10 + (b - '0')
        if (x > 2147483648L) intOverflowError(pos)
        pos += 1
      }
      head = pos
      if ((b | 0x20) == 'e' || b == '.') numberError(pos)
      if (isNeg) x = -x
      else if (x == 2147483648L) intOverflowError(pos - 1)
    }
    x.toInt
  }

  private[this] def parseLong(isToken: Boolean): Long = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var x: Long = '0' - b
    if (isToken && x == 0) ensureNotLeadingZero()
    else {
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (x < -922337203685477580L || {
          x = x * 10 + ('0' - b)
          x > 0
        }) longOverflowError(pos)
        pos += 1
      }
      head = pos
      if ((b | 0x20) == 'e' || b == '.') numberError(pos)
      if (!isNeg) {
        if (x == -9223372036854775808L) longOverflowError(pos - 1)
        x = -x
      }
    }
    x
  }

  private[this] def ensureNotLeadingZero(): Unit = {
    var pos = head
    if ((pos < tail || {
      pos = loadMore(pos)
      pos < tail
    }) && {
      val b = buf(pos)
      b >= '0' && b <= '9'
    }) leadingZeroError(pos - 1)
  }

  private[this] def parseDouble(isToken: Boolean): Double = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var pos = head
    var buf = this.buf
    val from = pos - 1
    val oldMark = mark
    val newMark =
      if (oldMark < 0) from
      else oldMark
    mark = newMark
    try {
      var posMant: Long = b - '0'
      var exp = 0L
      var digits = 1
      if (isToken && posMant == 0) {
        if ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) leadingZeroError(pos - 1)
      } else {
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMant < 922337203685477580L) {
            posMant = posMant * 10 + (b - '0')
            digits += 1
          } else exp += 1
          pos += 1
        }
      }
      if (b == '.') {
        pos += 1
        exp += digits
        var noFracDigits = true
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMant < 922337203685477580L) {
            posMant = posMant * 10 + (b - '0')
            digits += 1
          }
          noFracDigits = false
          pos += 1
        }
        exp -= digits
        if (noFracDigits) numberError(pos)
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        val isNegExp = b == '-'
        if (isNegExp || b == '+') b = nextByte(head)
        if (b < '0' || b > '9') numberError()
        var posExp: Long = b - '0'
        pos = head
        buf = this.buf
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posExp < 92233720368547758L) posExp = posExp * 10 + (b - '0')
          pos += 1
        }
        if (isNegExp) posExp = -posExp
        exp += posExp
      }
      head = pos
      var x: Double =
        if (exp == 0 && posMant < 922337203685477580L) posMant.toDouble
        else if (posMant < 4503599627370496L && Math.abs(exp) <= 22) {
          if (exp < 0) posMant / pow10Doubles(-exp.toInt)
          else posMant * pow10Doubles(exp.toInt)
        } else if (posMant < 4503599627370496L && exp > 22 && exp + digits <= 38) {
          val pow10 = pow10Doubles
          val slop = 16 - digits
          (posMant * pow10(slop)) * pow10(exp.toInt - slop)
        } else toDouble(posMant, exp, from, newMark, pos)
      if (isNeg) x = -x
      x
    } finally if (mark != 0 || oldMark < 0) mark = oldMark
  }

  // Based on the 'Moderate Path' algorithm from the awesome library of Alexander Huszagh: https://github.com/Alexhuszagh/rust-lexical
  // Here is his inspiring post: https://www.reddit.com/r/rust/comments/a6j5j1/making_rust_float_parsing_fast_and_correct
  private[this] def toDouble(m: Long, e: Long, from: Int, newMark: Int, pos: Int): Double =
    if (m == 0 || e < -343) 0.0
    else if (e >= 310) Double.PositiveInfinity
    else {
      var shift = java.lang.Long.numberOfLeadingZeros(m)
      var mant = mulMant(m << shift, e.toInt)
      var exp = addExp(-shift, e.toInt)
      shift = java.lang.Long.numberOfLeadingZeros(mant)
      mant <<= shift
      exp -= shift
      val roundingError =
        (if (m < 922337203685477580L) 2
        else 20) << shift
      val truncatedBitNum = Math.max(-1074 - exp, 11)
      val savedBitNum = 64 - truncatedBitNum
      val mask = -1L >>> Math.max(savedBitNum, 0)
      val halfwayDiff = (mant & mask) - (mask >>> 1)
      if (Math.abs(halfwayDiff) >= roundingError || savedBitNum <= 0) java.lang.Double.longBitsToDouble {
        if (savedBitNum <= 0) mant = 0
        mant >>>= truncatedBitNum
        exp += truncatedBitNum
        if (savedBitNum >= 0 && halfwayDiff > 0) {
          if (mant == 0x001FFFFFFFFFFFFFL) {
            mant = 0x0010000000000000L
            exp += 1
          } else mant += 1
        }
        if (exp == -1074) mant
        else if (exp >= 972) 0x7FF0000000000000L
        else (exp + 1075L) << 52 | mant & 0x000FFFFFFFFFFFFFL
      } else {
        var offset = from
        if (mark == 0) offset -= newMark
        java.lang.Double.parseDouble(new String(buf, offset, pos - offset))
      }
    }

  private[this] def parseFloat(isToken: Boolean): Float = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b < '0' || b > '9') numberError()
    var pos = head
    var buf = this.buf
    val from = pos - 1
    val oldMark = mark
    val newMark =
      if (oldMark < 0) from
      else oldMark
    mark = newMark
    try {
      var posMant: Long = b - '0'
      var exp = 0L
      var digits = 1
      if (isToken && posMant == 0) {
        if ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) leadingZeroError(pos - 1)
      } else {
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMant < 922337203685477580L) {
            posMant = posMant * 10 + (b - '0')
            digits += 1
          } else exp += 1
          pos += 1
        }
      }
      if (b == '.') {
        pos += 1
        exp += digits
        var noFracDigits = true
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posMant < 922337203685477580L) {
            posMant = posMant * 10 + (b - '0')
            digits += 1
          }
          noFracDigits = false
          pos += 1
        }
        exp -= digits
        if (noFracDigits) numberError(pos)
      }
      if ((b | 0x20) == 'e') {
        b = nextByte(pos + 1)
        val isNegExp = b == '-'
        if (isNegExp || b == '+') b = nextByte(head)
        if (b < '0' || b > '9') numberError()
        var posExp: Long = b - '0'
        pos = head
        buf = this.buf
        while ((pos < tail || {
          pos = loadMore(pos)
          buf = this.buf
          pos < tail
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) {
          if (posExp < 92233720368547758L) posExp = posExp * 10 + (b - '0')
          pos += 1
        }
        if (isNegExp) posExp = -posExp
        exp += posExp
      }
      head = pos
      var x: Float =
        if (exp == 0 && posMant < 922337203685477580L) posMant.toFloat
        else if (posMant < 4294967296L && exp < 0 && exp >= digits - 23) (posMant / pow10Doubles(-exp.toInt)).toFloat
        else if (posMant < 4294967296L && exp >= 0 && exp <= 19 - digits) (posMant * pow10Doubles(exp.toInt)).toFloat
        else toFloat(posMant, exp, from, newMark, pos)
      if (isNeg) {
        if (x == 0f) x = -0f // FIXME: Workaround for https://github.com/scala-js/scala-js/issues/4034
        else x = -x
      }
      x
    } finally if (mark != 0 || oldMark < 0) mark = oldMark
  }

  // Based on the 'Moderate Path' algorithm from the awesome library of Alexander Huszagh: https://github.com/Alexhuszagh/rust-lexical
  // Here is his inspiring post: https://www.reddit.com/r/rust/comments/a6j5j1/making_rust_float_parsing_fast_and_correct
  private[this] def toFloat(m: Long, e: Long, from: Int, newMark: Int, pos: Int): Float =
    if (m == 0 || e < -64) 0.0f
    else if (e >= 39) Float.PositiveInfinity
    else {
      var shift = java.lang.Long.numberOfLeadingZeros(m)
      var mant = mulMant(m << shift, e.toInt)
      var exp = addExp(-shift, e.toInt)
      shift = java.lang.Long.numberOfLeadingZeros(mant)
      mant <<= shift
      exp -= shift
      val roundingError =
        (if (m < 922337203685477580L) 2
        else 20) << shift
      val truncatedBitNum = Math.max(-149 - exp, 40)
      val savedBitNum = 64 - truncatedBitNum
      val mask = -1L >>> Math.max(savedBitNum, 0)
      val halfwayDiff = (mant & mask) - (mask >>> 1)
      if (Math.abs(halfwayDiff) >= roundingError || savedBitNum <= 0) java.lang.Float.intBitsToFloat {
        if (savedBitNum <= 0) mant = 0
        mant >>>= truncatedBitNum
        exp += truncatedBitNum
        if (savedBitNum >= 0 && halfwayDiff > 0) {
          if (mant == 0x00FFFFFF) {
            mant = 0x00800000
            exp += 1
          } else mant += 1
        }
        if (exp == -149) mant.toInt
        else if (exp >= 105) 0x7F800000
        else (exp + 150) << 23 | mant.toInt & 0x007FFFFF
      } else {
        var offset = from
        if (mark == 0) offset -= newMark
        java.lang.Float.parseFloat(new String(buf, offset, pos - offset))
      }
    }

  // 64-bit unsigned multiplication was adopted from the great Hacker's Delight function
  // (Henry S. Warren, Hacker's Delight, Addison-Wesley, 2nd edition, Fig. 8.2)
  // https://doc.lagout.org/security/Hackers%20Delight.pdf
  private[this] def mulMant(x: Long, e10: Int): Long = {
    val y = pow10Mantissas(e10 + 343)
    val xl = x & 0xFFFFFFFFL
    val xh = x >>> 32
    val yl = y & 0xFFFFFFFFL
    val yh = y >>> 32
    val t = xh * yl + (xl * yl >>> 32)
    xh * yh + (t >>> 32) + (xl * yh + (t & 0xFFFFFFFFL) >>> 32)
  }

  private[this] def addExp(e2: Int, e10: Int): Int =
    (e10 * 14267572527L >> 32).toInt + e2 + 1 // (e10 * Math.log(10) / Math.log(2)).toInt + e2 + 1

  private[this] def parseBigInt(isToken: Boolean, default: BigInt, digitsLimit: Int): BigInt = {
    var b =
      if (isToken) nextToken(head)
      else nextByte(head)
    if (isToken && b == 'n') readNullOrNumberError(default, head)
    else {
      val isNeg = b == '-'
      if (isNeg) b = nextByte(head)
      if (b < '0' || b > '9') numberError()
      if (isToken && b == '0') {
        ensureNotLeadingZero()
        BigInt(0)
      } else {
        var pos = head
        var buf = this.buf
        var from = pos - 1
        val oldMark = mark
        val newMark =
          if (oldMark < 0) from
          else oldMark
        mark = newMark
        try {
          while ((pos < tail || {
            pos = loadMore(pos)
            buf = this.buf
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) pos += 1
          head = pos
          if ((b | 0x20) == 'e' || b == '.') numberError(pos)
          if (mark == 0) from -= newMark
          if (pos - from >= digitsLimit) digitsLimitError(from + digitsLimit - 1)
          new BigInt(toBigDecimal(buf, from, pos, isNeg, 0).unscaledValue)
        } finally if (mark != 0 || oldMark< 0) mark = oldMark
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
      val isNeg = b == '-'
      if (isNeg) b = nextByte(head)
      if (b < '0' || b > '9') numberError()
      var pos = head
      var buf = this.buf
      var from = pos - 1
      val oldMark = mark
      val newMark =
        if (oldMark < 0) from
        else oldMark
      mark = newMark
      try {
        var digits = 1
        if (isToken && b == '0') {
          if ((pos < tail || {
            pos = loadMore(pos)
            buf = this.buf
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) leadingZeroError(pos - 1)
        } else {
          digits -= pos
          while ((pos < tail || {
            digits += pos
            pos = loadMore(pos)
            digits -= pos
            buf = this.buf
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) pos += 1
          digits += pos
        }
        var fracLen, scale = 0
        if (digits >= digitsLimit) digitsLimitError(pos + digitsLimit - digits - 1)
        if (b == '.') {
          pos += 1
          fracLen -= pos
          while ((pos < tail || {
            fracLen += pos
            pos = loadMore(pos)
            fracLen -= pos
            buf = this.buf
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) pos += 1
          fracLen += pos
          digits += fracLen
          if (fracLen == 0) numberError(pos)
          if (digits >= digitsLimit) digitsLimitError(pos + digitsLimit - digits - 1)
        }
        if ((b | 0x20) == 'e') {
          b = nextByte(pos + 1)
          val isNegExp = b == '-'
          if (isNegExp || b == '+') b = nextByte(head)
          if (b < '0' || b > '9') numberError()
          var exp: Long = b - '0'
          pos = head
          buf = this.buf
          while ((pos < tail || {
            pos = loadMore(pos)
            buf = this.buf
            pos < tail
          }) && {
            b = buf(pos)
            b >= '0' && b <= '9'
          }) {
            exp = exp * 10 + (b - '0')
            if (exp > 2147483648L) numberError(pos)
            pos += 1
          }
          scale =
            if (isNegExp) exp.toInt
            else if (exp == 2147483648L) numberError(pos - 1)
            else -exp.toInt
        }
        head = pos
        if (mark == 0) from -= newMark
        var x =
          if (fracLen != 0) {
            val limit = from + digits + 1
            val fracPos = limit - fracLen
            val fracLimit = fracPos - 1
            if (digits < 19) {
              var x: Long = buf(from) - '0'
              from += 1
              while (from < fracLimit) {
                x = x * 10 + (buf(from) - '0')
                from += 1
              }
              from += 1
              while (from < limit) {
                x = x * 10 + (buf(from) - '0')
                from += 1
              }
              if (isNeg) x = -x
              java.math.BigDecimal.valueOf(x, scale + fracLen)
            } else toBigDecimal(buf, from, fracLimit, isNeg, scale)
              .add(toBigDecimal(buf, fracPos, limit, isNeg, scale + fracLen))
          } else toBigDecimal(buf, from, from + digits, isNeg, scale)
        if (digits > mc.getPrecision) x = x.plus(mc)
        if (Math.abs(x.scale) >= scaleLimit) scaleLimitError()
        new BigDecimal(x, mc)
      } finally if (mark != 0 || oldMark< 0) mark = oldMark
    }
  }

  // Based on a great idea of Eric Obermühlner to use a tree of smaller BigDecimals for parsing a really big numbers
  // with O(n^1.5) complexity instead of O(n^2) when using the constructor for a decimal representation from JDK 8/11:
  // https://github.com/eobermuhlner/big-math/commit/7a5419aac8b2adba2aa700ccf00197f97b2ad89f
  private[this] def toBigDecimal(buf: Array[Byte], offset: Int, limit: Int, isNeg: Boolean,
                                 scale: Int): java.math.BigDecimal = {
    val len = limit - offset
    if (len < 19) {
      var pos = offset
      var x: Long = buf(pos) - '0'
      pos += 1
      while (pos < limit) {
        x = x * 10 + (buf(pos) - '0')
        pos += 1
      }
      if (isNeg) x = -x
      java.math.BigDecimal.valueOf(x, scale)
    } else if (len < 37) toBigDecimal37(buf, offset, limit, isNeg, scale)
    else if (len < 280) toBigDecimal280(buf, offset, limit, isNeg, scale)
    else {
      val mid = len >> 1
      val midPos = limit - mid
      toBigDecimal(buf, offset, midPos, isNeg, scale - mid).add(toBigDecimal(buf, midPos, limit, isNeg, scale))
    }
  }

  private[this] def toBigDecimal37(buf: Array[Byte], offset: Int, limit: Int, isNeg: Boolean,
                                   scale: Int): java.math.BigDecimal = {
    val firstBlockLimit = limit - 18
    var pos = offset
    var x1: Long = buf(pos) - '0'
    pos += 1
    while (pos < firstBlockLimit) {
      x1 = x1 * 10 + (buf(pos) - '0')
      pos += 1
    }
    var x2 =
      (buf(pos) * 10 + buf(pos + 1)) * 10000000000000000L +
      (buf(pos + 2) * 10 + buf(pos + 3)) * 100000000000000L +
      (buf(pos + 4) * 10 + buf(pos + 5)) * 1000000000000L +
      (buf(pos + 6) * 10 + buf(pos + 7)) * 10000000000L +
      (buf(pos + 8) * 10 + buf(pos + 9)) * 100000000L +
      (buf(pos + 10) * 10 + buf(pos + 11)) * 1000000 +
      (buf(pos + 12) * 10 + buf(pos + 13)) * 10000 +
      (buf(pos + 14) * 10 + buf(pos + 15)) * 100 +
      buf(pos + 16) * 10 + buf(pos + 17) - 5333333333333333328L // 5333333333333333328L == '0' * 111111111111111111L
    if (isNeg) {
      x1 = -x1
      x2 = -x2
    }
    java.math.BigDecimal.valueOf(x1, scale - 18).add(java.math.BigDecimal.valueOf(x2, scale))
  }

  private[this] def toBigDecimal280(buf: Array[Byte], offset: Int, limit: Int, isNeg: Boolean,
                                    scale: Int): java.math.BigDecimal = {
    val len = limit - offset
    var x = 0L
    val firstBlockLimit = (len % 9) + offset
    var pos = offset
    while (pos < firstBlockLimit) {
      x = x * 10 + (buf(pos) - '0')
      pos += 1
    }
    val lastWord = ((len * 445861642L) >>> 32).toInt // (len * log(10) / log (1L << 32)).toInt
    val numWords = lastWord + 1
    val magWords = new Array[Int](numWords)
    magWords(lastWord) = x.toInt
    while (pos < limit) {
      x =
        (buf(pos) * 10 + buf(pos + 1)) * 10000000L +
        (buf(pos + 2) * 10 + buf(pos + 3)) * 100000 +
        (buf(pos + 4) * 10 + buf(pos + 5)) * 1000 +
        (buf(pos + 6) * 10 + buf(pos + 7)) * 10 +
        buf(pos + 8) - 5333333328L // 5333333328L == '0' * 111111111L
      var i = lastWord
      while (i >= 0) {
        val p = (magWords(i) & 0xFFFFFFFFL) * 1000000000 + x
        magWords(i) = p.toInt
        x = p >>> 32
        i -= 1
      }
      pos += 9
    }
    val magBytes = new Array[Byte](numWords << 2)
    var i = 0
    while (i < numWords) {
      val w = magWords(i)
      val j = i << 2
      magBytes(j) = (w >> 24).toByte
      magBytes(j + 1) = (w >> 16).toByte
      magBytes(j + 2) = (w >> 8).toByte
      magBytes(j + 3) = w.toByte
      i += 1
    }
    val signum =
      if (isNeg) -1
      else 1
    new java.math.BigDecimal(new java.math.BigInteger(signum, magBytes), scale)
  }

  private[this] def readNullOrNumberError[@sp A](default: A, pos: Int): A =
    if (default != null) parseNullOrError(default, "expected number or null", pos)
    else numberError(pos - 1)

  private[this] def numberError(pos: Int = head - 1): Nothing = decodeError("illegal number", pos)

  private[this] def digitsLimitError(pos: Int): Nothing = decodeError("value exceeds limit for number of digits", pos)

  private[this] def scaleLimitError(pos: Int = head - 1): Nothing = decodeError("value exceeds limit for scale", pos)

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
    if (b != 'P') durationOrPeriodStartError(isNeg)
    b = nextByte(head)
    while ({
      if (state == 0) {
        if (b == 'T') {
          b = nextByte(head)
          state = 1
        }
      } else if (state == 1) {
        if (b != 'T') tokensError('T', '"')
        b = nextByte(head)
      } else if (state == 4) tokenError('"')
      val isNegX = b == '-'
      if (isNegX) b = nextByte(head)
      if (b < '0' || b > '9') durationOrPeriodDigitError(isNegX, state <= 1)
      var x: Long = '0' - b
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        if (x < -922337203685477580L || {
          x = x * 10 + ('0' - b)
          x > 0
        }) durationError(pos)
        pos += 1
      }
      if (!(isNeg ^ isNegX)) {
        if (x == -9223372036854775808L) durationError(pos)
        x = -x
      }
      if (b == 'D' && state <= 0) {
        if (x < -106751991167300L || x > 106751991167300L) durationError(pos) // -106751991167300L == Long.MinValue / 86400
        seconds = x * 86400
        state = 1
      } else if (b == 'H' && state <= 1) {
        if (x < -2562047788015215L || x > 2562047788015215L) durationError(pos) // -2562047788015215L == Long.MinValue / 3600
        seconds = sumSeconds(x * 3600, seconds, pos)
        state = 2
      } else if (b == 'M' && state <= 2) {
        if (x < -153722867280912930L || x > 153722867280912930L) durationError(pos) // -153722867280912930L == Long.MinValue / 60
        seconds = sumSeconds(x * 60, seconds, pos)
        state = 3
      } else if (b == '.') {
        pos += 1
        seconds = sumSeconds(x, seconds, pos)
        var nanoDigitWeight = 100000000
        while ({
          if (pos >= tail) {
            pos = loadMoreOrError(pos)
            buf = this.buf
          }
          b = buf(pos)
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nanos += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
          pos += 1
        }
        if (b != 'S') {
          head = pos + 1
          nanoError(nanoDigitWeight, 'S')
        }
        if (isNeg ^ isNegX) nanos = -nanos
        state = 4
      } else if (b == 'S') {
        seconds = sumSeconds(x, seconds, pos)
        state = 4
      } else durationError(state, pos)
      b = nextByte(pos + 1)
      b != '"'
    }) ()
    Duration.ofSeconds(seconds, nanos)
  }

  private[this] def sumSeconds(s1: Long, s2: Long, pos: Int): Long = {
    val s = s1 + s2
    if (((s1 ^ s) & (s2 ^ s)) < 0) durationError(pos)
    s
  }

  private[this] def parseInstant(): Instant = {
    val year = parseYearWithByte('-', 10, head)
    val month = parseMonthWithByte('-', head)
    val day = parseDayWithByte(year, month, 'T', head)
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    val b = nextByte(head)
    if (b == ':') {
      second = parseSecond(head)
      nano = parseOptionalNanoWithByte('Z')
    } else if (b != 'Z') tokensError(':', 'Z')
    nextByteOrError('"', head)
    val epochDay = epochDayForYear(year) + (dayOfYearForYearMonth(year, month) + day - 719529) // 719528 == days 0000 to 1970
    val secondOfDay = hour * 3600 + minute * 60 + second
    Instant.ofEpochSecond(epochDay * 86400 + secondOfDay, nano) // 86400 == seconds per day
  }

  private[this] def parseLocalDate(): LocalDate = {
    val year = parseYearWithByte('-', 9, head)
    val month = parseMonthWithByte('-', head)
    val day = parseDayWithByte(year, month, '"', head)
    LocalDate.of(year, month, day)
  }

  private[this] def parseLocalDateTime(): LocalDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = parseMonthWithByte('-', head)
    val day = parseDayWithByte(year, month, 'T', head)
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    val b = nextByte(head)
    if (b == ':') {
      second = parseSecond(head)
      nano = parseOptionalNanoWithByte('"')
    } else if (b != '"') tokensError(':', '"')
    LocalDateTime.of(year, month, day, hour, minute, second, nano)
  }

  private[this] def parseLocalTime(): LocalTime = {
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    val b = nextByte(head)
    if (b == ':') {
      second = parseSecond(head)
      nano = parseOptionalNanoWithByte('"')
    } else if (b != '"') tokensError(':', '"')
    LocalTime.of(hour, minute, second, nano)
  }

  @tailrec
  private[this] def parseMonthDay(pos: Int): MonthDay =
    if (pos + 7 < tail) {
      val buf = this.buf
      val b1 = buf(pos)
      val b2 = buf(pos + 1)
      val b3 = buf(pos + 2)
      val b4 = buf(pos + 3)
      val b5 = buf(pos + 4)
      val b6 = buf(pos + 5)
      val b7 = buf(pos + 6)
      val b8 = buf(pos + 7)
      val month = b3 * 10 + b4 - 528 // 528 == '0' * 11
      val day = b6 * 10 + b7 - 528 // 528 == '0' * 11
      head = pos + 8
      if (b1 != '-') tokenError('-', pos)
      if (b2 != '-') tokenError('-', pos + 1)
      if (b3 < '0' || b3 > '9') digitError(pos + 2)
      if (b4 < '0' || b4 > '9') digitError(pos + 3)
      if (month < 1 || month > 12) monthError(pos + 3)
      if (b5 != '-') tokenError('-', pos + 4)
      if (b6 < '0' || b6 > '9') digitError(pos + 5)
      if (b7 < '0' || b7 > '9') digitError(pos + 6)
      if (day == 0 || (day > 28 && day > maxDayForMonth(month))) dayError(pos + 6)
      if (b8 != '"') tokenError('"', pos + 7)
      MonthDay.of(month, day)
    } else parseMonthDay(loadMoreOrError(pos))

  private[this] def parseOffsetDateTime(): OffsetDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = parseMonthWithByte('-', head)
    val day = parseDayWithByte(year, month, 'T', head)
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    var nanoDigitWeight = -1
    var b = nextByte(head)
    if (b == ':') {
      nanoDigitWeight = -2
      second = parseSecond(head)
      b = nextByte(head)
      if (b == '.') {
        nanoDigitWeight = 100000000
        var pos = head
        var buf = this.buf
        while ({
          if (pos >= tail) {
            pos = loadMoreOrError(pos)
            buf = this.buf
          }
          b = buf(pos)
          pos += 1
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
        }
        head = pos
      }
    }
    val zoneOffset =
      if (b == 'Z') {
        nextByteOrError('"', head)
        ZoneOffset.UTC
      } else {
        val offsetNeg = b == '-' || (b != '+' && timeError(nanoDigitWeight))
        val offsetHour = parseOffsetHour(head)
        var offsetMinute, offsetSecond = 0
        b = nextByte(head)
        if (b == ':' && {
          offsetMinute = parseOffsetMinute(head)
          b = nextByte(head)
          b == ':'
        }) offsetSecond = parseOffsetSecondWithByte('"', head)
        else if (b != '"') tokensError(':', '"')
        toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      }
    OffsetDateTime.of(year, month, day, hour, minute, second, nano, zoneOffset)
  }

  private[this] def parseOffsetTime(): OffsetTime = {
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    var nanoDigitWeight = -1
    var b = nextByte(head)
    if (b == ':') {
      nanoDigitWeight = -2
      second = parseSecond(head)
      b = nextByte(head)
      if (b == '.') {
        nanoDigitWeight = 100000000
        var pos = head
        var buf = this.buf
        while ({
          if (pos >= tail) {
            pos = loadMoreOrError(pos)
            buf = this.buf
          }
          b = buf(pos)
          pos += 1
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
        }
        head = pos
      }
    }
    val zoneOffset =
      if (b == 'Z') {
        nextByteOrError('"', head)
        ZoneOffset.UTC
      } else {
        val offsetNeg = b == '-' || (b != '+' && timeError(nanoDigitWeight))
        val offsetHour = parseOffsetHour(head)
        var offsetMinute, offsetSecond = 0
        b = nextByte(head)
        if (b == ':' && {
          offsetMinute = parseOffsetMinute(head)
          b = nextByte(head)
          b == ':'
        }) offsetSecond = parseOffsetSecondWithByte('"', head)
        else if (b != '"') tokensError(':', '"')
        toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      }
    OffsetTime.of(hour, minute, second, nano, zoneOffset)
  }

  private[this] def parsePeriod(): Period = {
    var years, months, days, state = 0
    var b = nextByte(head)
    val isNeg = b == '-'
    if (isNeg) b = nextByte(head)
    if (b != 'P') durationOrPeriodStartError(isNeg)
    b = nextByte(head)
    while ({
      if (state == 4) tokenError('"')
      val isNegX = b == '-'
      if (isNegX) b = nextByte(head)
      if (b < '0' || b > '9') durationOrPeriodDigitError(isNegX, state <= 0)
      var x: Long = b - '0'
      var pos = head
      var buf = this.buf
      while ((pos < tail || {
        pos = loadMore(pos)
        buf = this.buf
        pos < tail
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) {
        x = x * 10 + (b - '0')
        if (x > 2147483648L) periodError(pos)
        pos += 1
      }
      if (isNeg ^ isNegX) x = -x
      else if (x == 2147483648L) periodError(pos)
      if (b == 'Y' && state <= 0) {
        years = x.toInt
        state = 1
      } else if (b == 'M' && state <= 1) {
        months = x.toInt
        state = 2
      } else if (b == 'W' && state <= 2) {
        val ds = x * 7
        if (ds != ds.toInt) periodError(pos)
        days = ds.toInt
        state = 3
      } else if (b == 'D') {
        val ds = x + days
        if (ds != ds.toInt) periodError(pos)
        days = ds.toInt
        state = 4
      } else periodError(state, pos)
      b = nextByte(pos + 1)
      b != '"'
    }) ()
    Period.of(years, months, days)
  }

  private[this] def parseYearMonth(): YearMonth =
    YearMonth.of(parseYearWithByte('-', 9, head), parseMonthWithByte('"', head))

  private[this] def parseZonedDateTime(): ZonedDateTime = {
    val year = parseYearWithByte('-', 9, head)
    val month = parseMonthWithByte('-', head)
    val day = parseDayWithByte(year, month, 'T', head)
    val hour = parseHourWithByte(':', head)
    val minute = parseMinute(head)
    var second, nano = 0
    var nanoDigitWeight = -1
    var b = nextByte(head)
    if (b == ':') {
      nanoDigitWeight = -2
      second = parseSecond(head)
      b = nextByte(head)
      if (b == '.') {
        nanoDigitWeight = 100000000
        var pos = head
        var buf = this.buf
        while ({
          if (pos >= tail) {
            pos = loadMoreOrError(pos)
            buf = this.buf
          }
          b = buf(pos)
          pos += 1
          b >= '0' && b <= '9' && nanoDigitWeight != 0
        }) {
          nano += (b - '0') * nanoDigitWeight
          nanoDigitWeight = (nanoDigitWeight * 3435973837L >> 35).toInt // divide a positive int by 10
        }
        head = pos
      }
    }
    val localDateTime = LocalDateTime.of(year, month, day, hour, minute, second, nano)
    val zoneOffset =
      if (b == 'Z') {
        b = nextByte(head)
        ZoneOffset.UTC
      } else {
        val offsetNeg = b == '-' || (b != '+' && timeError(nanoDigitWeight))
        nanoDigitWeight = -3
        val offsetHour = parseOffsetHour(head)
        var offsetMinute, offsetSecond = 0
        b = nextByte(head)
        if (b == ':') {
          offsetMinute = parseOffsetMinute(head)
          b = nextByte(head)
          if (b == ':') {
            nanoDigitWeight = -4
            offsetSecond = parseOffsetSecond(head)
            b = nextByte(head)
          }
        }
        toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
      }
    if (b == '"') ZonedDateTime.ofLocal(localDateTime, zoneOffset, null)
    else if (b == '[') {
      val zone = parseZoneIdWithByte(']')
      nextByteOrError('"', head)
      ZonedDateTime.ofInstant(localDateTime, zoneOffset, zone)
    } else zonedDateTimeError(nanoDigitWeight)
  }

  private[this] def parseZoneOffset(): ZoneOffset = {
    var b = nextByte(head)
    if (b == 'Z') {
      nextByteOrError('"', head)
      ZoneOffset.UTC
    } else {
      val offsetNeg = b == '-' || (b != '+' && decodeError("expected '+' or '-' or 'Z'"))
      val offsetHour = parseOffsetHour(head)
      var offsetMinute, offsetSecond = 0
      b = nextByte(head)
      if (b == ':' && {
        offsetMinute = parseOffsetMinute(head)
        b = nextByte(head)
        b == ':'
      }) offsetSecond = parseOffsetSecondWithByte('"', head)
      else if (b != '"') tokensError(':', '"')
      toZoneOffset(offsetNeg, offsetHour, offsetMinute, offsetSecond)
    }
  }

  private[this] def toZoneOffset(isNeg: Boolean, offsetHour: Int, offsetMinute: Int, offsetSecond: Int): ZoneOffset = {
    var offsetTotal = offsetHour * 3600 + offsetMinute * 60 + offsetSecond
    var q1 = (offsetTotal * 2443359173L >> 41).toInt // divide a positive int by 900
    if (offsetTotal > 64800) timezoneOffsetError() // 64800 == 18 * 60 * 60
    if (q1 * 900 == offsetTotal) {
      if (isNeg) q1 = -q1
      var zoneOffset = zoneOffsets(q1 + 72)
      if (zoneOffset ne null) zoneOffset
      else {
        if (isNeg) offsetTotal = -offsetTotal
        zoneOffset = ZoneOffset.ofTotalSeconds(offsetTotal)
        zoneOffsets(q1 + 72) = zoneOffset
        zoneOffset
      }
    } else {
      if (isNeg) offsetTotal = -offsetTotal
      ZoneOffset.ofTotalSeconds(offsetTotal)
    }
  }

  private[this] def epochDayForYear(year: Int): Long =
    year * 365L + (((year + 3) >> 2) - {
      if (year < 0) {
        val century = year * 1374389535L >> 37 // divide a positive int by 100
        century - (century >> 2)
      } else ((year + 99) * 1374389535L >> 37) - ((year + 399) * 1374389535L >> 39) // divide a positive int by 100 and by 400 accordingly
    })

  private[this] def dayOfYearForYearMonth(year: Int, month: Int): Int =
    ((month * 1050835331877L - 1036518774222L) >> 35).toInt - // (month * 367 - 362) / 12
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
    val century = year / 100
    century * 100 != year || (century & 0x3) == 0
  }

  private[this] def digitError(pos: Int): Nothing = decodeError("expected digit", pos)

  private[this] def periodError(pos: Int): Nothing = decodeError("illegal period", pos)

  private[this] def periodError(state: Int, pos: Int): Nothing = decodeError((state: @switch) match {
    case 0 => "expected 'Y' or 'M' or 'W' or 'D' or digit"
    case 1 => "expected 'M' or 'W' or 'D' or digit"
    case 2 => "expected 'W' or 'D' or digit"
    case 3 => "expected 'D' or digit"
  }, pos)

  private[this] def durationOrPeriodStartError(isNeg: Boolean): Nothing = decodeError {
    if (isNeg) "expected 'P'"
    else "expected 'P' or '-'"
  }

  private[this] def durationOrPeriodDigitError(isNegX: Boolean, isNumReq: Boolean): Nothing = decodeError {
    if (isNegX) "expected digit"
    else if (isNumReq) "expected '-' or digit"
    else "expected '\"' or '-' or digit"
  }

  private[this] def durationError(pos: Int): Nothing = decodeError("illegal duration", pos)

  private[this] def durationError(state: Int, pos: Int): Nothing = decodeError((state: @switch) match {
    case 0 => "expected 'D' or digit"
    case 1 => "expected 'H' or 'M' or 'S or '.' or digit"
    case 2 => "expected 'M' or 'S or '.' or digit"
    case 3 => "expected 'S or '.' or digit"
  }, pos)

  private[this] def yearError(t: Byte, maxDigits: Int, pos: Int, yearNeg: Boolean, yearDigits: Int): Nothing = {
    if (!yearNeg && yearDigits == 4) digitError(pos)
    if (yearDigits == maxDigits) tokenError(t, pos)
    tokenOrDigitError(t, pos)
  }

  private[this] def yearError(pos: Int): Nothing = decodeError("illegal year", pos)

  private[this] def monthError(pos: Int): Nothing = decodeError("illegal month", pos)

  private[this] def dayError(pos: Int): Nothing = decodeError("illegal day", pos)

  private[this] def hourError(pos: Int): Nothing = decodeError("illegal hour", pos)

  private[this] def minuteError(pos: Int): Nothing = decodeError("illegal minute", pos)

  private[this] def secondError(pos: Int): Nothing = decodeError("illegal second", pos)

  private[this] def nanoError(nanoDigitWeight: Int, t: Byte): Nothing = {
    if (nanoDigitWeight == 0) tokenError(t)
    tokenOrDigitError(t)
  }

  private[this] def timeError(nanoDigitWeight: Int): Nothing = decodeError {
    if (nanoDigitWeight == -2) "expected '.' or '+' or '-' or 'Z'"
    else if (nanoDigitWeight == -1) "expected ':' or '+' or '-' or 'Z'"
    else if (nanoDigitWeight == 0) "expected '+' or '-' or 'Z'"
    else "expected '+' or '-' or 'Z' or digit"
  }

  private[this] def timezoneError(ex: DateTimeException): Nothing = decodeError("illegal timezone", head - 1, ex)

  private[this] def timezoneOffsetError(): Nothing = decodeError("illegal timezone offset")

  private[this] def timezoneOffsetHourError(pos: Int): Nothing = decodeError("illegal timezone offset hour", pos)

  private[this] def timezoneOffsetMinuteError(pos: Int): Nothing = decodeError("illegal timezone offset minute", pos)

  private[this] def timezoneOffsetSecondError(pos: Int): Nothing = decodeError("illegal timezone offset second", pos)

  private[this] def zonedDateTimeError(nanoDigitWeight: Int): Nothing = decodeError {
    if (nanoDigitWeight > -3) "expected '[' or '\"'"
    else "expected ':' or '[' or '\"'"
  }

  @tailrec
  private[this] def parseUUID(pos: Int): UUID =
    if (pos + 36 < tail) {
      val ns = nibbles
      val buf = this.buf
      val mostSigBits1: Long =
        (ns(buf(pos) & 0xFF).toLong << 28) |
        ((ns(buf(pos + 1) & 0xFF) << 24) |
        (ns(buf(pos + 2) & 0xFF) << 20) |
        (ns(buf(pos + 3) & 0xFF) << 16) |
        (ns(buf(pos + 4) & 0xFF) << 12) |
        (ns(buf(pos + 5) & 0xFF) << 8) |
        (ns(buf(pos + 6) & 0xFF) << 4) |
        ns(buf(pos + 7) & 0xFF))
      if (mostSigBits1 < 0) hexDigitError(pos)
      if (buf(pos + 8) != '-') tokenError('-', pos + 8)
      val mostSigBits2: Int =
        (ns(buf(pos + 9) & 0xFF) << 12) |
        (ns(buf(pos + 10) & 0xFF) << 8) |
        (ns(buf(pos + 11) & 0xFF) << 4) |
        ns(buf(pos + 12) & 0xFF)
      if (mostSigBits2 < 0) hexDigitError(pos + 9)
      if (buf(pos + 13) != '-') tokenError('-', pos + 13)
      val mostSigBits3: Int =
        (ns(buf(pos + 14) & 0xFF) << 12) |
        (ns(buf(pos + 15) & 0xFF) << 8) |
        (ns(buf(pos + 16) & 0xFF) << 4) |
        ns(buf(pos + 17) & 0xFF)
      if (mostSigBits3 < 0) hexDigitError(pos + 14)
      if (buf(pos + 18) != '-') tokenError('-', pos + 18)
      val leastSigBits1: Int =
        (ns(buf(pos + 19) & 0xFF) << 12) |
        (ns(buf(pos + 20) & 0xFF) << 8) |
        (ns(buf(pos + 21) & 0xFF) << 4) |
        ns(buf(pos + 22) & 0xFF)
      if (leastSigBits1 < 0) hexDigitError(pos + 19)
      if (buf(pos + 23) != '-') tokenError('-', pos + 23)
      val leastSigBits2: Long =
        ((ns(buf(pos + 24) & 0xFF) << 16) |
        (ns(buf(pos + 25) & 0xFF) << 12) |
        (ns(buf(pos + 26) & 0xFF) << 8) |
        (ns(buf(pos + 27) & 0xFF) << 4) |
        ns(buf(pos + 28) & 0xFF)).toLong << 28 |
        ((ns(buf(pos + 29) & 0xFF) << 24) |
        (ns(buf(pos + 30) & 0xFF) << 20) |
        (ns(buf(pos + 31) & 0xFF) << 16) |
        (ns(buf(pos + 32) & 0xFF) << 12) |
        (ns(buf(pos + 33) & 0xFF) << 8) |
        (ns(buf(pos + 34) & 0xFF) << 4) |
        ns(buf(pos + 35) & 0xFF))
      if (leastSigBits2 < 0) hexDigitError(pos + 24)
      if (buf(pos + 36) != '"') tokenError('"', pos + 36)
      head = pos + 37
      new UUID((mostSigBits1 << 32) | (mostSigBits2.toLong << 16) | mostSigBits3,
        (leastSigBits1.toLong << 48) | leastSigBits2)
    } else parseUUID(loadMoreOrError(pos))

  private[this] def parseString(): Int = {
    val minLim = Math.min(charBuf.length, tail - head)
    parseString(0, minLim, charBuf, head)
  }

  @tailrec
  private[this] def parseString(i: Int, minLim: Int, charBuf: Array[Char], pos: Int): Int =
    if (i < minLim) {
      val b = buf(pos)
      charBuf(i) = b.toChar
      if (b == '"') {
        head = pos + 1
        i
      } else if (((b - 32) ^ 60) <= 0) parseEncodedString(i, charBuf.length - 1, charBuf, pos)
      else parseString(i + 1, minLim, charBuf, pos + 1)
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
            if (b2 != 'u') {
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
            } else if (remaining > 5) {
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
          if (b2 != 'u') {
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
          } else if (remaining > 5) {
            val ch = readEscapedUnicode(pos + 2, buf)
            if (ch >= 0xD800 && ch <= 0xDFFF) decodeError("illegal surrogate character", pos + 5)
            head = pos + 6
            ch
          } else parseChar(loadMoreOrError(pos))
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

  private[this] def parseBase16(ns: Array[Byte]): Array[Byte] = {
    var charBuf = this.charBuf
    var len = charBuf.length
    var pos = head
    var buf = this.buf
    var i, bits = 0
    while (bits >= 0 && (pos + 3 < tail || {
      pos = loadMore(pos)
      buf = this.buf
      pos + 3 < tail
    })) {
      if (i >= len) {
        len = growCharBuf(i + 1)
        charBuf = this.charBuf
      }
      val posLim = Math.min(tail - 3, ((len - i) << 2) + pos)
      while (pos < posLim && {
        bits =
          ns(buf(pos) & 0xFF) << 12 |
          ns(buf(pos + 1) & 0xFF) << 8 |
          ns(buf(pos + 2) & 0xFF) << 4 |
          ns(buf(pos + 3) & 0xFF)
        bits >= 0
      }) {
        charBuf(i) = bits.toChar
        i += 1
        pos += 4
      }
    }
    val bLen = i << 1
    val bs = {
      var b = nextByte(pos)
      if (b != '"') {
        bits = ns(b & 0xFF)
        if (bits < 0) decodeError("expected '\"' or hex digit")
        b = nextByte(head)
        bits = (bits << 4) | ns(b & 0xFF)
        if (bits < 0) decodeError("expected hex digit")
        b = nextByte(head)
        if (b != '"') {
          if (ns(b & 0xFF) < 0) decodeError("expected '\"' or hex digit")
          nextByte(head)
          decodeError("expected hex digit")
        }
        val bs = new Array[Byte](bLen + 1)
        bs(bLen) = bits.toByte
        bs
      } else new Array[Byte](bLen)
    }
    i = 0
    var j = 0
    while (j < bLen) {
      val ch = charBuf(i)
      bs(j) = (ch >> 8).toByte
      bs(j + 1) = ch.toByte
      i += 1
      j += 2
    }
    bs
  }

  private[this] def parseBase64(ds: Array[Byte]): Array[Byte] = {
    var charBuf = this.charBuf
    var lenM1 = charBuf.length - 1
    var pos = head
    var buf = this.buf
    var i, bits = 0
    while (bits >= 0 && (pos + 3 < tail || {
      pos = loadMore(pos)
      buf = this.buf
      pos + 3 < tail
    })) {
      if (i >= lenM1) {
        lenM1 = growCharBuf(i + 1) - 1
        charBuf = this.charBuf
      }
      val posLim = Math.min(tail - 3, ((lenM1 - i) << 1) + pos)
      while (pos < posLim && {
        bits =
          ds(buf(pos) & 0xFF) << 18 |
          ds(buf(pos + 1) & 0xFF) << 12 |
          ds(buf(pos + 2) & 0xFF) << 6 |
          ds(buf(pos + 3) & 0xFF)
        bits >= 0
      }) {
        charBuf(i) = (bits >> 8).toChar
        charBuf(i + 1) = bits.toChar
        i += 2
        pos += 4
      }
    }
    val bLen = i + (i >> 1)
    val bs = {
      var b = nextByte(pos)
      if (b != '"') {
        bits = ds(b & 0xFF)
        if (bits < 0) decodeError("expected '\"' or base64 digit")
        b = nextByte(head)
        bits = (bits << 6) | ds(b & 0xFF)
        if (bits < 0) decodeError("expected base64 digit")
        b = nextByte(head)
        if (b == '"' || b == '=') {
          if (b == '=') {
            nextByteOrError('=', head)
            nextByteOrError('"', head)
          }
          val bs = new Array[Byte](bLen + 1)
          bs(bLen) = (bits >> 4).toByte
          bs
        } else {
          bits = (bits << 6) | ds(b & 0xFF)
          if (bits < 0) decodeError("expected '\"' or '=' or base64 digit")
          b = nextByte(head)
          if (b == '=') nextByteOrError('"', head)
          else if (b != '"') tokensError('"', '=')
          val bs = new Array[Byte](bLen + 2)
          bs(bLen) = (bits >> 10).toByte
          bs(bLen + 1) = (bits >> 2).toByte
          bs
        }
      } else new Array[Byte](bLen)
    }
    i = 0
    var j = 0
    while (j < bLen) {
      val ch = charBuf(i)
      bs(j) = (ch >> 8).toByte
      bs(j + 1) = ch.toByte
      bs(j + 2) = charBuf(i + 1).toByte
      i += 2
      j += 3
    }
    bs
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

  private[this] def appendHexDump(pos: Int, offset: Int, from: Int): Int = {
    val start = Math.max((pos - 32) & 0xFFFFFFF0, 0)
    val end = Math.min((pos + 48) & 0xFFFFFFF0, tail)
    val alignedAbsFrom = (start + offset) & 0xFFFFFFF0
    val alignedAbsTo = (end + offset + 15) & 0xFFFFFFF0
    val len = alignedAbsTo - alignedAbsFrom
    val bufOffset = alignedAbsFrom - offset
    var i = appendChars(dumpBorder, from)
    i = appendChars(dumpHeader, i)
    i = appendChars(dumpBorder, i)
    val buf = this.buf
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

  private[this] def appendHexOffset(d: Long, i: Int): Int = {
    if (i + 16 >= charBuf.length) growCharBuf(i + 16)
    val ds = hexDigits
    var j = i
    if (d.toInt != d) {
      var shift = (64 - java.lang.Long.numberOfLeadingZeros(d)) & 0x3C
      while (shift >= 32) {
        charBuf(j) = ds((d >>> shift).toInt & 0xF)
        shift -= 4
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
    val newLim = (-1 >>> Integer.numberOfLeadingZeros(charBuf.length | required)) + 1
    charBuf = java.util.Arrays.copyOf(charBuf, newLim)
    newLim
  }

  @tailrec
  private[this] def skipString(evenBackSlashes: Boolean, pos: Int): Int =
    if (pos < tail) {
      if (evenBackSlashes) {
        val b = buf(pos)
        if (b == '"') pos + 1
        else skipString(b != '\\', pos + 1)
      } else skipString(evenBackSlashes = true, pos + 1)
    } else skipString(evenBackSlashes, loadMoreOrError(pos))

  private[this] def skipNumber(p: Int): Int = {
    var pos = p
    var buf = this.buf
    while ((pos < tail || {
      pos = loadMore(pos)
      buf = this.buf
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
      else if (b == '{') skipObject(level + 1, pos + 1)
      else if (b != '}') skipObject(level, pos + 1)
      else if (level != 0) skipObject(level - 1, pos + 1)
      else pos + 1
    } else skipObject(level, loadMoreOrError(pos))

  @tailrec
  private[this] def skipArray(level: Int, pos: Int): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') skipArray(level, skipString(evenBackSlashes = true, pos + 1))
      else if (b == '[') skipArray(level + 1, pos + 1)
      else if (b != ']') skipArray(level, pos + 1)
      else if (level != 0) skipArray(level - 1, pos + 1)
      else pos + 1
    } else skipArray(level, loadMoreOrError(pos))

  @tailrec
  private[this] def skipFixedBytes(n: Int, pos: Int): Int = {
    val newPos = pos + n
    if (newPos <= tail) newPos
    else skipFixedBytes(n, loadMoreOrError(pos))
  }

  private[this] def loadMoreOrError(pos: Int): Int = {
    if ((bbuf eq null) && (in eq null)) endOfInputError()
    loadMore(pos, throwOnEndOfInput = true)
  }

  private[this] def loadMore(pos: Int): Int =
    if ((bbuf eq null) && (in eq null)) pos
    else loadMore(pos, throwOnEndOfInput = false)

  private[this] def loadMore(pos: Int, throwOnEndOfInput: Boolean): Int = {
    var newPos = pos
    val offset =
      if (mark < 0) pos
      else mark
    if (offset > 0) {
      newPos -= offset
      val remaining = tail - offset
      var i = 0
      while (i < remaining) {
        buf(i) = buf(i + offset)
        i += 1
      }
      if (mark > 0) mark = 0
      tail = remaining
      head = newPos
    } else buf = java.util.Arrays.copyOf(buf, buf.length << 1)
    var len = buf.length - tail
    if (bbuf ne null) {
      len = Math.min(bbuf.remaining, len)
      bbuf.get(buf, tail, len)
    } else len = Math.max(in.read(buf, tail, len), 0)
    if (throwOnEndOfInput && len == 0) endOfInputError()
    tail += len
    totalRead += len
    newPos
  }

  private[this] def endOfInputError(): Nothing = decodeError("unexpected end of input", tail)

  private[this] def reallocateBufToPreferredSize(): Unit = buf = new Array[Byte](config.preferredBufSize)

  private[this] def reallocateCharBufToPreferredSize(): Unit = charBuf = new Array[Char](config.preferredCharBufSize)
}

object JsonReader {
  private final val pow10Doubles: Array[Double] =
    Array(1, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11,
      1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22)
  private final val pow10Mantissas: Array[Long] = eval {
    val ms = new Array[Long](653)
    var pow10 = BigInt(10)
    var i = 342
    while (i >= 0) {
      ms(i) = ((BigInt(1) << (pow10.bitLength + 63)) / pow10).longValue
      pow10 *= 10
      i -= 1
    }
    pow10 = BigInt(1) << 63
    i = 343
    while (i < 653) {
      ms(i) = (pow10 >> (pow10.bitLength - 64)).longValue
      pow10 *= 10
      i += 1
    }
    ms
  }
  private final val nibbles: Array[Byte] = eval {
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
  private final val base64Bytes: Array[Byte] = eval {
    val bs = new Array[Byte](256)
    java.util.Arrays.fill(bs, -1: Byte)
    val ds = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    var i = 0
    while (i < ds.length) {
      bs(ds.charAt(i).toInt) = i.toByte
      i += 1
    }
    bs
  }
  private final val base64UrlBytes: Array[Byte] = eval {
    val bs = new Array[Byte](256)
    java.util.Arrays.fill(bs, -1: Byte)
    val ds = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    var i = 0
    while (i < ds.length) {
      bs(ds.charAt(i).toInt) = i.toByte
      i += 1
    }
    bs
  }
  private final val zoneOffsets: Array[ZoneOffset] = new Array(145)
  private final val zoneIds: ConcurrentHashMap[Key, ZoneId] = new ConcurrentHashMap(256)
  private final val hexDigits: Array[Char] =
    Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  private final val dumpBorder: Array[Char] =
    eval("\n+----------+-------------------------------------------------+------------------+".toCharArray)
  private final val dumpHeader: Array[Char] =
    eval("\n|          |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f | 0123456789abcdef |".toCharArray)
  final val bigDecimalMathContext: MathContext = MathContext.DECIMAL128
  final val bigDecimalDigitsLimit: Int = 308
  final val bigDecimalScaleLimit: Int = 6178
  final val bigIntDigitsLimit: Int = 308

  /**
    * Calculates hash code value string represented by sequence of characters from beginning of the provided char array
    * up to limit position.
    *
    * @param cs a char array
    * @param len an exclusive limit
    * @return a hash code value
    * @throws NullPointerException if the `cs` is null
    * @throws ArrayIndexOutOfBoundsException if the length of `cs` is less than the provided `len`
    */
  final def toHashCode(cs: Array[Char], len: Int): Int = {
    if (cs eq null) throw new NullPointerException
    if (cs.length < len) throw new ArrayIndexOutOfBoundsException(len)
    var h, i = 0
    while (i < len) {
      h = (h << 5) + (cs(i) - h)
      i += 1
    }
    h
  }
}

private class Key(var hash: Int, var bs: Array[Byte], var from: Int, var to: Int) {
  def copy: Key = {
    val len = to - from
    val bs1 = new Array[Byte](len)
    System.arraycopy(bs, from, bs1, 0, len)
    new Key(hash, bs1, 0, len)
  }

  override def hashCode: Int = hash

  override def equals(obj: Any): Boolean = {
    val k = obj.asInstanceOf[Key]
    val koff = k.from
    val len = to - from
    k.to - koff == len && {
      val kbs = k.bs
      val off = from
      var i = 0
      while (i < len && kbs(i + koff) == bs(i + off)) i += 1
      i == len
    }
  }

  override def toString: String = new String(bs, from, to - from)
}