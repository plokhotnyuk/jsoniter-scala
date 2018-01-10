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
  val obj: DistanceMatrix = JsonReader.read(googleMapsAPICodec, jsonBytes)

  @Benchmark
  def readCirce(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes)

  @Benchmark
  def readJsoniter(): DistanceMatrix = JsonReader.read(googleMapsAPICodec, jsonBytes)

  @Benchmark
  def readPlay(): DistanceMatrix = Json.parse(jsonBytes).as[DistanceMatrix](googleMapsAPIFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(googleMapsAPICodec, obj)

  @Benchmark
  def writeJsoniterPrealloc(): Int = JsonWriter.write(googleMapsAPICodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(googleMapsAPIFormat))
}