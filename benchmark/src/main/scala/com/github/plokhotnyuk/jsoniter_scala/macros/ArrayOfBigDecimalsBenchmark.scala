package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfBigDecimalsBenchmark extends CommonParams {
  val sourceObj: Array[BigDecimal] = (1 to 128).map { i =>
    //FIXME | 1 is used to hide JDK bug of serialization redundant 0 after .
    BigDecimal(BigInt(Array.fill((i & 15) + 1)((i | 1).toByte)), i % 37)
  }.toArray // up to 128-bit numbers for unscaledVal and up to 37-digit (~127 bits) scale
  val jsonString: String = sourceObj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  //FIXME it affects results but required to avoid misleading results due internal caching of the string representation
  private def obj: Array[BigDecimal] =
    sourceObj.map(x => BigDecimal(x.bigDecimal.unscaledValue(), x.bigDecimal.scale()))

  @Benchmark
  def readCirce(): Array[BigDecimal] = decode[Array[BigDecimal]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[BigDecimal] = jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigDecimal] = readFromArray[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[BigDecimal] = Json.parse(jsonBytes).as[Array[BigDecimal]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}