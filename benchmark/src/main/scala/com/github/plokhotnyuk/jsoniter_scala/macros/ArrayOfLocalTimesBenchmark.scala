package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._
import java.time.LocalTime

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.java8.time._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfLocalTimesBenchmark extends CommonParams {
  val obj: Array[LocalTime] = (1 to 128).map(i => LocalTime.ofSecondOfDay((i * 127) | 1)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[LocalTime] = decode[Array[LocalTime]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[LocalTime] = jacksonMapper.readValue[Array[LocalTime]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[LocalTime] = read[Array[LocalTime]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[LocalTime] = Json.parse(jsonBytes).as[Array[LocalTime]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/* FIXME jackson serializes LocalTime as array of numbers
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