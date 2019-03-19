package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
//import play.api.libs.json.Json

class BigIntBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var obj: BigInt = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    obj = BigInt(jsonString)
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): BigInt = JsonStringInput.read[BigInt](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): BigInt = decode[BigInt](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): BigInt = dslJsonDecode[BigInt](jsonBytes)

  @Benchmark
  def readJacksonScala(): BigInt = jacksonMapper.readValue[BigInt](jsonBytes)

  @Benchmark
  def readJsoniterScala(): BigInt = readFromArray[BigInt](jsonBytes)(bigIntCodec)
/* FIXME: PlayJson looses significant digits in big values
  @Benchmark
  def readPlayJson(): BigInt = Json.parse(jsonBytes).as[BigInt]
*/
  @Benchmark
  def readUPickle(): BigInt = read[BigInt](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)(bigIntgEncoder)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(bigIntCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(bigIntCodec)
/* FIXME: Play-json uses BigDecimal with engineering decimal representation to serialize numbers
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}