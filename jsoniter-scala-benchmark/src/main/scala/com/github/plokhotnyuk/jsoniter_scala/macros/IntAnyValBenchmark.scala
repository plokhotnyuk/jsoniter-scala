package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

//import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
//import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
//import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterScalaCodecs._
//import com.jsoniter.input.JsoniterJavaParser
//import com.jsoniter.output.JsoniterJavaSerializer
//import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
//import play.api.libs.json.Json
//import upickle.default._

case class i32(value: Int) extends AnyVal

class IntAnyValBenchmark extends CommonParams {
  var obj: i32 = i32(1234567890)
  var jsonString: String = obj.value.toString
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
/*
  @Benchmark
  def readAVSystemGenCodec(): i32 = JsonStringInput.read[i32](new String(jsonBytes, UTF_8))
*/
/*
  @Benchmark
  def readCirce(): i32 = decode[i32](new String(jsonBytes, UTF_8)).fold(throw _, x => x)
*/
/*
  @Benchmark
  def readDslJsonJava(): i32 = decodeDslJson[i32](jsonBytes)
*/
/*
  @Benchmark
  def readJacksonScala(): i32 = jacksonMapper.readValue[i32](jsonBytes)
*/
/*
  @Benchmark
  def readJsoniterJava(): i32 = JsoniterJavaParser.parse(jsonBytes, classOf[i32])
*/
  @Benchmark
  def readJsoniterScala(): i32 = readFromArray[i32](jsonBytes)(i32Codec)

  @Benchmark
  def readNaiveScala(): i32 = i32(new String(jsonBytes, UTF_8).toInt)
/*
  @Benchmark
  def readPlayJson(): i32 = Json.parse(jsonBytes).as[i32]
*/
/*
  @Benchmark
  def readUPickle(): i32 = read[i32](jsonBytes)
*/
/*
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
*/
/*
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
/*
  @Benchmark
  def writeDslJsonJava(): Array[Byte] = encodeDslJson[i32](obj)
*/
/*
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/
/*
  @Benchmark
  def writeJsoniterJava(): Array[Byte] = JsoniterJavaSerializer.serialize(obj)
*/
  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(i32Codec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(i32Codec)

  @Benchmark
  def writeNaiveScala(): Array[Byte] = obj.value.toString.getBytes(UTF_8)
/*
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
/*
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}