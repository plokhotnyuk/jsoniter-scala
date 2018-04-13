package com.github.plokhotnyuk.jsoniter_scala.macros

//import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

import scala.collection.breakOut
import scala.collection.mutable

class MutableLongMapOfBooleansBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: mutable.LongMap[Boolean] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  setup()

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map { i =>
      ((i * 372036854775807L) / Math.pow(10, i % 18).toLong, ((i * 1498724053) & 1) == 0)
    }(breakOut)
    jsonString = obj.map(e => "\"" + e._1 + "\":" + e._2).mkString("{", ",", "}")
    jsonBytes = jsonString.getBytes
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

/* FIXME: Circe doesn't support mutable.LongMap
  @Benchmark
  def readCirce(): mutable.LongMap[Boolean] = decode[mutable.LongMap[Boolean]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
/* FIXME: Jackson throws Need exactly 2 type parameters for map like types (scala.collection.mutable.LongMap)
  @Benchmark
  def readJacksonScala(): mutable.LongMap[Boolean] = jacksonMapper.readValue[mutable.LongMap[Boolean]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): mutable.LongMap[Boolean] = readFromArray[mutable.LongMap[Boolean]](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.LongMap[Boolean] = Json.parse(jsonBytes).as[mutable.LongMap[Boolean]](mutableLongMapOfBooleansFormat)
/* FIXME: Circe doesn't support mutable.LongMap
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
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableLongMapOfBooleansFormat))
}