package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZoneId
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfZoneIdsReading extends ArrayOfZoneIdsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[ZoneId] = JsonStringInput.read[Array[ZoneId]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[ZoneId] = io.bullet.borer.Json.decode(jsonBytes).to[Array[ZoneId]].value

  @Benchmark
  def circe(): Array[ZoneId] = decode[Array[ZoneId]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[ZoneId] = jacksonMapper.readValue[Array[ZoneId]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[ZoneId] = readFromArray[Array[ZoneId]](jsonBytes)

  @Benchmark
  def playJson(): Array[ZoneId] = Json.parse(jsonBytes).as[Array[ZoneId]]

  @Benchmark
  def playJsonJsoniter(): Array[ZoneId] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[ZoneId]])

  @Benchmark
  def sprayJson(): Array[ZoneId] = JsonParser(jsonBytes).convertTo[Array[ZoneId]]

  @Benchmark
  def uPickle(): Array[ZoneId] = read[Array[ZoneId]](jsonBytes)

  @Benchmark
  def zioJson(): Array[ZoneId] = new String(jsonBytes, UTF_8).fromJson[Array[ZoneId]].fold(sys.error, identity)
}