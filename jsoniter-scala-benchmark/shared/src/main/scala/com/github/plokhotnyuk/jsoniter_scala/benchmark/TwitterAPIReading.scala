package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import org.openjdk.jmh.annotations.Benchmark

class TwitterAPIReading extends TwitterAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): Seq[Tweet] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Seq[Tweet]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Seq[Tweet]].value
  }

  @Benchmark
  def circe(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[Seq[Tweet]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Seq[Tweet]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Seq[Tweet] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Seq[Tweet]]
  }

  @Benchmark
  def json4sNative(): Seq[Tweet] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Seq[Tweet]]
  }

  @Benchmark
  def jsoniterScala(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def playJson(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Seq[Tweet]]
  }

  @Benchmark
  def playJsonJsoniter(): Seq[Tweet] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Seq[Tweet]]
  }

  @Benchmark
  def smithy4sJson(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Seq[Tweet]]
  }

  @Benchmark
  def uPickle(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Seq[Tweet]])
  }

  @Benchmark
  def zioJson(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import zio.json.JsonDecoder._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Seq[Tweet]].fold(sys.error, identity)
  }
}