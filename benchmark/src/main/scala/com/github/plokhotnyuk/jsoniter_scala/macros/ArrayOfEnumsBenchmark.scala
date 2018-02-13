package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats.enumArrayFormat
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

object SuitEnum extends Enumeration {
  type SuitEnum = Value
  val Hearts, Spades, Diamonds, Clubs = Value
}

class ArrayOfEnumsBenchmark extends CommonParams {
  val obj: Array[SuitEnum] = (1 to 128).map(i => SuitEnum((i * 1498724053) & 3)).toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[SuitEnum] = decode[Array[SuitEnum]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
/* FIXME jackson-module-scala cannot parse string representation of enum values
  @Benchmark
  def readJacksonScala(): Array[SuitEnum] = jacksonMapper.readValue[Array[SuitEnum]](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): Array[SuitEnum] = read[Array[SuitEnum]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[SuitEnum] = Json.parse(jsonBytes).as[Array[SuitEnum]](enumArrayFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME jackson-module-scala store array of objects with "enumClass" field instead of strings
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = write(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(enumArrayFormat))
}