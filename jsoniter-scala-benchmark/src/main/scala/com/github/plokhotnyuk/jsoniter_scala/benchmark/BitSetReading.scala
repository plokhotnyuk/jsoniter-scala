package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.immutable.BitSet

class BitSetReading extends BitSetBenchmark {
  @Benchmark
  def avSystemGenCodec(): BitSet = JsonStringInput.read[BitSet](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): BitSet = decode[BitSet](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws scala.collection.immutable.HashSet$HashTrieSet cannot be cast to scala.collection.immutable.BitSet
  @Benchmark
  def dslJsonScala(): BitSet = dslJsonDecode[BitSet](jsonBytes)
*/
/* FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
  @Benchmark
  def jacksonScala(): BitSet = jacksonMapper.readValue[BitSet](jsonBytes)
*/
  @Benchmark
  def jsoniterScala(): BitSet = readFromArray[BitSet](jsonBytes)

  @Benchmark
  def playJson(): BitSet = Json.parse(jsonBytes).as[BitSet](bitSetFormat)
}