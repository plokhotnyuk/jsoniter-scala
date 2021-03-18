package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.immutable.IntMap

class IntMapOfBooleansReading extends IntMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): IntMap[Boolean] = JsonStringInput.read[IntMap[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): IntMap[Boolean] = decode[IntMap[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for scala.collection.immutable.IntMap[Boolean]
  @Benchmark
  def dslJsonScala(): IntMap[Boolean] = dslJsonDecode[IntMap[Boolean]](jsonBytes)
*/
/* FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 2 type parameters for map like types (scala.collection.immutable.IntMap)
  @Benchmark
  def jacksonScala(): IntMap[Boolean] = jacksonMapper.readValue[IntMap[Boolean]](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): IntMap[Boolean] = readFromArray[IntMap[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): IntMap[Boolean] = Json.parse(jsonBytes).as[IntMap[Boolean]]

  @Benchmark
  def playJsonJsoniter(): IntMap[Boolean] = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[IntMap[Boolean]])
}