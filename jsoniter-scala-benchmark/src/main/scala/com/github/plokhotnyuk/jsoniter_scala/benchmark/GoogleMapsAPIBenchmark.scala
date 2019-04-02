package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class GoogleMapsAPIBenchmark extends CommonParams {
  var obj: DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): DistanceMatrix = JsonStringInput.read[DistanceMatrix](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException
  @Benchmark
  def readDslJsonScala(): DistanceMatrix = dslJsonDecode[DistanceMatrix](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes)

  @Benchmark
  def readJsoniterScala(): DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes)

  @Benchmark
  def readPlayJson(): DistanceMatrix = Json.parse(jsonBytes).as[DistanceMatrix]

  @Benchmark
  def readSprayJson(): DistanceMatrix = JsonParser(jsonBytes).convertTo[DistanceMatrix]

  @Benchmark
  def readUPickle(): DistanceMatrix = read[DistanceMatrix](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}