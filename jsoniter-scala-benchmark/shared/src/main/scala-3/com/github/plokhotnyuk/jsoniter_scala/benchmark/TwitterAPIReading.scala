package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import org.openjdk.jmh.annotations.Benchmark

class TwitterAPIReading extends TwitterAPIBenchmark {
  @Benchmark
  def borer(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Seq[Tweet]].value
  }

  @Benchmark
  def jacksonScala(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Seq[Tweet]](jsonBytes)
  }
/* FIXME: json4s.jackson throws org.json4s.MappingException: No usable value for entities
  @Benchmark
  def json4sJackson(): Seq[Tweet] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Seq[Tweet]]
  }
*/
/* FIXME: json4s.native throws org.json4s.MappingException: No usable value for entities
  @Benchmark
  def json4sNative(): Seq[Tweet] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Seq[Tweet]]
  }
*/
  @Benchmark
  def jsoniterScala(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Seq[Tweet]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Seq[Tweet] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Seq[Tweet]])
  }
}