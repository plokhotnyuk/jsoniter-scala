package com.github.plokhotnyuk.jsoniter_scala.macros

import java.lang.Character._
import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.fasterxml.jackson.core.JsonGenerator
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup, TearDown}
import play.api.libs.json.Json
import upickle.default._

class StringOfEscapedCharsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: String = _
  var jsonString: String = _
  var jsonString2: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jacksonMapper.getFactory.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
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
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @TearDown
  def restore(): Unit =
    jacksonMapper.getFactory.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)

  @Benchmark
  def readAVSystemGenCodec(): String = JsonStringInput.read[String](new String(jsonBytes, UTF_8))

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

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj, escapingConfig)(stringCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff, escapingConfig)(stringCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.asciiStringify(Json.toJson(obj)).getBytes

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}