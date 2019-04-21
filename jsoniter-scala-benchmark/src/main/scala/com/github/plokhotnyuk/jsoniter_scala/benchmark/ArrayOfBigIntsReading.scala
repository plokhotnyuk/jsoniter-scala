package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import play.api.libs.json.Json
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import spray.json._

class ArrayOfBigIntsReading extends ArrayOfBigIntsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[BigInt] = JsonStringInput.read[Array[BigInt]](new String(jsonBytes, UTF_8))

  @Benchmark
  def circe(): Array[BigInt] = decode[Array[BigInt]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[BigInt] = dslJsonDecode[Array[BigInt]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[BigInt] = jacksonMapper.readValue[Array[BigInt]](jsonBytes)

  @Benchmark
  def jsoniterScala(): Array[BigInt] = readFromArray[Array[BigInt]](jsonBytes)
/* FIXME: PlayJson looses significant digits in big values
  @Benchmark
  def playJson(): Array[BigInt] = Json.parse(jsonBytes).as[Array[BigInt]](bigIntArrayFormat)
*/
  @Benchmark
  def sprayJson(): Array[BigInt] = JsonParser(jsonBytes).convertTo[Array[BigInt]]

  @Benchmark
  def uPickle(): Array[BigInt] = read[Array[BigInt]](jsonBytes)
}