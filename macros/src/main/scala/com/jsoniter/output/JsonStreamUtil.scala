package com.jsoniter.output

import com.jsoniter.spi.JsoniterSpi

object JsonStreamUtil {
  val pool: ThreadLocal[JsonStream] = new ThreadLocal[JsonStream] {
    override def initialValue(): JsonStream = {
      val stream = new JsonStream(null, 1024)
      stream.configCache = JsoniterSpi.getDefaultConfig
      stream
    }

    override def get(): JsonStream = {
      val stream = super.get()
      if (stream.buf.length > 32768) {
        val newStream = initialValue()
        set(newStream)
        newStream
      } else stream
    }
  }

  def getBytes(stream: JsonStream): Array[Byte] = {
    val arr = new Array[Byte](stream.count)
    System.arraycopy(stream.buf, 0, arr, 0, arr.length)
    arr
  }
}
