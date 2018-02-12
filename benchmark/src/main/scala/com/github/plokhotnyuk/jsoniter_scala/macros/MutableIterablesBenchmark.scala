package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.mutable

case class MutableIterables(l: mutable.ArrayBuffer[String], s: mutable.TreeSet[Int], ls: mutable.ResizableArray[mutable.Set[Long]])

class MutableIterablesBenchmark extends CommonParams {
  val obj: MutableIterables = MutableIterables(mutable.ArrayBuffer("1", "2", "3"), mutable.TreeSet(4, 5, 6),
    mutable.ResizableArray(mutable.Set(1, 2), mutable.Set()))
  val jsonString: String = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes
  @Benchmark
  def readCirce(): MutableIterables = decode[MutableIterables](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

/* FIXME: Jackson-module-scala doesn't support parsing of tree sets
  @Benchmark
  def readJacksonScala(): MutableIterables = jacksonMapper.readValue[MutableIterables](jsonBytes)
*/

  @Benchmark
  def readJsoniterScala(): MutableIterables = read[MutableIterables](jsonBytes)

  @Benchmark
  def readPlayJson(): MutableIterables = Json.parse(jsonBytes).as[MutableIterables](mutableIterablesFormat)
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = write(obj)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(mutableIterablesFormat))
}