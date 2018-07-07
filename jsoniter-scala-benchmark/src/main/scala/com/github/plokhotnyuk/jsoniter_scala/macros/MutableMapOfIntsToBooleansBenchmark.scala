package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable

class MutableMapOfIntsToBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: mutable.Map[Int, Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.Map((1 to size).map { i =>
      (((i * 1498724053) / Math.pow(10, i % 10)).toInt, ((i * 1498724053) & 1) == 0)
    }:_*)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): mutable.Map[Int, Boolean] = decode[mutable.Map[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

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
/* FIXME: Circe changes order of entries
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableMapOfIntsToBooleansFormat))
/* FIXME: uPickle doesn't support mutable maps
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}