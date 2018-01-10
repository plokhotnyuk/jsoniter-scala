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
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

case class Primitives(b: Byte, s: Short, i: Int, l: Long, bl: Boolean, ch: Char, dbl: Double, f: Float)

class PrimitivesBenchmark extends CommonParams {
  val obj: Primitives = Primitives(1, 2, 3, 4, bl = true, ch = 'x', 1.1, 2.5f)
  val jsonString: String = """{"b":1,"s":2,"i":3,"l":4,"bl":true,"ch":"x","dbl":1.1,"f":2.5}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Primitives = decode[Primitives](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): Primitives = jacksonMapper.readValue[Primitives](jsonBytes)

  @Benchmark
  def readJsoniter(): Primitives = JsonReader.read(primitivesCodec, jsonBytes)

  @Benchmark
  def readPlay(): Primitives = Json.parse(jsonBytes).as[Primitives](primitivesFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(primitivesCodec, obj)

  @Benchmark
  def writeJsoniterPrealloc(): Int = JsonWriter.write(primitivesCodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(primitivesFormat))
}