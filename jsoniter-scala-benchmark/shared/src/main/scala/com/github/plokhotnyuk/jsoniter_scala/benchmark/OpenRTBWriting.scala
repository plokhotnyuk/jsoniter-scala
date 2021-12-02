package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringOutput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.NinnyFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json
//import spray.json._

class OpenRTBWriting extends OpenRTBBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray
/* FIXME: Circe serializes fields with default values
  @Benchmark
  def circe(): Array[Byte] = printer.print(obj.asJson).getBytes(UTF_8)
*/
/* FIXME: Jackson serializes fields with default values
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play-JSON serializes lists with default values
  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
/* FIXME: Spray-JSON serializes fields with default values
  @Benchmark
  def sprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)
*/
/* FIXME: we don't like the defaults
  @Benchmark
  def ninnyJson(): Array[Byte] = {
    import io.github.kag0.ninny.AnySyntax
    obj.toSomeJson.toString.getBytes(UTF_8)
  }

  @Benchmark
  def ninnyJsonJsoniter(): Array[Byte] = {
    import io.github.kag0.ninny.AnySyntax
    writeToArray(obj.toSomeJson)
  }
*/
  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToJson.bytes)
/* FIXME: Zio-JSON serializes empty collections
  @Benchmark
  def zioJson(): Array[Byte] = {
    import zio.json._

    obj.toJson.getBytes(UTF_8)
  }
*/
}