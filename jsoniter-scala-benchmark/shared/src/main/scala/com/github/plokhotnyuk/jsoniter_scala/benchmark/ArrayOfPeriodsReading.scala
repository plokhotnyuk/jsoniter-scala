package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Period
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfPeriodsReading extends ArrayOfPeriodsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Period] = JsonStringInput.read[Array[Period]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Period] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Period]].value

  @Benchmark
  def circe(): Array[Period] = decode[Array[Period]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[Period] = jacksonMapper.readValue[Array[Period]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Period] = readFromArray[Array[Period]](jsonBytes)

  @Benchmark
  def playJson(): Array[Period] = Json.parse(jsonBytes).as[Array[Period]]

  @Benchmark
  def playJsonJsoniter(): Array[Period] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Period]])

  @Benchmark
  def sprayJson(): Array[Period] = JsonParser(jsonBytes).convertTo[Array[Period]]

  @Benchmark
  def uPickle(): Array[Period] = read[Array[Period]](jsonBytes)
}