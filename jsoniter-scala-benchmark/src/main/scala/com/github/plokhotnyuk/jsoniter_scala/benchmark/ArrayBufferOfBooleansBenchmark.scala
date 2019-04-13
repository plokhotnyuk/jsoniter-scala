package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._
import spray.json._

import scala.collection.mutable

class ArrayBufferOfBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: mutable.ArrayBuffer[Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.ArrayBuffer((1 to size).map(i => ((i * 1498724053) & 0x1) == 0):_*)
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): mutable.ArrayBuffer[Boolean] =
    JsonStringInput.read[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readBorerJson(): mutable.ArrayBuffer[Boolean] =
    io.bullet.borer.Json.decode(jsonBytes).to[mutable.ArrayBuffer[Boolean]].value

  @Benchmark
  def readCirce(): mutable.ArrayBuffer[Boolean] =
    decode[mutable.ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): mutable.ArrayBuffer[Boolean] = dslJsonDecode[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def readJacksonScala(): mutable.ArrayBuffer[Boolean] = jacksonMapper.readValue[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): mutable.ArrayBuffer[Boolean] = readFromArray[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.ArrayBuffer[Boolean] = Json.parse(jsonBytes).as[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def readSprayJson(): mutable.ArrayBuffer[Boolean] = JsonParser(jsonBytes).convertTo[mutable.ArrayBuffer[Boolean]]

  @Benchmark
  def readUPickle(): mutable.ArrayBuffer[Boolean] = read[mutable.ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeBorerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)

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