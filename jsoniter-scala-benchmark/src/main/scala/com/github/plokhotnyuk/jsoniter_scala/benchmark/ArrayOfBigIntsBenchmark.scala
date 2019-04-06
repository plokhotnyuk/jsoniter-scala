package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import play.api.libs.json.Json
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import spray.json._

class ArrayOfBigIntsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[BigInt] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => BigInt(Array.fill((i & 0xF) + 1)(i.toByte))).toArray // up to 128-bit numbers
    jsonString = obj.map(x => new java.math.BigDecimal(x.bigInteger).toPlainString).mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[BigInt] = JsonStringInput.read[Array[BigInt]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[BigInt] = decode[Array[BigInt]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): Array[BigInt] = dslJsonDecode[Array[BigInt]](jsonBytes)

  @Benchmark
  def readJacksonScala(): Array[BigInt] = jacksonMapper.readValue[Array[BigInt]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigInt] = readFromArray[Array[BigInt]](jsonBytes)
/* FIXME: PlayJson looses significant digits in big values
  @Benchmark
  def readPlayJson(): Array[BigInt] = Json.parse(jsonBytes).as[Array[BigInt]](bigIntArrayFormat)
*/
  @Benchmark
  def readSprayJson(): Array[BigInt] = JsonParser(jsonBytes).convertTo[Array[BigInt]]

  @Benchmark
  def readUPickle(): Array[BigInt] = read[Array[BigInt]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode[Array[BigInt]](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}