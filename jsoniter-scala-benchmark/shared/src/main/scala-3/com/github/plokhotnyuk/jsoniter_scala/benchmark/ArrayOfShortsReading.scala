package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfShortsReading extends ArrayOfShortsBenchmark {
  @Benchmark
  def borer(): Array[Short] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Short]].value
  }

  @Benchmark
  def jacksonScala(): Array[Short] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Short]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Short] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Short]]
  }

  @Benchmark
  def json4sNative(): Array[Short] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Short]]
  }

  @Benchmark
  def jsoniterScala(): Array[Short] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Short]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Array[Short] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Short]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Short] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Short]])
  }
}