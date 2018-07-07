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
import upickle.default._

import scala.collection.immutable.Set

class SetOfIntsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Set[Int] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => ((i * 1498724053) / Math.pow(10, i % 10)).toInt).toSet
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Set[Int] = decode[Set[Int]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Set[Int] = jacksonMapper.readValue[Set[Int]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Set[Int] = readFromArray[Set[Int]](jsonBytes)

  @Benchmark
  def readPlayJson(): Set[Int] = Json.parse(jsonBytes).as[Set[Int]]

  @Benchmark
  def readUPickle(): Set[Int] = read[Set[Int]](jsonBytes)

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

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}