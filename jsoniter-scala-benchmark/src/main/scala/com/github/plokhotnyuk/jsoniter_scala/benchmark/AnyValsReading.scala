package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.FlatSprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class AnyValsReading extends AnyValsBenchmark {
  @Benchmark
  def avSystemGenCodec(): AnyVals = JsonStringInput.read[AnyVals](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): AnyVals = io.bullet.borer.Json.decode(jsonBytes).to[AnyVals].value

  @Benchmark
  def circe(): AnyVals = decode[AnyVals](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for com.github.plokhotnyuk.jsoniter_scala.benchmark.AnyVals
  @Benchmark
  def dslJsonScala(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[AnyVals](jsonBytes)
  }
*/
  @Benchmark
  def jacksonScala(): AnyVals = jacksonMapper.readValue[AnyVals](jsonBytes)

  @Benchmark
  def jsoniterScala(): AnyVals = readFromArray[AnyVals](jsonBytes)

  @Benchmark
  def playJson(): AnyVals = Json.parse(jsonBytes).as[AnyVals]

  @Benchmark
  def sprayJson(): AnyVals = JsonParser(jsonBytes).convertTo[AnyVals]

  @Benchmark
  def uPickle(): AnyVals = read[AnyVals](jsonBytes)
/* FIXME: weePickle doesn't derive for AnyVal types?
  @Benchmark
  def weePickle(): AnyVals = FromJson(jsonBytes).transform(ToScala[AnyVals])
*/
}