package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.fasterxml.jackson.databind.JsonNode
import com.github.pathikrit.dijon._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.bullet.borer.Dom._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class TwitterAPIReading extends TwitterAPIBenchmark {
  @Benchmark
  def borerJson(): Element = io.bullet.borer.Json.decode(jsonBytes).to[Element].value

  @Benchmark
  def circe(): Either[io.circe.ParsingFailure, io.circe.Json] = io.circe.parser.parse(new String(jsonBytes, UTF_8))

  @Benchmark
  def dijon(): SomeJson = readFromArray[SomeJson](jsonBytes)

  @Benchmark
  def jacksonScala(): JsonNode = jacksonMapper.readTree(jsonBytes)

  @Benchmark
  def playJson(): play.api.libs.json.JsValue = Json.parse(jsonBytes)

  @Benchmark
  def sprayJson(): spray.json.JsValue = JsonParser(jsonBytes)

  @Benchmark
  def uJson(): ujson.Value.Value = ujson.read(jsonBytes)
}