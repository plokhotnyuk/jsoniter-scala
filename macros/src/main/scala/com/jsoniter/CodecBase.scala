package com.jsoniter

import java.io.{InputStream, OutputStream}

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

  protected def decodeError(in: JsonIterator, msg: String): Nothing = throw in.reportError("decode", msg)

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
    out.writeObjectField(x)

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

  protected def writeObjectField(out: JsonStream, x: BigInt): Unit = {
    writeParentheses(out)
    out.writeRaw(x.toString)
    writeParenthesesWithColon(out)
  }

  protected def writeObjectField(out: JsonStream, x: BigDecimal): Unit = {
    writeParentheses(out)
    out.writeRaw(x.toString)
    writeParenthesesWithColon(out)
  }

  protected def toByte(in: JsonIterator, n: Int): Byte = {
    if (n > Byte.MaxValue || n < Byte.MinValue) decodeError(in, s"byte overflow: $n")
    else n.toByte
  }

  protected def toChar(in: JsonIterator, n: Int): Char = {
    if (n > Char.MaxValue || n < Char.MinValue) decodeError(in, s"char overflow: $n")
    else n.toChar
  }

  protected def toShort(in: JsonIterator, n: Int): Byte = {
    if (n > Short.MaxValue || n < Short.MinValue) decodeError(in, s"short overflow: $n")
    else n.toByte
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
  def readObjectFieldAsHash(in: JsonIterator): Long = try {
    if (IterImpl.nextToken(in) != '"') readObjectFieldAsHashError(in, "expect \"")
    var hash: Long = -8796714831421723037L
    var b1: Byte = 0
    while ({ b1 = IterImpl.readByte(in); b1 != '"' }) hash = {
      if (b1 >= 0) {
        // 1 byte, 7 bits: 0xxxxxxx
        if (b1 != '\\') mix(hash, b1.toChar)
        else {
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
              val c1 = ((IterImplString.translateHex(IterImpl.readByte(in)) << 12) +
                (IterImplString.translateHex(IterImpl.readByte(in)) << 8) +
                (IterImplString.translateHex(IterImpl.readByte(in)) << 4) +
                IterImplString.translateHex(IterImpl.readByte(in))).toChar
              if (c1  < 2048) mix(hash, c1)
              else if (!Character.isHighSurrogate(c1)) {
                if (Character.isLowSurrogate(c1)) readObjectFieldAsHashError(in, "expect high surrogate character")
                mix(hash, c1)
              } else if (IterImpl.readByte(in) == '\\' && IterImpl.readByte(in) == 'u') {
                val c2 = ((IterImplString.translateHex(IterImpl.readByte(in)) << 12) +
                  (IterImplString.translateHex(IterImpl.readByte(in)) << 8) +
                  (IterImplString.translateHex(IterImpl.readByte(in)) << 4) +
                  IterImplString.translateHex(IterImpl.readByte(in))).toChar
                if (!Character.isLowSurrogate(c2)) readObjectFieldAsHashError(in, "expect low surrogate character")
                mix(mix(hash, c1), c2)
              } else readObjectFieldAsHashError(in, "invalid escape sequence")
            case _ => readObjectFieldAsHashError(in, "invalid escape sequence")
          }
        }
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
    if (IterImpl.nextToken(in) != ':') readObjectFieldAsHashError(in, "expect :")
    hash
  } catch {
    case _: ArrayIndexOutOfBoundsException => readObjectFieldAsHashError(in, "invalid byte or escape sequence")
  }

  private def mix(hash: Long, ch: Char): Long = { // use 64-bit hash to minimize collisions in field name switch
    val h = (hash ^ ch) * 1609587929392839161L
    h ^ (h >>> 47)
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
      } else sb.append(", 0x")).append(hexDigits(b >>> 4)).append(hexDigits(b))
    }
    readObjectFieldAsHashError(in, sb.toString)
  }

  private def hexDigits(n: Int): Char = {
    val nibble = n & 15
    nibble + 48 + (((9 - nibble) >> 31) & 7)
  }.toChar

  private def readObjectFieldAsHashError(in: JsonIterator, msg: String): Nothing =
    throw in.reportError("readObjectFieldAsHash", msg)
}