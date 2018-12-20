package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class DoubleBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var obj: Double = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    obj = jsonString.toDouble
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Double = JsonStringInput.read[Double](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Double = decode[Double](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): Double = decodeDslJson[Double](jsonBytes)

  @Benchmark
  def readJacksonScala(): Double = jacksonMapper.readValue[Double](jsonBytes)

  @Benchmark
  def readJsoniterJava(): Double = JsoniterJavaParser.parse(jsonBytes, classOf[Double])

  @Benchmark
  def readJsoniterScala(): Double = readFromArray[Double](jsonBytes, longNumberConfig)(doubleCodec)

  @Benchmark
  def readPlayJson(): Double = Json.parse(jsonBytes).as[Double]

  @Benchmark
  def readUPickle(): Double = read[Double](jsonBytes)
}