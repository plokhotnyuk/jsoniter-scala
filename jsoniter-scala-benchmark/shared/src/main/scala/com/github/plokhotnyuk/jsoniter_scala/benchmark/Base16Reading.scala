package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.avsystem.commons.serialization.json.JsonStringInput
import com.github.plokhotnyuk.jsoniter_scala.benchmark.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.BorerJsonEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core.readFromArray
import org.openjdk.jmh.annotations.Benchmark

class Base16Reading extends Base16Benchmark {
  @Benchmark
  def avSystemGenCodec(): Array[Byte] = JsonStringInput.read[Array[Byte]](new String(jsonBytes, UTF_8), jsonBase16Options)

  @Benchmark
  def borer(): Array[Byte] = io.bullet.borer.Json.decode(jsonBytes).to[Array[Byte]](base16Dec).value

  @Benchmark
  def jsoniterScala(): Array[Byte] = readFromArray[Array[Byte]](jsonBytes, tooLongStringConfig)(base16Codec)
}