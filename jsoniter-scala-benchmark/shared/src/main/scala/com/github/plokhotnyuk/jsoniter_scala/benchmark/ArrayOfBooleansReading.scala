package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONNonGenEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._
import zio.json.DecoderOps

class ArrayOfBooleansReading extends ArrayOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Boolean] = JsonStringInput.read[Array[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Boolean] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Boolean]].value

  @Benchmark
  def circe(): Array[Boolean] = decode[Array[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Boolean] = dslJsonDecode[Array[Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Boolean] = jacksonMapper.readValue[Array[Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Boolean] = readFromArray[Array[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): Array[Boolean] = Json.parse(jsonBytes).as[Array[Boolean]]

  @Benchmark
  def playJsonJsoniter(): Array[Boolean] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Boolean]])

  @Benchmark
  def sprayJson(): Array[Boolean] = JsonParser(jsonBytes).convertTo[Array[Boolean]]

  @Benchmark
  def uPickle(): Array[Boolean] = read[Array[Boolean]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Boolean] = FromJson(jsonBytes).transform(ToScala[Array[Boolean]])

  @Benchmark
  def zioJson(): Array[Boolean] = new String(jsonBytes, UTF_8).fromJson[Array[Boolean]].fold(sys.error, identity)
}