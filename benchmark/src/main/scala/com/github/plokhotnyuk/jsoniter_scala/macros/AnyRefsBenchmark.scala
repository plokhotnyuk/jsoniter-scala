package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

case class AnyRefs(s: String, bd: BigDecimal, os: Option[String])

class AnyRefsBenchmark extends CommonParams {
  val obj: AnyRefs = AnyRefs("s", 1, Some("os"))
  val jsonString: String = """{"s":"s","bd":1,"os":"os"}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): AnyRefs = decode[AnyRefs](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): AnyRefs = jacksonMapper.readValue[AnyRefs](jsonBytes)

  @Benchmark
  def readJsoniter(): AnyRefs = JsonReader.read(anyRefsCodec, jsonBytes)

  @Benchmark
  def readPlay(): AnyRefs = Json.parse(jsonBytes).as[AnyRefs](anyRefsFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(anyRefsCodec, obj)

  @Benchmark
  def writeJsoniterPrealloc(): Int = JsonWriter.write(anyRefsCodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(anyRefsFormat))
}