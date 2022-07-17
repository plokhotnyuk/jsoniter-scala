package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class BigDecimalReading extends BigDecimalBenchmark {
  @Benchmark
  def avSystemGenCodec(): BigDecimal = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[BigDecimal](new String(jsonBytes, UTF_8), jsonOptions)
  }

  @Benchmark
  def borer(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[BigDecimal].value
  }

  @Benchmark
  def circe(): BigDecimal = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[BigDecimal](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): BigDecimal = {
    import io.circe.jawn._

    decodeByteArray[BigDecimal](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BigDecimal].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[BigDecimal](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BigDecimal](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigDecimal](jsonBytes)(bigDecimalCodec)
  }
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def playJson(): BigDecimal = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[BigDecimal]
  }
*/
/* FIXME: smithy4sJson: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def smithy4sJson(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigDecimal](jsonBytes)(bigDecimalJCodec)
  }
*/
  @Benchmark
  def sprayJson(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes, jsonParserSettings).convertTo[BigDecimal]
  }

  @Benchmark
  def uPickle(): BigDecimal = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[BigDecimal](jsonBytes)
  }

  @Benchmark
  def weePickle(): BigDecimal = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BigDecimal])
  }
}