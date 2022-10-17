package com.github.plokhotnyuk.jsoniter_scala.benchmark

import org.openjdk.jmh.annotations.Benchmark

class OpenRTBWriting extends OpenRTBBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = {
    import com.avsystem.commons.serialization.json.JsonStringOutput
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
/* FIXME: Circe serializes fields with default values
  @Benchmark
  def circe(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
    import io.circe.syntax._

    printer.print(obj.asJson).getBytes(UTF_8)
  }
*/
/* FIXME: Jackson serializes fields with default values
  @Benchmark
  def jacksonScala(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._

    jacksonMapper.writeValueAsBytes(obj)
  }
*/
/* FIXME: json4s.jackson serializes fields with default values
  @Benchmark
  def json4sJackson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import org.json4s.jackson.Serialization._
    import java.nio.charset.StandardCharsets.UTF_8

    write(obj).getBytes(UTF_8)
  }
*/
/* FIXME: json4s.native serializes fields with default values
  @Benchmark
  def json4sNative(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Json4sFormats._
    import org.json4s.native.Serialization._
    import java.nio.charset.StandardCharsets.UTF_8

    write(obj).getBytes(UTF_8)
  }
*/
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

    writeToSubArray(obj, preallocatedBuf, 64, preallocatedBuf.length)
  }
/* FIXME: Play-JSON serializes lists with default values
  @Benchmark
  def playJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
    import play.api.libs.json.Json

    Json.toBytes(Json.toJson(obj))
  }
*/
  @Benchmark
  def smithy4sJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sJCodecs._
    import com.github.plokhotnyuk.jsoniter_scala.core._

    writeToArray(obj)
  }
/* FIXME: Spray-JSON serializes fields with default values
  @Benchmark
  def sprayJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
    import spray.json._

    obj.toJson.compactPrint.getBytes(UTF_8)
  }
*/
  @Benchmark
  def uPickle(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._

    writeToByteArray(obj)
  }

  @Benchmark
  def weePickle(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
    import com.rallyhealth.weepickle.v1.WeePickle.FromScala

    FromScala(obj).transform(ToJson.bytes)
  }
/* FIXME: Zio-JSON serializes empty collections
  @Benchmark
  def zioJson(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
    import zio.json._
    import zio.json.JsonEncoder._
    import java.nio.charset.StandardCharsets.UTF_8

    obj.toJson.getBytes(UTF_8)
  }
*/
}