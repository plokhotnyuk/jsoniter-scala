package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class SetOfIntsReading extends SetOfIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Set[Int] = {
    import java.nio.charset.StandardCharsets.UTF_8
    import com.avsystem.commons.serialization.json._

    JsonStringInput.read[Set[Int]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Set[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Set[Int]].value
  }

  @Benchmark
  def circe(): Set[Int] = {
    import io.circe.jawn._

    decodeByteArray[Set[Int]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Set[Int]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Set[Int]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Set[Int]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Set[Int] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Set[Int]]
  }

  @Benchmark
  def json4sNative(): Set[Int] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def playJson(): Set[Int] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Set[Int]]
  }

  @Benchmark
  def playJsonJsoniter(): Set[Int] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Set[Int]]
  }

  @Benchmark
  def smithy4sJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Set[Int]]
  }

  @Benchmark
  def uPickle(): Set[Int] = {
    import upickle.default._

    read[Set[Int]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Set[Int] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Set[Int]])
  }

  @Benchmark
  def zioJson(): Set[Int] = {
    import java.nio.charset.StandardCharsets.UTF_8
    import zio.json.DecoderOps

    new String(jsonBytes, UTF_8).fromJson[Set[Int]].fold(sys.error, identity)
  }
}