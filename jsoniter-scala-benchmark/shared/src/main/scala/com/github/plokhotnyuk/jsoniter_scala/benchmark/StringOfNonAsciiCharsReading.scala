package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._
import zio.json.DecoderOps

class StringOfNonAsciiCharsReading extends StringOfNonAsciiCharsBenchmark {
  @Benchmark
  def avSystemGenCodec(): String = JsonStringInput.read[String](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): String = io.bullet.borer.Json.decode(jsonBytes).to[String].value

  @Benchmark
  def circe(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): String = io.circe.jawn.decodeByteArray[String](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[String].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): String = dslJsonDecode[String](jsonBytes)(stringDecoder)

  @Benchmark
  def jacksonScala(): String = jacksonMapper.readValue[String](jsonBytes)

  @Benchmark
  def jsoniterScala(): String = readFromArray[String](jsonBytes, tooLongStringConfig)(stringCodec)

  @Benchmark
  def playJson(): String = Json.parse(jsonBytes).as[String]

  @Benchmark
  def playJsonJsoniter(): String = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[String])

  @Benchmark
  def smithy4sJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringJCodec)
  }

  @Benchmark
  def sprayJson(): String = spray.json.JsonParser(jsonBytes).convertTo[String]

  @Benchmark
  def uPickle(): String = read[String](jsonBytes)

  @Benchmark
  def weePickle(): String = FromJson(jsonBytes).transform(ToScala[String])

  @Benchmark
  def zioJson(): String = new String(jsonBytes, UTF_8).fromJson[String].fold(sys.error, identity)
}