package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class IntReading extends IntBenchmark {
  @Benchmark
  def borer(): Int = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Int].value
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
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Int]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  def json4sNative(): Int = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
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
  def smithy4sJson(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Int](jsonBytes)(intJCodec)
  }

  @Benchmark
  def weePickle(): Int = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Int])
  }
}