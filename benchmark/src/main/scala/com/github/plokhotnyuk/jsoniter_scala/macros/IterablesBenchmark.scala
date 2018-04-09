package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

import scala.collection.immutable.HashSet

case class Iterables(l: Vector[String], s: Set[Int], ls: List[HashSet[Long]])

class IterablesBenchmark extends CommonParams {
  val obj: Iterables = Iterables(Vector("1", "2", "3"), Set(4, 5, 6), List(HashSet(1, 2), HashSet()))
  val jsonString: String = """{"l":["1","2","3"],"s":[4,5,6],"ls":[[1,2],[]]}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Iterables = decode[Iterables](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Iterables = jacksonMapper.readValue[Iterables](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Iterables = readFromArray[Iterables](jsonBytes)

  @Benchmark
  def readPlayJson(): Iterables = Json.parse(jsonBytes).as[Iterables](iterablesFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(iterablesFormat))
}