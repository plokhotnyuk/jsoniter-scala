package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import java.time.OffsetTime

class ArrayOfOffsetTimesReading extends ArrayOfOffsetTimesBenchmark {
  @Benchmark
  def borer(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[OffsetTime]].value
  }

  @Benchmark
  def jacksonScala(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[OffsetTime]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[OffsetTime] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[OffsetTime]]
  }

  @Benchmark
  def json4sNative(): Array[OffsetTime] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JavaTimeJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[OffsetTime]]
  }

  @Benchmark
  def jsoniterScala(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[OffsetTime]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[OffsetTime] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[OffsetTime]])
  }
}