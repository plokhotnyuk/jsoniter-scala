package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json.JsonStringOutput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, writeToSubArray}
import org.openjdk.jmh.annotations.Benchmark

class Base16Writing extends Base16Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj, jsonBase16Options).getBytes(UTF_8)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.encode(obj)(base16Enc).toByteArray

  @Benchmark
  def jsoniterScala(): Array[Byte] = writeToArray(obj)(base16Codec)

  @Benchmark
  def jsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(base16Codec)
}