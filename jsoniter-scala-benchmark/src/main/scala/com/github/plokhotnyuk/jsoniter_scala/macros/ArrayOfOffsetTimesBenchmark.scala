package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats.offsetTimeArrayFormat
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class ArrayOfOffsetTimesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[OffsetTime] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      val n = Math.abs(i * 1498724053)
      OffsetTime.of(LocalTime.ofNanoOfDay(((n % 86000) | 1) * 1000000000L + ((i % 4 match {
          case 0 => 0
          case 1 => (n % 1000) * 1000000
          case 2 => (n % 1000000) * 1000
          case 3 => n % 1000000000
        }) | 1)),
        ZoneOffset.ofHours(i % 17))
    }.toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[OffsetTime] = JsonStringInput.read[Array[OffsetTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[OffsetTime] = decode[Array[OffsetTime]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[OffsetTime] = jacksonMapper.readValue[Array[OffsetTime]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[OffsetTime] = readFromArray[Array[OffsetTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[OffsetTime] = Json.parse(jsonBytes).as[Array[OffsetTime]](offsetTimeArrayFormat)

  @Benchmark
  def readUPickle(): Array[OffsetTime] = read[Array[OffsetTime]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

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

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}