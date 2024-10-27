package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ArrayOfDoublesReading extends ArrayOfDoublesBenchmark {
  @Benchmark
  def borer(): Array[Double] = {
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[Double]].value
  }

  @Benchmark
  def circe(): Array[Double] = {
    import io.circe.jawn._

    decodeByteArray[Array[Double]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.circe.CirceCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[Double]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[Double] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[Double]](jsonBytes)
  }

  @Benchmark
  @annotation.nowarn
  def json4sJackson(): Array[Double] = {
    import org.json4s._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sJacksonMappers._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._

    mapper.readValue[JValue](jsonBytes, jValueType).extract[Array[Double]]
  }

  @Benchmark
  @annotation.nowarn
  def json4sNative(): Array[Double] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CommonJson4sFormats._
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
  def simdjsonJava(): Array[Double] =
    simdJsonParser.get.parse[Array[Double]](jsonBytes, jsonBytes.length, classOf[Array[Double]])

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
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
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