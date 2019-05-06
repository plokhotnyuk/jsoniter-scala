package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8

import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfBigDecimalsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var sourceObj: Array[BigDecimal] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    sourceObj = (1 to size).map { i =>
      BigDecimal(BigInt(Array.fill((i % 14) + 1)((i | 0x1).toByte)), i % 38).round(MathContext.DECIMAL128)
    }.toArray // up to 112-bit BigInt numbers rounded to IEEE 754 Decimal128 format (34 decimal digits)
    jsonString = sourceObj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  private[benchmark] def obj: Array[BigDecimal] = {
    val xs = sourceObj
    val l = xs.length
    val ys = new Array[BigDecimal](l)
    var i = 0
    while (i < l) {
      val x = xs(i) // to avoid internal caching of the string representation
      ys(i) = new BigDecimal(new java.math.BigDecimal(x.bigDecimal.unscaledValue, x.bigDecimal.scale), x.mc)
      i += 1
    }
    ys
  }
}