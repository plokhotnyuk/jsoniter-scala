package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import spray.json._

class NestedStructsReading extends NestedStructsBenchmark {
  @Benchmark
  def avSystemGenCodec(): NestedStructs = JsonStringInput.read[NestedStructs](new String(jsonBytes, UTF_8))
/* FIXME: Borer doesn't support recusive structures, see https://github.com/sirthias/borer/issues/28
  @Benchmark
  def borerJson(): NestedStructs = io.bullet.borer.Json.decode(jsonBytes).to[NestedStructs].value
*/
  @Benchmark
  def circe(): NestedStructs = decode[NestedStructs](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): NestedStructs = dslJsonDecode[NestedStructs](jsonBytes)

  @Benchmark
  def jacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def jsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)

  @Benchmark
  def playJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs]

  @Benchmark
  def scalikeJackson(): NestedStructs = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[NestedStructs]
  }

/*
  @Benchmark
  def sprayJson(): NestedStructs = JsonParser(jsonBytes).convertTo[NestedStructs](nestedStructsJsonFormat)
*/

  @Benchmark
  def uPickle(): NestedStructs = read[NestedStructs](jsonBytes)
}