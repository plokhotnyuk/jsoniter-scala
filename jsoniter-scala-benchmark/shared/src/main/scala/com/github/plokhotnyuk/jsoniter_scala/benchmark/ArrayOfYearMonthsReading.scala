package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.YearMonth
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
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

class ArrayOfYearMonthsReading extends ArrayOfYearMonthsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[YearMonth] = JsonStringInput.read[Array[YearMonth]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[YearMonth] = io.bullet.borer.Json.decode(jsonBytes).to[Array[YearMonth]].value

  @Benchmark
  def circe(): Array[YearMonth] = decode[Array[YearMonth]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[YearMonth] = jacksonMapper.readValue[Array[YearMonth]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[YearMonth] = readFromArray[Array[YearMonth]](jsonBytes)

  @Benchmark
  def playJson(): Array[YearMonth] = Json.parse(jsonBytes).as[Array[YearMonth]]

  @Benchmark
  def playJsonJsoniter(): Array[YearMonth] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[YearMonth]])

  @Benchmark
  def sprayJson(): Array[YearMonth] = JsonParser(jsonBytes).convertTo[Array[YearMonth]]

  @Benchmark
  def uPickle(): Array[YearMonth] = read[Array[YearMonth]](jsonBytes)
}