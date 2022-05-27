package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class NestedStructsWriting extends NestedStructsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Borer throws io.bullet.borer.Borer$Error$Unsupported: The JSON renderer doesn't support more than 64 JSON Array/Object nesting levels
  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray
*/
  @Benchmark
  def circe(): Array[Byte] = printer.print(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    writeToArray(obj.asJson)
  }
/* FIXME: DSL-JSON serializes null value for Option.None
  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonEncode(obj)
*/
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def playJsonJsoniter(): Array[Byte] = PlayJsonJsoniter.serialize(Json.toJson(obj))

  @Benchmark
  def smithy4s(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    writeToArray(obj)
  }

  @Benchmark
  def sprayJson(): Array[Byte] = {
    import spray.json._

    obj.toJson(nestedStructsJsonFormat).compactPrint.getBytes(UTF_8)
  }

  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToJson.bytes)
/* FIXME: zio-json throws java.lang.NullPointerException: Cannot invoke "zio.json.JsonEncoder.isNothing(Object)" because "this.A$1" is null
  @Benchmark
  def zioJson(): Array[Byte] = {
    import zio.json._

    obj.toJson.getBytes(UTF_8)
  }
*/
}