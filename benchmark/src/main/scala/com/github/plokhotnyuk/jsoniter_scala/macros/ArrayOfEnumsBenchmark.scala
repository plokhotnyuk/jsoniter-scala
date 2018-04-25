package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

import scala.collection.breakOut

object SuitEnum extends Enumeration {
  type SuitEnum = Value
  //FIXME setting of ids & names save from locking at synchronized block during withName() or toString() calls
  val Hearts: SuitEnum = Value(0, "Hearts")
  val Spades: SuitEnum = Value(1, "Spades")
  val Diamonds: SuitEnum = Value(2, "Diamonds")
  val Clubs: SuitEnum = Value(3, "Clubs")
}

class ArrayOfEnumsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[SuitEnum] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => SuitEnum((i * 1498724053) & 3))(breakOut)
    jsonString = obj.mkString("[\"", "\",\"", "\"]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Array[SuitEnum] = decode[Array[SuitEnum]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[SuitEnum] = jacksonMapper.readValue[Array[SuitEnum]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[SuitEnum] = readFromArray[Array[SuitEnum]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[SuitEnum] = Json.parse(jsonBytes).as[Array[SuitEnum]](enumArrayFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(enumArrayFormat))
}