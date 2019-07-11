package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ListOfBooleansReading extends ListOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): List[Boolean] = JsonStringInput.read[List[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): List[Boolean] = io.bullet.borer.Json.decode(jsonBytes).to[List[Boolean]].value

  @Benchmark
  def circe(): List[Boolean] = decode[List[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): List[Boolean] = dslJsonDecode[List[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): List[Boolean] = jacksonMapper.readValue[List[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): List[Boolean] = readFromArray[List[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): List[Boolean] = Json.parse(jsonBytes).as[List[Boolean]]

  @Benchmark
  def scalikeJackson(): List[Boolean] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[List[Boolean]]
  }

  @Benchmark
  def sprayJson(): List[Boolean] = JsonParser(jsonBytes).convertTo[List[Boolean]]

  @Benchmark
  def uPickle(): List[Boolean] = read[List[Boolean]](jsonBytes)
}