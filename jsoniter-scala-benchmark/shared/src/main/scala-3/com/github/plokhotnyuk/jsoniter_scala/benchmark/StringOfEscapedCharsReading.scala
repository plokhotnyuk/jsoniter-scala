package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class StringOfEscapedCharsReading extends StringOfEscapedCharsBenchmark {
  @Benchmark
  def borer(): String = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[String].value
  }

  @Benchmark
  def jacksonScala(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonEscapeNonAsciiMapper.readValue[String](jsonBytes)
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
  def smithy4sJson(): String = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[String](jsonBytes, tooLongStringConfig)(stringJCodec)
  }

  @Benchmark
  def weePickle(): String = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[String])
  }
}