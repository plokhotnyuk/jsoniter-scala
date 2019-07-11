package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ArrayOfShortsReading extends ArrayOfShortsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Short] = JsonStringInput.read[Array[Short]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Short] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Short]].value

  @Benchmark
  def circe(): Array[Short] = decode[Array[Short]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Short] = dslJsonDecode[Array[Short]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Short] = jacksonMapper.readValue[Array[Short]](jsonBytes)

  @Benchmark
  def jsoniterJava(): Array[Short] = JsoniterJavaParser.parse[Array[Short]](jsonBytes, classOf[Array[Short]])

  @Benchmark
  def jsoniterScala(): Array[Short] = readFromArray[Array[Short]](jsonBytes)

  @Benchmark
  def playJson(): Array[Short] = Json.parse(jsonBytes).as[Array[Short]]

  @Benchmark
  def scalikeJackson(): Array[Short] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Array[Short]]
  }

  @Benchmark
  def sprayJson(): Array[Short] = JsonParser(jsonBytes).convertTo[Array[Short]]

  @Benchmark
  def uPickle(): Array[Short] = read[Array[Short]](jsonBytes)
}