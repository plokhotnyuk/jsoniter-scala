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
  def readGoogleMapsAPICirce(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readGoogleMapsAPIJackson(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes)

  @Benchmark
  def readGoogleMapsAPIJsoniter(): DistanceMatrix = JsonReader.read(googleMapsAPICodec, jsonBytes)

  @Benchmark
  def readGoogleMapsAPIPlay(): DistanceMatrix = Json.parse(jsonBytes).as[DistanceMatrix](googleMapsAPIFormat)

  @Benchmark
  def writeGoogleMapsAPICirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeGoogleMapsAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeGoogleMapsAPIJsoniter(): Array[Byte] = JsonWriter.write(googleMapsAPICodec, obj)

  @Benchmark
  def writeGoogleMapsAPIJsoniterPrealloc(): Int = JsonWriter.write(googleMapsAPICodec, obj, preallocatedBuf.get, 0)

  @Benchmark
  def writeGoogleMapsAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(googleMapsAPIFormat))
}