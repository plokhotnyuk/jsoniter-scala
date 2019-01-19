package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.TwitterAPI._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.generic.auto._
import io.circe.parser._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class TwitterAPIBenchmark extends CommonParams {
  var obj: Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)
  var preallocatedBuf: Array[Byte] = new Array(compactJsonBytes.length + 100/*to avoid possible out of bounds error*/)

  @Benchmark
  def readAVSystemGenCodec(): Seq[Tweet] = JsonStringInput.read[Seq[Tweet]](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): Seq[Tweet] = decode[Seq[Tweet]](new String(jsonBytes, UTF_8)).fold(throw _, identity)
/* FIXME: DSL_JSON throws java.lang.IllegalArgumentException: argument type mismatch
  @Benchmark
  def readDslJsonScala(): Seq[Tweet] = dslJsonDecode[Seq[Tweet]](jsonBytes)
*/
  @Benchmark
  def readJacksonScala(): Seq[Tweet] = jacksonMapper.readValue[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readJsoniterScala(): Seq[Tweet] = readFromArray[Seq[Tweet]](jsonBytes)

  @Benchmark
  def readPlayJson(): Seq[Tweet] = Json.parse(jsonBytes).as[Seq[Tweet]](twitterAPIFormat)

  @Benchmark
  def readUPickle(): Seq[Tweet] = read[Seq[Tweet]](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: circe serializes empty collections
  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
/* FIXME: DSL-JSON serializes empty collections
  @Benchmark
  def writeDslJsonScala(): Array[Byte] = encodeDslJson[Seq[Tweet]](obj)
*/
  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play-JSON serializes empty collections
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj)(twitterAPIFormat))
*/
/* FIXME: uPickle serializes empty collections
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
*/
}