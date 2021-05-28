package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
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

class ArrayOfLongsReading extends ArrayOfLongsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Long] = JsonStringInput.read[Array[Long]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Array[Long] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Long]].value

  @Benchmark
  def circe(): Array[Long] = decode[Array[Long]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Long] = dslJsonDecode[Array[Long]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Long] = jacksonMapper.readValue[Array[Long]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Long] = readFromArray[Array[Long]](jsonBytes)

  @Benchmark
  def playJson(): Array[Long] = Json.parse(jsonBytes).as[Array[Long]]

  @Benchmark
  def playJsonJsoniter(): Array[Long] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Array[Long]])

  @Benchmark
  def sprayJson(): Array[Long] = JsonParser(jsonBytes).convertTo[Array[Long]]

  @Benchmark
  def uPickle(): Array[Long] = read[Array[Long]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Long] = FromJson(jsonBytes).transform(ToScala[Array[Long]])

  @Benchmark
  def zioJson(): Array[Long] = new String(jsonBytes, UTF_8).fromJson[Array[Long]].fold(sys.error, identity)
}