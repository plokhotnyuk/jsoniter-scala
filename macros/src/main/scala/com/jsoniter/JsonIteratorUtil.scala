package com.jsoniter

import com.jsoniter.spi.JsoniterSpi

object JsonIteratorUtil {
  val pool: ThreadLocal[JsonIterator] = new ThreadLocal[JsonIterator] {
    override def initialValue(): JsonIterator = {
      val it = JsonIterator.parse(new Array[Byte](1024), 0, 0)
      it.configCache = JsoniterSpi.getDefaultConfig
      it
    }

    override def get(): JsonIterator = {
      val it = super.get()
      if (it.buf.length + it.reusableChars.length > 32768) {
        val newIt = initialValue()
        set(newIt)
        newIt
      } else it
    }
  }
}
