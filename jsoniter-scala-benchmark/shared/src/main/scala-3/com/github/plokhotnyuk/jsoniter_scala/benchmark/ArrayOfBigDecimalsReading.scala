package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfBigDecimalsReading extends ArrayOfBigDecimalsBenchmark {
  @Benchmark
  def borer(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders.decodingConfig
    import io.bullet.borer.Json

    Json.decode(jsonBytes).withConfig(decodingConfig).to[Array[BigDecimal]].value
  }

  @Benchmark
  def circe(): Array[BigDecimal] = {
    import io.circe.jawn._

    decodeByteArray[Array[BigDecimal]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[BigDecimal]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[BigDecimal] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    bigNumberMapper.readValue[JValue](jsonBytes, jValueType).extract[Array[BigDecimal]]
  }

  @Benchmark
  def json4sNative(): Array[BigDecimal] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8), useBigDecimalForDouble = true).extract[Array[BigDecimal]]
  }

  @Benchmark
  def jsoniterScala(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def smithy4sJson(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[BigDecimal]]
  }

  @Benchmark
  def uPickle(): Array[BigDecimal] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[BigDecimal]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[BigDecimal] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[BigDecimal]])
  }

  @Benchmark
  def zioJson(): Array[BigDecimal] = {
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[BigDecimal]].fold(sys.error, identity)
  }
}