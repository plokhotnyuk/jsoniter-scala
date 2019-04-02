package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._
import spray.json._

class ArrayOfBytesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Byte] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(_.toByte).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/*FIXME:dsl-json expects a base64 string for the byte array
  @Benchmark
  def readDslJsonScala(): Array[Byte] = dslJsonDecode[Array[Byte]](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def readJsoniterJava(): Array[Byte] = JsoniterJavaParser.parse[Array[Byte]](jsonBytes, classOf[Array[Byte]])

  @Benchmark
  def readJsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]]

  @Benchmark
  def readSprayJson(): Array[Byte] = JsonParser(jsonBytes).convertTo[Array[Byte]]

  @Benchmark
  def readUPickle(): Array[Byte] = read[Array[Byte]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME:dsl-json serializes a byte array to the base64 string
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode[Array[Byte]](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}