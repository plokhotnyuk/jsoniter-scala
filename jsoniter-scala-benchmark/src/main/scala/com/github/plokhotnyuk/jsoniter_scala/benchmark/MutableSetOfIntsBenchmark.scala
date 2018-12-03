package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

import scala.collection.mutable

class MutableSetOfIntsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: mutable.Set[Int] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.Set((1 to size).map(i => ((i * 1498724053) / Math.pow(10, i % 10)).toInt):_*)
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): mutable.Set[Int] = JsonStringInput.read[mutable.Set[Int]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): mutable.Set[Int] = decode[mutable.Set[Int]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): mutable.Set[Int] = jacksonMapper.readValue[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): mutable.Set[Int] = readFromArray[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.Set[Int] = Json.parse(jsonBytes).as[mutable.Set[Int]]

  @Benchmark
  def readUPickle(): mutable.Set[Int] = read[mutable.Set[Int]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

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
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}