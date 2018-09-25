package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class ArrayOfDoublesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[Double] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size)
      .map(i => ((i * 372036854775807L) / Math.pow(10, i % 17)).toLong * Math.pow(10, (i % 38) - 19)).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[Double] = JsonStringInput.read[Array[Double]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[Double] = decode[Array[Double]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): Array[Double] = decodeDslJson[Array[Double]](jsonBytes)

  @Benchmark
  def readJacksonScala(): Array[Double] = jacksonMapper.readValue[Array[Double]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Double] = readFromArray[Array[Double]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Double] = Json.parse(jsonBytes).as[Array[Double]]

  @Benchmark
  def readUPickle(): Array[Double] = read[Array[Double]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/*FIXME: dsl-json serializes doubles in a plain representation
  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[Array[Double]](obj).toByteArray
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play serializes doubles in BigDecimal format: 0.0 as 0, 7.0687002407403325E18 as 7068700240740332500
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}