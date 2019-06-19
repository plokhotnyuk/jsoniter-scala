package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

import scala.collection.immutable.Seq

class TwitterAPIReading extends TwitterAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): Seq[Tweet] = JsonStringInput.read[Seq[Tweet]](new String(jsonBytes, UTF_8))
/* FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected String or Text Bytes but got Null (input position 994)
  @Benchmark
  def borerJson(): Seq[Tweet] = io.bullet.borer.Json.decode(jsonBytes).to[Seq[Tweet]].value
*/
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
/* FIXME: ScalikeJackson parses to the sequence of maps instead a sequence of Tweet
  @Benchmark
  def scalikeJackson(): Seq[Tweet] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    new String(jsonBytes, UTF_8).read[Seq[Tweet]]
  }
*/
  @Benchmark
  def sprayJson(): Seq[Tweet] = JsonParser(jsonBytes).convertTo[Seq[Tweet]]

  @Benchmark
  def uPickle(): Seq[Tweet] = read[Seq[Tweet]](jsonBytes)
}