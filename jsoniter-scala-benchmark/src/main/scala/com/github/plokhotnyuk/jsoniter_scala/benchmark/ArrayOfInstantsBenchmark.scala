package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

import org.openjdk.jmh.annotations.{Param, Setup}

abstract class ArrayOfInstantsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Instant] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var cborBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      val n = Math.abs(i * 1498724053)
      Instant.ofEpochSecond(n, i % 4 match {
        case 0 => 0
        case 1 => ((n % 1000) | 0x1) * 1000000
        case 2 => ((n % 1000000) | 0x1) * 1000
        case 3 => (n | 0x1) % 1000000000
      })
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    cborBytes = {
      implicit val e: io.bullet.borer.Encoder[Instant] =
        io.bullet.borer.Encoder.forTuple2[Long, Long].contramap((x: Instant) => (x.getEpochSecond, x.getNano))
      io.bullet.borer.Json.encode(obj).toByteArray
    }
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}