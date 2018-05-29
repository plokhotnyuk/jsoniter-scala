package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json

case class NestedStructs(n: Option[NestedStructs])

class NestedStructsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 128
  var obj: NestedStructs = _
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _

  @Setup
  def setup(): Unit = {
    obj = {
      var n = NestedStructs(None)
      var i = 0
      while (i < size) {
        n = NestedStructs(Some(n))
        i += 1
      }
      n
    }
    jsonBytes = writeToArray(obj)(nestedStructsCodec)
    jsonString = new String(jsonBytes, UTF_8)
    preallocatedBuf = new Array[Byte](jsonBytes.length + preallocatedOff + 100/*to avoid possible out of bounds error*/)
  }

  @Benchmark
  def readCirce(): NestedStructs = decode[NestedStructs](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): NestedStructs = jacksonMapper.readValue[NestedStructs](jsonBytes)

  @Benchmark
  def readJsoniterScala(): NestedStructs = readFromArray[NestedStructs](jsonBytes)(nestedStructsCodec)

  @Benchmark
  def readPlayJson(): NestedStructs = Json.parse(jsonBytes).as[NestedStructs](nestedStructsFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(nestedStructsCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int =
    writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)(nestedStructsCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(nestedStructsFormat))
}