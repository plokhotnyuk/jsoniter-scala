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

case class Arrays(aa: Array[Array[Int]], a: Array[BigInt])

class ArraysBenchmark extends CommonParams {
  val obj: Arrays = Arrays(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(BigInt(7)))
  val jsonString: String = """{"aa":[[1,2,3],[4,5,6]],"a":[7]}"""
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Arrays = decode[Arrays](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJacksonScala(): Arrays = jacksonMapper.readValue[Arrays](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Arrays = readFromArray[Arrays](jsonBytes)

  @Benchmark
  def readPlayJson(): Arrays = Json.parse(jsonBytes).as[Arrays](arraysFormat)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(arraysFormat))
}