package com.github.plokhotnyuk.jsoniter_scala

import java.io.InputStream

import com.github.plokhotnyuk.jsoniter_scala.JsonReader._

import scala.annotation.{switch, tailrec}
import scala.util.control.NonFatal

class JsonParseException(msg: String, cause: Throwable, withStackTrace: Boolean)
  extends RuntimeException(msg, cause, true, withStackTrace)

// Use an option of throwing stack-less exception for cases when parse exceptions can be not exceptional,
// see more details here: https://shipilev.net/blog/2014/exceptional-performance/
case class ReaderConfig(
    throwParseExceptionWithStackTrace: Boolean = true,
    appendHexDumpToParseException: Boolean = true)

final class JsonReader private[jsoniter_scala](
    private var buf: Array[Byte] = new Array[Byte](4096),
    private var head: Int = 0,
    private var tail: Int = 0,
    private var charBuf: Array[Char] = new Array[Char](4096),
    private var in: InputStream = null,
    private var totalRead: Int = 0,
    private var config: ReaderConfig = ReaderConfig()) {
  def reqFieldError(reqFields: Array[String], reqs: Int*): Nothing = {
    val len = reqFields.length
    var i = 0
    var j = 0
    while (j < len) {
      if ((reqs(j >> 5) & (1 << j)) != 0) {
        i = appendString(if (i == 0) "missing required field(s) \"" else "\", \"", i)
        i = appendString(reqFields(j), i)
      }
      j += 1
    }
    i = appendString("\"", i)
    decodeError(i, head - 1, null)
  }

  def unexpectedFieldError(len: Int): Nothing = {
    var i = prependString("unexpected field: \"", len)
    i = appendString("\"", i)
    decodeError(i, head - 1, null)
  }

  def readObjectFieldAsString(): String = {
    readParentheses()
    val len = parseString()
    readColon()
    new String(charBuf, 0, len)
  }

  def readObjectFieldAsBoolean(): Boolean = {
    readParentheses()
    val x = parseBoolean(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsByte(): Byte = {
    readParentheses()
    val x = parseInt(isToken = false)
    if (x > Byte.MaxValue || x < Byte.MinValue) decodeError("value is too large for byte")
    readParenthesesWithColon()
    x.toByte
  }

  def readObjectFieldAsChar(): Char = {
    readParentheses()
    val x = parseChar()
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsShort(): Short = {
    readParentheses()
    val x = parseInt(isToken = false)
    if (x > Short.MaxValue || x < Short.MinValue) decodeError("value is too large for short")
    readParenthesesWithColon()
    x.toShort
  }

  def readObjectFieldAsInt(): Int = {
    readParentheses()
    val x = parseInt(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsLong(): Long = {
    readParentheses()
    val x = parseLong(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsFloat(): Float = {
    readParentheses()
    val x = parseFloat(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsDouble(): Double = {
    readParentheses()
    val x = parseDouble(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsBigInt(): BigInt = {
    readParentheses()
    val x = parseBigInt(isToken = false)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsBigDecimal(): BigDecimal = {
    readParentheses()
    val x = parseBigDecimal(isToken = false)
    readParenthesesWithColon()
    x
  }

  // TODO: use more efficient unrolled loop
  def readByte(): Byte = {
    val x = parseInt(isToken = true)
    if (x > Byte.MaxValue || x < Byte.MinValue) decodeError("value is too large for byte")
    x.toByte
  }

  def readChar(): Char = {
    readParentheses()
    val x = parseChar()
    readParentheses()
    x
  }

  // TODO: use more efficient unrolled loop
  def readShort(): Short = {
    val x = parseInt(isToken = true)
    if (x > Short.MaxValue || x < Short.MinValue) decodeError("value is too large for short")
    x.toShort
  }

  def readInt(): Int = parseInt(isToken = true)

  def readLong(): Long = parseLong(isToken = true)

  def readDouble(): Double = parseDouble(isToken = true)

  def readFloat(): Float = parseFloat(isToken = true)

  def readBigInt(default: BigInt): BigInt = parseBigInt(isToken = true, default)

  def readBigDecimal(default: BigDecimal): BigDecimal = parseBigDecimal(isToken = true, default)

  def readString(default: String = null): String = {
    val b = nextToken()
    if (b == '"') {
      val len = parseString()
      new String(charBuf, 0, len)
    } else if (b == 'n') parseNull(default)
    else decodeError("expected string value or null")
  }

  def readBoolean(): Boolean = parseBoolean(isToken = true)

  def parseNull[A](default: A): A =
    if (nextByte() == 'u' && nextByte() == 'l' && nextByte() == 'l') default
    else decodeError("expected value or null")

  def readObjectFieldAsCharBuf(): Int = {
    if (nextToken() != '"') decodeError("expected '\"'")
    val x = parseString()
    readColon()
    x
  }

  def nextToken(): Byte = nextToken(head)

  def charBufToHashCode(len: Int): Int = toHashCode(charBuf, len)

  def isCharBufEqualsTo(len: Int, s: String): Boolean = len == s.length && isCharBufEqualsTo(len, s, 0)

  def skip(): Unit = head = (nextToken(): @switch) match {
    case '"' => skipString()
    case '0' => skipNumber()
    case '1' => skipNumber()
    case '2' => skipNumber()
    case '3' => skipNumber()
    case '4' => skipNumber()
    case '5' => skipNumber()
    case '6' => skipNumber()
    case '7' => skipNumber()
    case '8' => skipNumber()
    case '9' => skipNumber()
    case '-' => skipNumber()
    case 'n' => skipFixedBytes(3)
    case 't' => skipFixedBytes(3)
    case 'f' => skipFixedBytes(4)
    case '{' => skipNested('{', '}')
    case '[' => skipNested('[', ']')
    case _ => decodeError("expected value")
  }

  def nextByte(): Byte = {
    var pos = head
    if (pos == tail) pos = loadMoreOrError(pos)
    head = pos + 1
    buf(pos)
  }

  def unreadByte(): Unit = head -= 1

  def arrayStartError(): Nothing = decodeError("expected '[' or null")

  def arrayEndError(): Nothing = decodeError("expected ']' or ','")

  def objectStartError(): Nothing = decodeError("expected '{' or null")

  def objectEndError(): Nothing = decodeError("expected '}' or ','")

  def decodeError(msg: String): Nothing = decodeError(msg, head - 1)

  @inline
  private def decodeError(msg: String, pos: Int, cause: Throwable = null): Nothing =
    decodeError(appendString(msg, 0), pos, cause)

  private def decodeError(from: Int, pos: Int, cause: Throwable) = {
    var i = appendString(", offset: 0x", from)
    val offset = if (in eq null) 0 else totalRead - tail
    i = appendHex(offset + pos, i) // TODO: consider support of offset values beyond 2Gb
    if (config.appendHexDumpToParseException) {
      i = appendString(", buf:", i)
      i = appendHexDump(Math.max((pos - 32) & -16, 0), Math.min((pos + 48) & -16, tail), offset, i)
    }
    throw new JsonParseException(new String(charBuf, 0, i), cause, config.throwParseExceptionWithStackTrace)
  }

  @tailrec
  private def nextToken(pos: Int): Byte =
    if (pos < tail) {
      val b = buf(pos)
      if (b != ' ' && b != '\n' && b != '\t' && b != '\r') {
        head = pos + 1
        b
      } else nextToken(pos + 1)
    } else nextToken(loadMoreOrError(pos))

  @tailrec
  private def isCharBufEqualsTo(len: Int, s: String, i: Int): Boolean =
    if (i == len) true
    else if (charBuf(i) != s.charAt(i)) false
    else isCharBufEqualsTo(len, s, i + 1)

  private def appendString(s: String, from: Int): Int = {
    val lim = from + s.length
    ensureCharBufCapacity(lim, tail)
    var i = from
    while (i < lim) {
      charBuf(i) = s.charAt(i - from)
      i += 1
    }
    i
  }

  private def prependString(s: String, from: Int): Int = {
    val len = s.length
    val lim = from + len
    ensureCharBufCapacity(lim, tail)
    var i = lim - 1
    while (i >= len) {
      charBuf(i) = charBuf(i - len)
      i -= 1
    }
    i = 0
    while (i < len) {
      charBuf(i) = s.charAt(i)
      i += 1
    }
    lim
  }

  private def readParentheses(): Unit = if (nextByte() != '"') decodeError("expected '\"'")

  private def readParenthesesWithColon(): Unit =
    if (nextByte() != '"') decodeError("expected '\"'")
    else readColon()

  private def readColon(): Unit = if (nextToken() != ':') decodeError("expected ':'")

  private def parseBoolean(isToken: Boolean): Boolean = {
    val b = if (isToken) nextToken() else nextByte()
    if (b == 't') {
      if (nextByte() == 'r' && nextByte() == 'u' && nextByte() == 'e') true
      else booleanError()
    } else if (b == 'f') {
      if (nextByte() == 'a' && nextByte() == 'l' && nextByte() == 's' && nextByte() == 'e') false
      else booleanError()
    } else booleanError()
  }

  private def booleanError(pos: Int = head - 1): Nothing = decodeError("illegal boolean", pos)

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseInt(isToken: Boolean): Int = {
    var b = if (isToken) nextToken() else nextByte()
    val negative = b == '-'
    if (negative) b = nextByte()
    var pos = head
    if (b >= '0' && b <= '9') {
      var v = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos == 0
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (v == 0) leadingZeroError(pos - 1)
        if (v < -214748364) intOverflowError(pos)
        v = v * 10 + ('0' - b)
        if (v >= 0) intOverflowError(pos)
        pos + 1
      }
      head = pos
      if (negative) v
      else if (v == Int.MinValue) intOverflowError(pos - 1)
      else -v
    } else numberError(pos - 1)
  }

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseLong(isToken: Boolean): Long = {
    var b = if (isToken) nextToken() else nextByte()
    val negative = b == '-'
    if (negative) b = nextByte()
    var pos = head
    if (b >= '0' && b <= '9') {
      var v: Long = '0' - b
      while ((pos < tail || {
        pos = loadMore(pos)
        pos == 0
      }) && {
        b = buf(pos)
        b >= '0' && b <= '9'
      }) pos = {
        if (v == 0) leadingZeroError(pos - 1)
        if (v < -922337203685477580L) longOverflowError(pos)
        v = v * 10 + ('0' - b)
        if (v >= 0) longOverflowError(pos)
        pos + 1
      }
      head = pos
      if (negative) v
      else if (v == Long.MinValue) longOverflowError(pos - 1)
      else -v
    } else numberError(pos - 1)
  }

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseDouble(isToken: Boolean): Double = {
    var posMan = 0L
    var manExp = 0
    var posExp = 0
    var isNeg = false
    var isExpNeg = false
    var isZeroFirst = false
    var i = 0
    var state = 0
    var pos = ensureCharBufCapacity(i, head)
    while (pos < tail || {
      pos = ensureCharBufCapacity(i, loadMore(pos))
      pos == 0
    }) {
      val ch = buf(pos).toChar
      (state: @switch) match {
        case 0 => // start
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            if (!isToken) numberError(pos)
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            isNeg = true
            state = 2
          } else numberError(pos)
        case 1 => // whitespaces
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            isNeg = true
            state = 2
          } else numberError(pos)
        case 2 => // signum
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else numberError(pos)
        case 3 => // first int digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = posMan * 10 + (ch - '0')
            if (isZeroFirst) leadingZeroError(pos - 1)
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 4 => // int digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 922337203685477580L) posMan = posMan * 10 + (ch - '0')
            else manExp += 1
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 5 => // dot
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 922337203685477580L) {
              posMan = posMan * 10 + (ch - '0')
              manExp -= 1
            }
            state = 6
          } else numberError(pos)
        case 6 => // frac digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 922337203685477580L) {
              posMan = posMan * 10 + (ch - '0')
              manExp -= 1
            }
            state = 6
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 7 => // e char
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = ch - '0'
            state = 9
          } else if (ch == '-' || ch == '+') {
            i = putCharAt(ch, i)
            isExpNeg = ch == '-'
            state = 8
          } else numberError(pos)
        case 8 => // exp. sign
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = ch - '0'
            state = 9
          } else numberError(pos)
        case 9 => // exp. digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = posExp * 10 + (ch - '0')
            state = if (Math.abs(toExp(manExp, isExpNeg, posExp)) > 350) 10 else 9
          } else {
            head = pos
            return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 10 => // exp. digit overflow
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 10
          } else {
            head = pos
            return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
      }
      pos += 1
    }
    head = pos
    if (state == 3 || state == 4 || state == 6 || state == 9) toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
    else if (state == 10) toExpOverflowDouble(isNeg, posMan, manExp, isExpNeg, posExp)
    else numberError()
  }

  private def toDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int, i: Int): Double =
    if (posMan <= 999999999999999L) { // 10^15 - 1, where max mantissa that can be converted w/o rounding error by double ops
      val man = if (isNeg) -posMan else posMan
      val exp = toExp(manExp, isExpNeg, posExp)
      if (exp == 0) man
      else {
        val maxExp = pow10d.length
        if (exp > -maxExp && exp < 0) man / pow10d(-exp)
        else if (exp > 0 && exp < maxExp) man * pow10d(exp)
        else toDouble(i)
      }
    } else toDouble(i)

  private def toDouble(i: Int): Double = java.lang.Double.parseDouble(new String(charBuf, 0, i))

  private def toExpOverflowDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int): Double =
    if (toExp(manExp, isExpNeg, posExp) > 0 && posMan != 0) {
      if (isNeg) Double.NegativeInfinity else Double.PositiveInfinity
    } else {
      if (isNeg) -0.0 else 0.0
    }

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseFloat(isToken: Boolean): Float = {
    var posMan = 0
    var manExp = 0
    var posExp = 0
    var isNeg = false
    var isExpNeg = false
    var isZeroFirst = false
    var i = 0
    var state = 0
    var pos = ensureCharBufCapacity(i, head)
    while (pos < tail || {
      pos = ensureCharBufCapacity(i, loadMore(pos))
      pos == 0
    }) {
      val ch = buf(pos).toChar
      (state: @switch) match {
        case 0 => // start
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            if (!isToken) numberError(pos)
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            isNeg = true
            state = 2
          } else numberError(pos)
        case 1 => // whitespaces
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            isNeg = true
            state = 2
          } else numberError(pos)
        case 2 => // signum
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = ch - '0'
            isZeroFirst = posMan == 0
            state = 3
          } else numberError(pos)
        case 3 => // first int digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posMan = posMan * 10 + (ch - '0')
            if (isZeroFirst) leadingZeroError(pos - 1)
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 4 => // int digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 214748364) posMan = posMan * 10 + (ch - '0')
            else manExp += 1
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 5 => // dot
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 214748364) {
              posMan = posMan * 10 + (ch - '0')
              manExp -= 1
            }
            state = 6
          } else numberError(pos)
        case 6 => // frac digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            if (posMan < 214748364) {
              posMan = posMan * 10 + (ch - '0')
              manExp -= 1
            }
            state = 6
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 7 => // e char
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = ch - '0'
            state = 9
          } else if (ch == '-' || ch == '+') {
            i = putCharAt(ch, i)
            isExpNeg = ch == '-'
            state = 8
          } else numberError(pos)
        case 8 => // exp. sign
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = ch - '0'
            state = 9
          } else numberError(pos)
        case 9 => // exp. digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            posExp = posExp * 10 + (ch - '0')
            state = if (Math.abs(toExp(manExp, isExpNeg, posExp)) > 55) 10 else 9
          } else {
            head = pos
            return toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
        case 10 => // exp. digit overflow
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 10
          } else {
            head = pos
            return toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
          }
      }
      pos += 1
    }
    head = pos
    if (state == 3 || state == 4 || state == 6 || state == 9) toFloat(isNeg, posMan, manExp, isExpNeg, posExp, i)
    else if (state == 10) toExpOverflowFloat(isNeg, posMan, manExp, isExpNeg, posExp)
    else numberError()
  }

  private def toFloat(isNeg: Boolean, posMan: Int, manExp: Int, isExpNeg: Boolean, posExp: Int, i: Int): Float = {
    val man = if (isNeg) -posMan else posMan
    val exp = toExp(manExp, isExpNeg, posExp)
    if (posMan <= 9999999) { // 10^7 - 1, max mantissa that can be converted w/o rounding error by float ops
      if (exp == 0) man
      else {
        val maxFloatExp = pow10f.length
        if (exp > -maxFloatExp && exp < 0) man / pow10f(-exp)
        else if (exp > 0 && exp < maxFloatExp) man * pow10f(exp)
        else toFloat(man, exp, i)
      }
    } else toFloat(i) //FIXME: use toFloat(man, exp, i) here to get better accuracy than `java.lang.Float.parseFloat`
  }

  private def toFloat(man: Int, exp: Int, i: Int): Float = {
    val maxDoubleExp = pow10d.length
    if (exp > -maxDoubleExp && exp < 0) (man / pow10d(-exp)).toFloat
    else if (exp > 0 && exp < maxDoubleExp) (man * pow10d(exp)).toFloat
    else toFloat(i)
  }

  private def toFloat(i: Int): Float = java.lang.Float.parseFloat(new String(charBuf, 0, i))

  private def toExpOverflowFloat(isNeg: Boolean, posMan: Int, manExp: Int, isExpNeg: Boolean, posExp: Int): Float =
    if (toExp(manExp, isExpNeg, posExp) > 0 && posMan != 0) {
      if (isNeg) Float.NegativeInfinity else Float.PositiveInfinity
    } else {
      if (isNeg) -0.0f else 0.0f
    }

  private def toExp(manExp: Int, isExpNeg: Boolean, exp: Int): Int = manExp + (if (isExpNeg) -exp else exp)

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseBigInt(isToken: Boolean, default: BigInt = null): BigInt = {
    var b = if (isToken) nextToken() else nextByte()
    if (b == 'n') {
      if (isToken) parseNull(default)
      else numberError()
    } else {
      ensureCharBufCapacity(2, head)
      var i = 0
      val negative = b == '-'
      if (negative) {
        i = putCharAt(b.toChar, i)
        b = nextByte()
      }
      var pos = head
      if (b >= '0' && b <= '9') {
        val isZeroFirst = b == '0'
        i = putCharAt(b.toChar, i)
        while ((pos < tail || {
          pos = ensureCharBufCapacity(i, loadMore(pos))
          pos == 0
        }) && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) pos = {
          if (isZeroFirst ) leadingZeroError(pos - 1)
          i = putCharAt(b.toChar, i)
          pos + 1
        }
        head = pos
        new BigInt(new java.math.BigDecimal(charBuf, 0, i).toBigInteger)
      } else numberError(pos - 1)
    }
  }

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseBigDecimal(isToken: Boolean, default: BigDecimal = null): BigDecimal = {
    var isZeroFirst = false
    var i = 0
    var state = 0
    var pos = ensureCharBufCapacity(i, head)
    while (pos < tail || {
      pos = ensureCharBufCapacity(i, loadMore(pos))
      pos == 0
    }) {
      val ch = buf(pos).toChar
      (state: @switch) match {
        case 0 => // start
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            if (!isToken) numberError(pos)
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            isZeroFirst = ch == '0'
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            state = 2
          } else if (ch == 'n' && isToken) {
            state = 10
          } else numberError(pos)
        case 1 => // whitespaces
          if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
            state = 1
          } else if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            isZeroFirst = ch == '0'
            state = 3
          } else if (ch == '-') {
            i = putCharAt(ch, i)
            state = 2
          } else if (ch == 'n' && isToken) {
            state = 10
          } else numberError(pos)
        case 2 => // signum
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            isZeroFirst = ch == '0'
            state = 3
          } else numberError(pos)
        case 3 => // first int digit
          if (ch >= '0' && ch <= '9') {
            if (isZeroFirst) leadingZeroError(pos - 1)
            i = putCharAt(ch, i)
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 4 => // int digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 4
          } else if (ch == '.') {
            i = putCharAt(ch, i)
            state = 5
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 5 => // dot
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 6
          } else numberError(pos)
        case 6 => // frac digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 6
          } else if (ch == 'e' || ch == 'E') {
            i = putCharAt(ch, i)
            state = 7
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 7 => // e char
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 9
          } else if (ch == '-' || ch == '+') {
            i = putCharAt(ch, i)
            state = 8
          } else numberError(pos)
        case 8 => // exp. sign
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 9
          } else numberError(pos)
        case 9 => // exp. digit
          if (ch >= '0' && ch <= '9') {
            i = putCharAt(ch, i)
            state = 9
          } else {
            head = pos
            return toBigDecimal(i)
          }
        case 10 => // n'u'll
          if (ch == 'u') state = 11
          else numberError(pos)
        case 11 => // nu'l'l
          if (ch == 'l') state = 12
          else numberError(pos)
        case 12 => // nul'l'
          if (ch == 'l') state = 13
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
    else numberError()
  }

  private def toBigDecimal(len: Int): BigDecimal =
    new BigDecimal(new java.math.BigDecimal(charBuf, 0, len))

  private def numberError(pos: Int = head): Nothing = decodeError("illegal number", pos)

  private def leadingZeroError(pos: Int): Nothing = decodeError("illegal number with leading zero", pos)

  private def intOverflowError(pos: Int): Nothing = decodeError("value is too large for int", pos)

  private def longOverflowError(pos: Int): Nothing = decodeError("value is too large for long", pos)

  @tailrec
  private def parseString(i: Int = 0, pos: Int = ensureCharBufCapacity(0, head)): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') {
        head = pos + 1
        i
      } else if ((b ^ '\\') < 1) slowParseString(i, pos)
      else parseString(putCharAt(b.toChar, i), pos + 1)
    } else parseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))

  @tailrec
  private def slowParseString(i: Int, pos: Int): Int = {
    val remaining = tail - pos
    if (remaining > 0) {
      val b1 = buf(pos)
      if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
        if (b1 == '"') {
          head = pos + 1
          i
        } else if (b1 != '\\') slowParseString(putCharAt(b1.toChar, i), pos + 1)
        else if (remaining > 1) {
          (buf(pos + 1): @switch) match {
            case 'b' => slowParseString(putCharAt('\b', i), pos + 2)
            case 'f' => slowParseString(putCharAt('\f', i), pos + 2)
            case 'n' => slowParseString(putCharAt('\n', i), pos + 2)
            case 'r' => slowParseString(putCharAt('\r', i), pos + 2)
            case 't' => slowParseString(putCharAt('\t', i), pos + 2)
            case '"' => slowParseString(putCharAt('"', i), pos + 2)
            case '/' => slowParseString(putCharAt('/', i), pos + 2)
            case '\\' => slowParseString(putCharAt('\\', i), pos + 2)
            case 'u' =>
              if (remaining > 5) {
                val ch1 = readEscapedUnicode(pos + 2)
                if (ch1 < 2048) slowParseString(putCharAt(ch1, i), pos + 6)
                else if (!Character.isHighSurrogate(ch1)) {
                  if (Character.isLowSurrogate(ch1)) decodeError("expected high surrogate character", pos + 5)
                  slowParseString(putCharAt(ch1, i), pos + 6)
                } else if (remaining > 11) {
                  if (buf(pos + 6) == '\\') {
                    if (buf(pos + 7) == 'u') {
                      val ch2 = readEscapedUnicode(pos + 8)
                      if (!Character.isLowSurrogate(ch2)) decodeError("expected low surrogate character", pos + 11)
                      slowParseString(putCharAt(ch2, putCharAt(ch1, i)), pos + 12)
                    } else illegalEscapeSequenceError(pos + 7)
                  } else illegalEscapeSequenceError(pos + 6)
                } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
              } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
            case _ => illegalEscapeSequenceError(pos + 1)
          }
        } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
      } else if ((b1 >> 5) == -2) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        if (remaining > 1) {
          val b2 = buf(pos + 1)
          if (isMalformed2(b1, b2)) malformedBytes(b1, b2, pos)
          slowParseString(putCharAt(((b1 << 6) ^ b2 ^ 0xF80).toChar, i), pos + 2) // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
        } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
      } else if ((b1 >> 4) == -2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        if (remaining > 2) {
          val b2 = buf(pos + 1)
          val b3 = buf(pos + 2)
          val ch = ((b1 << 12) ^ (b2 << 6) ^ b3 ^ 0xFFFE1F80).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
          if (isMalformed3(b1, b2, b3) || Character.isSurrogate(ch)) malformedBytes(b1, b2, b3, pos)
          slowParseString(putCharAt(ch, i), pos + 3)
        } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
      } else if ((b1 >> 3) == -2) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        if (remaining > 3) {
          val b2 = buf(pos + 1)
          val b3 = buf(pos + 2)
          val b4 = buf(pos + 3)
          val cp = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ b4 ^ 0x381F80 // 0x381F80 == ((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
          if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(cp)) malformedBytes(b1, b2, b3, b4, pos)
          slowParseString(putCharAt(Character.lowSurrogate(cp), putCharAt(Character.highSurrogate(cp), i)), pos + 4)
        } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
      } else malformedBytes(b1, pos)
    } else slowParseString(i, ensureCharBufCapacity(i, loadMoreOrError(pos)))
  }

  @tailrec
  private def parseChar(pos: Int = head): Char = {
    val remaining = tail - pos
    if (remaining > 0) {
      val b1 = buf(pos)
      if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
        if (b1 == '"') decodeError("illegal value for char")
        else if (b1 != '\\') returnChar(b1.toChar, pos + 1)
        else if (remaining > 1) {
          (buf(pos + 1): @switch) match {
            case 'b' => returnChar('\b', pos + 2)
            case 'f' => returnChar('\f', pos + 2)
            case 'n' => returnChar('\n', pos + 2)
            case 'r' => returnChar('\r', pos + 2)
            case 't' => returnChar('\t', pos + 2)
            case '"' => returnChar('"', pos + 2)
            case '/' => returnChar('/', pos + 2)
            case '\\' => returnChar('\\', pos + 2)
            case 'u' =>
              if (remaining > 5) {
                val ch1 = readEscapedUnicode(pos + 2)
                if (Character.isSurrogate(ch1)) decodeError("illegal surrogate character", pos + 5)
                returnChar(ch1, pos + 6)
              } else parseChar(loadMoreOrError(pos))
            case _ => illegalEscapeSequenceError(pos + 1)
          }
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 5) == -2) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        if (remaining > 1) {
          val b2 = buf(pos + 1)
          if (isMalformed2(b1, b2)) malformedBytes(b1, b2, pos)
          returnChar(((b1 << 6) ^ b2 ^ 0xF80).toChar, pos + 2) // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 4) == -2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        if (remaining > 2) {
          val b2 = buf(pos + 1)
          val b3 = buf(pos + 2)
          val ch = ((b1 << 12) ^ (b2 << 6) ^ b3 ^ 0xFFFE1F80).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
          if (isMalformed3(b1, b2, b3) || Character.isSurrogate(ch)) malformedBytes(b1, b2, b3, pos)
          returnChar(ch, pos + 3)
        } else parseChar(loadMoreOrError(pos))
      } else if ((b1 >> 3) == -2) decodeError("illegal surrogate character", pos + 3)
      else malformedBytes(b1, pos)
    } else parseChar(loadMoreOrError(pos))
  }

  @inline
  private def returnChar(ch: Char, pos: Int): Char = {
    head = pos
    ch
  }

  private def readEscapedUnicode(pos1: Int): Char = {
    val pos2 = pos1 + 1
    val pos3 = pos1 + 2
    val pos4 = pos1 + 3
    ((fromHexDigit(buf(pos1), pos1) << 12) +
      (fromHexDigit(buf(pos2), pos2) << 8) +
      (fromHexDigit(buf(pos3), pos3) << 4) +
      fromHexDigit(buf(pos4), pos4)).toChar
  }

  private def fromHexDigit(b: Byte, pos: Int): Int =
    if (b >= '0' && b <= '9') b - 48
    else {
      val b1 = b & -33
      if (b1 >= 'A' && b1 <= 'F') b1 - 55
      else decodeError("expected hex digit", pos)
    }

  private def isMalformed2(b1: Byte, b2: Byte): Boolean =
    (b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80

  private def isMalformed3(b1: Byte, b2: Byte, b3: Byte): Boolean =
    (b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80

  private def isMalformed4(b2: Byte, b3: Byte, b4: Byte): Boolean =
    (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80

  private def illegalEscapeSequenceError(pos: Int): Nothing = decodeError("illegal escape sequence", pos)

  private def malformedBytes(b1: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    decodeError(i, pos, null)
  }

  private def malformedBytes(b1: Byte, b2: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    i = appendString(", 0x", i)
    i = appendHex(b2, i)
    decodeError(i, pos + 1, null)
  }

  private def malformedBytes(b1: Byte, b2: Byte, b3: Byte, pos: Int): Nothing = {
    var i = appendString("malformed byte(s): 0x", 0)
    i = appendHex(b1, i)
    i = appendString(", 0x", i)
    i = appendHex(b2, i)
    i = appendString(", 0x", i)
    i = appendHex(b3, i)
    decodeError(i, pos + 2, null)
  }

  private def malformedBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, pos: Int): Nothing = {
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
    var i = appendString(dumpHeader, from)
    i = appendString(dumpBorder, i)
    var j = 0
    while (j < len) {
      ensureCharBufCapacity(i + 81, tail) // 81 == dumpBorder.length
      val linePos = j & 15
      if (linePos == 0) {
        i = putCharAt('\n', i)
        i = putCharAt('|', i)
        i = putCharAt(' ', i)
        i = appendHex(alignedAbsFrom + j, i)
        i = putCharAt(' ', i)
        i = putCharAt('|', i)
        i = putCharAt(' ', i)
      }
      val pos = bufOffset + j
      if (pos >= start && pos < end) {
        val b = buf(pos)
        i = appendHex(b, i)
        i = putCharAt(' ', i)
        putCharAt(if (b <= 31 || b >= 127) '.' else b.toChar, i + 47 - (linePos << 1))
      } else {
        i = putCharAt(' ', i)
        i = putCharAt(' ', i)
        i = putCharAt(' ', i)
        putCharAt(' ', i + 47 - (linePos << 1))
      }
      if (linePos == 15) {
        i = putCharAt('|', i)
        i = putCharAt(' ', i)
        i = putCharAt(' ', i + 16)
        i = putCharAt('|', i)
      }
      j += 1
    }
    appendString(dumpBorder, i)
  }

  private def appendHex(d: Int, i: Int): Int = {
    ensureCharBufCapacity(i + 8, tail)
    charBuf(i) = toHexDigit(d >>> 28)
    charBuf(i + 1) = toHexDigit(d >>> 24)
    charBuf(i + 2) = toHexDigit(d >>> 20)
    charBuf(i + 3) = toHexDigit(d >>> 16)
    charBuf(i + 4) = toHexDigit(d >>> 12)
    charBuf(i + 5) = toHexDigit(d >>> 8)
    charBuf(i + 6) = toHexDigit(d >>> 4)
    charBuf(i + 7) = toHexDigit(d)
    i + 8
  }

  private def appendHex(b: Byte, i: Int): Int = {
    ensureCharBufCapacity(i + 2, tail)
    charBuf(i) = toHexDigit(b >>> 4)
    charBuf(i + 1) = toHexDigit(b)
    i + 2
  }

  private def toHexDigit(n: Int): Char = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toChar

  @inline
  private def putCharAt(ch: Char, i: Int): Int = {
    // ensureCharBufCapacity(i + 1, tail) <- commented for better performance, so always call it externally
    charBuf(i) = ch
    i + 1
  }

  @inline
  private def ensureCharBufCapacity(i: Int, pos: Int): Int = {
    val required = tail - pos + i
    if (required > charBuf.length) growCharBuf(required)
    pos
  }

  private def growCharBuf(required: Int): Unit = {
    val cs = new Array[Char](Math.max(charBuf.length << 1, required))
    System.arraycopy(charBuf, 0, cs, 0, charBuf.length)
    charBuf = cs
  }

  @tailrec
  private def skipString(evenBackSlashes: Boolean = true, pos: Int = head): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"' && evenBackSlashes) pos + 1
      else if (b == '\\') skipString(!evenBackSlashes, pos + 1)
      else skipString(evenBackSlashes = true, pos + 1)
    } else skipString(evenBackSlashes, loadMoreOrError(pos))

  @tailrec
  private def skipNumber(pos: Int = head): Int =
    if (pos < tail) {
      val b = buf(pos)
      if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+' || b == 'e' || b == 'E') skipNumber(pos + 1)
      else pos
    } else skipNumber(loadMoreOrError(pos))

  @tailrec
  private def skipNested(opening: Byte, closing: Byte, level: Int = 0, pos: Int = head): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') skipNested(opening, closing, level, skipString(pos = pos + 1))
      else if (b == closing) {
        if (level == 0) pos + 1
        else skipNested(opening, closing, level - 1, pos + 1)
      } else if (b == opening) skipNested(opening, closing, level + 1, pos + 1)
      else skipNested(opening, closing, level, pos + 1)
    } else skipNested(opening, closing, level, loadMoreOrError(pos))

  @tailrec
  private def skipFixedBytes(n: Int, pos: Int = head): Int = {
    val newPos = pos + n
    val diff = newPos - tail
    if (diff <= 0) newPos
    else skipFixedBytes(diff, loadMoreOrError(pos))
  }

  private def loadMoreOrError(pos: Int): Int =
    if (in eq null) endOfInput()
    else {
      val remaining = tail - pos
      if (remaining > 0) {
        System.arraycopy(buf, pos, buf, 0, remaining)
      }
      val n = externalRead(remaining, buf.length - remaining)
      if (n > 0) {
        tail = remaining + n
        totalRead += n
        0
      } else endOfInput()
    }

  private def loadMore(pos: Int): Int =
    if (in eq null) pos
    else {
      val n = externalRead(0, buf.length)
      if (n > 0) {
        tail = n
        totalRead += n
        0
      } else pos
    }

  private def externalRead(from: Int, len: Int): Int =
    try in.read(buf, from, len) catch {
      case NonFatal(ex) => endOfInput(ex)
    }

  private def endOfInput(cause: Throwable = null): Nothing = decodeError("unexpected end of input", tail, cause)

  private def freeTooLongCharBuf(): Unit =
    if (charBuf.length > 16384) charBuf = new Array[Char](16384)
}

