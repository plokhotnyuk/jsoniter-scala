package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

class ArrayOfBigDecimalsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var sourceObj: Array[BigDecimal] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    sourceObj = (1 to size).map { i =>
      //FIXME: | 1 is used to hide JDK bug of serialization redundant 0 after .
      BigDecimal(BigInt(Array.fill((i & 15) + 1)((i | 1).toByte)), i % 37)
    }.toArray // up to 128-bit numbers for unscaledVal and up to 37-digit (~127 bits) scale
    jsonString = sourceObj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  //FIXME: it affects results but required to avoid misleading results due internal caching of the string representation
  private def obj: Array[BigDecimal] =
    sourceObj.map(x => BigDecimal(x.bigDecimal.unscaledValue(), x.bigDecimal.scale()))

/* FIXME: AVSystem GenCodec parses BigDecimal from JSON string only
  @Benchmark
  def readAVSystemGenCodec(): Array[BigDecimal] = JsonStringInput.read[Array[BigDecimal]](new String(jsonBytes, UTF_8))
*/
  @Benchmark
  def readCirce(): Array[BigDecimal] = decode[Array[BigDecimal]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[BigDecimal] = jacksonMapper.readValue[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[BigDecimal] = readFromArray[Array[BigDecimal]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[BigDecimal] = Json.parse(jsonBytes).as[Array[BigDecimal]]
/* FIXME: uPickle parses BigDecimal from JSON string only
  @Benchmark
  def readUPickle(): Array[BigDecimal] = read[Array[BigDecimal]](jsonBytes)
*/
/* FIXME: AVSystem GenCodec serializes BigDecimal to JSON string
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
/* FIXME: uPickle serializes BigDecimal to JSON string
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}