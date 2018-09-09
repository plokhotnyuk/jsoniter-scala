package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable.OpenHashMap

class OpenHashMapOfIntsToBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: OpenHashMap[Int, Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = OpenHashMap((1 to size).map { i =>
      (((i * 1498724053) / Math.pow(10, i % 10)).toInt, ((i * 1498724053) & 1) == 0)
    }:_*)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }
/* FIXME: AVSystem GenCodec doesn't support parsing of OpenHashMap
  @Benchmark
  def readAVSystemGenCodec(): OpenHashMap[Int, Boolean] = JsonStringInput.read[OpenHashMap[Int, Boolean]](new String(jsonBytes, UTF_8))
*/
/* FIXME: Circe doesn't support parsing of OpenHashMap
  @Benchmark
  def readCirce(): OpenHashMap[Int, Boolean] = decode[OpenHashMap[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
/* FIXME: Jackson doesn't support parsing of OpenHashMap
  @Benchmark
  def readJacksonScala(): OpenHashMap[Int, Boolean] = jacksonMapper.readValue[OpenHashMap[Int, Boolean]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): OpenHashMap[Int, Boolean] = readFromArray[OpenHashMap[Int, Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): OpenHashMap[Int, Boolean] = Json.parse(jsonBytes).as[OpenHashMap[Int, Boolean]](openHashMapOfIntsToBooleansFormat)
/* FIXME: uPickle doesn't support parsing of OpenHashMap
  @Benchmark
  def readUPickle(): OpenHashMap[Int, Boolean] = read[OpenHashMap[Int, Boolean]](jsonBytes)
*/
/* FIXME: AVSystem GenCodec doesn't support serialization of OpenHashMap
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
/* FIXME: Circe doesn't support serialization of OpenHashMap
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
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(openHashMapOfIntsToBooleansFormat))
/* FIXME: uPickle doesn't support serialization of OpenHashMap
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}