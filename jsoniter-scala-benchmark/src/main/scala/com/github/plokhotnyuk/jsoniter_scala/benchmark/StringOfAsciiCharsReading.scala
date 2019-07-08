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
import upickle.default._

class StringOfAsciiCharsReading extends StringOfAsciiCharsBenchmark {
  @Benchmark
  def avSystemGenCodec(): String = JsonStringInput.read[String](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): String = dslJsonDecode[String](jsonBytes)(stringDecoder)

  @Benchmark
  def jacksonScala(): String = jacksonMapper.readValue[String](jsonBytes)

  @Benchmark
  def jsoniterJava(): String = JsoniterJavaParser.parse[String](jsonBytes, classOf[String])

  @Benchmark
  def jsoniterScala(): String = readFromArray[String](jsonBytes)(stringCodec)

  @Benchmark
  def scalikeJackson(): String = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[String]
  }

  @Benchmark
  def playJson(): String = Json.parse(jsonBytes).as[String]

  @Benchmark
  def sprayJson(): String = spray.json.JsonParser(jsonBytes).convertTo[String]

  @Benchmark
  def uPickle(): String = read[String](jsonBytes)
}