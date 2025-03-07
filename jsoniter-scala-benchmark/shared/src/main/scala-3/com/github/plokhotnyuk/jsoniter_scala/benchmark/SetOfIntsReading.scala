package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class SetOfIntsReading extends SetOfIntsBenchmark {
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
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Set[Int]].decodeJson(readFromArray[io.circe.Json](jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Set[Int]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import org.json4s._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Set[Int]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import org.json4s._
    import org.json4s.native.JsonMethods._
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
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Set[Int]])
  }

  @Benchmark
  def zioJson(): Set[Int] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Set[Int]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    setOfIntsCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}