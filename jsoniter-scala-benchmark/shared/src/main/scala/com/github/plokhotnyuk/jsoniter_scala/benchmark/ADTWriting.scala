package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class ADTWriting extends ADTBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = {
    import com.avsystem.commons.serialization.json._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringOutput.write(obj).getBytes(UTF_8)
  }

  @Benchmark
  def borer(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
    import io.bullet.borer.Json

    Json.encode(obj).toByteArray
  }

  @Benchmark
  def circe(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.syntax._
    import java.nio.charset.StandardCharsets.UTF_8

    printer.print(obj.asJson).getBytes(UTF_8)
  }

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.syntax._

    writeToArray(obj.asJson)
  }

  @Benchmark
  def ninnyJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.NinnyFormats._
    import nrktkt.ninny.{AnySyntax, Json}
    import java.nio.charset.StandardCharsets.UTF_8

    Json.render(obj.toSomeJson).getBytes(UTF_8)
  }

  @Benchmark
  def jacksonScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.writeValueAsBytes(obj)
  }

  @Benchmark
  def jsoniterScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToArray(obj)
  }

  @Benchmark
  def jsoniterScalaPrealloc(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
  }

  @Benchmark
  def playJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.toBytes(Json.toJson(obj)(adtFormat))
  }

  @Benchmark
  def playJsonJsoniter(): Array[Byte] = {
    import com.evolutiongaming.jsonitertool.PlayJsonJsoniter._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import play.api.libs.json.Json

    writeToArray(Json.toJson(obj)(adtFormat))
  }

  @Benchmark
  def smithy4sJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToArray(obj)
  }

  @Benchmark
  def sprayJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._
    import java.nio.charset.StandardCharsets.UTF_8

    obj.toJson(adtBaseJsonFormat).compactPrint.getBytes(UTF_8)
  }

  @Benchmark
  def uPickle(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
    import java.nio.charset.StandardCharsets.UTF_8

    write(obj).getBytes(UTF_8)
  }

  @Benchmark
  def weePickle(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weejson.v1.jackson.ToJson
    import com.rallyhealth.weepickle.v1.WeePickle.FromScala

    FromScala(obj).transform(ToJson.bytes)
  }

  @Benchmark
  def zioJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    obj.toJson.getBytes(UTF_8)
  }
}