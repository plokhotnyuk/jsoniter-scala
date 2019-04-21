package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import org.openjdk.jmh.annotations.{Param, Setup}

sealed trait SuitADT extends Product with Serializable

case object Hearts extends SuitADT

case object Spades extends SuitADT

case object Diamonds extends SuitADT

case object Clubs extends SuitADT

abstract class ArrayOfEnumADTsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 100
  var obj: Array[SuitADT] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => (i * 1498724053) & 0x3 match {
      case 0 => Hearts
      case 1 => Spades
      case 2 => Diamonds
      case 3 => Clubs
    }).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}