package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark
import scala.collection.immutable.ArraySeq

class ArraySeqOfBooleansReading extends ArraySeqOfBooleansBenchmark {
  @Benchmark
  def borer(): ArraySeq[Boolean] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[ArraySeq[Boolean]].value
  }

  @Benchmark
  def jacksonScala(): ArraySeq[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[ArraySeq[Boolean]](jsonBytes)
  }
/* FIXME json4s.jackson throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sJackson(): ArraySeq[Boolean] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[ArraySeq[Boolean]]
  }
*/
/* FIXME json4s.native throws org.json4s.MappingException: unknown error
  @Benchmark
  def json4sNative(): ArraySeq[Boolean] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[ArraySeq[Boolean]]
  }
*/
  @Benchmark
  def jsoniterScala(): ArraySeq[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ArraySeq[Boolean]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): ArraySeq[Boolean] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[ArraySeq[Boolean]](jsonBytes)
  }

  @Benchmark
  def weePickle(): ArraySeq[Boolean] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[ArraySeq[Boolean]])
  }
}