package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfLocalDateTimesReading extends ArrayOfLocalDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[LocalDateTime] = JsonStringInput.read[Array[LocalDateTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Array[LocalDateTime] = decode[Array[LocalDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[LocalDateTime] = dslJsonDecode[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[LocalDateTime] = jacksonMapper.readValue[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[LocalDateTime] = readFromArray[Array[LocalDateTime]](jsonBytes)

  @Benchmark
  def playJson(): Array[LocalDateTime] = Json.parse(jsonBytes).as[Array[LocalDateTime]]

  @Benchmark
  def sprayJson(): Array[LocalDateTime] = JsonParser(jsonBytes).convertTo[Array[LocalDateTime]]

  @Benchmark
  def uPickle(): Array[LocalDateTime] = read[Array[LocalDateTime]](jsonBytes)
}