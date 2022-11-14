package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI.DistanceMatrix
import org.openjdk.jmh.annotations.Benchmark

class GitHubActionsAPIReading extends GitHubActionsAPIBenchmark {
  @Benchmark
  def borer(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[GitHubActionsAPI.Response].value
  }

  @Benchmark
  def circe(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[GitHubActionsAPI.Response](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[GitHubActionsAPI.Response].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[GitHubActionsAPI.Response](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): GitHubActionsAPI.Response = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.GitHubActionsAPIJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[GitHubActionsAPI.Response]
  }

  @Benchmark
  def json4sNative(): GitHubActionsAPI.Response = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.GitHubActionsAPIJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[GitHubActionsAPI.Response]
  }

  @Benchmark
  def jsoniterScala(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GitHubActionsAPI.Response](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GitHubActionsAPI.Response](jsonBytes)
  }
/* FIXME: uPickle throws java.lang.NullPointerException: Cannot invoke "upickle.core.Visitor.visitString(java.lang.CharSequence, int)" because the return value of "upickle.core.ObjArrVisitor.subVisitor()" is null
  @Benchmark
  def uPickle(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[GitHubActionsAPI.Response](jsonBytes)
  }
*/
  @Benchmark
  def weePickle(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[GitHubActionsAPI.Response])
  }

  @Benchmark
  def zioJson(): GitHubActionsAPI.Response = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import zio.json.JsonDecoder._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[GitHubActionsAPI.Response].fold(sys.error, identity)
  }
}