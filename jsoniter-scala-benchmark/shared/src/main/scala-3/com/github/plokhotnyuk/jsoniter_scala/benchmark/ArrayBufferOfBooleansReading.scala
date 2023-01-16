package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class ArrayBufferOfBooleansReading extends ArrayBufferOfBooleansBenchmark {
  @Benchmark
  def borer(): mutable.ArrayBuffer[Boolean] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[mutable.ArrayBuffer[Boolean]].value
  }

  @Benchmark
  def circe(): mutable.ArrayBuffer[Boolean] = {
    import io.circe.jawn._

    decodeByteArray[mutable.ArrayBuffer[Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): mutable.ArrayBuffer[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[mutable.ArrayBuffer[Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): mutable.ArrayBuffer[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.ArrayBuffer[Boolean]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): mutable.ArrayBuffer[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): mutable.ArrayBuffer[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  def jsoniterScala(): mutable.ArrayBuffer[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[mutable.ArrayBuffer[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): mutable.ArrayBuffer[Boolean] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): mutable.ArrayBuffer[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  def sprayJson(): mutable.ArrayBuffer[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[mutable.ArrayBuffer[Boolean]]
  }

  @Benchmark
  def uPickle(): mutable.ArrayBuffer[Boolean] = {
    import upickle.default._

    read[mutable.ArrayBuffer[Boolean]](jsonBytes)
  }

  @Benchmark
  def weePickle(): mutable.ArrayBuffer[Boolean] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[mutable.ArrayBuffer[Boolean]])
  }
}