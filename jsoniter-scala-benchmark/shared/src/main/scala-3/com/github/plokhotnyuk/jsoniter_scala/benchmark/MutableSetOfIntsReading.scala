package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.mutable

class MutableSetOfIntsReading extends MutableSetOfIntsBenchmark {
  @Benchmark
  def borer(): mutable.Set[Int] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[mutable.Set[Int]].value
  }

  @Benchmark
  def jacksonScala(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): mutable.Set[Int] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[mutable.Set[Int]]
  }

  @Benchmark
  def json4sNative(): mutable.Set[Int] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[mutable.Set[Int]]
  }

  @Benchmark
  def jsoniterScala(): mutable.Set[Int] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[mutable.Set[Int]](jsonBytes)
  }

  @Benchmark
  def weePickle(): mutable.Set[Int] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[mutable.Set[Int]])
  }
}