package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import org.openjdk.jmh.annotations.{Param, Setup}

object SuitEnum extends Enumeration {
  type SuitEnum = Value

  val Hearts: SuitEnum = Value(0, "Hearts") // Always set the name explicitly in your Scala enumeration definition.
  val Spades: SuitEnum = Value(1, "Spades") // If you still not sure, then please look and check that the following
  val Diamonds: SuitEnum = Value(2, "Diamonds") // synchronized block will not affect your code in run-time:
  val Clubs: SuitEnum = Value(3, "Clubs") // https://github.com/scala/scala/blob/1692ae306dc9a5ff3feebba6041348dfdee7cfb5/src/library/scala/Enumeration.scala#L203
}

abstract class ArrayOfEnumsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[SuitEnum] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => SuitEnum((i * 1498724053) & 0x3)).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}