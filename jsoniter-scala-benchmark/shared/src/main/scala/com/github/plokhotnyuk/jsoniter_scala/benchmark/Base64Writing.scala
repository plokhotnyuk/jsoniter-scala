package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringOutput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, writeToSubArray}
import com.rallyhealth.weejson.v1.jackson.ToJson
import com.rallyhealth.weepickle.v1.WeePickle.FromScala
import org.openjdk.jmh.annotations.Benchmark
import io.circe.syntax._

class Base64Writing extends Base64Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, jsonBase64Options).getBytes(UTF_8)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def circe(): Array[Byte] = printer.print(obj.asJson(base64E5r)).getBytes(UTF_8)

  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonEncode(obj)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)(base64Codec)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(base64Codec)

  @Benchmark
  def weePickle(): Array[Byte] = FromScala(obj).transform(ToJson.bytes)
}