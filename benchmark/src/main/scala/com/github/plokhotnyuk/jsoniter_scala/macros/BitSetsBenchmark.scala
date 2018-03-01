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

import scala.collection.immutable.BitSet
import scala.collection.mutable

case class BitSets(bs: BitSet, mbs: mutable.BitSet)

class BitSetsBenchmark extends CommonParams {
  val obj: BitSets = BitSets(BitSet(1, 2, 3), mutable.BitSet(4, 5, 6))
  val jsonString: String = """{"bs":[1,2,3],"mbs":[4,5,6]}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

/* FIXME: Circe doesn't support parsing of bitsets
  @Benchmark
  def readCirce(): BitSets = decode[BitSets](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/

  @Benchmark
  def readJacksonScala(): BitSets = jacksonMapper.readValue[BitSets](jsonBytes)

  @Benchmark
  def readJsoniterScala(): BitSets = readFromArray[BitSets](jsonBytes)

  @Benchmark
  def readPlayJson(): BitSets = Json.parse(jsonBytes).as[BitSets](bitSetsFormat)

/* FIXME: Circe doesn't support writing of bitsets
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(bitSetsFormat))
}