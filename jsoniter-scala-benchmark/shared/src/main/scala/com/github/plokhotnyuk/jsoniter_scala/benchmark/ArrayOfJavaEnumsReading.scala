package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfJavaEnumsReading extends ArrayOfJavaEnumsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Suit] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[Suit]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Suit]].value
  }

  @Benchmark
  def circe(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[Suit]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[Array[Suit]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Suit]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[Suit]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Suit]](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Suit]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Suit]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Suit] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[Suit]]
  }

  @Benchmark
  def sprayJson(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Suit]]
  }

  @Benchmark
  def uPickle(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[Suit]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Suit] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Suit]])
  }
}