package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import java.time.MonthDay
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json.DecoderOps

class ArrayOfMonthDaysReading extends ArrayOfMonthDaysBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[MonthDay] = JsonStringInput.read[Array[MonthDay]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[MonthDay] = io.bullet.borer.Json.decode(jsonBytes).to[Array[MonthDay]].value

  @Benchmark
  def circe(): Array[MonthDay] = decode[Array[MonthDay]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def jacksonScala(): Array[MonthDay] = jacksonMapper.readValue[Array[MonthDay]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[MonthDay] = readFromArray[Array[MonthDay]](jsonBytes)

  @Benchmark
  def playJson(): Array[MonthDay] = Json.parse(jsonBytes).as[Array[MonthDay]]

  @Benchmark
  def playJsonJsoniter(): Array[MonthDay] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[MonthDay]])

  @Benchmark
  def uPickle(): Array[MonthDay] = read[Array[MonthDay]](jsonBytes)

  @Benchmark
  def sprayJson(): Array[MonthDay] = JsonParser(jsonBytes).convertTo[Array[MonthDay]]

  @Benchmark
  def weePickle(): Array[MonthDay] = FromJson(jsonBytes).transform(ToScala[Array[MonthDay]])

  @Benchmark
  def zioJson(): Array[MonthDay] = new String(jsonBytes, UTF_8).fromJson[Array[MonthDay]].fold(sys.error, identity)
}