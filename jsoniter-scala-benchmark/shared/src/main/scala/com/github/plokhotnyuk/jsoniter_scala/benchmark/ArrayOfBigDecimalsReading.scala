package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfBigDecimalsReading extends ArrayOfBigDecimalsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[BigDecimal] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[BigDecimal]](new String(jsonBytes, UTF_8), jsonOptions)
  }

  @Benchmark
  def borer(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders.decodingConfig
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[BigDecimal]].value
  }

  @Benchmark
  def circe(): Array[BigDecimal] = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[BigDecimal]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[BigDecimal] = {
    import io.circe.jawn._

    decodeByteArray[Array[BigDecimal]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[BigDecimal]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[BigDecimal] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[BigDecimal]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[BigDecimal] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[BigDecimal]]
  }

  @Benchmark
  def smithy4sJson(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[BigDecimal]]
  }

  @Benchmark
  def uPickle(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[BigDecimal] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[BigDecimal]])
  }

  @Benchmark
  def zioJson(): Array[BigDecimal] = {
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[BigDecimal]].fold(sys.error, identity)
  }
}