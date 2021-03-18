package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZonedDateTime
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
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

class ArrayOfZonedDateTimesReading extends ArrayOfZonedDateTimesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[ZonedDateTime] = JsonStringInput.read[Array[ZonedDateTime]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[ZonedDateTime] = io.bullet.borer.Json.decode(jsonBytes).to[Array[ZonedDateTime]].value

  @Benchmark
  def circe(): Array[ZonedDateTime] = decode[Array[ZonedDateTime]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON does not parse preferred timezone
  @Benchmark
  def readDslJsonScala(): Array[ZonedDateTime] = dslJsonDecode[Array[ZonedDateTime]](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Array[ZonedDateTime] = jacksonMapper.readValue[Array[ZonedDateTime]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[ZonedDateTime] = readFromArray[Array[ZonedDateTime]](jsonBytes)

  @Benchmark
  def playJson(): Array[ZonedDateTime] = Json.parse(jsonBytes).as[Array[ZonedDateTime]]

  @Benchmark
  def playJsonJsoniter(): Array[ZonedDateTime] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[ZonedDateTime]])

  @Benchmark
  def sprayJson(): Array[ZonedDateTime] = JsonParser(jsonBytes).convertTo[Array[ZonedDateTime]]

  @Benchmark
  def uPickle(): Array[ZonedDateTime] = read[Array[ZonedDateTime]](jsonBytes)

  @Benchmark
  def weePickle(): Array[ZonedDateTime] = FromJson(jsonBytes).transform(ToScala[Array[ZonedDateTime]])
}