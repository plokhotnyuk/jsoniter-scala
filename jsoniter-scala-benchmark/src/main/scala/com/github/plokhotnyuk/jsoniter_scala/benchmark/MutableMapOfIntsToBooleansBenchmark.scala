package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable

class MutableMapOfIntsToBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: mutable.Map[Int, Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.Map((1 to size).map { i =>
      (((i * 1498724053) / Math.pow(10, i % 10)).toInt, ((i * 1498724053) & 0x1) == 0)
    }:_*)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): mutable.Map[Int, Boolean] = JsonStringInput.read[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): mutable.Map[Int, Boolean] = decode[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readJacksonScala(): mutable.Map[Int, Boolean] = jacksonMapper.readValue[mutable.Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): mutable.Map[Int, Boolean] = readFromArray[mutable.Map[Int, Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.Map[Int, Boolean] =
    Json.parse(jsonBytes).as[mutable.Map[Int, Boolean]](mutableMapOfIntsToBooleansFormat)
/* FIXME: uPickle doesn't support mutable maps
  @Benchmark
  def readUPickle(): mutable.Map[Int, Boolean] = read[mutable.Map[Int, Boolean]](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Circe changes order of entries
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableMapOfIntsToBooleansFormat))
/* FIXME: uPickle doesn't support mutable maps
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}