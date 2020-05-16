package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Year

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfYearsReading extends ArrayOfYearsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Year] = JsonStringInput.read[Array[Year]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Year] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Year]].value

  @Benchmark
  def circe(): Array[Year] = decode[Array[Year]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[Year] = jacksonMapper.readValue[Array[Year]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Year] = readFromArray[Array[Year]](jsonBytes)

  @Benchmark
  def playJson(): Array[Year] = Json.parse(jsonBytes).as[Array[Year]]

  @Benchmark
  def sprayJson(): Array[Year] = JsonParser(jsonBytes).convertTo[Array[Year]]

  @Benchmark
  def uPickle(): Array[Year] = read[Array[Year]](jsonBytes)
}