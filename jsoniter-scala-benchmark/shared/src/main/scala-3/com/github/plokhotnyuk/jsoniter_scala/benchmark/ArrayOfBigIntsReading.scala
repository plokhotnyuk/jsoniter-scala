package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfBigIntsReading extends ArrayOfBigIntsBenchmark {
  @Benchmark
  def borer(): Array[BigInt] = {
    import io.bullet.borer.Json
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._

    Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[BigInt]].value
  }

  @Benchmark
  def circe(): Array[BigInt] = {
    import io.circe.jawn._

    decodeByteArray[Array[BigInt]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[BigInt]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[BigInt]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[BigInt] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    bigNumberMapper.readValue[JValue](jsonBytes, jValueType).extract[Array[BigInt]]
  }

  @Benchmark
  def json4sNative(): Array[BigInt] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[BigInt]]
  }

  @Benchmark
  def jsoniterScala(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigInt]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigInt]](jsonBytes)
  }

  @Benchmark
  def uPickle(): Array[BigInt] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[BigInt]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[BigInt] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[BigInt]])
  }
}