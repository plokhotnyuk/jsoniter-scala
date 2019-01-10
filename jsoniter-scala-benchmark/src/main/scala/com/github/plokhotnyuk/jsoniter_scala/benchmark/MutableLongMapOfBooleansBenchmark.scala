package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable

class MutableLongMapOfBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: mutable.LongMap[Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.LongMap((1 to size).map { i =>
      ((i * 372036854775807L) / Math.pow(10, i % 18).toLong, ((i * 1498724053) & 0x1) == 0)
    }:_*)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): mutable.LongMap[Boolean] = JsonStringInput.read[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8))
/* FIXME: Circe doesn't support mutable.LongMap
  @Benchmark
  def readCirce(): mutable.LongMap[Boolean] = decode[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
*/
/* FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
  @Benchmark
  def readJacksonScala(): mutable.LongMap[Boolean] = jacksonMapper.readValue[mutable.LongMap[Boolean]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): mutable.LongMap[Boolean] = readFromArray[mutable.LongMap[Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.LongMap[Boolean] = Json.parse(jsonBytes).as[mutable.LongMap[Boolean]](mutableLongMapOfBooleansFormat)
/* FIXME: uPickle doesn't support mutable.LongMap
  @Benchmark
  def readUPickle(): mutable.LongMap[Boolean] = read[mutable.LongMap[Boolean]](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Circe doesn't support mutable.LongMap
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
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableLongMapOfBooleansFormat))
/* FIXME: uPickle doesn't support mutable.LongMap
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}