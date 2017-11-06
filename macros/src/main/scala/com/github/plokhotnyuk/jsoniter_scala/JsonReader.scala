package com.github.plokhotnyuk.jsoniter_scala

import java.io.InputStream

import com.github.plokhotnyuk.jsoniter_scala.JsonReader._

import scala.annotation.{switch, tailrec}

class JsonException(message: String) extends RuntimeException(message)

final class JsonReader private[jsoniter_scala](
    private var buf: Array[Byte] = new Array[Byte](4096),
    private var head: Int = 0,
    private var tail: Int = 0,
    private var reusableChars: Array[Char] = new Array[Char](4096),
    private var in: InputStream = null,
    private var totalRead: Int = 0) {
  def reqFieldError(reqFields: Array[String], reqs: Int*): Nothing = {
    val sb = new StringBuilder(64)
    val len = reqFields.length
    var i = 0
    while (i < len) {
      if ((reqs(i >> 5) & (1 << i)) != 0) {
        sb.append(if (sb.isEmpty) "missing required field(s) \"" else ", \"").append(reqFields(i)).append('"')
      }
      i += 1
    }
    decodeError(sb.toString())
  }

  def readObjectFieldAsString(): String = {
    readParentheses()
    val x = reusableCharsToString(parseString())
    readColon()
    x
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
    else { // FIXME: remove else
      readParenthesesWithColon()
      x.toByte
    }
  }

  def readObjectFieldAsChar(): Char = {
    readParentheses()
    val len = parseString()
    val x = reusableChars(0)
    if (len != 1) decodeError("illegal value for char")
    else {
      readColon()
      x
    }
  }

  def readObjectFieldAsShort(): Short = {
    readParentheses()
    val x = parseInt(isToken = false)
    if (x > Short.MaxValue || x < Short.MinValue) decodeError("value is too large for short")
    else {
      readParenthesesWithColon()
      x.toShort
    }
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
    val x = parseDouble(isToken = false).toFloat
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
    val x = new BigInt(parseBigDecimal(isToken = false).toBigInteger) // FIXME: rounding considered harmfull
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsBigDecimal(): BigDecimal = {
    readParentheses()
    val x = new BigDecimal(parseBigDecimal(isToken = false))
    readParenthesesWithColon()
    x
  }

  // TODO: use more efficient unrolled loop
  def readByte(): Byte = {
    val x = parseInt(isToken = true)
    if (x > Byte.MaxValue || x < Byte.MinValue) decodeError("value is too large for byte")
    x.toByte
  }

  // TODO: use more efficient unrolled loop
  def readChar(): Char = {
    readParentheses()
    if (parseString() != 1) decodeError("illegal value for char")
    reusableChars(0)
  }

  // TODO: unrolled loop
  def readShort(): Short = {
    val x = parseInt(isToken = true)
    if (x > Short.MaxValue || x < Short.MinValue) decodeError("value is too large for short")
    x.toShort
  }

  def readInt(): Int = parseInt(isToken = true)

  def readLong(): Long = parseLong(isToken = true)

  def readDouble(): Double = parseDouble(isToken = true)

  def readFloat(): Float = parseDouble(isToken = true).toFloat

  def readBigInt(default: BigInt): BigInt = {
    val x = parseBigDecimal(isToken = true)
    if (x eq null) default
    else new BigInt(x.toBigInteger)
  }

  def readBigDecimal(default: BigDecimal): BigDecimal = {
    val x = parseBigDecimal(isToken = true)
    if (x eq null) default
    else new BigDecimal(x)
  }

  def readString(default: String = null): String = {
    val b = nextToken()
    if (b == '"') reusableCharsToString(parseString())
    else if (b == 'n') parseNull(default)
    else decodeError("expected string value or null")
  }

  def readBoolean(): Boolean = parseBoolean(isToken = true)

  def parseNull[A](default: A): A =
    if (nextByte() == 'u' && nextByte() == 'l' && nextByte() == 'l') default
    else decodeError("expected value or null")

  def readObjectFieldAsReusableChars(): Int = {
    if (nextToken() != '"') decodeError("expected '\"'")
    val x = parseString()
    readColon()
    x
  }

  def nextToken(): Byte = nextToken(head)

  @tailrec
  private def nextToken(pos: Int): Byte =
    if (pos < tail) {
      val b = buf(pos)
      if (b != ' ' && b != '\n' && b != '\t' && b != '\r') {
        head = pos + 1
        b
      } else nextToken(pos + 1)
    } else nextToken(loadMoreOrError(pos))

  def reusableCharsToHashCode(len: Int): Int = toHashCode(reusableChars, len)

  def isReusableCharsEqualsTo(len: Int, s: String): Boolean = len == s.length && isReusableCharsEqualsTo(len, s, 0)

  @tailrec
  private def isReusableCharsEqualsTo(len: Int, s: String, i: Int): Boolean =
    if (i == len) true // FIXME simplify to a one boolean expression
    else if (reusableChars(i) != s.charAt(i)) false
    else isReusableCharsEqualsTo(len, s, i + 1)

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

  def unreadByte(): Unit = head -= 1

  def arrayStartError(): Nothing = decodeError("expected '[' or null")

  def arrayEndError(): Nothing = decodeError("expected ']' or ','")

  def objectStartError(): Nothing = decodeError("expected '{' or null")

  def objectEndError(): Nothing = decodeError("expected '}' or ','")

  def decodeError(msg: String): Nothing = decodeError(msg, head - 1)

  private def decodeError(msg: String, pos: Int): Nothing = {
    val from = Math.max((pos - 32) & -16, 0)
    val to = Math.min((pos + 48) & -16, tail)
    val sb = new StringBuilder(1024).append(msg).append(", offset: 0x")
    val offset = if (in eq null) 0 else totalRead - tail
    appendHex(offset + pos, sb) // TODO: consider support of offset values beyond 2Gb
    sb.append(", buf:")
    appendHexDump(buf, from, to, offset, sb)
    throw new JsonException(sb.toString)
  }

  private def reusableCharsToString(len: Int): String = new String(reusableChars, 0, len)

  private def readParentheses(): Unit = if (nextByte() != '"') decodeError("expected '\"'")

  private def readParenthesesWithColon(): Unit =
    if (nextByte() != '"') decodeError("expected '\"'")
    else readColon()

  private def readColon(): Unit = if (nextToken() != ':') decodeError("expected ':'")

  private def nextByte(): Byte = {
    var pos = head
    if (pos == tail) pos = loadMoreOrError(pos)
    head = pos + 1
    buf(pos)
  }

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
    if (b >= '0' && b <= '9') {
      var v = '0' - b
      var pos = head
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
    } else {
      unreadByte() // FIXME: use position offset in numberError instead
      numberError()
    }
  }

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseLong(isToken: Boolean): Long = {
    var b = if (isToken) nextToken() else nextByte()
    val negative = b == '-'
    if (negative) b = nextByte()
    if (b >= '0' && b <= '9') {
      var v: Long = '0' - b
      var pos = head
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
    } else {
      unreadByte()
      numberError()
    }
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
    var pos = ensureReusableCharsCapacity(i, head)
    while (pos < tail || {
      pos = ensureReusableCharsCapacity(i, loadMore(pos))
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
            state = if (Math.abs(toExp(manExp, isExpNeg, posExp)) > 214748364) 10 else 9
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
    else if (state == 10) toExpOverflowDouble(isNeg, manExp, isExpNeg, posExp)
    else numberError()
  }

  private def toDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int, i: Int): Double = {
    val man = if (isNeg) -posMan else posMan
    val exp = toExp(manExp, isExpNeg, posExp)
    if (exp == 0) man
    else {
      val maxExp = pow10.length
      if (exp >= -maxExp && exp < 0) man / pow10(-exp)
      else if (exp > 0 && exp <= maxExp) man * pow10(exp)
      else java.lang.Double.parseDouble(reusableCharsToString(i))
    }
  }

  private def toExpOverflowDouble(isNeg: Boolean, manExp: Int, isExpNeg: Boolean, posExp: Int): Double =
    if (toExp(manExp, isExpNeg, posExp) > 0) {
      if (isNeg) Double.NegativeInfinity else Double.PositiveInfinity
    } else {
      if (isNeg) -0.0 else 0.0
    }

  private def toExp(manExp: Int, isExpNeg: Boolean, exp: Int): Int = manExp + (if (isExpNeg) -exp else exp)

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseBigDecimal(isToken: Boolean): java.math.BigDecimal = {
    var isZeroFirst = false
    var i = 0
    var state = 0
    var pos = ensureReusableCharsCapacity(i, head)
    while (pos < tail || {
      pos = ensureReusableCharsCapacity(i, loadMore(pos))
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
          return null
      }
      pos += 1
    }
    head = pos
    if (state == 3 || state == 4 || state == 6 || state == 9) toBigDecimal(i)
    else if (state == 13) null
    else numberError()
  }

  private def toBigDecimal(len: Int): java.math.BigDecimal =
    new java.math.BigDecimal(reusableChars, 0, len)

  private def numberError(pos: Int = head): Nothing = decodeError("illegal number", pos)

  private def leadingZeroError(pos: Int): Nothing = decodeError("illegal number with leading zero", pos)

  private def intOverflowError(pos: Int): Nothing = decodeError("value is too large for int", pos)

  private def longOverflowError(pos: Int): Nothing = decodeError("value is too large for long", pos)

  @tailrec
  private def parseString(i: Int = 0, pos: Int = ensureReusableCharsCapacity(0, head)): Int =
    if (pos < tail) {
      val b = buf(pos)
      if (b == '"') {
        head = pos + 1
        i
      } else if ((b ^ '\\') < 1) slowParseString(i, pos)
      else parseString(putCharAt(b.toChar, i), pos + 1)
    } else parseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))

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
                } else slowParseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))
              } else slowParseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))
            case _ => illegalEscapeSequenceError(pos + 1)
          }
        } else slowParseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))
      } else if ((b1 >> 5) == -2 && remaining > 1) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = buf(pos + 1)
        if (isMalformed2(b1, b2)) malformedBytes(b1, b2, pos)
        slowParseString(putCharAt(((b1 << 6) ^ (b2 ^ 0xF80)).toChar, i), pos + 2) // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2 && remaining > 2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = buf(pos + 1)
        val b3 = buf(pos + 2)
        val ch = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(ch)) malformedBytes(b1, b2, b3, pos)
        slowParseString(putCharAt(ch, i), pos + 3)
      } else if ((b1 >> 3) == -2 && remaining > 3) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = buf(pos + 1)
        val b3 = buf(pos + 2)
        val b4 = buf(pos + 3)
        val cp = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) // 0x381F80 == ((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(cp)) malformedBytes(b1, b2, b3, b4, pos)
        slowParseString(putCharAt(Character.lowSurrogate(cp), putCharAt(Character.highSurrogate(cp), i)), pos + 4)
      } else if (b1 < 0) malformedBytes(b1, pos)
      else slowParseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))
    } else slowParseString(i, ensureReusableCharsCapacity(i, loadMoreOrError(pos)))
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
    val sb = new StringBuilder("malformed byte(s): 0x")
    appendHex(b1, sb)
    decodeError(sb.toString, pos)
  }

  private def malformedBytes(b1: Byte, b2: Byte, pos: Int): Nothing = {
    val sb = new StringBuilder("malformed byte(s): 0x")
    appendHex(b1, sb)
    sb.append(", 0x")
    appendHex(b2, sb)
    decodeError(sb.toString, pos + 1)
  }

  private def malformedBytes(b1: Byte, b2: Byte, b3: Byte, pos: Int): Nothing = {
    val sb = new StringBuilder("malformed byte(s): 0x")
    appendHex(b1, sb)
    sb.append(", 0x")
    appendHex(b2, sb)
    sb.append(", 0x")
    appendHex(b3, sb)
    decodeError(sb.toString, pos + 2)
  }

  private def malformedBytes(b1: Byte, b2: Byte, b3: Byte, b4: Byte, pos: Int): Nothing = {
    val sb = new StringBuilder("malformed byte(s): 0x")
    appendHex(b1, sb)
    sb.append(", 0x")
    appendHex(b2, sb)
    sb.append(", 0x")
    appendHex(b3, sb)
    sb.append(", 0x")
    appendHex(b4, sb)
    decodeError(sb.toString, pos + 3)
  }

  @inline
  private def putCharAt(ch: Char, i: Int) = {
    reusableChars(i) = ch
    i + 1
  }

  private def ensureReusableCharsCapacity(i: Int, pos: Int): Int = {
    val required = tail - pos + i
    if (required > reusableChars.length) {
      val cs = new Array[Char](Math.max(reusableChars.length << 1, required))
      System.arraycopy(reusableChars, 0, cs, 0, reusableChars.length)
      reusableChars = cs
    }
    pos
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
      val n = in.read(buf, remaining, buf.length - remaining)
      if (n > 0) {
        tail = remaining + n
        totalRead += n
        0
      } else endOfInput()
    }

  private def loadMore(pos: Int): Int =
    if (in eq null) pos
    else {
      val n = in.read(buf)
      if (n > 0) {
        tail = n
        totalRead += n
        0
      } else pos
    }

  private def endOfInput(): Nothing = decodeError("unexpected end of input", tail)
}

