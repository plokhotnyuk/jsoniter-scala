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

class ArrayOfIntsReading extends ArrayOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Int] = JsonStringInput.read[Array[Int]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Int] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Int]].value

  @Benchmark
  def circe(): Array[Int] = decode[Array[Int]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Int] = dslJsonDecode[Array[Int]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Int] = jacksonMapper.readValue[Array[Int]](jsonBytes)

  @Benchmark
  def jsoniterJava(): Array[Int] = JsoniterJavaParser.parse[Array[Int]](jsonBytes, classOf[Array[Int]])

  @Benchmark
  def jsoniterScala(): Array[Int] = readFromArray[Array[Int]](jsonBytes)

  @Benchmark
  def playJson(): Array[Int] = Json.parse(jsonBytes).as[Array[Int]]

  @Benchmark
  def scalikeJackson(): Array[Char] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Array[Char]]
  }

  @Benchmark
  def sprayJson(): Array[Int] = JsonParser(jsonBytes).convertTo[Array[Int]]

  @Benchmark
  def uPickle(): Array[Int] = read[Array[Int]](jsonBytes)
}