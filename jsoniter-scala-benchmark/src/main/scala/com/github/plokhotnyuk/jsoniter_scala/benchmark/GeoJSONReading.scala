package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class GeoJSONReading extends GeoJSONBenchmark {
  @Benchmark
  def avSystemGenCodec(): GeoJSON = JsonStringInput.read[GeoJSON](new String(jsonBytes, UTF_8))
/* FIXME: Borer throws: io.bullet.borer.Borer$Error$InvalidInputData: Expected type id key for subtype of `com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON.Geometry` but got String (input position 104)
  @Benchmark
  def borerJson(): GeoJSON = io.bullet.borer.Json.decode(jsonBytes).to[GeoJSON].value
*/
  @Benchmark
  def circe(): GeoJSON = decode[GeoJSON](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): GeoJSON = jacksonMapper.readValue[GeoJSON](jsonBytes)

  @Benchmark
  def jsoniterScala(): GeoJSON = readFromArray[GeoJSON](jsonBytes)

  @Benchmark
  def playJson(): GeoJSON = Json.parse(jsonBytes).as[GeoJSON](geoJSONFormat)

  @Benchmark
  def sprayJson(): GeoJSON = JsonParser(jsonBytes).convertTo[GeoJSON](geoJSONJsonFormat)

  @Benchmark
  def uPickle(): GeoJSON = read[GeoJSON](jsonBytes)
}