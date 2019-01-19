package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.jsoniter.input.JsoniterJavaParser
import com.jsoniter.output.JsoniterJavaSerializer
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

class ArrayOfLongsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 1000
  var obj: Array[Long] = _
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).map(i => (i * 372036854775807L) / Math.pow(10, i % 18).toLong).toArray
    jsonString = obj.mkString("[", ",", "]")
    jsonBytes = jsonString.getBytes(UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): Array[Long] = JsonStringInput.read[Array[Long]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Array[Long] = decode[Array[Long]](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): Array[Long] = dslJsonDecode[Array[Long]](jsonBytes)

  @Benchmark
  def readJacksonScala(): Array[Long] = jacksonMapper.readValue[Array[Long]](jsonBytes)

  @Benchmark
  def readJsoniterJava(): Array[Long] = JsoniterJavaParser.parse[Array[Long]](jsonBytes, classOf[Array[Long]])

  @Benchmark
  def readJsoniterScala(): Array[Long] = readFromArray[Array[Long]](jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Long] = Json.parse(jsonBytes).as[Array[Long]]

  @Benchmark
  def readUPickle(): Array[Long] = read[Array[Long]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)

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
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}