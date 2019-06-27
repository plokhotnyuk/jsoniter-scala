package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.ScalikeJacksonFormatters._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
//import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark

class TwitterAPIWriting extends TwitterAPIBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Borer serializes empty collections
  @Benchmark
  def borerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray
*/
/* FIXME: circe serializes empty collections
  @Benchmark
  def circe(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)
*/
/* FIXME: DSL-JSON serializes empty collections
  @Benchmark
  def dslJsonScala(): Array[Byte] = dslJsonEncode(obj)
*/
  @Benchmark
  def jacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)
/* FIXME: Play-JSON serializes empty collections
  @Benchmark
  def playJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
/*
  @Benchmark
  def scalikeJackson(): Array[Byte] = {
    import reug.scalikejackson.ScalaJacksonImpl._

    obj.write.getBytes(UTF_8)
  }
*/
/* FIXME: Spray-JSON serializes empty collections
  @Benchmark
  def sprayJson(): Array[Byte] = obj.toJson.compactPrint.getBytes(UTF_8)
*/
  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}