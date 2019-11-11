package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringInput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core.readFromArray
import io.circe.parser.decode
import org.openjdk.jmh.annotations.Benchmark

class Base64Reading extends Base64Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8))(base64GenCodec)

  @Benchmark
  def borerJson(): Array[Byte] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Byte]].value

  @Benchmark
  def circe(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8))(base64D5r).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonDecode[Array[Byte]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base64Codec)
}