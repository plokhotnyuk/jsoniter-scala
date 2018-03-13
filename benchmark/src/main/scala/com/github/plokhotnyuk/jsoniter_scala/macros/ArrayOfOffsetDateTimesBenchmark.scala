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
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfOffsetDateTimesBenchmark extends CommonParams {
  val obj: Array[OffsetDateTime] = (1 to 128).map { i =>
    OffsetDateTime.of(LocalDateTime.of(LocalDate.ofEpochDay(i * 256), LocalTime.ofSecondOfDay((i * 127) | 1)),
      ZoneOffset.ofHours(i % 17))
  }.toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[OffsetDateTime] = decode[Array[OffsetDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[OffsetDateTime] = jacksonMapper.readValue[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[OffsetDateTime] = readFromArray[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[OffsetDateTime] = Json.parse(jsonBytes).as[Array[OffsetDateTime]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}