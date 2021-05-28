package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalTime
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfLocalTimesReading extends ArrayOfLocalTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[LocalTime] = JsonStringInput.read[Array[LocalTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[LocalTime] = io.bullet.borer.Json.decode(jsonBytes).to[Array[LocalTime]].value

  @Benchmark
  def circe(): Array[LocalTime] = decode[Array[LocalTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[LocalTime] = dslJsonDecode[Array[LocalTime]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[LocalTime] = jacksonMapper.readValue[Array[LocalTime]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[LocalTime] = readFromArray[Array[LocalTime]](jsonBytes)

  @Benchmark
  def playJson(): Array[LocalTime] = Json.parse(jsonBytes).as[Array[LocalTime]]

  @Benchmark
  def playJsonJsoniter(): Array[LocalTime] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[LocalTime]])

  @Benchmark
  def sprayJson(): Array[LocalTime] = JsonParser(jsonBytes).convertTo[Array[LocalTime]]

  @Benchmark
  def uPickle(): Array[LocalTime] = read[Array[LocalTime]](jsonBytes)

  @Benchmark
  def weePickle(): Array[LocalTime] = FromJson(jsonBytes).transform(ToScala[Array[LocalTime]])

  @Benchmark
  def zioJson(): Array[LocalTime] = new String(jsonBytes, UTF_8).fromJson[Array[LocalTime]].fold(sys.error, identity)
}