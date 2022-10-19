package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class MutableSetOfIntsReading extends MutableSetOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): mutable.Set[Int] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[mutable.Set[Int]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): mutable.Set[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[mutable.Set[Int]].value
  }

  @Benchmark
  def circe(): mutable.Set[Int] = {
    import io.circe.jawn._

    decodeByteArray[mutable.Set[Int]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[mutable.Set[Int]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): mutable.Set[Int] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue(jsonBytes, classOf[JValue]).extract[mutable.Set[Int]]
  }

  @Benchmark
  def json4sNative(): mutable.Set[Int] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[mutable.Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def playJson(): mutable.Set[Int] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[mutable.Set[Int]]
  }

  @Benchmark
  def playJsonJsoniter(): mutable.Set[Int] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[mutable.Set[Int]]
  }

  @Benchmark
  def uPickle(): mutable.Set[Int] = {
    import upickle.default._

    read[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def weePickle(): mutable.Set[Int] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[mutable.Set[Int]])
  }
}