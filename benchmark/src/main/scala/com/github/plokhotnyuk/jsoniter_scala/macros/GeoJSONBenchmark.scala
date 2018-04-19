package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json

class GeoJSONBenchmark extends CommonParams {
  val obj: GeoJSON = readFromArray[GeoJSON](jsonBytes)

  @Benchmark
  def readCirce(): GeoJSON = decode[GeoJSON](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): GeoJSON = jacksonMapper.readValue[GeoJSON](jsonBytes)

  @Benchmark
  def readJsoniterScala(): GeoJSON = readFromArray[GeoJSON](jsonBytes)
/* FIXME: Play-JSON doesn't support Tuple2?
  @Benchmark
  def readPlayJson(): GeoJSON = Json.parse(jsonBytes).as[GeoJSON](geoJSONFormat)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)
/* FIXME: Play-JSON doesn't support Tuple2?
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(geoJSONFormat))
*/
}