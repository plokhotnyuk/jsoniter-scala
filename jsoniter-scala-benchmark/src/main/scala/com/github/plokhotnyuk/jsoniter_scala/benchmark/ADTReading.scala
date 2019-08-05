package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
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

class ADTReading extends ADTBenchmark {
  @Benchmark
  def avSystemGenCodec(): ADTBase = JsonStringInput.read[ADTBase](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): ADTBase = io.bullet.borer.Json.decode(jsonBytes).to[ADTBase].value

  @Benchmark
  def circe(): ADTBase = decode[ADTBase](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): ADTBase = jacksonMapper.readValue[ADTBase](jsonBytes)

  @Benchmark
  def jsoniterScala(): ADTBase = readFromArray[ADTBase](jsonBytes)

  @Benchmark
  def playJson(): ADTBase = Json.parse(jsonBytes).as[ADTBase](adtFormat)

  @Benchmark
  def sprayJson(): ADTBase = JsonParser(jsonBytes).convertTo[ADTBase](adtBaseJsonFormat)

  @Benchmark
  def uPickle(): ADTBase = read[ADTBase](jsonBytes)
}