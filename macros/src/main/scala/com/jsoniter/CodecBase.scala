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
    if (IterImpl.readByte(in) != '"' && IterImpl.nextToken(in) != '"') readObjectFieldAsHashError(in, "expect \"")
    val limit = in.tail
    val buf = in.buf
    var i = in.head
    var hash: Long = -8796714831421723037L
    var b = 0
    while (i < limit && { b = buf(i); i += 1; b != '"' }) hash = {
      if (b == '\\') {
        b = buf(i)
        i += 1
        b match {
          case 'b' => mix(hash, '\b')
          case 'f' => mix(hash, '\f')
          case 'n' => mix(hash, '\n')
          case 'r' => mix(hash, '\r')
          case 't' => mix(hash, '\t')
          case '"' => mix(hash, '"')
          case '/' => mix(hash, '/')
          case '\\' => mix(hash, '\\')
          case 'u' =>
            val c1 = ((IterImplString.translateHex(buf(i)) << 12) +
              (IterImplString.translateHex(buf(i + 1)) << 8) +
              (IterImplString.translateHex(buf(i + 2)) << 4) +
              IterImplString.translateHex(buf(i +3))).toChar
            i += 4
            if (c1 < 128) mix(hash, c1)
            else if (c1  < 2048) mix(mix(hash, 0xC0 | (c1 >> 6)), 0x80 | (c1 & 0x3F))
            else if (!Character.isHighSurrogate(c1)) {
              if (Character.isLowSurrogate(c1)) readObjectFieldAsHashError(in, "expect high surrogate character")
              mix(mix(mix(hash, 0xE0 | (c1 >> 12)), 0x80 | ((c1 >> 6) & 0x3F)), 0x80 | (c1 & 0x3F))
            } else if (buf(i) == '\\' && buf(i + 1) == 'u') {
              val c2 = ((IterImplString.translateHex(buf(i + 2)) << 12) +
                (IterImplString.translateHex(buf(i + 3)) << 8) +
                (IterImplString.translateHex(buf(i + 4)) << 4) +
                IterImplString.translateHex(buf(i + 5))).toChar
              i += 6
              if (!Character.isLowSurrogate(c2)) readObjectFieldAsHashError(in, "expect low surrogate character")
              val c = Character.toCodePoint(c1, c2)
              mix(mix(mix(mix(hash, 0xF0 | (c >> 18)), 0x80 | ((c >> 12) & 0x3F)), 0x80 | ((c >> 6) & 0x3F)), 0x80 | (c & 0x3F))
            } else readObjectFieldAsHashError(in, "invalid escape sequence")
          case _ => readObjectFieldAsHashError(in, "invalid escape sequence")
        }
      } else mix(hash, b)
    }
    in.head = i
    if (IterImpl.readByte(in) != ':' && IterImpl.nextToken(in) != ':') readObjectFieldAsHashError(in, "expect :")
    hash
  } catch {
    case _: ArrayIndexOutOfBoundsException => readObjectFieldAsHashError(in, "invalid escape sequence")
  }

  private def mix(hash: Long, byte: Int): Long = { // use 64-bit hash to minimize collisions in field name switch
    val h1 = hash ^ (byte & 0xFF)
    val h2 = h1 * 1609587929392839161L
    h2 ^ (h2 >>> 47)
  }

  private def readObjectFieldAsHashError(in: JsonIterator, msg: String): Nothing =
    throw in.reportError("readObjectFieldAsHash", msg)
}