package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class MutableMapOfIntsToBooleansReading extends MutableMapOfIntsToBooleansBenchmark {
  @Benchmark
  def circe(): mutable.Map[Int, Boolean] = {
    import io.circe.jawn._

    decodeByteArray[mutable.Map[Int, Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[mutable.Map[Int, Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): mutable.Map[Int, Boolean] = {
    import com.fasterxml.jackson.module.scala.JavaTypeable.gen2JavaTypeable
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): mutable.Map[Int, Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[mutable.Map[Int, Boolean]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): mutable.Map[Int, Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[mutable.Map[Int, Boolean]]
  }

  @Benchmark
  def jsoniterScala(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[mutable.Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[mutable.Map[Int, Boolean]]
  }

  @Benchmark
  def weePickle(): mutable.Map[Int, Boolean] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[mutable.Map[Int, Boolean]])
  }
}