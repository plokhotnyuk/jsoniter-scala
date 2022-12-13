package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfIntsReading extends ArrayOfIntsBenchmark {
  @Benchmark
  def borer(): Array[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Int]].value
  }

  @Benchmark
  def circe(): Array[Int] = {
    import io.circe.jawn._

    decodeByteArray[Array[Int]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Int]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Int]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[Int] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Int]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[Int] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Int]]
  }

  @Benchmark
  def jsoniterScala(): Array[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Int]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Int] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Int]]
  }

  @Benchmark
  def smithy4sJson(): Array[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Int]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Int]]
  }

  @Benchmark
  def uPickle(): Array[Int] = {
    import upickle.default._

    read[Array[Int]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Int] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Int]])
  }

  @Benchmark
  def zioJson(): Array[Int] = {
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Int]].fold(sys.error, identity)
  }
}