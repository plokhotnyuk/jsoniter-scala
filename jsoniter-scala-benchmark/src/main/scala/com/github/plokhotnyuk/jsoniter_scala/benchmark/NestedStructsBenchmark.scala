package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.avsystem.commons.serialization.transientDefault
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

case class NestedStructs(@transientDefault n: Option[NestedStructs] = None)

class NestedStructsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var obj: NestedStructs = _
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    obj = (1 to size).foldLeft(NestedStructs(None))((n, _) => NestedStructs(Some(n)))
    jsonBytes = writeToArray(obj)
    jsonString = new String(jsonBytes, UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readAVSystemGenCodec(): NestedStructs = JsonStringInput.read[NestedStructs](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): NestedStructs = decode[NestedStructs](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL-JSON throws java.io.IOException: Mandatory property (n) not found at position: 1502, following: `n":{"n":{"n":{"n":{}`
  @Benchmark
  def readDslJsonScala(): NestedStructs = decodeDslJson[NestedStructs](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)

  @Benchmark
  def readPlayJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs](nestedStructsFormat)

  @Benchmark
  def readUPickle(): NestedStructs = read[NestedStructs](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME: DSL-JSON serializes null value for Option.None
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = encodeDslJson[NestedStructs](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int =
    writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(nestedStructsFormat))

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}