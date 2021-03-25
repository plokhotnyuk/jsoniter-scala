package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import zio.json._
import scala.collection.immutable.Seq

class TwitterAPIReading extends TwitterAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): Seq[Tweet] = JsonStringInput.read[Seq[Tweet]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borer(): Seq[Tweet] = io.bullet.borer.Json.decode(jsonBytes).to[Seq[Tweet]].value

  @Benchmark
  def circe(): Seq[Tweet] = decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Seq[Tweet] = dslJsonDecode[Seq[Tweet]](jsonBytes)

  @Benchmark
  def jacksonScala(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)

  @Benchmark
  def playJson(): Seq[Tweet] = Json.parse(jsonBytes).as[Seq[Tweet]]

  @Benchmark
  def playJsonJsoniter(): Seq[Tweet] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Seq[Tweet]])

  @Benchmark
  def sprayJson(): Seq[Tweet] = JsonParser(jsonBytes).convertTo[Seq[Tweet]]

  @Benchmark
  def uPickle(): Seq[Tweet] = read[Seq[Tweet]](jsonBytes)

  @Benchmark
  def weePickle(): Seq[Tweet] = FromJson(jsonBytes).transform(ToScala[Seq[Tweet]])

  @Benchmark
  def zioJson(): Seq[Tweet] = new String(jsonBytes, UTF_8).fromJson[Seq[Tweet]].fold(sys.error, identity)
}