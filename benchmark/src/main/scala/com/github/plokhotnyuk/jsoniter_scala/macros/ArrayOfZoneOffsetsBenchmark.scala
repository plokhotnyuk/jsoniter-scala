package com.github.plokhotnyuk.jsoniter_scala.macros

//import java.nio.charset.StandardCharsets._
import java.time.ZoneOffset

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import io.circe.java8.time._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json

class ArrayOfZoneOffsetsBenchmark extends CommonParams {
  val obj: Array[ZoneOffset] = (1 to 128).map(i => ZoneOffset.ofHoursMinutesSeconds(i % 17, (i % 4) * 15, 0)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes
/* FIXME Circe require custom decoder
  @Benchmark
  def readCirce(): Array[ZoneOffset] = decode[Array[ZoneOffset]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
  @Benchmark
  def readJacksonScala(): Array[ZoneOffset] = jacksonMapper.readValue[Array[ZoneOffset]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[ZoneOffset] = JsonReader.read[Array[ZoneOffset]](jsonBytes)
/* FIXME Play json require custom format
  @Benchmark
  def readPlayJson(): Array[ZoneOffset] = Json.parse(jsonBytes).as[Array[ZoneOffset]]
*/
/* FIXME Circe require custom encoder
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(obj, preallocatedBuf, 0)
/* FIXME Play json require custom format
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
}