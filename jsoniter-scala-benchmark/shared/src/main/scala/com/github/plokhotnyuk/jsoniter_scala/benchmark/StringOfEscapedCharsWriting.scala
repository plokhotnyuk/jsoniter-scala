package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json._
import com.evolutiongaming.jsonitertool.PlayJsonJsoniter
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs.{escapingConfig, _}
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._

class StringOfEscapedCharsWriting extends StringOfEscapedCharsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, JsonOptions(asciiOutput = true)).getBytes(UTF_8)

  @Benchmark
  def circe(): Array[Byte] = escapingPrinter.print(obj.asJson).getBytes

  @Benchmark
  def circeJsoniter(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceJsoniterCodecs._

    writeToArray(obj.asJson, escapingConfig)
  }

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonEscapeNonAsciiMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj, escapingConfig)(stringCodec)

  @Benchmark
  def jsoniterScalaPrealloc(): Int =
    writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length, escapingConfig)(stringCodec)

  @Benchmark
  def playJson(): Array[Byte] = Json.asciiStringify(Json.toJson(obj)).getBytes

  @Benchmark
  def playJsonJsoniter(): Array[Byte] = writeToArray(Json.toJson(obj), escapingConfig)(PlayJsonJsoniter.jsValueCodec)

  @Benchmark
  def smithy4s(): Array[Byte] = {
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.Smithy4sCodecs._

    writeToArray(obj, escapingConfig)(stringJCodec)
  }

  @Benchmark
  def uPickle(): Array[Byte] = write(obj, escapeUnicode = true).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToEscapedNonAsciiJson.bytes)
}