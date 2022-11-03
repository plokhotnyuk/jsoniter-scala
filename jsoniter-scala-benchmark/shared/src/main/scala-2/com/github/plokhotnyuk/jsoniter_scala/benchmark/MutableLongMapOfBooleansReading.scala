package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class MutableLongMapOfBooleansReading extends MutableLongMapOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.LongMap[Boolean] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def circe(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[mutable.LongMap[Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[mutable.LongMap[Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }
/* FIXME: DSL-JSON doesn't support mutable.LongMap
  @Benchmark
  def dslJsonScala(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[mutable.LongMap[Boolean]](jsonBytes)
  }
*/
  @Benchmark
  def jacksonScala(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.LongMap[Boolean]](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sJackson(): mutable.LongMap[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[mutable.LongMap[Boolean]]
  }
*/
/* FIXME: json4s.native throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sNative(): mutable.LongMap[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[mutable.LongMap[Boolean]]
  }
*/
  @Benchmark
  def jsoniterScala(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[mutable.LongMap[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): mutable.LongMap[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[mutable.LongMap[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): mutable.LongMap[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[mutable.LongMap[Boolean]]
  }
}