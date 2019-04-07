package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.avsystem.commons.serialization.transientDefault
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.SprayFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import spray.json._

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

  @Benchmark
  def readDslJsonScala(): NestedStructs = dslJsonDecode[NestedStructs](jsonBytes)

  @Benchmark
  def readJacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)

  @Benchmark
  def readPlayJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs]

  @Benchmark
  def readSprayJson(): NestedStructs = JsonParser(jsonBytes).convertTo[NestedStructs](nestedStructsJsonFormat)

  @Benchmark
  def readUPickle(): NestedStructs = read[NestedStructs](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
/* FIXME: DSL-JSON serializes null value for Option.None
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode(obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int =
    writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))

  @Benchmark
  def writeSprayJson(): Array[Byte] = obj.toJson(nestedStructsJsonFormat).compactPrint.getBytes(UTF_8)

  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}