object JsonReader {
  private val pool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader()

    override def get(): JsonReader = {
      val reader = super.get()
      if (reader.reusableChars.length > 16384) reader.reusableChars = new Array[Char](16384)
      reader // FIXME: reset too long reusableChars on exit from read() instead
    }
  }
  private val pow10: Array[Double] = // all powers of 10 that can be represented exactly in double/float
    Array(1, 1e+01, 1e+02, 1e+03, 1e+04, 1e+05, 1e+06, 1e+07, 1e+08, 1e+09, 1e+10,
      1e+11, 1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21)

  final def read[A](codec: JsonCodec[A], in: InputStream): A = {
    if (codec eq null) throw new JsonException("codec should be not null")
    if (in eq null) throw new JsonException("in should be not null")
    val reader = pool.get
    reader.in = in
    reader.head = 0
    reader.tail = 0
    reader.totalRead = 0
    try codec.decode(reader)
    finally reader.in = null // to help GC, and to avoid modifying of supplied for parsing Array[Byte]
  }

  final def read[A](codec: JsonCodec[A], buf: Array[Byte]): A = {
    if (buf eq null) throw new JsonException("buf should be non empty")
    read(codec, buf, 0, buf.length)
  }

  final def read[A](codec: JsonCodec[A], buf: Array[Byte], from: Int, to: Int): A = {
    if (codec eq null) throw new JsonException("codec should be not null")
    if ((buf eq null) || buf.length == 0) throw new JsonException("buf should be non empty")
    if (to < 0 || to > buf.length) throw new JsonException("to should be positive and not greater than buf length")
    if (from < 0 || from > to) throw new JsonException("from should be positive and not greater than to")
    val reader = pool.get
    val currBuf = reader.buf
    reader.buf = buf
    reader.head = from
    reader.tail = to
    reader.totalRead = 0
    try codec.decode(reader)
    finally reader.buf = currBuf
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

  private[jsoniter_scala] def appendHexDump(buf: Array[Byte], from: Int, to: Int, offset: Int, sb: StringBuilder): Unit = {
    val hexCodes = new StringBuilder(48)
    val chars = new StringBuilder(16)
    val alignedAbsFrom = (from + offset) & -16
    val alignedAbsTo = (to + offset + 15) & -16
    val len = alignedAbsTo - alignedAbsFrom
    val bufOffset = alignedAbsFrom - offset
    sb.append("\n           +-------------------------------------------------+")
    sb.append("\n           |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |")
    sb.append("\n+----------+-------------------------------------------------+------------------+")
    var i = 0
    while (i < len) {
      val pos = bufOffset + i
      if (pos >= from && pos < to) {
        val b = buf(pos)
        appendHex(b, hexCodes)
        hexCodes.append(' ')
        chars.append(if (b <= 31 || b >= 127) '.' else b.toChar)
      } else {
        hexCodes.append("   ")
        chars.append(' ')
      }
      if ((i & 15) == 15) {
        sb.append("\n| ")
        appendHex(alignedAbsFrom + i - 15, sb)
        sb.append(" | ").append(hexCodes).append("| ").append(chars).append(" |")
        hexCodes.setLength(0)
        chars.setLength(0)
      }
      i += 1
    }
    sb.append("\n+----------+-------------------------------------------------+------------------+")
  }

  private def appendHex(d: Int, sb: StringBuilder): Unit =
    sb.append(toHexDigit(d >>> 28)).append(toHexDigit(d >>> 24))
      .append(toHexDigit(d >>> 20)).append(toHexDigit(d >>> 16))
      .append(toHexDigit(d >>> 12)).append(toHexDigit(d >>> 8))
      .append(toHexDigit(d >>> 4)).append(toHexDigit(d))

  private def appendHex(b: Byte, sb: StringBuilder): Unit = sb.append(toHexDigit(b >>> 4)).append(toHexDigit(b))

  private def toHexDigit(n: Int): Char = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toChar
}