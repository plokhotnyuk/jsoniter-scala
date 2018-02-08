package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class ArrayOfBytesBenchmark extends CommonParams {
  val obj: Array[Byte] = (1 to 128).map(_.toByte).toArray
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Byte] = decode[Array[Byte]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Array[Byte] = jacksonMapper.readValue[Array[Byte]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Array[Byte] = JsonReader.read(byteArrayCodec, jsonBytes)

  @Benchmark
  def readPlayJson(): Array[Byte] = Json.parse(jsonBytes).as[Array[Byte]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

/* FIXME: jackson serializes an array of bytes to a string encoded in base64
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)
*/

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = JsonWriter.write(byteArrayCodec, obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = JsonWriter.write(byteArrayCodec, obj, preallocatedBuf, 0)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
}