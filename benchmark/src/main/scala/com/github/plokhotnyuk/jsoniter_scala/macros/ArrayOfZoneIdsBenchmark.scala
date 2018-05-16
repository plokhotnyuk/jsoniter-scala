package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

import scala.collection.JavaConverters._

class ArrayOfZoneIdsBenchmark extends CommonParams {
  val zoneIds: Array[ZoneId] = (ZoneId.getAvailableZoneIds.asScala.take(100).map(ZoneId.of) ++
    (1 to 7).map(i => ZoneId.of(s"+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"UT+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"UTC+0$i:00")) ++
    (1 to 7).map(i => ZoneId.of(s"GMT+0$i:00"))).toArray
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[ZoneId] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => zoneIds(i % zoneIds.length)).to
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Array[ZoneId] = decode[Array[ZoneId]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[ZoneId] = jacksonMapper.readValue[Array[ZoneId]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[ZoneId] = readFromArray[Array[ZoneId]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[ZoneId] = Json.parse(jsonBytes).as[Array[ZoneId]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}