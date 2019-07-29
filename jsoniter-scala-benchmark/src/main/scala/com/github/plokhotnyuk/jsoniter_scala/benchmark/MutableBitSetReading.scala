package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable

class MutableBitSetReading extends MutableBitSetBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.BitSet = JsonStringInput.read[mutable.BitSet](new String(jsonBytes, UTF_8))
/* FIXME: DSL-JSON throws scala.collection.mutable.HashSet cannot be cast to scala.collection.mutable.BitSet
  @Benchmark
  def dslJsonScala(): mutable.BitSet = dslJsonDecode[mutable.BitSet](jsonBytes)
*/
/* FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
  @Benchmark
  def jacksonScala(): mutable.BitSet = jacksonMapper.readValue[mutable.BitSet](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): mutable.BitSet = readFromArray[mutable.BitSet](jsonBytes)

  @Benchmark
  def playJson(): mutable.BitSet = Json.parse(jsonBytes).as[mutable.BitSet]

/* FIXME: uPickle doesn't support mutable bitsets
  @Benchmark
  def uPickle(): mutable.BitSet = read[mutable.BitSet](jsonBytes)
*/
}