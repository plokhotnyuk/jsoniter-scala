package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ArrayOfShortsReading extends ArrayOfShortsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Short] = JsonStringInput.read[Array[Short]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Short] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Short]].value

  @Benchmark
  def circe(): Array[Short] = decode[Array[Short]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Short] = dslJsonDecode[Array[Short]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Short] = jacksonMapper.readValue[Array[Short]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Short] = readFromArray[Array[Short]](jsonBytes)

  @Benchmark
  def playJson(): Array[Short] = Json.parse(jsonBytes).as[Array[Short]]

  @Benchmark
  def playJsonJsoniter(): Array[Short] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Short]])

  @Benchmark
  def sprayJson(): Array[Short] = JsonParser(jsonBytes).convertTo[Array[Short]]

  @Benchmark
  def uPickle(): Array[Short] = read[Array[Short]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Short] = FromJson(jsonBytes).transform(ToScala[Array[Short]])
}