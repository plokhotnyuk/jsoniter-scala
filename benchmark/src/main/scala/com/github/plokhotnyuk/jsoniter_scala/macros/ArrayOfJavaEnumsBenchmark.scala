package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats.javaEnumArrayFormat
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfJavaEnumsBenchmark extends CommonParams {
  val obj: Array[Suit] = (1 to 128).map { i =>
    (i * 1498724053) & 3 match {
      case 0 => Suit.Hearts
      case 1 => Suit.Spades
      case 2 => Suit.Diamonds
      case 3 => Suit.Clubs
    }
  }.toArray
  val jsonString: String = obj.mkString("[\"", "\",\"", "\"]")
  val jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
/* FIXME circe doesn't support Java enums
  @Benchmark
  def readCirce(): Array[Suit] = decode[Array[Suit]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
  @Benchmark
  def readJacksonScala(): Array[Suit] = jacksonMapper.readValue[Array[Suit]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Suit] = read[Array[Suit]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Suit] = Json.parse(jsonBytes).as[Array[Suit]](javaEnumArrayFormat)
/* FIXME circe doesn't support Java enums
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = write(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = write(obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(javaEnumArrayFormat))
}