package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.GoogleMapsAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class GoogleMapsAPIReading extends GoogleMapsAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): DistanceMatrix = JsonStringInput.read[DistanceMatrix](new String(jsonBytes1, UTF_8))

  @Benchmark
  def circe(): DistanceMatrix = decode[DistanceMatrix](new String(jsonBytes1, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): DistanceMatrix = dslJsonDecode[DistanceMatrix](jsonBytes1)

  @Benchmark
  def jacksonScala(): DistanceMatrix = jacksonMapper.readValue[DistanceMatrix](jsonBytes1)

  @Benchmark
  def jsoniterScala(): DistanceMatrix = readFromArray[DistanceMatrix](jsonBytes1)

  @Benchmark
  def playJson(): DistanceMatrix = Json.parse(jsonBytes1).as[DistanceMatrix]

  @Benchmark
  def scalikeJackson(): DistanceMatrix = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes1, UTF_8).read[DistanceMatrix]
  }

  @Benchmark
  def sprayJson(): DistanceMatrix = JsonParser(jsonBytes1).convertTo[DistanceMatrix]

  @Benchmark
  def uPickle(): DistanceMatrix = read[DistanceMatrix](jsonBytes1)
}