package com.github.plokhotnyuk.jsoniter_scala

import java.io.InputStream
import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.JsonReader._

import scala.annotation.{switch, tailrec}

class JsonException(message: String) extends RuntimeException(message)

//noinspection EmptyCheck
final class JsonReader private[jsoniter_scala](
    private var buf: Array[Byte],
    private var head: Int,
    private var tail: Int,
    private var reusableChars: Array[Char],
    private var in: InputStream) {
  require((buf ne null) && buf.length > 0, "buf should be non empty")
  require((reusableChars ne null) && reusableChars.length > 0, "reusableChars should be non empty")

  def reset(in: InputStream): Unit = {
    this.in = in
    head = 0
    tail = 0
  }

  def reset(buf: Array[Byte]): Array[Byte] = {
    val currBuf = this.buf
    this.buf = buf
    this.in = null
    head = 0
    tail = buf.length
    currBuf
  }

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
    else decodeError("expect string or null")
  }

  def readBoolean(): Boolean = parseBoolean(isToken = true)

  def parseNull[A](default: A): A =
    if (nextByte() == 'u' && nextByte() == 'l' && nextByte() == 'l') default
    else decodeError("unexpected value")

  def readObjectFieldAsReusableChars(): Int = {
    if (nextToken() != '"') decodeError("expect \"")
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

  def skip(): Unit = {
    val b = nextToken()
    if (b == '"') skipString()
    else if ((b >= '0' && b <= '9') || b == '-') skipNumber()
    else if (b == 'n' || b == 't') skipFixedBytes(3)
    else if (b == 'f') skipFixedBytes(4)
    else if (b == '{') skipObject()
    else if (b == '[') skipArray()
    else decodeError("expect JSON value")
  }

  def unreadByte(): Unit = head -= 1

  def decodeError(msg: String): Nothing = {
    var peekStart = head - 10
    if (peekStart < 0) peekStart = 0
    var peekSize = head - peekStart
    if (head > tail) peekSize = tail - peekStart
    val peek = new String(buf, peekStart, peekSize, UTF_8)
    throw new JsonException(msg + ", head: " + head + ", peek: " + peek + ", buf: " + new String(buf, UTF_8))
  }

  private def reusableCharsToString(len: Int): String = new String(reusableChars, 0, len)

  private def readParentheses(): Unit =
    if (nextByte() != '"') decodeError("expect \"")

  private def readParenthesesWithColon(): Unit =
    if (nextByte() != '"') decodeError("expect \"")
    else readColon()

  private def readColon(): Unit = if (nextToken() != ':') decodeError("expect :")

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
      else decodeError("illegal boolean")
    } else if (b == 'f') {
      if (nextByte() == 'a' && nextByte() == 'l' && nextByte() == 's' && nextByte() == 'e') false
      else decodeError("illegal boolean")
    } else decodeError("illegal boolean")
  }

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
          if (v == 0) decodeError("illegal number")
          if (v < -214748364) decodeError("value is too large for int")
          v = v * 10 + ('0' - b)
          if (v >= 0) decodeError( "value is too large for int")
          pos + 1
        }
      } while (loadMore(pos))
      if (negative) v
      else if (v == Int.MinValue) decodeError("value is too large for int")
      else -v
    } else {
      unreadByte()
      numberError()
    }
  }

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
          if (v == 0) decodeError("illegal number")
          if (v < -922337203685477580L) decodeError("value is too large for long")
          v = v * 10 + ('0' - b)
          if (v >= 0) decodeError( "value is too large for long")
          pos + 1
        }
      } while (loadMore(pos))
      if (negative) v
      else if (v == Long.MinValue) decodeError("value is too large for long")
      else -v
    } else {
      unreadByte()
      numberError()
    }
  }

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
      pos = head
      while (pos < tail) {
        val ch = buf(pos).toChar
        (state: @switch) match {
          case 0 => // start
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              if (!isToken) numberError()
              state = 1
            } else if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posMan = ch - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else if (ch == '-') {
              i = putCharAt(i, ch)
              isNeg = true
              state = 2
            } else numberError()
          case 1 => // whitespaces
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              state = 1
            } else if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posMan = ch - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else if (ch == '-') {
              i = putCharAt(i, ch)
              isNeg = true
              state = 2
            } else numberError()
          case 2 => // signum
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posMan = ch - '0'
              isZeroFirst = posMan == 0
              state = 3
            } else numberError()
          case 3 => // first int digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posMan = posMan * 10 + (ch - '0')
              if (isZeroFirst) numberError()
              state = 4
            } else if (ch == '.') {
              i = putCharAt(i, ch)
              state = 5
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
            }
          case 4 => // int digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              if (posMan < 922337203685477580L) posMan = posMan * 10 + (ch - '0')
              else manExp += 1
              state = 4
            } else if (ch == '.') {
              i = putCharAt(i, ch)
              state = 5
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
            }
          case 5 => // dot
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              if (posMan < 922337203685477580L) {
                posMan = posMan * 10 + (ch - '0')
                manExp -= 1
              }
              state = 6
            } else numberError()
          case 6 => // frac digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              if (posMan < 922337203685477580L) {
                posMan = posMan * 10 + (ch - '0')
                manExp -= 1
              }
              state = 6
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
            }
          case 7 => // e
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posExp = ch - '0'
              state = 9
            } else if (ch == '-' || ch == '+') {
              i = putCharAt(i, ch)
              isExpNeg = ch == '-'
              state = 8
            } else numberError()
          case 8 => // exp. sign
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posExp = ch - '0'
              state = 9
            } else numberError()
          case 9 => // exp. digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              posExp = posExp * 10 + (ch - '0')
              state = if (Math.abs(toExp(manExp, isExpNeg, posExp)) > 214748364) 10 else 9
            } else {
              head = pos
              return toDouble(isNeg, posMan, manExp, isExpNeg, posExp, i)
            }
          case 10 => // exp. digit overflow
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
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

  private def toDouble(isNeg: Boolean, posMan: Long, manExp: Int, isExpNeg: Boolean, posExp: Int,
                       j: Int): Double = {
    val man = if (isNeg) -posMan else posMan
    val exp = toExp(manExp, isExpNeg, posExp)
    if (exp == 0) man
    else {
      val maxExp = pow10.length
      if (exp >= -maxExp && exp < 0) man / pow10(-exp)
      else if (exp > 0 && exp <= maxExp) man * pow10(exp)
      else java.lang.Double.parseDouble(reusableCharsToString(j))
    }
  }

  private def toExpOverflowDouble(isNeg: Boolean, manExp: Int, isExpNeg: Boolean, posExp: Int): Double =
    if (toExp(manExp, isExpNeg, posExp) > 0) {
      if (isNeg) Double.NegativeInfinity else Double.PositiveInfinity
    } else {
      if (isNeg) -0.0 else 0.0
    }

  private def toExp(manExp: Int, isExpNeg: Boolean, exp: Int): Int = manExp + (if (isExpNeg) -exp else exp)

  private def parseBigDecimal(isToken: Boolean): java.math.BigDecimal = {
    var isZeroFirst = false
    var i = 0
    var pos = 0
    var state = 0
    do {
      pos = head
      while (pos < tail) {
        val ch = buf(pos).toChar
        (state: @switch) match {
          case 0 => // start
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              if (!isToken) numberError()
              state = 1
            } else if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              isZeroFirst = ch == '0'
              state = 3
            } else if (ch == '-') {
              i = putCharAt(i, ch)
              state = 2
            } else if (ch == 'n' && isToken) {
              state = 10
            } else numberError()
          case 1 => // whitespaces
            if (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r') {
              state = 1
            } else if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              isZeroFirst = ch == '0'
              state = 3
            } else if (ch == '-') {
              i = putCharAt(i, ch)
              state = 2
            } else if (ch == 'n' && isToken) {
              state = 10
            } else numberError()
          case 2 => // signum
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              isZeroFirst = ch == '0'
              state = 3
            } else numberError()
          case 3 => // first int digit
            if (ch >= '0' && ch <= '9') {
              if (isZeroFirst) numberError()
              i = putCharAt(i, ch)
              state = 4
            } else if (ch == '.') {
              i = putCharAt(i, ch)
              state = 5
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toBigDecimal(i)
            }
          case 4 => // int digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              state = 4
            } else if (ch == '.') {
              i = putCharAt(i, ch)
              state = 5
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toBigDecimal(i)
            }
          case 5 => // dot
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              state = 6
            } else numberError()
          case 6 => // frac digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              state = 6
            } else if (ch == 'e' || ch == 'E') {
              i = putCharAt(i, ch)
              state = 7
            } else {
              head = pos
              return toBigDecimal(i)
            }
          case 7 => // e
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              state = 9
            } else if (ch == '-' || ch == '+') {
              i = putCharAt(i, ch)
              state = 8
            } else numberError()
          case 8 => // exp. sign
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
              state = 9
            } else numberError()
          case 9 => // exp. digit
            if (ch >= '0' && ch <= '9') {
              i = putCharAt(i, ch)
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
    var pos = head
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
      putCharAt(i, b.toChar)
    }
    if (loadMore(pos)) parseString(i)
    else decodeError("unexpected end of input")
  }

  private def slowParseString(pos: Int): Int = {
    var j: Int = pos
    var b1: Byte = 0
    while ({
      b1 = nextByte()
      b1 != '"'
    }) j = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') putCharAt(j, b1.toChar)
        else parseEscapeSequence(j)
      } else if ((b1 >> 5) == -2) {
        // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = nextByte()
        if (isMalformed2(b1, b2)) malformedBytes(b1, b2)
        putCharAt(j, ((b1 << 6) ^ (b2 ^ 0xF80)).toChar) //((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) {
        // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = nextByte()
        val b3 = nextByte()
        val c = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar //((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(c)) malformedBytes(b1, b2, b3)
        putCharAt(j, c)
      } else if ((b1 >> 3) == -2) {
        // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = nextByte()
        val b3 = nextByte()
        val b4 = nextByte()
        val uc = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) //((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(uc)) malformedBytes(b1, b2, b3, b4)
        putCharAt(putCharAt(j, Character.highSurrogate(uc)), Character.lowSurrogate(uc))
      } else malformedBytes(b1)
    }
    j
  }

  private def parseEscapeSequence(pos: Int): Int =
    (nextByte(): @switch) match {
      case 'b' => putCharAt(pos, '\b')
      case 'f' => putCharAt(pos, '\f')
      case 'n' => putCharAt(pos, '\n')
      case 'r' => putCharAt(pos, '\r')
      case 't' => putCharAt(pos, '\t')
      case '"' => putCharAt(pos, '"')
      case '/' => putCharAt(pos, '/')
      case '\\' => putCharAt(pos, '\\')
      case 'u' =>
        val c1 = readHexDigitPresentedChar()
        if (c1 < 2048) putCharAt(pos, c1)
        else if (!Character.isHighSurrogate(c1)) {
          if (Character.isLowSurrogate(c1)) decodeError("expect high surrogate character")
          putCharAt(pos, c1)
        } else if (nextByte() == '\\' && nextByte() == 'u') {
          val c2 = readHexDigitPresentedChar()
          if (!Character.isLowSurrogate(c2)) decodeError("expect low surrogate character")
          putCharAt(putCharAt(pos, c1), c2)
        } else decodeError("invalid escape sequence")
      case _ => decodeError("invalid escape sequence")
    }

  @inline
  private def putCharAt(pos: Int, ch: Char): Int = {
    ensureSizeOfReusableChars(pos)
    reusableChars(pos) = ch
    pos + 1
  }

  @inline
  private def ensureSizeOfReusableChars(pos: Int): Unit = if (pos >= reusableChars.length) growReusableChars(pos)

  private def growReusableChars(pos: Int): Unit = {
    val cs = new Array[Char](pos << 1)
    System.arraycopy(reusableChars, 0, cs, 0, reusableChars.length)
    reusableChars = cs
  }

  private def readHexDigitPresentedChar(): Char =
    ((fromHexDigit(nextByte()) << 12) +
      (fromHexDigit(nextByte()) << 8) +
      (fromHexDigit(nextByte()) << 4) +
      fromHexDigit(nextByte())).toChar

  private def isMalformed2(b1: Byte, b2: Byte): Boolean =
    (b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80

  private def isMalformed3(b1: Byte, b2: Byte, b3: Byte): Boolean =
    (b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80

  private def isMalformed4(b2: Byte, b3: Byte, b4: Byte): Boolean =
    (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80

  private def malformedBytes(bytes: Byte*): Nothing = {
    val sb = new StringBuilder("malformed byte(s): ")
    var first = true
    bytes.foreach { b =>
      (if (first) {
        first = false
        sb.append("0x")
      } else sb.append(", 0x")).append(toHexDigit(b >>> 4)).append(toHexDigit(b))
    }
    decodeError(sb.toString)
  }

  private def fromHexDigit(b: Byte): Int = {
    if (b >= '0' && b <= '9') b - 48
    else {
      val b1 = b & -33
      if (b1 >= 'A' && b1 <= 'F') b1 - 55
      else decodeError("expect hex digit character")
    }
  }

  private def toHexDigit(n: Int): Char = {
    val nibble = n & 15
    (((9 - nibble) >> 31) & 7) + nibble + 48 // branchless conversion of nibble to hex digit
  }.toChar

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
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+' || b == 'e' ||  b == 'E') pos += 1
        else {
          head = pos
          return
        }
      }
    } while (loadMore(pos))
  }

  private def skipArray(): Unit = {
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
        } else if (b == '[') level += 1
        else if (b == ']') {
          level -= 1
          if (level == 0) {
            head = pos
            return
          }
        }
      }
    } while (loadMore(pos))
  }

  private def skipObject(): Unit = {
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
        } else if (b == '{') level += 1
        else if (b == '}') {
          level -= 1
          if (level == 0) {
            head = pos
            return
          }
        }
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
      true
    } else {
      head = pos
      false
    }
  }
}

object JsonReader {
  private val pool: ThreadLocal[JsonReader] = new ThreadLocal[JsonReader] {
    override def initialValue(): JsonReader =
      new JsonReader(new Array[Byte](8192), 0, 0, new Array[Char](1024), null)

    override def get(): JsonReader = {
      val reader = super.get()
      if (reader.reusableChars.length > 32768) reader.reusableChars = new Array[Char](1024)
      reader
    }
  }
  private val pow10: Array[Double] = // all powers of 10 that can be represented exactly in double/float
    Array(1, 1e+01, 1e+02, 1e+03, 1e+04, 1e+05, 1e+06, 1e+07, 1e+08, 1e+09, 1e+10,
      1e+11, 1e+12, 1e+13, 1e+14, 1e+15, 1e+16, 1e+17, 1e+18, 1e+19, 1e+20, 1e+21)

  final def read[A](codec: JsonCodec[A], in: InputStream): A = {
    val reader = pool.get
    reader.reset(in)
    codec.decode(reader)
  }

  final def read[A](codec: JsonCodec[A], buf: Array[Byte]): A = {
    val reader = pool.get
    val currBuf = reader.reset(buf)
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
