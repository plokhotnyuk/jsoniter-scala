package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.NinnyFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import spray.json.JsonParser
import zio.json._
import nrktkt.ninny._
import scala.collection.immutable.ArraySeq

class GitHubActionsAPIReading extends GitHubActionsAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): GitHubActionsAPI.Response =
    JsonStringInput.read[GitHubActionsAPI.Response](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): GitHubActionsAPI.Response = io.bullet.borer.Json.decode(jsonBytes).to[GitHubActionsAPI.Response].value

  @Benchmark
  def circe(): GitHubActionsAPI.Response =
    decode[GitHubActionsAPI.Response](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): GitHubActionsAPI.Response =
    io.circe.jawn.decodeByteArray[GitHubActionsAPI.Response](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[GitHubActionsAPI.Response].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): GitHubActionsAPI.Response = jacksonMapper.readValue[GitHubActionsAPI.Response](jsonBytes)

  @Benchmark
  def jsoniterScala(): GitHubActionsAPI.Response = readFromArray[GitHubActionsAPI.Response](jsonBytes)

  @Benchmark
  def ninnyJson(): GitHubActionsAPI.Response =
    Json.parseArray(ArraySeq.unsafeWrapArray(jsonBytes)).to[GitHubActionsAPI.Response].get

  @Benchmark
  def sprayJson(): GitHubActionsAPI.Response = JsonParser(jsonBytes).convertTo[GitHubActionsAPI.Response]

  @Benchmark
  def uPickle(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[GitHubActionsAPI.Response](jsonBytes)
  }

  @Benchmark
  def weePickle(): GitHubActionsAPI.Response = FromJson(jsonBytes).transform(ToScala[GitHubActionsAPI.Response])

  @Benchmark
  def zioJson(): GitHubActionsAPI.Response =
    new String(jsonBytes, UTF_8).fromJson[GitHubActionsAPI.Response].fold(sys.error, identity)
}