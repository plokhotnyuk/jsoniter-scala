package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._

class GeoJSONBenchmark extends CommonParams {
  var obj: GeoJSON = readFromArray[GeoJSON](jsonBytes1)(geoJSONCodec)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes1.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): GeoJSON = JsonStringInput.read[GeoJSON](new String(jsonBytes1, UTF_8))

  @Benchmark
  def readCirce(): GeoJSON = decode[GeoJSON](new String(jsonBytes1, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): GeoJSON = jacksonMapper.readValue[GeoJSON](jsonBytes1)

  @Benchmark
  def readJsoniterScala(): GeoJSON = readFromArray[GeoJSON](jsonBytes1)(geoJSONCodec)

  @Benchmark
  def readPlayJson(): GeoJSON = Json.parse(jsonBytes1).as[GeoJSON](geoJSONFormat)
/* FIXME: cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def readUPickle(): GeoJSON = read[GeoJSON](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(geoJSONCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(geoJSONCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(geoJSONFormat))
/* FIXME: cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}