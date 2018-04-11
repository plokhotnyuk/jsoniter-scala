package com.github.plokhotnyuk.jsoniter_scala.macros

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.generic.auto._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.mutable

class MutableBitSetBenchmark extends CommonParams {
  val obj: mutable.BitSet = mutable.BitSet(0 to 127: _*)
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

/* FIXME: Circe doesn't support parsing of bitsets
  @Benchmark
  def readCirce(): BitSets = decode[BitSets](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
/* FIXME: Jackson throws java.lang.IllegalArgumentException: Need exactly 1 type parameter for collection like types (scala.collection.immutable.BitSet)
  @Benchmark
  def readJacksonScala(): BitSet = jacksonMapper.readValue[BitSet](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): mutable.BitSet = readFromArray[mutable.BitSet](jsonBytes)

  @Benchmark
  def readPlayJson(): mutable.BitSet = Json.parse(jsonBytes).as[mutable.BitSet](mutableBitSetFormat)

/* FIXME: Circe doesn't support writing of bitsets
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
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableBitSetFormat))
}