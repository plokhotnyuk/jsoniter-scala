package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class BigIntWriting extends BigIntBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = {
    import com.avsystem.commons.serialization.json._
    import java.nio.charset.StandardCharsets.UTF_8

    JsonStringOutput.write(obj).getBytes(UTF_8)
  }

  @Benchmark
  def borer(): Array[Byte] = {
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
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._
    import io.circe.syntax._

    writeToArray(obj.asJson)
  }

  @Benchmark
  def dslJsonScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._

    dslJsonEncode(obj)(bigIntgEncoder)
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

    writeToArray(obj)(bigIntCodec)
  }

  @Benchmark
  def jsoniterScalaPrealloc(): Int = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(bigIntCodec)
  }
/* FIXME: Play-JSON serializes BigInt values as floating point numbers with a scientific representation
  @Benchmark
  def playJson(): Array[Byte] = {
    import play.api.libs.json.Json

    Json.toBytes(Json.toJson(obj))
  }
*/
  @Benchmark
  def smithy4sJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToArray(obj)(bigIntJCodec)
  }

  @Benchmark
  def sprayJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._
    import java.nio.charset.StandardCharsets.UTF_8

    obj.toJson.compactPrint.getBytes(UTF_8)
  }

  @Benchmark
  def uPickle(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
    import java.nio.charset.StandardCharsets.UTF_8

    write(obj).getBytes(UTF_8)
  }
/* FIXME: weePickle serializes BigInt values as JSON strings
  @Benchmark
  def weePickle(): Array[Byte] = {
    import com.rallyhealth.weejson.v1.jackson.ToJson
    import com.rallyhealth.weepickle.v1.WeePickle.FromScala

    FromScala(obj).transform(ToJson.bytes)
  }
*/
  @Benchmark
  def zioJson(): Array[Byte] = {
    import zio.json._
    import java.nio.charset.StandardCharsets.UTF_8

    obj.toJson.getBytes(UTF_8)
  }
}