package com.jsoniter

import java.io.{IOException, InputStream, OutputStream}

import com.jsoniter.output.{JsonStream, JsonStreamUtil}
import com.jsoniter.spi.{Config, Decoder, Encoder, JsoniterSpi}
import com.jsoniter.spi.JsoniterSpi._

import scala.annotation.tailrec

abstract class CodecBase[A](implicit m: Manifest[A]) extends Encoder with Decoder {
  private val cls = m.runtimeClass.asInstanceOf[Class[A]]

  JsoniterSpi.setDefaultConfig((new Config.Builder).escapeUnicode(false).build)
  addNewEncoder(getCurrentConfig.getEncoderCacheKey(cls), this)
  addNewDecoder(getCurrentConfig.getDecoderCacheKey(cls), this)

  final def read(in: InputStream): A = {
    val it = JsonIteratorUtil.pool.get
    JsonIterator.enableStreamingSupport()
    it.in = in
    it.head = 0
    it.tail = 0
    decode(it).asInstanceOf[A]
  }

  final def read(buf: Array[Byte]): A = {
    val it = JsonIteratorUtil.pool.get
    val currBuf = it.buf
    it.reset(buf)
    try decode(it).asInstanceOf[A]
    finally it.buf = currBuf
  }

  final def write(obj: A, out: OutputStream): Unit = {
    val stream = JsonStreamUtil.pool.get
    stream.reset(out)
    try encode(obj, stream)
    finally stream.flush()
  }

  final def write(obj: A): Array[Byte] = {
    val stream = JsonStreamUtil.pool.get
    stream.reset(null)
    encode(obj, stream)
    JsonStreamUtil.getBytes(stream)
  }

  protected def reqFieldError(in: JsonIterator, reqFields: Array[String], reqs: Long*): Nothing = {
    val sb = new StringBuilder(64)
    val l = reqFields.length
    var i = 0
    while (i < l) {
      if ((reqs(i >> 6) & (1L << i)) != 0) {
        sb.append(if (sb.isEmpty) "missing required field(s) " else ", ").append('"').append(reqFields(i)).append('"')
      }
      i += 1
    }
    decodeError(in, sb.toString())
  }

  protected def decodeError(in: JsonIterator, msg: String): Nothing = CodecBase.decodeError(in, msg)

  protected def encodeError(msg: String): Nothing = CodecBase.encodeError(msg)

  protected def readObjectFieldAsString(in: JsonIterator): String = {
    readParentheses(in)
    val x = CodecBase.parseString(in, 0)
    if (CodecBase.nextToken(in) != ':') decodeError(in, "expect :")
    x
  }

