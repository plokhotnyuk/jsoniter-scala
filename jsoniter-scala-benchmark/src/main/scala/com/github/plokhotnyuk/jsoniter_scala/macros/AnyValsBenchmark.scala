package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterScalaCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
//import io.circe.generic.auto._
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json
//import upickle.default._

case class ByteVal(a: Byte) extends AnyVal

case class ShortVal(a: Short) extends AnyVal

case class IntVal(a: Int) extends AnyVal

case class LongVal(a: Long) extends AnyVal

case class BooleanVal(a: Boolean) extends AnyVal

case class CharVal(a: Char) extends AnyVal

case class DoubleVal(a: Double) extends AnyVal

case class FloatVal(a: Float) extends AnyVal

case class AnyVals(b: ByteVal, s: ShortVal, i: IntVal, l: LongVal, bl: BooleanVal, ch: CharVal, dbl: DoubleVal, f: FloatVal)

class AnyValsBenchmark extends CommonParams {
  //FIXME: 2.5 is for hiding of Play-JSON bug in serialization of floats: 2.2 -> 2.200000047683716
  var obj: AnyVals = AnyVals(ByteVal(1), ShortVal(2), IntVal(3), LongVal(4), BooleanVal(true), CharVal('x'), DoubleVal(1.1), FloatVal(2.5f))
  var jsonString: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)

/*
  @Benchmark
  def readAVSystemGenCodec(): AnyVals = JsonStringInput.read[AnyVals](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): AnyVals = decode[AnyVals](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): AnyVals = jacksonMapper.readValue[AnyVals](jsonBytes)
*/
  @Benchmark
  def readJsoniterScala(): AnyVals = readFromArray[AnyVals](jsonBytes)
/*
  @Benchmark
  def readPlayJson(): AnyVals = Json.parse(jsonBytes).as[AnyVals](anyValsFormat)
*/
/* FIXME: uPickle parses Long from JSON string only
  @Benchmark
  def readUPickle(): AnyVals = read[AnyVals](jsonBytes)
*/
/*
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/*
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(anyValsFormat))
*/
/* FIXME: uPickle serializes Long as JSON string
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}