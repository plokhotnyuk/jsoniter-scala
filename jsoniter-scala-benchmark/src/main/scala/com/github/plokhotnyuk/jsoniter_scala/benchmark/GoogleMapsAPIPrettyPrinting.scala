package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.WeePickleFromTos._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class GoogleMapsAPIPrettyPrinting extends GoogleMapsAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, JsonOptions.Pretty).getBytes(UTF_8)

  @Benchmark
  def circe(): Array[Byte] = prettyPrinter.print(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonPrettyMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj, prettyConfig)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length, prettyConfig)

  @Benchmark
  def playJson(): Array[Byte] = prettyPrintBytes(Json.toJson(obj))

  @Benchmark
  def sprayJson(): Array[Byte] = CustomPrettyPrinter(obj.toJson).getBytes(UTF_8)

  @Benchmark
  def uPickle(): Array[Byte] = write(obj, 2).getBytes(UTF_8)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToPrettyJson.bytes)
}