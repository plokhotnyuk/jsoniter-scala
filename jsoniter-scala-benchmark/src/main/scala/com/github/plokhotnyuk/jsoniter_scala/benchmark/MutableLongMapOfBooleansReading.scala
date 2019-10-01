package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
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
//import upickle.default._

import scala.collection.mutable

class MutableLongMapOfBooleansReading extends MutableLongMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.LongMap[Boolean] = JsonStringInput.read[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): mutable.LongMap[Boolean] = decode[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON doesn't support mutable.LongMap
  @Benchmark
  def dslJsonScala(): mutable.LongMap[Boolean] = dslJsonDecode[mutable.LongMap[Boolean]](jsonBytes)
*/
/* FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
  @Benchmark
  def jacksonScala(): mutable.LongMap[Boolean] = jacksonMapper.readValue[mutable.LongMap[Boolean]](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): mutable.LongMap[Boolean] = readFromArray[mutable.LongMap[Boolean]](jsonBytes)

  @Benchmark
  def playJson(): mutable.LongMap[Boolean] = Json.parse(jsonBytes).as[mutable.LongMap[Boolean]]
/* FIXME: uPickle doesn't support mutable.LongMap
  @Benchmark
  def uPickle(): mutable.LongMap[Boolean] = read[mutable.LongMap[Boolean]](jsonBytes)
*/
}