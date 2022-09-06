package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class Base16Benchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Byte] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  val toHex: Array[Char] = "0123456789abcdef".toCharArray

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(_.toByte).toArray
    jsonString = toBase16(obj)
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  def toBase16(bs: Array[Byte]): String = {
    val sb = (new StringBuilder).append('"')
    var i = 0
    while (i < bs.length) {
      val b = bs(i)
      sb.append(toHex(b >> 4 & 0xF)).append(toHex(b & 0xF))
      i += 1
    }
    sb.append('"').toString
  }
}