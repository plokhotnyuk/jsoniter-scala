package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats.offsetTimeArrayFormat
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

import scala.collection.breakOut

class ArrayOfOffsetTimesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[OffsetTime] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  setup()

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      OffsetTime.of(LocalTime.ofSecondOfDay((Math.abs(i * 1498724053) % 86000) | 1), ZoneOffset.ofHours(i % 17))
    }(breakOut)
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Array[OffsetTime] = decode[Array[OffsetTime]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[OffsetTime] = jacksonMapper.readValue[Array[OffsetTime]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[OffsetTime] = readFromArray[Array[OffsetTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[OffsetTime] = Json.parse(jsonBytes).as[Array[OffsetTime]](offsetTimeArrayFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(offsetTimeArrayFormat))
}