package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
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
import zio.json.DecoderOps

class ArrayOfDurationsReading extends ArrayOfDurationsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Duration] = JsonStringInput.read[Array[Duration]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Duration] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Duration]].value

  @Benchmark
  def circe(): Array[Duration] = decode[Array[Duration]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[Duration] = jacksonMapper.readValue[Array[Duration]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Duration] = readFromArray[Array[Duration]](jsonBytes)

  @Benchmark
  def playJson(): Array[Duration] = Json.parse(jsonBytes).as[Array[Duration]]

  @Benchmark
  def playJsonJsoniter(): Array[Duration] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Duration]])

  @Benchmark
  def sprayJson(): Array[Duration] = JsonParser(jsonBytes).convertTo[Array[Duration]]

  @Benchmark
  def uPickle(): Array[Duration] = read[Array[Duration]](jsonBytes)

  @Benchmark
  def zioJson(): Array[Duration] = new String(jsonBytes, UTF_8).fromJson[Array[Duration]].fold(sys.error, identity)
}