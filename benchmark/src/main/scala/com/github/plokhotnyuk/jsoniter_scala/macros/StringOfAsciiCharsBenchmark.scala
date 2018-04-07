package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class StringOfAsciiCharsBenchmark extends CommonParams {
  val obj: String =
    "In computer science, an inverted index is an index data structure storing a mapping from content to its locations in a document."
  val jsonString: String =
    """"In computer science, an inverted index is an index data structure storing a mapping from content to its locations in a document.""""
  val jsonBytes: Array[Byte] = jsonString.getBytes("UTF-8")

  @Benchmark
  def readCirce(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): String = decodeDslJson[String](jsonBytes)

  @Benchmark
  def readJacksonScala(): String = jacksonMapper.readValue[String](jsonBytes)

  @Benchmark
  def readJsoniterScala(): String = readFromArray[String](jsonBytes)(stringCodec)

  @Benchmark
  def readPlayJson(): String = Json.parse(jsonBytes).as[String]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[String](obj).toByteArray

  @Benchmark
  def writeDslJsonJavaPrealloc(): com.dslplatform.json.JsonWriter = encodeDslJson[String](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(stringCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, 0)(stringCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}