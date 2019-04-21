package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import org.openjdk.jmh.annotations.{Param, Setup}

abstract class BigDecimalBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var sourceObj: BigDecimal = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    sourceObj = BigDecimal(jsonString, MathContext.UNLIMITED)
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  private[benchmark] def obj: BigDecimal = {
    val x = sourceObj // to avoid internal caching of the string representation
    new BigDecimal(new java.math.BigDecimal(x.bigDecimal.unscaledValue, x.bigDecimal.scale), x.mc)
  }
}