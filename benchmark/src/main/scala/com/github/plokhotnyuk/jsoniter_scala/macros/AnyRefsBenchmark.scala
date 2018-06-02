package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

class AnyRefsBenchmark extends CommonParams {
  var obj: AnyRefs = AnyRefs("s", 1, Some("os"))
  var jsonString: String = """{"s":"s","bd":1,"os":"os"}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def readCirce(): AnyRefs = decode[AnyRefs](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): AnyRefs = decodeDslJson[AnyRefs](jsonBytes)

  @Benchmark
  def readJacksonScala(): AnyRefs = jacksonMapper.readValue[AnyRefs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): AnyRefs = readFromArray[AnyRefs](jsonBytes)

  @Benchmark
  def readPlayJson(): AnyRefs = Json.parse(jsonBytes).as[AnyRefs](anyRefsFormat)

/* FIXME: cannot alter uPickle to store BigDecimal as JSON number
  @Benchmark
  def readUPickle(): AnyRefs = read[AnyRefs](jsonBytes)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[AnyRefs](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(anyRefsFormat))

/* FIXME: cannot alter uPickle to store BigDecimal as JSON number
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}