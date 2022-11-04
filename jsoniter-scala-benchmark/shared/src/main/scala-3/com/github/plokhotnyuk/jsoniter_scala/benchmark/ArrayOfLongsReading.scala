package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfLongsReading extends ArrayOfLongsBenchmark {
  @Benchmark
  def borer(): Array[Long] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Long]].value
  }

  @Benchmark
  def jacksonScala(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Long]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Long] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Long]]
  }

  @Benchmark
  def json4sNative(): Array[Long] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Long]]
  }

  @Benchmark
  def jsoniterScala(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Long]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Array[Long] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Long]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Long] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Long]])
  }
}