object JsonReader {
  private val pool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader()
  }
  private val defaultConfig = ReaderConfig()
  private val pow10f: Array[Float] = // all powers of 10 that can be represented exactly in float
    Array(1f, 1e+1f, 1e+2f, 1e+3f, 1e+4f, 1e+5f, 1e+6f, 1e+7f, 1e+8f, 1e+9f, 1e+10f)
  private val pow10d: Array[Double] = // all powers of 10 that can be represented exactly in double
    Array(1, 1e+1, 1e+2, 1e+3, 1e+4, 1e+5, 1e+6, 1e+7, 1e+8, 1e+9, 1e+10, 1e+11,
      1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21, 1e+22)
  private val dumpHeader =
    "\n           +-------------------------------------------------+" +
    "\n           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |"
  private val dumpBorder =
    "\n+----------+-------------------------------------------------+------------------+"

  /**
    * Deserialize JSON content encoded in UTF-8 from an input stream into a value of given `A` type 
    * with default parsing options that maximize description of error. 
    * 
    * Use custom configuration to turn on racing of stack less exceptions and/or turn off a hex dump printing 
    * to the error message. 
    *
    * @param codec a codec for the given `A` type
    * @param in the input stream to parse from
    * @tparam A type of the value to parse
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end of input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException If the `codec` or `in` is null.
    */
  final def read[A](codec: JsonCodec[A], in: InputStream): A = read(codec, in, defaultConfig)

  /**
    * Deserialize JSON content encoded in UTF-8 from an input stream into a value of given `A` type.
    *
    * @param codec a codec for the given `A` type
    * @param in the input stream to parse from
    * @param config a parsing configuration
    * @tparam A type of the value to parse
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also if a low-level I/O problem (unexpected end-of-input, network error) occurs
    *                            while some input bytes are expected
    * @throws NullPointerException if the `codec`, `in` or `config` is null
    */
  final def read[A](codec: JsonCodec[A], in: InputStream, config: ReaderConfig): A = {
    if ((in eq null) || (config eq null)) throw new NullPointerException
    val reader = pool.get
    reader.config = config
    reader.in = in
    reader.head = 0
    reader.tail = 0
    reader.totalRead = 0
    try codec.decode(reader, codec.default) // also checks that `codec` is not null before any parsing
    finally {
      reader.in = null  // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
      reader.freeTooLongCharBuf()
    }
  }

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type
    * with default parsing options that maximize description of error. 
    *
    * Use custom configuration to turn on racing of stack less exceptions and/or turn off a hex dump printing 
    * to the error message. 
    *
    * @param codec a codec for the given `A` type
    * @param buf the byte array to parse from
    * @tparam A type of the value to parse
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException If the `codec` or `buf` is null.
    */
  final def read[A](codec: JsonCodec[A], buf: Array[Byte]): A = read(codec, buf, defaultConfig)

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type
    * with specified parsing options.
    *
    * @param codec a codec for the given `A` type
    * @param buf the byte array to parse from
    * @param config a parsing configuration
    * @tparam A type of the value to parse
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    */
  final def read[A](codec: JsonCodec[A], buf: Array[Byte], config: ReaderConfig): A =
    read(codec, buf, 0, buf.length, config)

  /**
    * Deserialize JSON content encoded in UTF-8 from a byte array into a value of given `A` type with
    * specified parsing options or with defaults that maximize description of error. 
    *
    * @param codec a codec for the given `A` type
    * @param buf the byte array to parse from
    * @param from the start position of the provided byte array
    * @param to the position of end of input in the provided byte array
    * @param config a parsing configuration
    * @tparam A type of the value to parse
    * @return a successfully parsed value
    * @throws JsonParseException if underlying input contains malformed UTF-8 bytes, invalid JSON content or
    *                            the input JSON structure does not match structure that expected for result type,
    *                            also in case if end of input is detected while some input bytes are expected
    * @throws NullPointerException if the `codec`, `buf` or `config` is null
    * @throws ArrayIndexOutOfBoundsException if the `to` is greater than `buf` length or negative,
    *                                        or `from` is greater than `to` or negative
    */
  final def read[A](codec: JsonCodec[A], buf: Array[Byte], from: Int, to: Int, config: ReaderConfig = defaultConfig): A = {
    if (config eq null) throw new NullPointerException
    if (to > buf.length || to < 0) // also checks that `buf` is not null before any parsing
      throw new ArrayIndexOutOfBoundsException("`to` should be positive and not greater than `buf` length")
    if (from > to || from < 0)
      throw new ArrayIndexOutOfBoundsException("`from` should be positive and not greater than `to`")
    val reader = pool.get
    val currBuf = reader.buf
    reader.config = config
    reader.buf = buf
    reader.head = from
    reader.tail = to
    reader.totalRead = 0
    try codec.decode(reader, codec.default) // also checks that `codec` is not null before any parsing
    finally {
      reader.buf = currBuf
      reader.freeTooLongCharBuf()
    }
  }

  final def toHashCode(cs: Array[Char], len: Int): Int = {
    var i = 0
    var h = 777767777
    while (i < len) {
      h = (h ^ cs(i)) * 1500450271
      h ^= (h >>> 21) // mix highest bits to reduce probability of zeroing and loosing part of hash from preceding chars
      i += 1
    }
    h
  }
}