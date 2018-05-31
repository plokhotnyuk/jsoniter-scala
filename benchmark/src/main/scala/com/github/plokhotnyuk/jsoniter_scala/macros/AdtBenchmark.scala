package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json
//import upickle.default._

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(Array(
  new Type(value = classOf[A], name = "A"),
  new Type(value = classOf[B], name = "B"),
  new Type(value = classOf[C], name = "C")))
sealed trait AdtBase extends Product with Serializable

case class A(a: Int) extends AdtBase

case class B(b: String) extends AdtBase

case class C(l: AdtBase, r: AdtBase) extends AdtBase

class AdtBenchmark extends CommonParams {
  var obj: AdtBase = C(A(1), B("VVV"))
  var jsonString: String = """{"type":"C","l":{"type":"A","a":1},"r":{"type":"B","b":"VVV"}}"""
  var jsonString2: String = """{"l":{"a":1,"type":"A"},"r":{"b":"VVV","type":"B"},"type":"C"}"""
  var jsonBytes: Array[Byte] = jsonString.getBytes(UTF_8)

  @Benchmark
  def readCirce(): AdtBase = decode[AdtBase](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): AdtBase = jacksonMapper.readValue[AdtBase](jsonBytes)

  @Benchmark
  def readJsoniterScala(): AdtBase = readFromArray[AdtBase](jsonBytes)(adtCodec)

  @Benchmark
  def readPlayJson(): AdtBase = Json.parse(jsonBytes).as[AdtBase](adtFormat)

/* FIXME cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def readUPickle(): AdtBase = read[AdtBase](jsonBytes)
*/
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(adtCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToPreallocatedArray(obj, preallocatedBuf, preallocatedOff)(adtCodec)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(adtFormat))

/* FIXME cannot alter uPickle discriminator name and value for ADT
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}