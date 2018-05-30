package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

class ArrayOfBytesBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 10
  var obj: Array[Byte] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(_.toByte).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
/*FIXME:dsl-json expects a base64 string for the byte array
  @Benchmark
  def readDslJsonJava(): Array[Byte] = decodeDslJson[Array[Byte]](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]]

  @Benchmark
  def readUPickle(): Array[Byte] = read[Array[Byte]](jsonBytes)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME:dsl-json serializes a byte array to the base64 string
  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[Array[Byte]](obj).toByteArray
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeUPickle(): Array[Byte] = writeToBytes(obj)
}