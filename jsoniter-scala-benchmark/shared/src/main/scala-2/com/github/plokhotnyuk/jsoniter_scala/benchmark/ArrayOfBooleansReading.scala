package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfBooleansReading extends ArrayOfBooleansBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Boolean] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[Boolean]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[Boolean] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Boolean]].value
  }

  @Benchmark
  def circe(): Array[Boolean] = {
    import io.circe.jawn._

    decodeByteArray[Array[Boolean]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Boolean]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[Boolean]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Boolean]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Boolean]]
  }

  @Benchmark
  def json4sNative(): Array[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Boolean]]
  }

  @Benchmark
  def jsoniterScala(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Boolean]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Boolean] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Boolean]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Boolean] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[Boolean]]
  }

  @Benchmark
  def smithy4sJson(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Boolean]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Boolean]]
  }

  @Benchmark
  def uPickle(): Array[Boolean] = {
    import upickle.default._

    read[Array[Boolean]](jsonBytes, trace = false)
  }

  @Benchmark
  def weePickle(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Boolean]])
  }

  @Benchmark
  def zioJson(): Array[Boolean] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Boolean]].fold(sys.error, identity)
  }

  @Benchmark
  def zioSchemaJson(): Array[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioSchemaJsonCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    arrayOfBooleansCodec.decodeJson(new String(jsonBytes, UTF_8)).fold(sys.error, identity)
  }
}