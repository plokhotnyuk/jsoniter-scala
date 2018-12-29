package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
//import play.api.libs.json.Json
//import upickle.default._

class BigDecimalBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var sourceObj: BigDecimal = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    sourceObj = BigDecimal(jsonString)
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  //FIXME: it affects results but required to avoid misleading results due internal caching of the string representation
  private def obj: BigDecimal = BigDecimal(sourceObj.bigDecimal.unscaledValue(), sourceObj.bigDecimal.scale())
/* FIXME: AVSystem GenCodec: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def readAVSystemGenCodec(): BigDecimal = JsonStringInput.read[BigDecimal](new String(jsonBytes, UTF_8))
*/
  @Benchmark
  def readCirce(): BigDecimal = decode[BigDecimal](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/*
  @Benchmark
  def readDslJsonScala(): BigDecimal = decodeDslJson[BigDecimal](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): BigDecimal = jacksonMapper.readValue[BigDecimal](jsonBytes)

  @Benchmark
  def readJsoniterScala(): BigDecimal = readFromArray[BigDecimal](jsonBytes, longNumberConfig)(bigDecimalCodec)
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def readPlayJson(): BigDecimal = Json.parse(jsonBytes).as[BigDecimal]
*/
/* FIXME: uPickle parses BigDecimal from JSON strings only
  @Benchmark
  def readUPickle(): BigDecimal = read[BigDecimal](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/*
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = encodeDslJson[BigDecimal](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(bigDecimalCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(bigDecimalCodec)
/* FIXME: Play-JSON serializes BigInt in a scientific representation (as BigDecimal)
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
/* FIXME: uPickle serializes BigDecimal to JSON strings
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}