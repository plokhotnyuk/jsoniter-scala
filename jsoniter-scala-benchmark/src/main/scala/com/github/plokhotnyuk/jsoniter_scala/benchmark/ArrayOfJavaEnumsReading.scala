package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfJavaEnumsReading extends ArrayOfJavaEnumsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Suit] = JsonStringInput.read[Array[Suit]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Suit] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Suit]].value

  @Benchmark
  def circe(): Array[Suit] = decode[Array[Suit]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws java.lang.ArrayIndexOutOfBoundsException: 0
  @Benchmark
  def dslJsonScala(): Array[Suit] = dslJsonDecode[Array[Suit]](jsonBytes)
*/
  @Benchmark
  def jacksonScala(): Array[Suit] = jacksonMapper.readValue[Array[Suit]](jsonBytes)

  @Benchmark
  def jsoniterJava(): Array[Suit] = JsoniterJavaParser.parse[Array[Suit]](jsonBytes, classOf[Array[Suit]])

  @Benchmark
  def jsoniterScala(): Array[Suit] = readFromArray[Array[Suit]](jsonBytes)

  @Benchmark
  def playJson(): Array[Suit] = Json.parse(jsonBytes).as[Array[Suit]]

  @Benchmark
  def sprayJson(): Array[Suit] = JsonParser(jsonBytes).convertTo[Array[Suit]]

  @Benchmark
  def uPickle(): Array[Suit] = read[Array[Suit]](jsonBytes)
}