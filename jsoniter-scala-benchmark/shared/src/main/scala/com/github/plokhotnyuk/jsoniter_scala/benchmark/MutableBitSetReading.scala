package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.Decoder
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.collection.mutable

class MutableBitSetReading extends MutableBitSetBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.BitSet = JsonStringInput.read[mutable.BitSet](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): mutable.BitSet = decode[mutable.BitSet](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): mutable.BitSet = io.circe.jawn.decodeByteArray[mutable.BitSet](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): mutable.BitSet = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[mutable.BitSet].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
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

  @Benchmark
  def playJsonJsoniter(): mutable.BitSet = PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[mutable.BitSet])
}