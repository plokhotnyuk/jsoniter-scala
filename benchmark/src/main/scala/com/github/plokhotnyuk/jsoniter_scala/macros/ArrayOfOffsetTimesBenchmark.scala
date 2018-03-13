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
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfOffsetTimesBenchmark extends CommonParams {
  val obj: Array[OffsetTime] = (1 to 128).map { i =>
    OffsetTime.of(LocalTime.ofSecondOfDay((i * 127) | 1), ZoneOffset.ofHours(i % 17))
  }.toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

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
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(offsetTimeArrayFormat))
}