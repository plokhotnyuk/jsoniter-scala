package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONNonGenEncoderDecoders._
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._
//import spray.json._
import zio.json._

import scala.collection.immutable.Map

class MapOfIntsToBooleansReading extends MapOfIntsToBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): Map[Int, Boolean] = JsonStringInput.read[Map[Int, Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Map[Int, Boolean] = decode[Map[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Map[Int, Boolean] = dslJsonDecode[Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def jacksonScala(): Map[Int, Boolean] = jacksonMapper.readValue[Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Map[Int, Boolean] = readFromArray[Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def playJson(): Map[Int, Boolean] = Json.parse(jsonBytes).as[Map[Int, Boolean]]

  @Benchmark
  def playJsonJsoniter(): Map[Int, Boolean] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[Map[Int, Boolean]])
/* FIXME: Spray-JSON throws spray.json.DeserializationException: Expected Int as JsNumber, but got "-1"
  @Benchmark
  def sprayJson(): Map[Int, Boolean] = JsonParser(jsonBytes).convertTo[Map[Int, Boolean]]
*/
/* FIXME: uPickle parses maps from JSON arrays only
  @Benchmark
  def uPickle(): Map[Int, Boolean] = read[Map[Int, Boolean]](jsonBytes)
*/
  @Benchmark
  def weePickle(): Map[Int, Boolean] = FromJson(jsonBytes).transform(ToScala[Map[Int, Boolean]])

  @Benchmark
  def zioJson(): Map[Int, Boolean] = new String(jsonBytes, UTF_8).fromJson[Map[Int, Boolean]].fold(sys.error, identity)
}