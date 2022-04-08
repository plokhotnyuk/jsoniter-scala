package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import io.circe.Decoder
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.collection.mutable

class MutableLongMapOfBooleansReading extends MutableLongMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.LongMap[Boolean] =
    JsonStringInput.read[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): mutable.LongMap[Boolean] =
    decode[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def circeJawn(): mutable.LongMap[Boolean] =
    io.circe.jawn.decodeByteArray[mutable.LongMap[Boolean]](jsonBytes).fold(throw _, identity)

  @Benchmark
  def circeJsoniter(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    Decoder[mutable.LongMap[Boolean]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }
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

  @Benchmark
  def playJsonJsoniter(): mutable.LongMap[Boolean] =
    PlayJsonJsoniter.deserialize(jsonBytes).fold(throw _, _.as[mutable.LongMap[Boolean]])
}