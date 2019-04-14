package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._

sealed trait SuitADT extends Product with Serializable

case object Hearts extends SuitADT

case object Spades extends SuitADT

case object Diamonds extends SuitADT

case object Clubs extends SuitADT

class ArrayOfEnumADTsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 100
  var obj: Array[SuitADT] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => (i * 1498724053) & 0x3 match {
      case 0 => Hearts
      case 1 => Spades
      case 2 => Diamonds
      case 3 => Clubs
    }).toArray
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[SuitADT] = JsonStringInput.read[Array[SuitADT]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readBorerJson(): Array[SuitADT] = io.bullet.borer.Json.decode(jsonBytes).to[Array[SuitADT]].value

  @Benchmark
  def readCirce(): Array[SuitADT] = decode[Array[SuitADT]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readJacksonScala(): Array[SuitADT] = jacksonMapper.readValue[Array[SuitADT]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[SuitADT] = readFromArray[Array[SuitADT]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[SuitADT] = Json.parse(jsonBytes).as[Array[SuitADT]]

  @Benchmark
  def readSprayJson(): Array[SuitADT] = JsonParser(jsonBytes).convertTo[Array[SuitADT]]

  @Benchmark
  def readUPickle(): Array[SuitADT] = read[Array[SuitADT]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeBorerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

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