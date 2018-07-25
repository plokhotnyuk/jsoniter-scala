package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.GoogleMapsAPI._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._

class GoogleMapsAPIBenchmark extends CommonParams {
  var obj: DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes)

  @Benchmark
  def readAVSystemGenCodec(): DistanceMatrix = JsonStringInput.read[DistanceMatrix](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException
  @Benchmark
  def readDslJsonJava(): DistanceMatrix = decodeDslJson[DistanceMatrix](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes)

  @Benchmark
  def readJsoniterScala(): DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes)

  @Benchmark
  def readPlayJson(): DistanceMatrix = Json.parse(jsonBytes).as[DistanceMatrix](googleMapsAPIFormat)

  @Benchmark
  def readUPickle(): DistanceMatrix = read[DistanceMatrix](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[DistanceMatrix](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(googleMapsAPIFormat))

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}