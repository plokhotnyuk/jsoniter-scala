package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalaJackStuff.sj
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class AnyRefsReading extends AnyRefsBenchmark {
  @Benchmark
  def avSystemGenCodec(): AnyRefs = JsonStringInput.read[AnyRefs](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): AnyRefs = io.bullet.borer.Json.decode(jsonBytes).to[AnyRefs].value

  @Benchmark
  def circe(): AnyRefs = decode[AnyRefs](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): AnyRefs = dslJsonDecode[AnyRefs](jsonBytes)

  @Benchmark
  def jacksonScala(): AnyRefs = jacksonMapper.readValue[AnyRefs](jsonBytes)

  @Benchmark
  def jsoniterScala(): AnyRefs = readFromArray[AnyRefs](jsonBytes)

  @Benchmark
  def playJson(): AnyRefs = Json.parse(jsonBytes).as[AnyRefs]

  @Benchmark
  def scalaJack(): AnyRefs = sj.read[AnyRefs](new String(jsonBytes, UTF_8))

  @Benchmark
  def sprayJson(): AnyRefs = JsonParser(jsonBytes).convertTo[AnyRefs]

  @Benchmark
  def uPickle(): AnyRefs = read[AnyRefs](jsonBytes)
}