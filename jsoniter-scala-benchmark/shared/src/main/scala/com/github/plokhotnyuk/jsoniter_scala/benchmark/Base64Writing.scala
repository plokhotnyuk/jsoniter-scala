package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json.JsonStringOutput
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.ZioJSONScalaJsEncoderDecoders._
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, writeToSubArray}
import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import org.openjdk.jmh.annotations.Benchmark
import io.circe.syntax._
import play.api.libs.json.Json
import smithy4s.ByteArray

class Base64Writing extends Base64Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, jsonBase64Options).getBytes(UTF_8)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def circe(): Array[Byte] = printer.print(obj.asJson(base64E5r)).getBytes(UTF_8)

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    writeToArray(obj.asJson(base64E5r))
  }

  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonEncode(obj)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)(base64Codec)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(base64Codec)

  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(base64Format))

  @Benchmark
  def playJsonJsoniter(): Array[Byte] = PlayJsonJsoniter.serialize(Json.toJson(obj)(base64Format))
/* FIXME: smithy4s doesn't pad during serialization to Base64 encoded string
  @Benchmark
  def smithy4s(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    writeToArray(obj)(base64JCodec)
  }
*/
  @Benchmark
  def sprayJson(): Array[Byte] = {
    import spray.json._

    obj.toJson(base64JsonFormat).compactPrint.getBytes(UTF_8)
  }

  @Benchmark
  def uPickle(): Array[Byte] = write(obj)(base64ReadWriter).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToJson.bytes)

  @Benchmark
  def zioJson(): Array[Byte] = {
    import zio.json._

    obj.toJson(base64C3c.encoder).getBytes(UTF_8)
  }
}