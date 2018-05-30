package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import upickle.default._

class IntBenchmark extends CommonParams {
  var obj: Int = 1234567890
  var jsonString: String = obj.toString
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def readNaiveScala(): Int = new String(jsonBytes, UTF_8).toInt

  @Benchmark
  def readCirce(): Int = decode[Int](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): Int = decodeDslJson[Int](jsonBytes)

  @Benchmark
  def readJacksonScala(): Int = jacksonMapper.readValue[Int](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Int = readFromArray[Int](jsonBytes)(intCodec)

  @Benchmark
  def readPlayJson(): Int = Json.parse(jsonBytes).as[Int]

  @Benchmark
  def readUPickle(): Int = read[Int](jsonBytes)

  @Benchmark
  def writeNaiveScala(): Array[Byte] = obj.toString.getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[Int](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(intCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)(intCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeUPickle(): Array[Byte] = writeToBytes(obj)
}