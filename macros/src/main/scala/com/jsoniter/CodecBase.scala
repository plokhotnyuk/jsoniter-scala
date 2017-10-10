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
  // FIXME add decoding of hex coded characters
  def readObjectFieldAsHash(in: JsonIterator): Long = { // use 64-bit hash to minimize collisions in field name switch
    if (IterImpl.readByte(in) != '"' && IterImpl.nextToken(in) != '"') throw in.reportError("readObjectFieldAsHash", "expect \"")
    val limit = in.tail
    val buf = in.buf
    var i = in.head
    var hash: Long = -8796714831421723037L
    var ch = 0
    while (i < limit && { ch = buf(i); ch != '"' }) {
      hash ^= (ch & 0xFF)
      hash *= 1609587929392839161L
      hash ^= hash >> 47
      i += 1
    }
    in.head = i
    if (IterImpl.readByte(in) != ':' && IterImpl.nextToken(in) != ':') throw in.reportError("readObjectFieldAsHash", "expect :")
    hash
  }
}