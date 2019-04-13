package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.lang.Character._
import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
//import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class StringOfNonAsciiCharsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: String = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

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
    jsonString = "\"" + obj + "\""
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): String = JsonStringInput.read[String](new String(jsonBytes, UTF_8))

  @Benchmark
  def readBorerJson(): String = io.bullet.borer.Json.decode(jsonBytes).to[String].value

  @Benchmark
  def readCirce(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): String = dslJsonDecode[String](jsonBytes)

  @Benchmark
  def readJacksonScala(): String = jacksonMapper.readValue[String](jsonBytes)

  @Benchmark
  def readJsoniterJava(): String = JsoniterJavaParser.parse[String](jsonBytes, classOf[String])

  @Benchmark
  def readJsoniterScala(): String = readFromArray[String](jsonBytes)(stringCodec)

  @Benchmark
  def readPlayJson(): String = Json.parse(jsonBytes).as[String]

  @Benchmark
  def readSprayJson(): String = spray.json.JsonParser(jsonBytes).convertTo[String]

  @Benchmark
  def readUPickle(): String = read[String](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeBorerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
/* FIXME: Jsoniter Java escapes non-ASCII characters
  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(stringCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(stringCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeSprayJson(): Array[Byte] = {
    import spray.json._
    obj.toJson.compactPrint.getBytes(UTF_8)
  }

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}