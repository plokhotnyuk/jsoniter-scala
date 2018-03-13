package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import play.api.libs.json.Json
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark

class ArrayOfBigIntsBenchmark extends CommonParams {
  val obj: Array[BigInt] = (1 to 128).map(i => BigInt(Array.fill((i & 15) + 1)(i.toByte))).toArray // up to 128-bit numbers
  val jsonString: String = obj.map(x => new java.math.BigDecimal(x.bigInteger).toPlainString).mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[BigInt] = decode[Array[BigInt]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[BigInt] = jacksonMapper.readValue[Array[BigInt]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigInt] = readFromArray[Array[BigInt]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[BigInt] = Json.parse(jsonBytes).as[Array[BigInt]](bigIntArrayFormat)

/* FIXME: Circe uses an engineering decimal notation to serialize BigInt
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)
/* FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(bigIntArrayFormat))
*/
}