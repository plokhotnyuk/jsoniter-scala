package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

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
  val obj: AdtBase = C(A(1), B("VVV"))
  val jsonString: String = """{"type":"C","l":{"type":"A","a":1},"r":{"type":"B","b":"VVV"}}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): AdtBase = decode[AdtBase](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): AdtBase = jacksonMapper.readValue[AdtBase](jsonBytes)

  @Benchmark
  def readJsoniter(): AdtBase = JsonReader.read(adtCodec, jsonBytes)

  @Benchmark
  def readPlay(): AdtBase = Json.parse(jsonBytes).as[AdtBase](adtFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(adtCodec, obj)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(adtFormat))
}