package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import play.api.libs.json.Json
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
//import upickle.default._

class ArrayOfBigIntsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[BigInt] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => BigInt(Array.fill((i & 15) + 1)(i.toByte))).toArray // up to 128-bit numbers
    jsonString = obj.map(x => new java.math.BigDecimal(x.bigInteger).toPlainString).mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Array[BigInt] = decode[Array[BigInt]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

/* FIXME: dsl-json cannot find decoder for array of BigInt
  @Benchmark
  def readDslJsonJava(): Array[BigInt] = decodeDslJson[Array[BigInt]](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): Array[BigInt] = jacksonMapper.readValue[Array[BigInt]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigInt] = readFromArray[Array[BigInt]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[BigInt] = Json.parse(jsonBytes).as[Array[BigInt]](bigIntArrayFormat)
/* FIXME: uPickle parses BigInt from JSON strings only
  @Benchmark
  def readUPickle(): Array[BigInt] = read[Array[BigInt]](jsonBytes)
*/
/* FIXME: Circe uses an engineering decimal notation to serialize BigInt
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
/* FIXME: dsl-json cannot find encoder for array of BigInt
  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[Array[BigInt]](obj).toByteArray
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)
/* FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(bigIntArrayFormat))
*/
/* FIXME: uPickle serializes BigInt to JSON strings
  @Benchmark
  def writeUPickle(): Array[Byte] = writeToBytes(obj)
*/
}