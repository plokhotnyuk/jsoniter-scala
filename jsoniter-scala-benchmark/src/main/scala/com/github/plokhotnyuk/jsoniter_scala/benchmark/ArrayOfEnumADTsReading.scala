package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
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

class ArrayOfEnumADTsReading extends ArrayOfEnumADTsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[SuitADT] = JsonStringInput.read[Array[SuitADT]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[SuitADT] = io.bullet.borer.Json.decode(jsonBytes).to[Array[SuitADT]].value

  @Benchmark
  def circe(): Array[SuitADT] = decode[Array[SuitADT]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[SuitADT] = jacksonMapper.readValue[Array[SuitADT]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[SuitADT] = readFromArray[Array[SuitADT]](jsonBytes)

  @Benchmark
  def playJson(): Array[SuitADT] = Json.parse(jsonBytes).as[Array[SuitADT]]

  @Benchmark
  def sprayJson(): Array[SuitADT] = JsonParser(jsonBytes).convertTo[Array[SuitADT]]

  @Benchmark
  def uPickle(): Array[SuitADT] = read[Array[SuitADT]](jsonBytes)
}