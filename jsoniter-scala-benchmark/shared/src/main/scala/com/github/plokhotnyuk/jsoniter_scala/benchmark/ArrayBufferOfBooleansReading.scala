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
import spray.json._
import upickle.default._
import scala.collection.mutable

class ArrayBufferOfBooleansReading extends ArrayBufferOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.ArrayBuffer[Boolean] =
    JsonStringInput.read[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): mutable.ArrayBuffer[Boolean] =
    io.bullet.borer.Json.decode(jsonBytes).to[mutable.ArrayBuffer[Boolean]].value

  @Benchmark
  def circe(): mutable.ArrayBuffer[Boolean] =
    decode[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): mutable.ArrayBuffer[Boolean] =
    io.circe.jawn.decodeByteArray[mutable.ArrayBuffer[Boolean]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): mutable.ArrayBuffer[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[mutable.ArrayBuffer[Boolean]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): mutable.ArrayBuffer[Boolean] = dslJsonDecode[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): mutable.ArrayBuffer[Boolean] = jacksonMapper.readValue[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): mutable.ArrayBuffer[Boolean] = readFromArray[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): mutable.ArrayBuffer[Boolean] = Json.parse(jsonBytes).as[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def playJsonJsoniter(): mutable.ArrayBuffer[Boolean] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[mutable.ArrayBuffer[Boolean]])

  @Benchmark
  def sprayJson(): mutable.ArrayBuffer[Boolean] = JsonParser(jsonBytes).convertTo[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def uPickle(): mutable.ArrayBuffer[Boolean] = read[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def weePickle(): mutable.ArrayBuffer[Boolean] = FromJson(jsonBytes).transform(ToScala[mutable.ArrayBuffer[Boolean]])
}