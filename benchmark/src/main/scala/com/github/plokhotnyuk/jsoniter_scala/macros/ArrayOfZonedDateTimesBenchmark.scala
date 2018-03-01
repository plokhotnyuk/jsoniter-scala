package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.{ZoneId, _}

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.collection.JavaConverters._

class ArrayOfZonedDateTimesBenchmark extends CommonParams {
  val zoneIds: Array[String] = ZoneId.getAvailableZoneIds.asScala.toArray
  val obj: Array[ZonedDateTime] = (1 to 128).map { i =>
    ZonedDateTime.of(LocalDateTime.of(LocalDate.ofEpochDay(i * 256), LocalTime.ofSecondOfDay((i * 127) | 1)),
      ZoneId.of(zoneIds(i)))
  }.toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[ZonedDateTime] = decode[Array[ZonedDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
/* FIXME jackson parse ZonedDateTime with conversion to Z time zone
  @Benchmark
  def readJacksonScala(): Array[ZonedDateTime] = jacksonMapper.readValue[Array[ZonedDateTime]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): Array[ZonedDateTime] = readFromArray[Array[ZonedDateTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[ZonedDateTime] = Json.parse(jsonBytes).as[Array[ZonedDateTime]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME jackson serializes ZonedDateTime as array of numbers
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}