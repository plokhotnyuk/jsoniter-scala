package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.GoogleMapsAPI._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class GoogleMapsAPIBenchmark extends CommonParams {
  val obj: DistanceMatrix = JsonReader.read[DistanceMatrix](jsonBytes)

  @Benchmark
  def readCirce(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes)

  @Benchmark
  def readJsoniterScala(): DistanceMatrix = JsonReader.read[DistanceMatrix](jsonBytes)

  @Benchmark
  def readPlayJson(): DistanceMatrix = Json.parse(jsonBytes).as[DistanceMatrix](googleMapsAPIFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(googleMapsAPIFormat))
}