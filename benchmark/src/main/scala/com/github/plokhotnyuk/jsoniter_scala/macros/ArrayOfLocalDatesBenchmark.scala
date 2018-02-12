package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.LocalDate

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfLocalDatesBenchmark extends CommonParams {
  val obj: Array[LocalDate] = (1 to 128).map(i => LocalDate.ofEpochDay(i * 256)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[LocalDate] = decode[Array[LocalDate]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[LocalDate] = jacksonMapper.readValue[Array[LocalDate]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[LocalDate] = read[Array[LocalDate]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[LocalDate] = Json.parse(jsonBytes).as[Array[LocalDate]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME Jackson serializes LocalData in format [<year>,<month>,<day>]
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = write(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}