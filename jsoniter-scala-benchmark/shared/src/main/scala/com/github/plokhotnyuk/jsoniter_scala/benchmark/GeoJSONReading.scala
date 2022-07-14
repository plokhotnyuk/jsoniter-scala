package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.GeoJSON._
import org.openjdk.jmh.annotations.Benchmark

class GeoJSONReading extends GeoJSONBenchmark {
  @Benchmark
  def avSystemGenCodec(): GeoJSON = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[GeoJSON](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[GeoJSON].value
  }

  @Benchmark
  def circe(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[GeoJSON](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[GeoJSON](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[GeoJSON].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
    jacksonMapper.readValue[GeoJSON](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GeoJSON](jsonBytes)
  }

  @Benchmark
  def playJson(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[GeoJSON](geoJSONFormat)
  }

  @Benchmark
  def playJsonJsoniter(): GeoJSON = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[GeoJSON](geoJSONFormat)
  }

  @Benchmark
  def smithy4sJson(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[GeoJSON](jsonBytes)
  }

  @Benchmark
  def sprayJson(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[GeoJSON](geoJSONJsonFormat)
  }

  @Benchmark
  def uPickle(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[GeoJSON](jsonBytes)
  }

  @Benchmark
  def weePickle(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[GeoJSON])
  }

  @Benchmark
  def zioJson(): GeoJSON = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[GeoJSON].fold(sys.error, identity)
  }
}