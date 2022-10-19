package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class IntReading extends IntBenchmark {
  @Benchmark
  def avSystemGenCodec(): Int = {
    import java.nio.charset.StandardCharsets.UTF_8
    import com.avsystem.commons.serialization.json._

    JsonStringInput.read[Int](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Int = {
    io.bullet.borer.Json.decode(jsonBytes).to[Int].value
  }

  @Benchmark
  def circe(): Int = {
    io.circe.jawn.decodeByteArray[Int](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Int].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Int](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Int](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Int = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Int]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  def json4sNative(): Int = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Int]
  }
*/
  @Benchmark
  def jsoniterScala(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Int](jsonBytes)(intCodec)
  }

  @Benchmark
  def playJson(): Int = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Int]
  }

  @Benchmark
  def playJsonJsoniter(): Int = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Int]
  }

  @Benchmark
  def smithy4sJson(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Int](jsonBytes)(intJCodec)
  }

  @Benchmark
  def sprayJson(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Int]
  }

  @Benchmark
  def uPickle(): Int = {
    import upickle.default._

    read[Int](jsonBytes)
  }

  @Benchmark
  def weePickle(): Int = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Int])
  }

  @Benchmark
  def zioJson(): Int = {
    import java.nio.charset.StandardCharsets.UTF_8
    import zio.json.DecoderOps

    new String(jsonBytes, UTF_8).fromJson[Int].fold(sys.error, identity)
  }
}