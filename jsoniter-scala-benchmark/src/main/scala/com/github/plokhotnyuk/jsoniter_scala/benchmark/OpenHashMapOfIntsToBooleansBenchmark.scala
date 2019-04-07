package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
//import upickle.default._

import scala.collection.mutable

class OpenHashMapOfIntsToBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: mutable.OpenHashMap[Int, Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = mutable.OpenHashMap((1 to size).map { i =>
      (((i * 1498724053) / Math.pow(10, i % 10)).toInt, ((i * 1498724053) & 0x1) == 0)
    }:_*)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): mutable.OpenHashMap[Int, Boolean] =
    JsonStringInput.read[mutable.OpenHashMap[Int, Boolean]](new String(jsonBytes, UTF_8))
/* FIXME: Circe doesn't support parsing of OpenHashMap
  @Benchmark
  def readCirce(): mutable.OpenHashMap[Int, Boolean] =
    decode[mutable.OpenHashMap[Int, Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
*/
/* FIXME: DSL-JSON throws NPE at com.dslplatform.json.runtime.Generics.getTypeNameCompat(Generics.java:200)
  @Benchmark
  def readDslJsonScala(): mutable.OpenHashMap[Int, Boolean] = dslJsonDecode[mutable.OpenHashMap[Int, Boolean]](jsonBytes)
*/
/* FIXME: Jackson doesn't support parsing of OpenHashMap
  @Benchmark
  def readJacksonScala(): mutable.OpenHashMap[Int, Boolean] =
    jacksonMapper.readValue[mutable.OpenHashMap[Int, Boolean]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): mutable.OpenHashMap[Int, Boolean] = readFromArray[mutable.OpenHashMap[Int, Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.OpenHashMap[Int, Boolean] =
    Json.parse(jsonBytes).as[mutable.OpenHashMap[Int, Boolean]](openHashMapOfIntsToBooleansFormat)
/* FIXME: uPickle doesn't support parsing of OpenHashMap
  @Benchmark
  def readUPickle(): mutable.OpenHashMap[Int, Boolean] = read[mutable.OpenHashMap[Int, Boolean]](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Circe doesn't support serialization of OpenHashMap
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
/* FIXME: DSL-JSON throws NPE at com.dslplatform.json.runtime.Generics.getTypeNameCompat(Generics.java:200)
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(openHashMapOfIntsToBooleansFormat))
/* FIXME: uPickle doesn't support serialization of OpenHashMap
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}