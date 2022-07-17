package com.github.plokhotnyuk.jsoniter_scala.benchmark

import com.github.plokhotnyuk.jsoniter_scala.benchmark.SuitEnum.SuitEnum
import org.openjdk.jmh.annotations.Benchmark

class ArrayOfEnumsReading extends ArrayOfEnumsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[SuitEnum] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringInput.read[Array[SuitEnum]](new String(jsonBytes, UTF_8))
  }

  @Benchmark
  def borer(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.decode(jsonBytes).to[Array[SuitEnum]].value
  }

  @Benchmark
  def circe(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.parser._
    import java.nio.charset.StandardCharsets.UTF_8

    decode[Array[SuitEnum]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
  }

  @Benchmark
  def circeJawn(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.jawn._

    decodeByteArray[Array[SuitEnum]](jsonBytes).fold(throw _, identity)
  }

  @Benchmark
  def circeJsoniter(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.Decoder

    Decoder[Array[SuitEnum]].decodeJson(readFromArray(jsonBytes)).fold(throw _, identity)
  }

  @Benchmark
  def jacksonScala(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.readValue[Array[SuitEnum]](jsonBytes)
  }

  @Benchmark
  def jsoniterScala(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray[Array[SuitEnum]](jsonBytes)
  }

  @Benchmark
  def playJson(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.parse(jsonBytes).as[Array[SuitEnum]]
  }

  @Benchmark
  def playJsonJsoniter(): Array[SuitEnum] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    readFromArray(jsonBytes).as[Array[SuitEnum]]
  }

  @Benchmark
  def sprayJson(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    JsonParser(jsonBytes).convertTo[Array[SuitEnum]]
  }

  @Benchmark
  def uPickle(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    read[Array[SuitEnum]](jsonBytes)
  }

  @Benchmark
  def weePickle(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.FromJson
    import com.rallyhealth.weepickle.v1.WeePickle.ToScala

    FromJson(jsonBytes).transform(ToScala[Array[SuitEnum]])
  }

  @Benchmark
  def zioJson(): Array[SuitEnum] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    new String(jsonBytes, UTF_8).fromJson[Array[SuitEnum]].fold(sys.error, identity)
  }
}