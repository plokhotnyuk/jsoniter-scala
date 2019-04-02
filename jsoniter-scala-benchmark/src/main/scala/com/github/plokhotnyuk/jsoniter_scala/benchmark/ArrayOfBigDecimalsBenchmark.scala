package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._

class ArrayOfBigDecimalsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var sourceObj: Array[BigDecimal] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    sourceObj = (1 to size).map { i =>
      BigDecimal(BigInt(Array.fill((i & 0xF) + 1)((i | 0x1).toByte)), i % 37, MathContext.UNLIMITED)
    }.toArray // up to 128-bit numbers for unscaledVal and up to 37-digit (~127 bits) scale
    jsonString = sourceObj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  private def obj: Array[BigDecimal] = {
    val xs = sourceObj
    val l = xs.length
    val ys = new Array[BigDecimal](l)
    var i = 0
    while (i < l) {
      val x = xs(i) // to avoid internal caching of the string representation
      ys(i) = new BigDecimal(new java.math.BigDecimal(x.bigDecimal.unscaledValue, x.bigDecimal.scale), x.mc)
      i += 1
    }
    ys
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[BigDecimal] =
    JsonStringInput.read[Array[BigDecimal]](new String(jsonBytes, UTF_8), jsonOptions)

  @Benchmark
  def readCirce(): Array[BigDecimal] = decode[Array[BigDecimal]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: dsl-json cannot find decoder for array of BigDecimal
  @Benchmark
  def readDslJsonScala(): Array[BigDecimal] = dslJsonDecode[Array[BigDecimal]](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): Array[BigDecimal] = jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigDecimal] = readFromArray[Array[BigDecimal]](jsonBytes)
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def readPlayJson(): Array[BigDecimal] = Json.parse(jsonBytes).as[Array[BigDecimal]]
*/
  @Benchmark
  def readSprayJson(): Array[BigDecimal] = JsonParser(jsonBytes).convertTo[Array[BigDecimal]]

  @Benchmark
  def readUPickle(): Array[BigDecimal] = read[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME: dsl-json cannot find encoder for array of BigDecimal
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode[Array[BigDecimal]](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}