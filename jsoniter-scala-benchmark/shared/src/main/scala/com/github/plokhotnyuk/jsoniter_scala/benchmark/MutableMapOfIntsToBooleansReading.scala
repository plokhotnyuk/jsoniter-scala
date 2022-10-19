package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class MutableMapOfIntsToBooleansReading extends MutableMapOfIntsToBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.Map[Int, Boolean] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def circe(): mutable.Map[Int, Boolean] = {
    io.circe.jawn.decodeByteArray[mutable.Map[Int, Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[mutable.Map[Int, Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): mutable.Map[Int, Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[mutable.Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): mutable.Map[Int, Boolean] = {
    import com.fasterxml.jackson.module.scala.JavaTypeable.gen2JavaTypeable
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.Map[Int, Boolean]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): mutable.Map[Int, Boolean] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue(jsonBytes, classOf[JValue]).extract[mutable.Map[Int, Boolean]]
  }

  @Benchmark
  def json4sNative(): mutable.Map[Int, Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
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
  def playJsonJsoniter(): mutable.Map[Int, Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[mutable.Map[Int, Boolean]]
  }

  @Benchmark
  def weePickle(): mutable.Map[Int, Boolean] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[mutable.Map[Int, Boolean]])
  }
}