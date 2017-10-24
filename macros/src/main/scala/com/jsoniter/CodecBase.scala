package com.jsoniter

import java.io.{IOException, InputStream, OutputStream}

import com.jsoniter.output.{JsonStream, JsonStreamPool}
import com.jsoniter.spi.{Config, Decoder, Encoder, JsoniterSpi}
import com.jsoniter.spi.JsoniterSpi._

abstract class CodecBase[A](implicit m: Manifest[A]) extends Encoder with Decoder {
  private val cls = m.runtimeClass.asInstanceOf[Class[A]]

  JsoniterSpi.setDefaultConfig((new Config.Builder).escapeUnicode(false).build)
  addNewEncoder(getCurrentConfig.getEncoderCacheKey(cls), this)
  addNewDecoder(getCurrentConfig.getDecoderCacheKey(cls), this)

  def read(in: InputStream, bufSize: Int = 512): A = {
    val it = JsonIteratorPool.borrowJsonIterator()
    try {
      JsonIterator.enableStreamingSupport()
      it.in = in
      if (it.buf.length != bufSize) it.buf = new Array[Byte](bufSize)
      it.head = 0
      it.tail = 0
      decode(it).asInstanceOf[A]
    } finally JsonIteratorPool.returnJsonIterator(it)
  }

  def read(buf: Array[Byte]): A = {
    val it = JsonIteratorPool.borrowJsonIterator()
    try {
      it.reset(buf)
      decode(it).asInstanceOf[A]
    } finally JsonIteratorPool.returnJsonIterator(it)
  }

  def write(obj: A, out: OutputStream): Unit = {
    val stream = JsonStreamPool.borrowJsonStream()
    try {
      stream.reset(out)
      if (obj == null) stream.writeNull()
      else encode(obj, stream)
    } finally {
      stream.close()
      JsonStreamPool.returnJsonStream(stream)
    }
  }

