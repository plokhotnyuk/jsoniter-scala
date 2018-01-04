package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
//import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class IntArrayBenchmark extends CommonParams {
  val obj: Array[Int] = (1 to 1000).map(i => ((i * 1498724053) / Math.pow(10, i % 10)).toInt).toArray
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Int] = decode[Array[Int]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): Array[Int] = jacksonMapper.readValue[Array[Int]](jsonBytes)

  @Benchmark
  def readJsoniter(): Array[Int] = JsonReader.read(intArrayCodec, jsonBytes)

  @Benchmark
  def readPlay(): Array[Int] = Json.parse(jsonBytes).as[Array[Int]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(intArrayCodec, obj)

  @Benchmark
  def writeJsoniterPrealloc(): Int = JsonWriter.write(intArrayCodec, obj, preallocatedBuf.get, 0)

  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}