package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class BigIntReading extends BigIntBenchmark {
  @Benchmark
  def borer(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[BigInt].value
  }

  @Benchmark
  def jacksonScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[BigInt](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): BigInt = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    bigNumberMapper.readValue[JValue](jsonBytes, jValueType).extract[BigInt]
  }
/* FIXME: json4s.native throws org.json4s.ParserUtil$ParseException: expected field or array
  @Benchmark
  def json4sNative(): BigInt = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[BigInt]
  }
*/
  @Benchmark
  def jsoniterScala(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntCodec)
  }

  @Benchmark
  def smithy4sJson(): BigInt = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[BigInt](jsonBytes)(bigIntJCodec)
  }

  @Benchmark
  def weePickle(): BigInt = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[BigInt])
  }
}