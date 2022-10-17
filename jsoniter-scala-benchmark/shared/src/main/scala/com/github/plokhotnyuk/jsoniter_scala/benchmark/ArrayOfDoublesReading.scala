package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfDoublesReading extends ArrayOfDoublesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Double] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[Double]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[Double] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Double]].value
  }

  @Benchmark
  def circe(): Array[Double] = {
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[Double] = {
    import io.circe.jawn._

    decodeByteArray[Array[Double]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Double]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def dslJsonScala(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonDecode[Array[Double]](jsonBytes)
  }

  @Benchmark
  def jacksonScala(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Double]](jsonBytes)
  }

  @Benchmark
  def json4sJackson(): Array[Double] = {
    import org.json4s._
    import org.json4s.jackson.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Double]]
  }

  @Benchmark
  def json4sNative(): Array[Double] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import java.nio.charset.StandardCharsets.UTF_8

    parse(new String(jsonBytes, UTF_8)).extract[Array[Double]]
  }

  @Benchmark
  def jsoniterScala(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Double]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[Double] = {
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[Double]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[Double] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[Double]]
  }

  @Benchmark
  def smithy4sJson(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[Double]](jsonBytes)
  }

  @Benchmark
  def sprayJson(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[Double]]
  }

  @Benchmark
  def uPickle(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[Double]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[Double] = {
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[Double]])
  }

  @Benchmark
  def zioJson(): Array[Double] = {
    import zio.json.DecoderOps
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[Double]].fold(sys.error, identity)
  }
}