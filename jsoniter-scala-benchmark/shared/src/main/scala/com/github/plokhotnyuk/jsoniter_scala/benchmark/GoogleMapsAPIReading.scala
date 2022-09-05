package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI._
import org.openjdk.jmh.annotations.Benchmark

class GoogleMapsAPIReading extends GoogleMapsAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): DistanceMatrix = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[DistanceMatrix](new String(jsonBytes1, UTF_8))
  }

  @Benchmark
  def borer(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes1).to[DistanceMatrix].value
  }

  @Benchmark
  def circe(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[DistanceMatrix](new String(jsonBytes1, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[DistanceMatrix](jsonBytes1).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[DistanceMatrix].decodeJson(readFromArray(jsonBytes1)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[DistanceMatrix](jsonBytes1)
  }

  @Benchmark
  def jacksonScala(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[DistanceMatrix](jsonBytes1)
  }

  @Benchmark
  def jsoniterScala(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[DistanceMatrix](jsonBytes1)
  }

  @Benchmark
  def playJson(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes1).as[DistanceMatrix]
  }

  @Benchmark
  def playJsonJsoniter(): DistanceMatrix = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes1).as[DistanceMatrix]
  }

  @Benchmark
  def smithy4sJson(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[DistanceMatrix](jsonBytes1)
  }

  @Benchmark
  def sprayJson(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes1).convertTo[DistanceMatrix]
  }

  @Benchmark
  def uPickle(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[DistanceMatrix](jsonBytes1)
  }

  @Benchmark
  def weePickle(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes1).transform(ToScala[DistanceMatrix])
  }

  @Benchmark
  def zioJson(): DistanceMatrix = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
    import zio.json._
    import zio.json.JsonDecoder._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes1, UTF_8).fromJson[DistanceMatrix].fold(sys.error, identity)
  }
}