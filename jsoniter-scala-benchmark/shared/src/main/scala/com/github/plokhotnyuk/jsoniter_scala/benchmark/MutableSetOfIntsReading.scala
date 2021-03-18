package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._

import scala.collection.mutable

class MutableSetOfIntsReading extends MutableSetOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.Set[Int] = JsonStringInput.read[mutable.Set[Int]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): mutable.Set[Int] = io.bullet.borer.Json.decode(jsonBytes).to[mutable.Set[Int]].value

  @Benchmark
  def circe(): mutable.Set[Int] = decode[mutable.Set[Int]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): mutable.Set[Int] = dslJsonDecode[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def jacksonScala(): mutable.Set[Int] = jacksonMapper.readValue[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def jsoniterScala(): mutable.Set[Int] = readFromArray[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def playJson(): mutable.Set[Int] = Json.parse(jsonBytes).as[mutable.Set[Int]]

  @Benchmark
  def playJsonJsoniter(): mutable.Set[Int] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[mutable.Set[Int]])

  @Benchmark
  def uPickle(): mutable.Set[Int] = read[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def weePickle(): mutable.Set[Int] = FromJson(jsonBytes).transform(ToScala[mutable.Set[Int]])
}