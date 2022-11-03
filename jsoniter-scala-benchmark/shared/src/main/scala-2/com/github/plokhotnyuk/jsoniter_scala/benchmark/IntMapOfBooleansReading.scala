package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.immutable.IntMap

class IntMapOfBooleansReading extends IntMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): IntMap[Boolean] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[IntMap[Boolean]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def circe(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[IntMap[Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[IntMap[Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON throws java.lang.IllegalArgumentException: requirement failed: Unable to create decoder for scala.collection.immutable.IntMap[Boolean]
  @Benchmark
  def dslJsonScala(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[IntMap[Boolean]](jsonBytes)
  }
*/
  @Benchmark
  def jacksonScala(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[IntMap[Boolean]](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sJackson(): IntMap[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[IntMap[Boolean]]
  }
*/
/* FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sNative(): IntMap[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[IntMap[Boolean]]
  }
*/
  @Benchmark
  def jsoniterScala(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[IntMap[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): IntMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[IntMap[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): IntMap[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[IntMap[Boolean]]
  }
}