package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class VectorOfBooleansReading extends VectorOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): Vector[Boolean] = JsonStringInput.read[Vector[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Vector[Boolean] = io.bullet.borer.Json.decode(jsonBytes).to[Vector[Boolean]].value

  @Benchmark
  def circe(): Vector[Boolean] = decode[Vector[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Vector[Boolean] = dslJsonDecode[Vector[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): Vector[Boolean] = jacksonMapper.readValue[Vector[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Vector[Boolean] = readFromArray[Vector[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): Vector[Boolean] = Json.parse(jsonBytes).as[Vector[Boolean]]

  @Benchmark
  def sprayJson(): Vector[Boolean] = JsonParser(jsonBytes).convertTo[Vector[Boolean]]

  @Benchmark
  def uPickle(): Vector[Boolean] = read[Vector[Boolean]](jsonBytes)
}