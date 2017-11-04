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
        sb.append(if (sb.isEmpty) "missing required field(s) " else ", ").append('"').append(reqFields(i)).append('"')
      }
      i += 1
    }
    decodeError(sb.toString())
  }

  def readObjectFieldAsString(): String = {
    readParentheses()
    val x = reusableCharsToString(parseString(0))
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
    else {
      readParenthesesWithColon()
      x.toByte
    }
  }

  def readObjectFieldAsChar(): Char = {
    readParentheses()
    val len = parseString(0)
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
    val x = new BigInt(parseBigDecimal(isToken = false).toBigInteger)
    readParenthesesWithColon()
    x
  }

  def readObjectFieldAsBigDecimal(): BigDecimal = {
    readParentheses()
    val x = new BigDecimal(parseBigDecimal(isToken = false))
    readParenthesesWithColon()
    x
  }

  def readByte(): Byte = {
    val x = parseInt(isToken = true)
    if (x > Byte.MaxValue || x < Byte.MinValue) decodeError("value is too large for byte")
    x.toByte
  }

  def readChar(): Char = {
    readParentheses()
    val len = parseString(0)
    val x = reusableChars(0)
    if (len != 1) decodeError("illegal value for char")
    x
  }

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
    if (b == '"') reusableCharsToString(parseString(0))
    else if (b == 'n') parseNull(default)
    else decodeError("expected string value or `null`")
  }

  def readBoolean(): Boolean = parseBoolean(isToken = true)

  def parseNull[A](default: A): A =
    if (nextByte() == 'u' && nextByte() == 'l' && nextByte() == 'l') default
    else decodeError("expected value or `null`")

  def readObjectFieldAsReusableChars(): Int = {
    if (nextToken() != '"') decodeError("expected `\"`")
    val x = parseString(0)
    readColon()
    x
  }

  @tailrec
  def nextToken(): Byte = {
    var pos = head
    while (pos < tail) {
      val b = buf(pos)
      pos += 1
      if (b != ' ' && b != '\n' && b != '\t' && b != '\r') {
        head = pos
        return b
      }
    }
    if (loadMore(pos)) nextToken()
    else decodeError("unexpected end of input")
  }

  def reusableCharsToHashCode(len: Int): Int = toHashCode(reusableChars, len)

  def isReusableCharsEqualsTo(len: Int, s: String): Boolean =
    if (len != s.length) false
    else {
      var i = 0
      while (i < len) {
        if (reusableChars(i) != s.charAt(i)) return false
        i += 1
      }
      true
    }

  def skip(): Unit = (nextToken(): @switch) match {
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
    case 'n' => skipFixedBytes(3) // TODO: consider replace skipping of bytes by parsing of 'null'
    case 't' => skipFixedBytes(3) // TODO: consider skipping of bytes by parsing of 'true'
    case 'f' => skipFixedBytes(4) // TODO: consider skipping of bytes by parsing of 'false'
    case '{' => skipNested('{', '}')
    case '[' => skipNested('[', ']')
    case _ => decodeError("expected value")
  }

  def unreadByte(): Unit = head -= 1

  def arrayStartError(): Nothing = decodeError("expected `[` or `null`")

  def objectStartError(): Nothing = decodeError("expected `{` or `null`")

  def decodeError(msg: String): Nothing = {
    val from = Math.max((head - 32) & -16, 0)
    val to = Math.min((head + 48) & -16, buf.length)
    val sb = new StringBuilder(1024).append(msg).append(", offset: ")
    val offset = if (in eq null) 0 else totalRead - tail
    appendHex(offset + head - 1, sb) // TODO: consider supporting offset values beyond 2Gb
    sb.append(", buf:")
    appendHexDump(buf, from, to, offset, sb)
    throw new JsonException(sb.toString)
  }

  private def reusableCharsToString(len: Int): String = new String(reusableChars, 0, len)

  private def readParentheses(): Unit = if (nextByte() != '"') decodeError("expected `\"`")

  private def readParenthesesWithColon(): Unit =
    if (nextByte() != '"') decodeError("expected `\"`")
    else readColon()

  private def readColon(): Unit = if (nextToken() != ':') decodeError("expected `:`")

  private def nextByte(): Byte = {
    var pos = head
    if (pos == tail) {
      if (loadMore(pos)) pos = 0
      else decodeError("unexpected end of input")
    }
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

  private def booleanError(): Nothing = decodeError("illegal boolean")

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseInt(isToken: Boolean): Int = {
    var b = if (isToken) nextToken() else nextByte()
    val negative = b == '-'
    if (negative) b = nextByte()
    if (b >= '0' && b <= '9') {
      var v = '0' - b
      var pos = 0
      do {
        pos = head
        while (pos < tail && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) pos = {
          if (v == 0) numberError()
          if (v < -214748364) intOverflowError()
          v = v * 10 + ('0' - b)
          if (v >= 0) intOverflowError()
          pos + 1
        }
      } while (loadMore(pos))
      if (negative) v
      else if (v == Int.MinValue) intOverflowError()
      else -v
    } else {
      unreadByte()
      numberError()
    }
  }

  private def intOverflowError(): Nothing = decodeError("value is too large for int")

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseLong(isToken: Boolean): Long = {
    var b = if (isToken) nextToken() else nextByte()
    val negative = b == '-'
    if (negative) b = nextByte()
    if (b >= '0' && b <= '9') {
      var v: Long = '0' - b
      var pos = 0
      do {
        pos = head
        while (pos < tail && {
          b = buf(pos)
          b >= '0' && b <= '9'
        }) pos = {
          if (v == 0) numberError()
          if (v < -922337203685477580L) longOverflowError()
          v = v * 10 + ('0' - b)
          if (v >= 0) longOverflowError()
          pos + 1
        }
      } while (loadMore(pos))
      if (negative) v
      else if (v == Long.MinValue) longOverflowError()
      else -v
    } else {
      unreadByte()
      numberError()
    }
  }

  private def longOverflowError(): Nothing = decodeError("value is too large for long")

  // TODO: consider fast path with unrolled loop for small numbers
  private def parseDouble(isToken: Boolean): Double = {
    var posMan = 0L
    var manExp = 0
    var posExp = 0
    var isNeg = false
    var isExpNeg = false
    var isZeroFirst = false
    var i = 0
    var pos = 0
    var state = 0
    do {
      pos = ensureSizeOfReusableChars(i)
      while (pos < tail) {
        val ch = buf(pos).toChar
        (state: @switch) match {
          case 0 => // start
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              if (!isToken) numberError()
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
            } else numberError()
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
            } else numberError()
          case 2 => // signum
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(ch, i)
              posMan = ch - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else numberError()
          case 3 => // first int digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(ch, i)
              posMan = posMan * 10 + (ch - '0')
              if (isZeroFirst) numberError()
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
            } else numberError()
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
            } else numberError()
          case 8 => // exp. sign
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(ch, i)
              posExp = ch - '0'
              state = 9
            } else numberError()
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
    } while (loadMore(pos))
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
    var pos = 0
    var state = 0
    do {
      pos = ensureSizeOfReusableChars(i)
      while (pos < tail) {
        val ch = buf(pos).toChar
        (state: @switch) match {
          case 0 => // start
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              if (!isToken) numberError()
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
            } else numberError()
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
            } else numberError()
          case 2 => // signum
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(ch, i)
              isZeroFirst = ch == '0'
              state = 3
            } else numberError()
          case 3 => // first int digit
            if (ch >= '0' && ch <= '9') {
              if (isZeroFirst) numberError()
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
            } else numberError()
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
            } else numberError()
          case 8 => // exp. sign
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(ch, i)
              state = 9
            } else numberError()
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
            else numberError()
          case 11 => // nu'l'l
            if (ch == 'l') state = 12
            else numberError()
          case 12 => // nul'l'
            if (ch == 'l') state = 13
            else numberError()
          case 13 => // null
            head = pos
            return null
        }
        pos += 1
      }
    } while (loadMore(pos))
    if (state == 3 || state == 4 || state == 6 || state == 9) toBigDecimal(i)
    else if (state == 13) null
    else numberError()
  }

  private def toBigDecimal(len: Int): java.math.BigDecimal =
    new java.math.BigDecimal(reusableChars, 0, len)

  private def numberError(): Nothing = decodeError("illegal number")

  @tailrec
  private def parseString(from: Int): Int = {
    var i = from
    var pos = ensureSizeOfReusableChars(from)
    while (pos < tail) i = {
      val b = buf(pos)
      pos += 1
      if (b == '"') {
        head = pos
        return i
      } else if ((b ^ '\\') < 1) {
        head = pos - 1
        return slowParseString(i)
      }
      putCharAt(b.toChar, i)
    }
    if (loadMore(pos)) parseString(i)
    else decodeError("unexpected end of input")
  }

  private def slowParseString(from: Int): Int = {
    var i = from
    var b1: Byte = 0
    while ({
      b1 = nextByte()
      b1 != '"'
    }) i = {
      ensureSizeOfReusableChars(i + 2) // +2 for surrogate pair case
      if (b1 >= 0) { // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') putCharAt(b1.toChar, i)
        else readEscapeSequence(i)
      } else if ((b1 >> 5) == -2) { // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = nextByte()
        if (isMalformed2(b1, b2)) malformedBytes(b1, b2)
        putCharAt(((b1 << 6) ^ (b2 ^ 0xF80)).toChar, i) // 0xF80 == ((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) { // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = nextByte()
        val b3 = nextByte()
        val ch = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar // 0xFFFE1F80 == ((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(ch)) malformedBytes(b1, b2, b3)
        putCharAt(ch, i)
      } else if ((b1 >> 3) == -2) { // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = nextByte()
        val b3 = nextByte()
        val b4 = nextByte()
        val cp = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) // 0x381F80 == ((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(cp)) malformedBytes(b1, b2, b3, b4)
        putCharAt(Character.lowSurrogate(cp), putCharAt(Character.highSurrogate(cp), i))
      } else malformedBytes(b1)
    }
    i
  }

  private def readEscapeSequence(i: Int): Int = (nextByte(): @switch) match {
    case 'b' => putCharAt('\b', i)
    case 'f' => putCharAt('\f', i)
    case 'n' => putCharAt('\n', i)
    case 'r' => putCharAt('\r', i)
    case 't' => putCharAt('\t', i)
    case '"' => putCharAt('"', i)
    case '/' => putCharAt('/', i)
    case '\\' => putCharAt('\\', i)
    case 'u' =>
      val ch1 = readEscapedUnicode()
      if (ch1 < 2048) putCharAt(ch1, i)
      else if (!Character.isHighSurrogate(ch1)) {
        if (Character.isLowSurrogate(ch1)) decodeError("expected high surrogate character")
        putCharAt(ch1, i)
      } else if (nextByte() == '\\' && nextByte() == 'u') {
        val ch2 = readEscapedUnicode()
        if (!Character.isLowSurrogate(ch2)) decodeError("expected low surrogate character")
        putCharAt(ch2, putCharAt(ch1, i))
      } else decodeError("illegal escape sequence")
    case _ => decodeError("illegal escape sequence")
  }

  private def readEscapedUnicode(): Char =
    ((fromHexDigit(nextByte()) << 12) +
      (fromHexDigit(nextByte()) << 8) +
      (fromHexDigit(nextByte()) << 4) +
      fromHexDigit(nextByte())).toChar

  private def fromHexDigit(b: Byte): Int =
    if (b >= '0' && b <= '9') b - 48
    else {
      val b1 = b & -33
      if (b1 >= 'A' && b1 <= 'F') b1 - 55
      else decodeError("expected hex digit")
    }

  private def isMalformed2(b1: Byte, b2: Byte): Boolean =
    (b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80

  private def isMalformed3(b1: Byte, b2: Byte, b3: Byte): Boolean =
    (b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80

  private def isMalformed4(b2: Byte, b3: Byte, b4: Byte): Boolean =
    (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80

  private def malformedBytes(bytes: Byte*): Nothing = {
    val sb = new StringBuilder("malformed byte(s): ")
    var comma = false
    bytes.foreach { b =>
      appendHex(b, if (comma) sb.append(", ") else {
        comma = true
        sb
      })
    }
    decodeError(sb.toString)
  }

  @inline
  private def putCharAt(ch: Char, i: Int) = {
    reusableChars(i) = ch
    i + 1
  }

  @inline
  private def ensureSizeOfReusableChars(i: Int): Int = {
    val pos = head
    val required = tail - pos + i
    if (required > reusableChars.length) growReusableChars(required)
    pos
  }

  private def growReusableChars(required: Int): Unit = {
    val cs = new Array[Char](Math.max(reusableChars.length << 1, required))
    System.arraycopy(reusableChars, 0, cs, 0, reusableChars.length)
    reusableChars = cs
  }

  private def skipString(): Unit = {
    var evenBackSlashes = true
    var pos = 0
    do {
      pos = head
      while (pos < tail) {
        val b = buf(pos)
        pos += 1
        if (b == '"' && evenBackSlashes) {
          head = pos
          return
        } else if (b == '\\') evenBackSlashes = !evenBackSlashes
        else evenBackSlashes = true
      }
    } while (loadMore(pos))
  }

  private def skipNumber(): Unit = {
    var pos = 0
    do {
      pos = head
      while (pos < tail) {
        val b = buf(pos)
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+' || b == 'e' || b == 'E') pos += 1
        else {
          head = pos
          return
        }
      }
    } while (loadMore(pos))
  }

  private def skipNested(opening: Byte, closing: Byte): Unit = {
    var level = 1
    var pos = 0
    do {
      pos = head
      while (pos < tail) {
        val b = buf(pos)
        pos += 1
        if (b == '"') {
          head = pos
          skipString()
          pos = head
        } else if (b == closing) {
          level -= 1
          if (level == 0) {
            head = pos
            return
          }
        } else if (b == opening) level += 1
      }
    } while (loadMore(pos))
  }

  @tailrec
  private def skipFixedBytes(n: Int): Unit = {
    val pos = head + n
    val diff = pos - tail
    if (diff <= 0) head = pos
    else if (loadMore(tail)) skipFixedBytes(diff)
    else decodeError("unexpected end of input")
  }

  @inline
  private def loadMore(pos: Int): Boolean =
    if (in eq null) {
      head = pos
      false
    } else realLoadMore(pos)

  private def realLoadMore(pos: Int): Boolean = {
    val n = in.read(buf)
    if (n > 0) {
      head = 0
      tail = n
      totalRead += n
      true
    } else {
      head = pos
      false
    }
  }

  private def appendHexDump(buf: Array[Byte], from: Int, to: Int, offset: Int, sb: StringBuilder): Unit = {
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
        chars.append(if (b < 32 || b > 126) '.' else b.toChar)
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

  private def appendHex(b: Byte, sb: StringBuilder): Unit =
    sb.append(toHexDigit(b >>> 4)).append(toHexDigit(b))

  private def toHexDigit(n: Int): Char = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 39) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toChar
}

object JsonReader {
  private val pool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader = new JsonReader()

    override def get(): JsonReader = {
      val reader = super.get()
      if (reader.reusableChars.length > 16384) reader.reusableChars = new Array[Char](16384)
      reader
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
    codec.decode(reader)
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
}