  protected def readObjectFieldAsBoolean(in: JsonIterator): Boolean = {
    readParentheses(in)
    val x = in.readBoolean()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsInt(in: JsonIterator): Int = {
    readParentheses(in)
    val x = CodecBase.readInt(in)
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsLong(in: JsonIterator): Long = {
    readParentheses(in)
    val x = CodecBase.readLong(in)
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsFloat(in: JsonIterator): Float = {
    readParentheses(in)
    val len = CodecBase.parseNumber(in, isToken = false)
    val x = try java.lang.Float.parseFloat(new String(in.reusableChars, 0, len)) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsDouble(in: JsonIterator): Double = {
    readParentheses(in)
    val len = CodecBase.parseNumber(in, isToken = false)
    val x = try java.lang.Double.parseDouble(new String(in.reusableChars, 0, len)) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsBigInt(in: JsonIterator): BigInt = {
    readParentheses(in)
    val len = CodecBase.parseNumber(in, isToken = false)
    val x = try new BigInt(new java.math.BigDecimal(in.reusableChars, 0, len).toBigInteger) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsBigDecimal(in: JsonIterator): BigDecimal = {
    readParentheses(in)
    val len = CodecBase.parseNumber(in, isToken = false)
    val x = try new BigDecimal(new java.math.BigDecimal(in.reusableChars, 0, len)) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
    readParenthesesWithColon(in)
    x
  }

  protected def writeSep(out: JsonStream, first: Boolean): Boolean = {
    if (first) out.writeIndention()
    else out.writeMore()
    false
  }

  protected def writeObjectField(out: JsonStream, x: String): Unit =
    if (x ne null) out.writeObjectField(x)
    else encodeError("key cannot be null")

  protected def writeObjectField(out: JsonStream, x: Boolean): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: Int): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: Long): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: Float): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: Double): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: BigInt): Unit =
    if (x ne null) {
      writeParentheses(out)
      out.writeRaw(x.toString)
      writeParenthesesWithColon(out)
    } else encodeError("key cannot be null")

  protected def writeObjectField(out: JsonStream, x: BigDecimal): Unit =
    if (x ne null) {
      writeParentheses(out)
      out.writeRaw(x.toString)
      writeParenthesesWithColon(out)
    } else encodeError("key cannot be null")

  protected def toByte(in: JsonIterator, n: Int): Byte = {
    if (n > Byte.MaxValue || n < Byte.MinValue) decodeError(in, "value is too large for byte")
    else n.toByte
  }

  protected def toChar(in: JsonIterator, n: Int): Char = {
    if (n > Char.MaxValue || n < Char.MinValue) decodeError(in, "value is too large for char")
    else n.toChar
  }

  protected def toShort(in: JsonIterator, n: Int): Short = {
    if (n > Short.MaxValue || n < Short.MinValue) decodeError(in, "value is too large for short")
    else n.toShort
  }

  private def readParentheses(in: JsonIterator): Unit =
    if (CodecBase.readByte(in) != '"') decodeError(in, "expect \"")

  private def readParenthesesWithColon(in: JsonIterator): Unit = {
    if (CodecBase.readByte(in) != '"') decodeError(in, "expect \"")
    if (CodecBase.nextToken(in) != ':') decodeError(in, "expect :")
  }

  private def writeParentheses(out: JsonStream): Unit = out.write('"')

  private def writeParenthesesWithColon(out: JsonStream): Unit = {
    if (out.currentConfig.indentionStep > 0) out.write('"'.toByte, ':'.toByte, ' '.toByte)
    else out.write('"'.toByte, ':'.toByte)
  }
}

object CodecBase {
  final def readInt(in: JsonIterator): Int = {
    var b = readByte(in)
    val negative = b == '-'
    if (negative) b = readByte(in)
    if (b >= '0' & b <= '9') {
      var v = '0' - b
      var i = 0
      do {
        i = in.head
        while (i < in.tail && {
          b = in.buf(i)
          b >= '0' & b <= '9'
        }) i = {
          if (v == 0) decodeError(in, "leading zero is invalid")
          if (v < -214748364) decodeError(in, "value is too large for int")
          v = v * 10 + '0' - b
          if (v >= 0) decodeError(in,  "value is too large for int")
          i + 1
        }
      } while (loadMore(in, i))
      if (negative) v
      else if (v == Int.MinValue) decodeError(in, "value is too large for int")
      else -v
    } else {
      unreadByte(in)
      decodeError(in, "illegal number")
    }
  }

  final def readLong(in: JsonIterator): Long = {
    var b = readByte(in)
    val negative = b == '-'
    if (negative) b = readByte(in)
    if (b >= '0' & b <= '9') {
      var v: Long = '0' - b
      var i = 0
      do {
        i = in.head
        while (i < in.tail && {
          b = in.buf(i)
          b >= '0' & b <= '9'
        }) i = {
          if (v == 0) decodeError(in, "leading zero is invalid")
          if (v < -922337203685477580L) decodeError(in, "value is too large for long")
          v = v * 10 + '0' - b
          if (v >= 0) decodeError(in,  "value is too large for long")
          i + 1
        }
      } while (loadMore(in, i))
      if (negative) v
      else if (v == Long.MinValue) decodeError(in, "value is too large for long")
      else -v
    } else {
      unreadByte(in)
      decodeError(in, "illegal number")
    }
  }

  final def readDouble(in: JsonIterator): Double = {
    val len = parseNumber(in, isToken = true)
    try java.lang.Double.parseDouble(new String(in.reusableChars, 0, len)) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
  }

  final def readFloat(in: JsonIterator): Float = {
    val len = parseNumber(in, isToken = true)
    try java.lang.Float.parseFloat(new String(in.reusableChars, 0, len)) catch {
      case _: NumberFormatException => decodeError(in, "illegal number")
    }
  }

