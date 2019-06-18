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

import scala.collection.mutable

class ArrayBufferOfBooleansReading extends ArrayBufferOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.ArrayBuffer[Boolean] =
    JsonStringInput.read[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): mutable.ArrayBuffer[Boolean] =
    io.bullet.borer.Json.decode(jsonBytes).to[mutable.ArrayBuffer[Boolean]].value

  @Benchmark
  def circe(): mutable.ArrayBuffer[Boolean] =
    decode[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): mutable.ArrayBuffer[Boolean] = dslJsonDecode[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): mutable.ArrayBuffer[Boolean] = jacksonMapper.readValue[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): mutable.ArrayBuffer[Boolean] = readFromArray[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): mutable.ArrayBuffer[Boolean] = Json.parse(jsonBytes).as[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def scalikeJackson(): mutable.ArrayBuffer[Boolean] = {
    import reug.scalikejackson.ScalaJacksonImpl._
    new String(jsonBytes, UTF_8).read[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  def sprayJson(): mutable.ArrayBuffer[Boolean] = JsonParser(jsonBytes).convertTo[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def uPickle(): mutable.ArrayBuffer[Boolean] = read[mutable.ArrayBuffer[Boolean]](jsonBytes)
}