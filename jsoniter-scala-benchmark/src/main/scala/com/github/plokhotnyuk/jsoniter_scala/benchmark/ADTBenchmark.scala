package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets._

import com.avsystem.commons.serialization.flatten
import com.avsystem.commons.serialization.json._
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
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
//import upickle.default._

@flatten("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[X], name = "X"),
  new Type(value = classOf[Y], name = "Y"),
  new Type(value = classOf[Z], name = "Z")))
sealed trait ADTBase extends Product with Serializable

case class X(a: Int) extends ADTBase

case class Y(b: String) extends ADTBase

case class Z(l: ADTBase, r: ADTBase) extends ADTBase

class ADTBenchmark extends CommonParams {
  var obj: ADTBase = Z(X(1), Y("VVV"))
  var jsonString1: String = """{"type":"Z","l":{"type":"X","a":1},"r":{"type":"Y","b":"VVV"}}"""
  var jsonString2: String = """{"l":{"a":1,"type":"X"},"r":{"b":"VVV","type":"Y"},"type":"Z"}"""
  var jsonString3: String = """{"r":{"b":"VVV","type":"Y"},"type":"Z","l":{"a":1,"type":"X"}}"""
  var jsonBytes: Array[Byte] = jsonString1.getBytes(UTF_8)
  var preallocatedBuf: Array[Byte] = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): ADTBase = JsonStringInput.read[ADTBase](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): ADTBase = decode[ADTBase](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): ADTBase = jacksonMapper.readValue[ADTBase](jsonBytes)

  @Benchmark
  def readJsoniterScala(): ADTBase = readFromArray[ADTBase](jsonBytes)(adtCodec)

  @Benchmark
  def readPlayJson(): ADTBase = Json.parse(jsonBytes).as[ADTBase](adtFormat)

/* FIXME: cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def readUPickle(): ADTBase = read[ADTBase](jsonBytes)
*/
  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(adtCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(adtCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(adtFormat))

/* FIXME: cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}