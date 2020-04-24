package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.output.JsoniterJavaSerializer
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
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj, escapingConfig)(stringCodec)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length, escapingConfig)(stringCodec)

  @Benchmark
  def playJson(): Array[Byte] = Json.asciiStringify(Json.toJson(obj)).getBytes

  @Benchmark
  def uPickle(): Array[Byte] = write(obj, escapeUnicode = true).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToEscapedNonAsciiJson.bytes)

  @Benchmark
  def sjson(): Array[Byte] = {
    import sjsonnew.support.scalajson.unsafe._
    import com.github.plokhotnyuk.jsoniter_scala.benchmark.SJsonEncodersDecoders._
    CompactPrinter(Converter.toJsonUnsafe(obj)).getBytes(UTF_8)
  }
}
