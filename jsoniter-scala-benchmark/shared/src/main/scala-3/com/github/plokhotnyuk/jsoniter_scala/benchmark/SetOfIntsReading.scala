package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class SetOfIntsReading extends SetOfIntsBenchmark {
  @Benchmark
  def borer(): Set[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Set[Int]].value
  }

  @Benchmark
  def jacksonScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Set[Int]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import org.json4s._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Set[Int]]
  }

  @Benchmark
  def json4sNative(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Set[Int]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Set[Int] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Set[Int]])
  }
}