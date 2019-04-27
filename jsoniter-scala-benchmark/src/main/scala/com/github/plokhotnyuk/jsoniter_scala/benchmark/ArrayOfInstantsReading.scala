package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfInstantsReading extends ArrayOfInstantsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Instant] = JsonStringInput.read[Array[Instant]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Array[Instant] = decode[Array[Instant]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[Instant] = jacksonMapper.readValue[Array[Instant]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Instant] = readFromArray[Array[Instant]](jsonBytes)

  @Benchmark
  def playJson(): Array[Instant] = Json.parse(jsonBytes).as[Array[Instant]]

  @Benchmark
  def sprayJson(): Array[Instant] = JsonParser(jsonBytes).convertTo[Array[Instant]]

  @Benchmark
  def uPickle(): Array[Instant] = read[Array[Instant]](jsonBytes)
}