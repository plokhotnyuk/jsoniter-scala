package com.jsoniter.output

import java.io.{IOException, OutputStream}

import com.github.plokhotnyuk.jsoniter_scala.Codec
import com.jsoniter.spi.{Config, JsoniterSpi}

object JsonStreamUtil {
  private val pool: ThreadLocal[JsonStream] = new ThreadLocal[JsonStream] {
    override def initialValue(): JsonStream = {
      val stream = new JsonStream(null, 1024)
      stream.configCache = JsoniterSpi.getDefaultConfig
      stream
    }

    override def get(): JsonStream = {
      var stream = super.get()
      if (stream.buf.length > 32768) {
        stream = initialValue()
        set(stream)
      }
      stream
    }
  }

  final def write[A](codec: Codec[A], obj: A, out: OutputStream): Unit = {
    val stream = pool.get
    stream.reset(out)
    try codec.write(obj, stream)
    finally stream.close()
  }

  final def write[A](codec: Codec[A], obj: A): Array[Byte] = {
    val stream = pool.get
    stream.reset(null)
    codec.write(obj, stream)
    val arr = new Array[Byte](stream.count)
    System.arraycopy(stream.buf, 0, arr, 0, arr.length)
    arr
  }

  final def write[A](codec: Codec[A], obj: A, out: OutputStream, cfg: Config): Unit = {
    val stream = pool.get
    val currCfg = stream.configCache
    stream.reset(out)
    stream.configCache = cfg
    try codec.write(obj, stream)
    finally {
      stream.configCache = currCfg
      stream.close()
    }
  }

  final def write[A](codec: Codec[A], obj: A, cfg: Config): Array[Byte] = {
    val stream = pool.get
    val currCfg = stream.configCache
    stream.reset(null)
    stream.configCache = cfg
    try codec.write(obj, stream)
    finally stream.configCache = currCfg
    val arr = new Array[Byte](stream.count)
    System.arraycopy(stream.buf, 0, arr, 0, arr.length)
    arr
  }

  final def writeSep(out: JsonStream, first: Boolean): Boolean = {
    if (first) out.writeIndention()
    else out.writeMore()
    false
  }

  final def writeObjectField(out: JsonStream, x: String): Unit =
    if (x ne null) out.writeObjectField(x)
    else encodeError("key cannot be null")

  final def writeObjectField(out: JsonStream, x: Boolean): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  final def writeObjectField(out: JsonStream, x: Int): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  final def writeObjectField(out: JsonStream, x: Long): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  final def writeObjectField(out: JsonStream, x: Float): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  final def writeObjectField(out: JsonStream, x: Double): Unit = {
    writeParentheses(out)
    out.writeVal(x)
    writeParenthesesWithColon(out)
  }

  final def writeObjectField(out: JsonStream, x: BigInt): Unit =
    if (x ne null) {
      writeParentheses(out)
      out.writeRaw(x.toString)
      writeParenthesesWithColon(out)
    } else encodeError("key cannot be null")

  final def writeObjectField(out: JsonStream, x: BigDecimal): Unit =
    if (x ne null) {
      writeParentheses(out)
      out.writeRaw(x.toString)
      writeParenthesesWithColon(out)
    } else encodeError("key cannot be null")

  final def encodeError(msg: String): Nothing = throw new IOException("encode: " + msg)

  private def writeParentheses(out: JsonStream): Unit = out.write('"')

  private def writeParenthesesWithColon(out: JsonStream): Unit =
    if (out.currentConfig.indentionStep > 0) out.write('"'.toByte, ':'.toByte, ' '.toByte)
    else out.write('"'.toByte, ':'.toByte)
}
