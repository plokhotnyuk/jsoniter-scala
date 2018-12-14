package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.lang.Character._
import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
//import com.jsoniter.output.{EncodingMode, JsoniterJavaSerializer}
//import com.jsoniter.spi.{Config, DecodingMode}
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class StringOfEscapedCharsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 100
  var obj: String = _
  var jsonString: String = _
  var jsonString2: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = {
    val jm = createJacksonMapper
    jm.getFactory.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
    jm
  }
/*
  val jsoniterJavaConfig: Config = new Config.Builder()
    .escapeUnicode(true)
    .encodingMode(EncodingMode.DYNAMIC_MODE)
    .decodingMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_STRICTLY)
    .build()
*/

  @Setup
  def setup(): Unit = {
    obj = {
      val cs = new Array[Char](size)
      var i = 0
      var j = 1
      while (i < cs.length) {
        cs(i) = {
          var ch: Char = 0
          do {
            ch = (j * 1498724053).toChar
            j += 1
          } while (ch < 128 || isSurrogate(ch))
          ch
        }
        i += 1
      }
      new String(cs)
    }
    jsonString = "\"" + obj.map(ch => f"\\u$ch%04x").mkString  + "\""
    jsonString2 = "\"" + obj.map(ch => f"\\u$ch%04X").mkString  + "\""
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): String = JsonStringInput.read[String](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): String = decodeDslJson[String](jsonBytes)

  @Benchmark
  def readJacksonScala(): String = jacksonMapper.readValue[String](jsonBytes)

  @Benchmark
  def readJsoniterJava(): String = JsoniterJavaParser.parse[String](jsonBytes, classOf[String])

  @Benchmark
  def readJsoniterScala(): String = readFromArray[String](jsonBytes, longStringConfig)(stringCodec)

  @Benchmark
  def readPlayJson(): String = Json.parse(jsonBytes).as[String]

  @Benchmark
  def readUPickle(): String = read[String](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, JsonOptions(asciiOutput = true)).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = escapingPrinter.pretty(obj.asJson).getBytes

/* FIXME: DSL-JSON doesn't support escaping of non-ASCII characters
  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[String](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

/* FIXME: Jsoniter Java cannot restore config properly
  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj, jsoniterJavaConfig)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj, escapingConfig)(stringCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length, escapingConfig)(stringCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.asciiStringify(Json.toJson(obj)).getBytes

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj, escapeUnicode = true).getBytes(UTF_8)
}