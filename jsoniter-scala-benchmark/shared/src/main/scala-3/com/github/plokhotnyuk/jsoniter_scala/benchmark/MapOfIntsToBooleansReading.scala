package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class MapOfIntsToBooleansReading extends MapOfIntsToBooleansBenchmark {
  @Benchmark
  def circe(): Map[Int, Boolean] = {
    import io.circe.jawn._

    decodeByteArray[Map[Int, Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Map[Int, Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Map[Int, Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Map[Int, Boolean]]
  }

  @Benchmark
  def json4sNative(): Map[Int, Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Map[Int, Boolean]]
  }

  @Benchmark
  def jsoniterScala(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Map[Int, Boolean]](jsonBytes)
  }
/* FIXME: uPickle parses maps from JSON arrays only
  @Benchmark
  def uPickle(): Map[Int, Boolean] = {
    import upickle.default._

    read[Map[Int, Boolean]](jsonBytes)
  }
*/
  @Benchmark
  def weePickle(): Map[Int, Boolean] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Map[Int, Boolean]])
  }
}