package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.jsoniter.input.JsoniterJavaParser
//import io.circe.parser._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import spray.json._

class ArrayOfFloatsReading extends ArrayOfFloatsBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Float] = JsonStringInput.read[Array[Float]](new String(jsonBytes, UTF_8))
/* FIXME: circe parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readCirce(): Array[Float] = decode[Array[Float]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
*/
/* FIXME: DSL-JSON parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readDslJsonScala(): Array[Float] = dslJsonDecode[Array[Float]](jsonBytes)
*/
/* FIXME: Jackson Scala parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readJacksonScala(): Array[Float] = jacksonMapper.readValue[Array[Float]](jsonBytes)
*/
/* FIXME: Jsoniter Java parses 1.199999988079071 as 1.2f instead of 1.1999999f
  @Benchmark
  def readJsoniterJava(): Array[Float] = JsoniterJavaParser.parse[Array[Float]](jsonBytes, classOf[Array[Float]])
*/
  @Benchmark
  def readJsoniterScala(): Array[Float] = readFromArray[Array[Float]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Float] = Json.parse(jsonBytes).as[Array[Float]]

  @Benchmark
  def readSprayJson(): Array[Float] = JsonParser(jsonBytes).convertTo[Array[Float]]

  @Benchmark
  def readUPickle(): Array[Float] = read[Array[Float]](jsonBytes)
}