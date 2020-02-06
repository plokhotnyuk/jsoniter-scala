package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.output.JsoniterJavaSerializer
import com.rallyhealth.weejson.v1.jackson.FromJson
import com.rallyhealth.weepickle.v1.WeePickle.ToScala
import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfDoublesReading extends ArrayOfDoublesBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Double] = JsonStringInput.read[Array[Double]](new String(jsonBytes, UTF_8))

  @Benchmark
  def borerJson(): Array[Double] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Double]].value

  @Benchmark
  def circe(): Array[Double] = decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def dslJsonScala(): Array[Double] = dslJsonDecode[Array[Double]](jsonBytes)

  @Benchmark
  def jacksonScala(): Array[Double] = jacksonMapper.readValue[Array[Double]](jsonBytes)
/* FIXME: Jsoniter Java cannot parse some numbers like 5.9823526 precisely
  @Benchmark
  def jsoniterJava(): Array[Double] = JsoniterJavaParser.parse[Array[Double]](jsonBytes, classOf[Array[Double]])
*/
  @Benchmark
  def jsoniterScala(): Array[Double] = readFromArray[Array[Double]](jsonBytes)

  @Benchmark
  def playJson(): Array[Double] = Json.parse(jsonBytes).as[Array[Double]]

  @Benchmark
  def sprayJson(): Array[Double] = JsonParser(jsonBytes).convertTo[Array[Double]]

  @Benchmark
  def uPickle(): Array[Double] = read[Array[Double]](jsonBytes)

  @Benchmark
  def weePickle(): Array[Double] = FromJson(jsonBytes).transform(ToScala[Array[Double]])
}