package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.{Param, Setup}

abstract class BigIntBenchmark extends CommonParams {
  var obj: BigInt = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 128

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    obj = BigInt(jsonString)
    preallocatedBuf = new Array(jsonBytes.length + 128/*to avoid possible out-of-bounds error*/)
  }
}