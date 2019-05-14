package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringOutput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
//import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.Benchmark

class OpenRTBWriting extends OpenRTBBenchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)
/* FIXME: Borer serializes fields with default values
  @Benchmark
  def borerJson(): Array[Byte] = io.bullet.borer.Json.encode(obj).toByteArray
*/
  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)

  @Benchmark
  def uPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}