package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class AnyValsReading extends AnyValsBenchmark {
  @Benchmark
  def borer(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[AnyVals].value
  }

  @Benchmark
  def circe(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[AnyVals](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[AnyVals].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[AnyVals](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): AnyVals = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AnyValsJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[AnyVals]
  }

  @Benchmark
  def json4sNative(): AnyVals = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AnyValsJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[AnyVals]
  }

  @Benchmark
  def jsoniterScala(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[AnyVals](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[AnyVals](jsonBytes)
  }

  @Benchmark
  def uPickle(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[AnyVals](jsonBytes)
  }

  @Benchmark
  def weePickle(): AnyVals = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[AnyVals])
  }
}