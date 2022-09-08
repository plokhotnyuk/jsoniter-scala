package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.{Param, Setup}
import scala.collection.mutable

abstract class MutableBitSetBenchmark extends CommonParams {
  var obj: mutable.BitSet = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString: String = _
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000

  @Setup
  def setup(): Unit = {
    obj = mutable.BitSet(0 until size: _*)
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes("UTF-8")
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}