  final def readBigInt(in: JsonIterator, default: BigInt): BigInt =
    nextToken(in) match {
      case 'n' => parseNull(in, default)
      case _ =>
        unreadByte(in)
        val len = parseNumber(in, isToken = false)
        try new BigInt(new java.math.BigDecimal(in.reusableChars, 0, len).toBigInteger) catch {
          case _: NumberFormatException => decodeError(in, "illegal number")
        }
    }

  final def readBigDecimal(in: JsonIterator, default: BigDecimal): BigDecimal =
    nextToken(in) match {
      case 'n' => parseNull(in, default)
      case _ =>
        unreadByte(in)
        val len = parseNumber(in, isToken = false)
        try new BigDecimal(new java.math.BigDecimal(in.reusableChars, 0, len)) catch {
          case _: NumberFormatException => decodeError(in, "illegal number")
        }
    }

  private def parseNumber(in: JsonIterator, isToken: Boolean): Int = {
    var j = 0
    var b = if (isToken) nextToken(in) else readByte(in)
    if (b == '-') {
      j = putCharAt(in, j, b.toChar)
      b = readByte(in)
    }
    if (b >= '0' & b <= '9') {
      j = putCharAt(in, j, b.toChar)
      var lz = b == '0'
      var i = 0
      do {
        i = in.head
        while (i < in.tail) i = {
          b = in.buf(i)
          if (b >= '0' & b <= '9') {
            if (lz) decodeError(in, "leading zero is invalid")
            j = putCharAt(in, j, b.toChar)
            i + 1
          } else if (b == '.' || b == 'e' || b == 'E' || b == '-' || b == '+') {
            lz = false
            j = putCharAt(in, j, b.toChar)
            i + 1
          } else {
            in.head = i
            return j
          }
        }
      } while (loadMore(in, i))
      j
    } else {
      unreadByte(in)
      decodeError(in, "illegal number")
    }
  }

  final def readString(in: JsonIterator, default: String = null): String =
    nextToken(in) match {
      case '"' => parseString(in, 0)
      case 'n' => parseNull(in, default)
      case _ => decodeError(in, "expect string or null")
    }

  final def parseNull[A](in: JsonIterator, default: A): A =
    if (readByte(in) != 'u' || readByte(in) != 'l' ||  readByte(in) != 'l') decodeError(in, "unexpected value")
    else default

  @tailrec
  private def parseString(in: JsonIterator, pos: Int): String = {
    var j = pos
    var i = in.head
    while (i < in.tail) j = {
      val b = in.buf(i)
      i += 1
      if (b == '"') {
        in.head = i
        return new String(in.reusableChars, 0, j)
      } else if ((b ^ '\\') < 1) {
        in.head = i - 1
        return slowParseString(in, j)
      }
      putCharAt(in, j, b.toChar)
    }
    if (loadMore(in, i)) parseString(in, j)
    else decodeError(in, "unexpected end of input")
  }

