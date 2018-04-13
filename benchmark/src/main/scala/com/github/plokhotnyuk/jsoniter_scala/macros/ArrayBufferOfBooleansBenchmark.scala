package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

import scala.collection.breakOut
import scala.collection.mutable.ArrayBuffer

class ArrayBufferOfBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: ArrayBuffer[Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  setup()

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => ((i * 1498724053) & 1) == 0)(breakOut)
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): ArrayBuffer[Boolean] = decode[ArrayBuffer[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): ArrayBuffer[Boolean] = jacksonMapper.readValue[ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): ArrayBuffer[Boolean] = readFromArray[ArrayBuffer[Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): ArrayBuffer[Boolean] = Json.parse(jsonBytes).as[ArrayBuffer[Boolean]]

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
}