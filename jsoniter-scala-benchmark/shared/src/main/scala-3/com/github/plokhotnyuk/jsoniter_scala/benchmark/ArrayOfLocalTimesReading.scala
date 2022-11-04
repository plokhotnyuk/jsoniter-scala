package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

import java.time.LocalTime

class ArrayOfLocalTimesReading extends ArrayOfLocalTimesBenchmark {
  @Benchmark
  def borer(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[LocalTime]].value
  }

  @Benchmark
  def jacksonScala(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[LocalTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[LocalTime] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[LocalTime]]
  }

  @Benchmark
  def json4sNative(): Array[LocalTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[LocalTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[LocalTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[LocalTime]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[LocalTime] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[LocalTime]])
  }
}