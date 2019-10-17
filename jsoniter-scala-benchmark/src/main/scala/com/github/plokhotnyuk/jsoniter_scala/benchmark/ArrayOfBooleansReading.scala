package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ArrayOfBooleansReading extends ArrayOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Boolean] = JsonStringInput.read[Array[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Boolean] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Boolean]].value

  @Benchmark
  def circe(): Array[Boolean] = decode[Array[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Boolean] = dslJsonDecode[Array[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Boolean] = jacksonMapper.readValue[Array[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterJava(): Array[Boolean] = JsoniterJavaParser.parse[Array[Boolean]](jsonBytes, classOf[Array[Boolean]])

  @Benchmark
  def jsoniterScala(): Array[Boolean] = readFromArray[Array[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): Array[Boolean] = Json.parse(jsonBytes).as[Array[Boolean]]

  @Benchmark
  def sprayJson(): Array[Boolean] = JsonParser(jsonBytes).convertTo[Array[Boolean]]

  @Benchmark
  def uPickle(): Array[Boolean] = read[Array[Boolean]](jsonBytes)
}