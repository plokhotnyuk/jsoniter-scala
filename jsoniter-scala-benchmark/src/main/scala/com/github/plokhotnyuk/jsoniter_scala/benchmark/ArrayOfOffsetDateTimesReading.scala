package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time._

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

class ArrayOfOffsetDateTimesReading extends ArrayOfOffsetDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[OffsetDateTime] = JsonStringInput.read[Array[OffsetDateTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Array[OffsetDateTime] = decode[Array[OffsetDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[OffsetDateTime] = dslJsonDecode[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[OffsetDateTime] = jacksonMapper.readValue[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[OffsetDateTime] = readFromArray[Array[OffsetDateTime]](jsonBytes)

  @Benchmark
  def playJson(): Array[OffsetDateTime] = Json.parse(jsonBytes).as[Array[OffsetDateTime]]

  @Benchmark
  def sprayJson(): Array[OffsetDateTime] = JsonParser(jsonBytes).convertTo[Array[OffsetDateTime]]

  @Benchmark
  def uPickle(): Array[OffsetDateTime] = read[Array[OffsetDateTime]](jsonBytes)
}