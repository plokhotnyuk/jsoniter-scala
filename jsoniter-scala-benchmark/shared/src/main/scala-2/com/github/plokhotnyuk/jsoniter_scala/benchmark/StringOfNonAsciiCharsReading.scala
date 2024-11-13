package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class StringOfNonAsciiCharsReading extends StringOfNonAsciiCharsBenchmark {
  @Benchmark
  def avSystemGenCodec(): String = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[String](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): String = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[String].value
  }

  @Benchmark
  def circe(): String = {
    import io.circe.jawn._

    decodeByteArray[String](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[String].decodeJson(readFromArray(jsonBytes, tooLongStringConfig)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[String](jsonBytes)(stringDecoder)
  }

  @Benchmark
  def jacksonScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[String](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): String = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[String]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  def json4sNative(): String = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[String]
  }
*/
  @Benchmark
  def jsoniterScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringCodec)
  }

  @Benchmark
  def playJson(): String = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[String]
  }

  @Benchmark
  def playJsonJsoniter(): String = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonJsoniterFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[play.api.libs.json.JsValue](jsonBytes, tooLongStringConfig).as[String]
  }

  @Benchmark
  def smithy4sJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringJCodec)
  }

  @Benchmark
  def sprayJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[String]
  }

  @Benchmark
  def uPickle(): String = {
    import upickle.default._

    read[String](jsonBytes)
  }

  @Benchmark
  def weePickle(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[String])
  }

  @Benchmark
  def zioJson(): String = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[String].fold(sys.error, identity)
  }
}