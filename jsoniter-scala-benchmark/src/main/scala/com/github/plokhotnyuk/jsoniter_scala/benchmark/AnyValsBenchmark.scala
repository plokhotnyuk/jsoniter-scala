package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.json._
import com.avsystem.commons.serialization.transparent
import com.fasterxml.jackson.annotation.JsonValue
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
import scala.annotation.meta.getter
//import upickle.default._

@transparent case class ByteVal(@(JsonValue @getter) a: Byte) extends AnyVal

@transparent case class ShortVal(@(JsonValue @getter) a: Short) extends AnyVal

@transparent case class IntVal(@(JsonValue @getter) a: Int) extends AnyVal

@transparent case class LongVal(@(JsonValue @getter) a: Long) extends AnyVal

@transparent case class BooleanVal(@(JsonValue @getter) a: Boolean) extends AnyVal

@transparent case class CharVal(@(JsonValue @getter) a: Char) extends AnyVal

@transparent case class DoubleVal(@(JsonValue @getter) a: Double) extends AnyVal

@transparent case class FloatVal(@(JsonValue @getter) a: Float) extends AnyVal

case class AnyVals(b: ByteVal, s: ShortVal, i: IntVal, l: LongVal, bl: BooleanVal, ch: CharVal, dbl: DoubleVal, f: FloatVal)

class AnyValsBenchmark extends CommonParams {
  //FIXME: 2.5 is for hiding of Play-JSON bug in serialization of floats: 2.2 -> 2.200000047683716
  var obj: AnyVals = AnyVals(ByteVal(1), ShortVal(2), IntVal(3), LongVal(4), BooleanVal(true), CharVal('x'), DoubleVal(1.1), FloatVal(2.5f))
  var jsonString: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): AnyVals = JsonStringInput.read[AnyVals](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): AnyVals = decode[AnyVals](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): AnyVals = jacksonMapper.readValue[AnyVals](jsonBytes)

  @Benchmark
  def readJsoniterScala(): AnyVals = readFromArray[AnyVals](jsonBytes)

  @Benchmark
  def readPlayJson(): AnyVals = Json.parse(jsonBytes).as[AnyVals](anyValsFormat)
/* FIXME: uPickle parses Long from JSON string only
  @Benchmark
  def readUPickle(): AnyVals = read[AnyVals](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(anyValsFormat))
/* FIXME: uPickle serializes Long as JSON string
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}