  private def slowParseString(in: JsonIterator, pos: Int): String = {
    var j: Int = pos
    var b1: Byte = 0
    while ({
      b1 = readByte(in)
      b1 != '"'
    }) j = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') putCharAt(in, j, b1.toChar)
        else parseEscapeSequence(in, j)
      } else if ((b1 >> 5) == -2) {
        // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = readByte(in)
        if (isMalformed2(b1, b2)) malformedBytes(in, b1, b2)
        putCharAt(in, j, ((b1 << 6) ^ (b2 ^ 0xF80)).toChar) //((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) {
        // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = readByte(in)
        val b3 = readByte(in)
        val c = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar //((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(c)) malformedBytes(in, b1, b2, b3)
        putCharAt(in, j, c)
      } else if ((b1 >> 3) == -2) {
        // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = readByte(in)
        val b3 = readByte(in)
        val b4 = readByte(in)
        val uc = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) //((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(uc)) malformedBytes(in, b1, b2, b3, b4)
        putCharAt(in, putCharAt(in, j, Character.highSurrogate(uc)), Character.lowSurrogate(uc))
      } else malformedBytes(in, b1)
    }
    new String(in.reusableChars, 0, j)
  }

  private def parseEscapeSequence(in: JsonIterator, pos: Int): Int =
    readByte(in) match {
      case 'b' => putCharAt(in, pos, '\b')
      case 'f' => putCharAt(in, pos, '\f')
      case 'n' => putCharAt(in, pos, '\n')
      case 'r' => putCharAt(in, pos, '\r')
      case 't' => putCharAt(in, pos, '\t')
      case '"' => putCharAt(in, pos, '"')
      case '/' => putCharAt(in, pos, '/')
      case '\\' => putCharAt(in, pos, '\\')
      case 'u' =>
        val c1 = readHexDigitPresentedChar(in)
        if (c1 < 2048) putCharAt(in, pos, c1)
        else if (!Character.isHighSurrogate(c1)) {
          if (Character.isLowSurrogate(c1)) decodeError(in, "expect high surrogate character")
          putCharAt(in, pos, c1)
        } else if (readByte(in) == '\\' && readByte(in) == 'u') {
          val c2 = readHexDigitPresentedChar(in)
          if (!Character.isLowSurrogate(c2)) decodeError(in, "expect low surrogate character")
          putCharAt(in, putCharAt(in, pos, c1), c2)
        } else decodeError(in, "invalid escape sequence")
      case _ => decodeError(in, "invalid escape sequence")
    }

  private def putCharAt(in: JsonIterator, pos: Int, ch: Char): Int = {
    val buf = in.reusableChars
    if (buf.length > pos) buf(pos) = ch
    else {
      val newBuf: Array[Char] = new Array[Char](buf.length << 1)
      System.arraycopy(buf, 0, newBuf, 0, buf.length)
      in.reusableChars = newBuf
      newBuf(pos) = ch
    }
    pos + 1
  }

  // using 64-bit hash greatly reducing collisions in field name switch
  final def readObjectFieldAsHash(in: JsonIterator): Long = {
    if (nextToken(in) != '"') decodeError(in, "expect \"")
    val h = parseObjectFieldAsHash(in, -8796714831421723037L)
    if (nextToken(in) != ':') decodeError(in, "expect :")
    h
  }

  @tailrec
  private def parseObjectFieldAsHash(in: JsonIterator, hash: Long): Long = {
    var h = hash
    var i = in.head
    while (i < in.tail) h = {
      val b = in.buf(i)
      i += 1
      if (b == '"') {
        in.head = i
        return h
      } else if ((b ^ '\\') < 1) {
        in.head = i - 1
        return slowParseObjectFieldAsHash(in, h)
      }
      mix(h, b.toChar)
    }
    if (loadMore(in, i)) parseObjectFieldAsHash(in, h)
    else decodeError(in, "unexpected end of input")
  }

  private def slowParseObjectFieldAsHash(in: JsonIterator, hash: Long): Long = {
    var h = hash
    var b1: Byte = 0
    while ({
      b1 = readByte(in)
      b1 != '"'
    }) h = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') mix(h, b1.toChar)
        else parseAndHashEscapeSequence(in, h)
      } else if ((b1 >> 5) == -2) {
        // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = readByte(in)
        if (isMalformed2(b1, b2)) malformedBytes(in, b1, b2)
        mix(h, ((b1 << 6) ^ (b2 ^ 0xF80)).toChar) //((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) {
        // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = readByte(in)
        val b3 = readByte(in)
        val c = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar //((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(c)) malformedBytes(in, b1, b2, b3)
        mix(h, c)
      } else if ((b1 >> 3) == -2) {
        // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = readByte(in)
        val b3 = readByte(in)
        val b4 = readByte(in)
        val uc = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) //((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(uc)) malformedBytes(in, b1, b2, b3, b4)
        mix(mix(h, Character.highSurrogate(uc)), Character.lowSurrogate(uc))
      } else malformedBytes(in, b1)
    }
    h
  }

  private def parseAndHashEscapeSequence(in: JsonIterator, hash: Long) =
    readByte(in) match {
      case 'b' => mix(hash, '\b')
      case 'f' => mix(hash, '\f')
      case 'n' => mix(hash, '\n')
      case 'r' => mix(hash, '\r')
      case 't' => mix(hash, '\t')
      case '"' => mix(hash, '"')
      case '/' => mix(hash, '/')
      case '\\' => mix(hash, '\\')
      case 'u' =>
        val c1 = readHexDigitPresentedChar(in)
        if (c1 < 2048) mix(hash, c1)
        else if (!Character.isHighSurrogate(c1)) {
          if (Character.isLowSurrogate(c1)) decodeError(in, "expect high surrogate character")
          mix(hash, c1)
        } else if (readByte(in) == '\\' && readByte(in) == 'u') {
          val c2 = readHexDigitPresentedChar(in)
          if (!Character.isLowSurrogate(c2)) decodeError(in, "expect low surrogate character")
          mix(mix(hash, c1), c2)
        } else decodeError(in, "invalid escape sequence")
      case _ => decodeError(in, "invalid escape sequence")
    }

  private def readHexDigitPresentedChar(in: JsonIterator): Char =
    ((fromHexDigit(in, readByte(in)) << 12) +
      (fromHexDigit(in, readByte(in)) << 8) +
      (fromHexDigit(in, readByte(in)) << 4) +
      fromHexDigit(in, readByte(in))).toChar

  private def mix(hash: Long, ch: Char): Long = {
    val h = (hash ^ ch) * 1609587929392839161L
    h ^ (h >>> 47) // mix highest bits to reduce probability of zeroing and loosing part of hash from preceding chars
  }

  private def isMalformed2(b1: Byte, b2: Byte) = (b1 & 0x1E) == 0 || (b2 & 0xC0) != 0x80

  private def isMalformed3(b1: Byte, b2: Byte, b3: Byte) =
    (b1 == 0xE0.toByte && (b2 & 0xE0) == 0x80) || (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80

  private def isMalformed4(b2: Byte, b3: Byte, b4: Byte) =
    (b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80 || (b4 & 0xC0) != 0x80

  private def malformedBytes(in: JsonIterator, bytes: Byte*): Nothing = {
    val sb = new StringBuilder("malformed byte(s): ")
    var first = true
    bytes.foreach { b =>
      (if (first) {
        first = false
        sb.append("0x")
      } else sb.append(", 0x")).append(toHexDigit(b >>> 4)).append(toHexDigit(b))
    }
    decodeError(in, sb.toString)
  }

  private def fromHexDigit(in: JsonIterator, b: Byte): Int = {
    if (b >= '0' & b <= '9') b - 48
    else {
      val b1 = b & -33
      if (b1 >= 'A' & b1 <= 'F') b1 - 55
      else decodeError(in, "expect hex digit character")
    }
  }

  private def toHexDigit(n: Int): Char = {
    val n1 = n & 15
    n1 + (if (n1 > 9) 55 else 48)
  }.toChar

  @tailrec
  final def nextToken(in: JsonIterator): Byte = {
    var i = in.head
    while (i < in.tail) {
      in.buf(i) match {
        case ' ' => // continue
        case '\n' => // continue
        case '\t' => // continue
        case '\r' => // continue
        case b =>
          in.head = i + 1
          return b
      }
      i += 1
    }
    if (loadMore(in, i)) nextToken(in)
    else decodeError(in, "unexpected end of input")
  }

  final def readByte(in: JsonIterator): Byte = {
    var i = in.head
    if (i == in.tail) {
      if (loadMore(in, i)) i = 0
      else decodeError(in, "unexpected end of input")
    }
    in.head = i + 1
    in.buf(i)
  }

  final def unreadByte(in: JsonIterator): Unit = in.head -= 1

  private def loadMore(in: JsonIterator, i: Int): Boolean =
    if (in.in == null) {
      in.head = i
      false
    } else realLoadMore(in, i)

  private def realLoadMore(in: JsonIterator, i: Int): Boolean = {
    val n = in.in.read(in.buf)
    if (n > 0) {
      in.head = 0
      in.tail = n
      true
    } else if (n == -1) {
      in.head = i
      false
    } else decodeError(in, "invalid parser state")
  }

  private def decodeError(in: JsonIterator, msg: String): Nothing = throw in.reportError("decode", msg)

  private def encodeError(msg: String): Nothing = throw new IOException("encode: " + msg)
}