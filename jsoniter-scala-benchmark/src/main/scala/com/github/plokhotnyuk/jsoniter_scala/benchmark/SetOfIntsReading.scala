package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
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

import scala.collection.immutable.Set

class SetOfIntsReading extends SetOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Set[Int] = JsonStringInput.read[Set[Int]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Set[Int] = io.bullet.borer.Json.decode(jsonBytes).to[Set[Int]].value

  @Benchmark
  def circe(): Set[Int] = decode[Set[Int]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Set[Int] = dslJsonDecode[Set[Int]](jsonBytes)

  @Benchmark
  def jacksonScala(): Set[Int] = jacksonMapper.readValue[Set[Int]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Set[Int] = readFromArray[Set[Int]](jsonBytes)

  @Benchmark
  def playJson(): Set[Int] = Json.parse(jsonBytes).as[Set[Int]]

  @Benchmark
  def sprayJson(): Set[Int] = JsonParser(jsonBytes).convertTo[Set[Int]]

  @Benchmark
  def uPickle(): Set[Int] = read[Set[Int]](jsonBytes)

  @Benchmark
  def weePickle(): Set[Int] = FromJson(jsonBytes).transform(ToScala[Set[Int]])
}