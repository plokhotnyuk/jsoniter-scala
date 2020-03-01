package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GitHubActionsAPI._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import org.openjdk.jmh.annotations.Benchmark
import spray.json.JsonParser

class GitHubActionsAPIReading extends GitHubActionsAPIBenchmark {
/* FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected Bool but got Chars (input position 361)
  @Benchmark
  def borerJson(): GitHubActionsAPI.Response =
    io.bullet.borer.Json.decode(jsonBytes).to[GitHubActionsAPI.Response].value
*/
  @Benchmark
  def jacksonScala(): GitHubActionsAPI.Response = jacksonMapper.readValue[GitHubActionsAPI.Response](jsonBytes)

  @Benchmark
  def jsoniterScala(): GitHubActionsAPI.Response = readFromArray[GitHubActionsAPI.Response](jsonBytes)

  @Benchmark
  def sprayJson(): GitHubActionsAPI.Response = JsonParser(jsonBytes).convertTo[GitHubActionsAPI.Response]

  @Benchmark
  def weePickle(): GitHubActionsAPI.Response = FromJson(jsonBytes).transform(ToScala[GitHubActionsAPI.Response])
}