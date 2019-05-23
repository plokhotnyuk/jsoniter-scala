package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._
import upickle.default._

class ArrayOfBytesReading extends ArrayOfBytesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8))
/* FIXME: Borer throws io.bullet.borer.Borer$Error$InvalidInputData: Expected Bytes but got Start of unbounded Array (input position 0)
  @Benchmark
  def borerJson(): Array[Byte] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Byte]].value
*/
  @Benchmark
  def circe(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/*FIXME:dsl-json expects a base64 string for the byte array
  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonDecode[Array[Byte]](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def jsoniterJava(): Array[Byte] = JsoniterJavaParser.parse[Array[Byte]](jsonBytes, classOf[Array[Byte]])

  @Benchmark
  def jsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes)

  @Benchmark
  def playJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]]

  @Benchmark
  def sprayJson(): Array[Byte] = JsonParser(jsonBytes).convertTo[Array[Byte]]

  @Benchmark
  def uPickle(): Array[Byte] = read[Array[Byte]](jsonBytes)
}