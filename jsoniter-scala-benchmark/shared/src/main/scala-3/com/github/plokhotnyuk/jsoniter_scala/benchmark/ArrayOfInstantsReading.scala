package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.Instant

class ArrayOfInstantsReading extends ArrayOfInstantsBenchmark {
  @Benchmark
  def borer(): Array[Instant] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Instant]].value
  }

  @Benchmark
  def jacksonScala(): Array[Instant] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Instant]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Instant] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Instant]]
  }

  @Benchmark
  def json4sNative(): Array[Instant] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Instant]]
  }

  @Benchmark
  def jsoniterScala(): Array[Instant] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Instant]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Array[Instant] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Instant]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Instant] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Instant]])
  }
}