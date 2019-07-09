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

class IntReading extends IntBenchmark {
  @Benchmark
  def avSystemGenCodec(): Int = JsonStringInput.read[Int](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Int = decode[Int](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Int = dslJsonDecode[Int](jsonBytes)

  @Benchmark
  def jacksonScala(): Int = jacksonMapper.readValue[Int](jsonBytes)

  @Benchmark
  def jsoniterJava(): Int = JsoniterJavaParser.parse(jsonBytes, classOf[Int])

  @Benchmark
  def jsoniterScala(): Int = readFromArray[Int](jsonBytes)(intCodec)

  @Benchmark
  def playJson(): Int = Json.parse(jsonBytes).as[Int]

  @Benchmark
  def scalikeJackson(): Int = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Int]
  }

  @Benchmark
  def sprayJson(): Int = JsonParser(jsonBytes).convertTo[Int]

  @Benchmark
  def uPickle(): Int = read[Int](jsonBytes)
}