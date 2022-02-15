package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time._
import org.openjdk.jmh.annotations.{Param, Setup}
import scala.jdk.CollectionConverters._

abstract class ArrayOfZoneIdsBenchmark extends CommonParams {
  val zoneIds: Array[ZoneId] = (ZoneId.getAvailableZoneIds.asScala.take(100).map(ZoneId.of) ++
    (1 to 7).map(i => ZoneId.of(s"+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"UT+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"UTC+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"GMT+0$i:00"))).toArray
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[ZoneId] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => zoneIds(i % zoneIds.length)).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }
}