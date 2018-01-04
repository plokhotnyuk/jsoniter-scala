package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.TwitterAPI._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class TwitterAPIBenchmark extends CommonParams {
  val obj: Seq[Tweet] = JsonReader.read(twitterAPICodec, jsonBytes)

  @Benchmark
  def readTwitterAPICirce(): Seq[Tweet] = decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readTwitterAPIJackson(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readTwitterAPIJsoniter(): Seq[Tweet] = JsonReader.read(twitterAPICodec, jsonBytes)

  @Benchmark
  def readTwitterAPIPlay(): Seq[Tweet] = Json.parse(jsonBytes).as[Seq[Tweet]](twitterAPIFormat)

  @Benchmark
  def writeTwitterAPICirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeTwitterAPIJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeTwitterAPIJsoniter(): Array[Byte] = JsonWriter.write(twitterAPICodec, obj)

  @Benchmark
  def writeTwitterAPIJsoniterPrealloc(): Int = JsonWriter.write(twitterAPICodec, obj, preallocatedBuf.get, 0)

  @Benchmark
  def writeTwitterAPIPlay(): Array[Byte] = Json.toBytes(Json.toJson(obj)(twitterAPIFormat))
}