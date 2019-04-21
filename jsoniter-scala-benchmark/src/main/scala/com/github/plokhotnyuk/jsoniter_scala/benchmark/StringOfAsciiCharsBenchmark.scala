package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class StringOfAsciiCharsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: String = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = {
      val cs = new Array[Char](size)
      var i = 0
      var j = 1
      while (i < cs.length) {
        cs(i) = {
          var ch: Char = 0
          do {
            ch = ((j * 1498724053) % 128).toChar
            j += 1
          } while (!JsonWriter.isNonEscapedAscii(ch) || ch == '~') // FIXME: Jsoniter(Java) uses hexadecimal escaping for `~`
          ch
        }
        i += 1
      }
      new String(cs)
    }
    jsonString = "\"" + obj + "\""
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}