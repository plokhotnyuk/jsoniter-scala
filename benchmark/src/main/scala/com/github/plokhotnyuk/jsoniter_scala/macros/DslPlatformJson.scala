package com.github.plokhotnyuk.jsoniter_scala.macros

import com.dslplatform.json.DslJson
import java.io.OutputStream

object DslPlatformJson {
  val dslJson = new DslJson[Any]
}

class PreallocByteArrayOutputStream(private[this] val buf: Array[Byte]) extends OutputStream {
  var count = 0

  override def write(b: Int): Unit = {
    buf(count) = b.toByte
    count += 1
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    System.arraycopy(b, off, buf, count, len)
    count += len
  }
}