  def write(obj: A): Array[Byte] = {
    val out = JsonStreamPool.borrowJsonStream
    try {
      out.reset(null)
      if (obj == null) out.writeNull()
      else encode(obj, out)
      val buf = out.buffer()
      val array = new Array[Byte](buf.len)
      System.arraycopy(buf.data, 0, array, 0, buf.len)
      array
    } finally JsonStreamPool.returnJsonStream(out)
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
    val x = CodecBase.parseString(in)
    if (IterImpl.nextToken(in) != ':') decodeError(in, "expect :")
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
    val x = in.readInt()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsLong(in: JsonIterator): Long = {
    readParentheses(in)
    val x = in.readLong()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsFloat(in: JsonIterator): Float = {
    readParentheses(in)
    val x = in.readFloat()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsDouble(in: JsonIterator): Double = {
    readParentheses(in)
    val x = in.readDouble()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsBigInt(in: JsonIterator): BigInt = {
    readParentheses(in)
    val x = in.readBigInteger()
    readParenthesesWithColon(in)
    x
  }

  protected def readObjectFieldAsBigDecimal(in: JsonIterator): BigDecimal = {
    readParentheses(in)
    val x = in.readBigDecimal()
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
    if (IterImpl.readByte(in) != '"') decodeError(in, "expect \"")

  private def readParenthesesWithColon(in: JsonIterator): Unit = {
    if (IterImpl.readByte(in) != '"') decodeError(in, "expect \"")
    if (IterImpl.nextToken(in) != ':') decodeError(in, "expect :")
  }

  private def writeParentheses(out: JsonStream): Unit = out.write('"')

  private def writeParenthesesWithColon(out: JsonStream): Unit = {
    if (out.currentConfig.indentionStep > 0) out.write('"'.toByte, ':'.toByte, ' '.toByte)
    else out.write('"'.toByte, ':'.toByte)
  }
}

object CodecBase {
  def readString(in: JsonIterator, default: String = null): String =
    IterImpl.nextToken(in) match {
      case '"' => parseString(in)
      case 'n' => parseNull(in, default)
      case _ => decodeError(in, "expect string or null")
    }

  def parseNull[A](in: JsonIterator, default: A): A =
    if (IterImpl.readByte(in) != 'u' || IterImpl.readByte(in) != 'l' ||  IterImpl.readByte(in) != 'l') decodeError(in, "unexpected value")
    else default

  private def parseString(in: JsonIterator): String = try {
    var pos: Int = 0
    var b1: Byte = 0
    while ({
      b1 = IterImpl.readByte(in)
      b1 != '"'
    }) pos = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') putCharAt(in, pos, b1.toChar)
        else parseEscapeSequence(in, pos)
      } else if ((b1 >> 5) == -2) {
        // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        if (isMalformed2(b1, b2)) malformedBytes(in, b1, b2)
        putCharAt(in, pos, ((b1 << 6) ^ (b2 ^ 0xF80)).toChar) //((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) {
        // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        val b3 = IterImpl.readByte(in)
        val c = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar //((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(c)) malformedBytes(in, b1, b2, b3)
        putCharAt(in, pos, c)
      } else if ((b1 >> 3) == -2) {
        // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        val b3 = IterImpl.readByte(in)
        val b4 = IterImpl.readByte(in)
        val uc = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) //((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(uc)) malformedBytes(in, b1, b2, b3, b4)
        putCharAt(in, putCharAt(in, pos, Character.highSurrogate(uc)), Character.lowSurrogate(uc))
      } else malformedBytes(in, b1)
    }
    new String(in.reusableChars, 0, pos)
  } catch {
    case _: ArrayIndexOutOfBoundsException => decodeError(in, "invalid byte or escape sequence")
  }

  private def parseEscapeSequence(in: JsonIterator, pos: Int): Int =
    IterImpl.readByte(in) match {
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
        } else if (IterImpl.readByte(in) == '\\' && IterImpl.readByte(in) == 'u') {
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
  def readObjectFieldAsHash(in: JsonIterator): Long = try {
    if (IterImpl.nextToken(in) != '"') decodeError(in, "expect \"")
    var hash: Long = -8796714831421723037L
    var b1: Byte = 0
    while ({
      b1 = IterImpl.readByte(in)
      b1 != '"'
    }) hash = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') mix(hash, b1.toChar)
        else parseAndHashEscapeSequence(in, hash)
      } else if ((b1 >> 5) == -2) {
        // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        if (isMalformed2(b1, b2)) malformedBytes(in, b1, b2)
        mix(hash, ((b1 << 6) ^ (b2 ^ 0xF80)).toChar) //((0xC0.toByte << 6) ^ 0x80.toByte)
      } else if ((b1 >> 4) == -2) {
        // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        val b3 = IterImpl.readByte(in)
        val c = ((b1 << 12) ^ (b2 << 6) ^ (b3 ^ 0xFFFE1F80)).toChar //((0xE0.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed3(b1, b2, b3) || Character.isSurrogate(c)) malformedBytes(in, b1, b2, b3)
        mix(hash, c)
      } else if ((b1 >> 3) == -2) {
        // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
        val b2 = IterImpl.readByte(in)
        val b3 = IterImpl.readByte(in)
        val b4 = IterImpl.readByte(in)
        val uc = (b1 << 18) ^ (b2 << 12) ^ (b3 << 6) ^ (b4 ^ 0x381F80) //((0xF0.toByte << 18) ^ (0x80.toByte << 12) ^ (0x80.toByte << 6) ^ 0x80.toByte)
        if (isMalformed4(b2, b3, b4) || !Character.isSupplementaryCodePoint(uc)) malformedBytes(in, b1, b2, b3, b4)
        mix(mix(hash, Character.highSurrogate(uc)), Character.lowSurrogate(uc))
      } else malformedBytes(in, b1)
    }
    if (IterImpl.nextToken(in) != ':') decodeError(in, "expect :")
    hash
  } catch {
    case _: ArrayIndexOutOfBoundsException => decodeError(in, "invalid byte or escape sequence")
  }

  private def parseAndHashEscapeSequence(in: JsonIterator, hash: Long) =
    IterImpl.readByte(in) match {
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
        } else if (IterImpl.readByte(in) == '\\' && IterImpl.readByte(in) == 'u') {
          val c2 = readHexDigitPresentedChar(in)
          if (!Character.isLowSurrogate(c2)) decodeError(in, "expect low surrogate character")
          mix(mix(hash, c1), c2)
        } else decodeError(in, "invalid escape sequence")
      case _ => decodeError(in, "invalid escape sequence")
    }

  private def readHexDigitPresentedChar(in: JsonIterator): Char =
    ((fromHexDigit(in, IterImpl.readByte(in)) << 12) +
      (fromHexDigit(in, IterImpl.readByte(in)) << 8) +
      (fromHexDigit(in, IterImpl.readByte(in)) << 4) +
      fromHexDigit(in, IterImpl.readByte(in))).toChar

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

  private def decodeError(in: JsonIterator, msg: String): Nothing = throw in.reportError("decode", msg)

  private def encodeError(msg: String): Nothing = throw new IOException("encode: " + msg)
}