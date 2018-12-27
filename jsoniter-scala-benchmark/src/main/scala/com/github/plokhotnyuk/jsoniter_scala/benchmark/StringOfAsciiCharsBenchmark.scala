package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class StringOfAsciiCharsBenchmark extends CommonParams {
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
            ch = ((j * 1498724053) % 128).toChar
            j += 1
          } while (!JsonWriter.isNonEscapedAscii(ch) || ch == '~') // FIXME: Jsoniter(Java) uses hexadecimal escaping for `~`
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
  def readCirce(): String = decode[String](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): String = decodeDslJson[String](jsonBytes)

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
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = encodeDslJson[String](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(stringCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(stringCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}