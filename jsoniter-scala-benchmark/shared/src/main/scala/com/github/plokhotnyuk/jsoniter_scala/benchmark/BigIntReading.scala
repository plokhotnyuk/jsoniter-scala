package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class BigIntReading extends BigIntBenchmark {
  @Benchmark
  def avSystemGenCodec(): BigInt = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[BigInt](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[BigInt].value
  }

  @Benchmark
  def circe(): BigInt = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[BigInt](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): BigInt = {
    import io.circe.jawn._

    decodeByteArray[BigInt](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[BigInt].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[BigInt](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BigInt](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntCodec)
  }
/* FIXME: Play-JSON looses significant digits in BigInt values
  @Benchmark
  def playJson(): BigInt = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[BigInt]
  }
*/
  @Benchmark
  def smithy4sJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntJCodec)
  }

  @Benchmark
  def sprayJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes, jsonParserSettings).convertTo[BigInt]
  }

  @Benchmark
  def uPickle(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[BigInt](jsonBytes)
  }

  @Benchmark
  def weePickle(): BigInt = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BigInt])
  }
}